package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import edu.uc.campusmarket.persistence.Database;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MarketplaceServiceTest {
    @TempDir Path folder;
    private MarketplaceService service;

    @BeforeEach
    void startWithFreshDatabase() throws Exception {
        service = new MarketplaceService(new Database(folder.resolve("test.db")));
        service.initialize();
    }

    @Test
    void recursionFindsDeepDescendantsButExcludesSiblings() {
        List<Category> tree = List.of(new Category(1, null, "Root"),
                new Category(2, 1L, "Electronics"), new Category(3, 2L, "Computers"),
                new Category(4, 3L, "Laptops"), new Category(5, 4L, "Gaming"),
                new Category(6, 1L, "Furniture"));
        assertEquals(List.of(2L, 3L, 4L, 5L), MarketplaceService.categoryIdsUnder(2, tree));
        assertEquals(List.of(5L), MarketplaceService.categoryIdsUnder(5, tree));
    }

    @Test
    void recursionRejectsCyclesAndMissingParents() {
        List<Category> cycle = List.of(new Category(10, 11L, "A"), new Category(11, 10L, "B"));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceService.categoryIdsUnder(10, cycle));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceService.categoryIdsUnder(1,
                List.of(new Category(1, 99L, "Missing parent"))));
    }

    @Test
    void duplicateCategoryIdsAndUnknownSelectionAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MarketplaceService.categoryIdsUnder(1,
                List.of(new Category(1, null, "A"), new Category(1, null, "B"))));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceService.categoryIdsUnder(99,
                List.of(new Category(1, null, "Root"))));
    }

    @Test
    void keywordSearchIsLiteralAndCaseInsensitive() throws Exception {
        long id = postBook("Student's SQL ' OR 1=1 -- guide");
        List<Listing> result = service.search(2, "' or 1=1 --");
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
        assertTrue(service.search(3, "' or 1=1 --").isEmpty());
    }

    @Test
    void newPostBelongsToSelectedMemberAndSurvivesReopen() throws Exception {
        long id = postBook("Demo notes");
        MarketplaceService reopened = new MarketplaceService(new Database(folder.resolve("test.db")));
        reopened.initialize();
        assertTrue(contains(reopened.myListings(1), id));
        assertFalse(contains(reopened.myListings(2), id));
    }

    @Test
    void viewingUsesUniqueLifoHistoryAndKeepsMembersSeparate() throws Exception {
        long first = postBook("First");
        long second = postBook("Second");
        service.view(2, first);
        service.view(2, second);
        service.view(2, first);
        assertEquals(List.of(first, second), ids(service.recent(2)));
        assertEquals(List.of(first, second), ids(service.recent(2))); // Reading does not consume it.
        assertTrue(service.recent(3).isEmpty());
    }

    @Test
    void recentHistoryIsBoundedAndStartsEmptyNextSession() throws Exception {
        List<Long> viewed = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            long id = postBook("Book " + i);
            viewed.add(id);
            service.view(2, id);
        }
        assertEquals(20, service.recent(2).size());
        assertEquals(viewed.get(20), service.recent(2).get(0).getId());
        assertFalse(contains(service.recent(2), viewed.get(0)));
        MarketplaceService reopened = new MarketplaceService(new Database(folder.resolve("test.db")));
        reopened.initialize();
        assertTrue(reopened.recent(2).isEmpty());
    }

    @Test
    void withdrawnListingLeavesMarketplaceAndHistoryButStaysInOwnersArchive() throws Exception {
        long id = postBook("Archive me");
        service.view(2, id);
        service.joinWaitlist(id, 2);
        assertThrows(IllegalArgumentException.class, () -> service.withdraw(2, id));
        service.withdraw(1, id);
        assertFalse(contains(service.search(1, ""), id));
        assertTrue(service.recent(2).isEmpty());
        assertTrue(contains(service.myListings(1), id));
        assertEquals(List.of(2L), service.getWaitlist(id));
        assertThrows(IllegalArgumentException.class, () -> service.joinWaitlist(id, 3));
    }

    @Test
    void waitlistKeepsFifoOrderAndRejectsDuplicatesAndOwner() throws Exception {
        long id = postBook("Queue demo");
        service.joinWaitlist(id, 3);
        assertEquals(List.of(3L, 2L), service.joinWaitlist(id, 2));
        assertThrows(IllegalArgumentException.class, () -> service.joinWaitlist(id, 2));
        assertThrows(IllegalArgumentException.class, () -> service.joinWaitlist(id, 1));
        assertEquals(List.of(3L, 2L), service.getWaitlist(id));
    }

    private long postBook(String title) throws Exception {
        return service.post(1, new Textbook(0, service.sellerFor(1), 2, title,
                new BigDecimal("150.00"), "PHP", "AVAILABLE", 0, WearLevel.LIGHT, false));
    }

    private boolean contains(List<Listing> listings, long id) {
        return ids(listings).contains(id);
    }

    private List<Long> ids(List<Listing> listings) {
        List<Long> result = new ArrayList<>();
        for (Listing listing : listings) result.add(listing.getId());
        return result;
    }
}
