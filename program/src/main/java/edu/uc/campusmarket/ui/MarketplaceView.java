package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AccountController;
import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.HomeController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.ReviewController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.controller.UserSession;
import edu.uc.campusmarket.controller.WaitlistController;
import edu.uc.campusmarket.model.InitialAccount;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Application shell: composes small views and binds their shared status. */
public class MarketplaceView {
    private final BorderPane root = new BorderPane();
    private final BorderPane workspace = new BorderPane();
    private final AccountController accounts;
    private final TaskRunner tasks;
    private final RefreshController refresh;
    private final UserSession session;
    private final Label title = UiTools.label("Marketplace", "page-title");
    private final VBox accountPane = new VBox();
    private final TabPane pages = new TabPane();
    private HBox sidebar;

    /** Creates this component from the supplied values or dependencies. */
    public MarketplaceView(AccountController accounts, ListingController listings,
            ReviewController reviews, WaitlistController waitlists, AnalyticsController analytics,
            RefreshController refresh, UserSession session, TaskRunner tasks, String databasePath,
            HomeController home) {
        this.accounts = accounts;
        this.tasks = tasks;
        this.refresh = refresh;
        this.session = session;
        root.getStyleClass().add("app-root");
        pages.getStyleClass().add("workspace-pages");
        pages.getTabs().addAll(
                new Tab("Marketplace",
                        new ListingTableView(listings, reviews, waitlists, tasks, refresh,
                                () -> root.getScene().getWindow(),
                                () -> pages.getSelectionModel().select(1))),
                new Tab("Sell an item",
                        new ListingFormView(listings, tasks,
                                () -> pages.getSelectionModel().selectFirst())),
                new Tab("Sales history",
                        new ImportView(analytics, tasks, refresh,
                                () -> root.getScene().getWindow())),
                new Tab("Analytics", new ChartsView(analytics, tasks)));
        pages.getTabs()
                .add(new Tab("My home",
                        new HomeView(home, tasks, session,
                                () -> pages.getSelectionModel().select(0),
                                () -> pages.getSelectionModel().select(1))));
        for (Tab tab : pages.getTabs()) {
            tab.setClosable(false);
        }
        workspace.setCenter(pages);
        workspace.getStyleClass().add("workspace");
        HBox header = new HBox(16, title);
        header.getStyleClass().add("page-header");
        workspace.setTop(header);
        footer(databasePath);
        bindNavigation(listings, analytics, home);
        accounts.readyProperty().addListener((o, before, ready) -> {
            if (ready && !accounts.initialAccounts().isEmpty()) {
                showInitialCredentials(List.copyOf(accounts.initialAccounts()));
                accounts.initialAccounts().clear();
            }
        });
        showAccount("login");
    }

    private HBox sidebar() {
        if (sidebar != null) {
            return sidebar;
        }
        HBox sidebar = new HBox(8, UiTools.label("Campus Marketplace", "top-brand"));
        sidebar.setAlignment(Pos.CENTER_LEFT);
        sidebar.getStyleClass().add("top-navigation");
        ToggleGroup group = new ToggleGroup();
        for (int i : new int[] { 4, 0, 1, 2, 3 }) {
            final int index = i;
            ToggleButton button = new ToggleButton(pages.getTabs().get(i).getText());
            button.setId("nav-" + i);
            button.setToggleGroup(group);
            button.setMinWidth(Region.USE_PREF_SIZE);
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("nav-button");
            button.setSelected(i == 0);
            button.setOnAction(event -> pages.getSelectionModel().select(index));
            pages.getSelectionModel().selectedIndexProperty()
                    .addListener((o, a, b) -> button.setSelected(b.intValue() == index));
            sidebar.getChildren().add(button);
        }
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        Label memberName = UiTools.label("", "account-name");
        memberName.setMaxWidth(110);
        memberName.setWrapText(false);
        memberName.textProperty()
                .bind(javafx.beans.binding.Bindings.createStringBinding(
                        () -> session.member() == null ? "" : session.member().getName(),
                        session.memberProperty()));
        sidebar.getChildren().addAll(space, memberName,
                UiTools.button("Sign out", "logout-button", tasks, accounts::logout));
        this.sidebar = sidebar;
        return sidebar;
    }

    private void showAccount(String page) {
        accounts.clearFeedback();
        Parent form = switch (page) {
        case "register" -> new RegistrationView(accounts, tasks, () -> showAccount("login"));
        case "reset" -> new PasswordResetView(accounts, tasks, () -> showAccount("login"));
        default -> new LoginView(accounts, tasks, () -> showAccount("register"),
                () -> showAccount("reset"));
        };
        VBox brand = new VBox(18,
                UiTools.label("Your campus.\nYour marketplace.", "login-hero-title"),
                UiTools.label(
                        "Find textbooks, electronics and furniture. Give your next great find a new owner.",
                        "login-hero-copy"),
                ItemArtwork.draw("TEXTBOOK"), ItemArtwork.draw("ELECTRONICS"));
        brand.getStyleClass().add("login-identity");
        brand.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        scroll.setMaxWidth(460);
        HBox layout = new HBox(40, brand, scroll);
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("login-layout");
        accountPane.getChildren().setAll(layout);
        accountPane.setAlignment(Pos.CENTER);
        root.setLeft(null);
        root.setTop(null);
        root.setCenter(accountPane);
    }

    private void showInitialCredentials(List<InitialAccount> accounts) {
        StringBuilder text = new StringBuilder(
                "Save these new local credentials now. They are shown once.\n\n");
        for (InitialAccount account : accounts) {
            text.append(account.username()).append("\nPassword: ")
                    .append(account.password().isEmpty() ? "(unchanged)" : account.password())
                    .append("\nRecovery code: ")
                    .append(account.recoveryCode().isEmpty() ? "(unchanged)"
                            : account.recoveryCode())
                    .append("\n\n");
        }
        TextArea details = new TextArea(text.toString());
        details.setEditable(false);
        details.setWrapText(true);
        details.setId("initial-credentials");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Save your classroom account details");
        dialog.getDialogPane().setContent(details);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        UiTools.dialog(dialog, root.getScene().getWindow(), refresh);
        details.clear();
    }

    /** Returns the root. */
    public Parent getRoot() {
        return root;
    }

    /**
     * Initializes the schema or account state while preserving existing records.
     */
    public void initialize() {
        accounts.initialize();
    }

    private void footer(String databasePath) {
        Label status = UiTools.label("", "status-text");
        status.textProperty().bind(tasks.statusProperty());
        Label errors = UiTools.label("", "error-text");
        errors.textProperty().bind(tasks.errorProperty());
        HBox footer = new HBox(15, status, UiTools.label("Database: " + databasePath, "muted"));
        footer.getStyleClass().add("status-bar");
        root.setBottom(new VBox(errors, footer));
    }

    private void bindNavigation(ListingController listings, AnalyticsController analytics,
            HomeController home) {
        pages.getSelectionModel().selectedIndexProperty().addListener((o, before, after) -> {
            int page = after.intValue();
            if (page < 0) {
                return;
            }
            title.setText(pages.getTabs().get(page).getText());
            refresh.setPage(page);
            if (page == 0) {
                listings.refresh();
            }
            if (page == 4) {
                home.load(false);
            }
            if (page == 3) {
                analytics.refresh();
            }
        });
        session.memberProperty().addListener((o, previous, member) -> {
            if (member == null) {
                showAccount("login");
                return;
            }
            root.setTop(sidebar());
            root.setCenter(workspace);
            pages.getSelectionModel().select(4);
            title.setText("My home");
            refresh.setPage(4);
        });
    }
}
