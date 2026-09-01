package fr.lapetina.music.desktop;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Opens the interface when the application is started from an icon.
 *
 * <p>Without this, a person who installs the application sees a window of log output and no
 * tutor, and has to be told to type an address. It runs only when the packaged launcher asks
 * for it, so a developer running the server does not get a browser window on every restart.
 *
 * <p>Not {@code java.awt.Desktop}: the server runs headless, and under
 * {@code java.awt.headless=true} {@code Desktop.isDesktopSupported()} is false, so the call
 * that looks obvious does nothing at all. Asking the operating system directly also works on
 * a machine with no desktop toolkit installed.
 */
@ApplicationScoped
public class BrowserLauncher {

    private static final Logger LOG = Logger.getLogger(BrowserLauncher.class);

    /**
     * Chromium first, then Firefox, and only then whatever the machine prefers.
     *
     * <p>This is not a favourite: it is the Web MIDI API. Firefox and the Chromium browsers
     * implement it and Safari does not, so on a Mac — where the default browser is usually
     * Safari — opening the default browser is opening the one where a piano cannot be
     * plugged in. The application still teaches, and says why the keyboard is unavailable,
     * but it is a poor thing to choose for somebody when the alternative is installed.
     */
    private static final List<String> MAC = List.of(
            "Google Chrome", "Microsoft Edge", "Brave Browser", "Chromium", "Firefox");

    private static final List<String> WINDOWS = List.of("chrome", "msedge", "brave", "firefox");

    private static final List<String> LINUX = List.of(
            "google-chrome", "google-chrome-stable", "chromium", "chromium-browser",
            "microsoft-edge", "brave-browser", "firefox");

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8088")
    int port;

    // Last, so the browser opens onto an application that has finished starting rather than
    // onto whichever half of it happened to be ready.
    void onStart(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER + 100) StartupEvent event) {
        if (!Boolean.getBoolean(DataDirectory.PACKAGED)) {
            return;
        }
        String url = "http://localhost:" + port + "/";
        if (!open(url)) {
            // The one thing they must not be left without.
            System.out.println();
            System.out.println("  Leading Tone is running. Open " + url + " to use it.");
            System.out.println();
        }
    }

    /** True once a browser has been asked to open the address. */
    static boolean open(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            for (String browser : MAC) {
                if (run("open", "-a", browser, url)) {
                    return true;
                }
            }
            return run("open", url);
        }
        if (os.contains("win")) {
            for (String browser : WINDOWS) {
                // The empty string is the window title: without it the first quoted argument
                // is taken for one and the browser never opens.
                if (run("cmd", "/c", "start", "", browser, url)) {
                    return true;
                }
            }
            return run("cmd", "/c", "start", "", url);
        }
        for (String browser : LINUX) {
            if (which(browser) && run(browser, url)) {
                return true;
            }
        }
        return run("xdg-open", url);
    }

    private static boolean which(String command) {
        return run("which", command);
    }

    /**
     * Runs a command and reports whether it succeeded.
     *
     * <p>Bounded, because this is on the startup path: a browser that takes its time starting
     * must not hold the server, and a command that hangs must not stop the application from
     * running. Failing here is not an error — it is how the next candidate gets its turn.
     */
    private static boolean run(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroy();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException notThere) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (RuntimeException refused) {
            LOG.debugf("Could not run %s: %s", command[0], refused.getMessage());
            return false;
        }
    }
}
