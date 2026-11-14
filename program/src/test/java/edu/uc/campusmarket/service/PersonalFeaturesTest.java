package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.HomeDao;
import edu.uc.campusmarket.dao.InterestDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.model.Listing;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PersonalFeaturesTest {
    @TempDir
    Path directory;
    private TestDatabase database;
    private ListingService listings;
    private WaitlistService waitlists;
    private HomeService home;

    @BeforeEach
    void setup() throws Exception {
        database = new TestDatabase(directory.resolve("personal.db"));
        database.initialize();
        var market = database.market();
        market.initialize();
        listings = new ListingService(market, new ListingDao(database), new CategoryDao(database));
        waitlists = new WaitlistService(market);
        home = new HomeService(new HomeDao(database));
    }

    private Listing first() throws Exception {
        return database.getListings().getFirst();
    }

    @Test
    void homeScopesListingsWaitlistsAndReviewsToTheRequestedMember() throws Exception {
        waitlists.join(1, 2);
        waitlists.join(1, 3);
        database.addReview(1, 2, 4, "Useful book");
        database.addReview(1, 3, 2, "Worn cover");
        var owner = home.loadFor(1);
        assertEquals(1, owner.listings().size());
        assertEquals(1, owner.listings().getFirst().listingId());
        assertEquals(2, owner.listings().getFirst().count());
        assertTrue(owner.listings().getFirst().buyers().contains("Person B"));
        assertEquals(2, owner.reviewCount());
        assertEquals(3.0, owner.averageRating());
        assertTrue(owner.waitlists().isEmpty());
        var buyer = home.loadFor(3);
        assertEquals(3, buyer.listings().getFirst().listingId());
        assertEquals(1, buyer.waitlists().getFirst().listingId());
        assertEquals(2, buyer.waitlists().getFirst().count());
        assertEquals(0, buyer.reviewCount());
        assertThrows(UnsupportedOperationException.class, () -> owner.listings().clear());
    }

    @Test
    void homeRejectsUnknownMemberAndOmitsSoldItemsFromActiveListings() {
        assertThrows(IllegalArgumentException.class, () -> home.loadFor(99));
        listings.markSold(1, 1);
        assertTrue(home.loadFor(1).listings().isEmpty());
    }

    @Test
    void ownerEditPreservesDateConditionReviewsAndWaitlist() throws Exception {
        Listing before = first();
        waitlists.join(1, 2);
        database.addReview(1, 2, 5, "Saved review");
        listings.update(1, before, "  Revised title  ", "425.25");
        Listing after = first();
        assertEquals("Revised title", after.getTitle());
        assertEquals(new BigDecimal("425.25"), after.getPrice());
        assertEquals(before.getCreatedAt(), after.getCreatedAt());
        assertEquals(before.getConditionLabel(), after.getConditionLabel());
        assertEquals(before.getSellerId(), after.getSellerId());
        assertEquals(1, waitlists.position(1, 2));
        assertTrue(database.reviews(1).contains("Saved review"));
    }

    @Test
    void editRejectsOtherOwnersAndStaleSoldOrRemovedRows() throws Exception {
        Listing before = first();
        assertThrows(IllegalArgumentException.class,
                () -> listings.update(2, before, "Wrong", "1"));
        listings.markSold(1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> listings.update(1, before, "Stale", "1"));
        assertEquals(before.getTitle(), first().getTitle());
        Listing laptop = database.getListings().get(1);
        listings.withdraw(2, laptop.getId());
        assertThrows(IllegalArgumentException.class,
                () -> listings.update(2, laptop, "Stale", "1"));
    }

    @Test
    void editValidationRejectsBlankNegativeFractionalAndOverflowPrices() throws Exception {
        Listing before = first();
        assertThrows(IllegalArgumentException.class, () -> listings.update(1, before, " ", "1"));
        for (String price : new String[] { "0", "-1", "1.001", "92233720368547758.08", "bad" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> listings.update(1, before, "Valid", price));
        }
        assertEquals(before.getTitle(), first().getTitle());
    }

    @Test
    void leavingAdvancesPositionAndRejoiningGoesToTheTailWithoutDeletingReviews() throws Exception {
        waitlists.join(1, 2);
        waitlists.join(1, 3);
        database.addReview(1, 2, 4, "Retained");
        waitlists.leave(1, 2);
        assertEquals(0, waitlists.position(1, 2));
        assertEquals(1, waitlists.position(1, 3));
        assertEquals(1, home.loadFor(3).waitlists().getFirst().count());
        assertTrue(database.reviews(1).contains("Retained"));
        waitlists.join(1, 2);
        assertEquals(2, waitlists.position(1, 2));
        assertEquals(java.util.List.of(3L, 2L), new InterestDao(database).getWaitlist(1));
        assertThrows(IllegalArgumentException.class,
                () -> database.addReview(1, 2, 5, "Duplicate"));
    }

    @Test
    void leaveCannotRemoveAnotherMembersInterestOrAcceptAnUnknownMember() {
        waitlists.join(1, 2);
        assertThrows(IllegalArgumentException.class, () -> waitlists.leave(1, 3));
        assertThrows(IllegalArgumentException.class, () -> waitlists.leave(1, 99));
        assertEquals(1, waitlists.position(1, 2));
        waitlists.leave(1, 2);
        assertThrows(IllegalArgumentException.class, () -> waitlists.leave(1, 2));
    }

    @Test
    void archivedWaitlistCanBeLeftAndPositionRemainsScopedToTheBuyer() {
        waitlists.join(1, 2);
        listings.markSold(1, 1);
        assertEquals(Listing.SOLD, home.loadFor(2).waitlists().getFirst().status());
        assertEquals(0, waitlists.position(1, 3));
        waitlists.leave(1, 2);
        assertTrue(home.loadFor(2).waitlists().isEmpty());
    }
}
