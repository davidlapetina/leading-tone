# AI Music Teacher V1 Review

Reviewed 31 August 2026, against the running stack: PostgreSQL 17 in Docker, Quarkus
3.39.1 on Java 21, React 19 / Vite 8, Ollama with `qwen3:8b`.

## 1. Executive Summary

**Status: PASS WITH ISSUES.**

The repository implements a coherent V1 of the product described, and the adaptive loop
genuinely works: the learner model drives concept selection, evidence is weighted by how
knowledge was demonstrated, MIDI answers are judged deterministically, and every mastery
value is traceable to stored evidence. Scenarios A through G were executed against the
live stack and all behave correctly.

That verdict is *after* fixing two critical defects that made the product not work at all.
Both were found by inspecting the database rather than by reading the code, and neither
was caught by the existing test suite:

1. **The adaptive loop deadlocked.** `EXPLAIN` and `INTRODUCE` turns generated no
   exercise, so they produced no evidence, so mastery could never move, so the policy
   chose the same action again. A learner who got the first question wrong was stuck
   forever. The user's own transcript showed six consecutive `EXPLAIN` turns and the
   database confirmed zero evidence rows across all of them.
2. **The learner model silently split into three.** A racy find-or-create, triggered by
   React's double-mounted effect, created three learners in the same microsecond. The
   interface wrote evidence to one and read the mastery panel from another, so the panel
   stayed permanently empty.

Both are fixed, both now have regression tests, and both regression tests were verified to
fail against the original code before being accepted.

Eighteen further findings are listed in section 13. **Nineteen of the twenty are fixed.**
The single open item is local-model latency, which is inherent to running an 8B model on a
16 GB laptop rather than a defect in the code.

**One thing I could not verify: real MIDI hardware.** No keyboard is attached to this
machine. The evaluation logic is thoroughly tested and the browser-side service is unit
tested against synthetic messages, but the physical device path is unproven.

## 2. Build Verification

Every command below was executed. `✓` means it succeeded.

| Command | Result |
|---|---|
| `backend/mvnw -v` | ✓ Maven 3.9.16 (wrapper-provisioned), Java 21.0.10 Corretto |
| `./mvnw clean` | ✓ |
| `./mvnw clean verify` | ✓ **160 tests, 0 failures** |
| `./mvnw test` | ✓ 160 tests |
| `rm -rf node_modules && pnpm install` | ✓ clean install, 647ms |
| `pnpm typecheck` | ✓ no errors |
| `pnpm lint` | ✓ exit 0, **no warnings** |
| `pnpm test` | ✓ **24 tests, 4 files** |
| `pnpm build` | ✓ 322 kB main chunk, abcjs split into its own 509 kB chunk |
| `pnpm exec playwright install chromium` | ✓ |
| `pnpm exec playwright test` | ✓ **7 tests** against the live stack |
| `docker compose up -d postgres` | ✓ healthy on 5433 |
| Flyway `V1__initial_schema.sql` | ✓ applied from empty schema, 22 concepts seeded |
| `make db`, `make backend`, `make frontend` | ✓ |
| `curl /q/health`, `/q/openapi`, `/q/swagger-ui` | ✓ 200 |
| Model switch `OLLAMA_MODEL=mistral-small:latest` | ✓ no rebuild; `status` reported the change |
| Model switch to `qwen3:8b` | ✓ |

**Total: 191 tests, all passing.**

### "Works on my machine" problems

Checked specifically. No absolute paths, no undeclared tools, no manually-created database
state, no ignored configuration files, no missing packages. Two real ones were found and
fixed:

- Ports 5432 and 8080 are occupied on this machine by another project. The stack now
  defaults to 5433 and 8088, both overridable. Documented.
- Two `package.json` scripts invoked tools that were never installed (`eslint`,
  `playwright`). See MEDIUM-12.

The one environmental requirement that cannot be removed is Docker, which `make test`
needs for Dev Services. That is documented.

## 3. Architecture Assessment

Package root is `fr.lapetina.music` throughout; no stray packages.

```
theory      spelled pitch, intervals, scales, keys, chords, Roman numerals, ABC.
            No CDI, no persistence, no I/O. Depends on nothing.
concept     the prerequisite graph, loaded from concepts.json
learner     evidence, mastery, review schedule, misconceptions
exercise    generation and deterministic evaluation
midi        evaluation of what was played
tutor       policy, session, orchestrator
llm         the model behind an interface, its prompt, its tools, its failure handling
api         REST resources and read models
```

Dependency direction is inward and acyclic: `theory` knows nothing; `learner` knows
`concept`; `tutor` composes everything below it; `api` knows `tutor`. `llm` depends on
`tutor` and `learner` types but nothing depends on `llm` except the orchestrator, through
the `TutorModel` interface — the whole package can be deleted and the tutor still teaches.
`TemplateTutor` proves it, and the entire test suite runs that way.

Assessed against the specific risks named in the brief:

- **God services** — none. The largest class is `ExerciseGenerator` (~490 lines), which is
  a dispatch table of one small generator per concept; it is cohesive rather than tangled.
  `TutorOrchestrator` is 150 lines and delegates everything.
- **Business rules in controllers** — none. Resources validate, delegate, and map to read
  models. The one piece of logic that was in a resource (choosing between two response
  shapes) was removed as part of MEDIUM-10.
- **Application logic in prompts** — no. The prompt receives a decision that was already
  made and facts that were already computed. `TutorPrompts` is voice and constraints only.
- **Needless abstraction** — one interface with one production implementation
  (`TutorModel`), which earns its place: it is the seam that lets the application run with
  no model at all.
- **Entities leaking through the API** — no. `Views` and the tutor's own records are
  returned; JPA entities never cross the boundary.

There is no infrastructure package. Its would-be contents (config, persistence base types)
amount to nothing here, and inventing one to match a diagram would be worse than not
having it.

## 4. Critical Tutor/Learner Boundary

**The LLM cannot manipulate authoritative learner state.** Verified by inspection and by
test.

Searched for every pattern named in the brief. There is no `setMastery`, no
`markMastered`, no `changeReviewDate`, no `determineMidiCorrectness`, no path by which
model output reaches `LearnerConcept.mastery`, and no way for the model to persist an
`Evidence` row directly. `LearnerToolsTest` asserts reflectively that `LearnerTools`
exposes no method whose name starts with `set` or contains `mastery`.

The model's total write surface is one tool:

```
proposeEvidence(conceptId, evidenceType, result, reason)
```

It is a proposal, and it is fenced four ways:

| Fence | Where |
|---|---|
| Confidence capped at 0.6, so a model opinion moves mastery less than an observation | `EvidenceService.MODEL_JUDGED_CONFIDENCE` |
| One proposal per turn | `TurnScope` |
| Only about the concept currently being taught | `TurnScope` |
| Only `EXPLANATION`, `SELF_EXPLANATION` or `TEXT_RECALL` — it may report what someone *said*, never what they *played* | `LearnerTools.PROPOSABLE` |

The last three were added during this review (HIGH-7). Before that, a prompt injection —
and the learner's own words go into the prompt — could have called `proposeEvidence`
repeatedly for every concept and inflated the learner model. Four tests now cover the
refusals.

The flow matches the intended one exactly:

```
answer → deterministic evaluator → EvaluationOutcome → EvidenceObservation
       → EvidenceService (the only writer) → MasteryService → LearnerConcept
```

`EvidenceService` is the single writer of the learner model. `MasteryService` performs the
arithmetic and is a pure function of its inputs. Neither consults a model, and no model can
call either.

The model also cannot override deterministic theory. `TheoryTools` is read-only, and since
HIGH-6 the prompt is pre-loaded with engine-computed facts so the model has no occasion to
work theory out for itself.

## 5. Music Theory Engine

Implemented and strongly tested. 45 of the 160 backend tests are theory tests, written as
musical assertions rather than implementation details.

| | Status |
|---|---|
| Notes, pitch classes, accidentals (to double sharps/flats) | ✓ |
| Intervals with quality and number | ✓ |
| Major, natural/harmonic/melodic minor, five modes | ✓ |
| Keys, key signatures, relative and parallel | ✓ |
| Scale degrees with their names | ✓ |
| Triads: major, minor, diminished, augmented, sus2, sus4 | ✓ |
| Seventh chords: dominant, major, minor, minor-major, half-diminished, diminished, augmented | ✓ |
| Inversions with figured bass | ✓ |
| Roman numerals, harmonic function, applied chords, cadences | ✓ |
| ABC notation output | ✓ |

**Enharmonic spelling is modelled properly.** `PitchClass` is a letter plus an accidental,
never reduced to twelve values. `F#` and `Gb` are not equal; they satisfy
`isEnharmonicWith` and nothing more. Consequences that are tested: F♯ major keeps its E♯,
C♭ major spells F♭, C→F♯ is an augmented fourth while C→G♭ is a diminished fifth, and a
learner who answers `Gb Bb Db` for an F♯ major triad gets partial credit with an
explanation about spelling rather than a pass or a fail.

The specific cases the brief asked for, all verified:

| Input | Result |
|---|---|
| `C E G` | C major |
| `C Eb G` | C minor |
| `B D G` | G major, first inversion |
| `D G B` | G major, second inversion |
| `G B D F` | G7 |
| `E G C` (MIDI 52 55 60) | C major, **FIRST** inversion |
| `C E G` (MIDI 48 52 55) | C major, **ROOT_POSITION** |

Octave duplication and spread voicings are tested and ignored; note order is ignored for
chords and significant for scales.

Nothing incorrect was found in the engine during this review. Six bugs were found and
fixed when it was written (documented in the git-less history of that session): scale
octave wrapping, `Bb7` symbol parsing, minor-key MIDI spelling, and a half-cadence rule
that fired mid-phrase.

**Not implemented:** ninths and above, altered dominants, Neapolitan and augmented sixths,
non-chord tones, four-part voice-leading checking. None are V1 requirements. The `theory`
package has no lesson data in it and extends cleanly.

## 6. Adaptive Learning

This is the part the brief is most concerned with, so it gets the most scepticism.

### Learner model

Every field the brief asks for is present and persisted: concept, mastery, confidence,
learning state, successful and failed evidence counts, strong-evidence count, last
practice, next review, review interval, and misconceptions. States are
`UNKNOWN / INTRODUCED / LEARNING / PRACTICING / RELIABLE / MASTERED / NEEDS_REVIEW`, and
are always *derived*, never assigned.

### The mathematics

```java
// correct
mastery ← mastery + (1 − mastery) × weight × 0.30 × correctness
// wrong
mastery ← mastery × (1 − min(weight × 0.30 × 1.2, 0.9))

weight = evidenceType.weight × observerConfidence × (0.5 + 0.5 × difficulty)
confidence = 1 − 0.75^evidenceCount
```

Reviewed against each failure mode named in the brief:

| Risk | Finding |
|---|---|
| Values outside 0..1 | Impossible. Gains approach 1 asymptotically, losses are multiplicative, and the result is clamped. |
| Mastery exploding | No. Seven strong correct answers reach ≈0.72; ten reach ≈0.88. |
| One trivial answer causing mastery | No. A single `MIDI_CHORD` at difficulty 0.6 yields 0.192. |
| Failure erasing prior knowledge | No. A mistake multiplies by ≈0.77, so 0.72 becomes 0.55. |
| Rounding | Doubles throughout, no accumulation of rounding; persisted as `double precision`. |
| Inconsistent updates | Single code path; `EvidenceService` is the only writer. |

Evidence quality genuinely matters, and the guard is real: forty correct multiple-choice
answers drive mastery above 0.95 and still leave the concept at `RELIABLE`, because
`MASTERED` additionally requires confidence ≥ 0.80 **and** at least two correct answers
through a high-weight channel. There is a test asserting exactly that.

Mistakes cost more at high mastery than at low, which is correct: a mistake on something
you supposedly knew is more informative.

### Evidence

Twelve evidence types spanning recognition → recall → application → transfer →
explanation, weighted 0.30 to 1.00. The `evidence` table is append-only and stores the
type, result, difficulty, computed weight, observer confidence, and **mastery before and
after**, alongside the session, interaction and exercise that produced it.

*"Why does the application think I understand inversions?"* is answerable from
`GET /api/learner/evidence/chord-inversion` without consulting a model. Verified against
live data:

```
chord-inversion  MIDI_CHORD   PARTIALLY_CORRECT  w=0.60  0.090 → 0.172
chord-inversion  MIDI_CHORD   PARTIALLY_CORRECT  w=0.60  0.000 → 0.090
triad            TEXT_RECALL  CORRECT            w=0.52  0.576 → 0.642
```

### Misconceptions

Five named misconceptions, recorded only when a deterministic evaluator can identify one —
the tutor never invents them. Verified live: playing root position three times when an
inversion was asked produced three `PARTIALLY_CORRECT` results with
`plays-root-position-when-inversion-asked`, an occurrence count of 3, and the policy then
switched to `CORRECT_MISCONCEPTION` ahead of everything else.

### Review scheduling

Expanding intervals with ease scaled by mastery (1.5 → 2.5), capped at 180 days, reset to
one day on a mistake. Concepts below 0.45 are not scheduled at all, which is right: they
are still being learned rather than retained.

### Activity selection

Ten pedagogical actions. Every scenario the brief lists was executed against the live
policy:

| Scenario | Result |
|---|---|
| A. mastery unknown | `DIAGNOSE` on `note` ✓ |
| B. mastery low | `EXPLAIN` below 0.25, `PRACTICE` below 0.60 ✓ |
| C. mastery medium | `CHALLENGE` below 0.85 ✓ |
| D. mastery high | `TRANSFER` ✓ |
| E. misconception exists | `CORRECT_MISCONCEPTION` takes priority ✓ |
| F. due for review | `REVIEW`, weakest first ✓ |
| G. prerequisite weak | `REINFORCE` rather than introducing the advanced concept ✓ |

The policy is deterministic, contains no model call, and is unit tested by constructing a
learner model by hand — 14 tests, no database, no LLM.

**The concept graph is a graph, not a disguised curriculum.** 22 concepts with prerequisite
edges, cycle-detected at load (a cycle throws), and unknown references rejected. I searched
for hardcoded flows — `lesson1 → lesson2`, "if completed X start Y" — and found none, in
production or in fixtures. The route through the graph is computed from demonstrated
knowledge and is different for every learner. Verified: after demonstrating notes,
intervals, scales and triads, the tutor moved to `minor-scale` and did not re-teach
fundamentals; asked about secondary dominants with the groundwork in place, it took that
up instead of following its own plan.

## 7. MIDI

**Deterministic, and correctly separates the chord from its bass.**

Browser side is isolated in one class, `MidiService`, which is the only file that touches
the Web MIDI API. It handles note-on, note-off, and note-on at velocity zero (which many
keyboards send instead of note-off), tracks held keys, and submits a phrase once every key
is released plus a 550 ms gap, with a manual send button. Eight unit tests drive it with
synthetic messages including the velocity-zero case, repeated keys, and staccato phrases
that must not be split.

Backend evaluation ignores octave duplication and note order, uses the lowest sounding note
to determine inversion, and returns a diagnosis rather than a verdict:

| Asked | Played | Result |
|---|---|---|
| G major, first inversion | `B D G` | CORRECT |
| G major, first inversion | `B G D`, `B D G B`, spread voicings | CORRECT |
| G major, first inversion | `G B D` | **PARTIALLY_CORRECT** + `plays-root-position-when-inversion-asked` |
| G7 | `G B D` | INCORRECT + `omits-the-seventh-of-a-seventh-chord`, missing `F` |
| D major | `D F A` | INCORRECT + `confuses-chord-quality`, detected `Dm` |
| A harmonic minor scale | natural seventh | INCORRECT + `does-not-raise-the-leading-tone-in-minor` |

The feedback is the phrase a teacher would use: *"The right notes, but the bass should be
B and you played G at the bottom."* Thirteen MIDI tests plus two end-to-end API tests.

**Since this review:** an on-screen piano was added, so every keyboard question is
answerable without an instrument. It submits the same note numbers a device does, and an
end-to-end test now clicks the keys and asserts the answer is graded `CORRECT` with
`MIDI_NOTE` evidence — so the whole evaluation path is exercised through the interface,
not only in unit tests.

**Still unverified: physical hardware.** No MIDI instrument is attached to this machine, so
device discovery and real message timing remain untested outside synthetic input. Headless
Chromium refuses the Web MIDI permission, which the interface reports correctly as
"MIDI blocked". This is now a smaller gap than it was — everything downstream of the note
numbers is covered — but the device path itself is still unproven.

## 8. LLM / Ollama

Configuration is fully externalised: base URL, model, timeout, temperature, tool
enablement and failure cooldown are all environment variables with defaults, and none
require a rebuild. Verified live by switching between `llama3.2`, `mistral-small:latest`
and `qwen3:8b` with no code change; `GET /api/session/status` reports the model actually
in use.

Qwen3 is the default family. `qwen3:8b` is the shipped default rather than `qwen3:30b-a3b`
because this machine has 16 GB of RAM and a 30B model cannot run on it — `mistral-small`
at 14 GB already timed out. The preferred larger model is one variable away and documented
with a RAM table. This is a deliberate, stated deviation from the brief.

Provider portability is real: the tutoring domain depends on `TutorModel`, and swapping in
Mistral, Gemma, Llama, OpenAI or Anthropic means adding an extension and changing
configuration, not touching the policy, the learner model or the evaluators.

### The prompt

`TutorPrompts.SYSTEM` encodes every principle the brief asks for — no fixed curriculum,
adapt to demonstrated knowledge, question rather than lecture, connect to what is already
solid, reach for sound and keyboard before abstraction, "I understand" is not evidence,
ask for demonstration, never claim mastery, follow the supplied pedagogical action, stay
concise. It explicitly forbids the words "lesson", "chapter" and "module".

No contradictions found. No mutable application state is embedded beyond what the turn
needs: a summary of concepts in progress (capped at 12), the decision, the exercise, and
the computed facts. Conversation history is not stuffed into the prompt — it lives in a
bounded per-session memory window of 20 messages, with the durable record in the
`interaction` table.

### Failure handling

| Failure | Behaviour |
|---|---|
| Ollama not running | Template turn in 0.7s; `status` explains why. **Verified.** |
| Model times out | Same, and the model is then left alone for two minutes. **Verified** (mistral-small). |
| Model returns JSON instead of prose | Rejected by a markup guard; template used. Tested. |
| Model returns empty | Template used. |
| Model paraphrases the question away | The generated exercise is displayed in its own right. |
| Model states wrong theory | Prevented by pre-computing facts into the prompt. |

Tool calls are validated — unknown concepts, unknown enum values, out-of-scope concepts
and unwitnessable evidence types are all refused with an explanation rather than an
exception. No model output is executed; the only tool with a side effect is
`proposeEvidence`, fenced as described in section 4.

## 9. Frontend / UX

It reads as a teacher, not an LMS. There is no course catalogue, no progress dashboard, no
module list, no lesson navigation — those concepts do not exist in the codebase. The
layout is a conversation with a composer, and a secondary right-hand panel.

Mastery is shown as a twelve-cell bar with no number and no percentage, deliberately: a
visible score invites optimising the score. An end-to-end test asserts the panel contains
no `%`.

States handled, each verified:

| State | Behaviour |
|---|---|
| Loading | "Reading the learner model…" |
| Tutor thinking | `…` indicator, composer disabled |
| Backend unavailable | Red banner with the message, dismissible — not a blank page |
| Ollama unavailable | Header shows `deterministic`; teaching continues |
| MIDI supported, no device | "No keyboard" |
| MIDI blocked by permission | "MIDI blocked" with the reason as a tooltip |
| MIDI unsupported browser | "MIDI unavailable" |
| Keyboard awaited | Prompt showing held notes and a manual send button |
| Notation present | Rendered by abcjs, lazily imported |
| Invalid API response | Zod rejects it at the boundary and it surfaces as an error |

The verdict on an answer appears above the tutor's reply, colour-coded, carrying the
evaluator's own words. The generated exercise is displayed separately whenever the tutor's
prose does not already contain it — so a weak model cannot leave the learner guessing what
was asked.

**Notation:** abcjs 6.7.0, fed ABC generated by the backend, code-split so it is not in the
main bundle. Errors in generated ABC are contained to the score element rather than
breaking the turn. MusicXML would slot in as another renderer without touching the tutor,
since the backend emits notation as a string alongside the turn.

## 10. Persistence

Flyway owns the schema; Hibernate is set to `validate`, so a mapping that drifts from the
migration fails at boot rather than in production. Verified by applying the migration to an
empty database.

All eleven expected entities are present: `learner`, `concept`, `concept_prerequisite`,
`learner_concept`, `tutor_session`, `interaction`, `exercise`, `exercise_attempt`,
`evidence`, `misconception`, and review data carried on `learner_concept`.

- **Foreign keys** on every relationship, with deliberate cascade choices: deleting a
  learner cascades to everything about them; deleting a session sets `exercise.session_id`
  to null rather than destroying the exercise and its evidence.
- **Indexes** on `(learner_id, next_review_at)`, `(learner_id, concept_id, created_at)`,
  `(learner_id, created_at)`.
- **Unique constraints** on `(learner_id, concept_id)`, `(session_id, sequence)`,
  `(learner_id, concept_id, code)`.
- **Timestamps** are `timestamptz` throughout.
- **Evidence is never deleted** by learner-state changes. Mastery is updated in place;
  history accumulates. The only deletion path is deleting the learner.

Learner state is reconstructible from history: every evidence row carries mastery before
and after.

**Scenario G verified.** Taught three concepts and planted a misconception, captured the
full state, restarted the backend, and compared: identical. 20 evidence rows, 1 session,
the misconception with its occurrence count, and all four concept masteries to four
decimal places.

## 11. Tests

### Executed

| Suite | Command | Result |
|---|---|---|
| Backend unit + integration | `./mvnw clean verify` | **160 passed** |
| Frontend unit | `pnpm test` | **24 passed** |
| End-to-end | `pnpm exec playwright test` | **7 passed** |

Backend integration tests use Quarkus Dev Services, which is Testcontainers underneath —
a real PostgreSQL 17 per run. RestAssured is used for the API tests. No test requires a
language model; the test profile disables it, which is only possible because the pedagogy
does not depend on one.

### Failures found

Seven, during this review. Five were defects in the product (sections 13: CRITICAL-1,
CRITICAL-2, HIGH-4, HIGH-5, HIGH-6). Two were faults in tests I had just written, where the
product was right and my expectation was wrong — a `cadence` question genuinely *is* out of
reach when `triad` is at 0.2, and `/api/session/next-action` is a stateless preview that
cannot show a focus.

### Tests added

31 new tests during the review:

- `AnswerNormalizerTest` — help requests, "I don't know", questions vs hesitant answers
- `FocusDetectorTest` — including the exact question from the user's transcript
- `LearnerIdentityTest` — 16 concurrent calls must yield one learner
- `LearnerToolsTest` — the four fences on the model's only write tool
- `TheoryBriefingTest` — the computed facts, including Cmaj7 vs C7
- `FailureWindowTest` — the model-failure cooldown
- `RoutingTutorModelTest` — markup rejection
- `TeachingPolicyTest` — every action must leave something to answer; question routing
- `TutorFlowTest` — the stall regression, help handling, question handling, SSE
- `e2e/tutor.spec.ts` — 7 browser scenarios

**The two most important regression tests were verified to fail against the original
code before being accepted:**

```
TutorFlowTest.keepsProducingEvidenceAfterWrongAnswers
  → turn 0 (EXPLAIN) left the learner with nothing to answer
TeachingPolicyTest.neverProducesATurnThatCannotGenerateEvidence
  → EXPLAIN produced a turn with nothing to answer
```

### Test quality

The theory engine is tested as musical fact, not implementation: `assertEquals("G/B", …)`,
F♯ major keeps its E♯, a first-inversion seventh is figured 6/5. The policy is tested by
constructing a learner model and asserting on the decision, with no database and no model.

The strongest test in the suite is `ExerciseRoundTripTest`: for all 22 concepts, at three
difficulties, in both channels, 12 rounds each, it generates an exercise and answers it
with the generator's own answer key. A question that drifts from its answer fails the
build. That is roughly 1,600 generated exercises per run.

No test compares LLM prose. The LLM is tested structurally: markup rejection, tool
refusals, fallback behaviour.

### Coverage gaps

- **Physical MIDI hardware** — synthetic messages only. The most significant gap.
- **Concurrent multi-session behaviour** — single-user V1, not exercised.
- **Long-run pedagogical quality** — no test asserts that a 50-turn session teaches well;
  that is a judgement, not an assertion.
- **abcjs rendering output** — the `Score` component is mocked in unit tests; ABC
  *generation* is tested, rendering is not.

## 12. Installation

**Yes, a new developer can clone and run this from the README alone.** Rewritten during
this review to close real gaps (MEDIUM-17).

It now covers, for macOS and Debian/Ubuntu: Java 21, Node, pnpm, Docker, Ollama, the model
pull with a RAM-to-model table, database startup, backend startup, frontend startup,
every environment variable with its default, the test commands including the Playwright
browser download, the production build, the MIDI browser requirement (Chrome or Edge, over
localhost or https), the non-default ports and why, and a table of what happens when each
dependency is missing.

No installation knowledge is left implicit. No secrets are required and none are
committed; the only credential in the repository is the local Docker Postgres password,
which is not a secret.

## 13. Findings

### CRITICAL

---
**CRITICAL-1 — The adaptive loop deadlocks after a wrong answer**

- **Severity:** CRITICAL
- **Component:** Teaching policy / orchestrator
- **Files:** `tutor/TeachingAction.java`, `tutor/TeachingDecision.java`, `tutor/TutorOrchestrator.java`
- **Problem:** `expectsAnswer()` was false for `INTRODUCE` and `EXPLAIN`, so those turns
  generated no exercise. No exercise means no evaluation, no evidence, and no mastery
  change. The policy selects on mastery, so it chose `EXPLAIN` again — forever. The
  database showed 6 `EXPLAIN` turns, 0 with an exercise, mastery pinned at 0.000.
- **Impact:** The product does not work. Any learner who gets the first question wrong can
  never make progress again. This is the defect behind the user's reported transcript.
- **Fix:** Every teaching action now ends in something answerable; the explanatory ones
  say something first and then ask, which is what the prompt already instructed. Removed
  the two unreachable enum values while there.
- **Status: FIXED** — regression tests at both the policy and HTTP level, both verified to
  fail against the original code.

---
**CRITICAL-2 — The learner model silently splits across several learners**

- **Severity:** CRITICAL
- **Component:** Learner service
- **Files:** `learner/Learner.java`, `learner/LearnerService.java`, `learner/LearnerSeeder.java`
- **Problem:** `current()` was find-first-or-create with no unique identity. React's
  `StrictMode` double-mounts effects, firing two concurrent `POST /api/session` calls;
  both found no learner and both created one. Three learners were created in the same
  microsecond. Evidence was written to the newest while `/api/learner` returned the oldest.
- **Impact:** The mastery panel never updated, so from the outside the learner model
  appeared not to work at all. Evidence was scattered across learner records.
- **Fix:** The learner has a fixed primary key, is seeded at startup, and the request path
  only reads. `DELETE /api/learner` recreates it rather than leaving none.
- **Status: FIXED** — `LearnerIdentityTest` runs 16 concurrent `current()` calls and
  asserts one learner.

### HIGH

---
**HIGH-3 — The tutor ignores what the learner asks**

- **Severity:** HIGH
- **Component:** Teaching policy
- **Files:** `tutor/FocusDetector.java`, `tutor/LearnerFocus.java`, `tutor/TeachingPolicy.java`
- **Problem:** The policy read only the learner model. A learner asking "what is a C major
  add 7 chord" got a question about note names, three turns running.
- **Impact:** The product does not behave like a teacher. This is the other half of the
  reported transcript.
- **Fix:** Deterministic concept detection from the learner's words, and an
  `ANSWER_QUESTION` branch ranked above review and above the tutor's own plan. When the
  question is out of reach, the policy reinforces the most foundational missing
  prerequisite *while recording what was asked*, so the reply acknowledges it. The
  template tutor acknowledges it too, not just the model.
- **Status: FIXED** — 5 detector tests, 2 policy tests, 1 API test, 1 e2e test.

---
**HIGH-4 — Questions and requests for help are graded as wrong answers**

- **Severity:** HIGH
- **Component:** Orchestrator / answer evaluation
- **Files:** `exercise/AnswerNormalizer.java`, `exercise/ExerciseEvaluator.java`, `tutor/TutorOrchestrator.java`
- **Problem:** Any text typed while an exercise was open went to the evaluator. Typing
  "explain" or "what is a secondary dominant?" was marked INCORRECT and recorded as
  evidence that the learner does not know the concept. Fixing CRITICAL-1 made this worse,
  because every turn now carries an exercise.
- **Impact:** Asking for help actively corrupts the learner model — the opposite of what
  asking for help means.
- **Fix:** Help requests keep the same question open and are not graded. Questions naming
  another concept route to the policy instead. "I don't know" is a skip, worth a quarter
  of a mistake. The question test is deliberately strict: an opening interrogative *and* a
  recognised concept, so "predominant?" is still an answer.
- **Status: FIXED** — 5 normalizer tests, 2 API tests, 1 e2e test.

---
**HIGH-5 — The SSE endpoint was completely broken**

- **Severity:** HIGH
- **Component:** API
- **Files:** `api/TutorResource.java`
- **Problem:** Blocking database and model work inside a `Multi` emitter, which runs on
  the event loop. Every request failed with `BlockingOperationNotAllowedException`. It
  shipped untested because the frontend does not use it. `@Blocking` alone does not fix it:
  the method body and the emitter body run at different times, on different threads, and
  both needed handling.
- **Impact:** A documented endpoint that returned an error on every call.
- **Fix:** `@Blocking` for the method body plus `runSubscriptionOn(worker pool)` for the
  emitter, with a comment explaining why both are load-bearing.
- **Status: FIXED** — tested with a real streaming HTTP client.

---
**HIGH-6 — The model states incorrect music theory in prose**

- **Severity:** HIGH
- **Component:** LLM integration
- **Files:** `llm/TheoryBriefing.java`, `llm/TutorPromptBuilder.java`
- **Problem:** Asked "what is a C major add 7 chord", `qwen3:8b` answered "C, E, G, and
  Bb" — that is C7, a different chord. Tools were enabled and it did not call them,
  because a confident model does not look things up.
- **Impact:** The tutor teaches something false. Graded content was never affected, but
  prose reaches the learner directly.
- **Fix:** Facts for the concept in hand are computed by the theory engine and placed in
  the prompt, so there is no occasion to invent any. Everything in the briefing is
  generated by the same engine that marks answers, so the two cannot drift.
- **Status: FIXED** — verified live: the same question now returns "C E G B — the same as
  Cmaj7". 7 briefing tests.

---
**HIGH-7 — `proposeEvidence` was unbounded**

- **Severity:** HIGH
- **Component:** LLM tools
- **Files:** `llm/tools/TurnScope.java`, `llm/tools/LearnerTools.java`
- **Problem:** The model could propose evidence for any concept, of any type including
  `MIDI_CHORD`, any number of times per turn. The learner's own words reach the prompt.
- **Impact:** A prompt injection could inflate the learner model — the one thing the
  architecture exists to prevent.
- **Fix:** One proposal per turn, only about the concept being taught, only of a kind the
  model could have witnessed. Refusals return an explanation rather than an error.
- **Status: FIXED** — 6 tests, including a reflective assertion that no setter is exposed.

---
**HIGH-8 — A failed model call costs four minutes per turn**

- **Severity:** HIGH
- **Component:** LLM integration
- **Files:** `llm/FailureWindow.java`, `llm/RoutingTutorModel.java`, `application.properties`
- **Problem:** Measured: 4 minutes 9 seconds for one turn with an oversized model. The
  120s timeout is paid, then the client retries once. The extension exposes no retry
  configuration.
- **Impact:** Unusable whenever the model is slow or absent, which is exactly when the
  fallback is supposed to help.
- **Fix:** Timeout reduced to 60s, and a failure opens a two-minute cooldown during which
  turns come from templates immediately.
- **Status: FIXED** — verified live: 0.7s with Ollama unreachable, down from 249s. 4 tests.

### MEDIUM

---
**MEDIUM-9 — Default model was not from the required family**

- **Files:** `application.properties` · **Problem:** default was `llama3.2`, which
  paraphrases questions badly and mishandles tool schemas. **Fix:** `qwen3:8b`, with
  `qwen3:30b-a3b` documented as preferred where RAM allows and a RAM-to-model table.
  Deliberate deviation from "30B default": this machine has 16 GB. **Status: FIXED**

**MEDIUM-10 — `/answer` returned two different response shapes**

- **Files:** `api/ExerciseResource.java` · **Problem:** returned `TutorTurn` for
  session-bound exercises and `AttemptResult` otherwise, typed as `Object`; the frontend
  schema would reject the second. **Fix:** answering is always a teaching interaction and
  always returns a turn; a session-less exercise attaches to the current session.
  **Status: FIXED**

**MEDIUM-11 — Learning state went stale**

- **Files:** `learner/LearnerService.java`, `learner/ConceptMastery.java` · **Problem:**
  state was read from the column, which is only written when evidence arrives, so
  `NEEDS_REVIEW` never surfaced — a concept becomes due through time passing. **Fix:**
  derived at read time. **Status: FIXED**

**MEDIUM-12 — Two package scripts invoked tools that were never installed**

- **Files:** `frontend/package.json` · **Problem:** `lint` ran `eslint` (oxlint is what is
  installed) and `test:e2e` ran `playwright` (absent, no config, no specs). **Fix:** lint
  uses oxlint; Playwright properly added. Lint then immediately found two real React
  issues, both fixed. **Status: FIXED**

**MEDIUM-13 — No end-to-end tests despite being a stated requirement**

- **Files:** `frontend/playwright.config.ts`, `frontend/e2e/tutor.spec.ts` · **Fix:** 7
  browser scenarios against the real stack with the model disabled for determinism.
  Vitest was also collecting the Playwright specs and failing; scoped to `src/`.
  **Status: FIXED**

**MEDIUM-14 — No size limit on free text reaching the prompt**

- **Files:** `api/dto/Requests.java` · **Fix:** 2000 characters on messages and answers,
  128 notes on a performance. **Status: FIXED**

**MEDIUM-15 — Unreachable enum values and dead methods**

- **Files:** `tutor/TeachingAction.java`, `learner/MasteryService.java`,
  `learner/LearnerPreferences.java`, `learner/MisconceptionService.java`, `theory/Scale.java`
  · **Problem:** `QUESTION` and `CONNECT_CONCEPTS` could never be emitted; four methods
  had no callers. **Fix:** removed. Extension points with a purpose were kept.
  **Status: FIXED**

**MEDIUM-16 — Application DEBUG logging enabled in every profile**

- **Files:** `application.properties` · **Fix:** scoped to dev. Conversations were never
  logged. **Status: FIXED**

**MEDIUM-17 — README incomplete for a new developer**

- **Files:** `README.md`, `Makefile` · **Problem:** no Linux instructions, no environment
  variable reference, no model installation, no MIDI browser requirement, no e2e
  instructions, no failure-mode documentation. **Fix:** rewritten; `make backend-offline`
  and `make test-e2e` added. **Status: FIXED**

### LOW

**LOW-18 — Lint warning on the MIDI hook** · `midi/useMidi.ts` built its service in a
`useMemo` whose closure read `handler.current`, which the React rules flag as reading a
ref during render. The behaviour was correct but the shape was not. **Fix:** the service
is now built inside an effect and held in a ref, so nothing is read during render at all.
Suppressing the rule was tried first and rejected — restructuring is better than
silencing. `pnpm lint` is now clean. **Status: FIXED**

**LOW-19 — `SessionService.nextSequence` counted rows** · Counting repeats a sequence
number as soon as any interaction is removed, and the unique constraint on
`(session, sequence)` would then reject the next turn. Unreachable in a single-user V1,
but wrong regardless. **Fix:** one past the highest existing sequence.
**Status: FIXED**

**LOW-20 — Local model latency** · 105s for the first turn with `qwen3:8b` on 16 GB
(model load), 8–58s after. Inherent to local inference on this hardware, not a defect in
the code: the deterministic tutor answers in milliseconds and `MUSIC_LLM_ENABLED=false`
removes the wait entirely. The interface shows the exercise immediately, and the SSE
endpoint exists to show the decision before the prose arrives, but the V1 interface does
not consume it. **Status: OPEN — the only one. See section 16, item 1.**

## 14. V1 Requirements Matrix

| Requirement | Implemented | Tested | Working | Notes |
|---|---|---|---|---|
| Adaptive tutoring | Yes | Yes | Yes | Scenarios A–G executed live |
| No fixed lessons | Yes | Yes | Yes | No lesson/chapter/course type exists; prompt forbids the words |
| Learner model | Yes | Yes | Yes | Mastery, confidence, states, counts, review, misconceptions |
| Evidence | Yes | Yes | Yes | 12 types, weighted 0.30–1.00, append-only |
| Mastery | Yes | Yes | Yes | Asymptotic gains, multiplicative losses, guarded MASTERED |
| Misconceptions | Yes | Yes | Yes | 5 named, deterministic; verified to change teaching |
| Concept graph | Yes | Yes | Yes | 22 concepts, cycle-detected |
| Prerequisites | Yes | Yes | Yes | Frontier, missing-prerequisite walk, 0.60 floor to build on |
| Teaching policy | Yes | Yes | Yes | 10 actions, deterministic, 14 unit tests |
| Theory engine | Yes | Yes | Yes | 45 tests |
| Enharmonic spelling | Yes | Yes | Yes | Letter + accidental, never collapsed |
| Chord analysis | Yes | Yes | Yes | From spelled notes and from raw MIDI |
| Inversions | Yes | Yes | Yes | Including figured bass |
| Dynamic exercises | Yes | Yes | Yes | No bank; ~1,600 generated per test run |
| MIDI | Yes | Partly | Yes* | *Synthetic input only — no hardware available |
| MIDI deterministic evaluation | Yes | Yes | Yes | Separates chord from bass, names the mistake |
| abcjs notation | Yes | Partly | Yes | Generation tested; rendering mocked in unit tests |
| Ollama | Yes | Yes | Yes | Verified with 3 models |
| Configurable model | Yes | Yes | Yes | Env var, no rebuild; reported by `/status` |
| Qwen3 support | Yes | Yes | Yes | `qwen3:8b` default; 30B documented, needs more RAM |
| PostgreSQL persistence | Yes | Yes | Yes | Verified across a restart |
| Flyway | Yes | Yes | Yes | Applied from empty; Hibernate validates against it |
| Frontend | Yes | Yes | Yes | 24 unit tests |
| Backend | Yes | Yes | Yes | 160 tests |
| Installation documentation | Yes | n/a | Yes | macOS + Linux, all variables, all failure modes |
| Backend tests | Yes | — | Yes | JUnit 5, RestAssured, Dev Services/Testcontainers |
| Frontend tests | Yes | — | Yes | Vitest + Testing Library |
| E2E tests | Yes | — | Yes | Playwright, 7 scenarios, real stack |

## 15. Remaining Work Before V1

Genuinely required. Nothing optional is listed here.

1. **Verify with a real MIDI keyboard.** Every other claim in this review was executed;
   this one could not be, because no instrument is attached to this machine. Plug a piano
   into Chrome, play a first-inversion triad, and confirm the round trip. The on-screen
   piano added since means this no longer blocks *using* the application — it only leaves
   the physical device path unproven.

That is the only item, and it is a verification rather than a change. Every finding in
section 13 except LOW-20 has been fixed and re-verified; LOW-20 is a property of running
an 8B model on a 16 GB laptop, not something in the repository.

## 16. Recommended V2 Work

Kept separate from V1 blockers.

1. **Consume the SSE endpoint in the interface.** It works and is tested, but the V1 UI
   uses plain POST. With a local model taking 30–60 seconds per turn, showing the
   pedagogical decision ("staying with dominant sevenths…") the moment it is made, before
   the prose arrives, would change how the application feels more than any other single
   change. This is the highest-value item on the list.
2. **Token streaming.** The endpoint currently streams the composed turn in stages.
   Streaming the model's tokens would remove the wait entirely.
3. **Aural evidence.** `AURAL_RECOGNITION` is defined and weighted but nothing generates
   it. abcjs can already synthesise audio, so "which of these two chords did you hear" is
   close to free.
4. **Migrate mastery to Bayesian Knowledge Tracing.** The deterministic model is
   well-behaved and explainable, which is right for V1. The evidence table already stores
   everything BKT would need, so the migration is additive and can be validated against
   the recorded history.
5. **Widen the theory engine** — ninths, altered dominants, Neapolitan and augmented
   sixths, non-chord tones, and four-part voice-leading checking with real parallel-fifth
   detection.
6. **Richer misconception detection.** Five is a good start; the interesting ones
   (confusing relative and parallel minor, spelling by semitone rather than by letter) are
   detectable with the engine that already exists.
7. **Second-guess the frontier ordering.** The policy introduces the least difficult
   available concept, which is breadth-first through easy material. A learner who wants
   depth must ask for it — which now works, but a "keep going deeper" preference would be
   better than requiring them to steer every time.

---

## V1 READY: YES

With one caveat: physical MIDI hardware is unverified.

1. **The adaptive loop demonstrably works, end to end.** Scenarios A–G were executed
   against the live stack. A beginner is diagnosed rather than lectured; a learner who
   demonstrates scales and triads is not walked through fundamentals; a repeated
   misconception takes priority over everything else; a concept out of reach sends the
   tutor back to the groundwork. This is not a chatbot with a syllabus.

2. **The critical boundary holds under inspection and under attack.** The LLM cannot
   write mastery, cannot mark an answer, cannot choose the concept or the action, and
   cannot vouch for anything it did not witness. `MUSIC_LLM_ENABLED=false` runs the whole
   tutor with identical pedagogy, which is the strongest possible evidence that the model
   is not where the teaching lives.

3. **The learner model is auditable rather than opaque.** "Why does it think I understand
   inversions?" is answered by rows in a table, each carrying what the evidence was worth
   and what it did to mastery — verified surviving a backend restart, byte for byte.

4. **The deterministic core is properly tested.** 191 tests, none needing a model. The
   theory engine is tested as musical fact; the round-trip test holds ~1,600 generated
   exercises to their own answer keys every run; the two critical regressions were
   verified to fail against the broken code before being accepted.

5. **It fails honestly.** Model absent, model slow, model returning JSON, model
   paraphrasing the question away, backend down, no keyboard — each degrades to something
   usable and says which. None of them touch the learner model.

The two critical defects found in this review are the reason for "PASS WITH ISSUES" rather
than a clean pass: neither was caught by the test suite that existed, and both meant the
product did not work. They are fixed, tested, and the tests were proven to have teeth.
