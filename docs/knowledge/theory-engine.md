# The theory engine

Records and enums, four static analyzer classes, and **no framework imports at all**. There
is a test that walks the package and fails if a `jakarta.`, `io.quarkus`, `dev.langchain4j`
or `org.apache.lucene` import appears in it.

That is worth enforcing because this is the part that marks a learner's answers. It should
be reasonable about, testable and trustworthy on its own, with no opinion about HTTP,
persistence or language models. The CDI façade, `MusicTheoryService`, sits in a sibling
package so the claim stays literally true.

## Spelling is the point

`PitchClass` is a letter and an accidental, deliberately not reduced to twelve values,
because F♯ and G♭ behave differently in analysis even though both sound as 6.

That distinction is carried everywhere and tested explicitly rather than by pitch-class
equality:

```
F# major          F# G# A# B C# D# E#      not F, and not G flat
G altered         G Ab Bb Cb Db Eb F       a diminished fourth, so C flat rather than B
C whole tone      C D E F# G# A#           an augmented sixth, so A sharp rather than B flat
Ger+6 in C        Ab C Eb F#               the F sharp is what makes it not an Ab7
V7/V in C major   D F# A C
```

## Roman numerals

`RomanNumeral.parse` reads numerals as they are actually written — in textbooks and in
corpus annotations alike: `V7/V`, `viiø7`, `I64`, `bII6`, `Ger+6`, `#iv°`, and the corpus
spellings `viio7` and `Ger6`.

**The accidental convention**, which is the part that is easy to get wrong: a numeral with
no accidental names the key's own scale degree, so `III` in C minor is E♭. A numeral *with*
an accidental is measured from the **major** scale of the tonic, so `bVI` is A♭ in C major
and A♭ in C minor alike. The other reading — lowering the key's own degree — would make
flat-six in C minor an A double flat, which is not what anybody writing `bVI` means.

An applied chord is realised by standing in the key it points at: `V7/V` in C means the
dominant seventh *of G major*, which is D F♯ A C.

## Refusing to guess

When the analyser meets a chord no rule honestly explains, it prints `?` rather than
inventing a numeral.

```
C  Eb    →  I - bIII    E flat MAJOR is genuinely borrowed from C minor
C  Ebm   →  I - ?       E flat MINOR is not a chord anyone writes in C major
C  Ab7   →  I - ?       sounds identical to Ger+6, but is spelled Ab C Eb Gb
```

The chromatic recogniser works from a **closed list** — the augmented sixths, the
Neapolitan, and the triads borrowed from the parallel minor. A rule of the form "any
chromatic root with any quality" would let it put a confident label on something it has not
understood, and a wrong label is worse than a question mark. The only thing separating the
first two rows above is a chord-quality check; it is commented as such.
