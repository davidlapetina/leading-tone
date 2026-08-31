package fr.lapetina.music.knowledge.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** SHA-256, used to tell whether anything actually changed. */
public final class Checksums {

    private Checksums() {}

    public static String of(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }

    /**
     * The key that decides whether an ingestion is a no-op.
     *
     * <p>Everything that could change the result is in it: the content itself, the parser
     * that read it, the chunking that cut it and the model that embedded it. Including the
     * embedding model is what makes changing the model force a rebuild instead of leaving
     * an index holding vectors from two models that cannot be compared.
     */
    public static String fingerprint(String sourceVersion, List<String> documentChecksums,
                                     int parserVersion, int chunkPolicyVersion,
                                     int analyzerVersion, String embeddingSignature) {
        return of(String.join("|",
                sourceVersion == null ? "" : sourceVersion,
                String.join(",", documentChecksums.stream().sorted().toList()),
                Integer.toString(parserVersion),
                Integer.toString(chunkPolicyVersion),
                Integer.toString(analyzerVersion),
                embeddingSignature));
    }
}
