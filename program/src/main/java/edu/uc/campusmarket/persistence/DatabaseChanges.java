package edu.uc.campusmarket.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Watches commits through a dedicated connection, including other app windows.
 */
public final class DatabaseChanges implements AutoCloseable {
    private final Database database;
    private Connection observer;

    /** Creates a lazy observer without changing the database or its schema. */
    public DatabaseChanges(Database database) {
        this.database = database;
    }

    /** Reads data_version on one connection without holding a read transaction. */
    public synchronized long version() throws SQLException {
        if (observer == null) {
            observer = database.open();
        }
        try (var query = observer.prepareStatement("PRAGMA data_version");
                var row = query.executeQuery()) {
            row.next();
            return row.getLong(1);
        }
    }

    /** Releases the observer connection when polling stops permanently. */
    @Override
    public synchronized void close() throws SQLException {
        if (observer != null) {
            observer.close();
            observer = null;
        }
    }
}
