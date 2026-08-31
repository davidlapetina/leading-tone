package fr.lapetina.music.theory;

public enum Inversion {

    ROOT_POSITION(0, "root position"),
    FIRST(1, "first inversion"),
    SECOND(2, "second inversion"),
    THIRD(3, "third inversion");

    private final int index;
    private final String displayName;

    Inversion(int index, String displayName) {
        this.index = index;
        this.displayName = displayName;
    }

    /** Which chord tone is in the bass: 0 is the root, 1 the third, 2 the fifth, 3 the seventh. */
    public int index() {
        return index;
    }

    public String displayName() {
        return displayName;
    }

    public static Inversion ofIndex(int index) {
        for (Inversion inversion : values()) {
            if (inversion.index == index) {
                return inversion;
            }
        }
        throw new IllegalArgumentException("No inversion with bass chord-tone index " + index);
    }

    /** Figured bass depends on how many notes the chord has: a first-inversion triad is 6, a seventh is 65. */
    public String figuredBass(int chordSize) {
        if (chordSize >= 4) {
            return switch (this) {
                case ROOT_POSITION -> "7";
                case FIRST -> "65";
                case SECOND -> "43";
                case THIRD -> "42";
            };
        }
        return switch (this) {
            case ROOT_POSITION -> "";
            case FIRST -> "6";
            case SECOND -> "64";
            case THIRD -> throw new IllegalStateException("A triad has no third inversion");
        };
    }
}
