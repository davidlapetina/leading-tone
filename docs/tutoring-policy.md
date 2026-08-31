# The tutoring policy

`TeachingPolicy.next(snapshot)` is the whole of "what happens next". It reads the learner
model and returns a `TeachingDecision`. It is deterministic, it has no dependency on a
language model, and it is unit-tested on its own.

## The order of precedence

```
1. Nothing known, and nothing asked?        → DIAGNOSE
2. A misconception seen twice or more?      → CORRECT_MISCONCEPTION
3. Did the learner ask about something?     → ANSWER_QUESTION  (or REINFORCE, see below)
4. Something due for review?                → REVIEW           (the weakest one first)
5. Otherwise                                → advance
```

A repeated misconception is corrected before anything else because a wrong belief that
keeps firing will corrupt everything built on top of it. A single slip is noted and not
acted on.

What the learner asked comes next, ahead of the tutor's own plan. Ignoring a direct
question is how a tutor stops being one — and it is what an earlier version of this policy
did, answering "what is a C major add 7 chord" with a question about note names.

## Every turn ends in something to do

There is no action that only talks. `INTRODUCE`, `EXPLAIN` and `CORRECT_MISCONCEPTION`
say something first and then put a question, but they still put one.

This is not a style preference. A turn with nothing to answer produces no evidence, a
concept with no evidence cannot move, and a policy that selects on mastery will then
choose the same action again. An earlier version deadlocked exactly there: six consecutive
`EXPLAIN` turns, zero evidence, mastery pinned at 0.00. `TeachingDecision.expectsAnswer()`
now depends only on whether there is a channel to answer on, and two tests hold it there.

## Answers, questions and asking for help

Not everything typed into the box is an answer, and grading it as one is worse than
useless — it records "does not know this" about someone who was asking for help.

| What was typed | What happens |
|---|---|
| An answer | Evaluated deterministically; evidence recorded |
| "explain", "why?", "I don't understand" | Not graded. The same question stays open and the tutor says more |
| "I don't know" | Recorded as a skip, which costs a quarter of a mistake |
| A question naming another concept | Not graded. The policy takes that concept up instead |
| "predominant?" | Still an answer. A hesitant answer is not a question |

The distinction is made in `AnswerNormalizer` and is deliberately strict: a question needs
an opening interrogative *and* a recognised concept before it stops being an answer.

## Advancing

The **frontier** is every concept whose prerequisites are all known but which is not yet
known itself. Within it:

- something already started comes before something new — you finish what you began;
- otherwise the least intrinsically difficult concept comes first.

Before anything genuinely new is introduced, every direct prerequisite must be at
mastery **0.60** or better. The bar to *count* as known is 0.45; the bar to *build on* is
higher. A prerequisite in between produces `REINFORCE` — go back one step — rather than
stacking a new idea on ground that will not hold.

When the frontier is empty, everything within reach is held, and the policy consolidates
the weakest of it instead.

## Choosing the action

```
state is UNKNOWN   → INTRODUCE
mastery < 0.25     → EXPLAIN      met it, has not landed
mastery < 0.60     → PRACTICE     needs repetition here
mastery < 0.85     → CHALLENGE    solid enough to push
otherwise          → TRANSFER     the test is whether it moves to new ground
```

When the learner asks about something out of reach, the policy does not refuse and does
not plough ahead. It returns `REINFORCE` on the most foundational missing prerequisite,
while recording what was asked, so the answer can begin by acknowledging the question and
saying plainly what has to come first.

## Difficulty

```
difficulty = 0.60 × (mastery + 0.15) + 0.40 × concept.intrinsicDifficulty
```

clamped to 0.15 … 0.95. Aim slightly above where the learner is: too easy produces no
evidence, too hard produces only frustration. Difficulty then feeds back into the weight
the resulting evidence carries, so a hard question that is answered right is worth more.

## Choosing the channel

Playable concepts — fundamentals, scales, chords — are asked at the keyboard when
`keyboardPreference ≥ 0.5`. Since that preference moves with what the learner actually
succeeds at, the tutor gradually finds the channel that works for this person.

## What the policy hands over

```java
record TeachingDecision(
    TeachingAction action,
    String conceptId, String conceptName,
    List<String> supportingConcepts,   // mastered neighbours to anchor the explanation to
    double difficulty,
    AnswerMode preferredAnswerMode,
    String rationale,                  // recorded, so the choice stays auditable
    MisconceptionView misconception,
    String learnerAskedAbout)          // what the learner raised, when they raised something
```

`rationale` is written into the prompt and returned by the API, so it is always possible
to ask the tutor why it did what it did and get an answer that is not a rationalisation.

## Testing it

`TeachingPolicyTest` builds a learner model by hand and asserts on the decision. No
database, no language model. Examples:

- everything at 0.9 except `modulation` unseen → `INTRODUCE modulation`
- the same, but `seventh-chord` at 0.50 and `dominant-seventh` unseen →
  `REINFORCE seventh-chord`
- a misconception seen three times → `CORRECT_MISCONCEPTION`, whatever else was due
- asked about cadences with the groundwork in place → `ANSWER_QUESTION cadence`
- asked about secondary dominants knowing nothing → `REINFORCE`, still recording the
  question
- every action it can emit leaves something to answer
