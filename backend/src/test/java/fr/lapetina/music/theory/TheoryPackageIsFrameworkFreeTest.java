package fr.lapetina.music.theory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The theory engine knows nothing about the application it lives in.
 *
 * <p>Stated in docs/architecture.md as a rule, and enforced here so it stays true. It is
 * what lets the engine be reasoned about, tested and trusted on its own: the thing that
 * marks a learner's answer has no opinion about HTTP, persistence or language models.
 */
class TheoryPackageIsFrameworkFreeTest {

    private static final List<String> FORBIDDEN =
            List.of("jakarta.", "javax.", "io.quarkus", "dev.langchain4j", "org.apache.lucene",
                    "com.fasterxml", "org.hibernate");

    @Test
    @DisplayName("nothing in the theory package imports a framework")
    void importsNoFramework() throws IOException {
        Path root = Path.of("src/main/java/fr/lapetina/music/theory");
        List<String> offences = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String forbidden : FORBIDDEN) {
                        if (line.contains(forbidden)) {
                            offences.add(file.getFileName() + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertTrue(offences.isEmpty(), "the theory package must stay framework-free, but found: " + offences);
    }
}
