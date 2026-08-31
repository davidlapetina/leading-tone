package fr.lapetina.music.theory;

public enum Mode {

    MAJOR(ScaleType.MAJOR),
    MINOR(ScaleType.NATURAL_MINOR);

    private final ScaleType scaleType;

    Mode(ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    public ScaleType scaleType() {
        return scaleType;
    }
}
