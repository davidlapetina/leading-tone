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
    updated_at        timestamp with time zone not null
);
