package fr.lapetina.music.theory;

/** How two voices move in relation to each other. The whole of counterpoint starts here. */
public enum Motion {

    /** Both voices move the same way, keeping the same interval between them. */
    PARALLEL("parallel motion"),

    /** Both voices move the same way, but the interval between them changes. */
    SIMILAR("similar motion"),

    /** The voices move in opposite directions. The most independent, and the safest. */
    CONTRARY("contrary motion"),

    /** One voice moves while the other holds. */
    OBLIQUE("oblique motion"),

    /** Neither voice moved. */
    STATIC("no motion");

    private final String displayName;

    Motion(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
