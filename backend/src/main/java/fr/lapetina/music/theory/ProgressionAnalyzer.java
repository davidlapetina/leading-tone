package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns a chord sequence into Roman numerals, applied dominants and cadences.
 *
 * <p>This is the analysis the tutor trusts. The language model may narrate it but never
 * overrides it.
 */
public final class ProgressionAnalyzer {

    private ProgressionAnalyzer() {
    }

    public static ProgressionAnalysis analyze(List<Chord> chords, Key key) {
        List<ChordAnalysis> analyses = new ArrayList<>(chords.size());
        for (Chord chord : chords) {
            analyses.add(analyzeChord(chord, key));
        }
        return new ProgressionAnalysis(key, List.copyOf(analyses), findCadences(analyses));
    }

    public static ChordAnalysis analyzeChord(Chord chord, Key key) {
        Optional<Integer> diatonicDegree = diatonicDegreeOf(chord, key);
        if (diatonicDegree.isPresent()) {
            RomanNumeral numeral = RomanNumeral.of(diatonicDegree.get(), chord.quality(), chord.inversion());
            return new ChordAnalysis(chord, numeral, numeral.function(), true);
        }
        Optional<RomanNumeral> applied = appliedChord(chord, key);
        if (applied.isPresent()) {
            return new ChordAnalysis(chord, applied.get(), HarmonicFunction.APPLIED_DOMINANT, false);
        }
        Optional<RomanNumeral> chromatic = chromaticChord(chord, key);
        if (chromatic.isPresent()) {
            return new ChordAnalysis(chord, chromatic.get(), HarmonicFunction.PREDOMINANT, false);
        }
        return new ChordAnalysis(chord, null, HarmonicFunction.CHROMATIC, false);
    }

    /**
     * The chromatic chords worth naming: the augmented sixths, the Neapolitan, and the
     * triads borrowed from the parallel minor.
     *
     * <p>Deliberately a closed list. The alternative -- calling any chromatic root by a
     * numeral -- would let the analyser put a confident label on something it has not
     * actually understood, and a wrong label is worse than a question mark. E flat minor in
     * C major is left unexplained, because no rule here honestly claims it.
     */
    private static Optional<RomanNumeral> chromaticChord(Chord chord, Key key) {
        Scale major = new Scale(key.tonic(), ScaleType.MAJOR);
        int root = chord.root().semitone();
        int flatTwo = major.degree(2).alter(-1).semitone();
        int flatThree = major.degree(3).alter(-1).semitone();
        int flatSix = major.degree(6).alter(-1).semitone();
        int flatSeven = major.degree(7).alter(-1).semitone();

        if (root == flatSix && chord.quality().isAugmentedSixth()) {
            return Optional.of(RomanNumeral.of(Accidental.FLAT, 6, chord.quality(), Inversion.ROOT_POSITION));
        }
        if (root == flatTwo && chord.quality() == ChordQuality.MAJOR) {
            return Optional.of(RomanNumeral.of(Accidental.FLAT, 2, ChordQuality.MAJOR, chord.inversion()));
        }
        // Borrowed from the parallel minor. Major quality is the whole guard here: E flat
        // MINOR on flat-three is not a borrowing anyone writes, and must stay unexplained.
        if (key.mode() == Mode.MAJOR && chord.quality() == ChordQuality.MAJOR) {
            if (root == flatThree) {
                return Optional.of(RomanNumeral.of(Accidental.FLAT, 3, ChordQuality.MAJOR, chord.inversion()));
            }
            if (root == flatSix) {
                return Optional.of(RomanNumeral.of(Accidental.FLAT, 6, ChordQuality.MAJOR, chord.inversion()));
            }
            if (root == flatSeven) {
                return Optional.of(RomanNumeral.of(Accidental.FLAT, 7, ChordQuality.MAJOR, chord.inversion()));
            }
        }
        return Optional.empty();
    }

    /** Matches a chord against every diatonic triad and seventh, including the raised-seventh forms. */
    private static Optional<Integer> diatonicDegreeOf(Chord chord, Key key) {
        for (int degree = 1; degree <= 7; degree++) {
            for (boolean raised : new boolean[]{false, true}) {
                if (matches(chord, safeTriad(key, degree, raised))
                        || matches(chord, safeSeventh(key, degree, raised))) {
                    return Optional.of(degree);
                }
            }
        }
        return Optional.empty();
    }

    private static Chord safeTriad(Key key, int degree, boolean raised) {
        try {
            return key.triad(degree, raised);
        } catch (RuntimeException notATriad) {
            return null;
        }
    }

    private static Chord safeSeventh(Key key, int degree, boolean raised) {
        try {
            return key.seventh(degree, raised);
        } catch (RuntimeException notASeventh) {
            return null;
        }
    }

    private static boolean matches(Chord chord, Chord diatonic) {
        return diatonic != null
                && chord.quality() == diatonic.quality()
                && chord.root().semitone() == diatonic.root().semitone();
    }

    /**
     * Recognises applied chords: a dominant-functioning chord that points at a scale
     * degree other than the tonic, such as V7/V or vii°7/ii.
     */
    private static Optional<RomanNumeral> appliedChord(Chord chord, Key key) {
        for (int target = 2; target <= 7; target++) {
            Chord targetChord = safeTriad(key, target, target == 5 || target == 7);
            if (targetChord == null || targetChord.quality() == ChordQuality.DIMINISHED) {
                continue;
            }
            PitchClass targetRoot = targetChord.root();
            String targetSymbol = RomanNumeral.numeralFor(target, targetChord.quality() == ChordQuality.MAJOR);

            int dominantSemitone = Math.floorMod(targetRoot.semitone() + 7, 12);
            if (chord.root().semitone() == dominantSemitone
                    && (chord.quality() == ChordQuality.MAJOR || chord.quality() == ChordQuality.DOMINANT_SEVENTH)) {
                return Optional.of(RomanNumeral.applied(5, chord.quality(), chord.inversion(), targetSymbol));
            }

            int leadingToneSemitone = Math.floorMod(targetRoot.semitone() - 1, 12);
            if (chord.root().semitone() == leadingToneSemitone
                    && (chord.quality() == ChordQuality.DIMINISHED
                        || chord.quality() == ChordQuality.DIMINISHED_SEVENTH
                        || chord.quality() == ChordQuality.HALF_DIMINISHED_SEVENTH)) {
                return Optional.of(RomanNumeral.applied(7, chord.quality(), chord.inversion(), targetSymbol));
            }
        }
        return Optional.empty();
    }

    private static List<CadencePoint> findCadences(List<ChordAnalysis> analyses) {
        List<CadencePoint> points = new ArrayList<>();
        for (int i = 0; i < analyses.size() - 1; i++) {
            boolean atEnd = i + 1 == analyses.size() - 1;
            Cadence cadence = cadenceBetween(analyses.get(i), analyses.get(i + 1), atEnd);
            if (cadence != Cadence.NONE) {
                points.add(new CadencePoint(i, cadence));
            }
        }
        return List.copyOf(points);
    }

    /**
     * {@code atEnd} matters for half cadences: arriving on V only ends a phrase when the
     * music stops there. A V passed through on the way to I is not a cadence.
     */
    private static Cadence cadenceBetween(ChordAnalysis first, ChordAnalysis second, boolean atEnd) {
        RomanNumeral from = first.romanNumeral();
        RomanNumeral to = second.romanNumeral();
        if (from == null || to == null || from.isSecondary()) {
            return Cadence.NONE;
        }
        // A chromatic numeral carries a degree, and comparing that degree to a diatonic one
        // would read V -> Ger+6 as a deceptive cadence, because a German sixth sits on the
        // sixth degree. Cadence claims are only made about diatonic chords.
        if (from.isChromatic() || to.isChromatic()) {
            return Cadence.NONE;
        }
        boolean dominantFirst = !from.isSecondary() && (from.degree() == 5 || from.degree() == 7);
        if (dominantFirst && to.degree() == 1 && !to.isSecondary()) {
            boolean bothRootPosition = from.inversion() == Inversion.ROOT_POSITION
                    && to.inversion() == Inversion.ROOT_POSITION;
            return bothRootPosition ? Cadence.PERFECT_AUTHENTIC : Cadence.IMPERFECT_AUTHENTIC;
        }
        if (from.degree() == 4 && to.degree() == 1 && !to.isSecondary()) {
            return Cadence.PLAGAL;
        }
        if (from.degree() == 5 && to.degree() == 6 && !to.isSecondary()) {
            return Cadence.DECEPTIVE;
        }
        if (atEnd && to.degree() == 5 && !to.isSecondary() && to.inversion() == Inversion.ROOT_POSITION) {
            return Cadence.HALF;
        }
        return Cadence.NONE;
    }
}
