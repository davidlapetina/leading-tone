package fr.lapetina.music.theoryservice;

import fr.lapetina.music.theory.Cadence;
import fr.lapetina.music.theory.CadencePoint;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Interval;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.Progression;
import fr.lapetina.music.theory.ProgressionAnalysis;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import fr.lapetina.music.theory.RomanNumeralAnalysis;
import fr.lapetina.music.theory.RomanNumeralAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The one way into the music theory engine from the rest of the application.
 *
 * <p>It lives outside {@code fr.lapetina.music.theory} on purpose. The theory package has
 * no framework imports at all — no CDI, no Jakarta, nothing — and that is a property worth
 * being able to state without qualification, so the annotated service sits beside it
 * rather than inside it. There is a test that enforces it.
 *
 * <p>This is also where a question stops being a string. Everything above this line deals
 * in {@code "V7/V"} and {@code "C major"}; everything below deals in chords and keys. That
 * boundary used to be four separate copies of the same key parser.
 */
@ApplicationScoped
public class MusicTheoryService {

    public Key parseKey(String text) {
        return Key.parse(text);
    }

    public Optional<Key> tryParseKey(String text) {
        return Key.tryParse(text);
    }

    public Interval identifyInterval(String from, String to) {
        return Interval.between(PitchClass.parse(from), PitchClass.parse(to));
    }

    public PitchClass buildInterval(String from, String interval) {
        return PitchClass.parse(from).transpose(Interval.parse(interval));
    }

    public Note buildIntervalAbove(String note, String interval) {
        return Note.parse(note).transpose(Interval.parse(interval));
    }

    public Scale buildScale(String tonic, String scaleType) {
        return Scale.of(tonic, ScaleType.valueOf(scaleType.trim().toUpperCase(Locale.ROOT).replace(' ', '_')));
    }

    public Chord buildChord(String symbol) {
        return ChordAnalyzer.parse(symbol);
    }

    public Optional<Chord> analyzeChord(List<Integer> midiNotes, String key) {
        return tryParseKey(key)
                .map(context -> ChordAnalyzer.fromMidi(midiNotes, context))
                .orElseGet(() -> ChordAnalyzer.fromMidi(midiNotes));
    }

    /**
     * What a Roman numeral actually is in a key: the notes, the quality, the function, and
     * what it points at. The tutor uses this instead of asking a language model, because
     * the answer is arithmetic.
     */
    public RomanNumeralAnalysis realizeRomanNumeral(String numeral, String key) {
        return RomanNumeralAnalyzer.analyze(numeral, parseKey(key));
    }

    public RomanNumeralAnalysis analyzeRomanNumeral(String numeral, String key) {
        return realizeRomanNumeral(numeral, key);
    }

    public ProgressionAnalysis analyzeProgression(List<String> chordSymbols, String key) {
        return ProgressionAnalyzer.analyze(chordSymbols.stream().map(ChordAnalyzer::parse).toList(), parseKey(key));
    }

    public List<Chord> realizeProgression(String progression, String key) {
        return Progression.parse(progression).realize(parseKey(key));
    }

    public List<Chord> transposeProgression(String progression, String fromKey, String toKey) {
        return Progression.parse(progression).transpose(parseKey(fromKey), parseKey(toKey));
    }

    /**
     * The cadence a progression ends on, or none.
     *
     * <p>Conservative by construction: a chord the analyser could not explain, or a
     * chromatic one, produces no cadence claim rather than a guess. Note also that a
     * perfect authentic cadence properly wants the tonic in the soprano, and a chord
     * carries no voicing here — so "perfect" means V–I with both in root position, which is
     * what the engine can actually see.
     */
    public Cadence identifyCadence(List<String> chordSymbols, String key) {
        if (chordSymbols == null || chordSymbols.size() < 2) {
            return Cadence.NONE;
        }
        ProgressionAnalysis analysis = analyzeProgression(chordSymbols, key);
        int lastStep = chordSymbols.size() - 2;
        return analysis.cadences().stream()
                .filter(point -> point.fromIndex() == lastStep)
                .map(CadencePoint::cadence)
                .findFirst()
                .orElse(Cadence.NONE);
    }
}
