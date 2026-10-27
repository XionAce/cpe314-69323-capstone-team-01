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
        assertEquals(1, number("PRAGMA user_version"));
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
        assertThrows(SQLException.class, database::getListings);
    }

    @Test
    void createsSellerProfileOnlyForAnExistingMember() throws SQLException {
        execute("INSERT INTO Members(id, name) VALUES (4, 'New demo member')");
        long sellerId = database.ensureSeller(4);
        assertEquals(sellerId, database.ensureSeller(4));
        assertEquals(4, number("SELECT COUNT(*) FROM Sellers"));
        assertThrows(IllegalArgumentException.class, () -> database.ensureSeller(999));
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
