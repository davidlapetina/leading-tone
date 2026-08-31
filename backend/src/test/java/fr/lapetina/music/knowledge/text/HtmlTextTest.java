package fr.lapetina.music.knowledge.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmlTextTest {

    @Test
    void dropsElementsWholeRatherThanJustTheirTags() {
        String text = HtmlText.toText("<p>Kept.</p><script>var evil = 1;</script><iframe>x</iframe>");

        assertTrue(text.contains("Kept."));
        assertFalse(text.contains("evil"));
        assertFalse(text.contains("<"));
    }

    @Test
    @DisplayName("a flat sign survives as a flat sign, because B and B flat are different notes")
    void keepsMusicalEntities() {
        assertTrue(HtmlText.toText("<p>B&#9837; major</p>").contains("♭"));
        assertTrue(HtmlText.toText("<p>F&#9839;</p>").contains("♯"));
        assertEquals("A & B", HtmlText.toText("<p>A &amp; B</p>"));
    }

    @Test
    @DisplayName("scale degrees written as LaTeX become scale degrees, not shortcode noise")
    void translatesLatexShortcodes() {
        String text = HtmlText.toText(
                "<h2>Ger+6: me vs. ri [latex](\\downarrow\\hat3[/latex] vs. [latex]\\uparrow\\hat2)[/latex]</h2>");

        assertEquals("Ger+6: me vs. ri (↓3̂ vs. ↑2̂)", text);
        assertFalse(text.contains("latex"));
        assertFalse(text.contains("\\"));
    }

    @Test
    void leavesOrdinaryTextAlone() {
        assertEquals("A secondary dominant tonicizes V7/V.",
                HtmlText.toText("<p>A secondary dominant tonicizes V7/V.</p>"));
    }

    @Test
    void describesAnImageByItsAltTextOrNotAtAll() {
        assertTrue(HtmlText.toText("<img alt=\"a cadence\" src=\"x.png\">").contains("[figure: a cadence]"));
        assertEquals("", HtmlText.toText("<img src=\"x.png\">"));
    }
}
