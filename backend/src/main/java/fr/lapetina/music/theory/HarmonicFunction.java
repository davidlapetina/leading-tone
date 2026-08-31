package fr.lapetina.music.theory;

public enum HarmonicFunction {

    TONIC,
    PREDOMINANT,
    DOMINANT,
    APPLIED_DOMINANT,
    CHROMATIC;

    public static HarmonicFunction forDegree(int degree) {
        return switch (Math.floorMod(degree - 1, 7) + 1) {
            case 1, 3, 6 -> TONIC;
            case 2, 4 -> PREDOMINANT;
            case 5, 7 -> DOMINANT;
            default -> CHROMATIC;
        };
    }
}
