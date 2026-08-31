package fr.lapetina.music.theory;

public enum IntervalQuality {

    DIMINISHED("d"),
    MINOR("m"),
    PERFECT("P"),
    MAJOR("M"),
    AUGMENTED("A");

    private final String symbol;

    IntervalQuality(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
