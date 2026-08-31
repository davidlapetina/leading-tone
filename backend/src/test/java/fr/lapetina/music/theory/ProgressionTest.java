package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgressionTest {

    private static List<String> symbols(List<Chord> chords) {
        return chords.stream().map(Chord::symbol).toList();
    }

    @Test
    void parsesAndRealisesAProgression() {
        Progression progression = Progression.parse("ii7 V7 Imaj7");

        assertEquals(List.of("Dm7", "G7", "Cmaj7"), symbols(progression.realize(Key.major("C"))));
        assertEquals(List.of("Fm7", "Bb7", "Ebmaj7"), symbols(progression.realize(Key.major("Eb"))));
    }

    @Test
    void acceptsTheWaysPeopleWriteAProgression() {
        assertEquals(Progression.parse("ii7 V7 Imaj7"), Progression.parse("ii7 - V7 - Imaj7"));
        assertEquals(Progression.parse("ii7 V7 Imaj7"), Progression.parse("ii7–V7–Imaj7"));
    }

    @Test
    @DisplayName("Roman numerals do not move when the music is transposed; the key does")
    void transposes() {
        Progression twoFiveOne = Progression.parse("ii7 V7 Imaj7");

        assertEquals(List.of("Em7", "A7", "Dmaj7"),
                symbols(twoFiveOne.transpose(Key.major("C"), Key.major("D"))));
        assertEquals("ii7 - V7 - IM7", twoFiveOne.symbol(), "the numerals are unchanged");
    }

    @Test
    @DisplayName("the minor two-five-one has a half-diminished ii, which is what makes it minor")
    void realisesAMinorTwoFiveOne() {
        assertEquals(List.of("Dm7b5", "G7", "Cm"),
                symbols(Progression.parse("iiø7 V7 i").realize(Key.minor("C"))));
    }

    @Test
    @DisplayName("a secondary dominant inside a progression is realised in the key it points at")
    void realisesAppliedChordsInContext() {
        assertEquals(List.of("C", "D7", "G", "C"),
                symbols(Progression.parse("I V7/V V I").realize(Key.major("C"))));
        assertNotEquals("Dm", symbols(Progression.parse("V7/V").realize(Key.major("C"))).get(0));
    }
}
