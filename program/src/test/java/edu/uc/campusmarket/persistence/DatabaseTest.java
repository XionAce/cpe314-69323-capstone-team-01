package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path folder;
    private Database database;

    @BeforeEach
    void setUp() throws SQLException {
        database = new Database(folder.resolve("test.db"));
        database.initialize();
    }

    @Test
    void createsTenTablesAndSeedOnlyOnce() throws SQLException {
        database.initialize();
        assertEquals(10, number("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"));
        assertEquals(3, database.getMembers().size());
        assertEquals(7, database.getCategories().size());
        assertEquals(3, database.getListings().size());
        assertEquals(1, number("PRAGMA foreign_keys"));
        assertEquals(3, number("PRAGMA user_version"));
    }

    @Test
    void verifiesEachAccountAndRejectsIncorrectPasswords() throws SQLException {
        for (int i = 0; i < 3; i++) {
            char letter = (char) ('A' + i);
            Member member = database.authenticate("person_" + Character.toLowerCase(letter),
                    ("Campus" + letter + "123!").toCharArray());
            assertEquals(i + 1, member.getId());
            assertEquals("Person " + letter, member.getName());
        }
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a", "wrong".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("unknown", "CampusA123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a", new char[0]));
    }

    @Test
    void resetsPasswordAndConsumesTheRecoveryCode() throws SQLException {
        database.addInterest(1, 2);
        String next = database.resetPassword("person_a", "RESET-A-2026".toCharArray(), "MyNewPass123!".toCharArray());
        assertEquals(1, database.authenticate("person_a", "MyNewPass123!".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a", "CampusA123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a", "RESET-A-2026".toCharArray(), "OtherPass123!".toCharArray()));
        assertEquals(24, next.length());
        assertEquals(3, database.getListings().size());
        assertEquals(List.of(2L), database.getWaitlist(1));
        database.initialize();
        database.resetPassword("person_a", next.toCharArray(), "NextPassword123!".toCharArray());
        assertEquals(1, database.authenticate("person_a", "NextPassword123!".toCharArray()).getId());
    }

    @Test
    void rejectsWrongRecoveryAndShortPasswordsWithoutChangingTheAccount() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_b", "RESET-A-2026".toCharArray(), "NewPassword123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("unknown", "RESET-A-2026".toCharArray(), "NewPassword123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a", "RESET-A-2026".toCharArray(), "short".toCharArray()));
        assertEquals(1, database.authenticate("person_a", "CampusA123!".toCharArray()).getId());
        assertEquals(2, database.authenticate("person_b", "CampusB123!".toCharArray()).getId());
    }

    @Test
    void upgradesVersionTwoWithRecoveryWithoutResettingItsPassword() throws SQLException {
        database.resetPassword("person_c", "RESET-C-2026".toCharArray(), "KeepThisPassword!".toCharArray());
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE Members DROP COLUMN recoverySalt");
            statement.execute("ALTER TABLE Members DROP COLUMN recoveryHash");
            statement.execute("PRAGMA user_version = 2");
        }
        database.initialize();
        assertEquals(3, database.authenticate("person_c", "KeepThisPassword!".toCharArray()).getId());
        assertEquals(3, number("PRAGMA user_version"));
        database.resetPassword("person_c", "RESET-C-2026".toCharArray(), "AnotherPassword!".toCharArray());
        assertEquals(3, database.authenticate("person_c", "AnotherPassword!".toCharArray()).getId());
    }

    @Test
    void preservesExistingRowsWhenUpgradingDemoMembersToLoginAccounts() throws SQLException {
        database.addInterest(1, 2);
        database.addReview(1, 2, 4, "Keep this review");
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("DROP INDEX idx_Members_username");
            statement.execute("ALTER TABLE Members DROP COLUMN username");
            statement.execute("ALTER TABLE Members DROP COLUMN passwordSalt");
            statement.execute("ALTER TABLE Members DROP COLUMN passwordHash");
            statement.execute("UPDATE Members SET name = 'Alex (demo)' WHERE id = 1");
            statement.execute("PRAGMA user_version = 1");
        }
        database.initialize();
        assertEquals(1, database.authenticate("person_a", "CampusA123!".toCharArray()).getId());
        assertEquals("Person A", database.getMembers().getFirst().getName());
        assertEquals(3, database.getListings().size());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertTrue(database.reviews(1).contains("Keep this review"));
        String hash;
        try (var connection = database.open(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
            hash = result.getString(1);
        }
        database.initialize();
        try (var connection = database.open(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
            assertEquals(hash, result.getString(1));
            assertNotEquals("CampusA123!", hash);
        }
    }

    @Test
    void savesAndReopensAllThreeSubtypesWithConditionEvidence() throws SQLException {
        long bookId = database.insertListing(book("Quoted ' Java book", 2));
        long electronicId = database.insertListing(new Electronics(0, 2, 7, "Broken laptop", new BigDecimal("1234.56"),
                "PHP", "AVAILABLE", false, true, true));
        long furnitureId = database.insertListing(new Furniture(0, 3, 4, "Worn chair", new BigDecimal("300.25"),
                "PHP", "AVAILABLE", true, false, WearLevel.HEAVY));
        Database reopened = new Database(database.getPath());
        reopened.initialize();
        Listing book = find(reopened.getListings(), bookId);
        assertInstanceOf(Textbook.class, book);
        assertEquals(2, ((Textbook) book).getMissingPages());
        assertEquals("Quoted ' Java book", book.getTitle());
        assertEquals(ConditionGrade.POOR, book.gradeCondition());
        Listing electronic = find(reopened.getListings(), electronicId);
        assertInstanceOf(Electronics.class, electronic);
        assertFalse(((Electronics) electronic).isPowersOn());
        assertEquals(123456, electronic.getPriceMinor());
        Listing furniture = find(reopened.getListings(), furnitureId);
        assertInstanceOf(Furniture.class, furniture);
        assertEquals(WearLevel.HEAVY, ((Furniture) furniture).getSurfaceWear());
        assertEquals(ConditionGrade.FAIR, furniture.gradeCondition());
    }

    @Test
    void titleContainingSqlIsSavedAsOrdinaryText() throws SQLException {
        String title = "Book'); DROP TABLE Members; --";
        long id = database.insertListing(book(title, 0));
        assertEquals(title, find(database.getListings(), id).getTitle());
        assertEquals(3, database.getMembers().size());
    }

    @Test
    void foreignKeysRejectUnknownCategoryWithoutLeavingARow() throws SQLException {
        Textbook invalid = new Textbook(0, 1, 999, "Unknown category", new BigDecimal("50.00"),
                "PHP", "AVAILABLE", 0, WearLevel.NONE, false);
        assertThrows(SQLException.class, () -> database.insertListing(invalid));
        assertEquals(3, database.getListings().size());
    }

    @Test
    void detailInsertFailureRollsBackItsParentListing() throws SQLException {
        // Force a failure AFTER the shared row was inserted to test the transaction.
        execute("CREATE TRIGGER reject_book BEFORE INSERT ON TextbookDetails BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        assertThrows(SQLException.class, () -> database.insertListing(book("Must roll back", 0)));
        assertEquals(3, number("SELECT COUNT(*) FROM Listings"));
        assertEquals(1, number("SELECT COUNT(*) FROM TextbookDetails"));
    }

    @Test
    void waitlistPreservesJoinOrderAndRejectsDuplicatesAndOwner() throws SQLException {
        database.addInterest(1, 3);
        database.addInterest(1, 2);
        assertEquals(List.of(3L, 2L), database.getWaitlist(1));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 3));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 1));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 999));
        assertEquals(List.of(3L, 2L), new Database(database.getPath()).getWaitlist(1));
    }

    @Test
    void onlyOwnerWithdrawsAndExistingWaitlistAndDetailsRemain() throws SQLException {
        database.addInterest(1, 2);
        assertThrows(IllegalArgumentException.class, () -> database.withdrawListing(1, 2));
        database.withdrawListing(1, 1);
        assertEquals("WITHDRAWN", find(database.getListings(), 1).getStatus());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertEquals(1, number("SELECT COUNT(*) FROM TextbookDetails WHERE listingId = 1"));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 3));
    }

    @Test
    void unrecognizedDatabaseIsRejectedWithoutErasingIt() throws SQLException {
        Database unrelated = new Database(folder.resolve("unrelated.db"));
        try (Connection connection = unrelated.open();
             PreparedStatement statement = connection.prepareStatement("CREATE TABLE ExistingWork(id INTEGER PRIMARY KEY)")) {
            statement.execute();
        }
        assertThrows(SQLException.class, unrelated::initialize);
        try (Connection connection = unrelated.open();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM ExistingWork");
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void missingSubtypeDetailsAreReportedInsteadOfInventingACondition() throws SQLException {
        execute("DELETE FROM TextbookDetails WHERE listingId = 1");
        SQLException error = assertThrows(SQLException.class, database::getListings);
        assertTrue(error.getMessage().contains("Restore missing details"));
    }

    @Test
    void ownerRestoresAllThreeTypesWithoutChangingSharedData() throws SQLException {
        List<Listing> original = database.getListings();
        database.addInterest(1, 2);
        database.addReview(1, 2, 4, "Keep this review");
        execute("DELETE FROM TextbookDetails");
        execute("DELETE FROM ElectronicsDetails");
        execute("DELETE FROM FurnitureDetails");
        for (int i = 0; i < original.size(); i++) {
            long owner = i + 1;
            assertEquals(original.get(i).getId(), database.listingsForRepair(owner).getFirst().id());
            database.restoreDetails(owner, original.get(i));
            assertTrue(database.listingsForRepair(owner).isEmpty());
        }
        List<Listing> restored = new Database(database.getPath()).getListings();
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).getTitle(), restored.get(i).getTitle());
            assertEquals(original.get(i).getPriceMinor(), restored.get(i).getPriceMinor());
            assertEquals(original.get(i).gradeCondition(), restored.get(i).gradeCondition());
        }
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertTrue(database.reviews(1).contains("Keep this review"));
    }

    @Test
    void restorationRejectsAnotherOwnerWrongTypeAndExistingDetails() throws SQLException {
        Listing original = database.getListings().getFirst();
        execute("DELETE FROM TextbookDetails WHERE listingId = 1");
        assertTrue(database.listingsForRepair(2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> database.restoreDetails(2, original));
        Listing wrongType = new Electronics(1, 1, 2, "Wrong type", new BigDecimal("10.00"),
                "PHP", "AVAILABLE", true, false, false);
        assertThrows(IllegalArgumentException.class, () -> database.restoreDetails(1, wrongType));
        assertEquals(0, number("SELECT COUNT(*) FROM TextbookDetails"));
        database.restoreDetails(1, original);
        assertThrows(IllegalArgumentException.class, () -> database.restoreDetails(1, original));
        assertEquals(3, database.getListings().size());
    }

    @Test
    void failedRestorationLeavesTheListingAndOtherDataIntact() throws SQLException {
        Listing original = database.getListings().getFirst();
        database.addInterest(1, 2);
        execute("DELETE FROM TextbookDetails WHERE listingId = 1");
        execute("CREATE TRIGGER reject_restore AFTER INSERT ON TextbookDetails BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        assertThrows(SQLException.class, () -> database.restoreDetails(1, original));
        assertEquals(0, number("SELECT COUNT(*) FROM TextbookDetails"));
        assertEquals(3, number("SELECT COUNT(*) FROM Listings"));
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertEquals(1, database.listingsForRepair(1).size());
    }

    @Test
    void createsSellerProfileOnlyForAnExistingMember() throws SQLException {
        execute("INSERT INTO Members(id, name) VALUES (4, 'New demo member')");
        long sellerId = database.ensureSeller(4);
        assertEquals(sellerId, database.ensureSeller(4));
        assertEquals(4, number("SELECT COUNT(*) FROM Sellers"));
        assertThrows(IllegalArgumentException.class, () -> database.ensureSeller(999));
    }

    @Test
    void loginNormalizesUsernameButKeepsPasswordCaseSensitive() throws SQLException {
        assertEquals(1, database.authenticate("  PERSON_A  ", "CampusA123!".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", "campusa123!".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", " CampusA123! ".toCharArray()));
    }

    @Test
    void emptyCredentialsCannotAuthenticateAnAccount() {
        assertThrows(IllegalArgumentException.class, () -> database.authenticate(null, "x".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("  ", "x".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a", null));
    }

    @Test
    void eachDatabaseUsesFreshSaltAndStoresOnlyCredentialHashes() throws SQLException {
        Database other = new Database(folder.resolve("other.db"));
        other.initialize();
        try (var first = database.open(); var second = other.open();
             var firstQuery = first.createStatement(); var secondQuery = second.createStatement();
             var a = firstQuery.executeQuery("SELECT passwordSalt, passwordHash, recoveryHash FROM Members WHERE id = 1");
             var b = secondQuery.executeQuery("SELECT passwordSalt, passwordHash FROM Members WHERE id = 1")) {
            assertTrue(a.next());
            assertTrue(b.next());
            assertNotEquals(a.getString("passwordSalt"), b.getString("passwordSalt"));
            assertNotEquals(a.getString("passwordHash"), b.getString("passwordHash"));
            assertNotEquals("CampusA123!", a.getString("passwordHash"));
            assertNotEquals("RESET-A-2026", a.getString("recoveryHash"));
        }
        assertEquals(1, other.authenticate("person_a", "CampusA123!".toCharArray()).getId());
    }

    @Test
    void passwordResetAcceptsBothLengthLimitsAndRejectsAnOversizedPassword() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword(
                "person_a", "RESET-A-2026".toCharArray(), "x".repeat(129).toCharArray()));
        String code = database.resetPassword("person_a", "RESET-A-2026".toCharArray(), "abcdefgh".toCharArray());
        assertEquals(1, database.authenticate("person_a", "abcdefgh".toCharArray()).getId());
        database.resetPassword("person_a", code.toCharArray(), "x".repeat(128).toCharArray());
        assertEquals(1, database.authenticate("person_a", "x".repeat(128).toCharArray()).getId());
        assertEquals(2, database.authenticate("person_b", "CampusB123!".toCharArray()).getId());
    }

    @Test
    void failedPasswordResetKeepsOldPasswordAndRecoveryCodeUsable() throws SQLException {
        execute("CREATE TRIGGER reject_reset AFTER UPDATE OF passwordHash ON Members BEGIN SELECT RAISE(ABORT, 'test rollback'); END");
        assertThrows(SQLException.class, () -> database.resetPassword(
                "person_a", "RESET-A-2026".toCharArray(), "NewPassword123!".toCharArray()));
        assertEquals(1, database.authenticate("person_a", "CampusA123!".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", "NewPassword123!".toCharArray()));
        execute("DROP TRIGGER reject_reset");
        database.resetPassword("person_a", "RESET-A-2026".toCharArray(), "RetryPassword123!".toCharArray());
        assertEquals(1, database.authenticate("person_a", "RetryPassword123!".toCharArray()).getId());
    }

    @Test
    void emptyRecoveryInputsKeepTheOriginalAccountUsable() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword(null, "x".toCharArray(), "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a", null, "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a", new char[0], "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a", "RESET-A-2026".toCharArray(), null));
        assertEquals(1, database.authenticate("person_a", "CampusA123!".toCharArray()).getId());
    }

    private Textbook book(String title, int missingPages) {
        return new Textbook(0, 1, 2, title, new BigDecimal("125.50"), "PHP", "AVAILABLE",
                missingPages, WearLevel.LIGHT, true);
    }

    private Listing find(List<Listing> listings, long id) {
        for (Listing listing : listings) {
            if (listing.getId() == id) return listing;
        }
        throw new AssertionError("Listing not found: " + id);
    }

    private int number(String sql) throws SQLException {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
