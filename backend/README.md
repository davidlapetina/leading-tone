# Backend

The tutor itself: the theory engine, the learner model, the teaching policy, the knowledge
subsystem, and the HTTP API the browser talks to. Java 21 on Quarkus 3.39.1.

For what the application *is*, read the [root README](../README.md). This file is about
working on the backend.

## Running it

```shell
./mvnw quarkus:dev          # live reload, http://localhost:8088
./mvnw test                 # 362 tests, no network, no model, no Docker
```

`make backend` from the repo root does the same thing. The port is `MUSIC_HTTP_PORT`,
default 8088.

State lives in one directory, `MUSIC_DATA_DIR` (default `./data`): an H2 file database and,
once you have brought sources in, the Lucene index and the downloaded corpora. Delete the
directory and you have a clean install; there is nothing else to reset.

## Packaging

```shell
./mvnw package              # backend/target/leading-tone-runner.jar
```

One uber-jar, around 134 MB, and `java -jar` is the whole install. It is large because the
embedding model and the ONNX runtime that executes it are inside it — that is the price of
not asking anyone to run a Python service or an embedding API. `make package` from the root
builds the frontend first and bundles it into the same jar.

Two things about that jar are load-bearing and easy to break:

- **Lucene resolves its codecs through `META-INF/services`.** An uber-jar has to merge those
  files, not overwrite them. Overwrite them and everything compiles, every test passes, and
  opening an index throws in front of a user — so `make verify-jar` runs the built jar and
  asks it to open the index for real.
- **The ONNX runtime ships a 304 MB debug-symbol file** that exceeds the jar entry limit.
  It is excluded in `application.properties`; all ten native libraries are kept.

## Layout

Package `fr.lapetina.music`:

| Package | What lives there |
|---|---|
| `theory` | Pitch, interval, scale, chord, Roman numeral, cadence. Pure records and enums — **no framework**, enforced by `TheoryPackageIsFrameworkFreeTest` |
| `theoryservice` | The injectable façade over `theory` |
| `concept` | The concept graph, lessons, prerequisites |
| `learner` | Mastery, evidence, review scheduling |
| `exercise` | Generating questions and marking answers, including played ones |
| `tutor` | The teaching policy: what to do next, and why |
| `knowledge` | Sources, licences, ingestion, chunking, embedding, Lucene, retrieval, structured harmony, provenance |
| `llm` | The optional language model. **Deletable** — everything above still works without it |
| `midi`, `settings`, `store`, `api`, `infrastructure` | Input, configuration, persistence, HTTP, plumbing |

The dependency runs one way: `theory` knows nothing about anything else, and `llm` is a leaf.
That is what lets the tutor teach with the model switched off, which is a design rule rather
than a fallback.

## Conventions

These are followed everywhere; match them rather than introducing a second style.

- Panache **active-record** entities: public fields, manual `UUID` ids, `@Transactional` on
  writes. No repositories.
- Services are `@ApplicationScoped` with `@Inject` on package-private fields.
- Tests are plain JUnit 5 with prose `@DisplayName`s that state the behaviour, not the method
  name. They run offline: no network, no model, no container.
- The schema is one Flyway migration, `db/migration/V1__schema.sql`, with Hibernate set to
  `validate`. The schema is not generated from the entities.

## Knowledge subsystem

Nothing downloads on its own. A source is declared in `knowledge-sources.yaml` — which is
authoritative, so a source the manifest does not name cannot be fetched at all — and brought
in from the Settings screen or `POST /api/knowledge/sources/{id}/ingest`.

Licensing is enforced in code rather than documented and hoped for. `LicensePolicyService` is
the single gate: a source whose licence is unknown can never become active, and everything
retrievable is filtered through it on every route. Our code is MIT; retrieved material keeps
its own licence, and the two are never conflated. See [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).

Deeper notes are in [`docs/knowledge/`](../docs/knowledge/).
