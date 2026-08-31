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
    abstraction_tolerance double precision not null default 0.5
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
    constraint learner_concept_unique unique (learner_id, concept_id)
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
    created_at      timestamp with time zone not null
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
