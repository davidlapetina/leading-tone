# Licensing

## The rule

**This application's code is MIT. None of the knowledge it reads is.**

Open Music Theory is CC BY-SA 4.0. Every score corpus and the Jazz Harmony Treebank are
CC BY-NC-SA 4.0. Downloading, parsing, normalising, chunking, embedding, indexing or
exporting that material does not move it onto our licence, and does not discharge an
attribution, ShareAlike or NonCommercial obligation.

That boundary is enforced in code rather than described in a file nobody reads:

- `LICENSE` — MIT, application code only. Not modified by this feature.
- `THIRD_PARTY_NOTICES.md` — generated from the source registry, so it cannot drift.
- `licenses/` — the full upstream texts.
- Every `knowledge_document`, `knowledge_chunk` and `knowledge_harmony_event` row carries
  its own `license_id`. Not the source's — its own.

## Why per document and not per source

A source's licence is only a default.

Open Music Theory is CC BY-SA 4.0 as a book. Of its 140 chapters, **138 are**, one is
CC BY-NC-SA 4.0, and one — *Composing with Twelve Tones* — is **All Rights Reserved**.
Trusting the book-level licence would quietly ingest a chapter whose author reserved it.

So each chapter's licence is read from the publisher and checked on its own, and a chapter
that fails the check **is never requested**. Its text does not reach this machine at all,
which is a stronger guarantee than downloading it and choosing not to use it.

## How the check works

`LicenseUrls` holds a closed list of licence URLs we recognise. Anything unmapped is
`UNKNOWN`, and unknown material is never ingested. There is no optimistic fallback.

That is deliberately general rather than a list of exceptions. If a new chapter appears
tomorrow under terms nobody has considered, it is refused automatically — nothing has to be
noticed by a person first.

## NonCommercial

Thirteen of the fourteen sources are NonCommercial. This deployment is not commercial, so
the restriction is **recorded** — on the source, on every row derived from it, and in the
notices file — rather than enforced by a runtime switch.

If this ever became a commercial product, that recording is what tells you which sources
must be disabled, and the honest answer is: all of the corpora. Embedding and indexing do
not remove a NonCommercial restriction, and nobody should assume otherwise because the
material has been transformed.

## Exports

Learner data and source material are exported separately and never merged into one file.
The learner's own progress carries no third-party claim; anything derived from a source
carries that source's licence and its required citation.
