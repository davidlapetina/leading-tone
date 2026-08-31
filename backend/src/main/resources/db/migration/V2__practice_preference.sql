-- A learner may say how they want to practise ("let me play these"), and that choice
-- should outlive the turn it was made in. Null means "let the tutor decide", which stays
-- the default: the tutor infers it from what the learner actually succeeds at.
alter table learner add column preferred_answer_mode text;
