package fr.lapetina.music.knowledge.harmony;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One harmonic annotation of a real piece of music, at one point in one score.
 *
 * <p>These rows are what stop the tutor inventing a Beethoven example. A question like
 * "show me a Mozart ii6-V-I" is answered by querying this table, and when it returns
 * nothing the honest answer is that there is no verified example, not a plausible-sounding
 * measure number.
 *
 * <p>Deliberately not embedded into the vector index: this is structured data, and
 * "find V/V in Beethoven" is a query, not a similarity search.
 */
@Entity
@Table(name = "knowledge_harmony_event")
public class HarmonyEvent extends PanacheEntityBase {

    @Id
    public UUID id = UUID.randomUUID();

    @Column(name = "source_id", nullable = false)
    public String sourceId;

    @Column(nullable = false)
    public int generation;

    @Column(nullable = false)
    public String composer;

    @Column(nullable = false, length = 1000)
    public String work;

    @Column(length = 1000)
    public String movement;

    /** The printed measure number, so a citation points at something a reader can find. */
    @Column
    public Integer measure;

    @Column
    public Double beat;

    @Column(name = "global_key")
    public String globalKey;

    /** As published: a Roman numeral relative to the global key, not an absolute key. */
    @Column(name = "local_key")
    public String localKey;

    @Column(name = "roman_numeral")
    public String romanNumeral;

    @Column(name = "chord_label")
    public String chordLabel;

    @Column(name = "chord_type")
    public String chordType;

    @Column
    public String figbass;

    @Column(name = "relative_root")
    public String relativeRoot;

    @Column
    public String cadence;

    @Column(name = "phrase_end", nullable = false)
    public boolean phraseEnd = false;

    /** The file and row this came from, so a claim can be traced back to the corpus. */
    @Column(name = "source_reference", nullable = false, length = 1000)
    public String sourceReference;

    @Column(name = "license_id", nullable = false)
    public String licenseId;

    @Column(nullable = false)
    public boolean active = false;

    public static long countActive() {
        return count("active = true");
    }
}
