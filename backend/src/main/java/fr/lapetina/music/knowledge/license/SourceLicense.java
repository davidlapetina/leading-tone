package fr.lapetina.music.knowledge.license;

/**
 * One licence in the manifest's licence vocabulary.
 *
 * <p>This describes the terms of somebody else's work. It is unrelated to the licence of
 * this application, which is MIT and applies only to the code in this repository. No
 * amount of downloading, parsing, chunking or embedding moves a source from its licence
 * to ours.
 *
 * @param textPath classpath-relative path to the full licence text, or null when the
 *     licence is one we record but do not redistribute the text of
 */
public record SourceLicense(
        String id,
        String name,
        String url,
        String textPath,
        boolean attributionRequired,
        boolean shareAlikeRequired,
        boolean derivativesAllowed,
        boolean commercialUseAllowed,
        boolean ingestible) {

    /** The licence used for anything whose terms could not be established. */
    public static SourceLicense unknown() {
        return new SourceLicense("UNKNOWN", "Unknown", null, null, true, false, false, false, false);
    }

    public LicenseStatus status() {
        if (!ingestible) {
            return LicenseStatus.REJECTED;
        }
        return shareAlikeRequired || !commercialUseAllowed ? LicenseStatus.RESTRICTED : LicenseStatus.VERIFIED;
    }

    /** A one-line form for a citation footer, e.g. {@code "CC BY-SA 4.0"}. */
    public String shortName() {
        return id.replace("CC-", "CC ").replace("-4.0", " 4.0").replace('-', ' ');
    }
}
