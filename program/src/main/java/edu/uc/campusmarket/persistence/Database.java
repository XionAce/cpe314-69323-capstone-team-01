package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.model.Electronics;
import edu.uc.campusmarket.model.Furniture;
import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens SQLite, initializes its schema and preserves data during account
 * upgrades.
 */
public class Database {
    private static final int SCHEMA_VERSION = 4;
    private static final int TABLE_COUNT = 10;
    private static final int BUSY_TIMEOUT_MS = 5000;
    private final Path file;
    private final List<InitialAccount> initialAccounts = new ArrayList<>();

    /** Creates this component from the supplied values or dependencies. */
    public Database(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    /** Returns the path. */
    public Path getPath() {
        return file;
    }

    /** Provides the default path operation for this component. */
    public static Path defaultPath() {
        String directory = System.getProperty("campusmarket.dataDir");
        if (directory == null || directory.isBlank()) {
            return Path.of(System.getProperty("user.home"), ".ucb-campus-marketplace-simple",
                    "marketplace.db");
        }
        return Path.of(directory, "marketplace.db");
    }

    /** Opens a connection with SQLite foreign keys and the busy timeout enabled. */
    public Connection open() throws SQLException {
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException error) {
            throw new SQLException("Cannot create the database folder: " + file.getParent(), error);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try {
            // SQLite needs this setting on EVERY new connection.
            try (PreparedStatement statement = connection
                    .prepareStatement("PRAGMA foreign_keys = ON")) {
                statement.execute();
            }
            try (PreparedStatement statement = connection
                    .prepareStatement("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS)) {
                statement.execute();
            }
            return connection;
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    /**
     * Initializes the schema or account state while preserving existing records.
     */
    public void initialize() throws SQLException {
        try (Connection connection = open()) {
            int tables = countTables(connection);
            if (tables > 0) {
                // Never recreate or erase a database that already contains tables.
                try (PreparedStatement statement = connection
                        .prepareStatement("PRAGMA user_version");
                        ResultSet result = statement.executeQuery()) {
                    result.next();
                    if (result.getInt(1) < 1 || result.getInt(1) > SCHEMA_VERSION || tables != TABLE_COUNT) {
                        throw new SQLException("Unrecognized database. Choose a new data folder.");
                    }
                    if (result.getInt(1) == SCHEMA_VERSION) {
                        return;
                    }
                }
                initialAccounts.addAll(AccountSetup.upgrade(connection));
                return;
            }
            connection.setAutoCommit(false);
            try {
                String schema = readSchema();
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.execute();
                        }
                    }
                }
                seed(connection);
                try (PreparedStatement statement = connection
                        .prepareStatement("PRAGMA user_version = 1")) {
                    statement.execute();
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                rollback(connection, error);
                throw error;
            }
            initialAccounts.addAll(AccountSetup.upgrade(connection));
        }
    }

    private String readSchema() throws SQLException {
        try (InputStream input = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) {
                throw new IOException("Missing /db/schema.sql resource.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new SQLException("Cannot read the table definitions.", error);
        }
    }

    private void seed(Connection connection) throws SQLException {
        String[] names = { "Person A", "Person B", "Person C" };
        for (int i = 0; i < names.length; i++) {
            try (PreparedStatement statement = connection
                    .prepareStatement("INSERT INTO Members(id, name) VALUES (?, ?)")) {
                statement.setLong(1, i + 1);
                statement.setString(2, names[i]);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Sellers(id, memberId, displayName, sellerKind) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, i + 1);
                statement.setLong(2, i + 1);
                statement.setString(3, names[i]);
                statement.setString(4, "STUDENT");
                statement.executeUpdate();
            }
        }
        String[] categories = { "All finds", "Textbooks", "Electronics", "Furniture", "Computers",
                "Laptops", "Gaming Laptops" };
        Long[] parents = { null, 1L, 1L, 1L, 3L, 5L, 6L };
        for (int i = 0; i < categories.length; i++) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Categories(id, parentId, name) VALUES (?, ?, ?)")) {
                statement.setLong(1, i + 1);
                if (parents[i] == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setLong(2, parents[i]);
                }
                statement.setString(3, categories[i]);
                statement.executeUpdate();
            }
        }
        ListingDao.insertListing(connection, new Textbook(0, 1, 2, "Java textbook (demo)",
                new BigDecimal("350.00"), "PHP", "ACTIVE", 0, WearLevel.LIGHT, false));
        ListingDao.insertListing(connection, new Electronics(0, 2, 7, "Gaming laptop (demo)",
                new BigDecimal("18000.00"), "PHP", "ACTIVE", true, false, true));
        ListingDao.insertListing(connection, new Furniture(0, 3, 4, "Study chair (demo)",
                new BigDecimal("650.00"), "PHP", "ACTIVE", true, false, WearLevel.LIGHT));
    }

    /** Reads the identifier inserted on the same connection. */
    public static long lastId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT last_insert_rowid()");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    /**
     * Rolls back an unfinished write and retains any rollback failure as a
     * suppressed cause.
     */
    public static void rollback(Connection connection, Exception error) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            error.addSuppressed(rollbackError);
        }
    }

    /** Return newly issued credentials once; they are never saved as plaintext. */
    public List<InitialAccount> takeInitialAccounts() {
        List<InitialAccount> result = List.copyOf(initialAccounts);
        initialAccounts.clear();
        return result;
    }

    private int countTables(Connection connection) throws SQLException {
        int tables;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name NOT LIKE ?")) {
                statement.setString(1, "table");
                statement.setString(2, "sqlite_%");
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    tables = result.getInt(1);
                }
            }
        return tables;
    }
}
