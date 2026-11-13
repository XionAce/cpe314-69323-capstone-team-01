package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewAnalyticsTest {
    @TempDir
    Path folder;
    private TestDatabase db;

    @BeforeEach
    void start() throws Exception {
        db = new TestDatabase(folder.resolve("test.db"));
        db.initialize();
    }

    @Test
    void optionalCommentIsTrimmedAndCannotExceedOneThousandCharacters() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 4, "x".repeat(1001)));
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 4, null));
        assertTrue(db.ratingSummary().isEmpty());
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 4, "x".repeat(1000));
        if (!db.getWaitlist(1).contains((long) 3)) {
            db.addInterest(1, 3);
        }
        db.addReview(1, 3, 5, "   ");
        try (var connection = db.open();
                var statement = connection
                        .prepareStatement("SELECT comment FROM Reviews ORDER BY id");
                var rows = statement.executeQuery()) {
            assertTrue(rows.next());
            assertEquals(1000, rows.getString(1).length());
            assertTrue(rows.next());
            assertEquals("", rows.getString(1));
        }
    }

    @Test
    void reviewForMissingOrWithdrawnListingCannotChangeSummaries() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> db.addReview(999, 2, 5, "Missing"));
        db.withdrawListing(1, 1);
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 5, "Withdrawn"));
        assertTrue(db.ratingSummary().isEmpty());
        assertEquals("No reviews yet.", db.reviews(1));
    }

    @Test
    void unknownReviewerRollsBackWithoutLeavingAReview() throws Exception {
        assertThrows(java.sql.SQLException.class, () -> db.addReview(1, 999, 5, "Unknown member"));
        assertTrue(db.ratingSummary().isEmpty());
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 3, "Valid retry");
        assertEquals(1, db.ratingSummary().getFirst().count());
    }

    @Test
    void categoryAverageWeightsEachReviewInsteadOfAveragingItemAverages() throws Exception {
        long second = db.insertListing(new Textbook(0, 1, 2, "Second book", BigDecimal.TEN, "PHP",
                "ACTIVE", 0, WearLevel.NONE, false));
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 5, "First");
        if (!db.getWaitlist(1).contains((long) 3)) {
            db.addInterest(1, 3);
        }
        db.addReview(1, 3, 5, "Second");
        if (!db.getWaitlist(second).contains((long) 2)) {
            db.addInterest(second, 2);
        }
        db.addReview(second, 2, 1, "Third");
        var result = db.ratingSummary().getFirst();
        assertEquals(11.0 / 3, result.average(), .00001);
        assertEquals(3, result.count());
        TestDatabase reopened = new TestDatabase(folder.resolve("test.db"));
        reopened.initialize();
        assertEquals(result, reopened.ratingSummary().getFirst());
    }

    @Test
    void tiedCategoryRatingsAreBothReturnedWithTheirOwnReviewCounts() throws Exception {
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 5, "Book");
        if (!db.getWaitlist(1).contains((long) 3)) {
            db.addInterest(1, 3);
        }
        db.addReview(1, 3, 5, "Book again");
        if (!db.getWaitlist(2).contains((long) 1)) {
            db.addInterest(2, 1);
        }
        db.addReview(2, 1, 5, "Laptop");
        var rows = db.ratingSummary();
        assertEquals(2, rows.size());
        assertEquals(5, rows.get(0).average());
        assertEquals(5, rows.get(1).average());
        assertEquals(2, rows.get(0).count());
        assertEquals(1, rows.get(1).count());
    }

    @Test
    void historicalSalesDoNotInflateTheCurrentListingPriceChart() throws Exception {
        var before = db.priceSummary("PHP");
        try (var connection = db.open();
                var statement = connection.prepareStatement(
                        """
                                INSERT INTO HistoricalSales(categoryId, sourceRecordId, priceMinor, currency, soldAt)
                                VALUES (2, 'old-sale', 99999999, 'PHP', '2026-10-20T08:00:00Z')
                                """)) {
            statement.executeUpdate();
        }
        assertEquals(before, db.priceSummary("PHP"));
        assertTrue(db.ratingSummary().isEmpty());
    }

    @Test
    void rejectsSelfDuplicateAndInvalidReviewsAndKeepsSavedReviews() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 1, 5, "self"));
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 0, "bad"));
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 6, "bad"));
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 1, "Needs work");
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 5, "twice"));
        if (!db.getWaitlist(1).contains((long) 3)) {
            db.addInterest(1, 3);
        }
        db.addReview(1, 3, 5, "Good");
        assertTrue(new TestDatabase(folder.resolve("test.db")).reviews(1).contains("Needs work"));
        assertEquals(3.0, db.ratingSummary().getFirst().average(), .00001);
        assertEquals(2, db.ratingSummary().getFirst().count());
    }

    @Test
    void changedDataChangesMeansWithoutReviewFanoutOrMixedCurrencies() throws Exception {
        double before = db.priceSummary("PHP").stream()
                .filter(r -> r.category().equals("Textbooks")).findFirst().orElseThrow().average();
        assertEquals(350, before);
        db.insertListing(new Textbook(0, 1, 2, "Second book", new BigDecimal("100"), "PHP",
                "ACTIVE", 0, WearLevel.NONE, false));
        db.insertListing(new Textbook(0, 1, 2, "Dollar book", new BigDecimal("999"), "USD",
                "ACTIVE", 0, WearLevel.NONE, false));
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 4, "OK");
        if (!db.getWaitlist(1).contains((long) 3)) {
            db.addInterest(1, 3);
        }
        db.addReview(1, 3, 5, "Great");
        var books = db.priceSummary("PHP").stream().filter(r -> r.category().equals("Textbooks"))
                .findFirst().orElseThrow();
        assertEquals(225, books.average());
        assertEquals(2, books.count());
        assertEquals(999, db.priceSummary("USD").getFirst().average());
        assertEquals(4.5, db.ratingSummary().getFirst().average());
    }

    @Test
    void emptySummariesAndWithdrawnItemsAreHandled() throws Exception {
        assertTrue(db.ratingSummary().isEmpty());
        assertTrue(db.priceSummary("EUR").isEmpty());
        if (!db.getWaitlist(1).contains((long) 2)) {
            db.addInterest(1, 2);
        }
        db.addReview(1, 2, 5, "Good");
        db.withdrawListing(1, 1);
        assertTrue(db.ratingSummary().isEmpty());
        assertFalse(
                db.priceSummary("PHP").stream().anyMatch(r -> r.category().equals("Textbooks")));
    }
}
