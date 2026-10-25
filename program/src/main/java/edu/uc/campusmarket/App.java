// A package groups related Java classes; this one holds the application's starting classes.
package edu.uc.campusmarket;

// Import the application's data, service and screen classes so we can connect them here.
import edu.uc.campusmarket.persistence.Database;
import edu.uc.campusmarket.persistence.MarketplaceRepository;
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.ui.MarketplaceView;
// These JavaFX classes provide the application lifecycle, window and scene.
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;

// Objects helps detect missing resources; the executor runs database work away from the UI thread.
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** JavaFX lifecycle. Database operations are serialized on a background worker. */
public final class App extends Application {
    // Extending Application lets JavaFX call start() when ready and stop() when closing.
    // One background worker handles tasks in order instead of blocking button clicks and drawing.
    private final ExecutorService databaseWorker = Executors.newSingleThreadExecutor(task -> {
        // task -> { ... } is a lambda: code that creates the thread used by this executor.
        Thread thread = new Thread(task, "campus-market-database");
        // A daemon worker does not keep Java alive by itself after the application closes.
        thread.setDaemon(true);
        // Hand the configured thread back to the executor.
        return thread;
    });

    @Override
    // @Override tells the compiler this method supplies behavior defined by Application.
    public void start(Stage stage) {
        // Stage is the desktop window. Database.defaultPath() chooses a stable writable file location.
        var database = new Database(Database.defaultPath());
        // var asks Java to infer the type; this variable is still a MarketplaceService, not any type.
        // The service uses a repository, and the repository uses the database connection factory.
        var service = new MarketplaceService(new MarketplaceRepository(database));
        // Give the screen access to its use cases and the shared background worker.
        var view = new MarketplaceView(service, databaseWorker);
        // Read the usable monitor area, excluding operating-system areas such as the taskbar.
        var bounds = Screen.getPrimary().getVisualBounds();
        // A Scene contains the screen's root node. Clamp its starting size for common laptop displays.
        var scene = new Scene(view.root(), Math.max(1000, Math.min(1360, bounds.getWidth() - 70)),
                Math.max(620, Math.min(860, bounds.getHeight() - 90)));
        // Load the bundled CSS from resources. It changes appearance, not the Java business rules.
        // requireNonNull gives a clear failure if the stylesheet was omitted from the build.
        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/ui/marketplace.css"), "Missing application stylesheet").toExternalForm());
        stage.setTitle("UC-B Campus Marketplace"); // Text displayed in the window's title bar.
        // Keep the window large enough for the sidebar and form; long pages can scroll.
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setScene(scene); // Put our JavaFX screen inside the desktop window.
        stage.show(); // Make the window visible.

        // An optional JVM property enables a short startup check; normal runs leave this false.
        boolean smoke = Boolean.getBoolean("campusmarket.smoke");
        // initialize() opens/reads the database on the worker, then calls these callbacks on the UI thread.
        view.initialize(snapshot -> {
            // snapshot is a read-only batch of data returned after successful initialization.
            if (smoke) {
                // This marker proves startup/read succeeded; it does not test every screen control.
                System.out.println("CAMPUS_MARKET_SMOKE_OK listings=" + snapshot.listings().size()
                        + " database=" + snapshot.databasePath());
                Platform.exit(); // Close JavaFX after the optional startup check.
            }
        }, failure -> {
            // This second callback handles initialization failure during a smoke check.
            if (smoke) {
                System.err.println("CAMPUS_MARKET_SMOKE_FAILED");
                failure.printStackTrace(System.err); // Print technical details for troubleshooting.
                Platform.exit();
            }
        });
    }

    @Override
    public void stop() {
        // Ask the worker to stop when JavaFX closes; database tasks also close their own JDBC resources.
        databaseWorker.shutdownNow();
    }
}
