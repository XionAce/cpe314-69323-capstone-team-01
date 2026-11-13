package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.service.CredentialSecurity;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Migrates older classroom accounts without replacing existing credentials or
 * IDs.
 */
final class AccountSetup {
    private AccountSetup() {
    }

    static List<InitialAccount> upgrade(Connection connection) throws SQLException {
        List<InitialAccount> issued = new ArrayList<>();
        connection.setAutoCommit(false);
        try {
            ensureColumns(connection);
            upgradeListings(connection);
            Database.ensureMessages(connection);
            String[] oldNames = { "Alex (demo)", "Bea (demo)", "Carlo (demo)" };
            for (int i = 0; i < oldNames.length; i++) {
                char letter = (char) ('A' + i);
                String username = "person_" + Character.toLowerCase(letter);
                String name = "Person " + letter;
                String password = issuePassword(connection, i + 1, username, oldNames[i], name);
                String recovery = issueRecovery(connection, i + 1);
                renameSeller(connection, i + 1, oldNames[i], name);
                if (!password.isEmpty() || !recovery.isEmpty()) {
                    issued.add(new InitialAccount(readUsername(connection, i + 1), password,
                            recovery));
                }
            }
            try (var statement = connection.prepareStatement("PRAGMA user_version = 5")) {
                statement.execute();
            }
            connection.commit();
            return List.copyOf(issued);
        } catch (SQLException | RuntimeException error) {
            Database.rollback(connection, error);
            throw error;
        }
    }

    private static void upgradeListings(Connection connection) throws SQLException {
        // Normalize old values before ALTER TABLE validates existing constraints.
        try (var query = connection.prepareStatement("""
                UPDATE Listings SET status = CASE status
                    WHEN 'AVAILABLE' THEN 'ACTIVE' WHEN 'WITHDRAWN' THEN 'REMOVED' ELSE status END
                WHERE status IN ('AVAILABLE', 'WITHDRAWN')
                """)) {
            query.executeUpdate();
        }
        boolean hasDate = false;
        try (var query = connection.prepareStatement("PRAGMA table_info(Listings)");
                var columns = query.executeQuery()) {
            while (columns.next()) {
                hasDate |= "created_at".equals(columns.getString("name"));
            }
        }
        if (!hasDate) {
            // Old records have no reliable creation date. Keep NULL rather than inventing
            // one.
            try (var query = connection.prepareStatement("""
                    ALTER TABLE Listings ADD COLUMN created_at TEXT
                    CHECK (created_at IS NULL OR length(trim(created_at)) > 0)
                    """)) {
                query.execute();
            }
        }
    }

    private static void ensureColumns(Connection connection) throws SQLException {
        boolean login = false;
        boolean recovery = false;
        try (var statement = connection.prepareStatement("PRAGMA table_info(Members)");
                var columns = statement.executeQuery()) {
            while (columns.next()) {
                login |= columns.getString("name").equals("username");
                recovery |= columns.getString("name").equals("recoveryHash");
            }
        }
        if (!login) {
            addColumns(connection, List.of("username", "passwordSalt", "passwordHash"));
        }
        if (!recovery) {
            addColumns(connection, List.of("recoverySalt", "recoveryHash"));
        }
        try (var statement = connection.prepareStatement(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_Members_username ON Members(username)")) {
            statement.execute();
        }
    }

    private static void addColumns(Connection connection, List<String> trustedNames)
            throws SQLException {
        // Identifiers are fixed internal names, never user input.
        for (String column : trustedNames) {
            try (var statement = connection
                    .prepareStatement("ALTER TABLE Members ADD COLUMN " + column + " TEXT")) {
                statement.execute();
            }
        }
    }

    private static String issuePassword(Connection connection, long id, String username,
            String oldName, String name) throws SQLException {
        byte[] salt = CredentialSecurity.randomBytes(CredentialSecurity.SALT_BYTES);
        String candidate = CredentialSecurity.randomSecret();
        char[] secret = candidate.toCharArray();
        try (var statement = connection.prepareStatement("""
                UPDATE Members SET username = ?, passwordSalt = ?, passwordHash = ?,
                name = CASE WHEN name = ? THEN ? ELSE name END
                WHERE id = ? AND username IS NULL
                """)) {
            statement.setString(1, username);
            statement.setString(2, Base64.getEncoder().encodeToString(salt));
            statement.setString(3,
                    Base64.getEncoder().encodeToString(CredentialSecurity.hash(secret, salt)));
            statement.setString(4, oldName);
            statement.setString(5, name);
            statement.setLong(6, id);
            return statement.executeUpdate() == 1 ? candidate : "";
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private static String issueRecovery(Connection connection, long id) throws SQLException {
        byte[] salt = CredentialSecurity.randomBytes(CredentialSecurity.SALT_BYTES);
        String candidate = CredentialSecurity.randomSecret();
        char[] secret = candidate.toCharArray();
        try (var statement = connection.prepareStatement(
                "UPDATE Members SET recoverySalt = ?, recoveryHash = ? WHERE id = ? AND recoveryHash IS NULL")) {
            statement.setString(1, Base64.getEncoder().encodeToString(salt));
            statement.setString(2,
                    Base64.getEncoder().encodeToString(CredentialSecurity.hash(secret, salt)));
            statement.setLong(3, id);
            return statement.executeUpdate() == 1 ? candidate : "";
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    private static void renameSeller(Connection connection, long id, String oldName, String name)
            throws SQLException {
        try (var statement = connection.prepareStatement(
                "UPDATE Sellers SET displayName = ? WHERE memberId = ? AND displayName = ?")) {
            statement.setString(1, name);
            statement.setLong(2, id);
            statement.setString(3, oldName);
            statement.executeUpdate();
        }
    }

    private static String readUsername(Connection connection, long id) throws SQLException {
        try (var statement = connection
                .prepareStatement("SELECT username FROM Members WHERE id = ?")) {
            statement.setLong(1, id);
            try (var row = statement.executeQuery()) {
                if (!row.next()) {
                    throw new SQLException("Member not found during account setup.");
                }
                return row.getString(1);
            }
        }
    }
}
