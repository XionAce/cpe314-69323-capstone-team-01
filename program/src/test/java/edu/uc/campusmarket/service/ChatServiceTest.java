package edu.uc.campusmarket.service;

import edu.uc.campusmarket.TestDatabase;
import edu.uc.campusmarket.dao.ChatDao;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {
    @TempDir
    Path directory;
    private TestDatabase database;
    private ChatService chats;

    @BeforeEach
    void setup() throws Exception {
        database = new TestDatabase(directory.resolve("chat.db"));
        database.initialize();
        chats = new ChatService(new ChatDao(database));
    }

    @Test
    void buyerStartsAndSellerRepliesWithSavedPlainTextAndUtcTimes() {
        var opened = chats.load(2, 1, 0);
        assertEquals(1, opened.selected().sellerMemberId());
        assertEquals("Person A", opened.selected().partnerName(2));
        assertTrue(chats.load(1, 0, 0).threads().isEmpty());
        Instant before = Instant.now();
        chats.send(2, 1, 2, "  Where can we meet?\nI'm near the library.  ");
        chats.send(1, 1, 2, "Library entrance at 3 pm. 'Quoted' text <stays plain>.");
        var buyer = chats.load(2, 1, 2);
        var seller = chats.load(1, 1, 2);
        assertEquals(buyer.messages(), seller.messages());
        assertEquals(List.of(2L, 1L), buyer.messages().stream().map(m -> m.senderId()).toList());
        assertEquals("Where can we meet?\nI'm near the library.", buyer.messages().getFirst().body());
        assertFalse(buyer.messages().getFirst().sentAt().isBefore(before));
        assertTrue(buyer.messages().getFirst().id() < buyer.messages().getLast().id());
        assertThrows(UnsupportedOperationException.class, () -> buyer.messages().clear());
    }

    @Test
    void separateBuyersCannotReadOrSendToEachOthersConversation() throws Exception {
        chats.send(2, 1, 2, "Private buyer B");
        chats.send(3, 1, 3, "Private buyer C");
        assertEquals(2, chats.load(1, 0, 0).threads().size());
        assertEquals(1, chats.load(2, 0, 0).threads().size());
        assertEquals(1, chats.load(3, 0, 0).threads().size());
        assertTrue(chats.load(3, 1, 2).messages().isEmpty());
        assertTrue(new ChatDao(database).messages(1, 2, 3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> chats.send(3, 1, 2, "Pretending to be B"));
        assertEquals(1, chats.load(2, 1, 2).messages().size());
    }

    @Test
    void rejectsSelfChatsUnknownIdentitiesAndSellerInitiationBeforeABuyerWrites() {
        assertNull(chats.load(1, 1, 0).selected());
        assertThrows(IllegalArgumentException.class, () -> chats.send(1, 1, 1, "My own item"));
        assertThrows(IllegalArgumentException.class, () -> chats.send(1, 1, 2, "Unrequested chat"));
        assertThrows(IllegalArgumentException.class, () -> chats.send(99, 1, 2, "Unknown sender"));
        assertThrows(IllegalArgumentException.class, () -> chats.send(2, 999, 2, "Unknown item"));
        assertThrows(IllegalArgumentException.class, () -> chats.send(1, 1, 99, "Unknown buyer"));
        assertTrue(chats.load(1, 0, 0).threads().isEmpty());
    }

    @Test
    void rejectsBlankAndOversizedMessagesButAcceptsTheExactBoundary() {
        for (String text : new String[] { null, "", " \n\t ", "x".repeat(2001) }) {
            assertThrows(IllegalArgumentException.class, () -> chats.send(2, 1, 2, text));
        }
        assertTrue(chats.load(1, 0, 0).threads().isEmpty());
        chats.send(2, 1, 2, "x".repeat(ChatService.MAX_MESSAGE_LENGTH));
        assertEquals(2000, chats.load(1, 1, 2).messages().getFirst().body().length());
    }

    @Test
    void existingSoldConversationContinuesButRemovedHistoryIsReadOnly() throws Exception {
        chats.send(2, 1, 2, "Interested");
        new edu.uc.campusmarket.dao.ListingDao(database).markSold(1, 1);
        chats.send(1, 1, 2, "Sold to you; we can still arrange pickup.");
        assertThrows(IllegalArgumentException.class, () -> chats.send(3, 1, 3, "New sold-item chat"));
        assertEquals(2, chats.load(2, 1, 2).messages().size());
        try (var connection = database.open(); var query = connection.prepareStatement(
                "UPDATE Listings SET status = 'REMOVED' WHERE id = ?")) {
            query.setLong(1, 1);
            query.executeUpdate();
        }
        assertFalse(chats.load(2, 1, 2).selected().canSend());
        assertThrows(IllegalArgumentException.class, () -> chats.send(1, 1, 2, "Removed"));
        assertEquals(2, chats.load(2, 1, 2).messages().size());
    }

    @Test
    void chatDoesNotJoinBuySellOrLoseHistoryWhenABuyerLeavesTheQueue() throws Exception {
        chats.send(2, 1, 2, "Is this available?");
        assertTrue(database.getWaitlist(1).isEmpty());
        assertEquals("ACTIVE", database.getListings().getFirst().getStatus());
        var waitlists = new WaitlistService(database.market());
        waitlists.join(1, 2);
        waitlists.leave(1, 2);
        var reopened = new ChatService(new ChatDao(new TestDatabase(database.getPath())));
        assertEquals("Is this available?", reopened.load(1, 1, 2).messages().getFirst().body());
    }

    @Test
    void versionFourUpgradeAddsChatWithoutResettingCredentialsOrListingData() throws Exception {
        var member = database.getMembers().getFirst();
        var listing = database.getListings().getFirst();
        String hash;
        try (var connection = database.open(); var query = connection.createStatement()) {
            try (var rows = query.executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
                assertTrue(rows.next());
                hash = rows.getString(1);
            }
            query.execute("DROP TABLE Messages");
            query.execute("PRAGMA user_version = 4");
        }
        TestDatabase upgraded = new TestDatabase(database.getPath());
        upgraded.initialize();
        assertTrue(upgraded.takeInitialAccounts().isEmpty());
        assertEquals(member.getName(), upgraded.getMembers().getFirst().getName());
        assertEquals(listing.getCreatedAt(), upgraded.getListings().getFirst().getCreatedAt());
        try (var connection = upgraded.open(); var query = connection.createStatement()) {
            try (var rows = query.executeQuery("PRAGMA user_version")) {
                assertTrue(rows.next());
                assertEquals(5, rows.getInt(1));
            }
            try (var rows = query.executeQuery("SELECT passwordHash FROM Members WHERE id = 1")) {
                assertTrue(rows.next());
                assertEquals(hash, rows.getString(1));
            }
        }
        new ChatService(new ChatDao(upgraded)).send(2, 1, 2, "Migration works");
        assertEquals(1, chats.load(1, 1, 2).messages().size());
    }

    @Test
    void simultaneousRepliesAreSavedOnceAndDisplayedInDatabaseOrder() throws Exception {
        chats.send(2, 1, 2, "Start");
        try (var workers = Executors.newFixedThreadPool(2)) {
            var buyer = workers.submit(() -> chats.send(2, 1, 2, "Buyer reply"));
            var seller = workers.submit(() -> chats.send(1, 1, 2, "Seller reply"));
            buyer.get();
            seller.get();
        }
        var saved = chats.load(1, 1, 2).messages();
        assertEquals(3, saved.size());
        assertEquals(3, saved.stream().map(m -> m.id()).distinct().count());
        assertEquals(saved.stream().map(m -> m.id()).sorted().toList(),
                saved.stream().map(m -> m.id()).toList());
    }
}
