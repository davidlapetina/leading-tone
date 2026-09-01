# Retrieval

Two searches, combined. Neither is sufficient on its own for this subject.

## Why not just embeddings

Music theory is full of tokens whose exact form is the meaning:

```
V/V   V7/V   iiø7   vii°7   Ger+6   Fr+6   It+6   N6   bII6   #iv°   ii-V-I
```

A standard tokenizer treats `/`, `#`, `+`, `°` and `ø` as punctuation. `V7/V` becomes two
tokens; `vii°7` loses its diminished sign and collides with `viiø7`; lowercasing merges `V`
and `v`, which are a major and a minor chord. Semantic similarity does not recover any of
that — it will happily return a passage about dominants when asked for a specific applied
dominant.

So there is a `symbol` field, analysed by `MusicSymbolTokenizer`, where a token is a
maximal run of letters, digits and the signs that carry harmonic meaning. Case is
preserved. Compound symbols also emit their parts at the same position, so a search for
`V7` still reaches a passage about `V7/V` while the exact symbol scores far higher.

Two other choices in the analyzer are worth knowing about:

- **"a" and "i" are not stop words.** A is a note and i is a roman numeral. The standard
  English list removes both, which would make the index useless for exactly the queries
  this application exists to answer.
- **Stemming is KStem, not Porter.** Porter turns *cadences* into `cadenc`; the same field
  is shown back to the learner in the diagnostic view, so terms should stay words.

## The ranking

```
normalise each list by its own best score, so the top hit is 1.0
base(c)  = wLex · lex(c) + wVec · vec(c)          a missing side contributes 0
score(c) = base(c) · (1 + conceptBoost + takeawayBoost)
dedupe by chunk id · sort · at most 2 chunks from one document · take top K
```

Defaults: `wLex 0.55`, `wVec 0.45`, concept boost `0.35`, "Key Takeaways" boost `0.20`.

**Dividing by the best score rather than stretching across [0,1].** Min-max is the more
obvious choice and is wrong here for two reasons: it forces the worst candidate to exactly
zero, where no boost can ever reach it however relevant it is; and on a short list it
discards how close the scores were, so a runaway winner and a photo finish come out
identical.

With no embedding model the vector weight is zero and pure BM25 falls out of the same
arithmetic. There is no separate lexical-only branch to get wrong.

`ScoreFusion` is a pure function over records of numbers, so the ranking is tested by
handing it numbers rather than by standing up an index.

## Degradation

Retrieval returns nothing rather than failing. No index, no active source, a broken
searcher, retrieval switched off — all produce an empty result, and the tutor teaches from
the theory engine exactly as it did before. This mirrors how the application already copes
with the language model being absent.

## What reaches the model

Retrieved text is fenced and labelled as quotation, and the prompt states that computed
facts win over anything quoted. Fence markers and chat control tokens are stripped from the
body so a passage cannot close its own fence and start speaking as the system.

Fencing is mitigation, not proof. The protection that actually holds is structural and
retrieval does not weaken it: **the model cannot write mastery, choose the concept, set the
difficulty or mark an answer.** Retrieval adds no new write path.

## Asking directly

`POST /api/ask` runs the same retrieval, with the question as the whole query rather than a
concept name steered by the learner's words. It returns the passages themselves — citation,
licence, link and the quoted text — alongside the answer, because the material is the reason
to believe the answer. With no model, the answer is written from that material instead:
computed facts first, then the top passage quoted. Plainer, and still attributed.

Corpus examples asked for this way are chosen with one extra preference: an example that can
be engraved beats one that cannot. Not every corpus has note tables — the jazz treebank
annotates chords over lead sheets — and those sort early alphabetically, so a query for V7/V
came back as lead sheets with engraved Beethoven behind them. Showing the music is the point,
so the annotated scores are searched first and the results spread across different works.

## Which examples a lesson gets

A concept is mapped to the harmonies that illustrate it, and every label in that mapping was
checked against the ingested corpora. A concept mapped to a label nothing uses shows an empty
section, which reads as "there are no examples of this in real music" — a different claim
from the truth. The corpora do not share a vocabulary: the treebank writes `bII` and `IM7`
where the annotated scores write `bII7` and `I`, so the jazz concepts are listed in its
terms.

A concept that *is* a progression — the ii-V-I, a turnaround, voice leading — is shown as one
or not at all. Falling back to a single chord from the middle of it put a lone `iii7` under
"Turnarounds", which illustrates nothing.

Matches for one harmony arrive clustered by piece, because one movement can hold fifty tonic
chords. Examples are chosen one per piece before any of them is engraved, so two examples of
a triad are two different pieces rather than two bars of the same Dvořák — and only the ones
actually shown have their note tables read.

Eleven concepts have no examples and say so: notes, intervals, scales and key signatures are
not harmonic annotations, and neither counterpoint, altered dominants nor the twelve-bar form
is labelled distinctly by any corpus here.
