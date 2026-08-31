package fr.lapetina.music.knowledge.license;

/**
 * How this deployment is being used, which decides what its sources permit.
 *
 * <p>Thirteen of the fourteen configured sources are NonCommercial. That restriction is a
 * condition their authors placed on the work, and it survives every transformation this
 * application performs: downloading, parsing, chunking, embedding and indexing do not
 * discharge it. So the mode is not a display preference — it decides what may be served.
 */
public enum RuntimeMode {

    /** Personal study, teaching, research. Every configured source is usable. */
    NON_COMMERCIAL,

    /**
     * Any commercial use. NonCommercial sources are refused, which in this configuration
     * means the Jazz Harmony Treebank and every annotated score corpus. Using them
     * commercially needs separate permission from the rights holders, recorded outside this
     * application.
     */
    COMMERCIAL;

    public static RuntimeMode parse(String text) {
        if (text == null || text.isBlank()) {
            return NON_COMMERCIAL;
        }
        try {
            return valueOf(text.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(
                    "Unknown runtime mode: " + text + ". Expected NON_COMMERCIAL or COMMERCIAL.");
        }
    }

    public boolean isCommercial() {
        return this == COMMERCIAL;
    }
}
