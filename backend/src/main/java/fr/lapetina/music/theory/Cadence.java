package fr.lapetina.music.theory;

public enum Cadence {

    PERFECT_AUTHENTIC("perfect authentic cadence"),
    IMPERFECT_AUTHENTIC("imperfect authentic cadence"),
    HALF("half cadence"),
    PLAGAL("plagal cadence"),
    DECEPTIVE("deceptive cadence"),
    NONE("no cadence");

    private final String displayName;

    Cadence(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
