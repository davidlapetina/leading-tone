package fr.lapetina.music.exercise;

import fr.lapetina.music.learner.EvidenceType;
import fr.lapetina.music.theory.AbcNotation;
import fr.lapetina.music.theory.Cadence;
import fr.lapetina.music.theory.CounterpointAnalyzer;
import fr.lapetina.music.theory.Motion;
import fr.lapetina.music.theory.CadencePoint;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalysis;
import fr.lapetina.music.theory.ChordQuality;
import fr.lapetina.music.theory.HarmonicFunction;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.IntervalQuality;
import fr.lapetina.music.theory.Inversion;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Mode;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.ProgressionAnalysis;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleDegree;
import fr.lapetina.music.theory.ScaleType;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builds exercises on demand from the theory engine.
 *
 * <p>Nothing here is a stored question. Every prompt and every expected answer is
 * computed, which is what lets the tutor ask for exactly the right thing at exactly the
 * right difficulty rather than picking from a bank.
 */
@ApplicationScoped
public class ExerciseGenerator {

    private static final List<Key> EASY_KEYS = List.of(
            Key.major("C"), Key.major("G"), Key.major("F"), Key.minor("A"), Key.minor("E"), Key.minor("D"));
    private static final List<Key> MEDIUM_KEYS = List.of(
            Key.major("D"), Key.major("Bb"), Key.major("A"), Key.major("Eb"), Key.major("E"),
            Key.minor("B"), Key.minor("G"), Key.minor("C"), Key.minor("F#"));
    private static final List<Key> HARD_KEYS = List.of(
            Key.major("B"), Key.major("Ab"), Key.major("Db"), Key.major("F#"),
            Key.minor("C#"), Key.minor("Bb"), Key.minor("Eb"));

    private static final List<String> EASY_ROOTS = List.of("C", "D", "E", "F", "G", "A");
    private static final List<String> MEDIUM_ROOTS = List.of("Bb", "Eb", "Ab", "B", "F#");
    private static final List<String> HARD_ROOTS = List.of("C#", "Db", "Gb", "G#", "A#");

    private final Random random;

    public ExerciseGenerator() {
        this(new Random());
    }

    /** Seeded construction keeps generator tests reproducible. */
    public ExerciseGenerator(Random random) {
        this.random = random;
    }

    /**
     * Every way each concept can be practised.
     *
     * <p>This is the tutor's menu, and its width is what stops the teaching becoming one
     * question in different clothes. Naming a chord, building one, and saying what it is
     * doing in a key are three separate skills; a learner who has only ever done the first
     * has not learned harmony.
     */
    private static final Map<String, List<ExerciseShape>> SHAPES = Map.ofEntries(
            Map.entry("note", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("interval", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("major-scale", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("minor-scale", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("key-signature", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("scale-degree", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("mode", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("triad", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("chord-inversion", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("diatonic-triads", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("seventh-chord", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("roman-numeral", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("figured-bass", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("harmonic-function", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("tonic-function", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("predominant-function", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("dominant-function", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("dominant-seventh", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("cadence", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("voice-leading", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("secondary-dominant", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("extended-chord", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("altered-dominant", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("chord-progression", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("two-five-one", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("modal-interchange", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("tritone-substitution", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("chord-symbol", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("chord-scale-theory", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD))),
            Map.entry("jazz-voicing", List.of(
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("turnaround", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD))),
            Map.entry("blues-scale", List.of(
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.play(TaskKind.BUILD))),
            Map.entry("blues-form", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("counterpoint", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("species-counterpoint", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))),
            Map.entry("modulation", List.of(
                    ExerciseShape.write(TaskKind.IDENTIFY),
                    ExerciseShape.write(TaskKind.BUILD),
                    ExerciseShape.write(TaskKind.ANALYSE))));

    /** The menu for a concept, or a single written form for anything unrecognised. */
    public static List<ExerciseShape> shapesFor(String conceptId) {
        return SHAPES.getOrDefault(conceptId, List.of(ExerciseShape.write(TaskKind.IDENTIFY)));
    }

    public static boolean supports(String conceptId, AnswerMode mode) {
        return shapesFor(conceptId).stream().anyMatch(shape -> shape.mode() == mode);
    }

    public ExerciseSpec generate(String conceptId, double difficulty, AnswerMode preferred) {
        return generate(conceptId, difficulty, defaultShape(conceptId, preferred));
    }

    /** The first shape on the menu matching the requested channel, else the first at all. */
    public static ExerciseShape defaultShape(String conceptId, AnswerMode preferred) {
        List<ExerciseShape> shapes = shapesFor(conceptId);
        return shapes.stream().filter(shape -> shape.mode() == preferred).findFirst().orElse(shapes.get(0));
    }

    /**
     * The same question, made smaller when a learner keeps missing it.
     *
     * <p>Distractors are other answers this very generator produced for the same shape, so
     * they are always plausible and always of the right kind — there is no separate list of
     * wrong answers to write or to let drift.
     */
    public ExerciseSpec generate(String conceptId, double difficulty, ExerciseShape shape, Scaffold scaffold) {
        ExerciseSpec base = generate(conceptId, difficulty, shape);
        if (scaffold == Scaffold.NONE || shape.isPlayed()
                || base.expectedAnswer().kind() == ExpectedAnswerKind.EXPLANATION) {
            return base;
        }
        EvidenceType weakened = scaffold.evidenceType() == null ? base.evidenceType() : scaffold.evidenceType();

        if (scaffold == Scaffold.HINT) {
            return new ExerciseSpec(base.conceptId(), base.type(), base.taskKind(), base.answerMode(),
                    weakened, base.prompt() + " " + hintFor(base.expectedAnswer()), base.expectedAnswer(),
                    base.notationAbc(), base.keyContext(), base.difficulty(), scaffold, List.of());
        }

        List<String> options = optionsFor(conceptId, difficulty, shape, base.expectedAnswer().canonical());
        String prompt = base.prompt() + " Choose one: " + String.join(", ", options) + ".";
        return new ExerciseSpec(base.conceptId(), base.type(), base.taskKind(), base.answerMode(),
                weakened, prompt,
                ExpectedAnswer.text(base.expectedAnswer().canonical(), base.expectedAnswer().canonical()),
                base.notationAbc(), base.keyContext(), base.difficulty(), scaffold, options);
    }

    /** Gives away the opening of the answer, which is a step down rather than a give-away. */
    private static String hintFor(ExpectedAnswer expected) {
        List<String> notes = expected.noteNames();
        if (notes != null && notes.size() > 1) {
            return "To start you off: the first note is %s.".formatted(notes.get(0));
        }
        String canonical = expected.canonical().trim();
        String first = canonical.split("\\s+")[0];
        return canonical.equals(first)
                ? "To start you off: it begins with \"%s\".".formatted(first.charAt(0))
                : "To start you off: it begins \"%s\".".formatted(first);
    }

    /** Plausible wrong answers, taken from what this generator produces for the same question. */
    private List<String> optionsFor(String conceptId, double difficulty, ExerciseShape shape, String correct) {
        Set<String> options = new LinkedHashSet<>();
        options.add(correct);
        for (int attempt = 0; attempt < 24 && options.size() < 4; attempt++) {
            options.add(generate(conceptId, difficulty, shape).expectedAnswer().canonical());
        }
        List<String> shuffled = new ArrayList<>(options);
        java.util.Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }

    public ExerciseSpec generate(String conceptId, double difficulty, ExerciseShape shape) {
        return switch (conceptId) {
            case "note" -> note(difficulty, shape);
            case "interval" -> interval(difficulty, shape);
            case "major-scale" -> majorScale(difficulty, shape);
            case "minor-scale" -> minorScale(difficulty, shape);
            case "key-signature" -> keySignature(difficulty, shape);
            case "scale-degree" -> scaleDegree(difficulty, shape);
            case "mode" -> mode(difficulty, shape);
            case "triad" -> triad(difficulty, shape);
            case "chord-inversion" -> inversion(difficulty, shape);
            case "diatonic-triads" -> diatonicTriads(difficulty, shape);
            case "seventh-chord" -> seventhChord(difficulty, shape);
            case "roman-numeral" -> romanNumeral(difficulty, shape);
            case "figured-bass" -> figuredBass(difficulty, shape);
            case "harmonic-function" -> harmonicFunction(difficulty, shape);
            case "tonic-function" -> tonicFunction(difficulty, shape);
            case "predominant-function" -> predominantFunction(difficulty, shape);
            case "dominant-function" -> dominantFunction(difficulty, shape);
            case "dominant-seventh" -> dominantSeventh(difficulty, shape);
            case "cadence" -> cadence(difficulty, shape);
            case "voice-leading" -> voiceLeading(difficulty, shape);
            case "secondary-dominant" -> secondaryDominant(difficulty, shape);
            case "modulation" -> modulation(difficulty, shape);
            case "extended-chord" -> extendedChord(difficulty, shape);
            case "altered-dominant" -> alteredDominant(difficulty, shape);
            case "chord-progression" -> chordProgression(difficulty, shape);
            case "two-five-one" -> twoFiveOne(difficulty, shape);
            case "modal-interchange" -> modalInterchange(difficulty, shape);
            case "tritone-substitution" -> tritoneSubstitution(difficulty, shape);
            case "chord-symbol" -> chordSymbol(difficulty, shape);
            case "chord-scale-theory" -> chordScale(difficulty, shape);
            case "jazz-voicing" -> jazzVoicing(difficulty, shape);
            case "turnaround" -> turnaround(difficulty, shape);
            case "blues-scale" -> bluesScale(difficulty, shape);
            case "blues-form" -> bluesForm(difficulty, shape);
            case "counterpoint" -> counterpoint(difficulty, shape);
            case "species-counterpoint" -> speciesCounterpoint(difficulty, shape);
            default -> explain(conceptId, difficulty);
        };
    }

    private ExerciseSpec spec(String conceptId, ExerciseType type, ExerciseShape shape, EvidenceType evidence,
                              String prompt, ExpectedAnswer expected, String abc, String key, double difficulty) {
        return new ExerciseSpec(conceptId, type, shape.kind(), shape.mode(), evidence, prompt, expected,
                abc, key, difficulty);
    }

    // ---------------------------------------------------------------- fundamentals

    private ExerciseSpec note(double difficulty, ExerciseShape shape) {
        if (shape.isPlayed()) {
            PitchClass pitchClass = randomRoot(difficulty);
            Note target = new Note(pitchClass, 3 + random.nextInt(2));
            return spec("note", ExerciseType.NAME_NOTE, shape, EvidenceType.MIDI_NOTE,
                    "Play %s on the keyboard.".formatted(target.name()),
                    ExpectedAnswer.midiNotes(List.of(pitchClass.name()), target.name()), null, null, difficulty);
        }
        if (shape.kind() == TaskKind.BUILD) {
            PitchClass from = randomRoot(difficulty);
            // Spelled as a second, so there is one right answer rather than two enharmonic ones.
            PitchClass answer = from.transpose(Interval.MINOR_SECOND);
            return spec("note", ExerciseType.NAME_NOTE, shape, EvidenceType.TEXT_RECALL,
                    "Which note is a minor second above %s?".formatted(from.name()),
                    ExpectedAnswer.text(answer.name(), answer.name()), null, null, difficulty);
        }
        PitchClass pitchClass = PitchClass.parse(pick(List.of("F#", "Bb", "C#", "Eb", "G#", "Ab", "D#")));
        List<String> alternatives = enharmonics(pitchClass);
        return spec("note", ExerciseType.NAME_NOTE, shape, EvidenceType.TEXT_RECALL,
                "Write another name for %s.".formatted(pitchClass.name()),
                ExpectedAnswer.text(alternatives.get(0), alternatives.toArray(new String[0])),
                null, null, difficulty);
    }

    private ExerciseSpec interval(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        Interval interval = randomInterval(difficulty);
        PitchClass upper = root.transpose(interval);

        if (shape.isPlayed()) {
            return spec("interval", ExerciseType.BUILD_INTERVAL, shape, EvidenceType.MIDI_INTERVAL,
                    "Play a %s above %s.".formatted(intervalName(interval), root.name()),
                    ExpectedAnswer.midiNotes(List.of(root.name(), upper.name()),
                            "%s and %s".formatted(root.name(), upper.name())), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case BUILD -> spec("interval", ExerciseType.BUILD_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                    "Which note is a %s above %s?".formatted(intervalName(interval), root.name()),
                    ExpectedAnswer.text(upper.name(), upper.name()), null, null, difficulty);
            case ANALYSE -> spec("interval", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                    "How many semitones are there in a %s?".formatted(intervalName(interval)),
                    ExpectedAnswer.text(String.valueOf(interval.semitones()),
                            String.valueOf(interval.semitones()), numberWord(interval.semitones())),
                    null, null, difficulty);
            default -> spec("interval", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                    "What interval is %s up to %s?".formatted(root.name(), upper.name()),
                    ExpectedAnswer.text(intervalName(interval), interval.symbol(),
                            intervalName(interval), shortIntervalName(interval)), null, null, difficulty);
        };
    }

    // ---------------------------------------------------------------- scales

    private ExerciseSpec majorScale(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        Scale scale = new Scale(key.tonic(), ScaleType.MAJOR);
        List<String> spelled = scale.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("major-scale", ExerciseType.PLAY_SCALE, shape, EvidenceType.MIDI_SCALE,
                    "Play the %s scale ascending, one octave.".formatted(scale.name()),
                    ExpectedAnswer.midiScale(key.tonic().name(), ScaleType.MAJOR.name(), scale.name()),
                    null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("major-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                    "Which major scale is this: %s?".formatted(String.join(" ", spelled)),
                    ExpectedAnswer.text(key.tonic().name(), key.tonic().name(), key.name()),
                    AbcNotation.scale(scale, 4), key.name(), difficulty);
            case ANALYSE -> spec("major-scale", ExerciseType.NAME_SCALE_DEGREE, shape, EvidenceType.TEXT_RECALL,
                    "In %s, between which two degrees does the upper semitone fall? Name the two notes."
                            .formatted(scale.name()),
                    ExpectedAnswer.noteSequence(List.of(spelled.get(6), spelled.get(0))),
                    null, key.name(), difficulty);
            default -> spec("major-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                    "Spell the %s scale, one note per letter.".formatted(scale.name()),
                    ExpectedAnswer.noteSequence(spelled), AbcNotation.scale(scale, 4), key.name(), difficulty);
        };
    }

    private ExerciseSpec minorScale(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MINOR);
        ScaleType type = difficulty < 0.4 ? ScaleType.NATURAL_MINOR
                : pick(List.of(ScaleType.NATURAL_MINOR, ScaleType.HARMONIC_MINOR, ScaleType.MELODIC_MINOR));
        Scale scale = new Scale(key.tonic(), type);
        List<String> spelled = scale.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("minor-scale", ExerciseType.PLAY_SCALE, shape, EvidenceType.MIDI_SCALE,
                    "Play the %s scale ascending, one octave.".formatted(scale.name()),
                    ExpectedAnswer.midiScale(key.tonic().name(), type.name(), scale.name()),
                    null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("minor-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                    "Which minor scale is this: %s? Name the tonic and the form."
                            .formatted(String.join(" ", spelled)),
                    ExpectedAnswer.text(scale.name(), scale.name(), type.displayName()),
                    AbcNotation.scale(scale, 4), key.name(), difficulty);
            case ANALYSE -> {
                Scale harmonic = new Scale(key.tonic(), ScaleType.HARMONIC_MINOR);
                List<PitchClass> notes = harmonic.pitchClasses();
                yield spec("minor-scale", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                        "In %s harmonic minor, what is the interval from %s up to %s?".formatted(
                                key.tonic().name(), notes.get(5).name(), notes.get(6).name()),
                        ExpectedAnswer.text("augmented second", "augmented second", "A2", "augmented 2"),
                        null, key.name(), difficulty);
            }
            default -> spec("minor-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                    "Spell the %s scale.".formatted(scale.name()),
                    ExpectedAnswer.noteSequence(spelled), AbcNotation.scale(scale, 4), key.name(), difficulty);
        };
    }

    private ExerciseSpec keySignature(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        int signature = key.keySignature();

        return switch (shape.kind()) {
            case BUILD -> {
                Key major = new Key(key.tonic(), Mode.MAJOR);
                yield spec("key-signature", ExerciseType.KEY_SIGNATURE, shape, EvidenceType.TEXT_RECALL,
                        "Which major key has %s?".formatted(describeSignature(major.keySignature())),
                        ExpectedAnswer.text(major.tonic().name(), major.tonic().name(), major.name()),
                        null, null, difficulty);
            }
            case ANALYSE -> {
                Key relative = key.relative();
                yield spec("key-signature", ExerciseType.KEY_SIGNATURE, shape, EvidenceType.TEXT_RECALL,
                        "Which key shares a signature with %s? Name its tonic.".formatted(key.name()),
                        ExpectedAnswer.text(relative.tonic().name(), relative.tonic().name(), relative.name()),
                        null, key.name(), difficulty);
            }
            default -> {
                String canonical = describeSignature(signature);
                List<String> acceptable = new ArrayList<>(List.of(canonical, String.valueOf(Math.abs(signature))));
                if (signature > 0) {
                    acceptable.add(signature + "#");
                    acceptable.add(numberWord(signature) + " sharps");
                } else if (signature < 0) {
                    acceptable.add(Math.abs(signature) + "b");
                    acceptable.add(numberWord(-signature) + " flats");
                } else {
                    acceptable.addAll(List.of("none", "no sharps or flats", "0"));
                }
                yield spec("key-signature", ExerciseType.KEY_SIGNATURE, shape, EvidenceType.TEXT_RECALL,
                        "How many sharps or flats does %s have?".formatted(key.name()),
                        ExpectedAnswer.text(canonical, acceptable.toArray(new String[0])),
                        null, key.name(), difficulty);
            }
        };
    }

    private ExerciseSpec scaleDegree(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        int degree = 2 + random.nextInt(6);
        ScaleDegree scaleDegree = ScaleDegree.of(degree);
        String degreeName = scaleDegree.displayName(key.mode(), degree == 7).toLowerCase();
        PitchClass note = key.scale(degree == 7).degree(degree);

        if (shape.isPlayed()) {
            return spec("scale-degree", ExerciseType.NAME_SCALE_DEGREE, shape, EvidenceType.MIDI_NOTE,
                    "Play the %s of %s.".formatted(degreeName, key.name()),
                    ExpectedAnswer.midiNotes(List.of(note.name()), note.name()), null, key.name(), difficulty);
        }
        if (shape.kind() == TaskKind.IDENTIFY) {
            return spec("scale-degree", ExerciseType.NAME_SCALE_DEGREE, shape, EvidenceType.TEXT_RECALL,
                    "In %s, what is %s called?".formatted(key.name(), note.name()),
                    ExpectedAnswer.text(degreeName, degreeName), null, key.name(), difficulty);
        }
        return spec("scale-degree", ExerciseType.NAME_SCALE_DEGREE, shape, EvidenceType.TEXT_RECALL,
                "In %s, which note is the %s?".formatted(key.name(), degreeName),
                ExpectedAnswer.text(note.name(), note.name()), null, key.name(), difficulty);
    }

    private ExerciseSpec mode(double difficulty, ExerciseShape shape) {
        ScaleType type = pick(List.of(ScaleType.DORIAN, ScaleType.PHRYGIAN, ScaleType.LYDIAN,
                ScaleType.MIXOLYDIAN, ScaleType.LOCRIAN));
        PitchClass tonic = randomRoot(difficulty);
        Scale scale = new Scale(tonic, type);
        List<String> spelled = scale.pitchClasses().stream().map(PitchClass::name).toList();
        String canonical = tonic.name() + " " + type.displayName();

        if (shape.isPlayed()) {
            return spec("mode", ExerciseType.IDENTIFY_MODE, shape, EvidenceType.MIDI_SCALE,
                    "Play %s ascending, one octave.".formatted(canonical),
                    ExpectedAnswer.midiScale(tonic.name(), type.name(), canonical), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case BUILD -> spec("mode", ExerciseType.IDENTIFY_MODE, shape, EvidenceType.TEXT_RECALL,
                    "Spell %s.".formatted(canonical),
                    ExpectedAnswer.noteSequence(spelled), AbcNotation.scale(scale, 4), null, difficulty);
            case ANALYSE -> {
                Scale major = new Scale(tonic, ScaleType.MAJOR);
                List<PitchClass> majorNotes = major.pitchClasses();
                List<PitchClass> modeNotes = scale.pitchClasses();
                List<String> different = new ArrayList<>();
                for (int i = 0; i < modeNotes.size(); i++) {
                    if (!modeNotes.get(i).equals(majorNotes.get(i))) {
                        different.add(modeNotes.get(i).name());
                    }
                }
                yield spec("mode", ExerciseType.IDENTIFY_MODE, shape, EvidenceType.TEXT_RECALL,
                        "Which notes of %s differ from %s major? Name them.".formatted(canonical, tonic.name()),
                        ExpectedAnswer.noteSet(different), null, null, difficulty);
            }
            default -> spec("mode", ExerciseType.IDENTIFY_MODE, shape, EvidenceType.TEXT_RECALL,
                    "Which mode is this, starting on %s: %s?".formatted(tonic.name(), String.join(" ", spelled)),
                    ExpectedAnswer.text(canonical, canonical, type.displayName()),
                    AbcNotation.scale(scale, 4), null, difficulty);
        };
    }

    // ---------------------------------------------------------------- chords

    private ExerciseSpec triad(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = difficulty < 0.4
                ? pick(List.of(ChordQuality.MAJOR, ChordQuality.MINOR))
                : pick(List.of(ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.DIMINISHED,
                        ChordQuality.AUGMENTED));
        Chord chord = Chord.of(root, quality);
        List<String> notes = chord.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("triad", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play a %s.".formatted(chord.describe()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("triad", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Which triad is %s? Name the root and the quality.".formatted(String.join(" ", notes)),
                    ExpectedAnswer.text(root.name() + " " + qualityWord(quality),
                            root.name() + " " + qualityWord(quality), chord.symbol()),
                    null, null, difficulty);
            case ANALYSE -> spec("triad", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                    "In a %s, what is the interval from the third up to the fifth?"
                            .formatted(quality.displayName()),
                    ExpectedAnswer.text(intervalName(Interval.between(chord.third(), chord.fifth())),
                            intervalName(Interval.between(chord.third(), chord.fifth())),
                            Interval.between(chord.third(), chord.fifth()).symbol()),
                    null, null, difficulty);
            default -> spec("triad", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell the %s.".formatted(chord.describe()),
                    ExpectedAnswer.noteSet(notes), null, null, difficulty);
        };
    }

    private ExerciseSpec inversion(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(List.of(ChordQuality.MAJOR, ChordQuality.MINOR));
        Inversion inv = pick(List.of(Inversion.FIRST, Inversion.SECOND));
        Chord chord = new Chord(root, quality, inv);
        String named = root.name() + " " + quality.displayName();

        if (shape.isPlayed()) {
            return spec("chord-inversion", ExerciseType.PLAY_INVERSION, shape, EvidenceType.MIDI_CHORD,
                    "Play %s in %s.".formatted(named, inv.displayName()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("chord-inversion", ExerciseType.NAME_BASS_NOTE, shape, EvidenceType.TEXT_RECALL,
                    "%s is written %s. Which inversion is that?".formatted(named, chord.symbol()),
                    ExpectedAnswer.text(inv.displayName(), inv.displayName(),
                            inv.figuredBass(quality.size())), null, null, difficulty);
            case ANALYSE -> {
                List<String> voiced = chord.notes(3).stream()
                        .map(note -> note.pitchClass().name()).toList();
                yield spec("chord-inversion", ExerciseType.NAME_BASS_NOTE, shape, EvidenceType.TEXT_RECALL,
                        "%s is sounding, lowest note first. Name the chord and its inversion."
                                .formatted(String.join(" ", voiced)),
                        ExpectedAnswer.text(chord.describe(), chord.describe(), chord.symbol()),
                        null, null, difficulty);
            }
            default -> spec("chord-inversion", ExerciseType.NAME_BASS_NOTE, shape, EvidenceType.TEXT_RECALL,
                    "Which note is in the bass when %s is in %s?".formatted(named, inv.displayName()),
                    ExpectedAnswer.text(chord.bass().name(), chord.bass().name()), null, null, difficulty);
        };
    }

    private ExerciseSpec diatonicTriads(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        int degree = 2 + random.nextInt(6);
        Chord chord = key.triad(degree);

        if (shape.isPlayed()) {
            return spec("diatonic-triads", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "In %s, play the triad on degree %d.".formatted(key.name(), degree),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case BUILD -> spec("diatonic-triads", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "In %s, spell the triad built on degree %d.".formatted(key.name(), degree),
                    ExpectedAnswer.noteSet(chord.pitchClasses().stream().map(PitchClass::name).toList()),
                    null, key.name(), difficulty);
            case ANALYSE -> {
                List<Chord> triads = key.diatonicTriads();
                List<String> diminished = triads.stream()
                        .filter(candidate -> candidate.quality() == ChordQuality.DIMINISHED)
                        .map(Chord::symbol).toList();
                yield spec("diatonic-triads", ExerciseType.CHORD_QUALITY_IN_KEY, shape, EvidenceType.TEXT_RECALL,
                        "In %s, which of the seven diatonic triads is diminished?".formatted(key.name()),
                        ExpectedAnswer.text(String.join(" ", diminished),
                                diminished.toArray(new String[0])),
                        null, key.name(), difficulty);
            }
            default -> spec("diatonic-triads", ExerciseType.CHORD_QUALITY_IN_KEY, shape, EvidenceType.TEXT_RECALL,
                    "In %s, what is the quality of the triad built on degree %d?".formatted(key.name(), degree),
                    ExpectedAnswer.text(qualityWord(chord.quality()), qualityWord(chord.quality()),
                            chord.quality().displayName(), chord.symbol()),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec seventhChord(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(List.of(ChordQuality.DOMINANT_SEVENTH, ChordQuality.MINOR_SEVENTH,
                ChordQuality.MAJOR_SEVENTH, ChordQuality.HALF_DIMINISHED_SEVENTH));
        Chord chord = Chord.of(root, quality);
        List<String> notes = chord.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("seventh-chord", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play %s.".formatted(chord.symbol()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("seventh-chord", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Which seventh chord is %s?".formatted(String.join(" ", notes)),
                    ExpectedAnswer.text(chord.symbol(), chord.symbol(), chord.describe()),
                    null, null, difficulty);
            case ANALYSE -> spec("seventh-chord", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                    "In %s, what is the seventh above the root, and what kind of seventh is it?"
                            .formatted(chord.symbol()),
                    ExpectedAnswer.text(notes.get(3), notes.get(3)), null, null, difficulty);
            default -> spec("seventh-chord", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell %s.".formatted(chord.symbol()),
                    ExpectedAnswer.noteSet(notes), null, null, difficulty);
        };
    }

    // ---------------------------------------------------------------- harmony

    /** A short diatonic progression in a key, with its Roman numerals computed by the engine. */
    private record Progression(List<Chord> chords, List<String> numerals) {
        String symbols() {
            return String.join(" ", chords.stream().map(Chord::symbol).toList());
        }

        String numeralLine() {
            return String.join(" ", numerals);
        }
    }

    private Progression progression(Key key, List<Integer> degrees) {
        List<Chord> chords = degrees.stream().map(key::triad).toList();
        List<String> numerals = ProgressionAnalyzer.analyze(chords, key).chords().stream()
                .map(ChordAnalysis::romanNumeralSymbol).toList();
        return new Progression(chords, numerals);
    }

    private ExerciseSpec romanNumeral(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        int degree = 1 + random.nextInt(7);
        Chord chord = key.triad(degree);
        ChordAnalysis analysis = ProgressionAnalyzer.analyzeChord(chord, key);

        if (shape.isPlayed()) {
            return spec("roman-numeral", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.MIDI_CHORD,
                    "In %s, play the chord written %s.".formatted(key.name(), analysis.romanNumeralSymbol()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case BUILD -> spec("roman-numeral", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "In %s, which chord is %s?".formatted(key.name(), analysis.romanNumeralSymbol()),
                    ExpectedAnswer.text(chord.symbol(), chord.symbol(), chord.root().name()),
                    AbcNotation.chord(chord, AbcNotation.CHORD_OCTAVE, key), key.name(), difficulty);
            case ANALYSE -> {
                Progression progression = progression(key, pick(List.of(
                        List.of(1, 4, 5, 1), List.of(1, 6, 4, 5), List.of(1, 2, 5, 1), List.of(6, 4, 1, 5))));
                yield spec("roman-numeral", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TRANSFER_PROBLEM,
                        "Analyse this in %s. Give the Roman numeral for each chord: %s"
                                .formatted(key.name(), progression.symbols()),
                        ExpectedAnswer.text(progression.numeralLine(), progression.numeralLine(),
                                String.join(" - ", progression.numerals())),
                        AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("roman-numeral", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "In %s, what Roman numeral describes %s?".formatted(key.name(), chord.symbol()),
                    ExpectedAnswer.text(analysis.romanNumeralSymbol(), analysis.romanNumeralSymbol(),
                            analysis.romanNumeralSymbol().replace("°", "o"),
                            analysis.romanNumeralSymbol().replace("°", "dim")),
                    AbcNotation.chord(chord, AbcNotation.CHORD_OCTAVE, key), key.name(), difficulty);
        };
    }

    private ExerciseSpec figuredBass(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        boolean seventh = difficulty >= 0.5;
        Inversion inv = seventh
                ? pick(List.of(Inversion.ROOT_POSITION, Inversion.FIRST, Inversion.SECOND, Inversion.THIRD))
                : pick(List.of(Inversion.FIRST, Inversion.SECOND));
        int size = seventh ? 4 : 3;
        String figures = inv.figuredBass(size);
        String canonical = figures.isEmpty() ? "no figures" : figures;

        return switch (shape.kind()) {
            case BUILD -> {
                Chord chord = seventh ? key.dominantSeventh().inverted(inv) : key.triad(5).inverted(inv);
                String label = "V" + figures;
                yield spec("figured-bass", ExerciseType.FIGURED_BASS, shape, EvidenceType.TEXT_RECALL,
                        "In %s, which note is in the bass of %s?".formatted(key.name(), label),
                        ExpectedAnswer.text(chord.bass().name(), chord.bass().name()),
                        null, key.name(), difficulty);
            }
            case ANALYSE -> {
                Chord chord = key.triad(1).inverted(pick(List.of(Inversion.FIRST, Inversion.SECOND)));
                String label = "I" + chord.inversion().figuredBass(3);
                yield spec("figured-bass", ExerciseType.FIGURED_BASS, shape, EvidenceType.TEXT_RECALL,
                        "In %s, spell %s from the bass upwards.".formatted(key.name(), label),
                        ExpectedAnswer.noteSequence(chord.notes(3).stream()
                                .map(note -> note.pitchClass().name()).toList()),
                        null, key.name(), difficulty);
            }
            default -> spec("figured-bass", ExerciseType.FIGURED_BASS, shape, EvidenceType.TEXT_RECALL,
                    "Which figures show a %s %s?".formatted(inv.displayName(),
                            seventh ? "seventh chord" : "triad"),
                    ExpectedAnswer.text(canonical, canonical, figures,
                            figures.length() == 2 ? figures.charAt(0) + "/" + figures.charAt(1) : figures),
                    null, null, difficulty);
        };
    }

    private ExerciseSpec harmonicFunction(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        int degree = pick(List.of(2, 3, 4, 5, 6, 7));
        Chord chord = key.triad(degree);
        HarmonicFunction function = ProgressionAnalyzer.analyzeChord(chord, key).function();

        return switch (shape.kind()) {
            case BUILD -> {
                Chord predominant = key.triad(4);
                yield spec("harmonic-function", ExerciseType.HARMONIC_FUNCTION, shape, EvidenceType.TEXT_RECALL,
                        "In %s, name the triad on degree 4 and say what function it serves."
                                .formatted(key.name()),
                        ExpectedAnswer.text(predominant.symbol() + " predominant",
                                predominant.symbol() + " predominant", predominant.symbol()),
                        null, key.name(), difficulty);
            }
            case ANALYSE -> {
                Progression progression = progression(key, List.of(1, 6, 4, 5));
                yield spec("harmonic-function", ExerciseType.HARMONIC_FUNCTION, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "In %s: %s. Which chord is the predominant?"
                                .formatted(key.name(), progression.symbols()),
                        ExpectedAnswer.text(progression.chords().get(2).symbol(),
                                progression.chords().get(2).symbol()),
                        AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("harmonic-function", ExerciseType.HARMONIC_FUNCTION, shape, EvidenceType.TEXT_RECALL,
                    "In %s, what function does %s serve?".formatted(key.name(), chord.symbol()),
                    ExpectedAnswer.text(functionWord(function), functionWord(function),
                            functionWord(function).replace("predominant", "pre-dominant")),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec tonicFunction(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        Chord tonic = key.tonicTriad();
        Chord submediant = key.triad(6);
        Chord mediant = key.triad(3);

        return switch (shape.kind()) {
            case IDENTIFY -> spec("tonic-function", ExerciseType.HARMONIC_FUNCTION, shape,
                    EvidenceType.TEXT_RECALL,
                    "Which three scale degrees carry tonic function?",
                    ExpectedAnswer.text("1 3 6", "1 3 6", "1 3 and 6", "i iii vi"),
                    null, key.name(), difficulty);
            case ANALYSE -> {
                List<PitchClass> shared = new ArrayList<>(tonic.pitchClasses());
                shared.retainAll(submediant.pitchClasses());
                yield spec("tonic-function", ExerciseType.HARMONIC_FUNCTION, shape, EvidenceType.TEXT_RECALL,
                        "In %s, %s and %s can stand in for one another. Which notes do they share?"
                                .formatted(key.name(), tonic.symbol(), submediant.symbol()),
                        ExpectedAnswer.noteSet(shared.stream().map(PitchClass::name).toList()),
                        null, key.name(), difficulty);
            }
            default -> spec("tonic-function", ExerciseType.HARMONIC_FUNCTION, shape, EvidenceType.TEXT_RECALL,
                    "In %s, name a triad other than %s that can stand in for the tonic."
                            .formatted(key.name(), tonic.symbol()),
                    ExpectedAnswer.text(submediant.symbol(), submediant.symbol(), mediant.symbol(), "vi", "iii"),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec predominantFunction(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        Chord supertonic = key.triad(2);
        Chord subdominant = key.triad(4);

        return switch (shape.kind()) {
            case IDENTIFY -> spec("predominant-function", ExerciseType.HARMONIC_FUNCTION, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, does %s have tonic, predominant or dominant function?"
                            .formatted(key.name(), subdominant.symbol()),
                    ExpectedAnswer.text("predominant", "predominant", "pre-dominant"),
                    null, key.name(), difficulty);
            case ANALYSE -> {
                Progression progression = progression(key, List.of(1, 4, 5, 1));
                yield spec("predominant-function", ExerciseType.HARMONIC_FUNCTION, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "In %s: %s. Which chord prepares the dominant?"
                                .formatted(key.name(), progression.symbols()),
                        ExpectedAnswer.text(subdominant.symbol(), subdominant.symbol()),
                        AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("predominant-function", ExerciseType.HARMONIC_FUNCTION, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, which triad on scale degree 2 typically leads to the dominant?"
                            .formatted(key.name()),
                    ExpectedAnswer.text(supertonic.symbol(), supertonic.symbol(), "ii", "ii°"),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec dominantFunction(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        Chord dominant = key.dominantTriad();
        PitchClass leadingTone = key.leadingTone();

        if (shape.isPlayed()) {
            return spec("dominant-function", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play the dominant triad of %s.".formatted(key.name()),
                    ExpectedAnswer.midiChord(dominant.symbol(), dominant.describe()),
                    null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("dominant-function", ExerciseType.HARMONIC_FUNCTION, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, the leading tone is %s. Which two triads contain it?"
                            .formatted(key.name(), leadingTone.name()),
                    ExpectedAnswer.text(dominant.symbol() + " " + key.triad(7, true).symbol(),
                            dominant.symbol(), key.triad(7, true).symbol()),
                    null, key.name(), difficulty);
            case ANALYSE -> spec("dominant-function", ExerciseType.RESOLVE_TENDENCY_TONE, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, the leading tone is %s. Which note does it want to move to?"
                            .formatted(key.name(), leadingTone.name()),
                    ExpectedAnswer.text(key.tonic().name(), key.tonic().name()),
                    null, key.name(), difficulty);
            default -> spec("dominant-function", ExerciseType.HARMONIC_FUNCTION, shape, EvidenceType.TEXT_RECALL,
                    "In %s, name the dominant triad.".formatted(key.name()),
                    ExpectedAnswer.text(dominant.symbol(), dominant.symbol(), "V"),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec dominantSeventh(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        Chord chord = key.dominantSeventh();
        List<PitchClass> members = chord.pitchClasses();

        if (shape.isPlayed()) {
            return spec("dominant-seventh", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play the dominant seventh of %s.".formatted(key.name()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("dominant-seventh", ExerciseType.IDENTIFY_TRITONE, shape,
                    EvidenceType.TEXT_RECALL,
                    "Which two notes of %s form the tritone that has to resolve?".formatted(chord.symbol()),
                    ExpectedAnswer.noteSet(List.of(members.get(1).name(), members.get(3).name())),
                    null, key.name(), difficulty);
            case ANALYSE -> {
                Chord tonic = key.tonicTriad();
                yield spec("dominant-seventh", ExerciseType.RESOLVE_TENDENCY_TONE, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "%s resolves to %s. Name where the seventh %s goes and where the third %s goes, in that order."
                                .formatted(chord.symbol(), tonic.symbol(), members.get(3).name(),
                                        members.get(1).name()),
                        ExpectedAnswer.noteSequence(List.of(tonic.third().name(), key.tonic().name())),
                        AbcNotation.progression(List.of(chord, tonic), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("dominant-seventh", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell the dominant seventh of %s.".formatted(key.name()),
                    ExpectedAnswer.noteSet(members.stream().map(PitchClass::name).toList()),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec cadence(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        List<List<Integer>> shapes = List.of(
                List.of(4, 5, 1), List.of(1, 4, 5), List.of(1, 5, 6), List.of(1, 4, 1));
        List<Integer> degrees = pick(shapes);
        List<Chord> chords = degrees.stream().map(key::triad).toList();
        ProgressionAnalysis analysis = ProgressionAnalyzer.analyze(chords, key);
        Cadence cadence = analysis.cadences().stream()
                .map(CadencePoint::cadence).reduce((first, second) -> second).orElse(Cadence.NONE);

        return switch (shape.kind()) {
            case BUILD -> {
                List<Chord> authentic = List.of(key.dominantTriad(), key.tonicTriad());
                yield spec("cadence", ExerciseType.IDENTIFY_CADENCE, shape, EvidenceType.TEXT_RECALL,
                        "In %s, name the two chords of a perfect authentic cadence, in order."
                                .formatted(key.name()),
                        ExpectedAnswer.text(authentic.get(0).symbol() + " " + authentic.get(1).symbol(),
                                authentic.get(0).symbol() + " " + authentic.get(1).symbol(), "V I"),
                        AbcNotation.progression(authentic, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            case ANALYSE -> spec("cadence", ExerciseType.IDENTIFY_CADENCE, shape, EvidenceType.TRANSFER_PROBLEM,
                    "In %s: %s. Give the Roman numerals, then name the cadence."
                            .formatted(key.name(), String.join(" ", chords.stream().map(Chord::symbol).toList())),
                    ExpectedAnswer.text(
                            String.join(" ", analysis.chords().stream()
                                    .map(ChordAnalysis::romanNumeralSymbol).toList())
                                    + " " + cadence.displayName(),
                            cadence.displayName(),
                            String.join(" ", analysis.chords().stream()
                                    .map(ChordAnalysis::romanNumeralSymbol).toList())),
                    AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            default -> spec("cadence", ExerciseType.IDENTIFY_CADENCE, shape, EvidenceType.TEXT_RECALL,
                    "In %s: %s. Name the cadence at the end."
                            .formatted(key.name(), String.join(" ", chords.stream().map(Chord::symbol).toList())),
                    ExpectedAnswer.text(cadence.displayName(), cadence.displayName(),
                            cadence.name().toLowerCase().replace('_', ' '),
                            cadence.displayName().replace(" cadence", "")),
                    AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
        };
    }

    private ExerciseSpec voiceLeading(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, null);
        Chord dominantSeventh = key.dominantSeventh();
        PitchClass seventh = dominantSeventh.pitchClasses().get(3);
        PitchClass third = dominantSeventh.third();
        Chord tonic = key.tonicTriad();

        return switch (shape.kind()) {
            case IDENTIFY -> spec("voice-leading", ExerciseType.RESOLVE_TENDENCY_TONE, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, which note is the tendency tone that must fall by step?"
                            .formatted(dominantSeventh.symbol()),
                    ExpectedAnswer.text(seventh.name(), seventh.name()), null, key.name(), difficulty);
            case ANALYSE -> spec("voice-leading", ExerciseType.RESOLVE_TENDENCY_TONE, shape,
                    EvidenceType.TRANSFER_PROBLEM,
                    "%s moves to %s. Name where %s goes, then where %s goes."
                            .formatted(dominantSeventh.symbol(), tonic.symbol(), seventh.name(), third.name()),
                    ExpectedAnswer.noteSequence(List.of(tonic.third().name(), key.tonic().name())),
                    AbcNotation.progression(List.of(dominantSeventh, tonic), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            default -> spec("voice-leading", ExerciseType.RESOLVE_TENDENCY_TONE, shape, EvidenceType.TEXT_RECALL,
                    "%s resolves to %s. The seventh is %s — which note should it move to?"
                            .formatted(dominantSeventh.symbol(), tonic.symbol(), seventh.name()),
                    ExpectedAnswer.text(tonic.third().name(), tonic.third().name()),
                    AbcNotation.progression(List.of(dominantSeventh, tonic), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
        };
    }

    private ExerciseSpec secondaryDominant(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        int target = pick(List.of(2, 4, 5, 6));
        Chord targetChord = key.triad(target);
        String targetNumeral = ProgressionAnalyzer.analyzeChord(targetChord, key).romanNumeralSymbol();
        Chord appliedTriad = Chord.of(targetChord.root().transpose(Interval.PERFECT_FIFTH), ChordQuality.MAJOR);
        Chord appliedSeventh = Chord.of(targetChord.root().transpose(Interval.PERFECT_FIFTH),
                ChordQuality.DOMINANT_SEVENTH);

        if (shape.isPlayed()) {
            return spec("secondary-dominant", ExerciseType.SECONDARY_DOMINANT, shape, EvidenceType.MIDI_CHORD,
                    "In %s, play V7/%s.".formatted(key.name(), targetNumeral),
                    ExpectedAnswer.midiChord(appliedSeventh.symbol(), appliedSeventh.describe()),
                    null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("secondary-dominant", ExerciseType.SECONDARY_DOMINANT, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, %s appears. Which chord is it the dominant of? Give the Roman numeral."
                            .formatted(key.name(), appliedSeventh.symbol()),
                    ExpectedAnswer.text(targetNumeral, targetNumeral, targetChord.symbol()),
                    null, key.name(), difficulty);
            case ANALYSE -> {
                List<Chord> chords = List.of(key.tonicTriad(), appliedSeventh, targetChord);
                List<String> numerals = ProgressionAnalyzer.analyze(chords, key).chords().stream()
                        .map(ChordAnalysis::romanNumeralSymbol).toList();
                yield spec("secondary-dominant", ExerciseType.SECONDARY_DOMINANT, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "Analyse this in %s: %s".formatted(key.name(),
                                String.join(" ", chords.stream().map(Chord::symbol).toList())),
                        ExpectedAnswer.text(String.join(" ", numerals), String.join(" ", numerals),
                                String.join(" - ", numerals)),
                        AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("secondary-dominant", ExerciseType.SECONDARY_DOMINANT, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, which chord is V/%s?".formatted(key.name(), targetNumeral),
                    ExpectedAnswer.text(appliedTriad.symbol(), appliedTriad.symbol(),
                            appliedTriad.root().name(), appliedSeventh.symbol()),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec modulation(double difficulty, ExerciseShape shape) {
        Key from = randomKey(difficulty, Mode.MAJOR);
        Key to = new Key(from.tonic().transpose(Interval.PERFECT_FIFTH), Mode.MAJOR);

        return switch (shape.kind()) {
            case IDENTIFY -> spec("modulation", ExerciseType.PIVOT_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Which major key lies a fifth above %s?".formatted(from.name()),
                    ExpectedAnswer.text(to.tonic().name(), to.tonic().name(), to.name()),
                    null, from.name(), difficulty);
            case ANALYSE -> {
                PitchClass raised = to.leadingTone();
                yield spec("modulation", ExerciseType.PIVOT_CHORD, shape, EvidenceType.TRANSFER_PROBLEM,
                        "Moving from %s to %s, one note has to be raised. Which one?"
                                .formatted(from.name(), to.name()),
                        ExpectedAnswer.text(raised.name(), raised.name()), null, from.name(), difficulty);
            }
            default -> {
                Set<String> destination = new LinkedHashSet<>(
                        to.diatonicTriads().stream().map(Chord::symbol).toList());
                List<String> shared = from.diatonicTriads().stream()
                        .map(Chord::symbol).filter(destination::contains).toList();
                yield spec("modulation", ExerciseType.PIVOT_CHORD, shape, EvidenceType.TEXT_RECALL,
                        "Name a triad that belongs to both %s and %s, and could pivot between them."
                                .formatted(from.name(), to.name()),
                        ExpectedAnswer.text(shared.isEmpty() ? "none" : shared.get(0),
                                shared.toArray(new String[0])),
                        null, from.name(), difficulty);
            }
        };
    }

    // ---------------------------------------------------------------- jazz harmony

    private ExerciseSpec extendedChord(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(List.of(ChordQuality.MAJOR_SIXTH, ChordQuality.MINOR_SIXTH,
                ChordQuality.DOMINANT_NINTH, ChordQuality.MAJOR_NINTH, ChordQuality.MINOR_NINTH,
                ChordQuality.DOMINANT_THIRTEENTH));
        Chord chord = Chord.of(root, quality);
        List<String> notes = chord.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("extended-chord", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play %s.".formatted(chord.symbol()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("extended-chord", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Which chord is %s?".formatted(String.join(" ", notes)),
                    ExpectedAnswer.text(chord.symbol(), chord.symbol(), chord.describe()),
                    null, null, difficulty);
            case ANALYSE -> {
                String top = notes.get(notes.size() - 1);
                Interval extension = quality.intervals().get(quality.size() - 1);
                yield spec("extended-chord", ExerciseType.IDENTIFY_INTERVAL, shape, EvidenceType.TEXT_RECALL,
                        "In %s, which note is the %s?".formatted(chord.symbol(), ordinalWord(extension.number())),
                        ExpectedAnswer.text(top, top), null, null, difficulty);
            }
            default -> spec("extended-chord", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell %s.".formatted(chord.symbol()),
                    ExpectedAnswer.noteSet(notes), null, null, difficulty);
        };
    }

    private ExerciseSpec alteredDominant(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(List.of(ChordQuality.DOMINANT_FLAT_NINTH,
                ChordQuality.DOMINANT_SHARP_NINTH, ChordQuality.DOMINANT_SHARP_ELEVENTH,
                ChordQuality.DOMINANT_FLAT_THIRTEENTH));
        Chord chord = Chord.of(root, quality);
        List<String> notes = chord.pitchClasses().stream().map(PitchClass::name).toList();
        String altered = notes.get(notes.size() - 1);

        if (shape.isPlayed()) {
            return spec("altered-dominant", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play %s.".formatted(chord.symbol()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("altered-dominant", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Which altered dominant is %s?".formatted(String.join(" ", notes)),
                    ExpectedAnswer.text(chord.symbol(), chord.symbol()), null, null, difficulty);
            case ANALYSE -> spec("altered-dominant", ExerciseType.IDENTIFY_INTERVAL, shape,
                    EvidenceType.TRANSFER_PROBLEM,
                    "In %s, which note is the altered tension?".formatted(chord.symbol()),
                    ExpectedAnswer.text(altered, altered), null, null, difficulty);
            default -> spec("altered-dominant", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell %s.".formatted(chord.symbol()),
                    ExpectedAnswer.noteSet(notes), null, null, difficulty);
        };
    }

    private ExerciseSpec chordProgression(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        List<Integer> degrees = pick(List.of(
                List.of(1, 5, 6, 4), List.of(1, 6, 4, 5), List.of(6, 4, 1, 5), List.of(1, 4, 2, 5)));
        Progression progression = progression(key, degrees);

        return switch (shape.kind()) {
            case BUILD -> spec("chord-progression", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "In %s, give the chords for %s.".formatted(key.name(), progression.numeralLine()),
                    ExpectedAnswer.text(progression.symbols(), progression.symbols()),
                    AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            case ANALYSE -> {
                int dominantAt = degrees.indexOf(5);
                Chord dominant = progression.chords().get(Math.max(dominantAt, 0));
                yield spec("chord-progression", ExerciseType.HARMONIC_FUNCTION, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "In %s: %s. Which chord is the dominant?".formatted(key.name(), progression.symbols()),
                        ExpectedAnswer.text(dominant.symbol(), dominant.symbol()),
                        AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("chord-progression", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "In %s: %s. Give the Roman numerals.".formatted(key.name(), progression.symbols()),
                    ExpectedAnswer.text(progression.numeralLine(), progression.numeralLine(),
                            String.join(" - ", progression.numerals())),
                    AbcNotation.progression(progression.chords(), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
        };
    }

    /** The ii-V-I, spelled with sevenths, which is how it is actually played. */
    private List<Chord> twoFiveOneChords(Key key) {
        return List.of(
                Chord.of(key.scale().degree(2), key.mode() == Mode.MAJOR
                        ? ChordQuality.MINOR_SEVENTH : ChordQuality.HALF_DIMINISHED_SEVENTH),
                key.dominantSeventh(),
                Chord.of(key.tonic(), key.mode() == Mode.MAJOR
                        ? ChordQuality.MAJOR_SEVENTH : ChordQuality.MINOR_SEVENTH));
    }

    private ExerciseSpec twoFiveOne(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        List<Chord> chords = twoFiveOneChords(key);
        String symbols = String.join(" ", chords.stream().map(Chord::symbol).toList());

        if (shape.isPlayed()) {
            return spec("two-five-one", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "In a ii-V-I in %s, play the V chord.".formatted(key.name()),
                    ExpectedAnswer.midiChord(chords.get(1).symbol(), chords.get(1).describe()),
                    null, key.name(), difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("two-five-one", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "%s. Which key is this ii-V-I in?".formatted(symbols),
                    ExpectedAnswer.text(key.tonic().name(), key.tonic().name(), key.name()),
                    AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            case ANALYSE -> {
                // The seventh of the ii becomes the third of the V: the join that makes it work.
                PitchClass shared = chords.get(0).pitchClasses().get(3);
                yield spec("two-five-one", ExerciseType.RESOLVE_TENDENCY_TONE, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "In the ii-V-I in %s (%s), which note is shared between %s and %s?"
                                .formatted(key.name(), symbols, chords.get(0).symbol(), chords.get(1).symbol()),
                        ExpectedAnswer.text(shared.name(), shared.name()),
                        AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            }
            default -> spec("two-five-one", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "In %s, name the three chords of a ii-V-I using seventh chords.".formatted(key.name()),
                    ExpectedAnswer.text(symbols, symbols),
                    AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
        };
    }

    private ExerciseSpec modalInterchange(double difficulty, ExerciseShape shape) {
        Key major = randomKey(difficulty, Mode.MAJOR);
        Key parallel = major.parallel();
        int degree = pick(List.of(4, 6, 7));
        Chord borrowed = parallel.triad(degree, false);

        return switch (shape.kind()) {
            case IDENTIFY -> spec("modal-interchange", ExerciseType.CHORD_QUALITY_IN_KEY, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, the chord %s appears. Which key is it borrowed from?"
                            .formatted(major.name(), borrowed.symbol()),
                    ExpectedAnswer.text(parallel.name(), parallel.name(), parallel.tonic().name(),
                            "parallel minor"),
                    null, major.name(), difficulty);
            case ANALYSE -> {
                Chord diatonic = major.triad(degree);
                yield spec("modal-interchange", ExerciseType.CHORD_QUALITY_IN_KEY, shape,
                        EvidenceType.TRANSFER_PROBLEM,
                        "In %s, the diatonic chord on degree %d is %s. What is it when borrowed from %s?"
                                .formatted(major.name(), degree, diatonic.symbol(), parallel.name()),
                        ExpectedAnswer.text(borrowed.symbol(), borrowed.symbol()),
                        null, major.name(), difficulty);
            }
            default -> spec("modal-interchange", ExerciseType.CHORD_QUALITY_IN_KEY, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, name the chord on degree %d borrowed from the parallel minor."
                            .formatted(major.name(), degree),
                    ExpectedAnswer.text(borrowed.symbol(), borrowed.symbol()),
                    null, major.name(), difficulty);
        };
    }

    private ExerciseSpec tritoneSubstitution(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        Chord dominant = key.dominantSeventh();
        Chord substitute = Chord.of(dominant.root().transpose(Interval.AUGMENTED_FOURTH),
                ChordQuality.DOMINANT_SEVENTH);
        List<PitchClass> tritone = List.of(dominant.pitchClasses().get(1), dominant.pitchClasses().get(3));

        return switch (shape.kind()) {
            case IDENTIFY -> spec("tritone-substitution", ExerciseType.SECONDARY_DOMINANT, shape,
                    EvidenceType.TEXT_RECALL,
                    "Which dominant seventh is the tritone substitution for %s?".formatted(dominant.symbol()),
                    ExpectedAnswer.text(substitute.symbol(), substitute.symbol(),
                            substitute.root().name()),
                    null, key.name(), difficulty);
            case ANALYSE -> spec("tritone-substitution", ExerciseType.IDENTIFY_TRITONE, shape,
                    EvidenceType.TRANSFER_PROBLEM,
                    "%s and %s work as substitutes because they share two notes. Name them."
                            .formatted(dominant.symbol(), substitute.symbol()),
                    ExpectedAnswer.noteSet(tritone.stream().map(PitchClass::name).toList()),
                    null, key.name(), difficulty);
            default -> spec("tritone-substitution", ExerciseType.SECONDARY_DOMINANT, shape,
                    EvidenceType.TEXT_RECALL,
                    "In %s, replace the V7 with its tritone substitution. Name the chord."
                            .formatted(key.name()),
                    ExpectedAnswer.text(substitute.symbol(), substitute.symbol()),
                    null, key.name(), difficulty);
        };
    }

    private ExerciseSpec bluesForm(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        Chord one = Chord.of(key.tonic(), ChordQuality.DOMINANT_SEVENTH);
        Chord four = Chord.of(key.scale().degree(4), ChordQuality.DOMINANT_SEVENTH);
        Chord five = Chord.of(key.scale().degree(5), ChordQuality.DOMINANT_SEVENTH);

        return switch (shape.kind()) {
            case BUILD -> spec("blues-form", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "In a blues in %s, name the three chords.".formatted(key.tonic().name()),
                    ExpectedAnswer.text("%s %s %s".formatted(one.symbol(), four.symbol(), five.symbol()),
                            "%s %s %s".formatted(one.symbol(), four.symbol(), five.symbol())),
                    AbcNotation.progression(List.of(one, four, five), key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
            case ANALYSE -> spec("blues-form", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TRANSFER_PROBLEM,
                    "In a twelve-bar blues in %s, which chord is played in bar 5?"
                            .formatted(key.tonic().name()),
                    ExpectedAnswer.text(four.symbol(), four.symbol(), "IV7", "IV"),
                    null, key.name(), difficulty);
            default -> spec("blues-form", ExerciseType.IDENTIFY_CADENCE, shape, EvidenceType.TEXT_RECALL,
                    "How many bars are there in a standard blues chorus?",
                    ExpectedAnswer.text("12", "12", "twelve"), null, null, difficulty);
        };
    }

    // ---------------------------------------------------------------- counterpoint

    /** Two voices moving, with the motion computed rather than asserted. */
    private record TwoVoices(Note lowerFrom, Note upperFrom, Note lowerTo, Note upperTo, Motion motion) {
        String describe() {
            return "%s over %s moving to %s over %s"
                    .formatted(upperFrom.name(), lowerFrom.name(), upperTo.name(), lowerTo.name());
        }
    }

    private TwoVoices twoVoices(double difficulty) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        List<PitchClass> scale = key.scale().pitchClasses();
        int lowerIndex = random.nextInt(4);
        int step = pick(List.of(-1, 1, 2));
        int upperGap = pick(List.of(2, 4, 5));

        Note lowerFrom = new Note(scale.get(lowerIndex), 3);
        Note upperFrom = new Note(scale.get((lowerIndex + upperGap) % 7), 4);
        Note lowerTo = new Note(scale.get(Math.floorMod(lowerIndex + step, 7)), 3);
        Note upperTo = new Note(scale.get(Math.floorMod(lowerIndex + step + upperGap, 7)), 4);
        return new TwoVoices(lowerFrom, upperFrom, lowerTo, upperTo,
                CounterpointAnalyzer.motionBetween(lowerFrom, upperFrom, lowerTo, upperTo));
    }

    private ExerciseSpec counterpoint(double difficulty, ExerciseShape shape) {
        TwoVoices voices = twoVoices(difficulty);

        return switch (shape.kind()) {
            case BUILD -> spec("counterpoint", ExerciseType.EXPLAIN, shape, EvidenceType.TEXT_RECALL,
                    "What is the name for two voices moving in opposite directions?",
                    ExpectedAnswer.text("contrary motion", "contrary motion", "contrary"),
                    null, null, difficulty);
            case ANALYSE -> {
                boolean forbidden = CounterpointAnalyzer.hasParallelPerfects(
                        voices.lowerFrom(), voices.upperFrom(), voices.lowerTo(), voices.upperTo());
                yield spec("counterpoint", ExerciseType.EXPLAIN, shape, EvidenceType.TRANSFER_PROBLEM,
                        "%s. Are these two voices allowed to move like that in strict counterpoint?"
                                .formatted(capitalise(voices.describe())),
                        ExpectedAnswer.text(forbidden ? "no" : "yes",
                                forbidden ? "no" : "yes",
                                forbidden ? "parallel fifths" : "allowed"),
                        null, null, difficulty);
            }
            default -> spec("counterpoint", ExerciseType.EXPLAIN, shape, EvidenceType.TEXT_RECALL,
                    "%s. What kind of motion is that?".formatted(capitalise(voices.describe())),
                    ExpectedAnswer.text(voices.motion().displayName(), voices.motion().displayName(),
                            voices.motion().name().toLowerCase()),
                    null, null, difficulty);
        };
    }

    private ExerciseSpec speciesCounterpoint(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        List<PitchClass> scale = key.scale().pitchClasses();
        Note cantus = new Note(scale.get(random.nextInt(5)), 3);
        Interval interval = pick(List.of(Interval.MAJOR_THIRD, Interval.PERFECT_FIFTH,
                Interval.MAJOR_SIXTH, Interval.MAJOR_SECOND, Interval.MINOR_SEVENTH,
                Interval.PERFECT_FOURTH));
        PitchClass above = cantus.pitchClass().transpose(interval);
        boolean consonant = CounterpointAnalyzer.isConsonant(interval);

        return switch (shape.kind()) {
            case BUILD -> spec("species-counterpoint", ExerciseType.BUILD_INTERVAL, shape,
                    EvidenceType.TEXT_RECALL,
                    "In first species, name a consonant interval a third or wider above the cantus firmus.",
                    ExpectedAnswer.text("major third", "major third", "minor third", "perfect fifth",
                            "major sixth", "minor sixth", "octave", "third", "sixth", "fifth"),
                    null, key.name(), difficulty);
            case ANALYSE -> spec("species-counterpoint", ExerciseType.EXPLAIN, shape,
                    EvidenceType.TRANSFER_PROBLEM,
                    "Against a cantus firmus on %s, is %s a consonance in first species?"
                            .formatted(cantus.pitchClass().name(), above.name()),
                    ExpectedAnswer.text(consonant ? "yes" : "no", consonant ? "yes" : "no",
                            consonant ? "consonant" : "dissonant"),
                    null, key.name(), difficulty);
            default -> spec("species-counterpoint", ExerciseType.IDENTIFY_INTERVAL, shape,
                    EvidenceType.TEXT_RECALL,
                    "Against a cantus firmus on %s, what interval does %s form above it?"
                            .formatted(cantus.pitchClass().name(), above.name()),
                    ExpectedAnswer.text(intervalName(interval), intervalName(interval),
                            interval.symbol()),
                    null, key.name(), difficulty);
        };
    }

    private static String capitalise(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Fallback for a concept with no generator: the model proposes a verdict, weakly. */
    private ExerciseSpec explain(String conceptId, double difficulty) {
        return new ExerciseSpec(conceptId, ExerciseType.EXPLAIN, TaskKind.ANALYSE, AnswerMode.TEXT,
                EvidenceType.EXPLANATION,
                "In your own words, explain %s and give an example.".formatted(conceptId.replace('-', ' ')),
                ExpectedAnswer.explanation("A correct explanation of " + conceptId), null, null, difficulty);
    }

    // ---------------------------------------------------------------- jazz

    /** Reading and writing lead-sheet chord symbols, which is where jazz notation starts. */
    private ExerciseSpec chordSymbol(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(difficulty < 0.5
                ? List.of(ChordQuality.MAJOR_SEVENTH, ChordQuality.MINOR_SEVENTH, ChordQuality.DOMINANT_SEVENTH)
                : List.of(ChordQuality.HALF_DIMINISHED_SEVENTH, ChordQuality.DOMINANT_FLAT_NINTH,
                        ChordQuality.MINOR_MAJOR_SEVENTH, ChordQuality.DOMINANT_SEVENTH_SUS4,
                        ChordQuality.SIX_NINE));
        Chord chord = Chord.of(root, quality);
        List<String> notes = chord.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("chord-symbol", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play %s.".formatted(chord.symbol()),
                    ExpectedAnswer.midiChord(chord.symbol(), chord.describe()), null, null, difficulty);
        }
        return switch (shape.kind()) {
            case IDENTIFY -> spec("chord-symbol", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "What chord symbol names %s?".formatted(String.join(" ", notes)),
                    ExpectedAnswer.text(chord.symbol(), chord.symbol()), null, null, difficulty);
            default -> spec("chord-symbol", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                    "Spell %s.".formatted(chord.symbol()),
                    ExpectedAnswer.noteSet(notes), null, null, difficulty);
        };
    }

    /** Which scale fits a chord: the step from naming harmony to improvising over it. */
    private ExerciseSpec chordScale(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        record Pairing(ChordQuality quality, ScaleType scale, String name) {}
        Pairing pairing = pick(difficulty < 0.6
                ? List.of(new Pairing(ChordQuality.DOMINANT_SEVENTH, ScaleType.MIXOLYDIAN, "Mixolydian"),
                        new Pairing(ChordQuality.MINOR_SEVENTH, ScaleType.DORIAN, "Dorian"),
                        new Pairing(ChordQuality.MAJOR_SEVENTH, ScaleType.MAJOR, "Ionian"))
                : List.of(new Pairing(ChordQuality.HALF_DIMINISHED_SEVENTH, ScaleType.LOCRIAN, "Locrian"),
                        new Pairing(ChordQuality.DOMINANT_FLAT_NINTH, ScaleType.ALTERED, "altered"),
                        new Pairing(ChordQuality.MAJOR_SEVENTH_SHARP_ELEVENTH, ScaleType.LYDIAN, "Lydian")));
        Chord chord = Chord.of(root, pairing.quality());
        Scale scale = new Scale(root, pairing.scale());

        if (shape.kind() == TaskKind.IDENTIFY) {
            return spec("chord-scale-theory", ExerciseType.IDENTIFY_MODE, shape, EvidenceType.TEXT_RECALL,
                    "Which scale is the usual choice over %s?".formatted(chord.symbol()),
                    ExpectedAnswer.text(pairing.name(), pairing.name(), pairing.scale().displayName()),
                    null, null, difficulty);
        }
        return spec("chord-scale-theory", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                "Spell the %s scale you would play over %s.".formatted(pairing.name(), chord.symbol()),
                ExpectedAnswer.noteSequence(scale.pitchClasses().stream().map(PitchClass::name).toList()),
                null, null, difficulty);
    }

    /**
     * Shell voicings. The third and the seventh are what make a chord sound like itself, so
     * the exercise is about those rather than about stacking everything.
     */
    private ExerciseSpec jazzVoicing(double difficulty, ExerciseShape shape) {
        PitchClass root = randomRoot(difficulty);
        ChordQuality quality = pick(List.of(ChordQuality.DOMINANT_SEVENTH, ChordQuality.MAJOR_SEVENTH,
                ChordQuality.MINOR_SEVENTH));
        Chord chord = Chord.of(root, quality);
        List<PitchClass> classes = chord.pitchClasses();
        List<String> shell = List.of(classes.get(0).name(), classes.get(1).name(), classes.get(3).name());

        if (shape.isPlayed()) {
            return spec("jazz-voicing", ExerciseType.PLAY_CHORD, shape, EvidenceType.MIDI_CHORD,
                    "Play a shell voicing of %s: root, third and seventh.".formatted(chord.symbol()),
                    ExpectedAnswer.midiNotes(shell, "a shell voicing of " + chord.symbol()),
                    null, null, difficulty);
        }
        if (shape.kind() == TaskKind.ANALYSE) {
            return spec("jazz-voicing", ExerciseType.SPELL_CHORD, shape, EvidenceType.TRANSFER_PROBLEM,
                    "In a rootless voicing of %s the bass player covers the root. Which note carries the quality?"
                            .formatted(chord.symbol()),
                    ExpectedAnswer.text(classes.get(1).name(), classes.get(1).name()), null, null, difficulty);
        }
        return spec("jazz-voicing", ExerciseType.SPELL_CHORD, shape, EvidenceType.TEXT_RECALL,
                "Spell a shell voicing of %s: root, third and seventh.".formatted(chord.symbol()),
                ExpectedAnswer.noteSet(shell), null, null, difficulty);
    }

    /** The few bars that carry the end of a chorus back to the top. */
    private ExerciseSpec turnaround(double difficulty, ExerciseShape shape) {
        Key key = randomKey(difficulty, Mode.MAJOR);
        List<Chord> chords = List.of(
                Chord.of(key.tonic(), ChordQuality.MAJOR_SEVENTH),
                Chord.of(key.scale().degree(6), ChordQuality.MINOR_SEVENTH),
                Chord.of(key.scale().degree(2), ChordQuality.MINOR_SEVENTH),
                key.dominantSeventh());
        String symbols = String.join(" ", chords.stream().map(Chord::symbol).toList());

        if (shape.kind() == TaskKind.IDENTIFY) {
            return spec("turnaround", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                    "%s. Which key does this turnaround belong to?".formatted(symbols),
                    ExpectedAnswer.text(key.tonic().name(), key.tonic().name(), key.name()),
                    AbcNotation.progression(chords, key, AbcNotation.CHORD_OCTAVE), key.name(), difficulty);
        }
        return spec("turnaround", ExerciseType.ROMAN_NUMERAL, shape, EvidenceType.TEXT_RECALL,
                "Spell a I-vi-ii-V turnaround in %s, as chord symbols.".formatted(key.name()),
                ExpectedAnswer.text(symbols, symbols), null, key.name(), difficulty);
    }

    /** The minor pentatonic with the flat fifth added: the note that makes it sound blue. */
    private ExerciseSpec bluesScale(double difficulty, ExerciseShape shape) {
        PitchClass tonic = randomRoot(difficulty);
        Scale scale = new Scale(tonic, ScaleType.BLUES);
        List<String> notes = scale.pitchClasses().stream().map(PitchClass::name).toList();

        if (shape.isPlayed()) {
            return spec("blues-scale", ExerciseType.PLAY_SCALE, shape, EvidenceType.MIDI_SCALE,
                    "Play the %s blues scale.".formatted(tonic.name()),
                    ExpectedAnswer.midiScale(tonic.name(), ScaleType.BLUES.name(), tonic.name() + " blues"),
                    null, null, difficulty);
        }
        if (shape.kind() == TaskKind.IDENTIFY) {
            return spec("blues-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                    "In the %s blues scale, which note is the blue note added to the minor pentatonic?"
                            .formatted(tonic.name()),
                    ExpectedAnswer.text(notes.get(3), notes.get(3)), null, null, difficulty);
        }
        return spec("blues-scale", ExerciseType.SPELL_SCALE, shape, EvidenceType.TEXT_RECALL,
                "Spell the %s blues scale.".formatted(tonic.name()),
                ExpectedAnswer.noteSequence(notes), null, null, difficulty);
    }

    // ---------------------------------------------------------------- helpers

    private <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    private Key randomKey(double difficulty, Mode mode) {
        List<Key> pool = new ArrayList<>(EASY_KEYS);
        if (difficulty >= 0.4) {
            pool.addAll(MEDIUM_KEYS);
        }
        if (difficulty >= 0.7) {
            pool.addAll(HARD_KEYS);
        }
        List<Key> filtered = mode == null ? pool : pool.stream().filter(key -> key.mode() == mode).toList();
        return pick(filtered.isEmpty() ? pool : filtered);
    }

    private PitchClass randomRoot(double difficulty) {
        List<String> pool = new ArrayList<>(EASY_ROOTS);
        if (difficulty >= 0.4) {
            pool.addAll(MEDIUM_ROOTS);
        }
        if (difficulty >= 0.75) {
            pool.addAll(HARD_ROOTS);
        }
        return PitchClass.parse(pick(pool));
    }

    private Interval randomInterval(double difficulty) {
        List<Interval> easy = List.of(Interval.MAJOR_THIRD, Interval.MINOR_THIRD, Interval.PERFECT_FIFTH,
                Interval.PERFECT_FOURTH, Interval.MAJOR_SECOND);
        List<Interval> harder = List.of(Interval.MAJOR_SIXTH, Interval.MINOR_SIXTH, Interval.MINOR_SEVENTH,
                Interval.MAJOR_SEVENTH, Interval.AUGMENTED_FOURTH, Interval.DIMINISHED_FIFTH, Interval.MINOR_SECOND);
        List<Interval> pool = new ArrayList<>(easy);
        if (difficulty >= 0.45) {
            pool.addAll(harder);
        }
        return pick(pool);
    }

    private static List<String> enharmonics(PitchClass pitchClass) {
        List<String> result = new ArrayList<>();
        for (PitchClass candidate : fr.lapetina.music.theory.ChordAnalyzer
                .spellingCandidates(pitchClass.semitone(), null)) {
            if (!candidate.equals(pitchClass) && Math.abs(candidate.accidental().offset()) <= 1) {
                result.add(candidate.name());
            }
        }
        return result.isEmpty() ? List.of(pitchClass.name()) : result;
    }

    private static String intervalName(Interval interval) {
        return qualityWord(interval.quality()) + " " + ordinalWord(interval.number());
    }

    private static String shortIntervalName(Interval interval) {
        return qualityWord(interval.quality()) + " " + interval.number();
    }

    private static String qualityWord(IntervalQuality quality) {
        return switch (quality) {
            case DIMINISHED -> "diminished";
            case MINOR -> "minor";
            case PERFECT -> "perfect";
            case MAJOR -> "major";
            case AUGMENTED -> "augmented";
        };
    }

    private static String qualityWord(ChordQuality quality) {
        return switch (quality) {
            case MAJOR -> "major";
            case MINOR -> "minor";
            case DIMINISHED -> "diminished";
            case AUGMENTED -> "augmented";
            default -> quality.displayName();
        };
    }

    private static String functionWord(HarmonicFunction function) {
        return switch (function) {
            case TONIC -> "tonic";
            case PREDOMINANT -> "predominant";
            case DOMINANT -> "dominant";
            case APPLIED_DOMINANT -> "applied dominant";
            case CHROMATIC -> "chromatic";
        };
    }

    private static String ordinalWord(int number) {
        return switch (number) {
            case 1 -> "unison";
            case 2 -> "second";
            case 3 -> "third";
            case 4 -> "fourth";
            case 5 -> "fifth";
            case 6 -> "sixth";
            case 7 -> "seventh";
            case 8 -> "octave";
            default -> number + "th";
        };
    }

    private static String describeSignature(int signature) {
        if (signature == 0) {
            return "no sharps or flats";
        }
        int count = Math.abs(signature);
        String kind = signature > 0 ? "sharp" : "flat";
        return count + " " + kind + (count == 1 ? "" : "s");
    }

    private static String numberWord(int number) {
        return switch (number) {
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            default -> String.valueOf(number);
        };
    }
}
