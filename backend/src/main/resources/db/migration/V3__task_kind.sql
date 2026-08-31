-- Which kind of task an exercise was, so the tutor can avoid asking the same one twice
-- running. Existing rows were all recognise-or-build questions; IDENTIFY is the safe
-- reading for them.
alter table exercise add column task_kind text not null default 'IDENTIFY';
