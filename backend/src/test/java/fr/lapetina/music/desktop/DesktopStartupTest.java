package fr.lapetina.music.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Where a packaged application keeps its data.
 *
 * <p>The obvious implementation fails silently: a launched application's working directory is
 * the root of the disk, so the default {@code ./data} sends the database to {@code /data} and
 * the application dies before it has drawn anything. Nothing about that is visible from a
 * terminal, where the default is right.
 *
 * <p>The browser launcher is not tested here on purpose. Every honest test of it would open a
 * browser on whoever ran the suite.
 */
class DesktopStartupTest {

    private String realHome;

    @BeforeEach
    void keepOutOfTheRealHomeDirectory(@TempDir Path pretendHome) {
        // choose() creates the directory it picks, which is right in an application and
        // rude in a test: without this, running the suite leaves an empty folder in
        // whoever ran it's home.
        realHome = System.getProperty("user.home");
        System.setProperty("user.home", pretendHome.toString());
    }

    @AfterEach
    void clearWhatTheTestSet() {
        System.setProperty("user.home", realHome);
        System.clearProperty(DataDirectory.PACKAGED);
        System.clearProperty(DataDirectory.DATA_DIR);
    }

    @Test
    @DisplayName("from a terminal the data directory is left alone")
    void leavesTheWorkingDirectoryAloneWhenNotPackaged() {
        System.clearProperty(DataDirectory.DATA_DIR);

        DataDirectory.choose();

        assertNull(System.getProperty(DataDirectory.DATA_DIR),
                "unset means ./data, which is where a developer's work already is");
    }

    @Test
    @DisplayName("a packaged application is given somewhere it is allowed to write")
    void choosesAPerUserDirectoryWhenPackaged() {
        System.setProperty(DataDirectory.PACKAGED, "true");
        System.clearProperty(DataDirectory.DATA_DIR);

        DataDirectory.choose();

        Path chosen = Path.of(System.getProperty(DataDirectory.DATA_DIR));
        assertTrue(chosen.isAbsolute(), "a relative path is relative to the disk root: " + chosen);
        assertTrue(chosen.startsWith(System.getProperty("user.home")),
                "it belongs to the person, not to the installation: " + chosen);
    }

    @Test
    @DisplayName("a directory somebody chose themselves is never overridden")
    void respectsAnExplicitChoice() {
        System.setProperty(DataDirectory.PACKAGED, "true");
        System.setProperty(DataDirectory.DATA_DIR, "/somewhere/of/their/own");

        DataDirectory.choose();

        assertEquals("/somewhere/of/their/own", System.getProperty(DataDirectory.DATA_DIR),
                "somebody keeping their data on another disk keeps it there");
    }

    @Test
    @DisplayName("the per-user directory is the one the operating system expects")
    void followsTheConventionOfEachSystem() {
        Path chosen = DataDirectory.perUserDirectory();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        assertTrue(chosen.isAbsolute(), "" + chosen);
        if (os.contains("mac")) {
            assertTrue(chosen.endsWith(Path.of("Library", "Application Support", "Leading Tone")),
                    "macOS keeps application data here: " + chosen);
        } else if (os.contains("win")) {
            assertTrue(chosen.endsWith("Leading Tone"), "" + chosen);
        } else {
            assertTrue(chosen.toString().contains("leading-tone"), "" + chosen);
        }
    }
}
