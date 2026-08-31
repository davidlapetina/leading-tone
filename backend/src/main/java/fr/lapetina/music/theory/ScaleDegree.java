package fr.lapetina.music.theory;

public enum ScaleDegree {

    TONIC(1),
    SUPERTONIC(2),
    MEDIANT(3),
    SUBDOMINANT(4),
    DOMINANT(5),
    SUBMEDIANT(6),
    LEADING_TONE(7);

    private final int number;

    ScaleDegree(int number) {
        this.number = number;
    }

    public int number() {
        return number;
    }

    public static ScaleDegree of(int number) {
        for (ScaleDegree degree : values()) {
            if (degree.number == number) {
                return degree;
            }
        }
        throw new IllegalArgumentException("Scale degree out of range: " + number);
    }

    /** In minor keys the unraised seventh is a subtonic rather than a leading tone. */
    public String displayName(Mode mode, boolean raisedSeventh) {
        if (this == LEADING_TONE && mode == Mode.MINOR && !raisedSeventh) {
            return "Subtonic";
        }
        String lower = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
