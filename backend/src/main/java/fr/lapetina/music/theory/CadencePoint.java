package fr.lapetina.music.theory;

/** A cadence found between the chord at {@code fromIndex} and the one after it. */
public record CadencePoint(int fromIndex, Cadence cadence) {
}
