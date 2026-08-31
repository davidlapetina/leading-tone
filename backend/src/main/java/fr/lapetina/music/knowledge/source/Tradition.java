package fr.lapetina.music.knowledge.source;

/**
 * The practice a piece of knowledge belongs to.
 *
 * <p>This exists so the tutor can say "in jazz" or "in common-practice harmony" rather
 * than presenting one tradition's conventions as universal law. A jazz chord symbol and
 * a figured bass are both correct; they are not correct about the same thing.
 */
public enum Tradition {
    GENERAL,
    CLASSICAL,
    JAZZ,
    POPULAR,
    COUNTERPOINT,
    TWENTIETH_CENTURY
}
