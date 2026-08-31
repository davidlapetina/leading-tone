# Ingestion

Nothing is downloaded until somebody asks. Reading the internet at startup would be slow,
fragile and rude to the publishers this depends on, so ingestion is an explicit act — from
the Settings screen, or `POST /api/knowledge/sources/{id}/ingest`.

## The pipeline

```
declared in knowledge-sources.yaml
  → licence checked            unknown or refused stops here, before any fetch
  → downloaded                 and kept locally
  → parsed                     Pressbooks JSON, or corpus TSV
  → normalised                 HTML to text, LaTeX shortcodes to notation
  → chunked                    along headings, never at a fixed token count
  → embedded                   in this JVM
  → indexed                    into a new generation
  → activated                  in one transaction
```

A source becomes `ACTIVE` only when the whole run succeeds. There is no state in which half
a source is searchable.

## The local copy

Everything fetched is written under `data/knowledge/sources/raw/` and read from there next
time. Re-chunking or re-embedding is then a local operation: changing the chunk policy does
not mean asking a publisher for the same 140 chapters again.

`POST /sources/{id}/reindex` rebuilds from that copy. `POST /sources/{id}/refresh` discards
it and goes back to the publisher — the one operation that genuinely needs the network.

File names come from a hash of the URL, never from its text. A remote resource must not be
able to choose where on this disk it lands.

## Idempotence

Re-running changes nothing unless something did. The fingerprint covers everything capable
of changing the result:

```
content · upstream version · parser · chunk policy · analyzer · embedding model
```

Including the embedding model is the subtle one. Without it, changing the model would leave
the index holding vectors from a model no longer in use, and nothing would say so.

## Failure

A run that fails at any stage leaves the previous generation serving and records why.
Licence refusals are counted and named — "we have the whole book" and "we have it except two
chapters" are different claims, and the report distinguishes them.
