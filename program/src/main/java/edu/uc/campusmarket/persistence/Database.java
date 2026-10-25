package edu.uc.campusmarket.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Opens independent SQLite connections; the live database never belongs in resources. */
public final class Database {
    // Remember the location, not an always-open connection. Each operation opens its own connection.
    private final Path file;

    public Database(Path file) {
        // Reject null, turn a relative path into an absolute path, and simplify any . or .. segments.
        this.file = Objects.requireNonNull(file, "Database file is required.")
                .toAbsolutePath().normalize();
    }

    public Path path() {
        // Other classes may show this location in the interface or database instructions.
        return file;
    }

    /** Both overrides name a directory, not a database file. */
    public static Path defaultPath() {
        // First priority: an optional JVM setting supplied when Java starts.
        String directory = System.getProperty("campusmarket.dataDir");
        if (directory == null || directory.isBlank()) {
            // Second priority: an environment variable set outside the Java program.
            directory = System.getenv("CAMPUS_MARKET_DATA_DIR");
        }
        // With neither override, save under the current Windows user's home directory.
        // The ?: operator chooses the default on the left or the supplied directory on the right.
        Path dataDirectory = directory == null || directory.isBlank()
                ? Path.of(System.getProperty("user.home"), ".ucb-campus-marketplace")
                : Path.of(directory);
        // Only schema instructions belong in resources. The changing .db file belongs here.
        return dataDirectory.resolve("marketplace.db").toAbsolutePath().normalize();
    }

    public Connection open() throws SQLException {
        try {
            // Create missing parent directories; existing directories are left in place.
            Files.createDirectories(file.getParent());
            // Stop early if this path is a directory or cannot be written to.
            if (Files.isDirectory(file) || !Files.isWritable(file.getParent())
                    || (Files.exists(file) && !Files.isWritable(file))) {
                throw new IOException("The database file or its directory is not writable.");
            }
        } catch (IOException | SecurityException error) {
            // Wrap a file-access failure as the database error expected by callers.
            // Keeping error as the cause preserves its original explanation for troubleshooting.
            throw new SQLException("Cannot prepare the database at " + file
                    + ". Choose a writable data directory.", error);
        }

        // JDBC selects the SQLite driver from this URL and opens (or creates) this database file.
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try {
            // PRAGMA is SQLite's configuration command. Foreign keys must be enabled per connection.
            // try-with-resources closes this statement automatically, even if execute() fails.
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA foreign_keys = ON")) {
                statement.execute();
            }
            // If another operation holds a database lock, wait up to 5000 ms before reporting busy.
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA busy_timeout = 5000")) {
                statement.execute();
            }
            // Read the setting back rather than assuming SQLite accepted it.
            try (PreparedStatement statement = connection.prepareStatement("PRAGMA foreign_keys");
                    ResultSet result = statement.executeQuery()) {
                // next() moves to the first result row; column 1 must contain 1, meaning enabled.
                if (!result.next() || result.getInt(1) != 1) {
                    throw new SQLException("SQLite foreign-key enforcement could not be enabled.");
                }
            }
            // The caller now owns the connection and closes it using its own try-with-resources.
            return connection;
        } catch (SQLException error) {
            // Setup failed, so do not leak the newly opened connection.
            try {
                connection.close();
            } catch (SQLException closeError) {
                // Keep the original failure primary while also recording a failure during cleanup.
                error.addSuppressed(closeError);
            }
            // Pass the same failure back to the repository and, eventually, the interface.
            throw error;
        }
    }
}
