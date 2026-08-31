-- Failures in a row, as distinct from failures in total. Someone who missed a concept
-- twice a month ago and has had it right since does not need scaffolding; someone who has
-- just missed it three times running does.
alter table learner_concept add column consecutive_failures integer not null default 0;

-- What help the question came with, so a right answer can be weighted for it.
alter table exercise add column scaffold text not null default 'NONE';
alter table exercise add column choices text;
