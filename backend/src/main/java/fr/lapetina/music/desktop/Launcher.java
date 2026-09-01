package fr.lapetina.music.desktop;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * The way in, for every way of starting this.
 *
 * <p>It exists for one line: the data directory has to be settled before the configuration is
 * read, because what it settles is where the database file goes, and by the time anything
 * injectable exists that question has already been answered.
 *
 * <p>Everything else is unchanged. Started from a terminal this behaves exactly as it did
 * before, and the packaged launcher is the only thing that asks for anything different.
 */
@QuarkusMain
public class Launcher implements QuarkusApplication {

    public static void main(String... args) {
        DataDirectory.choose();
        Quarkus.run(Launcher.class, args);
    }

    @Override
    public int run(String... args) {
        Quarkus.waitForExit();
        return 0;
    }
}
