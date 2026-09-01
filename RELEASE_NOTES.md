# Leading Tone 1.0.0

A music-theory tutor that keeps a model of what you know, teaches from it, and can now cite
its sources and show you real music.

Everything runs in one Java process. No Docker, no Python, no vector database, no embedding
service. The only optional external dependency is a language model, and the application
teaches without one.

---

## What it does

**Teaches adaptively.** Tracks mastery per concept from evidence — answers, played phrases,
explanations — and decides what to teach from that. Asked "what is V/V?" by someone already
solid on dominant function, it builds from the dominant instead of starting at scales.

**Calculates rather than guesses.** A question with an arithmetic answer is computed in
Java. "What is V7/V in C major?" returns D F♯ A C because the engine worked it out, not
because a model recalled it. The model explains the result; it is never the calculator.

**Cites what it quotes.** Explanations are grounded in Open Music Theory, retrieved by
hybrid search and quoted with the chapter, author and licence attached.

**Shows real music.** "Give me a Beethoven example of V/V" returns actual bars from an
annotated score — engraved on a proper grand staff, with the harmony marked above it, and
the work, movement and bar number you can look up. **When no real example exists it says
so.** Nothing is invented to fill the gap.

**Has a jazz path.** Ten jazz concepts in the order a jazz musician meets them, grouped into
themes, each with lessons, exercises and examples drawn from a jazz corpus rather than a
sonata.

---

## The knowledge layer

### Sources

Fourteen, all licence-verified against their publishers:

| Source | Licence | Content |
|---|---|---|
| Open Music Theory v2 | CC BY-SA 4.0 | 124 chapters → 1,229 retrievable passages |
| Twelve DCML corpora | CC BY-NC-SA 4.0 | 113,921 harmonic annotations of real scores |
| Jazz Harmony Treebank | CC BY-NC-SA 4.0 | 1,170 tunes → 59,150 chord events |

Beethoven (quartets and sonatas), Mozart, Corelli, Chopin, Grieg, Medtner, Liszt,
Tchaikovsky, Dvořák, Debussy, Schumann.

Nothing downloads until you ask. Everything downloaded is kept locally, so rebuilding never
goes back to the publisher.

### Search

Apache Lucene holds BM25 postings and 384-dimension vectors in one index. Embeddings come
from bge-small-en-v1.5 running on ONNX Runtime inside the same JVM — no service, no
download at first use.

Music notation survives tokenisation, which is the part generic search gets wrong:

```
V/V   V7/V   iiø7   vii°7   Ger+6   Fr+6   It+6   N6   bII   #iv°   ii-V-I
```

Each of those finds its own passage. `V` and `v` stay different chords; `iiø7` and `vii°7`
do not collapse into each other; "a" and "i" are not treated as stop words, because one is a
note and the other a Roman numeral.

### Theory engine

Framework-free records and enums — a test fails the build if a framework import appears in
the package. Spelling is preserved throughout, and tested as spelling rather than as pitch
class:

```
F# major        F# G# A# B C# D# E#
G altered       G Ab Bb Cb Db Eb F        a diminished fourth, so C flat
C whole tone    C D E F# G# A#            an augmented sixth, so A sharp
Ger+6 in C      Ab C Eb F#                the F sharp is what makes it not an Ab7
V7/V in C       D F# A C
```

Roman numerals parse — including `bII`, `#iv°`, `V7/V`, `Ger+6` and the spellings corpora
actually use. Chromatic harmony is recognised from a **closed list**; a chord no rule
honestly explains is marked `?` rather than labelled.

---

## Licensing

The application is MIT. **None of the knowledge is**, and ingesting it does not change that.

- Each document carries its own licence, not its source's. Open Music Theory is CC BY-SA 4.0
  as a book, but one of its 140 chapters is CC BY-NC-SA and one is All Rights Reserved — the
  latter is **never fetched at all**.
- Licence URLs are matched against a closed list. Anything unrecognised is refused; there is
  no optimistic fallback.
- Setting **Commercial** in Settings makes all thirteen NonCommercial sources unavailable —
  enforced at retrieval across structured search, cadence search, notation and direct
  lookup, not hidden in the interface.
- `THIRD_PARTY_NOTICES.md` is generated from the source registry, with full texts in
  `licenses/`.

---

## Provenance

Every answer records what it was built from: the passages retrieved, the annotation rows
cited, the calculations performed, the sources credited. Readable at
`GET /api/knowledge/provenance`.

The model's reasoning is deliberately **not** stored. It is not observable, and keeping a
plausible rationalisation beside real evidence would make the evidence harder to trust.

---

## Running it

```bash
java -jar leading-tone-runner.jar        # then http://localhost:8088
```

Java 21. The jar is 134 MB because the embedding model and every platform's ONNX native
library are inside it.

From source:

```bash
make test     # 400 tests: no Docker, no database, no network, no model
make run
```

---

## Fixed during final review

Found by running the packaged jar and looking at what it drew, not by running tests.


- **The tutor could open by agreeing with an answer that was wrong.** The model is told the
  verdict and asked to acknowledge it, and usually does; when it did not, a learner read
  "that's right" directly above an interface saying the answer was wrong. A turn that agrees
  with a wrong answer is now replaced by the template turn, which cannot disagree with the
  marking — the same guard that already caught the model asking for a different note than the
  exercise.

- **Wrong answers were being marked correct.** An answer counts wherever the expected words
  appear inside it, so that "it's G major" passes — but that also marked "F G A Bb C D E"
  correct for an answer of "G", accepted "A minor" on a question asking for the root and the
  quality, and accepted "D natural minor" where the form matched and nothing checked the note
  in front of it. Of 41,411 deliberately wrong answers, 1,407 were accepted; that is now 270,
  and what remains are the alternatives each exercise actually declares. Evidence of mastery
  that was never demonstrated had been going into the learner model.

- **One note in ten was engraved as the wrong pitch.** A key signature alters every note
  on its letters, so in F minor a plain D is D flat; a D natural written with no sign is a
  different note from the one in the score. 1,507 of 14,339 notes across the corpus excerpts
  were affected, and G Dorian was drawn as G Aeolian because its natural sixth silently
  became a flat. Accidentals are now tracked against the key signature and against what has
  already been written in the bar, which also removes the redundant sign that used to sit on
  every altered note.

- **Some exercises printed their own answer above the staff.** "Which minor scale is this:
  B C# D E F# G A#?" was drawn under a staff titled "B harmonic minor", and "In C major,
  which chord is V?" drew the chord with "G" written over it. Both marked correct, both
  taught nothing, and both fed the learner model evidence that was never earned. Notation
  carries no title at all now, and an exercise that would have to draw its answer draws
  nothing.
- **A scale was written in 4/4 and barred every four notes**, so a six-note blues scale
  ended in a bar three beats long. A scale has no metre, and is now written without one.

- **An unknown knowledge source answered 400 where an unknown concept answered 404**, so
  the same mistake looked like two different kinds of failure depending on what you asked for.

- **Every minor-key excerpt was drawn with no key signature.** ABC needs an upper-case
  tonic, and the corpora write minor in lower case, so `K:fm` was rejected outright and 171
  of 470 excerpts appeared in C major with every flat missing.
- **Bars did not add up.** A voice falling silent before the end left the bar short, a note
  held across the barline left it long, and either one draws every following bar in the wrong
  place. Bar length is now an invariant with a test that reads the notation back independently.
- **Rhythms with no notation were being approximated.** There is no five-eighth rest; a
  reader given one draws nothing. Durations are now split into real note values, unequal
  tuplets included, and an excerpt whose rhythm cannot be written exactly keeps its citation
  and shows no score rather than an invented one.
- **Beams ran the whole width of the bar** instead of grouping by beat, which is what makes
  a metre readable.
- **Grace notes collapsed the bar they were in**, having a duration of zero. They are now
  written as grace notes.
- **The tutor opened by referring to a turn that never happened** — "You just wrote another
  name for Ab" on the first screen a learner sees. The model cannot tell that a session is
  new, so it is now told.

- **Note durations were wrong in every engraved excerpt.** A sixteenth was written as a
  sixty-fourth and dotted rhythms as triplets, because the fraction was computed inside out.
  Classical piano writing is full of sixteenths, so most excerpts were affected.
- **The Roman numeral above the staff was truncated.** ABC read `V7/V` as a slash chord and
  drew `V7` — a different chord from the one being taught.
- **Corpus examples stopped rendering.** Unifying two example records turned `citation` from
  a field into a method, so it vanished from the JSON and the interface rejected the
  payload — silently. Both the field and the silence are fixed.
- **Fifteen of thirty-six concepts had no written lesson**, including secondary dominants.
  All are written now, and a test fails the build if a concept in the catalogue opens to an
  empty page.
- **Triplets were engraved as dotted notes.** Writing them at two-thirds length is
  arithmetically right and reads as a different rhythm; they are grouped as tuplets now.
- **A bar's rest was always a whole note**, which is twice the bar in 2/4.
- **The tutor asked for a different note than the exercise on screen** — "now play F#3"
  under a question asking for A3 — in about a third of turns. The reply falls back to the
  template when it contradicts the question that was actually set.
- **A flaky test.** Exercise variety was sampled over twelve random draws and could miss a
  kind by luck. A suite that fails at random teaches people to re-run it rather than read
  it.

## Notes

- Schema is a single Flyway migration. Upgrading from a pre-1.0 checkout means removing
  `backend/data`.
- The database is compacted on clean shutdown; bulk ingestion otherwise leaves H2 holding
  space it does not return.
- Turnarounds and blues form have no progression pattern matched in the loaded corpora yet,
  and report no verified example rather than approximating one.
