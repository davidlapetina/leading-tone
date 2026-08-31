package fr.lapetina.music.concept;

/**
 * The practice a concept belongs to.
 *
 * <p>Exists so the tutor can say "in jazz" rather than presenting one tradition's
 * conventions as universal law. A ii-V-I and a cadential six-four are both correct; they
 * are not correct about the same thing, and somebody working through jazz should not be
 * told a chord symbol is wrong because a figured bass would write it differently.
 *
 * <p>{@link #GENERAL} is the default and covers most of the subject: intervals, scales and
 * triads belong to nobody in particular.
 */
public enum Tradition {
    GENERAL,
    CLASSICAL,
    JAZZ
}
