package fr.lapetina.music.theory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which accidentals are already in force, so a note is written as the pitch it is.
 *
 * <p>A key signature alters every note on its letters, and an accidental written in a bar
 * holds for the rest of that bar. Writing a note without regard to either draws a different
 * pitch: in F minor a plain {@code D} is D flat, so a D natural written that way is not the
 * note the composer wrote. It is also the reason the naive fix -- an explicit accidental on
 * everything -- is wrong in the other direction, cluttering the staff with signs a reader
 * does not need.
 *
 * <p>Not thread safe, and not meant to be: one of these belongs to one voice being written.
 */
public final class AccidentalState {

    private final Set<NoteLetter> signature = new HashSet<>();
    private final Accidental signatureAccidental;
    private final Map<String, Accidental> inThisBar = new HashMap<>();

    public AccidentalState(Key key) {
        Accidental found = Accidental.NATURAL;
        for (PitchClass pitchClass : key.scale().pitchClasses()) {
            if (pitchClass.accidental() != Accidental.NATURAL) {
                signature.add(pitchClass.letter());
                found = pitchClass.accidental();
            }
        }
        this.signatureAccidental = found;
    }

    /** A bar line cancels every accidental written in the bar; the signature stays. */
    public void barLine() {
        inThisBar.clear();
    }

    /**
     * The accidental to write before this note: empty when it is already in force.
     *
     * <p>Standard practice, which ABC follows: an accidental holds for the same letter in the
     * same octave until the bar line.
     */
    public String accidentalFor(Note note) {
        Accidental wanted = note.pitchClass().accidental();
        String where = note.pitchClass().letter().name() + note.octave();
        Accidental inForce = inThisBar.containsKey(where)
                ? inThisBar.get(where)
                : (signature.contains(note.pitchClass().letter()) ? signatureAccidental : Accidental.NATURAL);
        if (wanted == inForce) {
            return "";
        }
        inThisBar.put(where, wanted);
        return switch (wanted) {
            case DOUBLE_FLAT -> "__";
            case FLAT -> "_";
            case NATURAL -> "=";
            case SHARP -> "^";
            case DOUBLE_SHARP -> "^^";
        };
    }
}
