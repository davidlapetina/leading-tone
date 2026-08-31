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
