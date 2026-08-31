package fr.lapetina.music.theory;

import java.util.List;
import java.util.stream.Collectors;

public record ProgressionAnalysis(Key key, List<ChordAnalysis> chords, List<CadencePoint> cadences) {

    public String romanNumeralLine() {
        return chords.stream().map(ChordAnalysis::romanNumeralSymbol).collect(Collectors.joining(" - "));
    }

    public boolean allDiatonic() {
        return chords.stream().allMatch(ChordAnalysis::diatonic);
    }

    public String summary() {
        StringBuilder builder = new StringBuilder();
        builder.append("In ").append(key.name()).append(": ").append(romanNumeralLine());
        if (!cadences.isEmpty()) {
            String names = cadences.stream()
                    .map(point -> point.cadence().displayName())
                    .collect(Collectors.joining(", "));
            builder.append(". Cadences: ").append(names);
        }
        return builder.append('.').toString();
    }
}
