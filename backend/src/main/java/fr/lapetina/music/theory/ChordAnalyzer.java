package fr.lapetina.music.theory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Identifies chords from spelled pitch classes or from raw MIDI numbers.
 *
 * <p>MIDI arrives with no spelling at all, so identification happens in semitone space
 * and a spelling is chosen afterwards, biased by a key when one is known.
 */
public final class ChordAnalyzer {

    private static final String[] DEFAULT_SPELLING = {
            "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
    };

    private ChordAnalyzer() {
    }

    /** Identifies a root-position chord from an already-spelled member set. */
    public static Optional<Chord> fromPitchClasses(Set<PitchClass> members) {
        for (PitchClass candidate : members) {
            Optional<ChordQuality> quality = ChordQuality.identify(candidate, members);
            if (quality.isPresent()) {
                return Optional.of(Chord.of(candidate, quality.get()));
            }
        }
        return Optional.empty();
    }

    /** Identifies a chord from sounding notes, using the lowest note to decide the inversion. */
    public static Optional<Chord> fromNotes(List<Note> notes) {
        if (notes.isEmpty()) {
            return Optional.empty();
        }
        List<Note> sorted = new ArrayList<>(notes);
        sorted.sort(Comparator.naturalOrder());
        Set<PitchClass> members = new LinkedHashSet<>();
        for (Note note : sorted) {
            members.add(note.pitchClass());
        }
        PitchClass bass = sorted.get(0).pitchClass();
        for (PitchClass candidate : members) {
            Optional<ChordQuality> quality = ChordQuality.identify(candidate, members);
            if (quality.isPresent()) {
                Chord rootPosition = Chord.of(candidate, quality.get());
                int bassIndex = rootPosition.pitchClasses().indexOf(bass);
                return Optional.of(rootPosition.inverted(Inversion.ofIndex(Math.max(bassIndex, 0))));
            }
        }
        return Optional.empty();
    }

    public static Optional<Chord> fromMidi(List<Integer> midiNotes) {
        return fromMidi(midiNotes, null);
    }

    /**
     * Identifies a chord from MIDI note numbers. Octave doubling is ignored; the lowest
     * note determines the inversion.
     */
    public static Optional<Chord> fromMidi(List<Integer> midiNotes, Key keyContext) {
        if (midiNotes == null || midiNotes.isEmpty()) {
            return Optional.empty();
        }
        int bassSemitone = Math.floorMod(midiNotes.stream().min(Integer::compareTo).orElseThrow(), 12);
        Set<Integer> semitones = new TreeSet<>();
        for (int midi : midiNotes) {
            semitones.add(Math.floorMod(midi, 12));
        }

        List<Integer> rootCandidates = new ArrayList<>();
        rootCandidates.add(bassSemitone);
        for (int semitone : semitones) {
            if (semitone != bassSemitone) {
                rootCandidates.add(semitone);
            }
        }

        for (int rootSemitone : rootCandidates) {
            Set<Integer> intervals = new LinkedHashSet<>();
            for (int semitone : semitones) {
                intervals.add(Math.floorMod(semitone - rootSemitone, 12));
            }
            for (ChordQuality quality : ChordQuality.values()) {
                if (quality.size() != semitones.size() || !quality.semitonesAboveRoot().equals(intervals)) {
                    continue;
                }
                Optional<Chord> spelled = spell(rootSemitone, quality, bassSemitone, keyContext);
                if (spelled.isPresent()) {
                    return spelled;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Chord> spell(int rootSemitone, ChordQuality quality, int bassSemitone, Key keyContext) {
        for (PitchClass root : spellingCandidates(rootSemitone, keyContext)) {
            try {
                Chord chord = Chord.of(root, quality);
                List<PitchClass> members = chord.pitchClasses();
                int bassIndex = -1;
                for (int i = 0; i < members.size(); i++) {
                    if (members.get(i).semitone() == bassSemitone) {
                        bassIndex = i;
                        break;
                    }
                }
                if (bassIndex < 0) {
                    continue;
                }
                return Optional.of(chord.inverted(Inversion.ofIndex(bassIndex)));
            } catch (IllegalArgumentException tooManyAccidentals) {
                // This spelling of the root would need triple accidentals; try the next one.
            }
        }
        return Optional.empty();
    }

    /** Spellings for a semitone, best first: the key's own spelling, then the common default. */
    public static List<PitchClass> spellingCandidates(int semitone, Key keyContext) {
        int normalized = Math.floorMod(semitone, 12);
        List<PitchClass> candidates = new ArrayList<>();
        if (keyContext != null) {
            for (PitchClass pitchClass : keyContext.scale(true).pitchClasses()) {
                if (pitchClass.semitone() == normalized && !candidates.contains(pitchClass)) {
                    candidates.add(pitchClass);
                }
            }
        }
        PitchClass fallback = PitchClass.parse(DEFAULT_SPELLING[normalized]);
        if (!candidates.contains(fallback)) {
            candidates.add(fallback);
        }
        for (NoteLetter letter : NoteLetter.values()) {
            int offset = normalized - letter.semitone();
            if (offset > 6) {
                offset -= 12;
            } else if (offset < -6) {
                offset += 12;
            }
            if (Math.abs(offset) <= 2) {
                PitchClass candidate = new PitchClass(letter, Accidental.ofOffset(offset));
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    /** Parses lead-sheet symbols such as {@code C}, {@code Cmaj7}, {@code F#m7b5} or {@code G/B}. */
    public static Chord parse(String symbol) {
        String trimmed = symbol.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty chord symbol");
        }
        String bassPart = null;
        int slash = trimmed.indexOf('/');
        if (slash >= 0) {
            bassPart = trimmed.substring(slash + 1).trim();
            trimmed = trimmed.substring(0, slash).trim();
        }

        int accidentalEnd = 1;
        while (accidentalEnd < trimmed.length() && (trimmed.charAt(accidentalEnd) == '#'
                || trimmed.charAt(accidentalEnd) == 'b' || trimmed.charAt(accidentalEnd) == 'x')) {
            accidentalEnd++;
        }
        // "Bb7" is a Bb chord, "Bm7b5" is a B chord: take the longest root that leaves a
        // quality we recognise, then shorten it if that fails.
        Chord chord = null;
        for (int split = accidentalEnd; split >= 1 && chord == null; split--) {
            PitchClass root;
            try {
                root = PitchClass.parse(trimmed.substring(0, split));
            } catch (IllegalArgumentException notARoot) {
                continue;
            }
            String qualityText = trimmed.substring(split).trim();
            Optional<ChordQuality> quality = qualityText.isEmpty()
                    ? Optional.of(ChordQuality.MAJOR)
                    : ChordQuality.parseSymbol(qualityText);
            if (quality.isPresent()) {
                chord = Chord.of(root, quality.get());
            }
        }
        if (chord == null) {
            throw new IllegalArgumentException("Unparseable chord symbol: " + symbol);
        }
        if (bassPart == null) {
            return chord;
        }
        PitchClass bass = PitchClass.parse(bassPart);
        List<PitchClass> members = chord.pitchClasses();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).semitone() == bass.semitone()) {
                return chord.inverted(Inversion.ofIndex(i));
            }
        }
        throw new IllegalArgumentException(bass.name() + " is not a chord tone of " + chord.symbol());
    }
}
