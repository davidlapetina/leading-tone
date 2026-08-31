package fr.lapetina.music.midi;

import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Inversion;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Scale;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Judges what was played against what was asked for, without a language model anywhere
 * near the decision.
 *
 * <p>The point of the detail here is the difference between "wrong" and "correct notes,
 * but listen to the bass" — the second is teachable, and it is only available because
 * this check is deterministic.
 */
@ApplicationScoped
public class MidiEvaluator {

    /** Misconception codes, referenced by the teaching policy when they repeat. */
    public static final String ROOT_POSITION_DEFAULT = "plays-root-position-when-inversion-asked";
    public static final String OMITS_SEVENTH = "omits-the-seventh-of-a-seventh-chord";
    public static final String WRONG_CHORD_QUALITY = "confuses-chord-quality";
    public static final String UNRAISED_LEADING_TONE = "does-not-raise-the-leading-tone-in-minor";
    public static final String WRONG_SCALE_DEGREE = "wrong-note-in-scale";

    public MidiEvaluation evaluateChord(Chord expected, MidiPerformance performance, Key keyContext) {
        if (performance.isEmpty()) {
            return silence(expected.symbol());
        }
        Set<Integer> expectedPitchClasses = new TreeSet<>(expected.semitoneSet());
        Set<Integer> playedPitchClasses = new TreeSet<>(performance.pitchClasses());
        boolean correctPitchClasses = expectedPitchClasses.equals(playedPitchClasses);
        boolean correctBass = performance.bassPitchClass() == expected.bass().semitone();

        List<String> missing = spellAll(difference(expectedPitchClasses, playedPitchClasses), keyContext);
        List<String> extra = spellAll(difference(playedPitchClasses, expectedPitchClasses), keyContext);
        String detected = ChordAnalyzer.fromMidi(performance.notes(), keyContext)
                .map(Chord::symbol)
                .orElse("no recognisable chord");

        if (correctPitchClasses && correctBass) {
            return new MidiEvaluation(EvidenceResult.CORRECT, true, true, List.of(), List.of(),
                    expected.symbol(), detected, "Correct.", null, null);
        }

        if (correctPitchClasses) {
            boolean playedRootInBass = performance.bassPitchClass() == expected.root().semitone();
            boolean inversionWasAsked = expected.inversion() != Inversion.ROOT_POSITION;
            String code = playedRootInBass && inversionWasAsked ? ROOT_POSITION_DEFAULT : null;
            String description = code == null ? null
                    : "Plays the right chord but puts the root in the bass when an inversion was asked for.";
            return new MidiEvaluation(EvidenceResult.PARTIALLY_CORRECT, true, false, List.of(), List.of(),
                    expected.symbol(), detected,
                    "The right notes, but the bass should be %s and you played %s at the bottom."
                            .formatted(expected.bass().name(), spell(performance.bassPitchClass(), keyContext)),
                    code, description);
        }

        Optional<String> namedMistake = diagnose(expected, performance, playedPitchClasses);
        String feedback = buildFeedback(expected, missing, extra, detected);
        return new MidiEvaluation(EvidenceResult.INCORRECT, false, correctBass, missing, extra,
                expected.symbol(), detected, feedback,
                namedMistake.orElse(null), namedMistake.map(this::describe).orElse(null));
    }

    /** Recognises the specific wrong answers that are worth naming. */
    private Optional<String> diagnose(Chord expected, MidiPerformance performance, Set<Integer> played) {
        if (expected.quality().isSeventh()) {
            Set<Integer> triadOnly = new LinkedHashSet<>();
            List<PitchClass> members = expected.pitchClasses();
            for (int i = 0; i < 3; i++) {
                triadOnly.add(members.get(i).semitone());
            }
            if (played.equals(triadOnly)) {
                return Optional.of(OMITS_SEVENTH);
            }
        }
        boolean sameRootInBass = performance.bassPitchClass() == expected.root().semitone();
        Optional<Chord> detected = ChordAnalyzer.fromMidi(performance.notes());
        if (sameRootInBass && detected.isPresent()
                && detected.get().root().semitone() == expected.root().semitone()
                && detected.get().quality() != expected.quality()) {
            return Optional.of(WRONG_CHORD_QUALITY);
        }
        return Optional.empty();
    }

    private String describe(String code) {
        return switch (code) {
            case OMITS_SEVENTH -> "Plays the triad and leaves out the seventh that gives the chord its pull.";
            case WRONG_CHORD_QUALITY -> "Builds the chord on the right root but with the wrong third or fifth.";
            case ROOT_POSITION_DEFAULT -> "Defaults to root position when an inversion was asked for.";
            case UNRAISED_LEADING_TONE -> "Uses the natural seventh in a minor key where it should be raised.";
            default -> "Unrecognised mistake.";
        };
    }

    /** Checks an ascending scale note by note, and reports where it first went wrong. */
    public MidiEvaluation evaluateScale(Scale expected, MidiPerformance performance) {
        if (performance.isEmpty()) {
            return silence(expected.name());
        }
        List<Integer> expectedSequence = new ArrayList<>();
        for (Note note : expected.notes(4)) {
            expectedSequence.add(Math.floorMod(note.midi(), 12));
        }
        List<Integer> played = performance.pitchClassSequence();

        // The closing tonic is optional: a scale played to the seventh still counts.
        List<Integer> target = played.size() == expectedSequence.size() - 1
                ? expectedSequence.subList(0, expectedSequence.size() - 1)
                : expectedSequence;

        if (played.equals(target)) {
            return new MidiEvaluation(EvidenceResult.CORRECT, true, true, List.of(), List.of(),
                    expected.name(), expected.name(), "Correct.", null, null);
        }

        int divergence = firstDivergence(target, played);
        String expectedNote = divergence < target.size()
                ? expected.pitchClasses().get(Math.min(divergence, expected.pitchClasses().size() - 1)).name()
                : "the tonic";
        String playedNote = divergence < played.size()
                ? spell(played.get(divergence), null)
                : "nothing";
        String code = unraisedLeadingTone(expected, target, played, divergence) ? UNRAISED_LEADING_TONE
                : WRONG_SCALE_DEGREE;

        return new MidiEvaluation(EvidenceResult.INCORRECT, false, true, List.of(expectedNote), List.of(playedNote),
                expected.name(), "a different scale",
                "Degree %d should be %s; you played %s.".formatted(divergence + 1, expectedNote, playedNote),
                code, describe(code));
    }

    private boolean unraisedLeadingTone(Scale expected, List<Integer> target, List<Integer> played, int divergence) {
        if (divergence != 6 || played.size() <= 6 || target.size() <= 6) {
            return false;
        }
        return Math.floorMod(target.get(6) - played.get(6), 12) == 1;
    }

    /** Checks a set of notes with no inversion requirement, such as "play a major third above D". */
    public MidiEvaluation evaluateNotes(List<Note> expected, MidiPerformance performance, Key keyContext) {
        if (performance.isEmpty()) {
            return silence(expected.stream().map(Note::name).reduce((a, b) -> a + " " + b).orElse(""));
        }
        Set<Integer> expectedPitchClasses = new TreeSet<>();
        for (Note note : expected) {
            expectedPitchClasses.add(note.pitchClass().semitone());
        }
        Set<Integer> played = new TreeSet<>(performance.pitchClasses());
        String expectedLabel = expected.stream().map(note -> note.pitchClass().name())
                .reduce((a, b) -> a + " " + b).orElse("");

        if (expectedPitchClasses.equals(played)) {
            return new MidiEvaluation(EvidenceResult.CORRECT, true, true, List.of(), List.of(),
                    expectedLabel, expectedLabel, "Correct.", null, null);
        }
        List<String> missing = spellAll(difference(expectedPitchClasses, played), keyContext);
        List<String> extra = spellAll(difference(played, expectedPitchClasses), keyContext);
        return new MidiEvaluation(EvidenceResult.INCORRECT, false, false, missing, extra,
                expectedLabel, spellAll(played, keyContext).toString(),
                buildFeedback(expectedLabel, missing, extra), null, null);
    }

    private static int firstDivergence(List<Integer> expected, List<Integer> played) {
        int limit = Math.min(expected.size(), played.size());
        for (int i = 0; i < limit; i++) {
            if (!expected.get(i).equals(played.get(i))) {
                return i;
            }
        }
        return limit;
    }

    private MidiEvaluation silence(String expected) {
        return new MidiEvaluation(EvidenceResult.SKIPPED, false, false, List.of(), List.of(),
                expected, "nothing", "Nothing was played.", null, null);
    }

    private String buildFeedback(Chord expected, List<String> missing, List<String> extra, String detected) {
        return buildFeedback(expected.symbol() + " (" + expected.describe() + ")", missing, extra)
                + " That sounds like " + detected + ".";
    }

    private String buildFeedback(String expectedLabel, List<String> missing, List<String> extra) {
        StringBuilder builder = new StringBuilder("Expected " + expectedLabel + ".");
        if (!missing.isEmpty()) {
            builder.append(" Missing: ").append(String.join(", ", missing)).append('.');
        }
        if (!extra.isEmpty()) {
            builder.append(" Not in the chord: ").append(String.join(", ", extra)).append('.');
        }
        return builder.toString();
    }

    private static Set<Integer> difference(Set<Integer> from, Set<Integer> remove) {
        Set<Integer> result = new TreeSet<>(from);
        result.removeAll(remove);
        return result;
    }

    private List<String> spellAll(Set<Integer> semitones, Key keyContext) {
        return semitones.stream().map(semitone -> spell(semitone, keyContext)).toList();
    }

    private String spell(int semitone, Key keyContext) {
        return ChordAnalyzer.spellingCandidates(semitone, keyContext).get(0).name();
    }
}
