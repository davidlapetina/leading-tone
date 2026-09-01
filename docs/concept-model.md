# The concept model

`concepts.json` holds 36 concepts of functional harmony and their prerequisites. It is the
source of truth; the `concept` and `concept_prerequisite` tables are a mirror, rebuilt at
every start by `ConceptSeeder` so that the graph can be joined against learner data in SQL.

## It is not a curriculum

```json
{
  "id": "secondary-dominant",
  "name": "Secondary dominants",
  "description": "Borrowing a dominant from another key to tonicise a chord: V7/V, V/vi, vii°7/ii.",
  "prerequisites": ["dominant-seventh", "roman-numeral", "key-signature"],
  "category": "HARMONY",
  "intrinsicDifficulty": 0.75
}
```

`prerequisites` states what secondary dominants *depend on*. It says nothing about when
they should be taught, in what order, or alongside what. The route through the graph is
produced by `TeachingPolicy` from what the learner has actually demonstrated, and is
different for every learner.

There is no `Course`, no `Chapter`, no `Lesson` and no `Exercise 1` anywhere in this
codebase. Adding one would replace the route through the graph with a fixed order, which is
the one thing the application is built not to do.

## The graph

```
note
 ├── interval
 │    ├── major-scale
 │    │    ├── key-signature ──────────────┐
 │    │    ├── scale-degree                │
 │    │    ├── minor-scale                 │
 │    │    └── mode                        │
 │    ├── triad                            │
 │    │    ├── chord-inversion             │
 │    │    ├── diatonic-triads             │
 │    │    └── seventh-chord               │
 │    └── (with major-scale) diatonic-triads
 │
 └── diatonic-triads + scale-degree
      └── roman-numeral
           ├── figured-bass
           └── harmonic-function
                ├── tonic-function
                ├── predominant-function
                └── dominant-function
                     └── dominant-seventh
                          ├── voice-leading
                          ├── cadence
                          └── secondary-dominant ──► modulation
```

## What the graph is used for

| `ConceptGraph` method | |
|---|---|
| `frontier(isKnown)` | everything teachable right now |
| `missingPrerequisites(id, isKnown)` | what is in the way of a target |
| `prerequisitesOf(id)` | what to anchor an explanation to |
| `dependentsOf(id)` | what this unlocks |
| `topologicalOrder()` | a stable ordering, and a cycle check |

Prerequisite cycles and references to concepts that do not exist both fail at load, so a
malformed graph cannot reach a learner.

## `intrinsicDifficulty`

0.10 for notes, 0.85 for modulation. It feeds two things: which frontier concept is chosen
first, and how the target difficulty of an exercise is set. It is a property of the
material, not of the learner.

## Extending it

Add an object to `concepts.json`, then teach the generator how to ask about it: add a case
to `ExerciseGenerator.generate` and a private method that computes both the question and
its answer from the theory engine. `ExerciseRoundTripTest` will immediately hold the new
generator to the same standard as the rest — every exercise it can produce, at every
difficulty, must accept its own correct answer.

Without a generator a concept still works, but falls back to a free-form explanation the
model has to judge, which is recorded at reduced confidence.
