package edu.uc.campusmarket;

import edu.uc.campusmarket.api.ProductCatalogClient;
import edu.uc.campusmarket.controller.HomeController;
import edu.uc.campusmarket.controller.ChatController;
import edu.uc.campusmarket.dao.ChatDao;
import edu.uc.campusmarket.service.ChatService;
import edu.uc.campusmarket.dao.HomeDao;
import edu.uc.campusmarket.service.HomeService;
import edu.uc.campusmarket.controller.AccountController;
import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.FxRefreshSchedule;
import edu.uc.campusmarket.controller.FxTaskRunner;
import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.ReviewController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.controller.UserSession;
import edu.uc.campusmarket.controller.WaitlistController;
import edu.uc.campusmarket.dao.AnalyticsDao;
import edu.uc.campusmarket.dao.CategoryDao;
import edu.uc.campusmarket.dao.HistoryDao;
import edu.uc.campusmarket.dao.InterestDao;
import edu.uc.campusmarket.dao.ListingDao;
import edu.uc.campusmarket.dao.MemberDao;
import edu.uc.campusmarket.dao.ReviewDao;
import edu.uc.campusmarket.dao.SellerDao;
import edu.uc.campusmarket.persistence.Database;
import edu.uc.campusmarket.persistence.DatabaseChanges;
import edu.uc.campusmarket.service.AnalyticsService;
import edu.uc.campusmarket.service.CatalogService;
import edu.uc.campusmarket.service.ListingService;
import edu.uc.campusmarket.service.MemberService;
import edu.uc.campusmarket.service.ReviewService;
import edu.uc.campusmarket.service.SalesHistoryImporter;
import edu.uc.campusmarket.service.WaitlistService;
import edu.uc.campusmarket.ui.MarketplaceView;
import edu.uc.campusmarket.ui.UiTools;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Composition root: creates dependencies and owns the shared worker's lifetime.
 */
public class App extends Application {
    private ExecutorService worker;
    private RefreshController refresh;
    private AnalyticsController analytics;
    private TaskRunner tasks;

    /** Wires the application dependencies and opens the themed JavaFX window. */
    @Override
    public void start(Stage stage) {
        Database database = new Database(Database.defaultPath());
        MemberDao members = new MemberDao(database);
        ListingDao listings = new ListingDao(database);
        CategoryDao categories = new CategoryDao(database);
        ReviewDao reviews = new ReviewDao(database);
        HistoryDao history = new HistoryDao(database);
        AnalyticsDao summaries = new AnalyticsDao(database);
        SellerDao sellers = new SellerDao(database);
        InterestDao interests = new InterestDao(database);
        MemberService memberService = new MemberService(database, members);
        ReviewService reviewService = new ReviewService(reviews);
        WaitlistService waitlistService = new WaitlistService(interests, memberService);
        ListingService listingService = new ListingService(listings, categories, sellers,
                memberService, reviewService, waitlistService);
        AnalyticsService analysisService = new AnalyticsService(summaries, history,
                new SalesHistoryImporter(database, categories, history));
        CatalogService catalog = new CatalogService(new ProductCatalogClient());
        worker = Executors.newSingleThreadExecutor(action -> {
            Thread thread = new Thread(action, "marketplace-database");
            thread.setDaemon(true);
            return thread;
        });
        tasks = new FxTaskRunner(worker);
        UserSession session = new UserSession();
        AccountController accountController = new AccountController(memberService, tasks, session);
        ListingController listingController = new ListingController(listingService, catalog, tasks,
                session);
        ReviewController reviewController = new ReviewController(reviewService, tasks, session);
        WaitlistController waitlistController = new WaitlistController(waitlistService, tasks,
                session);
        HomeController home = new HomeController(new HomeService(new HomeDao(database)), tasks,
                session);
        ChatController chats = new ChatController(new ChatService(new ChatDao(database)), tasks,
                session);
        analytics = new AnalyticsController(analysisService, tasks, session);
        refresh = new RefreshController(tasks, session, listingController, analytics,
                new FxRefreshSchedule(), home);
        refresh.setChats(chats);
        refresh.setChanges(new DatabaseChanges(database));
        MarketplaceView view = new MarketplaceView(accountController, listingController,
                reviewController, waitlistController, analytics, refresh, session, tasks,
                database.getPath().toString(), home, chats);
        openWindow(stage, view);
    }

    private void openWindow(Stage stage, MarketplaceView view) {
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(UiTools.window(stage, view.getRoot()),
                Math.min(1200, bounds.getWidth()), Math.min(800, bounds.getHeight()));
        scene.getStylesheets().add(App.class.getResource("/ui/marketplace.css").toExternalForm());
        Runnable compact = () -> view.getRoot().pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("compact"),
                scene.getWidth() < 950 || scene.getHeight() < 640);
        scene.widthProperty().addListener((o, before, after) -> compact.run());
        scene.heightProperty().addListener((o, before, after) -> compact.run());
        compact.run();
        stage.setTitle("Campus Marketplace with Reviews");
        stage.setMinWidth(Math.min(760, bounds.getWidth()));
        stage.setMinHeight(Math.min(560, bounds.getHeight()));
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - scene.getWidth()) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - scene.getHeight()) / 2);
        stage.show();
        UiTools.maximizeWindow(stage);
        view.initialize();
    }

    /** Stops the component or worker without starting further work. */
    @Override
    public void stop() {
        if (refresh != null) {
            refresh.close();
        }
        if (analytics != null) {
            analytics.cancelImport();
        }
        if (worker != null) {
            worker.shutdownNow();
        }
    }

    /**
     * Launches the JavaFX application; arguments are passed to the JavaFX runtime.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
