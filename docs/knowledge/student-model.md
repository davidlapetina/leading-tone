# The student model, and how teaching uses it

The learner model already existed; what the knowledge layer adds is that it now visibly
changes what gets said.

## What is stored

Mastery per concept, derived from evidence, in `learner_concept`. Evidence itself is
append-only in `evidence`, so any mastery figure can be traced back to the answers that
produced it. Full detail in [../learner-model.md](../learner-model.md).

Mastery moves on evidence: a correct answer, a played phrase, an explanation. It does **not**
move because a concept was explained, appeared in retrieved context, or was mentioned by the
model. There is no path from "the tutor talked about it" to "the learner knows it", and
adding retrieval did not create one — retrieval adds no write path at all.

## How it changes the teaching

Two ways, both in the prompt the model is given.

The learner state block lists what is in progress, what is solid, what is due and what
mistakes have been seen. Then, for the concept being taught, its prerequisites are split:

```
Already solid, so build on these rather than explaining them: triads, Roman numerals,
dominant function.
Not yet solid, so do not assume them: key signatures.
```

That is the mechanism behind the intended behaviour: asked "what is V/V?" by somebody solid
on dominant function, the answer starts from the dominant rather than from what a scale is.
Asked by somebody who is not, it does not assume.

The threshold is mastery ≥ 0.45 — "known" — and the teaching policy applies a stricter bar
of 0.60 before it will build on something, because known and solid enough to build on are
not the same thing.

## What it is not

The model is not told to assess. It cannot set mastery, and the one thing it may propose —
evidence, through a tool — is capped at 0.6 confidence, limited to three evidence types, and
restricted to the concept currently being taught, one proposal per turn.
