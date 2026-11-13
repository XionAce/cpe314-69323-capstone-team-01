package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.AnalyticsSnapshot;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.InitialAccount;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Member;
import edu.uc.campusmarket.model.Registration;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import edu.uc.campusmarket.service.AnalyticsService;
import edu.uc.campusmarket.service.CatalogService;
import edu.uc.campusmarket.service.ListingService;
import edu.uc.campusmarket.service.MemberService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ControllerTest {
    @Test
    void chatRejectsOldSessionResultsAndClearsPrivateDraftsOnLogout() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        ChatController chat = new ChatController(new Chats(), runner, session);
        session.signIn(new Member(2, "Buyer"));
        runner.automatic = false;
        chat.refresh(false);
        chat.draftProperty().set("Private unsent text");
        session.signOut();
        runner.finish();
        assertTrue(chat.threads().isEmpty());
        assertTrue(chat.messages().isEmpty());
        assertNull(chat.selectedProperty().get());
        assertEquals("", chat.draftProperty().get());
    }

    @Test
    void chatDefersNavigationAndDoesNotApplyThePreviousConversation() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        ChatController chat = new ChatController(new Chats(), runner, session);
        session.signIn(new Member(2, "Buyer"));
        runner.automatic = false;
        chat.refresh(false);
        chat.open(3);
        runner.finish();
        assertTrue(chat.messages().isEmpty());
        assertNotNull(runner.pending);
        runner.finish();
        assertEquals(3, chat.selectedProperty().get().listingId());
        assertEquals("Item 3", chat.messages().getFirst().body());
    }

    @Test
    void chatSendPreservesNewTypingAndKeepsTextWhenSavingFails() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        Chats service = new Chats();
        ChatController chat = new ChatController(service, runner, session);
        session.signIn(new Member(2, "Buyer"));
        chat.refresh(false);
        runner.automatic = false;
        chat.draftProperty().set("First message");
        chat.send();
        chat.draftProperty().set("Next message");
        runner.finish();
        assertEquals("First message", service.sent);
        assertEquals("Next message", chat.draftProperty().get());
        service.fail = true;
        chat.send();
        runner.finish();
        assertEquals("Next message", chat.draftProperty().get());
        assertEquals("Try again", runner.error.get());
    }

    private static class Chats extends edu.uc.campusmarket.service.ChatService {
        String sent;
        boolean fail;

        Chats() {
            super(null);
        }

        @Override
        public edu.uc.campusmarket.model.ChatSnapshot load(long member, long listing, long buyer) {
            long item = listing == 0 ? 1 : listing;
            var thread = new edu.uc.campusmarket.model.ChatThread(item, member, 1, "Item " + item,
                    "Buyer", "Seller", "ACTIVE", "", 0);
            var message = new edu.uc.campusmarket.model.ChatMessage(item, member, "Buyer",
                    "Item " + item, java.time.Instant.EPOCH);
            return new edu.uc.campusmarket.model.ChatSnapshot(List.of(thread), thread,
                    List.of(message));
        }

        @Override
        public void send(long member, long listing, long buyer, String message) {
            if (fail) {
                throw new IllegalArgumentException("Try again");
            }
            sent = message;
        }
    }

    @Test
    void personalHomeDefersBusyLoadAndRejectsResultsAfterLogout() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        var service = new edu.uc.campusmarket.service.HomeService(null) {
            @Override
            public edu.uc.campusmarket.model.HomeSummary loadFor(long member) {
                return new edu.uc.campusmarket.model.HomeSummary(List.of(), List.of(), (int) member,
                        4.0);
            }
        };
        HomeController home = new HomeController(service, runner, session);
        session.signIn(new Member(1, "One"));
        runner.busy.set(true);
        home.load(false);
        assertNull(home.summaryProperty().get());
        runner.busy.set(false);
        assertEquals(1, home.summaryProperty().get().reviewCount());
        runner.automatic = false;
        home.load(false);
        session.signOut();
        runner.finish();
        assertNull(home.summaryProperty().get());
        runner.automatic = true;
        session.signIn(new Member(2, "Two"));
        home.load(false);
        assertEquals(2, home.summaryProperty().get().reviewCount());
    }

    private static Listing book(long id, String title) {
        return new Textbook(id, 1, 2, title, BigDecimal.TEN, "PHP", "ACTIVE", 0, WearLevel.NONE,
                false);
    }

    @Test
    void loginUsesServiceAndClearsTheSubmittedSecret() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        AccountController controller = new AccountController(new Members(), runner, session);
        controller.initialize();
        char[] password = "test-password".toCharArray();
        controller.login("new_user", password);
        assertEquals(4, session.memberId());
        assertEquals('\0', password[0]);
    }

    @Test
    void registrationReturnsRecoveryStateWithoutSigningInAutomatically() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        AccountController controller = new AccountController(new Members(), runner, session);
        controller.initialize();
        char[] password = "test-password".toCharArray();
        controller.register("New", "new_user", password);
        assertEquals("new-recovery", controller.recoveryCodeProperty().get());
        assertTrue(controller.noticeProperty().get().contains("Account created"));
        assertNull(session.member());
        assertEquals('\0', password[0]);
    }

    @Test
    void logoutWaitsForActiveWorkThenClearsTheSession() {
        Runner runner = new Runner();
        UserSession session = new UserSession();
        AccountController controller = new AccountController(new Members(), runner, session);
        session.signIn(new Member(1, "One"));
        runner.busy.set(true);
        controller.logout();
        assertNotNull(session.member());
        runner.busy.set(false);
        controller.logout();
        assertNull(session.member());
    }

    @Test
    void timerSkipsBusyDialogsInactivePagesAndStopsAtLogoutAndClose() {
        Fixture fixture = new Fixture();
        Clock clock = new Clock();
        RefreshController refresh = new RefreshController(fixture.runner, fixture.session,
                fixture.listings, fixture.analytics, clock);
        fixture.session.signIn(new Member(1, "One"));
        int initial = fixture.service.searches;
        clock.tick.run();
        assertEquals(initial + 1, fixture.service.searches);
        fixture.runner.busy.set(true);
        clock.tick.run();
        fixture.runner.busy.set(false);
        refresh.setDialogOpen(true);
        clock.tick.run();
        refresh.setDialogOpen(false);
        refresh.setPage(1);
        clock.tick.run();
        assertEquals(initial + 1, fixture.service.searches);
        refresh.setPage(3);
        clock.tick.run();
        assertEquals(1, fixture.analysis.loads);
        fixture.session.signOut();
        assertTrue(clock.stops > 0);
        int loads = fixture.analysis.loads;
        refresh.tick();
        assertEquals(loads, fixture.analysis.loads);
        refresh.close();
        int starts = clock.starts;
        fixture.session.signIn(new Member(2, "Two"));
        assertEquals(starts, clock.starts);
    }

    @Test
    void staleSearchCannotRepopulateTheScreenAfterLogout() {
        Fixture fixture = new Fixture();
        fixture.session.signIn(new Member(1, "One"));
        fixture.runner.automatic = false;
        fixture.listings.refresh();
        fixture.session.signOut();
        fixture.runner.finish();
        assertTrue(fixture.listings.items().isEmpty());
        assertNull(fixture.listings.selectedProperty().get());
    }

    @Test
    void refreshKeepsSelectedIdentityWhenAnotherListingChanges() {
        Fixture fixture = new Fixture();
        fixture.session.signIn(new Member(1, "One"));
        fixture.listings.selectedProperty().set(fixture.listings.items().getFirst());
        fixture.service.rows = List.of(book(1, "First"), book(2, "Changed"));
        fixture.listings.refresh();
        assertEquals(1, fixture.listings.selectedProperty().get().getId());
        assertEquals("Changed", fixture.listings.items().getLast().getTitle());
    }

    @Test
    void failedPollingKeepsDisplayedDataAndExposesTheFailure() {
        Fixture fixture = new Fixture();
        fixture.session.signIn(new Member(1, "One"));
        fixture.service.fail = true;
        fixture.listings.refresh(true);
        assertEquals(2, fixture.listings.items().size());
        assertEquals("Read failed", fixture.runner.error.get());
        assertFalse(fixture.runner.busy.get());
    }

    @Test
    void unchangedAnalysisDoesNotNotifyTheChartToRedraw() {
        Fixture fixture = new Fixture();
        fixture.session.signIn(new Member(1, "One"));
        int[] notifications = { 0 };
        fixture.analytics.snapshotProperty().addListener((o, a, b) -> notifications[0]++);
        fixture.analytics.refresh();
        fixture.analytics.refresh(true);
        assertEquals(1, notifications[0]);
    }

    @Test
    void postingUsesTheActiveSessionIdentity() {
        Fixture fixture = new Fixture();
        fixture.session.signIn(new Member(7, "Seven"));
        fixture.listings.post("Book", "10", "TEXTBOOK", new Category(2, 1L, "Textbooks"),
                "No visible damage", () -> {
                });
        assertEquals(7, fixture.service.postedBy);
    }

    @Test
    void incompleteConditionRowsRemainVisibleDuringControllerRefresh() {
        Fixture fixture = new Fixture();
        fixture.service.rows = List.of(Listing.incomplete(1, 1, 2, "Missing details",
                BigDecimal.TEN, "PHP", Listing.ACTIVE, null, "ELECTRONICS", "Needs repair"));
        fixture.session.signIn(new Member(1, "One"));
        assertEquals(1, fixture.listings.items().size());
        assertEquals("Details unavailable",
                fixture.listings.items().getFirst().getConditionLabel());
        fixture.listings.refresh();
        assertEquals(1, fixture.listings.items().size());
        assertEquals("", fixture.runner.error.get());
    }

    @Test
    void ownerStatusFilterAndManageStateFollowTheSelectedItem() {
        Fixture fixture = new Fixture();
        fixture.service.rows = List.of(book(1, "Active"), new Textbook(2, 1, 2, "Sold",
                BigDecimal.TEN, "PHP", Listing.SOLD, 0, WearLevel.NONE, false));
        fixture.session.signIn(new Member(1, "One"));
        fixture.listings.ownProperty().set(true);
        fixture.listings.statusFilterProperty().set(Listing.SOLD);
        fixture.listings.refresh();
        assertEquals(1, fixture.listings.items().size());
        fixture.listings.selectedProperty().set(fixture.listings.items().getFirst());
        assertFalse(fixture.listings.canManageProperty().get());
        fixture.listings.statusFilterProperty().set("ALL");
        fixture.listings.refresh();
        fixture.listings.selectedProperty().set(fixture.listings.items().getFirst());
        assertTrue(fixture.listings.canManageProperty().get());
        fixture.listings.ownProperty().set(false);
        assertFalse(fixture.listings.canManageProperty().get());
    }

    @Test
    void quickFilterChangeRetriesAfterTheOlderRequestCompletes() {
        Fixture fixture = new Fixture();
        fixture.service.rows = List.of(book(1, "Active"), new Textbook(2, 1, 2, "Sold",
                BigDecimal.TEN, "PHP", Listing.SOLD, 0, WearLevel.NONE, false));
        fixture.session.signIn(new Member(1, "One"));
        fixture.runner.automatic = false;
        fixture.listings.refresh();
        fixture.listings.ownProperty().set(true);
        fixture.listings.statusFilterProperty().set(Listing.SOLD);
        fixture.listings.refresh();
        fixture.runner.finish();
        assertNotNull(fixture.runner.pending);
        fixture.runner.finish();
        assertEquals(1, fixture.listings.items().size());
        assertEquals(Listing.SOLD, fixture.listings.items().getFirst().getStatus());
    }

    private static class Fixture {
        final Runner runner = new Runner();
        final UserSession session = new UserSession();
        final Listings service = new Listings();
        final Analysis analysis = new Analysis();
        final ListingController listings = new ListingController(service, new CatalogService(null),
                runner, session);
        final AnalyticsController analytics = new AnalyticsController(analysis, runner, session,
                Runnable::run);
    }

    private static class Members extends MemberService {
        Members() {
            super(null, null);
        }

        @Override
        public List<InitialAccount> initialize() {
            return List.of();
        }

        @Override
        public Member login(String username, char[] password) {
            return new Member(4, "New");
        }

        @Override
        public Registration register(String name, String username, char[] password) {
            return new Registration(new Member(4, name), username, "new-recovery");
        }
    }

    private static class Listings extends ListingService {
        int searches;
        long postedBy;
        boolean fail;
        List<Listing> rows = List.of(book(1, "First"), book(2, "Second"));

        Listings() {
            super(null, null, null);
        }

        @Override
        public List<Category> categories() {
            return List.of(new Category(1, null, "All finds"));
        }

        @Override
        public List<Listing> search(long member, long category, String query, boolean own) {
            searches++;
            if (fail) {
                throw new IllegalArgumentException("Read failed");
            }
            return rows;
        }

        @Override
        public long post(long member, String title, String price, String type, long category,
                String evidence) {
            postedBy = member;
            return 8;
        }
    }

    private static class Analysis extends AnalyticsService {
        int loads;

        Analysis() {
            super(null, null, null);
        }

        @Override
        public AnalyticsSnapshot load(String currency) {
            loads++;
            return new AnalyticsSnapshot(List.of(), List.of(), List.of(), "No sales");
        }
    }

    private static class Clock implements RefreshSchedule {
        int starts;
        int stops;
        Runnable tick;

        public void start(Runnable action) {
            starts++;
            tick = action;
        }

        public void stop() {
            stops++;
        }
    }

    private static class Runner implements TaskRunner {
        final BooleanProperty busy = new SimpleBooleanProperty();
        final StringProperty status = new SimpleStringProperty("");
        final StringProperty error = new SimpleStringProperty("");
        boolean automatic = true;
        Runnable pending;

        @Override
        public <T> boolean run(Callable<T> work, Consumer<T> success, boolean quiet) {
            if (busy.get()) {
                return false;
            }
            busy.set(true);
            pending = () -> {
                try {
                    T value = work.call();
                    busy.set(false);
                    success.accept(value);
                } catch (Exception failure) {
                    busy.set(false);
                    error.set(failure.getMessage());
                }
            };
            if (automatic) {
                finish();
            }
            return true;
        }

        void finish() {
            Runnable action = pending;
            pending = null;
            action.run();
        }

        public ReadOnlyBooleanProperty busyProperty() {
            return busy;
        }

        public ReadOnlyStringProperty statusProperty() {
            return status;
        }

        public ReadOnlyStringProperty errorProperty() {
            return error;
        }
    }
}
