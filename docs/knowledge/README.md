# The knowledge layer

The tutor teaches from a theory engine it can check. This layer adds two things that engine
cannot supply: **published explanations**, and **real music**.

Both are attributable. An explanation says which chapter it came from; a musical example
says which bar of which piece. Neither is ever invented, and where nothing is found the
tutor says so.

## What runs where

Everything is in one JVM. There is no vector database, no embedding service, no Python and
no Docker. The only optional external process is the language model.

```
leading-tone (one Java process)
├── theory engine          computes; the source of truth for anything calculable
├── ONNX embeddings        bge-small-en-v1.5, 384 dimensions, in process
├── Lucene index           BM25 + vectors, on disk beside the H2 file
├── H2                     chunks, harmony rows, provenance, licences
└── tutor                  assembles all of it into one grounded answer
        │
        └── (optional) Ollama, or nothing at all
```

## The three kinds of answer

| Question | Answered by | Why |
|---|---|---|
| "What is V7/V in C major?" | the theory engine | it is arithmetic; D F♯ A C is not a matter of opinion |
| "Why do augmented sixths resolve to V?" | retrieval | it is an explanation somebody wrote better than we would |
| "Give me a Beethoven example of V/V" | the corpus | it is a fact about a score, and either it exists or it does not |

Mixing these up is how a tutor starts inventing measure numbers. They are kept apart
deliberately.

## Where the data lives

```
backend/data/
├── leading-tone.mv.db          learner, settings, chunks, harmony rows, provenance
└── knowledge/
    ├── sources/raw/            downloaded originals, so re-parsing needs no network
    └── index/
        ├── gen-000001/         a complete Lucene index: postings and vectors together
        └── current             the generation now serving
```

An index generation is written once and never modified. A new ingestion builds the next
generation alongside it and switches over in one transaction, so a run that fails halfway
leaves the working index exactly as it was.

## Reading on

- [architecture.md](architecture.md) — how the pieces fit
- [sources.md](sources.md) — what is ingested, and what each source gives
- [licensing.md](licensing.md) — the MIT/third-party boundary, which is not optional
- [ingestion.md](ingestion.md) — the pipeline and its guarantees
- [retrieval.md](retrieval.md) — hybrid search, and the ranking formula in full
