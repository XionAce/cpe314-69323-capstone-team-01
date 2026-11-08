package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.model.*;
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.api.ProductCatalogClient;
import edu.uc.campusmarket.persistence.Database.CategorySummary;
import edu.uc.campusmarket.persistence.Database.ListingForRepair;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.stage.Window;
import javafx.scene.chart.*;
import javafx.stage.FileChooser;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Login and marketplace screens share the same JavaFX scene and application service. */
public class MarketplaceView {
    private final MarketplaceService service;
    private final BorderPane root = new BorderPane();
    private final TableView<Listing> table = new TableView<>();
    private Member currentMember;
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label loginError = new Label();
    private final Button signIn = new Button("Sign in");
    private final Button signOut = new Button("Sign out");
    private final Label accountName = new Label();
    private final Label accountInitial = new Label();
    private final Label pageTitle = new Label("Marketplace");
    private final Label pageSubtitle = new Label("Explore what your campus has to offer.");
    private final Label listingCount = new Label("Loading listings");
    private final CheckBox myListings = new CheckBox("My listings");
    private final TilePane listingCards = new TilePane(16, 16);
    private final VBox loginFormArea = new VBox();
    private VBox sidebar;
    private final BorderPane workspace = new BorderPane();
    private final TabPane pages = new TabPane();
    private final ComboBox<Category> filterCategory = new ComboBox<>();
    private final TextField searchText = new TextField();
    private final TextField title = new TextField();
    private final TextField price = new TextField();
    private final ComboBox<String> type = new ComboBox<>();
    private final ComboBox<Category> postCategory = new ComboBox<>();
    private final ComboBox<String> observation = new ComboBox<>();
    private final TextArea details = new TextArea("Select a listing, then click View details.");
    private final Label status = new Label("Opening database...");
    private final Label databasePath = new Label();
    private boolean ready;
    private boolean busy;
    private boolean automaticRefreshRunning;
    private List<List<CategorySummary>> analyticsSnapshot = List.of();
    private final Timeline refreshTimer = new Timeline(
            new KeyFrame(Duration.seconds(3), event -> autoRefresh()));
    private final List<Button> workButtons = new ArrayList<>();
    private final Label catalogResult = new Label("Enter an electronics title, then request suggestions.");
    private final TextArea history = new TextArea();
    private final Label importProgress = new Label("Choose a JSON sales-history file.");
    private final Button cancelImport = new Button("Cancel import");
    private final AtomicBoolean cancelRequested = new AtomicBoolean();
    private final ComboBox<String> currency = new ComboBox<>();
    private final BarChart<String, Number> priceChart = chart("Average listing price by category", "Price (PHP)", false);
    private final BarChart<String, Number> ratingChart = chart("Average review rating by category", "Rating", true);
    private final Label ratingSummary = new Label("Refresh to load saved data.");
    private final ExecutorService worker = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "marketplace-database");
        thread.setDaemon(true);
        return thread;
    });

    public MarketplaceView(MarketplaceService service) {
        this.service = service;
        root.getStyleClass().add("app-root");
        pages.getStyleClass().add("workspace-pages");
        pages.getTabs().addAll(new Tab("Marketplace", createListings()),
                new Tab("Sell an item", createPostForm()), new Tab("Sales history", createImportPanel()),
                new Tab("Analytics", createAnalytics()));
        for (Tab tab : pages.getTabs()) tab.setClosable(false);
        pageTitle.getStyleClass().add("page-title");
        pageSubtitle.getStyleClass().add("muted");
        Button about = new Button("About this project");
        about.setId("about-button");
        about.getStyleClass().add("quiet");
        about.setOnAction(e -> message("Campus Marketplace with Reviews\nBrowse and post items, leave reviews, join waitlists, import sales history and explore category charts."));
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox header = new HBox(20, new VBox(5, pageTitle, pageSubtitle), space, about);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("page-header");
        workspace.setTop(header);
        workspace.setCenter(pages);
        workspace.getStyleClass().add("workspace");
        status.getStyleClass().add("status-text");
        databasePath.getStyleClass().add("muted");
        HBox footer = new HBox(15, status, databasePath);
        footer.getStyleClass().add("status-bar");
        root.setBottom(footer);
        signIn.setId("login-button");
        signIn.getStyleClass().add("primary");
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);
        signIn.setOnAction(event -> login());
        signOut.setId("logout-button");
        signOut.getStyleClass().add("sidebar-logout");
        signOut.setOnAction(event -> logout());
        workButtons.add(signIn);
        workButtons.add(signOut);
        sidebar = createSidebar();
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        showLogin();
    }

    private Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        return label;
    }

    private void showLogin() {
        username.setId("login-username");
        username.setPromptText("Your username");
        password.setId("login-password");
        password.setPromptText("Your password");
        password.setOnAction(event -> signIn.fire());
        loginError.setId("login-error");
        loginError.setWrapText(true);
        loginError.getStyleClass().add("login-error");
        VBox identity = new VBox(20, label("campus marketplace", "login-brand"),
                label("Find it.\nMake it yours.", "login-headline"),
                label("Textbooks, tech and furniture. Buy and sell with your campus community.", "login-description"));
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        HBox preview = new HBox(12, categoryPreview("TEXTBOOK", "Textbooks"), categoryPreview("ELECTRONICS", "Electronics"));
        identity.getChildren().add(preview);
        HBox categories = new HBox(8, label("Browse", "login-tag"), label("Sell", "login-tag"), label("Review", "login-tag"));
        identity.getChildren().addAll(gap, categories, label("Campus Marketplace with Reviews", "login-caption"));
        identity.getStyleClass().add("login-identity");
        identity.setPrefWidth(530);
        HBox.setHgrow(identity, Priority.ALWAYS);
        Button forgot = new Button("Forgot password?");
        forgot.setId("forgot-password");
        forgot.getStyleClass().add("text-button");
        forgot.setOnAction(event -> { if (!busy && ready) showPasswordReset(); });
        VBox form = new VBox(12, label("Welcome back", "page-title"),
                label("Sign in to browse, sell and review campus finds.", "muted"),
                label("Username", "field-label"), username, label("Password", "field-label"), password,
                forgot, loginError, signIn, label("Class accounts: person_a, person_b, person_c", "muted"));
        form.getStyleClass().add("login-form");
        form.setMaxWidth(390);
        loginFormArea.getChildren().setAll(form);
        loginFormArea.setAlignment(Pos.CENTER);
        loginFormArea.setPadding(new Insets(32));
        loginFormArea.setPrefWidth(530);
        HBox.setHgrow(loginFormArea, Priority.ALWAYS);
        root.setLeft(null);
        root.setCenter(new HBox(identity, loginFormArea));
        Platform.runLater(username::requestFocus);
    }

    private VBox categoryPreview(String type, String name) {
        VBox preview = new VBox(6, itemIllustration(type), label(name, "preview-label"));
        preview.getStyleClass().addAll("category-preview", "type-" + type.toLowerCase());
        return preview;
    }

    private void showPasswordReset() {
        password.clear();
        signIn.setDefaultButton(false);
        TextField account = new TextField(username.getText());
        account.setId("reset-username");
        account.setPromptText("Your username");
        PasswordField code = new PasswordField();
        code.setId("reset-code");
        code.setPromptText("Your saved recovery code");
        PasswordField replacement = new PasswordField();
        replacement.setId("reset-password");
        replacement.setPromptText("8 to 128 characters");
        PasswordField confirmation = new PasswordField();
        confirmation.setId("reset-confirmation");
        confirmation.setPromptText("Enter your new password again");
        loginError.setText("");
        Button reset = new Button("Reset password");
        reset.setId("reset-save");
        reset.getStyleClass().add("primary");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setDefaultButton(true);
        reset.disableProperty().bind(signIn.disableProperty());
        Button back = new Button("Back to sign in");
        back.setId("reset-back");
        back.getStyleClass().add("text-button");
        back.disableProperty().bind(signIn.disableProperty());
        back.setOnAction(event -> { if (!busy) { reset.setDefaultButton(false); signIn.setDefaultButton(true); showLogin(); } });
        VBox form = new VBox(10, label("Reset your password", "page-title"),
                label("Verify your account with its recovery code. A successful reset gives you a new code to save.", "muted"),
                label("Username", "field-label"), account, label("Recovery code", "field-label"), code,
                label("New password", "field-label"), replacement, label("Confirm password", "field-label"), confirmation,
                loginError, reset, back);
        form.getStyleClass().add("login-form");
        form.setMaxWidth(400);
        reset.setOnAction(event -> {
            if (busy) return;
            if (!replacement.getText().equals(confirmation.getText())) {
                loginError.setText("The two passwords must match."); return;
            }
            String name = account.getText();
            char[] secret = replacement.getText().toCharArray();
            char[] recovery = code.getText().toCharArray();
            replacement.clear(); confirmation.clear(); code.clear(); loginError.setText("");
            runTask(() -> {
                try { return service.resetPassword(name, recovery, secret); }
                finally { java.util.Arrays.fill(secret, '\0'); java.util.Arrays.fill(recovery, '\0'); }
            }, nextCode -> {
                reset.setDefaultButton(false);
                TextField savedCode = new TextField(nextCode);
                savedCode.setId("new-recovery-code");
                savedCode.setEditable(false);
                form.getChildren().setAll(label("Password updated", "page-title"),
                        label("Save this new recovery code. You will need it if you forget your password again.", "muted"), savedCode, back);
            });
        });
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setMaxWidth(420);
        scroll.setMaxHeight(720);
        scroll.getStyleClass().add("page-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        loginFormArea.getChildren().setAll(scroll);
    }

    private Canvas itemIllustration(String type) {
        Canvas canvas = new Canvas(170, 100);
        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#d9d4ed"));
        g.fillOval(24, 75, 125, 13);
        if (type.equals("TEXTBOOK")) {
            g.setFill(Color.web("#f0b875")); g.fillRoundRect(39, 16, 73, 65, 6, 6);
            g.setFill(Color.web("#7962ba")); g.fillRoundRect(57, 8, 73, 72, 6, 6);
            g.setFill(Color.web("#ebe4ff")); g.fillRect(66, 8, 4, 72);
            g.setFill(Color.WHITE); g.fillRoundRect(80, 24, 36, 5, 3, 3); g.fillRoundRect(80, 36, 23, 4, 3, 3);
        } else if (type.equals("ELECTRONICS")) {
            g.setFill(Color.web("#49445f")); g.fillRoundRect(35, 10, 103, 66, 7, 7);
            g.setFill(Color.web("#b5c4f0")); g.fillRoundRect(41, 16, 91, 53, 3, 3);
            g.setFill(Color.web("#e8edff")); g.fillOval(71, 23, 34, 34);
            g.setFill(Color.web("#8b87a8")); g.fillRoundRect(24, 76, 126, 8, 5, 5);
        } else {
            g.setFill(Color.web("#d6a069")); g.fillRoundRect(56, 8, 61, 43, 12, 12);
            g.setFill(Color.web("#8e6851")); g.fillRoundRect(49, 48, 75, 12, 5, 5);
            g.fillRect(55, 59, 7, 24); g.fillRect(111, 59, 7, 24);
        }
        return canvas;
    }

    private void login() {
        if (busy) return;
        if (!ready) { initialize(); return; }
        String name = username.getText();
        char[] secret = password.getText().toCharArray();
        password.clear();
        loginError.setText("");
        runTask(() -> {
            try { return service.login(name, secret); }
            finally { java.util.Arrays.fill(secret, '\0'); }
        }, member -> {
            currentMember = member;
            signIn.setDefaultButton(false);
            username.clear();
            accountName.setText(member.getName());
            accountInitial.setText(member.getName().substring(member.getName().length() - 1));
            root.setLeft(sidebar);
            root.setCenter(workspace);
            pages.getSelectionModel().selectFirst();
            pageTitle.setText("Marketplace");
            pageSubtitle.setText("Explore what your campus has to offer.");
            refresh();
            refreshTimer.playFromStart();
        });
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(9, label("Campus\nMarketplace", "sidebar-brand"), label("EXPLORE", "sidebar-caption"));
        sidebar.getStyleClass().add("sidebar");
        ToggleGroup navigation = new ToggleGroup();
        String[] subtitles = {"Explore what your campus has to offer.", "Give your next great find a new owner.",
                "Bring previous sales into your local records.", "Understand campus prices and reviews."};
        for (int i = 0; i < pages.getTabs().size(); i++) {
            final int index = i;
            ToggleButton item = new ToggleButton(pages.getTabs().get(i).getText());
            item.setId("nav-" + i);
            item.getStyleClass().add("nav-button");
            item.setToggleGroup(navigation);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setSelected(i == 0);
            item.setOnAction(event -> { item.setSelected(true); pages.getSelectionModel().select(index); });
            pages.getSelectionModel().selectedIndexProperty().addListener((observable, before, after) -> {
                item.setSelected(after.intValue() == index);
                pageTitle.setText(pages.getTabs().get(after.intValue()).getText());
                pageSubtitle.setText(subtitles[after.intValue()]);
            });
            sidebar.getChildren().add(item);
        }
        pageTitle.setText("Marketplace");
        pageSubtitle.setText(subtitles[0]);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        accountInitial.getStyleClass().setAll("avatar");
        accountName.getStyleClass().setAll("account-name");
        HBox account = new HBox(10, accountInitial, new VBox(4, accountName, label("Campus member", "sidebar-caption")));
        account.setAlignment(Pos.CENTER_LEFT);
        sidebar.getChildren().addAll(spacer, account, signOut);
        return sidebar;
    }

    private void logout() {
        if (busy) return;
        refreshTimer.stop();
        currentMember = null;
        table.getItems().clear();
        listingCards.getChildren().clear();
        details.clear();
        myListings.setSelected(false);
        searchText.clear();
        filterCategory.getSelectionModel().selectFirst();
        type.getSelectionModel().selectFirst();
        chooseObservations();
        title.clear();
        price.clear();
        history.clear();
        catalogResult.setText("Enter an electronics title, then request suggestions.");
        importProgress.setText("Choose a JSON sales-history file.");
        priceChart.getData().clear();
        ratingChart.getData().clear();
        analyticsSnapshot = List.of();
        ratingSummary.setText("Refresh to load saved data.");
        loginError.setText("");
        signIn.setDefaultButton(true);
        showLogin();
    }

    private VBox createListings() {
        filterCategory.setId("category-selector");
        filterCategory.setMaxWidth(Double.MAX_VALUE);
        searchText.setId("search-box");
        searchText.setPromptText("Search listing titles");
        Button search = new Button("Search");
        search.getStyleClass().add("primary");
        search.setId("search-button");
        workButtons.add(search);
        // This event handler runs when the user clicks Search.
        search.setOnAction(event -> refresh());
        searchText.setOnAction(event -> refresh());
        HBox searchRow = new HBox(10, searchText, filterCategory, search);
        filterCategory.setPrefWidth(220);
        filterCategory.setMinWidth(160);
        HBox.setHgrow(searchText, Priority.ALWAYS);
        table.setId("listing-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No matching listings."));
        table.getSelectionModel().selectedItemProperty().addListener((observable, before, after) ->
                details.setText("Select a listing, then click View details."));
        addColumn("Listing", 190, item -> item.getTitle());
        addColumn("Type", 100, item -> item.getType().substring(0, 1) + item.getType().substring(1).toLowerCase());
        addColumn("Price", 105, item -> item.getCurrency() + " " + item.getPrice());
        // A Listing reference runs the actual Textbook/Electronics/Furniture method.
        addColumn("Condition", 100, item -> item.gradeCondition().toString());
        addColumn("Status", 95, item -> item.getStatus().equals("AVAILABLE") ? "Available" : "Withdrawn");
        table.setRowFactory(view -> {
            TableRow<Listing> row = new TableRow<>();
            row.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && !row.isEmpty() && !busy) viewDetails(); });
            return row;
        });
        Button view = new Button("View details");
        view.setId("view-button");
        workButtons.add(view);
        view.setOnAction(event -> viewDetails());
        Button join = new Button("Join waitlist");
        join.setId("join-button");
        workButtons.add(join);
        join.setOnAction(event -> joinWaitlist());
        details.setId("listing-details");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(4);
        details.setMinHeight(90);
        Button review = new Button("Review listing");
        review.setId("review-button");
        review.setOnAction(e -> reviewListing());
        workButtons.add(review);
        Button restore = new Button("Restore missing details");
        restore.setId("restore-details-button");
        restore.setOnAction(event -> restoreMissingDetails());
        workButtons.add(restore);
        Button withdraw = new Button("Withdraw my listing");
        withdraw.setId("withdraw-button");
        withdraw.getStyleClass().add("quiet");
        withdraw.setOnAction(event -> {
            Listing item = table.getSelectionModel().getSelectedItem();
            if (item == null || busy || currentMember == null) { message("Select one of your listings first."); return; }
            long memberId = currentMember.getId();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Withdraw " + item.getTitle() + " from the marketplace?", ButtonType.OK, ButtonType.CANCEL);
            confirm.initOwner(root.getScene().getWindow());
            confirm.setHeaderText("Withdraw listing");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                runTask(() -> { service.withdraw(memberId, item.getId()); return true; }, saved -> refresh());
            }
        });
        workButtons.add(withdraw);
        myListings.setId("my-listings");
        myListings.setOnAction(event -> refresh());
        listingCount.getStyleClass().add("muted");
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        ToggleButton cards = new ToggleButton("Cards");
        cards.setId("cards-mode-button");
        ToggleButton rows = new ToggleButton("List");
        rows.setId("list-mode-button");
        ToggleGroup viewMode = new ToggleGroup();
        cards.setToggleGroup(viewMode); rows.setToggleGroup(viewMode); cards.setSelected(true);
        HBox heading = new HBox(12, label("Available finds", "section-title"), listingCount, space, myListings, cards, rows);
        heading.setAlignment(Pos.CENTER_LEFT);
        FlowPane actions = new FlowPane(8, 8, view, join, review, restore, withdraw);
        VBox detailCard = new VBox(8, label("Selected item", "section-title"), actions, details);
        detailCard.getStyleClass().add("card");
        detailCard.getStyleClass().add("detail-panel");
        details.setPrefRowCount(2);
        details.setMinHeight(58);
        ScrollPane cardScroll = new ScrollPane(listingCards);
        cardScroll.setId("listing-cards");
        cardScroll.setFitToWidth(true);
        cardScroll.getStyleClass().add("page-scroll");
        listingCards.setPadding(new Insets(2, 3, 4, 2));
        listingCards.prefTileWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(180, (cardScroll.getViewportBounds().getWidth() - 38)
                        / (cardScroll.getViewportBounds().getWidth() >= 760 ? 3 : 2)), cardScroll.viewportBoundsProperty()));
        table.setVisible(false); table.setManaged(false);
        cards.setOnAction(event -> { cards.setSelected(true); table.setVisible(false); table.setManaged(false); cardScroll.setVisible(true); cardScroll.setManaged(true); });
        rows.setOnAction(event -> { rows.setSelected(true); table.setVisible(true); table.setManaged(true); cardScroll.setVisible(false); cardScroll.setManaged(false); });
        StackPane browser = new StackPane(table, cardScroll);
        browser.setMinHeight(150);
        Button sell = new Button("+  Sell an item");
        sell.getStyleClass().add("hero-button");
        sell.setOnAction(event -> pages.getSelectionModel().select(1));
        Region heroSpace = new Region(); HBox.setHgrow(heroSpace, Priority.ALWAYS);
        HBox hero = new HBox(18, new VBox(7, label("Find your next campus essential.", "hero-title"),
                label("Browse textbooks, electronics and furniture.", "hero-description")), heroSpace, sell);
        hero.setAlignment(Pos.CENTER_LEFT);hero.getStyleClass().add("market-hero");
        hero.visibleProperty().bind(workspace.heightProperty().greaterThanOrEqualTo(690));
        hero.managedProperty().bind(hero.visibleProperty());
        VBox panel = new VBox(14, hero, searchRow, heading, browser, detailCard);
        panel.getStyleClass().add("page-content");
        VBox.setVgrow(browser, Priority.ALWAYS);
        return panel;
    }

    private ScrollPane createPostForm() {
        Label heading = new Label("Post a listing");
        heading.getStyleClass().add("section-title");
        title.setId("post-title");
        price.setId("post-price");
        price.setPromptText("e.g. 250.00");
        type.setId("post-type");
        type.getItems().addAll("TEXTBOOK", "ELECTRONICS", "FURNITURE");
        postCategory.setId("post-category");
        observation.setId("post-condition");
        type.setOnAction(event -> chooseObservations());
        type.getSelectionModel().selectFirst();
        postCategory.setMaxWidth(Double.MAX_VALUE);
        type.setMaxWidth(Double.MAX_VALUE);
        observation.setMaxWidth(Double.MAX_VALUE);
        Button post = new Button("Post listing");
        post.setId("post-save");
        workButtons.add(post);
        post.getStyleClass().add("primary");
        // This is the main Week 9 demonstration: click -> save -> reload the table.
        post.setOnAction(event -> postListing());
        Label note = new Label("Choose the condition you observed. Your item receives its condition grade automatically.");
        note.setWrapText(true);
        note.getStyleClass().add("muted");
        Button suggest = new Button("Suggest category / price");
        suggest.setId("suggest-button");
        suggest.setOnAction(e -> suggest());
        workButtons.add(suggest);
        catalogResult.setWrapText(true);
        catalogResult.setId("catalog-result");
        title.setPromptText("e.g. Java textbook, desk chair or laptop");
        post.setMaxWidth(Double.MAX_VALUE);
        VBox form = new VBox(10, heading, new Label("Title"), title, new Label("Price (PHP)"), price,
                new Label("Type"), type, new Label("Category"), postCategory,
                new Label("Condition observation"), observation, note, post);
        form.getStyleClass().add("card");
        HBox.setHgrow(form, Priority.ALWAYS);
        form.setMaxWidth(620);
        VBox suggestions = new VBox(16, label("Need a price reference?", "section-title"),
                label("Enter an electronics title and get matching products from the public catalog.", "muted"), suggest, catalogResult);
        suggestions.setPrefWidth(285);
        suggestions.setMinWidth(245);
        suggestions.getStyleClass().addAll("card", "suggestion-card");
        HBox content = new HBox(22, form, suggestions);
        content.getStyleClass().add("page-content");
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("page-scroll");
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void chooseObservations() {
        long defaultCategory;
        observation.getItems().setAll(observationsFor(type.getValue()));
        if (type.getValue().equals("TEXTBOOK")) {
            defaultCategory = 2;
        } else if (type.getValue().equals("ELECTRONICS")) {
            defaultCategory = 3;
        } else {
            defaultCategory = 4;
        }
        observation.getSelectionModel().selectFirst();
        for (Category category : postCategory.getItems()) {
            if (category.getId() == defaultCategory) postCategory.setValue(category);
        }
    }

    private List<String> observationsFor(String itemType) {
        if (itemType.equals("TEXTBOOK")) return List.of("No visible damage", "Light markings",
                "Heavy markings", "Damaged cover", "Missing pages");
        if (itemType.equals("ELECTRONICS")) return List.of("No visible damage", "Cosmetic damage",
                "Functional defect", "Does not power on");
        return List.of("No visible damage", "Light wear", "Heavy wear", "Structural damage", "Unstable");
    }

    public void initialize() {
        // Load members first so an owner can repair a damaged listing even if search fails.
        runTask(() -> { service.initialize(); return true; }, initialized -> {
            filterCategory.getItems().setAll(service.getCategories());
            filterCategory.getSelectionModel().selectFirst();
            for (Category category : service.getCategories()) {
                if (category.getParentId() != null) postCategory.getItems().add(category);
            }
            chooseObservations();
            databasePath.setText("Database: " + service.databasePath());
            ready = true;
            status.setText("Ready to sign in.");
            loginError.setText("");
        });
    }

    private void refresh() {
        if (!ready || busy || currentMember == null) return;
        long categoryId = filterCategory.getValue().getId();
        String keyword = searchText.getText();
        long memberId = currentMember.getId();
        boolean owned = myListings.isSelected();
        runTask(() -> owned ? service.myListings(memberId) : service.search(categoryId, keyword), this::showListings);
    }

    // JavaFX checks every three seconds. The existing worker performs the database reads.
    private void autoRefresh() {
        if (!ready || busy || automaticRefreshRunning || currentMember == null || showingDialog()) return;
        int page = pages.getSelectionModel().getSelectedIndex();
        if (page == 0) {
            long categoryId = filterCategory.getValue().getId();
            String keyword = searchText.getText();
            boolean owned = myListings.isSelected();
            long memberId = currentMember.getId();
            runAutoRefresh(() -> owned ? service.myListings(memberId) : service.search(categoryId, keyword), listings -> {
                if (pages.getSelectionModel().getSelectedIndex() != page
                        || filterCategory.getValue().getId() != categoryId
                        || !searchText.getText().equals(keyword) || myListings.isSelected() != owned) return;
                if (!table.getItems().stream().map(this::listingValues).toList()
                        .equals(listings.stream().map(this::listingValues).toList())) showListings(listings);
            });
        } else if (page == 3) {
            String selectedCurrency = currency.getEditor().getText().trim().toUpperCase(java.util.Locale.ROOT);
            if (!selectedCurrency.matches("[A-Z]{3}")) return;
            runAutoRefresh(() -> List.of(service.prices(selectedCurrency), service.ratings()), rows -> {
                if (pages.getSelectionModel().getSelectedIndex() == page
                        && currency.getEditor().getText().trim().equalsIgnoreCase(selectedCurrency)) {
                    showAnalytics(rows, selectedCurrency);
                }
            });
        }
    }

    private List<?> listingValues(Listing item) {
        return List.of(item.getId(), item.getTitle(), item.getType(), item.getPrice(),
                item.getCurrency(), item.getStatus(), item.gradeCondition());
    }

    private boolean showingDialog() {
        return Window.getWindows().stream().anyMatch(window -> window.isShowing()
                && window.getScene() != null && window.getScene().getRoot() instanceof DialogPane);
    }

    private <T> void runAutoRefresh(Callable<T> work, Consumer<T> onSuccess) {
        automaticRefreshRunning = true;
        Member member = currentMember;
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> {
            automaticRefreshRunning = false;
            if (ready && !busy && currentMember == member && !showingDialog()) onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            automaticRefreshRunning = false;
            if (ready && !busy && currentMember == member) {
                status.setText("Automatic refresh failed. Click Search or Refresh analytics to retry.");
            }
        });
        worker.execute(task);
    }

    private void postListing() {
        if (!ready || busy || currentMember == null) return;
        try {
            // Read controls on the UI thread before starting background database work.
            long memberId = currentMember.getId();
            long categoryId = postCategory.getValue().getId();
            String itemTitle = title.getText();
            BigDecimal amount = new BigDecimal(price.getText().trim());
            String itemType = type.getValue();
            String evidence = observation.getValue();
            runTask(() -> {
                long sellerId = service.sellerFor(memberId);
                Listing item = makeListing(0, sellerId, categoryId, itemTitle, amount,
                        "PHP", "AVAILABLE", itemType, evidence);
                service.post(memberId, item);
                return service.search(1, ""); // SELECT the saved data again.
            }, listings -> {
                myListings.setSelected(false);
                filterCategory.getSelectionModel().selectFirst();
                searchText.clear();
                showListings(listings);
                table.getSelectionModel().selectLast();
                title.clear();
                price.clear();
                status.setText("Listing saved. " + listings.size() + " listing(s).");
                pages.getSelectionModel().selectFirst();
            });
        } catch (NumberFormatException error) {
            message("Enter a numeric price, such as 250.00.");
        }
    }

    private Listing makeListing(long id, long seller, long category, String title, BigDecimal price,
                                String currency, String status, String type, String evidence) {
        if (type.equals("TEXTBOOK")) {
            WearLevel markings = WearLevel.NONE;
            if (evidence.equals("Light markings")) markings = WearLevel.LIGHT;
            if (evidence.equals("Heavy markings")) markings = WearLevel.HEAVY;
            // One missing page represents the chosen observation in this skeleton form.
            return new Textbook(id, seller, category, title, price, currency, status,
                    evidence.equals("Missing pages") ? 1 : 0, markings, evidence.equals("Damaged cover"));
        }
        if (type.equals("ELECTRONICS")) {
            return new Electronics(id, seller, category, title, price, currency, status,
                    !evidence.equals("Does not power on"), evidence.equals("Functional defect"), evidence.equals("Cosmetic damage"));
        }
        WearLevel wear = WearLevel.NONE;
        if (evidence.equals("Light wear")) wear = WearLevel.LIGHT;
        if (evidence.equals("Heavy wear")) wear = WearLevel.HEAVY;
        return new Furniture(id, seller, category, title, price, currency, status,
                !evidence.equals("Unstable"), evidence.equals("Structural damage"), wear);
    }

    private void restoreMissingDetails() {
        if (!ready || busy || currentMember == null) return;
        long memberId = currentMember.getId();
        runTask(() -> service.listingsForRepair(memberId), candidates -> {
            if (candidates.isEmpty()) { message("This member has no listings with missing details."); return; }
            ChoiceDialog<ListingForRepair> choose = new ChoiceDialog<>(candidates.getFirst(), candidates);
            choose.setTitle("Restore missing details");
            choose.setHeaderText("Choose one of your listings. Its other saved data will stay the same.");
            choose.initOwner(root.getScene().getWindow());
            var selection = choose.showAndWait();
            if (selection.isEmpty()) return;
            ListingForRepair item = selection.get();
            ChoiceDialog<String> condition = new ChoiceDialog<>(null, observationsFor(item.type()));
            condition.setSelectedItem(null); // Require an observation; never assume an undamaged item.
            condition.setTitle("Confirm condition");
            condition.setHeaderText(item.title() + " - select the condition you actually observed.");
            condition.initOwner(root.getScene().getWindow());
            condition.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                    .bind(condition.selectedItemProperty().isNull());
            var evidence = condition.showAndWait();
            if (evidence.isEmpty()) return;
            Listing observed = makeListing(item.id(), item.sellerId(), item.categoryId(), item.title(),
                    item.price(), item.currency(), item.status(), item.type(), evidence.get());
            runTask(() -> { service.restoreDetails(memberId, observed); return item.id(); }, id -> {
                details.setText("Condition details restored for listing #" + id + ".");
                refresh();
            });
        });
    }

    private void showListings(List<Listing> listings) {
        Listing selected = table.getSelectionModel().getSelectedItem();
        String previousDetails = details.getText();
        table.getItems().setAll(listings);
        status.setText(listings.size() + " listing(s) loaded.");
        listingCount.setText(listings.size() + " items");
        if (!listings.isEmpty()) table.getSelectionModel().selectFirst();
        if (selected != null) {
            for (Listing item : listings) {
                if (item.getId() == selected.getId()) {
                    table.getSelectionModel().select(item);
                    if (listingValues(item).equals(listingValues(selected))) details.setText(previousDetails);
                    break;
                }
            }
        }
        showListingCards(listings);
    }

    private void showListingCards(List<Listing> listings) {
        listingCards.getChildren().clear();
        if (listings.isEmpty()) {
            listingCards.getChildren().add(label("No items match your search. Try another title or category.", "muted"));
            return;
        }
        for (Listing item : listings) {
            StackPane illustration = new StackPane(itemIllustration(item.getType()));
            illustration.getStyleClass().addAll("item-art", "type-" + item.getType().toLowerCase());
            Label name = label(item.getTitle().replace(" (demo)", ""), "item-name");
            name.setMinHeight(35);
            Label grade = label(item.getStatus().equals("WITHDRAWN") ? "Withdrawn" : item.gradeCondition().toString().replace('_', ' '), "condition-badge");
            grade.getStyleClass().add("grade-" + item.gradeCondition().toString().toLowerCase());
            Label amount = label(item.getCurrency() + " " + String.format(java.util.Locale.ROOT, "%,.2f", item.getPrice()), "item-price");
            Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
            HBox footer = new HBox(8, amount, gap, grade); footer.setAlignment(Pos.CENTER_LEFT);
            VBox info = new VBox(5, label(item.getType().substring(0, 1) + item.getType().substring(1).toLowerCase(), "item-type"), name, footer);
            info.getStyleClass().add("item-info");
            VBox card = new VBox(illustration, info);
            card.getStyleClass().add("item-card");
            card.setMaxWidth(Double.MAX_VALUE);
            card.setUserData(item.getId());
            card.setOnMouseClicked(event -> {
                table.getSelectionModel().select(item);
                for (var node : listingCards.getChildren()) node.getStyleClass().remove("selected-card");
                card.getStyleClass().add("selected-card");
                if (!busy) viewDetails();
            });
            Listing selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getId() == item.getId()) card.getStyleClass().add("selected-card");
            listingCards.getChildren().add(card);
        }
    }

    private void viewDetails() {
        Listing item = table.getSelectionModel().getSelectedItem();
        if (item == null) { message("Choose a listing first."); return; }
        long memberId = currentMember.getId();
        runTask(() -> {
            service.view(memberId, item.getId());
            String text = item.getTitle() + " | " + item.getCurrency() + " " + item.getPrice()
                    + "\nCondition: " + item.gradeCondition() + "\nReviews:\n" + service.reviews(item.getId())
                    + "\nWaitlist (first buyer first): " + buyerNames(service.getWaitlist(item.getId()))
                    + "\nRecently viewed (newest first):";
            List<Listing> recent = service.recent(memberId);
            for (int i = 0; i < Math.min(3, recent.size()); i++) text += "\n- " + recent.get(i).getTitle();
            return text;
        }, text -> details.setText(text));
    }

    private void joinWaitlist() {
        Listing item = table.getSelectionModel().getSelectedItem();
        if (item == null) { message("Choose a listing first."); return; }
        long memberId = currentMember.getId();
        runTask(() -> buyerNames(service.joinWaitlist(item.getId(), memberId)),
                names -> details.setText(item.getTitle() + "\nWaitlist (first buyer first): " + names));
    }

    private String buyerNames(List<Long> ids) {
        String names = "";
        for (long id : ids) {
            for (Member member : service.getMembers()) {
                if (member.getId() == id) {
                    if (!names.isEmpty()) names += " -> ";
                    names += member.getName();
                }
            }
        }
        return names.isEmpty() ? "No buyers yet." : names;
    }

    private void addColumn(String heading, double width, Function<Listing, String> text) {
        TableColumn<Listing, String> column = new TableColumn<>(heading);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        if (heading.equals("Condition")) {
            column.setCellFactory(view -> new TableCell<>() {
                @Override protected void updateItem(String value, boolean empty) {
                    super.updateItem(value, empty);
                    setText(null);
                    if (empty || value == null) { setGraphic(null); return; }
                    Label badge = label(value.replace('_', ' '), "condition-badge");
                    badge.getStyleClass().add("grade-" + value.toLowerCase());
                    setGraphic(badge);
                }
            });
        }
        table.getColumns().add(column);
    }

    // Shared plumbing: JDBC runs on one worker, and JavaFX invokes the result handlers.
    private <T> void runTask(Callable<T> work, Consumer<T> onSuccess) {
        if (busy) return;
        setBusy(true);
        status.setText("Working...");
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> { setBusy(false); status.setText("Ready."); onSuccess.accept(task.getValue()); });
        task.setOnFailed(event -> { setBusy(false); status.setText("Action could not be completed.");
            if (currentMember == null) {
                loginError.setText(task.getException().getMessage());
                return;
            }
            importProgress.setText("Operation ended; any unfinished import was rolled back."); if (catalogResult.getText().equals("Requesting catalog...")) catalogResult.setText("Request failed. Retry or enter manually.");
            message(task.getException().getMessage()); });
        worker.execute(task);
    }

    private void setBusy(boolean value) {
        busy = value;
        for (Button button : workButtons) button.setDisable(value);
        cancelImport.setDisable(true);
    }

    private void suggest() {
        if (!ready || busy || currentMember == null) return;
        String query = title.getText();
        String selectedType = type.getValue();
        if (query.isBlank()) { message("Enter a title first."); return; }
        // A fresh request is made on every click. No cached/fake fallback is shown.
        catalogResult.setText("Requesting catalog...");
        runTask(() -> new ProductCatalogClient().fetch(), response -> {
            var matches = ProductCatalogClient.matches(response.products(), query, selectedType);
            if (!title.getText().equals(query) || !type.getValue().equals(selectedType)) {
                catalogResult.setText(response.requestInfo() + "\nForm changed; request suggestions again."); return;
            }
            String text = response.requestInfo() + "\n";
            if (matches.isEmpty()) text += "No comparable product found. Enter category/price manually.";
            else {
                for (Category category : postCategory.getItems())
                    if (category.getName().equals("Electronics")) postCategory.setValue(category);
                var min = matches.stream().map(p -> p.price()).min(BigDecimal::compareTo).orElseThrow();
                var max = matches.stream().map(p -> p.price()).max(BigDecimal::compareTo).orElseThrow();
                text += "Catalog reference: " + min + " - " + max + " (currency unspecified).\n";
                for (var match : matches) text += match.title() + "\n";
                text += "Choose your final PHP price manually.";
            }
            catalogResult.setText(text);
        });
    }

    private VBox createImportPanel() {
        Button choose = new Button("Import JSON sales history");
        choose.setId("import-button"); workButtons.add(choose);
        choose.setOnAction(e -> {
            if (!ready || busy || currentMember == null) return;
            FileChooser picker = new FileChooser();
            picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
            var file = picker.showOpenDialog(root.getScene().getWindow());
            if (file == null) return;
            startImport(file.toPath());
        });
        cancelImport.setDisable(true); cancelImport.setId("cancel-import");
        cancelImport.setOnAction(e -> { cancelRequested.set(true); cancelImport.setDisable(true);
            importProgress.setText("Cancelling; waiting for rollback..."); });
        history.setEditable(false); history.setWrapText(true); history.setId("history-results");
        VBox box = new VBox(16, label("Your sales records", "section-title"), label("Choose a JSON file to import previous sales into your local history.", "muted"),
                new HBox(8, choose, cancelImport), importProgress, history);
        VBox.setVgrow(history, Priority.ALWAYS); box.getStyleClass().addAll("page-content", "import-page");
        return box;
    }

    private void startImport(java.nio.file.Path file) {
        if (!ready || busy || currentMember == null) return;
            cancelRequested.set(false);
            history.clear();
            runTask(() -> {
                var result = service.importer().importFile(file, cancelRequested::get, count -> {
                    if (count == 1 || count % 1000 == 0) Platform.runLater(() ->
                            importProgress.setText("Processed " + count + " records (not committed yet)."));
                });
                String summary = "Saved: " + result.saved() + ", invalid: " + result.invalid() + ", duplicates: "
                        + result.duplicates() + "\n" + String.join("\n", result.errors());
                // Import is already committed. A preview error must not imply a rollback.
                try { return summary + "\n" + service.historyPreview(); }
                catch (java.sql.SQLException error) { return summary + "\nSaved successfully; preview unavailable: " + error.getMessage(); }
            }, result -> { importProgress.setText("Import committed."); history.setText(result); });
            cancelImport.setDisable(false);
    }

    private VBox createAnalytics() {
        currency.getItems().setAll("PHP", "USD", "EUR"); currency.setValue("PHP");
        currency.setEditable(true); currency.setId("analytics-currency");
        Button refresh = new Button("Refresh analytics"); refresh.setId("analytics-button"); workButtons.add(refresh);
        refresh.setOnAction(e -> {
            if (!ready || busy || currentMember == null) return;
            String selectedCurrency = currency.getEditor().getText().trim().toUpperCase(java.util.Locale.ROOT);
            if (!selectedCurrency.matches("[A-Z]{3}")) { message("Enter a three-letter currency."); return; }
            runTask(() -> List.of(service.prices(selectedCurrency), service.ratings()),
                    rows -> showAnalytics(rows, selectedCurrency));
        });
        VBox box = new VBox(8, new HBox(8, new Label("Price currency:"), currency, refresh), priceChart, ratingChart, ratingSummary);
        VBox.setVgrow(priceChart, Priority.ALWAYS); VBox.setVgrow(ratingChart, Priority.ALWAYS);
        box.getStyleClass().add("page-content"); return box;
    }

    private void showAnalytics(List<List<CategorySummary>> rows, String selectedCurrency) {
        String axis = "Price (" + selectedCurrency + ")";
        if (rows.equals(analyticsSnapshot) && priceChart.getYAxis().getLabel().equals(axis)) return;
        analyticsSnapshot = rows;
        priceChart.getYAxis().setLabel(axis);
        fillChart(priceChart, rows.get(0));
        fillChart(ratingChart, rows.get(1));
        if (rows.get(1).isEmpty()) {
            ratingSummary.setText("No campus reviews yet. Categories without reviews are unrated.");
        } else {
            double best = rows.get(1).getFirst().average();
            String text = "Best-rated: ";
            for (var row : rows.get(1)) if (Double.compare(row.average(), best) == 0)
                text += row.category() + " " + String.format(java.util.Locale.ROOT, "%.2f", best)
                        + "/5 (" + row.count() + " reviews)  ";
            ratingSummary.setText(text);
        }
    }

    private static BarChart<String, Number> chart(String title, String yLabel, boolean rating) {
        NumberAxis y = rating ? new NumberAxis(0, 5, 1) : new NumberAxis();
        y.setLabel(yLabel); CategoryAxis x = new CategoryAxis(); x.setLabel("Category");
        BarChart<String, Number> chart = new BarChart<>(x, y); chart.setTitle(title);
        chart.setLegendVisible(false); chart.setAnimated(false); chart.setMinHeight(180);
        return chart;
    }

    private void fillChart(BarChart<String, Number> chart, List<CategorySummary> rows) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (var row : rows) series.getData().add(new XYChart.Data<>(row.category(), row.average()));
        chart.getData().add(series);
        chart.setTitle(chart == priceChart ? "Average listing price by category" : "Average review rating by category");
        if (rows.isEmpty()) chart.setTitle(chart.getTitle() + " - no data");
    }

    private void reviewListing() {
        Listing item = table.getSelectionModel().getSelectedItem();
        if (!ready || busy || currentMember == null || item == null) { message("Choose a listing first."); return; }
        long member = currentMember.getId();
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Review: " + item.getTitle());
        dialog.initOwner(root.getScene().getWindow());
        Spinner<Integer> rating = new Spinner<>(1, 5, 5);
        TextArea comment = new TextArea(); comment.setPromptText("Optional comment (up to 1000 characters)");
        dialog.getDialogPane().setContent(new VBox(8, new Label("Rating (1-5)"), rating, comment));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int stars = rating.getValue(); String text = comment.getText();
        runTask(() -> { service.review(item.getId(), member, stars, text); return service.reviews(item.getId()); },
                reviews -> details.setText(item.getTitle() + "\n" + reviews));
    }

    private void message(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
    public void close() {
        refreshTimer.stop();
        ready = false;
        cancelRequested.set(true);
        worker.shutdownNow();
    }
}
