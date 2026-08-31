package fr.lapetina.music.knowledge.attribution;

/**
 * How one source must be credited.
 *
 * @param citation the academic citation the publisher asks for, where they ask for one
 */
public record Attribution(
        String sourceId,
        String name,
        String licenseId,
        String licenseName,
        String licenseUrl,
        String sourceUrl,
        String citation,
        boolean attributionRequired,
        boolean shareAlikeRequired,
        boolean commercialUseAllowed) {

    /** The compact form for a source panel: "Open Music Theory · CC BY-SA 4.0". */
    public String shortCredit() {
        return name + " · " + licenseId.replace("CC-", "CC ").replace("-4.0", " 4.0");
    }
}
