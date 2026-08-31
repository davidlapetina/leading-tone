-- What the learner has asked to work on, when they have asked. Null in both columns means
-- guided mode: the tutor picks from the learner model, which is the default.
alter table learner add column focus_concept_id text;
alter table learner add column focus_category text;
