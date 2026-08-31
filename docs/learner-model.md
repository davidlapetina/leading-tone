# The learner model

This is the part of the application worth protecting. The conversation is replaceable;
the record of what you know is not.

## Evidence, not answers

Every interaction produces an `Evidence` row, and every row carries what it was worth:

```
weight = evidenceType.weight × observerConfidence × (0.5 + 0.5 × difficulty)
```

Difficulty is folded in on a half-scale so an easy question still counts for something.

### What each kind of demonstration is worth

| Evidence type | Weight | |
|---|---|---|
| `HINTED_RECALL` | 0.30 | told most of the answer |
| `MULTIPLE_CHOICE` | 0.40 | could have been a guess |
| `TEXT_RECALL` | 0.70 | said it |
| `AURAL_RECOGNITION` | 0.70 | heard it |
| `MIDI_NOTE` | 0.70 | played one note |
| `MIDI_INTERVAL` | 0.75 | played two |
| `MIDI_CHORD` / `MIDI_SCALE` | 0.80 | played it |
| `MIDI_PROGRESSION` | 0.85 | played it in context |
| `SELF_EXPLANATION` | 0.85 | said why |
| `EXPLANATION` | 0.90 | taught it back |
| `TRANSFER_PROBLEM` | 1.00 | used it somewhere new |

The ordering is the point:

```
recognition  →  recall  →  application  →  transfer  →  explanation
```

Answering *"what is V7 in D?"* is weaker evidence than playing it, and playing it is
weaker evidence than explaining why the C natural has to fall to B.

## How mastery moves

```java
// right
mastery ← mastery + (1 − mastery) × weight × 0.30 × correctness

// wrong
mastery ← mastery × (1 − weight × 0.30 × 1.2)
```

Gains are asymptotic, so no single answer can carry a concept to mastery. Losses are
multiplicative, so a mistake costs more when mastery was high — which is right, because a
mistake on something you supposedly knew is more informative than one on something you are
still learning. Skipping costs a quarter of a mistake.

`confidence` is separate from `mastery`, and measures how much evidence the estimate
rests on rather than how well the concept is held:

```java
confidence = 1 − 0.75^evidenceCount
```

## States, and the guard on MASTERED

```
UNKNOWN → INTRODUCED → LEARNING → PRACTICING → RELIABLE → MASTERED
                                        ↑            │
                                        └─ NEEDS_REVIEW ─┘
```

States are always derived, never assigned. `MASTERED` additionally requires

- `confidence ≥ 0.80`, and
- at least **two** correct answers through a high-weight channel (`weight ≥ 0.80`) —
  playing it, explaining it, or transferring it.

So forty correct multiple-choice answers will drive mastery above 0.95 and still leave the
concept at `RELIABLE`. There is a test that asserts exactly that, because a tutor that can
be convinced by guessing is not measuring anything.

## Review

Expanding intervals, with the gap scaled by how well the concept is held:

```
ease     = 1.5 + mastery            (1.5 … 2.5)
interval = max(1, round(previous × ease))     capped at 180 days
```

A mistake sends the concept back to tomorrow. Concepts below mastery 0.45 are not
scheduled at all — they are still being learned rather than retained, and the policy will
pick them up on merit.

## Misconceptions

A misconception is recorded only when a deterministic evaluator can name one:

| Code | |
|---|---|
| `plays-root-position-when-inversion-asked` | the chord is right, the bass is not |
| `omits-the-seventh-of-a-seventh-chord` | played the triad underneath |
| `confuses-chord-quality` | right root, wrong third or fifth |
| `does-not-raise-the-leading-tone-in-minor` | natural seventh in harmonic minor |
| `wrong-note-in-scale` | first divergence is reported by degree |

The tutor never invents one. Seen twice, a misconception interrupts whatever else was
planned; answered correctly, it is resolved.

## Learning-style preferences

Seven values in `LearnerPreferences`, all starting at 0.5 and nudged 4% towards what
actually works. Succeed at the keyboard and `keyboardPreference` rises, which makes the
policy ask you to play rather than to write. Nothing here is asked about in a form; it is
all inferred from behaviour.
