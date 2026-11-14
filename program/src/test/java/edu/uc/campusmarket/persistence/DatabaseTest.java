package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.InterestDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.model.ConditionGrade;
import edu.uc.campusmarket.model.Electronics;
import edu.uc.campusmarket.model.Furniture;
import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {
    @TempDir
    Path folder;
    private TestDatabase database;

    private final java.util.Map<String, InitialAccount> issued = new java.util.HashMap<>();

    private InitialAccount account(String username) {
        for (InitialAccount account : database.takeInitialAccounts()) {
            issued.put(account.username(), account);
        }
        return java.util.Objects.requireNonNull(issued.get(username), username);
    }

    @BeforeEach
    void setUp() throws SQLException {
        database = new TestDatabase(folder.resolve("test.db"));
        database.initialize();
    }

    @Test
    void createsElevenTablesAndSeedOnlyOnce() throws SQLException {
        database.initialize();
        assertEquals(11, number(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"));
        assertEquals(3, database.getMembers().size());
        assertEquals(7, database.getCategories().size());
        assertEquals(3, database.getListings().size());
        assertEquals(1, number("PRAGMA foreign_keys"));
        assertEquals(5, number("PRAGMA user_version"));
    }

    @Test
    void verifiesEachAccountAndRejectsIncorrectPasswords() throws SQLException {
        for (int i = 0; i < 3; i++) {
            char letter = (char) ('A' + i);
            Member member = database.authenticate("person_" + Character.toLowerCase(letter),
                    account("person_" + Character.toLowerCase(letter)).password().toCharArray());
            assertEquals(i + 1, member.getId());
            assertEquals("Person " + letter, member.getName());
        }
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", "wrong".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("unknown",
                account("person_a").password().toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", new char[0]));
    }

    @Test
    void resetsPasswordAndConsumesTheRecoveryCode() throws SQLException {
        database.addInterest(1, 2);
        String next = database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "MyNewPass123!".toCharArray());
        assertEquals(1, database.authenticate("person_a", "MyNewPass123!".toCharArray()).getId());
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a",
                account("person_a").password().toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "OtherPass123!".toCharArray()));
        assertEquals(24, next.length());
        assertEquals(3, database.getListings().size());
        assertEquals(List.of(2L), database.getWaitlist(1));
        database.initialize();
        database.resetPassword("person_a", next.toCharArray(), "NextPassword123!".toCharArray());
        assertEquals(1,
                database.authenticate("person_a", "NextPassword123!".toCharArray()).getId());
    }

    @Test
    void rejectsWrongRecoveryAndShortPasswordsWithoutChangingTheAccount() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_b",
                account("person_a").recoveryCode().toCharArray(), "NewPassword123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("unknown",
                account("person_a").recoveryCode().toCharArray(), "NewPassword123!".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "short".toCharArray()));
        assertEquals(1, database
                .authenticate("person_a", account("person_a").password().toCharArray()).getId());
        assertEquals(2, database
                .authenticate("person_b", account("person_b").password().toCharArray()).getId());
    }

    @Test
    void upgradesVersionTwoWithRecoveryWithoutResettingItsPassword() throws SQLException {
        database.resetPassword("person_c", account("person_c").recoveryCode().toCharArray(),
                "KeepThisPassword!".toCharArray());
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE Members DROP COLUMN recoverySalt");
            statement.execute("ALTER TABLE Members DROP COLUMN recoveryHash");
            statement.execute("DROP TABLE Messages");
            statement.execute("PRAGMA user_version = 2");
        }
        database.initialize();
        assertEquals(3,
                database.authenticate("person_c", "KeepThisPassword!".toCharArray()).getId());
        assertEquals(5, number("PRAGMA user_version"));
        database.resetPassword("person_c", account("person_c").recoveryCode().toCharArray(),
                "AnotherPassword!".toCharArray());
        assertEquals(3,
                database.authenticate("person_c", "AnotherPassword!".toCharArray()).getId());
    }

    @Test
    void preservesExistingRowsWhenUpgradingDemoMembersToLoginAccounts() throws SQLException {
        database.addInterest(1, 2);
        if (!database.getWaitlist(1).contains((long) 2)) {
            database.addInterest(1, 2);
        }
        database.addReview(1, 2, 4, "Keep this review");
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("DROP INDEX idx_Members_username");
            statement.execute("ALTER TABLE Members DROP COLUMN username");
            statement.execute("ALTER TABLE Members DROP COLUMN passwordSalt");
            statement.execute("ALTER TABLE Members DROP COLUMN passwordHash");
            statement.execute("UPDATE Members SET name = 'Alex (demo)' WHERE id = 1");
            statement.execute("DROP TABLE Messages");
            statement.execute("PRAGMA user_version = 1");
        }
        database.initialize();
        assertEquals(1, database
                .authenticate("person_a", account("person_a").password().toCharArray()).getId());
        assertEquals("Person A", database.getMembers().getFirst().getName());
        assertEquals(3, database.getListings().size());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertTrue(database.reviews(1).contains("Keep this review"));
        String hash;
        try (var connection = database.open();
                var statement = connection.createStatement();
                var result = statement
                        .executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
            hash = result.getString(1);
        }
        database.initialize();
        try (var connection = database.open();
                var statement = connection.createStatement();
                var result = statement
                        .executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
            assertEquals(hash, result.getString(1));
            assertNotEquals(account("person_a").password(), hash);
        }
    }

    @Test
    void savesAndReopensAllThreeSubtypesWithConditionEvidence() throws SQLException {
        long bookId = database.insertListing(book("Quoted ' Java book", 2));
        long electronicId = database.insertListing(new Electronics(0, 2, 7, "Broken laptop",
                new BigDecimal("1234.56"), "PHP", "ACTIVE", false, true, true));
        long furnitureId = database.insertListing(new Furniture(0, 3, 4, "Worn chair",
                new BigDecimal("300.25"), "PHP", "ACTIVE", true, false, WearLevel.HEAVY));
        TestDatabase reopened = new TestDatabase(database.getPath());
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
                "PHP", "ACTIVE", 0, WearLevel.NONE, false);
        assertThrows(SQLException.class, () -> database.insertListing(invalid));
        assertEquals(3, database.getListings().size());
    }

    @Test
    void detailInsertFailureRollsBackItsParentListing() throws SQLException {
        // Force a failure AFTER the shared row was inserted to test the transaction.
        execute("""
                CREATE TRIGGER reject_book BEFORE INSERT ON TextbookDetails
                BEGIN SELECT RAISE(ABORT, 'test failure'); END
                """);
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
        assertEquals(List.of(3L, 2L), new TestDatabase(database.getPath()).getWaitlist(1));
    }

    @Test
    void onlyOwnerWithdrawsAndExistingWaitlistAndDetailsRemain() throws SQLException {
        database.addInterest(1, 2);
        assertThrows(IllegalArgumentException.class, () -> database.withdrawListing(1, 2));
        database.withdrawListing(1, 1);
        assertEquals("REMOVED", find(database.getListings(), 1).getStatus());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertEquals(1, number("SELECT COUNT(*) FROM TextbookDetails WHERE listingId = 1"));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 3));
    }

    @Test
    void unrecognizedDatabaseIsRejectedWithoutErasingIt() throws SQLException {
        TestDatabase unrelated = new TestDatabase(folder.resolve("unrelated.db"));
        try (Connection connection = unrelated.open();
                PreparedStatement statement = connection
                        .prepareStatement("CREATE TABLE ExistingWork(id INTEGER PRIMARY KEY)")) {
            statement.execute();
        }
        assertThrows(SQLException.class, unrelated::initialize);
        try (Connection connection = unrelated.open();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT COUNT(*) FROM ExistingWork");
                ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void missingSubtypeDetailsAreReportedInsteadOfInventingACondition() throws SQLException {
        execute("DELETE FROM TextbookDetails WHERE listingId = 1");
        List<Listing> rows = database.getListings();
        assertEquals(3, rows.size());
        Listing incomplete = find(rows, 1);
        assertFalse(incomplete.hasConditionDetails());
        assertEquals("Details unavailable", incomplete.getConditionLabel());
        assertTrue(incomplete.getConditionIssue().contains("Restore missing details"));
        assertThrows(IllegalStateException.class, incomplete::gradeCondition);
        assertTrue(find(rows, 2).hasConditionDetails());
    }

    @Test
    void ownerRestoresAllThreeTypesWithoutChangingSharedData() throws SQLException {
        List<Listing> original = database.getListings();
        database.addInterest(1, 2);
        if (!database.getWaitlist(1).contains((long) 2)) {
            database.addInterest(1, 2);
        }
        database.addReview(1, 2, 4, "Keep this review");
        execute("DELETE FROM TextbookDetails");
        execute("DELETE FROM ElectronicsDetails");
        execute("DELETE FROM FurnitureDetails");
        for (int i = 0; i < original.size(); i++) {
            long owner = i + 1;
            assertEquals(original.get(i).getId(),
                    database.listingsForRepair(owner).getFirst().id());
            database.restoreDetails(owner, original.get(i));
            assertTrue(database.listingsForRepair(owner).isEmpty());
        }
        List<Listing> restored = new TestDatabase(database.getPath()).getListings();
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
        Listing wrongType = new Electronics(1, 1, 2, "Wrong type", new BigDecimal("10.00"), "PHP",
                "ACTIVE", true, false, false);
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
        execute("""
                CREATE TRIGGER reject_restore AFTER INSERT ON TextbookDetails
                BEGIN SELECT RAISE(ABORT, 'test failure'); END
                """);
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
        assertEquals(1,
                database.authenticate("  PERSON_A  ", account("person_a").password().toCharArray())
                        .getId());
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a",
                ("wrong-" + account("person_a").password()).toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a",
                (" " + account("person_a").password() + " ").toCharArray()));
    }

    @Test
    void emptyCredentialsCannotAuthenticateAnAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate(null, "x".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("  ", "x".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.authenticate("person_a", null));
    }

    @Test
    void eachDatabaseUsesFreshSaltAndStoresOnlyCredentialHashes() throws SQLException {
        TestDatabase other = new TestDatabase(folder.resolve("other.db"));
        other.initialize();
        try (var first = database.open();
                var second = other.open();
                var firstQuery = first.createStatement();
                var secondQuery = second.createStatement();
                var a = firstQuery.executeQuery(
                        "SELECT passwordSalt, passwordHash, recoveryHash FROM Members WHERE id = 1");
                var b = secondQuery.executeQuery(
                        "SELECT passwordSalt, passwordHash FROM Members WHERE id = 1")) {
            assertTrue(a.next());
            assertTrue(b.next());
            assertNotEquals(a.getString("passwordSalt"), b.getString("passwordSalt"));
            assertNotEquals(a.getString("passwordHash"), b.getString("passwordHash"));
            assertNotEquals(account("person_a").password(), a.getString("passwordHash"));
            assertNotEquals(account("person_a").recoveryCode(), a.getString("recoveryHash"));
        }
        assertEquals(
                1, other
                        .authenticate("person_a",
                                other.takeInitialAccounts().getFirst().password().toCharArray())
                        .getId());
    }

    @Test
    void passwordResetAcceptsBothLengthLimitsAndRejectsAnOversizedPassword() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "x".repeat(129).toCharArray()));
        String code = database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "abcdefgh".toCharArray());
        assertEquals(1, database.authenticate("person_a", "abcdefgh".toCharArray()).getId());
        database.resetPassword("person_a", code.toCharArray(), "x".repeat(128).toCharArray());
        assertEquals(1, database.authenticate("person_a", "x".repeat(128).toCharArray()).getId());
        assertEquals(2, database
                .authenticate("person_b", account("person_b").password().toCharArray()).getId());
    }

    @Test
    void failedPasswordResetKeepsOldPasswordAndRecoveryCodeUsable() throws SQLException {
        execute("""
                CREATE TRIGGER reject_reset AFTER UPDATE OF passwordHash ON Members
                BEGIN SELECT RAISE(ABORT, 'test rollback'); END
                """);
        assertThrows(SQLException.class, () -> database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), "NewPassword123!".toCharArray()));
        assertEquals(1, database
                .authenticate("person_a", account("person_a").password().toCharArray()).getId());
        assertThrows(IllegalArgumentException.class,
                () -> database.authenticate("person_a", "NewPassword123!".toCharArray()));
        execute("DROP TRIGGER reject_reset");
        database.resetPassword("person_a", account("person_a").recoveryCode().toCharArray(),
                "RetryPassword123!".toCharArray());
        assertEquals(1,
                database.authenticate("person_a", "RetryPassword123!".toCharArray()).getId());
    }

    @Test
    void emptyRecoveryInputsKeepTheOriginalAccountUsable() throws SQLException {
        assertThrows(IllegalArgumentException.class,
                () -> database.resetPassword(null, "x".toCharArray(), "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> database.resetPassword("person_a", null, "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class,
                () -> database.resetPassword("person_a", new char[0], "NewPassword".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> database.resetPassword("person_a",
                account("person_a").recoveryCode().toCharArray(), null));
        assertEquals(1, database
                .authenticate("person_a", account("person_a").password().toCharArray()).getId());
    }

    @Test
    void creationTimestampIsSavedOnceAndSurvivesStatusChanges() throws SQLException {
        Instant before = Instant.now();
        long id = database.insertListing(book("Created now", 0));
        Listing saved = find(database.getListings(), id);
        assertFalse(saved.getCreatedAt().isBefore(before));
        assertFalse(saved.getCreatedAt().isAfter(Instant.now()));
        new ListingDao(database).markSold(id, 1);
        Listing reopened = find(new TestDatabase(database.getPath()).getListings(), id);
        assertEquals(saved.getCreatedAt(), reopened.getCreatedAt());
        assertEquals(Listing.SOLD, reopened.getStatus());
    }

    @Test
    void versionThreeUpgradePreservesAccountsAndLeavesUnknownDatesUnknown() throws SQLException {
        String password = account("person_a").password();
        database.addInterest(1, 2);
        database.addReview(1, 2, 4, "Keep this review");
        try (Connection connection = database.open();
                var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE Listings DROP COLUMN created_at");
            statement.execute("PRAGMA ignore_check_constraints = ON");
            statement.execute("UPDATE Listings SET status = 'AVAILABLE' WHERE id = 1");
            statement.execute("UPDATE Listings SET status = 'WITHDRAWN' WHERE id = 2");
            statement.execute("DROP TABLE Messages");
            statement.execute("PRAGMA user_version = 3");
        }
        database.initialize();
        assertEquals(5, number("PRAGMA user_version"));
        assertEquals(1, database.authenticate("person_a", password.toCharArray()).getId());
        Listing active = find(database.getListings(), 1);
        assertEquals(Listing.ACTIVE, active.getStatus());
        assertNull(active.getCreatedAt());
        assertEquals(Listing.REMOVED, find(database.getListings(), 2).getStatus());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertTrue(database.reviews(1).contains("Keep this review"));
        database.initialize();
        assertEquals(3, database.getListings().size());
    }

    @Test
    void soldListingRetainsRelatedRowsAndRejectsFurtherInterestAndReviews() throws SQLException {
        database.addInterest(1, 2);
        database.addReview(1, 2, 4, "Keep after sale");
        ListingDao dao = new ListingDao(database);
        assertThrows(IllegalArgumentException.class, () -> dao.markSold(1, 2));
        dao.markSold(1, 1);
        assertEquals(Listing.SOLD, find(database.getListings(), 1).getStatus());
        assertEquals(List.of(2L), database.getWaitlist(1));
        assertTrue(database.reviews(1).contains("Keep after sale"));
        assertEquals(1, number("SELECT COUNT(*) FROM TextbookDetails WHERE listingId = 1"));
        assertThrows(IllegalArgumentException.class, () -> database.addInterest(1, 3));
        assertThrows(IllegalArgumentException.class, () -> database.addReview(1, 3, 5, "Too late"));
        assertThrows(IllegalArgumentException.class, () -> dao.markSold(1, 1));
    }

    @Test
    void missingElectronicsAndFurnitureRowsDoNotHideHealthyListings() throws SQLException {
        execute("DELETE FROM ElectronicsDetails");
        execute("DELETE FROM FurnitureDetails");
        List<Listing> rows = database.getListings();
        assertEquals(3, rows.size());
        assertTrue(find(rows, 1).hasConditionDetails());
        assertFalse(find(rows, 2).hasConditionDetails());
        assertFalse(find(rows, 3).hasConditionDetails());
        assertEquals("ELECTRONICS", find(rows, 2).getType());
        assertEquals("FURNITURE", find(rows, 3).getType());
        assertEquals(ConditionGrade.GOOD, find(rows, 1).gradeCondition());
    }

    @Test
    void concurrentBuyersReceiveDistinctAtomicWaitlistPositions() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> joinTogether(2, ready, start));
            var second = pool.submit(() -> joinTogether(3, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS));
            assertTrue(second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
        assertEquals(2, number("SELECT COUNT(*) FROM BuyerInterests WHERE listingId = 1"));
        assertEquals(2, number("SELECT COUNT(DISTINCT joiningOrder) FROM BuyerInterests"));
        assertEquals(1, number("SELECT MIN(joiningOrder) FROM BuyerInterests"));
        assertEquals(2, number("SELECT MAX(joiningOrder) FROM BuyerInterests"));
        assertTrue(database.getWaitlist(1).containsAll(List.of(2L, 3L)));
    }

    @Test
    void simultaneousRepeatJoinSavesOnlyOneInterest() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        boolean firstResult;
        boolean secondResult;
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> joinTogether(2, ready, start));
            var second = pool.submit(() -> joinTogether(2, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            firstResult = first.get(10, TimeUnit.SECONDS);
            secondResult = second.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
        }
        assertNotEquals(firstResult, secondResult);
        assertEquals(List.of(2L), database.getWaitlist(1));
    }

    @Test
    void failedListingMigrationRollsBackTheNewColumnAndVersion() throws SQLException {
        execute("ALTER TABLE Listings DROP COLUMN created_at");
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("PRAGMA ignore_check_constraints = ON");
            statement.execute("UPDATE Listings SET status = 'AVAILABLE' WHERE id = 1");
            statement.execute("DROP TABLE Messages");
            statement.execute("PRAGMA user_version = 3");
        }
        execute("""
                CREATE TRIGGER reject_listing_upgrade BEFORE UPDATE ON Listings
                BEGIN SELECT RAISE(ABORT, 'migration failure'); END
                """);
        assertThrows(SQLException.class, database::initialize);
        assertEquals(3, number("PRAGMA user_version"));
        assertEquals(0, number("""
                SELECT COUNT(*) FROM pragma_table_info('Listings') WHERE name = 'created_at'
                """));
        execute("DROP TRIGGER reject_listing_upgrade");
        database.initialize();
        assertEquals(5, number("PRAGMA user_version"));
        assertEquals(Listing.ACTIVE, find(database.getListings(), 1).getStatus());
    }

    private boolean joinTogether(long buyer, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Concurrent join did not start.");
        }
        try {
            new InterestDao(database).addInterest(1, buyer);
            return true;
        } catch (IllegalArgumentException duplicateOrIneligible) {
            return false;
        }
    }

    private Textbook book(String title, int missingPages) {
        return new Textbook(0, 1, 2, title, new BigDecimal("125.50"), "PHP", "ACTIVE", missingPages,
                WearLevel.LIGHT, true);
    }

    private Listing find(List<Listing> listings, long id) {
        for (Listing listing : listings) {
            if (listing.getId() == id) {
                return listing;
            }
        }
        throw new AssertionError("Listing not found: " + id);
    }

    private int number(String sql) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = database.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
