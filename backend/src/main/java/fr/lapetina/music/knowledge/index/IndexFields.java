package fr.lapetina.music.knowledge.index;

/**
 * The Lucene field names, in one place.
 *
 * <p>Two text fields carry the body deliberately. {@link #CONTENT} is analysed for
 * ordinary English so that "cadences" finds "cadence". {@link #SYMBOL} is analysed
 * without lowercasing or stemming, because in this subject case is meaning: {@code V}
 * is a major chord and {@code v} is a minor one, and a search for {@code V7/V} must not
 * match every page that happens to contain a roman numeral.
 */
public final class IndexFields {

    public static final String CHUNK_ID = "chunkId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String SOURCE_ID = "sourceId";
    public static final String LICENSE_ID = "licenseId";

    public static final String DOCUMENT_TITLE = "documentTitle";
    public static final String SECTION_TITLE = "sectionTitle";
    public static final String CONTENT = "content";
    public static final String SYMBOL = "symbol";

    public static final String CONCEPT = "concept";
    public static final String TRADITION = "tradition";
    public static final String KIND = "kind";
    public static final String COMPOSER = "composer";
    public static final String WORK = "work";
    public static final String KEY = "key";
    public static final String ROMAN_NUMERAL = "romanNumeral";

    public static final String ATTRIBUTION = "attribution";
    public static final String URL = "url";
    public static final String ORDER = "order";
    public static final String WORD_COUNT = "wordCount";

    /** The embedding. Present only when an embedder was configured at build time. */
    public static final String VECTOR = "vector";

    private IndexFields() {}
}
