package edu.uc.campusmarket.persistence;

import edu.uc.campusmarket.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ReviewAnalyticsTest {
    @TempDir Path folder;
    private Database db;
    @BeforeEach void start() throws Exception { db = new Database(folder.resolve("test.db")); db.initialize(); }

    @Test void rejectsSelfDuplicateAndInvalidReviewsAndKeepsSavedReviews() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 1, 5, "self"));
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 0, "bad"));
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 6, "bad"));
        db.addReview(1, 2, 1, "Needs work");
        assertThrows(IllegalArgumentException.class, () -> db.addReview(1, 2, 5, "twice"));
        db.addReview(1, 3, 5, "Good");
        assertTrue(new Database(folder.resolve("test.db")).reviews(1).contains("Needs work"));
        assertEquals(3.0, db.ratingSummary().getFirst().average(), .00001);
        assertEquals(2, db.ratingSummary().getFirst().count());
    }

    @Test void changedDataChangesMeansWithoutReviewFanoutOrMixedCurrencies() throws Exception {
        double before = db.priceSummary("PHP").stream().filter(r -> r.category().equals("Textbooks")).findFirst().orElseThrow().average();
        assertEquals(350, before);
        db.insertListing(new Textbook(0, 1, 2, "Second book", new BigDecimal("100"), "PHP", "AVAILABLE", 0, WearLevel.NONE, false));
        db.insertListing(new Textbook(0, 1, 2, "Dollar book", new BigDecimal("999"), "USD", "AVAILABLE", 0, WearLevel.NONE, false));
        db.addReview(1, 2, 4, "OK"); db.addReview(1, 3, 5, "Great");
        var books = db.priceSummary("PHP").stream().filter(r -> r.category().equals("Textbooks")).findFirst().orElseThrow();
        assertEquals(225, books.average()); assertEquals(2, books.count());
        assertEquals(999, db.priceSummary("USD").getFirst().average());
        assertEquals(4.5, db.ratingSummary().getFirst().average());
    }

    @Test void emptySummariesAndWithdrawnItemsAreHandled() throws Exception {
        assertTrue(db.ratingSummary().isEmpty()); assertTrue(db.priceSummary("EUR").isEmpty());
        db.addReview(1, 2, 5, "Good"); db.withdrawListing(1, 1);
        assertTrue(db.ratingSummary().isEmpty());
        assertFalse(db.priceSummary("PHP").stream().anyMatch(r -> r.category().equals("Textbooks")));
    }
}
