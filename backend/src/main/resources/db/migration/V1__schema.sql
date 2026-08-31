-- The whole schema, in one file.
--
-- Kept as a single migration rather than a chain: this application is a local, single-user
-- program, and nobody upgrading it needs their existing database preserved across a
-- development version. If that changes, add V2 and stop editing this file.
--
-- Conventions here: lowercase SQL, `text` for strings, `timestamp with time zone` for
-- instants, and a comment on anything whose reason is not obvious from its name.

-- The learner model is the part of this application worth protecting: evidence is
-- append-only, and mastery is always derivable from it.

create table learner (
    id                    uuid primary key,
    display_name          text        not null,
    created_at            timestamp with time zone not null,
    explanation_depth     double precision not null default 0.5,
    socratic_preference   double precision not null default 0.5,
    notation_preference   double precision not null default 0.5,
    keyboard_preference   double precision not null default 0.5,
    aural_preference      double precision not null default 0.5,
    example_preference    double precision not null default 0.5,
    abstraction_tolerance double precision not null default 0.5,
    -- A learner may say how they want to practise ("let me play these"); null is
    -- guided mode, where the tutor chooses from the learner model.
    preferred_answer_mode  text,
    focus_concept_id       text,
    focus_category         text
);

-- Mirrors concepts.json so the graph can be queried in SQL. Reseeded at startup.
create table concept (
    id                   text primary key,
    name                 text not null,
    description          text not null,
    category             text not null,
    intrinsic_difficulty double precision not null
);

create table concept_prerequisite (
    concept_id      text not null references concept (id) on delete cascade,
    prerequisite_id text not null references concept (id) on delete cascade,
    primary key (concept_id, prerequisite_id)
);

create table learner_concept (
    id                   uuid primary key,
    learner_id           uuid not null references learner (id) on delete cascade,
    concept_id           text not null,
    mastery              double precision not null default 0,
    confidence           double precision not null default 0,
    state                text not null,
    successful_evidence  integer not null default 0,
    failed_evidence      integer not null default 0,
    strong_evidence      integer not null default 0,
    last_practiced_at    timestamp with time zone,
    next_review_at       timestamp with time zone,
    review_interval_days integer not null default 0,
    constraint learner_concept_unique unique (learner_id, concept_id),
    -- Failures in a row, as distinct from failures in total: someone who missed a
    -- concept three times running needs a smaller step, not a lower score.
    consecutive_failures   integer not null default 0
);

create index learner_concept_review_idx on learner_concept (learner_id, next_review_at);

create table tutor_session (
    id         uuid primary key,
    learner_id uuid not null references learner (id) on delete cascade,
    started_at timestamp with time zone not null,
    ended_at   timestamp with time zone
);

create table exercise (
    id              uuid primary key,
    learner_id      uuid not null references learner (id) on delete cascade,
    session_id      uuid references tutor_session (id) on delete set null,
    concept_id      text not null,
    exercise_type   text not null,
    answer_mode     text not null,
    prompt          text not null,
    expected_answer text not null,
    key_context     text,
    difficulty      double precision not null,
    notation_abc    text,
    solved          boolean not null default false,
    created_at      timestamp with time zone not null,
    task_kind              text not null default 'IDENTIFY',
    -- What help the question came with, so a right answer is weighted for it.
    scaffold               text not null default 'NONE',
    choices                text
);

create index exercise_learner_idx on exercise (learner_id, created_at desc);

create table interaction (
    id              uuid primary key,
    session_id      uuid not null references tutor_session (id) on delete cascade,
    sequence        integer not null,
    role            text not null,
    content         text not null,
    teaching_action text,
    target_concepts text,
    notation_abc    text,
    expects_answer  boolean not null default false,
    answer_mode     text,
    exercise_id     uuid references exercise (id) on delete set null,
    created_at      timestamp with time zone not null,
    constraint interaction_sequence_unique unique (session_id, sequence)
);

-- Append-only. Every mastery value in the application traces back to rows in here.
create table evidence (
    id             uuid primary key,
    learner_id     uuid not null references learner (id) on delete cascade,
    concept_id     text not null,
    session_id     uuid references tutor_session (id) on delete set null,
    interaction_id uuid references interaction (id) on delete set null,
    exercise_id    uuid references exercise (id) on delete set null,
    evidence_type  text not null,
    result         text not null,
    correctness    double precision not null,
    difficulty     double precision not null,
    confidence     double precision not null,
    weight         double precision not null,
    mastery_before double precision not null,
    mastery_after  double precision not null,
    source         text,
    created_at     timestamp with time zone not null
);

create index evidence_learner_concept_idx on evidence (learner_id, concept_id, created_at desc);

create table misconception (
    id          uuid primary key,
    learner_id  uuid not null references learner (id) on delete cascade,
    concept_id  text not null,
    code        text not null,
    description text not null,
    occurrences integer not null default 1,
    detected_at timestamp with time zone not null,
    last_seen_at timestamp with time zone not null,
    resolved_at timestamp with time zone,
    constraint misconception_unique unique (learner_id, concept_id, code)
);

create table exercise_attempt (
    id          uuid primary key,
    exercise_id uuid not null references exercise (id) on delete cascade,
    raw_answer  text not null,
    correct     boolean not null,
    partial     boolean not null default false,
    feedback    text,
    detail      text,
    created_at  timestamp with time zone not null
);


-- Configuration lives in the database rather than in the environment, so it can be changed
-- from the interface and survives a restart. The defaults below are exactly the values the
-- application shipped with as environment variables.
create table settings (
    id                uuid primary key,
    llm_enabled       boolean not null default true,
    tools_enabled     boolean not null default true,
    model             text not null default 'qwen3:8b',
    base_url          text not null default 'http://localhost:11434',
    temperature       double precision not null default 0.8,
    num_ctx           integer not null default 8192,
    think             boolean not null default false,
    timeout_seconds   integer not null default 60,
    cooldown_seconds  integer not null default 120,
    memory_messages   integer not null default 10,
    learner_name      text not null default 'Student',
    updated_at        timestamp with time zone not null,
    -- Retrieval can be switched off without uninstalling anything. Separate from
    -- llm_enabled: quoting a source and asking a model to phrase it are two decisions.
    knowledge_enabled      boolean not null default true,
    -- Whether this deployment is commercial. Thirteen of the fourteen configured
    -- sources are NonCommercial, and that condition survives ingestion, chunking and
    -- embedding, so it is enforced at retrieval rather than shown and hoped for.
    runtime_mode           text not null default 'NON_COMMERCIAL'
);


-- The knowledge layer: published sources the tutor can quote, and the provenance needed
-- to credit them.
--
-- Third-party knowledge is not this application's to relicense. The application's own code
-- is MIT (see /LICENSE); nothing recorded here is. Every document and every chunk carries
-- the licence it arrived under, and downloading, parsing, chunking, embedding or indexing
-- does not change those terms. See THIRD_PARTY_NOTICES.md and licenses/.
--
-- Additive only: five new tables and one new settings column, so an existing install
-- upgrades without touching a row it already has.

-- Retrieval can be switched off without uninstalling anything. It is separate from
-- llm_enabled because grounding a lesson in a published source and asking a language
-- model to phrase it are two different decisions.

-- What happened to a declared source on this machine. What a source *is*, and on whose
-- terms, lives in knowledge-sources.yaml under review; this table can never widen it.
create table knowledge_source (
    id                  text primary key,
    display_name        text not null,
    license_id          text not null,
    license_status      text not null,
    ingestion_mode      text not null,
    state               text not null,
    enabled             boolean not null default true,
    source_version      text,
    source_commit       text,
    fingerprint         text,
    parser_version      integer not null default 0,
    embedding_model     text,
    active_generation   integer not null default 0,
    document_count      integer not null default 0,
    chunk_count         integer not null default 0,
    harmony_count       integer not null default 0,
    retrieved_at        timestamp with time zone,
    last_error          text,
    updated_at          timestamp with time zone not null
);

-- The licence is per document because a source's licence is only a default. Of Open Music
-- Theory's 140 chapters, 138 are CC BY-SA 4.0, one is CC BY-NC-SA 4.0, and one is All
-- Rights Reserved and is never fetched at all.
create table knowledge_document (
    id            uuid primary key,
    source_id     text not null references knowledge_source (id) on delete cascade,
    generation    integer not null,
    external_id   text not null,
    title         text not null,
    part_title    text,
    url           text,
    authors       text,
    license_id    text not null,
    attribution   text not null,
    checksum      text not null,
    word_count    integer not null default 0,
    body          text,
    active        boolean not null default false,
    ingested_at   timestamp with time zone not null,
    constraint knowledge_document_unique unique (source_id, generation, external_id)
);

create index knowledge_document_active_idx on knowledge_document (source_id, active);

-- One retrievable passage. Every vector in the Lucene index resolves back to a row here:
-- a passage that cannot say where it came from does not get indexed.
create table knowledge_chunk (
    id             uuid primary key,
    document_id    uuid not null references knowledge_document (id) on delete cascade,
    source_id      text not null,
    generation     integer not null,
    chunk_key      text not null,
    document_title text,
    section_title  text,
    section_order  integer not null default 0,
    chunk_order    integer not null default 0,
    kind           text not null,
    body           text not null,
    concept_ids    text,
    license_id     text not null,
    attribution    text not null,
    url            text,
    word_count     integer not null default 0,
    active         boolean not null default false,
    constraint knowledge_chunk_unique unique (document_id, section_order, chunk_order)
);

create index knowledge_chunk_active_idx on knowledge_chunk (source_id, active);
create index knowledge_chunk_key_idx on knowledge_chunk (chunk_key);

-- One row per attempt, so a failure can still be explained a week later. A run that fails
-- halfway leaves the generation that is currently serving exactly as it was.
create table knowledge_ingestion_run (
    id                        uuid primary key,
    source_id                 text not null references knowledge_source (id) on delete cascade,
    generation                integer not null,
    started_at                timestamp with time zone not null,
    finished_at               timestamp with time zone,
    final_state               text not null,
    documents_seen            integer not null default 0,
    documents_ingested        integer not null default 0,
    documents_skipped_license integer not null default 0,
    documents_skipped_empty   integer not null default 0,
    chunks_written            integer not null default 0,
    harmony_written           integer not null default 0,
    skipped                   boolean not null default false,
    embedding_model           text,
    fingerprint               text,
    message                   text
);

create index knowledge_ingestion_run_source_idx on knowledge_ingestion_run (source_id, started_at desc);

-- Harmonic annotations of real music, kept as queryable rows rather than prose. This is
-- what lets "give me a Beethoven example of V/V" be answered from an actual score instead
-- of being invented: if there is no row, the tutor says there is no verified example.
create table knowledge_harmony_event (
    id              uuid primary key,
    source_id       text not null references knowledge_source (id) on delete cascade,
    generation      integer not null,
    composer        text not null,
    work            text not null,
    movement        text,
    measure         integer,
    beat            double precision,
    global_key      text,
    local_key       text,
    roman_numeral   text,
    chord_label     text,
    chord_type      text,
    figbass         text,
    relative_root   text,
    cadence         text,
    phrase_end      boolean not null default false,
    source_reference text not null,
    license_id      text not null,
    active          boolean not null default false
);

create index knowledge_harmony_lookup_idx
    on knowledge_harmony_event (active, roman_numeral, composer);
create index knowledge_harmony_cadence_idx
    on knowledge_harmony_event (active, cadence);


-- Two things the knowledge layer needs to be honest about.
--
-- 1. Whether this deployment is commercial. Thirteen of the fourteen configured sources are
--    NonCommercial, and that condition survives ingestion, chunking and embedding. It is
--    enforced at retrieval, not shown in the interface and hoped for.
--
-- 2. What actually went into an answer. Not the model's reasoning -- that is neither
--    observable nor ours to keep -- but the retrievals and calculations, which are.


create table response_provenance (
    id                  uuid primary key,
    interaction_id      uuid,
    session_id          uuid,
    concept_id          text,
    intents             text,
    chunk_ids           text,
    harmony_event_ids   text,
    theory_operations   text,
    source_ids          text,
    created_at          timestamp with time zone not null
);

create index response_provenance_interaction_idx on response_provenance (interaction_id);
create index response_provenance_session_idx on response_provenance (session_id, created_at desc);
