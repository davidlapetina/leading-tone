package fr.lapetina.music.llm.tools;

import dev.langchain4j.agent.tool.Tool;
import fr.lapetina.music.theory.AbcNotation;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Mode;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.ProgressionAnalysis;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;

/**
 * The theory engine, exposed to the language model.
 *
 * <p>Everything here is a question the model can ask and get a correct answer to, so it
 * never has to work out harmony in its head. It is all read-only: none of these tools
 * can change what the application believes about the learner.
 */
@ApplicationScoped
public class TheoryTools {

    @Tool("""
            Identify a chord from note names or MIDI numbers, for example "B D G" or "59 62 67". \
            Returns the chord symbol, its quality and its inversion.""")
    public String analyzeChord(String notes) {
        String trimmed = notes == null ? "" : notes.trim();
        if (trimmed.isEmpty()) {
            return "No notes given.";
        }
        try {
            List<String> tokens = Arrays.asList(trimmed.split("[\\s,]+"));
            if (tokens.stream().allMatch(token -> token.matches("\\d+"))) {
                List<Integer> midi = tokens.stream().map(Integer::parseInt).toList();
                return ChordAnalyzer.fromMidi(midi)
                        .map(chord -> "%s — %s. Notes: %s.".formatted(chord.symbol(), chord.describe(),
                                names(chord)))
                        .orElse("Those notes do not form a chord this engine recognises.");
            }
            List<Note> spelled = tokens.stream().map(token -> new Note(PitchClass.parse(token), 4)).toList();
            return ChordAnalyzer.fromNotes(spelled)
                    .map(chord -> "%s — %s. Notes: %s.".formatted(chord.symbol(), chord.describe(), names(chord)))
                    .orElse("Those notes do not form a chord this engine recognises.");
        } catch (RuntimeException e) {
            return "Could not read those notes: " + e.getMessage();
        }
    }

    @Tool("""
            Analyse a chord progression in a key. Give the chords as symbols separated by spaces, \
            for example "C F G7 C", and the key as "C major" or "A minor". \
            Returns Roman numerals, applied chords and cadences.""")
    public String analyzeProgression(String chords, String key) {
        try {
            Key parsedKey = parseKey(key);
            List<Chord> parsed = Arrays.stream(chords.trim().split("[\\s,]+"))
                    .filter(token -> !token.isBlank())
                    .map(ChordAnalyzer::parse)
                    .toList();
            ProgressionAnalysis analysis = ProgressionAnalyzer.analyze(parsed, parsedKey);
            return analysis.summary();
        } catch (RuntimeException e) {
            return "Could not analyse that progression: " + e.getMessage();
        }
    }

    @Tool("""
            Spell a chord from a root and a quality, for example root "F#" and quality "m7". \
            Inversion may be "root", "first", "second" or "third".""")
    public String buildChord(String root, String quality, String inversion) {
        try {
            String symbol = root.trim() + (quality == null ? "" : quality.trim());
            Chord chord = ChordAnalyzer.parse(symbol);
            Chord placed = switch (inversion == null ? "root" : inversion.trim().toLowerCase()) {
                case "first", "1", "first inversion" -> chord.inverted(fr.lapetina.music.theory.Inversion.FIRST);
                case "second", "2", "second inversion" -> chord.inverted(fr.lapetina.music.theory.Inversion.SECOND);
                case "third", "3", "third inversion" -> chord.inverted(fr.lapetina.music.theory.Inversion.THIRD);
                default -> chord;
            };
            return "%s (%s): %s. Bass: %s.".formatted(placed.symbol(), placed.describe(), names(placed),
                    placed.bass().name());
        } catch (RuntimeException e) {
            return "Could not build that chord: " + e.getMessage();
        }
    }

    @Tool("""
            Spell a scale. Type may be MAJOR, NATURAL_MINOR, HARMONIC_MINOR, MELODIC_MINOR, \
            DORIAN, PHRYGIAN, LYDIAN, MIXOLYDIAN or LOCRIAN.""")
    public String describeScale(String tonic, String type) {
        try {
            Scale scale = new Scale(PitchClass.parse(tonic.trim()),
                    ScaleType.valueOf(type.trim().toUpperCase().replace(' ', '_')));
            return "%s: %s".formatted(scale.name(),
                    String.join(" ", scale.pitchClasses().stream().map(PitchClass::name).toList()));
        } catch (RuntimeException e) {
            return "Could not spell that scale: " + e.getMessage();
        }
    }

    @Tool("Name the interval between two notes, for example \"C\" and \"F#\".")
    public String describeInterval(String from, String to) {
        try {
            Interval interval = Interval.between(PitchClass.parse(from.trim()), PitchClass.parse(to.trim()));
            return "%s up to %s is a %s (%d semitones)."
                    .formatted(from.trim(), to.trim(), interval.symbol(), interval.semitones());
        } catch (RuntimeException e) {
            return "Could not measure that interval: " + e.getMessage();
        }
    }

    @Tool("""
            List the diatonic triads and sevenths of a key, and its key signature. \
            Give the key as "Eb major" or "C# minor".""")
    public String describeKey(String key) {
        try {
            Key parsed = parseKey(key);
            int signature = parsed.keySignature();
            String accidentals = signature == 0 ? "no sharps or flats"
                    : Math.abs(signature) + (signature > 0 ? " sharps" : " flats");
            return """
                    %s has %s.
                    Triads: %s
                    Sevenths: %s""".formatted(parsed.name(), accidentals,
                    String.join(" ", parsed.diatonicTriads().stream().map(Chord::symbol).toList()),
                    String.join(" ", parsed.diatonicSevenths().stream().map(Chord::symbol).toList()));
        } catch (RuntimeException e) {
            return "Could not describe that key: " + e.getMessage();
        }
    }

    @Tool("""
            Produce ABC notation the learner's screen can render. Kind must be "chord", "scale" \
            or "progression". Content is a chord symbol, a scale as "D MAJOR", or chord symbols \
            separated by spaces.""")
    public String createNotation(String kind, String content, String key) {
        try {
            Key parsedKey = key == null || key.isBlank() ? Key.major("C") : parseKey(key);
            return switch (kind.trim().toLowerCase()) {
                case "chord" -> AbcNotation.chord(ChordAnalyzer.parse(content.trim()), AbcNotation.CHORD_OCTAVE, parsedKey);
                case "scale" -> {
                    String[] parts = content.trim().split("\\s+");
                    yield AbcNotation.scale(new Scale(PitchClass.parse(parts[0]),
                            ScaleType.valueOf(parts.length > 1 ? parts[1].toUpperCase() : "MAJOR")), 4);
                }
                case "progression" -> AbcNotation.progression(
                        Arrays.stream(content.trim().split("[\\s,]+")).map(ChordAnalyzer::parse).toList(),
                        parsedKey, 3);
                default -> "Unknown notation kind: " + kind;
            };
        } catch (RuntimeException e) {
            return "Could not create that notation: " + e.getMessage();
        }
    }

    private static String names(Chord chord) {
        return String.join(" ", chord.pitchClasses().stream().map(PitchClass::name).toList());
    }

    static Key parseKey(String key) {
        return Key.parse(key);
    }
}
