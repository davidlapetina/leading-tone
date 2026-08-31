package fr.lapetina.music.theory;

import java.util.Objects;

/**
 * A spelled pitch class: a letter plus an accidental. Deliberately not reduced to twelve
 * values, because F# and Gb behave differently in analysis even though
 * {@code semitone()} is 6 for both.
 */
public record PitchClass(NoteLetter letter, Accidental accidental) {

    public PitchClass {
        Objects.requireNonNull(letter, "letter");
        Objects.requireNonNull(accidental, "accidental");
    }

    public static PitchClass of(NoteLetter letter) {
        return new PitchClass(letter, Accidental.NATURAL);
    }

    public static PitchClass of(NoteLetter letter, Accidental accidental) {
        return new PitchClass(letter, accidental);
    }

    /** 0..11, where C natural is 0. */
    public int semitone() {
        return Math.floorMod(letter.semitone() + accidental.offset(), 12);
    }

    public String name() {
        return letter.name() + accidental.symbol();
    }

    /**
     * The same letter, moved by semitones. Used for numerals measured from the major scale:
     * flat-six in C is A flat, which is A altered down, not the key's own sixth degree
     * lowered again.
     */
    public PitchClass alter(int semitones) {
        return new PitchClass(letter, Accidental.ofOffset(accidental.offset() + semitones));
    }

    public boolean isEnharmonicWith(PitchClass other) {
        return semitone() == other.semitone();
    }

    /** Transposition that respects spelling: C transposed up an A4 is F#, never Gb. */
    public PitchClass transpose(Interval interval) {
        NoteLetter targetLetter = letter.step(interval.diatonicSteps());
        int targetSemitone = Math.floorMod(semitone() + interval.semitones(), 12);
        int offset = targetSemitone - targetLetter.semitone();
        if (offset > 6) {
            offset -= 12;
        } else if (offset < -6) {
            offset += 12;
        }
        if (offset < -2 || offset > 2) {
            throw new IllegalArgumentException(
                    "Transposing " + name() + " by " + interval.symbol() + " needs more than a double accidental");
        }
        return new PitchClass(targetLetter, Accidental.ofOffset(offset));
    }

    public static PitchClass parse(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty pitch class");
        }
        NoteLetter parsedLetter = NoteLetter.parse(trimmed.charAt(0));
        Accidental parsedAccidental = Accidental.parse(trimmed.substring(1));
        return new PitchClass(parsedLetter, parsedAccidental);
    }

    @Override
    public String toString() {
        return name();
    }
}
