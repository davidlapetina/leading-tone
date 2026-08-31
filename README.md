# Leading Tone

A personal music-theory tutor with no lessons in it.

*The leading tone is the note that pulls you to where you have to go next. So is this.*

There is no course, no chapter list and no exercise bank. The application keeps a model of
what you actually know, decides what to do about it, generates the question, marks the
answer — including one played on a MIDI keyboard — and only then asks a language model to
put the turn into words.

The hypothesis being tested:

> Can an AI keep an accurate model of what I know about music theory, and continuously
> teach me the right next thing, through conversation, notation and the piano, without
> following a predetermined course?

## The design in one line

**The language model teaches. It does not decide, and it does not mark.**

Concept selection, pedagogical action, difficulty, the exercise, the verdict on an answer,
and every mastery value are computed in Java and covered by tests. The model is given a
decision and asked for the words. It has a `proposeEvidence` tool and no `setMastery`.

Run it with `MUSIC_LLM_ENABLED=false` and the tutor still works — terser, but with
identical pedagogy. That is the point, and it is how the tests run.

---

## 1. Install the dependencies

You need: **Java 21**, **Maven** (the wrapper is included), **Node 20+**, **pnpm**,
**Docker**, and optionally **Ollama** for the language model.

### macOS

```bash
# Homebrew, if you do not have it
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

brew install openjdk@21 node git jq
brew install --cask docker        # then launch Docker Desktop once, so the daemon runs
npm install -g pnpm

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

brew install ollama               # optional
```

Maven itself is not required: `backend/mvnw` downloads the version this project uses.

### Linux (Debian / Ubuntu)

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk git curl jq docker.io docker-compose-v2
sudo usermod -aG docker "$USER"   # log out and back in for this to take effect

# Node 20+ via nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
. "$HOME/.nvm/nvm.sh" && nvm install 22
npm install -g pnpm

curl -fsSL https://ollama.com/install.sh | sh    # optional
```

### Check

```bash
java -version     # 21.x
node --version    # 20+
pnpm --version
docker info       # the daemon must be running
```

## 2. Get the code and its packages

```bash
git clone <this repository> music && cd music
cd frontend && pnpm install && cd ..
```

The backend downloads its own dependencies on first build; nothing to do by hand.

## 3. Run it

```bash
make db          # PostgreSQL 17 in Docker, on port 5433
make backend     # the API on port 8088
make frontend    # the interface on port 5173
```

Each in its own terminal. Then open <http://localhost:5173>.

Flyway creates the schema on first start and the concept graph is seeded automatically.
There is no manual database setup and no SQL to run by hand.

### Ports

**5433** for Postgres and **8088** for the API, both deliberately off the usual
5432/8080 so this can run beside another stack. Override with `MUSIC_DB_PORT` and
`MUSIC_HTTP_PORT`.

## 4. The language model (optional)

Everything works without it — you get a terser teacher, not a broken one. For prose:

```bash
ollama serve            # in its own terminal, or via the macOS app
ollama pull qwen3:8b
ollama run qwen3:8b ""  # optional: loads the model so the first turn is not slow
```

Then start the backend normally. Confirm what it is actually using:

```bash
curl -s localhost:8088/api/session/status
# {"narrator":"language model with theory tools","model":"qwen3:8b",...}
```

### Choosing a model

Any Ollama model works, and switching one is an environment variable, not a code change.

| Your RAM | Model | Notes |
|---|---|---|
| 8 GB | `qwen3:4b` | usable; short, plain teaching |
| 16 GB | `qwen3:8b` | **the default.** Good prose, handles tool calls |
| 32 GB+ | `qwen3:30b-a3b` | the preferred model where it fits; noticeably better teaching |
| any | `llama3.2` | small and fast, but paraphrases questions badly |

```bash
OLLAMA_MODEL=qwen3:30b-a3b make backend
```

A model much larger than your RAM will swap, take minutes per turn, and time out. The
tutor handles that (see *When things break* below), but the teaching is worse than a
smaller model that fits.

Cloud providers work too, and none of the tutoring logic changes: add the relevant
`quarkus-langchain4j-*` extension and set its configuration. `TutorModel` is the only
seam the rest of the application knows about.

## 5. The keyboard

**You do not need one.** Every question that asks you to play something can be answered on
the on-screen piano, which appears by itself when no instrument is connected. It submits
exactly what a real keyboard submits — MIDI note numbers, in the order struck — so the
answer is judged identically. Click the notes, then *Play it*.

With a real instrument: Web MIDI needs **Chrome or Edge** (Safari and Firefox do not
implement it) and a secure context, which `localhost` counts as. Plug the piano in before
loading the page and allow the MIDI permission prompt. The interface collects what you play
and submits it once every key is released; there is a button if you would rather not wait.

Everything is also answerable by typing, and you can tell the tutor which you prefer —
either in words ("let me play these") or with the **Mixed / Play / Write** control.

## 6. Configuration

Every setting has a working default. Nothing below is required.

| Variable | Default | |
|---|---|---|
| `MUSIC_HTTP_PORT` | `8088` | API port |
| `MUSIC_DB_PORT` | `5433` | Postgres port, host side |
| `MUSIC_LLM_ENABLED` | `true` | `false` runs the tutor on templates only |
| `MUSIC_LLM_TOOLS` | `true` | gives the model the theory engine as tools |
| `MUSIC_LLM_COOLDOWN` | `PT2M` | how long to stop calling a model that failed |
| `MUSIC_LEARNER_NAME` | `Student` | what the tutor calls you |
| `OLLAMA_MODEL` | `qwen3:8b` | any Ollama model |
| `OLLAMA_URL` | `http://localhost:11434` | |
| `OLLAMA_TIMEOUT` | `60s` | per model call |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local Postgres | production profile only |

No secrets are needed for the default setup, and none are committed.

## 7. Tests

```bash
make test        # backend (JUnit) + frontend (Vitest)
make test-e2e    # Playwright, against the running stack
make check       # the above, plus typecheck and production build
```

`make test` needs Docker running: the backend integration tests get a throwaway Postgres
from Quarkus Dev Services (Testcontainers underneath). No test needs a language model.

For `make test-e2e`, start `make db` and a backend with `MUSIC_LLM_ENABLED=false` first —
the Makefile target reminds you. Playwright downloads Chromium on first use:

```bash
cd frontend && pnpm exec playwright install chromium
```

## 8. Building for real

```bash
make build                       # backend jar + frontend dist/
java -jar backend/target/quarkus-app/quarkus-run.jar
```

Set `DB_URL`, `DB_USER` and `DB_PASSWORD` for anything other than the local database.

---

## What it does

- **Covers common practice and jazz.** 31 concepts from note names to species
  counterpoint: diatonic harmony, Roman numerals, cadences, secondary dominants and
  modulation, then extended and altered chords, the ii-V-I, modal interchange, tritone
  substitution and the twelve-bar blues. Counterpoint is marked, not opined about —
  parallel fifths are computed.
- **Asks in several ways.** Each concept has three or more forms — name it, build it,
  explain what it is doing in a key — and the tutor rotates through them, so the same
  question is never asked twice running. Challenging you means analysing a progression, not
  the same question in a harder key.
- **Makes the step smaller when you keep missing it.** Two failures in a row and the
  question comes with a hint; four and it becomes a choice of four. Both are weighted lower,
  so the easy route cannot earn mastery.
- **Diagnoses instead of asking.** A first session starts by finding out, not by
  announcing a syllabus.
- **Never leaves you with nothing to do.** Every turn, including the explanatory ones,
  ends in something answerable — otherwise no evidence is produced and the learner model
  cannot move.
- **Knows a question from an answer.** Type "explain" and the question stays open and
  nothing is marked. Ask about something else entirely and it takes that up instead.
- **Distinguishes right notes from the right chord.** Play G–B–D when asked for G major in
  first inversion and the answer is *"the right notes, but the bass should be B"* — partial
  credit against a named misconception, not a failure.
- **Weights evidence by how it was shown.** Naming V7 counts for less than playing it,
  which counts for less than explaining it. Forty correct multiple-choice answers will not
  earn `MASTERED`.
- **Keeps its spelling straight.** F♯ and G♭ are not the same object anywhere in the
  engine, so E♯ survives in F♯ major and an augmented fourth is never reported as a
  diminished fifth.
- **Explains itself.** `GET /api/learner/evidence` is the full audit trail: every
  observation, what it was worth, and the mastery before and after.

## When things break

| | What happens |
|---|---|
| Ollama not running | Turns come from templates. `status` says so, and why. |
| Model too slow or times out | The same, and the model is left alone for two minutes rather than every turn paying the timeout. |
| Model replies with JSON instead of prose | Rejected; the template turn is used. |
| Model paraphrases the question away | The generated exercise is shown in its own right, so you always see what was actually asked. |
| Backend down | The interface says so instead of showing an empty page. |
| No MIDI keyboard, or permission refused | Said plainly in the header; everything stays answerable by typing. |

None of these touch the learner model. Mastery only ever moves through
`EvidenceService`, from a deterministic verdict.

## Layout

```
music/
├── backend/          Quarkus, Java 21, package fr.lapetina.music
├── frontend/         React + TypeScript + Vite, abcjs for notation
│   └── e2e/          Playwright scenarios
├── docs/             architecture, learner model, tutoring policy, concept model
├── docker-compose.yml
└── Makefile
```

## The API

```
POST   /api/session                     start a session, get the opening turn
GET    /api/session/{id}                the whole conversation
POST   /api/session/{id}/message        say something
GET    /api/session/{id}/stream         the same turn as server-sent events
GET    /api/session/next-action         what the policy would do, without doing it
GET    /api/session/status              narrator, model, tools, concept count

POST   /api/exercises                   generate one directly
POST   /api/exercises/{id}/answer       answer in words
POST   /api/exercises/{id}/midi         answer by playing

GET    /api/learner                     the full learner model
GET    /api/learner/concepts            mastery per concept
GET    /api/learner/evidence            why the tutor believes what it believes
DELETE /api/learner                     start again from nothing (dev and test only)

GET    /api/concepts                    the prerequisite graph
POST   /api/theory/chord/analyze        the theory engine, directly
POST   /api/theory/progression/analyze
GET    /api/theory/key/{key}
GET    /api/theory/scale/{tonic}/{type}
POST   /api/theory/notation
```

OpenAPI at <http://localhost:8088/q/swagger-ui>, health at `/q/health`.

## Tests, and what they are for

Backend JUnit + RestAssured, frontend Vitest + Testing Library, Playwright end to end.
None of them need a language model.

The theory engine is tested as musical fact (`assertEquals("G/B", …)`, F♯ major keeps its
E♯, a first-inversion seventh is figured 6/5). The policy is tested by building a learner
model by hand and asserting on the decision. Two are worth knowing about:

- **`ExerciseRoundTripTest`** — for every concept, at three difficulties, in both
  channels, it generates an exercise and answers it with the generator's own answer key.
  A question that drifts away from its answer fails the build.
- **`keepsProducingEvidenceAfterWrongAnswers`** — six wrong answers in a row must still
  leave something to answer. An earlier version deadlocked here.

## What V1 deliberately leaves out

Audio-to-MIDI, RAG, vector search, user accounts, mobile, course authoring, teacher
dashboards, multi-user, gamification, MusicXML editing, and any external tutoring service.
None of them test the hypothesis. The learner model is the interesting part, and it is
built here rather than delegated.

## Licence

MIT. See [LICENSE](LICENSE).
