# Architecture

One Java process. No Docker, no Python, no vector database, no embedding service. The only
thing outside it is the language model, and the application teaches without that too.

```
leading-tone (one JVM)
│
├── theory ─────────── records and enums, no framework imports at all
│                      the source of truth for anything calculable
├── theoryservice ──── the CDI façade onto it, deliberately outside theory
│
├── knowledge
│   ├── source ─────── the registry: what exists, declared in knowledge-sources.yaml
│   ├── license ────── who may serve what, and in which mode
│   ├── ingestion ──── fetch, parse, chunk, embed, index; and the local copy of it all
│   ├── embedding ──── bge-small-en-v1.5-q on ONNX Runtime, in this process
│   ├── index ──────── Lucene: BM25 postings and 384-dim vectors in one segment
│   ├── retrieval ──── hybrid search and the ranking
│   ├── harmony ────── annotated scores, structured; and the engraved excerpts
│   ├── router ─────── which of the three kinds of answer a question wants
│   └── provenance ─── what an answer was actually built from
│
├── concept · learner · exercise · midi · tutor
└── llm ────────────── the model behind an interface
        │
        └── (optional) Ollama
```

## The dependency rule

Inward. `theory` knows nothing. `knowledge` knows `theory` and `concept` and nothing above
them. `tutor` knows everything below it. `api` knows `tutor`.

The consequence worth stating: **`llm` and `knowledge` can both be deleted and the
application still teaches** — from computed facts and generated exercises, without
citations. That is not a hypothetical. The entire test suite runs that way.

## Three kinds of answer

`KnowledgeRouter` classifies a question deterministically and gathers evidence before the
model is called.

| Kind | From | Failure if confused |
|---|---|---|
| Calculation | the theory engine | a guessed answer that is confidently wrong |
| Explanation | retrieved prose, quoted and cited | a dry recitation of arithmetic |
| Example | annotated scores, or nothing | **a fabrication with a composer's name on it** |

The third is why they are kept apart all the way into the prompt rather than merged into
one context. A model handed all three undifferentiated will treat a plausible sentence as
equal to a measure number.

The same three kinds answer a question asked directly, through `POST /api/ask`. There are
two differences. A teaching turn retrieves only when the policy decided the learner needed
prose; a question typed into a box **is** the request, so retrieval always runs. And the
three kinds are not only kept apart in the prompt but shown apart in the answer, so a reader
can see which part of it was computed, which was quoted and from where, and which bars are
real. An answer you can check is worth more than one you have to trust.

## Trust order in the prompt

Computed facts, then verified examples, then quoted prose — in that order, labelled, with
the quotation fenced and the model told plainly that computed facts win on disagreement.

Fencing is mitigation, not proof, and this document should not claim otherwise. What
actually protects the learner is structural and unchanged by retrieval: the model cannot
write mastery, choose the concept, set the difficulty or mark an answer. **Retrieval adds no
new write path.**

## Storage

```
backend/data/
├── leading-tone.mv.db      learner, settings, chunks, harmony rows, provenance
└── knowledge/
    ├── sources/raw/        the downloaded originals, kept so a rebuild needs no network
    └── index/gen-NNNNNN/   one complete Lucene index, written once and never modified
```

A generation is immutable. A new ingestion builds the next one alongside it and switches in
one transaction, then deletes the one it replaced — so a failed run leaves the working index
serving, and a successful one does not leave a second copy behind.

The database is compacted on clean shutdown. Bulk inserts leave H2 holding space it does not
return: measured at 853 MB for rows that compact to 23 MB.
