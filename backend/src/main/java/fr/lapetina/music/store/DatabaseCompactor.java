package fr.lapetina.music.store;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Compacts the database when the application stops.
 *
 * <p>Ingesting a corpus inserts tens of thousands of rows in one go, and H2's storage
 * engine holds on to the space it used getting there. Measured on a full ingestion of all
 * fourteen sources: the file was 853 MB for 173,000 rows that occupy 23 MB once compacted.
 * The space is reused rather than leaked, so it stays bounded — but half a gigabyte for a
 * few megabytes of data is not a reasonable thing to leave on somebody's disk.
 *
 * <p>Shutdown is the right moment because the database is closing anyway: {@code SHUTDOWN
 * COMPACT} closes it, which is exactly what was about to happen. On a small database it
 * costs nothing measurable; after a large ingestion it takes a few seconds and reclaims
 * most of the file.
 */
@ApplicationScoped
public class DatabaseCompactor {

    private static final Logger LOG = Logger.getLogger(DatabaseCompactor.class);

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "music.store.compact-on-shutdown", defaultValue = "true")
    boolean compactOnShutdown;

    void onStop(@Observes ShutdownEvent event) {
        if (!compactOnShutdown) {
            return;
        }
        long started = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN COMPACT");
            LOG.infof("Database compacted in %d ms", System.currentTimeMillis() - started);
        } catch (SQLException closing) {
            // The connection dies with the database, which is the point of the statement.
            LOG.debugf("Database closed during compaction: %s", closing.getMessage());
        } catch (RuntimeException e) {
            // Never let tidying up turn a clean shutdown into a failed one.
            LOG.warnf("Could not compact the database: %s", e.toString());
        }
    }
}
