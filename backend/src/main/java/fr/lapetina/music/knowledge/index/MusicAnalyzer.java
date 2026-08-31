package fr.lapetina.music.knowledge.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

/**
 * How text is broken into terms for this subject.
 *
 * <p>Two things here are not the usual English defaults, and both matter:
 *
 * <ul>
 *   <li><strong>"a" and "i" are not stop words.</strong> A is a note and i is a roman
 *       numeral. Dropping them would make the index useless for exactly the queries this
 *       application exists to answer.
 *   <li><strong>Stemming is KStem, not Porter.</strong> Porter turns "cadences" into
 *       "cadenc" and "voicing" into "voic"; the same field is shown back to the learner in
 *       the diagnostic view, so the terms should stay words.
 * </ul>
 *
 * <p>Bump {@link #ANALYZER_VERSION} on any change here. It is part of the ingestion
 * fingerprint, so a change forces a rebuild rather than leaving an index whose stored
 * terms and query terms disagree.
 */
public final class MusicAnalyzer {

    public static final int ANALYZER_VERSION = 1;

    /** English stop words, less the two that are musical vocabulary. */
    static final CharArraySet MUSIC_SAFE_STOP_WORDS = musicSafeStopWords();

    private MusicAnalyzer() {}

    private static CharArraySet musicSafeStopWords() {
        // Built by copying rather than by removing: CharArraySet.remove() is a no-op, so
        // the obvious version of this method silently keeps "a" and "i" as stop words.
        List<String> kept = new ArrayList<>();
        for (Object word : EnglishAnalyzer.ENGLISH_STOP_WORDS_SET) {
            String text = word instanceof char[] chars ? new String(chars) : word.toString();
            if (!"a".equals(text) && !"i".equals(text)) {
                kept.add(text);
            }
        }
        return CharArraySet.unmodifiableSet(new CharArraySet(kept, true));
    }

    /** Ordinary prose: lowercased, de-possessived, stop-filtered and lightly stemmed. */
    public static Analyzer prose() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                MusicSymbolTokenizer source = new MusicSymbolTokenizer();
                TokenStream stream = new LowerCaseFilter(source);
                stream = new EnglishPossessiveFilter(stream);
                stream = new StopFilter(stream, MUSIC_SAFE_STOP_WORDS);
                stream = new KStemFilter(stream);
                return new TokenStreamComponents(source, stream);
            }
        };
    }

    /** Harmonic symbols: no lowercasing, no stemming, because case and sign are meaning. */
    public static Analyzer symbols() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                MusicSymbolTokenizer source = new MusicSymbolTokenizer();
                return new TokenStreamComponents(source, new MusicSymbolFilter(source));
            }
        };
    }

    /** The analyzer the index and every query uses. */
    public static Analyzer create() {
        Map<String, Analyzer> perField = new HashMap<>();
        perField.put(IndexFields.SYMBOL, symbols());
        perField.put(IndexFields.CHUNK_ID, new KeywordAnalyzer());
        perField.put(IndexFields.DOCUMENT_ID, new KeywordAnalyzer());
        perField.put(IndexFields.SOURCE_ID, new KeywordAnalyzer());
        perField.put(IndexFields.LICENSE_ID, new KeywordAnalyzer());
        perField.put(IndexFields.CONCEPT, new KeywordAnalyzer());
        perField.put(IndexFields.TRADITION, new KeywordAnalyzer());
        perField.put(IndexFields.KIND, new KeywordAnalyzer());
        return new PerFieldAnalyzerWrapper(prose(), perField);
    }
}
