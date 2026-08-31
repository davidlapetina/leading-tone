# Sources

Declared in `backend/src/main/resources/knowledge-sources.yaml`, which is the authority.
Nothing outside that file can be fetched: the ingester refuses any URL whose host is not
one a declared source publishes from.

Every licence below was verified against the authoritative upstream source on
**31 August 2026**, by reading the publisher's own metadata or `LICENSE` file rather than
trusting documentation.

## Explanatory text

**Open Music Theory** (version 2) — CC BY-SA 4.0 — 140 chapters, of which 124 are ingested.

Read through the publisher's Pressbooks REST API rather than scraped, which gives per
chapter: title, part, link, word count, **licence** and **authorship**. Chapter authorship
is preserved; replacing a named author with the book's author list would be a worse credit
than none.

Two chapters are not under the book's licence, and are handled accordingly. See
[licensing.md](licensing.md).

⚠️ The API refuses the default JDK user agent with HTTP 403.

## Annotated scores

Twelve DCML corpora, all **CC BY-NC-SA 4.0**, each with its own required academic citation:
Beethoven string quartets and piano sonatas, Mozart piano sonatas, Corelli trio sonatas,
Chopin mazurkas, Grieg lyric pieces, Medtner *Skazki*, Liszt *Années de pèlerinage*,
Tchaikovsky *The Seasons*, Dvořák *Silhouettes*, Debussy *Suite bergamasque*, Schumann
*Kinderszenen*.

Read as `harmonies/*.tsv` over HTTPS — no cloning, and the MuseScore files are ignored.
Two details of the format are easy to get wrong and are handled explicitly:

- positions are **exact fractions** such as `13/2`, not decimals;
- `localkey` is a **Roman numeral relative to the global key**, not an absolute key.

`notes/*.tsv` is fetched lazily, only when somebody looks at an excerpt, and cached. That
is what makes an engraved example possible without ingesting every note of twelve corpora
up front.

Work titles come from each corpus's `metadata.tsv`, so a citation reads "Sonata no. 2,
Allegro vivace, bar 17" rather than "02-1, 1, bar 17".

## Jazz

**Jazz Harmony Treebank** — CC BY-NC-SA 4.0 — 1170 tunes.

A correction worth recording: all 1170 carry chords, measures and beats, but only **150
carry constituent trees** and 241 a turnaround. Hierarchical structure is available for part
of the corpus, not all of it.

Its chord shorthand is translated on the way in — `^` is a major seventh, `%` half
diminished, `o` diminished — and a flat is written as a hyphen **in the key field only**,
where chord symbols in the same file use `b`. Each chord is also given a Roman numeral
computed against the tune's key by this application's own theory engine, because a chord
symbol alone cannot answer "find me a minor two-five-one".
