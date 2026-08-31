package fr.lapetina.music.llm;

import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordQuality;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.Inversion;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Correct, worked facts about a concept, computed from the theory engine and handed to
 * the model with the turn.
 *
 * <p>Tools exist so the model can look things up, but a model that is confident and wrong
 * does not look anything up: asked about a C major add 7, one answered "C, E, G and B♭",
 * which is a different chord. Putting the facts in front of it removes the opportunity.
 * Everything here is generated, never written out by hand, so it cannot drift from the
 * engine that marks the answers.
 */
@ApplicationScoped
public class TheoryBriefing {

    public String forConcept(String conceptId) {
        String facts = facts(conceptId);
        return facts.isEmpty() ? "" : """
                Correct facts for this concept, computed by the application. Use these; do not
                work the theory out yourself, and do not contradict them:
                %s""".formatted(facts);
    }

    private String facts(String conceptId) {
        return switch (conceptId) {
            case "note" -> """
                    - The seven letters are C D E F G A B; a sharp raises by a semitone, a flat lowers by one.
                    - Enharmonics: %s.
                    """.formatted(enharmonicExamples());
            case "interval" -> """
                    - An interval is a number plus a quality: %s.
                    - Same sound, different spelling: C to F# is %s, C to Gb is %s.
                    """.formatted(intervalExamples(), Interval.AUGMENTED_FOURTH.symbol(),
                    Interval.DIMINISHED_FIFTH.symbol());
            case "major-scale", "key-signature", "scale-degree" -> """
                    - C major: %s. D major: %s. Eb major: %s.
                    - Key signatures: D major %s, Eb major %s, B major %s.
                    """.formatted(spell(Scale.of("C", ScaleType.MAJOR)), spell(Scale.of("D", ScaleType.MAJOR)),
                    spell(Scale.of("Eb", ScaleType.MAJOR)), signature(Key.major("D")),
                    signature(Key.major("Eb")), signature(Key.major("B")));
            case "minor-scale" -> """
                    - A natural minor: %s.
                    - A harmonic minor: %s (raised seventh).
                    - A melodic minor ascending: %s.
                    """.formatted(spell(Scale.of("A", ScaleType.NATURAL_MINOR)),
                    spell(Scale.of("A", ScaleType.HARMONIC_MINOR)), spell(Scale.of("A", ScaleType.MELODIC_MINOR)));
            case "mode" -> """
                    - D dorian: %s. E phrygian: %s. F lydian: %s. G mixolydian: %s.
                    """.formatted(spell(Scale.of("D", ScaleType.DORIAN)), spell(Scale.of("E", ScaleType.PHRYGIAN)),
                    spell(Scale.of("F", ScaleType.LYDIAN)), spell(Scale.of("G", ScaleType.MIXOLYDIAN)));
            case "triad" -> """
                    - C major %s, C minor %s, C diminished %s, C augmented %s.
                    """.formatted(notes("C", ChordQuality.MAJOR), notes("C", ChordQuality.MINOR),
                    notes("C", ChordQuality.DIMINISHED), notes("C", ChordQuality.AUGMENTED));
            case "chord-inversion", "figured-bass" -> """
                    - G major root position %s, first inversion %s (figured 6), second inversion %s (figured 6/4).
                    - The inversion is decided by the lowest note, not by the order of the others.
                    - A seventh chord is figured 7, 6/5, 4/3, 4/2 from root position downwards.
                    """.formatted(notes("G", ChordQuality.MAJOR),
                    inverted("G", ChordQuality.MAJOR, Inversion.FIRST),
                    inverted("G", ChordQuality.MAJOR, Inversion.SECOND));
            case "seventh-chord" -> """
                    - Cmaj7 (also written C major 7 or C add 7) is %s. The seventh is B, a major seventh.
                    - C7, the dominant seventh, is %s. Its seventh is Bb, a minor seventh. It is a different chord.
                    - Cm7 %s, Cm7b5 %s, Cdim7 %s.
                    """.formatted(notes("C", ChordQuality.MAJOR_SEVENTH), notes("C", ChordQuality.DOMINANT_SEVENTH),
                    notes("C", ChordQuality.MINOR_SEVENTH), notes("C", ChordQuality.HALF_DIMINISHED_SEVENTH),
                    notes("C", ChordQuality.DIMINISHED_SEVENTH));
            case "diatonic-triads", "roman-numeral", "harmonic-function", "tonic-function",
                 "predominant-function", "dominant-function" -> """
                    - The triads of C major: %s, numbered I ii iii IV V vi vii°.
                    - The triads of A minor: %s.
                    - Tonic is I iii vi; predominant is ii IV; dominant is V vii°.
                    """.formatted(triads(Key.major("C")), triads(Key.minor("A")));
            case "dominant-seventh" -> """
                    - V7 in C major is %s; in D major %s; in A minor %s.
                    - Its tritone is the third and the seventh: in G7 those are B and F.
                    """.formatted(Key.major("C").dominantSeventh().symbol() + " " + notesOf(Key.major("C").dominantSeventh()),
                    Key.major("D").dominantSeventh().symbol() + " " + notesOf(Key.major("D").dominantSeventh()),
                    Key.minor("A").dominantSeventh().symbol() + " " + notesOf(Key.minor("A").dominantSeventh()));
            case "cadence" -> """
                    - In C major, %s
                    - In C major, %s
                    """.formatted(cadence("F", "G", "C"), cadence("G", "Am"));
            case "voice-leading" -> """
                    - G7 to C: the seventh F falls to E, and the leading tone B rises to C.
                    - Avoid parallel fifths and octaves between any two voices.
                    """;
            case "secondary-dominant" -> """
                    - In C major, V7/V is %s and it points at G. V/vi is %s and it points at Am.
                    - An applied dominant is a major triad or dominant seventh a fifth above its target.
                    """.formatted(secondary("C", 5), secondary("C", 6));
            case "modulation" -> """
                    - C major and G major share these triads, any of which can pivot: %s.
                    - A modulation is only confirmed by a cadence in the new key.
                    """.formatted(shared(Key.major("C"), Key.major("G")));
            default -> "";
        };
    }

    private static String enharmonicExamples() {
        return "F# = Gb, C# = Db, A# = Bb, E# = F, Cb = B";
    }

    private static String intervalExamples() {
        return List.of(Interval.MAJOR_THIRD, Interval.MINOR_THIRD, Interval.PERFECT_FIFTH,
                        Interval.MINOR_SEVENTH).stream()
                .map(interval -> interval.symbol() + " = " + interval.semitones() + " semitones")
                .collect(Collectors.joining(", "));
    }

    private static String spell(Scale scale) {
        return scale.pitchClasses().stream().map(PitchClass::name).collect(Collectors.joining(" "));
    }

    private static String signature(Key key) {
        int signature = key.keySignature();
        return signature == 0 ? "none" : Math.abs(signature) + (signature > 0 ? " sharps" : " flats");
    }

    private static String notes(String root, ChordQuality quality) {
        return notesOf(Chord.of(root, quality));
    }

    private static String inverted(String root, ChordQuality quality, Inversion inversion) {
        Chord chord = Chord.of(root, quality, inversion);
        return chord.notes(3).stream().map(note -> note.pitchClass().name()).collect(Collectors.joining(" "));
    }

    private static String notesOf(Chord chord) {
        return "is " + chord.pitchClasses().stream().map(PitchClass::name).collect(Collectors.joining(" "));
    }

    private static String triads(Key key) {
        return key.diatonicTriads().stream().map(Chord::symbol).collect(Collectors.joining(" "));
    }

    private static String cadence(String... symbols) {
        List<Chord> chords = List.of(symbols).stream()
                .map(fr.lapetina.music.theory.ChordAnalyzer::parse).toList();
        var analysis = ProgressionAnalyzer.analyze(chords, Key.major("C"));
        String names = analysis.cadences().isEmpty()
                ? "no cadence"
                : analysis.cadences().get(analysis.cadences().size() - 1).cadence().displayName();
        return "%s is %s (%s).".formatted(String.join(" - ", symbols), names, analysis.romanNumeralLine());
    }

    private static String secondary(String key, int degree) {
        Key parsed = Key.major(key);
        Chord target = parsed.triad(degree);
        Chord applied = Chord.of(target.root().transpose(Interval.PERFECT_FIFTH),
                degree == 5 ? ChordQuality.DOMINANT_SEVENTH : ChordQuality.MAJOR);
        return applied.symbol() + " " + notesOf(applied);
    }

    private static String shared(Key from, Key to) {
        List<String> destination = to.diatonicTriads().stream().map(Chord::symbol).toList();
        return from.diatonicTriads().stream()
                .map(Chord::symbol)
                .filter(destination::contains)
                .collect(Collectors.joining(" "));
    }
}
