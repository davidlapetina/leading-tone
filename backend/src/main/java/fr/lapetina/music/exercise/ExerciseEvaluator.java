package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceResult;
import fr.lapetina.music.midi.MidiEvaluation;
import fr.lapetina.music.midi.MidiEvaluator;
import fr.lapetina.music.midi.MidiPerformance;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Mode;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether an answer is right.
 *
 * <p>Every path through this class except {@link ExpectedAnswerKind#EXPLANATION} is
 * deterministic. The language model reads the verdict; it does not produce it.
 */
@ApplicationScoped
public class ExerciseEvaluator {

    @Inject
    MidiEvaluator midiEvaluator;

    public EvaluationOutcome evaluateText(ExpectedAnswer expected, String answer, String keyContext) {
        if (answer == null || answer.isBlank()) {
            return new EvaluationOutcome(EvidenceResult.SKIPPED, "Nothing was answered.",
                    null, null, null, 1.0, false);
        }
        if (AnswerNormalizer.isDontKnow(answer)) {
            return new EvaluationOutcome(EvidenceResult.SKIPPED,
                    "Said they did not know, which is worth knowing but is not a wrong answer.",
                    null, null, null, 1.0, false);
        }
        return switch (expected.kind()) {
            case TEXT -> matchText(expected, answer);
            case NOTE_SET -> matchNotes(expected, answer, false);
            case NOTE_SEQUENCE -> matchNotes(expected, answer, true);
            case EXPLANATION -> EvaluationOutcome.needsJudgement(
                    "A free explanation: this needs reading rather than checking.");
            case MIDI_CHORD, MIDI_SCALE, MIDI_NOTES -> matchPlayedAnswerTypedAsText(expected, answer, keyContext);
        };
    }

    public EvaluationOutcome evaluateMidi(ExpectedAnswer expected, MidiPerformance performance, String keyContext) {
        Key key = parseKey(keyContext);
        MidiEvaluation evaluation = switch (expected.kind()) {
            case MIDI_CHORD -> midiEvaluator.evaluateChord(ChordAnalyzer.parse(expected.chordSymbol()),
                    performance, key);
            case MIDI_SCALE -> midiEvaluator.evaluateScale(
                    new Scale(PitchClass.parse(expected.scaleTonic()), ScaleType.valueOf(expected.scaleType())),
                    performance);
            case MIDI_NOTES, NOTE_SET, NOTE_SEQUENCE -> midiEvaluator.evaluateNotes(
                    expected.noteNames().stream().map(name -> new Note(PitchClass.parse(name), 4)).toList(),
                    performance, key);
            case TEXT, EXPLANATION -> null;
        };
        if (evaluation == null) {
            return EvaluationOutcome.needsJudgement("This exercise expects a typed answer, not notes.");
        }
        EvaluationOutcome outcome = new EvaluationOutcome(evaluation.result(), evaluation.feedback(),
                null, evaluation.misconceptionCode(), evaluation.misconceptionDescription(), 1.0, false);
        return outcome;
    }

    /** A learner may type the notes of a keyboard exercise instead of playing them: still checkable. */
    private EvaluationOutcome matchPlayedAnswerTypedAsText(ExpectedAnswer expected, String answer, String keyContext) {
        List<PitchClass> typed = AnswerNormalizer.notesIn(answer);
        if (typed.isEmpty()) {
            return EvaluationOutcome.incorrect("No notes recognised in that answer.");
        }
        Set<Integer> expectedSemitones = expectedSemitones(expected);
        Set<Integer> given = new LinkedHashSet<>();
        typed.forEach(pitchClass -> given.add(pitchClass.semitone()));
        if (expectedSemitones.equals(given)) {
            return EvaluationOutcome.partial(
                    "Right notes. Play it on the keyboard when you can — it is worth more.");
        }
        return EvaluationOutcome.incorrect("Expected %s.".formatted(expected.canonical()));
    }

    private Set<Integer> expectedSemitones(ExpectedAnswer expected) {
        Set<Integer> semitones = new LinkedHashSet<>();
        if (expected.chordSymbol() != null) {
            Chord chord = ChordAnalyzer.parse(expected.chordSymbol());
            chord.pitchClasses().forEach(pitchClass -> semitones.add(pitchClass.semitone()));
        } else if (expected.scaleTonic() != null) {
            new Scale(PitchClass.parse(expected.scaleTonic()), ScaleType.valueOf(expected.scaleType()))
                    .pitchClasses().forEach(pitchClass -> semitones.add(pitchClass.semitone()));
        } else if (expected.noteNames() != null) {
            expected.noteNames().forEach(name -> semitones.add(PitchClass.parse(name).semitone()));
        }
        return semitones;
    }

    private EvaluationOutcome matchText(ExpectedAnswer expected, String answer) {
        List<String> candidates = new ArrayList<>();
        candidates.add(expected.canonical());
        candidates.addAll(expected.acceptable());
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && AnswerNormalizer.matches(answer, candidate)) {
                return EvaluationOutcome.correct("Correct.");
            }
        }
        // A single spelled note that is enharmonically right is worth partial credit.
        Optional<EvaluationOutcome> enharmonic = enharmonicNearMiss(expected.canonical(), answer);
        return enharmonic.orElseGet(() -> EvaluationOutcome.incorrect(
                "Expected %s.".formatted(expected.canonical())));
    }

    private Optional<EvaluationOutcome> enharmonicNearMiss(String canonical, String answer) {
        List<PitchClass> expectedNotes = AnswerNormalizer.notesIn(canonical);
        List<PitchClass> givenNotes = AnswerNormalizer.notesIn(answer);
        if (expectedNotes.size() != 1 || givenNotes.size() != 1) {
            return Optional.empty();
        }
        PitchClass expected = expectedNotes.get(0);
        PitchClass given = givenNotes.get(0);
        if (!expected.equals(given) && expected.isEnharmonicWith(given)) {
            return Optional.of(EvaluationOutcome.partial(
                    "That is the right key on the piano, but in this context it is spelled %s, not %s."
                            .formatted(expected.name(), given.name())));
        }
        return Optional.empty();
    }

    private EvaluationOutcome matchNotes(ExpectedAnswer expected, String answer, boolean ordered) {
        List<PitchClass> given = AnswerNormalizer.notesIn(answer);
        List<PitchClass> target = expected.noteNames().stream().map(PitchClass::parse).toList();

        if (ordered ? given.equals(target) : new LinkedHashSet<>(given).equals(new LinkedHashSet<>(target))) {
            return EvaluationOutcome.correct("Correct.");
        }
        boolean sameSounds = sameSemitones(given, target, ordered);
        if (sameSounds) {
            return EvaluationOutcome.partial(
                    "Those are the right sounds, but the spelling should be %s.".formatted(expected.canonical()));
        }
        List<String> missing = target.stream()
                .filter(note -> given.stream().noneMatch(note::isEnharmonicWith))
                .map(PitchClass::name)
                .toList();
        String feedback = missing.isEmpty()
                ? "Expected %s.".formatted(expected.canonical())
                : "Expected %s. Missing: %s.".formatted(expected.canonical(), String.join(", ", missing));
        return EvaluationOutcome.incorrect(feedback);
    }

    private static boolean sameSemitones(List<PitchClass> given, List<PitchClass> target, boolean ordered) {
        if (ordered) {
            if (given.size() != target.size()) {
                return false;
            }
            for (int i = 0; i < given.size(); i++) {
                if (given.get(i).semitone() != target.get(i).semitone()) {
                    return false;
                }
            }
            return true;
        }
        Set<Integer> givenSemitones = new LinkedHashSet<>();
        given.forEach(note -> givenSemitones.add(note.semitone()));
        Set<Integer> targetSemitones = new LinkedHashSet<>();
        target.forEach(note -> targetSemitones.add(note.semitone()));
        return givenSemitones.equals(targetSemitones);
    }

    /** Key context is stored as "D major" / "F# minor"; absent for exercises with no key. */
    public static Key parseKey(String keyContext) {
        if (keyContext == null || keyContext.isBlank()) {
            return null;
        }
        // Deliberately stricter than Key.parse: this reads a key stored with an exercise,
        // which is always written in full ("C major"), and a half-recognised key here would
        // mark an answer against the wrong scale. Nothing beats null for that.
        if (keyContext.trim().split("\\s+").length < 2) {
            return null;
        }
        return Key.tryParse(keyContext).orElse(null);
    }
}
