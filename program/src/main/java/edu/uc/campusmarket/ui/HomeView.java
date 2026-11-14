package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.HomeController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.controller.UserSession;
import edu.uc.campusmarket.model.HomeItem;
import edu.uc.campusmarket.model.HomeSummary;
import java.util.Locale;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Personal home: renders the controller's account-scoped data, without SQL or
 * threads.
 */
public class HomeView extends ScrollPane {
    private final VBox content = new VBox(18);

    /**
     * Binds home results and supplies navigation to the marketplace and selling
     * form.
     */
    public HomeView(HomeController home, TaskRunner tasks, UserSession session, Runnable browse,
            Runnable sell) {
        setId("home-page");
        content.getStyleClass().add("page-content");
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
        home.summaryProperty()
                .addListener((o, before, value) -> render(value, tasks, session, browse, sell));
        render(null, tasks, session, browse, sell);
    }

    private void render(HomeSummary summary, TaskRunner tasks, UserSession session, Runnable browse,
            Runnable sell) {
        String name = session.member() == null ? "" : session.member().getName();
        VBox welcome = new VBox(10, UiTools.label("Welcome, " + name, "hero-title"),
                UiTools.label("Your listings, interested buyers and saved queue positions.",
                        "muted"),
                new FlowPane(10, 10,
                        UiTools.button("Browse marketplace", "home-browse", tasks, browse),
                        UiTools.button("Sell an item", "home-sell", tasks, sell)));
        welcome.getStyleClass().addAll("card", "market-hero");
        content.getChildren().setAll(welcome);
        if (summary == null) {
            content.getChildren().add(UiTools.label("Loading your account summary...", "muted"));
            return;
        }
        String rating = summary.reviewCount() == 0 ? "No reviews yet"
                : String.format(Locale.ROOT, "%.2f / 5 from %d reviews", summary.averageRating(),
                        summary.reviewCount());
        VBox stats = new VBox(8, UiTools.label("Your overview", "section-title"),
                UiTools.label(summary.listings().size() + " active listings | "
                        + summary.listings().stream().mapToInt(HomeItem::count).sum()
                        + " waiting buyers", "muted"),
                UiTools.label("Reviews received: " + rating, "muted"));
        stats.setId("home-summary");
        stats.getStyleClass().add("card");
        content.getChildren().addAll(stats, owned(summary), waiting(summary));
    }

    private VBox owned(HomeSummary summary) {
        VBox section = new VBox(12, UiTools.label("My active listings", "section-title"));
        if (summary.listings().isEmpty()) {
            section.getChildren().add(UiTools.label("You have no active listings yet.", "muted"));
        }
        for (HomeItem item : summary.listings()) {
            VBox card = new VBox(6, UiTools.label(item.title(), "section-title"), UiTools.label(item
                    .count() + " waiting | "
                    + (item.buyers().isBlank() ? "No interested buyers yet" : item.buyers()),
                    "muted"));
            card.getStyleClass().add("card");
            section.getChildren().add(card);
        }
        return section;
    }

    private VBox waiting(HomeSummary summary) {
        VBox section = new VBox(12, UiTools.label("My waitlist positions", "section-title"));
        if (summary.waitlists().isEmpty()) {
            section.getChildren().add(UiTools.label("You have not joined any waitlists.", "muted"));
        }
        for (HomeItem item : summary.waitlists()) {
            VBox card = new VBox(6, UiTools.label(item.title(), "section-title"),
                    UiTools.label(
                            "Position " + item.count() + " | " + UiTools.statusText(item.status()),
                            "muted"));
            card.getStyleClass().add("card");
            section.getChildren().add(card);
        }
        return section;
    }
}
