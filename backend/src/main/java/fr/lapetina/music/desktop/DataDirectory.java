package fr.lapetina.music.desktop;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Where a packaged application keeps its database, its index and its downloaded sources.
 *
 * <p>Run from a terminal, everything lives in {@code ./data} beside where you started it,
 * which is what a developer wants. Run from an icon, there is no such place: a launched
 * application's working directory is the root of the disk, so the same default sends H2 to
 * create {@code /data} and the application dies before it has drawn anything.
 *
 * <p>So the packaged launcher asks for a real one. This is deliberately not a CDI bean and
 * deliberately not configuration: it has to answer before the configuration is read, because
 * what it answers is where the database is.
 */
public final class DataDirectory {

    /** The name the operating systems will show a person who goes looking for it. */
    private static final String APPLICATION = "Leading Tone";

    /** Set by the packaged launcher, absent everywhere else. */
    static final String PACKAGED = "music.packaged";

    /** The property the datasource and the knowledge layer both read. */
    static final String DATA_DIR = "MUSIC_DATA_DIR";

    private DataDirectory() {}

    /**
     * Chooses the data directory, unless something has already chosen one.
     *
     * <p>An explicit {@code MUSIC_DATA_DIR} always wins, so a person can keep their data on a
     * different disk and the tests can keep theirs in {@code target}. Without one, only a
     * packaged application overrides the default; from a terminal {@code ./data} is left
     * alone, and someone's existing work stays where they left it.
     */
    public static void choose() {
        if (System.getProperty(DATA_DIR) != null || System.getenv(DATA_DIR) != null) {
            return;
        }
        if (!Boolean.getBoolean(PACKAGED)) {
            return;
        }
        Path home = perUserDirectory();
        try {
            Files.createDirectories(home);
        } catch (Exception couldNotCreate) {
            // Say so and carry on: Quarkus will fail on the database with a clearer message
            // than anything that could be thrown from here.
            System.err.println("Could not create " + home + ": " + couldNotCreate.getMessage());
        }
        System.setProperty(DATA_DIR, home.toString());
    }

    /** Where each operating system expects an application to keep a person's data. */
    static Path perUserDirectory() {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", APPLICATION);
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData == null || appData.isBlank()
                    ? Path.of(home, "AppData", "Roaming", APPLICATION)
                    : Path.of(appData, APPLICATION);
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        return xdg == null || xdg.isBlank()
                ? Path.of(home, ".local", "share", "leading-tone")
                : Path.of(xdg, "leading-tone");
    }
}
