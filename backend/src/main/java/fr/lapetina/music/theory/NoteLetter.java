package fr.lapetina.music.theory;

/**
 * The seven natural letter names. {@code semitone} is the pitch class of the natural
 * form; {@code diatonicIndex} is the position in the letter cycle, which is what makes
 * correct enharmonic spelling possible (F# and Gb share a semitone but not a letter).
 */
public enum NoteLetter {

    C(0, 0),
    D(2, 1),
    E(4, 2),
    F(5, 3),
    G(7, 4),
    A(9, 5),
    B(11, 6);

    private final int semitone;
    private final int diatonicIndex;

    NoteLetter(int semitone, int diatonicIndex) {
        this.semitone = semitone;
        this.diatonicIndex = diatonicIndex;
    }

    public int semitone() {
        return semitone;
    }

    public int diatonicIndex() {
        return diatonicIndex;
    }

    public static NoteLetter ofDiatonicIndex(int index) {
        int normalized = Math.floorMod(index, 7);
        for (NoteLetter letter : values()) {
            if (letter.diatonicIndex == normalized) {
                return letter;
            }
        }
        throw new IllegalStateException("unreachable");
    }

    public NoteLetter step(int diatonicSteps) {
        return ofDiatonicIndex(diatonicIndex + diatonicSteps);
    }

    public static NoteLetter parse(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'C' -> C;
            case 'D' -> D;
            case 'E' -> E;
            case 'F' -> F;
            case 'G' -> G;
            case 'A' -> A;
            case 'B' -> B;
            default -> throw new IllegalArgumentException("Not a note letter: " + c);
        };
    }
}
