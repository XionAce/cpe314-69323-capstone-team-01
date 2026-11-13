package edu.uc.campusmarket.dao;

import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Registration;
import edu.uc.campusmarket.persistence.Database;
import edu.uc.campusmarket.service.CredentialSecurity;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/** SQL operations for this table or closely related aggregate. */
public class MemberDao {
    private static final int MAX_DISPLAY_NAME_LENGTH = 80;
    private static final int SQLITE_CONSTRAINT = 19;
    private final Database database;

    /** Creates this component from the supplied values or dependencies. */
    public MemberDao(Database database) {
        this.database = database;
    }

    /** Verifies a salted password hash and returns the matching member. */
    public Member authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            throw new IllegalArgumentException("Enter your username and password.");
        }
        try (var connection = database.open();
                var statement = connection.prepareStatement(
                        "SELECT id, name, passwordSalt, passwordHash FROM Members WHERE username = ?")) {
            statement.setString(1, username.strip().toLowerCase(Locale.ROOT));
            try (var result = statement.executeQuery()) {
                if (result.next()) {
                    byte[] salt = Base64.getDecoder().decode(result.getString("passwordSalt"));
                    byte[] expected = Base64.getDecoder().decode(result.getString("passwordHash"));
                    if (MessageDigest.isEqual(expected, CredentialSecurity.hash(password, salt))) {
                        return new Member(result.getLong("id"), result.getString("name"));
                    }
                }
            }
        }
        throw new IllegalArgumentException("The username or password is incorrect.");
    }

    /**
     * Consumes a recovery code and returns a replacement after updating the
     * password.
     */
    public String resetPassword(String username, char[] recoveryCode, char[] newPassword)
            throws SQLException {
        if (username == null || username.isBlank() || recoveryCode == null
                || recoveryCode.length == 0) {
            throw new IllegalArgumentException("Enter your username and recovery code.");
        }
        if (newPassword == null || newPassword.length < CredentialSecurity.MIN_PASSWORD_LENGTH
                || newPassword.length > CredentialSecurity.MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Use a password between 8 and 128 characters.");
        }
        try (var connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                RecoveryCheck checked = verifyRecovery(connection, username, recoveryCode);
                String nextCode = replaceSecrets(connection, checked, newPassword);
                connection.commit();
                return nextCode;
            } catch (SQLException | RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        }
    }

    /** Loads member identities without exposing stored credential hashes. */
    public List<Member> getMembers() throws SQLException {
        List<Member> members = new ArrayList<>();
        try (Connection connection = database.open();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT id, name FROM Members ORDER BY id");
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                members.add(new Member(result.getLong("id"), result.getString("name")));
            }
        }
        return members;
    }

    /**
     * Insert a new account; the unique username index resolves concurrent
     * registrations.
     */
    public Registration register(String displayName, String username, char[] password)
            throws SQLException {
        String normalized = validateRegistration(displayName, username, password);
        byte[] salt = CredentialSecurity.randomBytes(CredentialSecurity.SALT_BYTES);
        byte[] recoverySalt = CredentialSecurity.randomBytes(CredentialSecurity.SALT_BYTES);
        String code = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(CredentialSecurity.randomBytes(CredentialSecurity.RECOVERY_BYTES));
        char[] codeChars = code.toCharArray();
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Members(name, username, passwordSalt, passwordHash, recoverySalt, recoveryHash)"
                            + " VALUES (?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, displayName.strip());
                statement.setString(2, normalized);
                statement.setString(3, Base64.getEncoder().encodeToString(salt));
                statement.setString(4, Base64.getEncoder()
                        .encodeToString(CredentialSecurity.hash(password, salt)));
                statement.setString(5, Base64.getEncoder().encodeToString(recoverySalt));
                statement.setString(6, Base64.getEncoder()
                        .encodeToString(CredentialSecurity.hash(codeChars, recoverySalt)));
                statement.executeUpdate();
                long id = Database.lastId(connection);
                connection.commit();
                return new Registration(new Member(id, displayName.strip()), normalized, code);
            } catch (SQLException error) {
                Database.rollback(connection, error);
                if (error.getErrorCode() == SQLITE_CONSTRAINT) {
                    throw new IllegalArgumentException("This username is already taken.", error);
                }
                throw error;
            } catch (RuntimeException error) {
                Database.rollback(connection, error);
                throw error;
            }
        } finally {
            Arrays.fill(codeChars, '\0');
        }
    }

    private String validateRegistration(String displayName, String username, char[] password) {
        if (displayName == null || displayName.isBlank()
                || displayName.strip().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Enter a display name of 1 to 80 characters.");
        }
        String normalized = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "Use 3-30 lowercase letters, digits or underscores for the username.");
        }
        if (password == null || password.length < CredentialSecurity.MIN_PASSWORD_LENGTH
                || password.length > CredentialSecurity.MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Use a password between 8 and 128 characters.");
        }
        return normalized;
    }

    private record RecoveryCheck(long memberId, String previousHash) {
    }

    private RecoveryCheck verifyRecovery(Connection connection, String username,
            char[] recoveryCode) throws SQLException {
        long memberId;
        String previousHash;
        try (var statement = connection.prepareStatement(
                "SELECT id, recoverySalt, recoveryHash FROM Members WHERE username = ?")) {
            statement.setString(1, username.strip().toLowerCase(Locale.ROOT));
            try (var result = statement.executeQuery()) {
                if (!result.next() || result.getString("recoveryHash") == null) {
                    throw new IllegalArgumentException(
                            "The username or recovery code is incorrect.");
                }
                byte[] salt = Base64.getDecoder().decode(result.getString("recoverySalt"));
                previousHash = result.getString("recoveryHash");
                if (!MessageDigest.isEqual(Base64.getDecoder().decode(previousHash),
                        CredentialSecurity.hash(recoveryCode, salt))) {
                    throw new IllegalArgumentException(
                            "The username or recovery code is incorrect.");
                }
                memberId = result.getLong("id");
            }
        }
        return new RecoveryCheck(memberId, previousHash);
    }

    private String replaceSecrets(Connection connection, RecoveryCheck checked, char[] newPassword)
            throws SQLException {
        SecureRandom random = new SecureRandom();
        byte[] passwordSalt = new byte[CredentialSecurity.SALT_BYTES];
        byte[] recoverySalt = new byte[CredentialSecurity.SALT_BYTES];
        byte[] codeBytes = new byte[CredentialSecurity.RECOVERY_BYTES];
        random.nextBytes(passwordSalt);
        random.nextBytes(recoverySalt);
        random.nextBytes(codeBytes);
        String nextCode = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);
        char[] nextCodeChars = nextCode.toCharArray();
        try (var statement = connection.prepareStatement("""
                UPDATE Members SET passwordSalt = ?, passwordHash = ?,
                    recoverySalt = ?, recoveryHash = ?
                WHERE id = ? AND recoveryHash = ?
                """)) {
            statement.setString(1, Base64.getEncoder().encodeToString(passwordSalt));
            statement.setString(2, Base64.getEncoder()
                    .encodeToString(CredentialSecurity.hash(newPassword, passwordSalt)));
            statement.setString(3, Base64.getEncoder().encodeToString(recoverySalt));
            statement.setString(4, Base64.getEncoder()
                    .encodeToString(CredentialSecurity.hash(nextCodeChars, recoverySalt)));
            statement.setLong(5, checked.memberId());
            statement.setString(6, checked.previousHash());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The recovery code has already been used.");
            }
        } finally {
            Arrays.fill(nextCodeChars, '\0');
        }
        return nextCode;
    }
}
