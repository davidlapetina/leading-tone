package fr.lapetina.music.theory;

public enum Accidental {

    DOUBLE_FLAT(-2, "bb"),
    FLAT(-1, "b"),
    NATURAL(0, ""),
    SHARP(1, "#"),
    DOUBLE_SHARP(2, "##");

    private final int offset;
    private final String symbol;

    Accidental(int offset, String symbol) {
        this.offset = offset;
        this.symbol = symbol;
    }

    public int offset() {
        return offset;
    }

    public String symbol() {
        return symbol;
    }

    public static Accidental ofOffset(int offset) {
        for (Accidental accidental : values()) {
            if (accidental.offset == offset) {
                return accidental;
            }
        }
        throw new IllegalArgumentException("No accidental for offset " + offset);
    }

    public static Accidental parse(String text) {
        return switch (text) {
            case "", "n" -> NATURAL;
            case "#", "s" -> SHARP;
            case "##", "x" -> DOUBLE_SHARP;
            case "b", "-" -> FLAT;
            case "bb", "--" -> DOUBLE_FLAT;
            default -> throw new IllegalArgumentException("Unknown accidental: " + text);
        };
    }
}
