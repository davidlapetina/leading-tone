package fr.lapetina.music.api;

import fr.lapetina.music.api.dto.Requests;
import fr.lapetina.music.llm.tools.TheoryTools;
import fr.lapetina.music.theory.AbcNotation;
import fr.lapetina.music.theory.Chord;
import fr.lapetina.music.theory.ChordAnalyzer;
import fr.lapetina.music.theory.Key;
import fr.lapetina.music.theory.Mode;
import fr.lapetina.music.theory.Note;
import fr.lapetina.music.theory.PitchClass;
import fr.lapetina.music.theory.ProgressionAnalysis;
import fr.lapetina.music.theory.ProgressionAnalyzer;
import fr.lapetina.music.theory.Scale;
import fr.lapetina.music.theory.ScaleType;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The theory engine over HTTP. The frontend uses it for the keyboard readout, and it is
 * the same code the language model's tools call.
 */
@Path("/api/theory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TheoryResource {

    @Inject
    TheoryTools theoryTools;

    @POST
    @Path("/chord/analyze")
    public Map<String, Object> analyzeChord(Requests.ChordAnalysisRequest request) {
        Key key = request.key() == null || request.key().isBlank() ? null : parseKey(request.key());
        Chord chord;
        if (request.midiNotes() != null && !request.midiNotes().isEmpty()) {
            chord = ChordAnalyzer.fromMidi(request.midiNotes(), key).orElse(null);
        } else if (request.notes() != null && !request.notes().isBlank()) {
            List<Note> notes = Arrays.stream(request.notes().trim().split("[\\s,]+"))
                    .map(token -> new Note(PitchClass.parse(token), 4))
                    .toList();
            chord = ChordAnalyzer.fromNotes(notes).orElse(null);
        } else {
            throw new BadRequestException("Give either midiNotes or notes");
        }
        if (chord == null) {
            return Map.of("recognised", false);
        }
        return Map.of(
                "recognised", true,
                "symbol", chord.symbol(),
                "description", chord.describe(),
                "root", chord.root().name(),
                "quality", chord.quality().name(),
                "inversion", chord.inversion().name(),
                "bass", chord.bass().name(),
                "notes", chord.pitchClasses().stream().map(PitchClass::name).toList(),
                "abc", AbcNotation.chord(chord, 3, key == null ? Key.major("C") : key));
    }

    @POST
    @Path("/progression/analyze")
    public Map<String, Object> analyzeProgression(@Valid Requests.ProgressionRequest request) {
        Key key = parseKey(request.key());
        List<Chord> chords = request.chords().stream().map(ChordAnalyzer::parse).toList();
        ProgressionAnalysis analysis = ProgressionAnalyzer.analyze(chords, key);
        return Map.of(
                "key", key.name(),
                "romanNumerals", analysis.chords().stream().map(chord -> chord.romanNumeralSymbol()).toList(),
                "functions", analysis.chords().stream().map(chord -> chord.function().name()).toList(),
                "allDiatonic", analysis.allDiatonic(),
                "cadences", analysis.cadences().stream()
                        .map(point -> Map.of("afterChord", point.fromIndex(),
                                "cadence", point.cadence().displayName()))
                        .toList(),
                "summary", analysis.summary(),
                "abc", AbcNotation.progression(chords, key, 3));
    }

    @GET
    @Path("/key/{key}")
    public Map<String, Object> describeKey(@PathParam("key") String keyText) {
        Key key = parseKey(keyText.replace('_', ' '));
        return Map.of(
                "name", key.name(),
                "keySignature", key.keySignature(),
                "scale", key.scale().pitchClasses().stream().map(PitchClass::name).toList(),
                "triads", key.diatonicTriads().stream().map(Chord::symbol).toList(),
                "sevenths", key.diatonicSevenths().stream().map(Chord::symbol).toList(),
                "relative", key.relative().name(),
                "parallel", key.parallel().name());
    }

    @GET
    @Path("/scale/{tonic}/{type}")
    public Map<String, Object> describeScale(@PathParam("tonic") String tonic, @PathParam("type") String type) {
        Scale scale = new Scale(PitchClass.parse(tonic), ScaleType.valueOf(type.toUpperCase()));
        return Map.of(
                "name", scale.name(),
                "notes", scale.pitchClasses().stream().map(PitchClass::name).toList(),
                "abc", AbcNotation.scale(scale, 4));
    }

    @POST
    @Path("/notation")
    public Map<String, Object> notation(@Valid Requests.NotationRequest request) {
        return Map.of("abc", theoryTools.createNotation(request.kind(), request.content(), request.key()));
    }

    private static Key parseKey(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 0) {
            throw new BadRequestException("Unreadable key: " + text);
        }
        Mode mode = parts.length > 1 && parts[1].toLowerCase().startsWith("min") ? Mode.MINOR : Mode.MAJOR;
        return new Key(PitchClass.parse(parts[0]), mode);
    }
}
