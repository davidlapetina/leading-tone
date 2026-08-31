# V2 Plan

V1 answered its question: the tutor keeps an accurate model of what you know and decides
what to teach next, and the language model is nowhere near that decision. 191 tests, the
loop verified end to end, one open finding.

V2 is not about proving that again. It is about three gaps that V1 left, in the order they
block daily use.

---

## Measured first

The plan below was re-cut after benchmarking, because two of its assumptions were wrong.

### Smaller models are not faster here

Same prompt, warm, on a 16 GB laptop:

| Model | Time | What it said |
|---|---|---|
| `qwen3:8b`, reasoning off | **1.7s** | "Let me hear you play an F minor triad. That should be F, Ab, and C. Play it slowly and listen carefully to the sound." |
| `qwen3:8b`, reasoning on | 7.0s | "Play an F minor triad." |
| `llama3.2` (3B) | 0.4s | "What's the F minor triad, and how is it built from thirds?" — asks a written question when the exercise wanted it played |
| `gemma3:4b` | 0.3s | "Play an F minor triad." — parrots the exercise, no teaching, and no tool support |
| `qwen3:4b`, reasoning on | 48.4s | 5,613 characters of deliberation, then one bland sentence |
| `qwen3:4b`, reasoning off | 64.4s | ignored the switch and leaked its reasoning into the reply |

`qwen3:4b` is roughly **thirty times slower** than `qwen3:8b`. The genuinely fast small
models are fast because they say almost nothing. `qwen3:8b` with reasoning off is both the
quickest useful option and the best teacher of the six.

### The latency was configuration, not model size

Reasoning mode was on by default, the context window was the stock 4096, and a
twenty-message memory plus tool schemas overflowed it — so every later turn was slower than
the one before. Three settings, no code:

```properties
quarkus.langchain4j.ollama.chat-model.model-options.think=false
quarkus.langchain4j.ollama.chat-model.model-options.num-ctx=8192
quarkus.langchain4j.chat-memory.memory-window.max-messages=10
```

**Median turn: 11–34s (one at 265s) → 2.7s.** Already applied.

### Conversational tone was the prompt, not the model

The prompt said "prefer a question over a paragraph" and, for practice turns, "go straight
to the question below". The model obeyed, and turns came out as bare echoes:
*"Play A# on the keyboard."* Rewritten to *teach, then ask* — connect to what is solid,
say what to listen for, then put the question — the same model on the same turn now says:

> You already know how to find intervals on the keyboard, so let's use that to build
> triads — just stack two thirds on top of each other. For an F minor triad, start with F,
> then find the minor third above it, which is Ab.

Also applied.

### Letting the learner choose — done, and what it revealed

Turning the choice of exercise form over to the learner was the obvious response to the
repetition, so it was built: say *"let me play these instead"* and the tutor honours it
from then on, and `PUT /api/learner/practice-mode/{play|write|auto}` gives an interface the
same control. The choice is persisted, stated in the prompt, and it is recognised in the
answer box as well as in conversation — otherwise "let me play these" would have been
marked as a wrong answer.

Two things came out of building it.

**Alternation mattered more than the choice.** When the learner expresses no preference,
the tutor now alternates between a concept's forms rather than asking the same one every
turn. That, on its own, broke the "play F3, play G3, play A3" loop.

**The menu was nearly empty** — only 9 of 22 concepts had more than one form, and the 13
without one included every harmony concept. That has since been fixed under W4; what
follows is what the gap looked like:

```
two forms (play / write)   note, interval, major-scale, minor-scale, triad,
                           chord-inversion, seventh-chord, dominant-seventh,
                           secondary-dominant
one form only              key-signature, scale-degree, mode, diatonic-triads,
                           roman-numeral, figured-bass, harmonic-function,
                           tonic-function, predominant-function, dominant-function,
                           cadence, voice-leading, modulation
```

The control surface is finished. What it needs now is something to control, which is W4.

### What the benchmark left behind

Turns are still formulaic across a run — *"You played G3 — well done! Now try A3. Think
about where that note sits…"*, over and over. More prompt tuning did not fix it, and it is
not really a model problem: **the policy hands the model the same shape of task every
turn**, so there is nothing new to say. That is W4 and W7 below, and it is why they moved
up the plan.

---

## The three gaps

**It is slow to answer** — *largely closed by the benchmark above; W1–W3 remain worth
doing, but they are no longer the emergency.* Median 2.7s now, with occasional 15s turns.

**The policy's decision does not reach the exercise.** `TutorOrchestrator` calls
`exerciseService.create(learner, session, conceptId, difficulty, mode)` — the pedagogical
action is not a parameter. `PRACTICE`, `CHALLENGE` and `TRANSFER` therefore produce the
same *kind* of task at different difficulties, and differ only in how the model words them.
Four of the twelve evidence types are defined, weighted, and never generated by anything —
including `TRANSFER_PROBLEM` at weight 1.00, the top of the ladder.

**The learner model collects more than it uses.** Seven learning-style preferences are
stored and nudged on every answer; three of them (`explanationDepth`,
`socraticPreference`, `examplePreference`) are read by nothing, and `auralPreference` is
written but never read. `intrinsicDifficulty` is hand-authored in `concepts.json` while the
`evidence` table holds the data to calibrate it.

---

## Phase 1 — Make the turn feel immediate

*Re-prioritised after benchmarking.* Configuration already took the median turn from
11–34s to 2.7s, so this phase is now polish rather than rescue. W3 is largely redundant;
W1 still matters because a 15s outlier with no feedback feels broken.

### W1. Stream the decision before the prose · S · low risk

**Why** The policy decides in milliseconds; the model then takes 8–58 seconds. Today the
learner sees `…` for all of it.

**Evidence** `GET /api/session/{id}/stream` already emits `decision` → `turn` → `done`, and
is tested. The V1 interface uses plain POST and never subscribes.

**Approach** Move `tutorStore.send`/`start` onto the SSE endpoint. On `decision`, render
the action and concept immediately ("staying with dominant sevenths…") plus the generated
exercise, which is known at that point. Replace with the prose when `turn` arrives. Keep
POST as the fallback when `EventSource` is unavailable.

**Done when** The exercise is on screen within 200 ms of sending, with the model's wording
arriving afterwards; the existing Playwright specs still pass unchanged.

### W2. Stream the model's tokens · M · medium risk

**Why** W1 removes the wait before the question. This removes it before the answer.

**Approach** `quarkus-langchain4j` supports streaming chat models. Add a streaming variant
behind the existing `TutorModel` seam, emit `token` events on the same SSE channel, and
keep the non-streaming path for the template tutor and for providers without it.

**Done when** Prose appears progressively; `TemplateTutor` and `MUSIC_LLM_ENABLED=false`
are unaffected; the failure window still opens on a mid-stream failure.

**Risk** Partial responses must not reach the learner if the markup guard would have
rejected them. Buffer the first ~40 characters before rendering.

### W3. Keep the model warm · S · low risk

**Why** The 105-second first turn is model load, not inference.

**Approach** Set Ollama's `keep_alive`, and issue a tiny warm-up completion at startup when
the model is enabled. Non-fatal, off the startup critical path.

**Done when** First turn after boot is within a factor of two of subsequent turns.

---

## Phase 2 — Close the loop between the policy and the practice

**Now the most valuable phase.** The benchmark showed why: with latency fixed and the
prompt teaching properly, what is left is that the tutor asks the same shape of question
over and over. No amount of prompt work fixes that, because the sameness is upstream of
the model.

### W4. Make exercise generation action-aware · M · medium risk — **DONE**

*Built. `ExerciseGenerator` now dispatches on a `TaskKind` (identify / build / analyse) as
well as a channel, every concept carries a menu of forms, and the teaching action chooses
the kind: challenging someone asks them to analyse a progression, practising asks them to
build. The menu went from 22 forms to 66, every concept has at least three, and the
round-trip test holds all of them to their own answer keys. The tutor rotates through a
concept's forms so the same one is never asked twice running.*

*What is left of W4: an `APPLY` kind, and passing the action through to difficulty as well
as to kind.*


**Why** `TRANSFER` means "apply it somewhere it has not been seen". Today it means "the
same question, harder".

**Approach** Pass `TeachingAction` into `ExerciseGenerator.generate`. Each concept's
generator gains a small dispatch: `PRACTICE` keeps today's task, `CHALLENGE` raises
constraints (harder keys, inversions, less common qualities), `TRANSFER` changes the frame
(identify the concept inside a progression, or use it in a key never practised),
`REINFORCE` reduces to the single sub-skill that failed.

**Done when** `ExerciseRoundTripTest` covers every (concept × action) pair and each still
accepts its own answer key; a policy test asserts that `TRANSFER` and `PRACTICE` on the
same concept produce different `ExerciseType`s.

**Risk** This is the widest change in V2. The round-trip test is the guard that makes it
safe — it will catch any generator that drifts from its answer.

### W5. Generate the top of the evidence ladder · M · low risk

**Why** `TRANSFER_PROBLEM` (1.00) and `MIDI_PROGRESSION` (0.85) are the strongest evidence
the model defines, and nothing produces either. Mastery is currently earned almost entirely
through recall and single-chord playing.

**Approach** Transfer problems fall out of W4. For `MIDI_PROGRESSION`, extend
`MidiEvaluator` to judge a played sequence of chords against an expected progression,
reporting the first chord that diverged — the same "wrong bass, right chord" treatment,
one level up.

**Done when** A learner can be asked to play ii–V–I in a named key and be told *which*
chord went wrong; both evidence types appear in `GET /api/learner/evidence`.

### W6. Aural evidence · M · low risk

**Why** `AURAL_RECOGNITION` is defined at weight 0.70 and never generated, yet the browser
already has a synthesiser.

**Evidence** abcjs is loaded for notation and ships with `synth`; the backend already emits
ABC for chords, scales and progressions.

**Approach** New answer mode `AURAL`. The backend generates two or three ABC fragments and
marks which is correct; the frontend plays them and takes the choice. Evaluation stays
deterministic — the answer is an index, not a judgement.

**Done when** "Which of these two is the minor triad?" works end to end with the model
disabled.

### W7. Scaffolding when something is not landing · S · low risk

**Why** `MULTIPLE_CHOICE` (0.40) and `HINTED_RECALL` (0.30) exist precisely so a struggling
learner can be given a smaller step. Neither is ever generated, so a learner who fails
three times gets the same open question a fourth time.

**Approach** In `TeachingPolicy`, after N consecutive failures on a concept, drop the
evidence type: open recall → multiple choice → hinted. The weights already encode that
these are worth less, so mastery cannot be farmed by taking the easy path.

**Done when** Three consecutive failures produce a multiple-choice question, and the
resulting evidence is weighted 0.40.

---

## Phase 3 — Let the model learn how you learn

Depends on Phases 1–2 for data volume. Do not start it early; these need real sessions
behind them.

### W8. Use the preferences already being collected · S · low risk

**Why** Three of seven preferences steer nothing. The claim that the tutor adapts to a
teaching style is currently half true.

**Approach** Feed `explanationDepth`, `socraticPreference` and `examplePreference` into
`TutorPromptBuilder` as explicit instructions ("this learner does better with a worked
example than a definition"), and let `auralPreference` bias W6's answer mode the way
`keyboardPreference` already biases MIDI.

**Done when** Two learners with opposite preference vectors produce measurably different
prompts for the same decision, asserted in a test.

### W9. Calibrate difficulty from data · M · medium risk

**Why** `intrinsicDifficulty` is a guess written by hand. Every answer ever given is
recorded with its difficulty and result.

**Approach** A scheduled job estimating per-concept and per-exercise-type difficulty from
observed success rates — Rasch-style, one parameter. Keep the hand-authored value as the
prior for concepts with little data.

**Done when** Calibrated difficulty is used by `TeachingPolicy.difficultyFor`, and a
concept whose real success rate contradicts its hand-set difficulty visibly moves.

**Risk** Feedback loop: the policy chooses difficulty, which biases the data used to
estimate difficulty. Anchor on the prior until a concept has enough observations.

### W10. Migrate mastery to Bayesian Knowledge Tracing · L · medium risk

**Why** The deterministic model is well-behaved and explainable, which was right for V1. It
cannot express "probably knows this but slipped".

**Approach** BKT alongside the current model, not replacing it: run both, store both, and
compare against the recorded evidence history before switching. The `evidence` table
already holds everything BKT needs, so the migration is a replay rather than a rewrite.

**Done when** Replaying the full history produces BKT estimates that can be compared with
the deterministic ones, and the switch is a configuration flag.

### W11. Depth-versus-breadth steering · S · low risk

**Why** The frontier picks the least intrinsically difficult unknown concept, which is
breadth-first. A learner who wants to go deep must ask every single time.

**Approach** One preference — breadth or depth — inferred from whether the learner keeps
steering toward the same branch, used to order the frontier by proximity to recent work
rather than by difficulty alone.

---

## Ongoing, not phased

**W12. Widen the theory engine · L** — ninths and above, altered dominants, Neapolitan and
augmented sixths, non-chord tones, and four-part voice leading with real parallel-fifth
detection. Each addition needs a generator, a briefing entry and tests; the round-trip test
enforces that automatically.

**W13. More misconceptions · S each** — five is a start. Confusing relative with parallel
minor, spelling by semitone rather than by letter, and resolving the leading tone downwards
are all detectable with the engine that exists.

**W14. Export the learner model · S** — everything of value lives in one Postgres database
with no export path. A JSON dump of learner, evidence, misconceptions and sessions, and an
import to match. Small, and the only protection against losing the thing the product is
for.

---

## Not in V2

Deliberately, and for the same reason they were out of V1 — none of them make the tutor
teach better:

multi-user and authentication · audio-to-microphone transcription · RAG or a vector
database · a mobile application · gamification, streaks or badges · course authoring ·
MusicXML editing · any external tutoring service.

*Amended.* Retrieval and a search index were on this list, and are now built. The reason
the list was right and the reason it changed are the same one: retrieval does not make the
tutor more fluent, and fluency was never the problem. It was added because it makes the
tutor **attributable** — it can no longer invent a Beethoven example, because examples now
come from annotated scores with bar numbers, and explanations cite the chapter they came
from. See docs/knowledge/.

Multi-user is the one worth naming explicitly. `Learner.SINGLETON_ID` is a deliberate
single-user decision, and undoing it means real authentication, per-request learner
resolution and a review of every query. That is a project, not a feature, and nothing in
V2 needs it.

---

## Order of work

```
[done] think=false, num-ctx, memory window   11-34s -> 2.7s median
[done] "teach, then ask" prompt              echoes -> real teaching
          │
W4 action-aware ────┬──► W5 ladder  Phase 2: what gets asked  <-- start here
W7 scaffolding  ────┤
W6 aural ───────────┘
          │
W1 stream ─┴──► W2 tokens           Phase 1: the remaining 15s outliers
          │
          └──► W8 preferences ──► W9 calibration ──► W10 BKT
               W11 steering        Phase 3: needs data
```

**Start with W4.** The benchmark moved it to the front: latency and tone are handled, and
the repetition that remains is the policy's, not the model's. Phase 3 still should not
start until there are real sessions behind it.

## How to know V2 worked

1. A turn shows its question within 200 ms, and its prose progressively after.
2. `TRANSFER` produces a genuinely different task from `PRACTICE`, and the strongest
   evidence types actually appear in the evidence table.
3. A learner who consistently fails an open question is given a smaller step rather than
   the same question again.
4. Two learners with different demonstrated preferences get visibly different teaching for
   the same concept.
5. Everything above still works with `MUSIC_LLM_ENABLED=false`.

The fifth is the one that matters most. If any V2 feature stops working when the language
model is switched off, it has been built in the wrong place.
