package fr.lapetina.music.knowledge.license;

/**
 * How far the licence of a source has been established.
 *
 * <p>Only {@link #VERIFIED} and {@link #RESTRICTED} material may be ingested. Anything
 * else is refused, because guessing at a licence is how someone else's work ends up
 * being redistributed on terms its author never agreed to.
 */
public enum LicenseStatus {

    /** The licence was read from the authoritative upstream source and is permitted. */
    VERIFIED,

    /** Known, and usable, but carrying obligations worth surfacing (NonCommercial, ShareAlike). */
    RESTRICTED,

    /** Not established. Never ingested. */
    UNKNOWN,

    /** Established, and it does not permit us to use the material. Never ingested. */
    REJECTED
}
