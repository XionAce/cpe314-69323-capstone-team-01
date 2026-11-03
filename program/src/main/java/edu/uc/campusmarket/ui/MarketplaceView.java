package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.model.*;
import edu.uc.campusmarket.service.MarketplaceService;
import edu.uc.campusmarket.api.ProductCatalogClient;
import edu.uc.campusmarket.persistence.Database.CategorySummary;
import javafx.application.Platform;
import javafx.scene.chart.*;
import javafx.stage.FileChooser;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
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

/** Simple tabs for listings, JSON history import and category charts. */
public class MarketplaceView {
    private final MarketplaceService service;
    private final BorderPane root = new BorderPane();
    private final TableView<Listing> table = new TableView<>();
    private final ComboBox<Member> memberBox = new ComboBox<>();
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
        root.setPadding(new Insets(20));
        Label heading = new Label("UC-B Campus Marketplace");
        heading.getStyleClass().add("title");
        memberBox.setId("member-selector");
        memberBox.setPrefWidth(170);
        memberBox.setOnAction(event -> details.setText("Select a listing, then click View details."));
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox header = new HBox(10, heading, space, new Label("Demo member:"), memberBox);
        header.setPadding(new Insets(0, 0, 18, 0));
        root.setTop(header);
        VBox listings = createListings();
        HBox.setHgrow(listings, Priority.ALWAYS);
        TabPane tabs = new TabPane();
        Tab market = new Tab("Marketplace", new HBox(20, listings, createPostForm()));
        Tab imports = new Tab("Sales history", createImportPanel());
        Tab analytics = new Tab("Analytics", createAnalytics());
        tabs.getTabs().addAll(market, imports, analytics);
        for (Tab tab : tabs.getTabs()) tab.setClosable(false);
        root.setCenter(tabs);
        Button about = new Button("About this project");
        about.setId("about-button");
        about.setOnAction(e -> message("Local campus prototype: listings, reviews, category search, API suggestions and JSON import.\nYou can open this message while a background task runs."));
        header.getChildren().add(about);
        databasePath.getStyleClass().add("muted");
        root.setBottom(new VBox(5, status, databasePath));
        BorderPane.setMargin(root.getBottom(), new Insets(12, 0, 0, 0));
    }

    private VBox createListings() {
        filterCategory.setId("category-selector");
        filterCategory.setMaxWidth(Double.MAX_VALUE);
        searchText.setId("search-box");
        searchText.setPromptText("Search listing titles");
        Button search = new Button("Search / refresh");
        search.setId("search-button");
        workButtons.add(search);
        // This event handler runs when the user clicks Search.
        search.setOnAction(event -> refresh());
        searchText.setOnAction(event -> refresh());
        HBox searchRow = new HBox(8, searchText, search);
        HBox.setHgrow(searchText, Priority.ALWAYS);
        table.setId("listing-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No matching listings."));
        table.getSelectionModel().selectedItemProperty().addListener((observable, before, after) ->
                details.setText("Select a listing, then click View details."));
        addColumn("Listing", 190, item -> item.getTitle());
        addColumn("Type", 100, item -> item.getType());
        addColumn("Price", 105, item -> item.getCurrency() + " " + item.getPrice());
        // A Listing reference runs the actual Textbook/Electronics/Furniture method.
        addColumn("Condition", 100, item -> item.gradeCondition().toString());
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
        details.setPrefRowCount(5);
        details.setMinHeight(100);
        Button review = new Button("Review listing");
        review.setId("review-button");
        review.setOnAction(e -> reviewListing());
        workButtons.add(review);
        VBox panel = new VBox(9, new Label("Category (includes subcategories)"), filterCategory,
                searchRow, table, new HBox(8, view, join, review), details);
        VBox.setVgrow(table, Priority.ALWAYS);
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
        Label note = new Label("Choose one condition observation. The Java subclass calculates its grade.");
        note.setWrapText(true);
        note.getStyleClass().add("muted");
        Button suggest = new Button("Suggest category / price");
        suggest.setId("suggest-button");
        suggest.setOnAction(e -> suggest());
        workButtons.add(suggest);
        catalogResult.setWrapText(true);
        catalogResult.setId("catalog-result");
        VBox form = new VBox(8, heading, new Label("Title"), title, new Label("Price (PHP)"), price,
                new Label("Type"), type, new Label("Category"), postCategory,
                new Label("Condition observation"), observation, post, note, suggest, catalogResult);
        form.setPrefWidth(245);
        form.setMinWidth(245);
        form.setMaxWidth(245);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true); scroll.setPrefWidth(280); scroll.setMinWidth(280); scroll.setMaxWidth(280);
        return scroll;
    }

    private void chooseObservations() {
        long defaultCategory;
        if (type.getValue().equals("TEXTBOOK")) {
            observation.getItems().setAll("No visible damage", "Light markings", "Heavy markings", "Damaged cover", "Missing pages");
            defaultCategory = 2;
        } else if (type.getValue().equals("ELECTRONICS")) {
            observation.getItems().setAll("No visible damage", "Cosmetic damage", "Functional defect", "Does not power on");
            defaultCategory = 3;
        } else {
            observation.getItems().setAll("No visible damage", "Light wear", "Heavy wear", "Structural damage", "Unstable");
            defaultCategory = 4;
        }
        observation.getSelectionModel().selectFirst();
        for (Category category : postCategory.getItems()) {
            if (category.getId() == defaultCategory) postCategory.setValue(category);
        }
    }

    public void initialize() {
        runTask(() -> { service.initialize(); return service.search(1, ""); }, listings -> {
            memberBox.getItems().setAll(service.getMembers());
            memberBox.getSelectionModel().selectFirst();
            filterCategory.getItems().setAll(service.getCategories());
            filterCategory.getSelectionModel().selectFirst();
            for (Category category : service.getCategories()) {
                if (category.getParentId() != null) postCategory.getItems().add(category);
            }
            chooseObservations();
            databasePath.setText("Database: " + service.databasePath());
            ready = true;
            showListings(listings);
        });
    }

    private void refresh() {
        if (!ready || busy) return;
        long categoryId = filterCategory.getValue().getId();
        String keyword = searchText.getText();
        runTask(() -> service.search(categoryId, keyword), this::showListings);
    }

    private void postListing() {
        if (!ready || busy) return;
        try {
            // Read controls on the UI thread before starting background database work.
            long memberId = memberBox.getValue().getId();
            long categoryId = postCategory.getValue().getId();
            String itemTitle = title.getText();
            BigDecimal amount = new BigDecimal(price.getText().trim());
            String itemType = type.getValue();
            String evidence = observation.getValue();
            runTask(() -> {
                long sellerId = service.sellerFor(memberId);
                Listing item = makeListing(sellerId, categoryId, itemTitle, amount, itemType, evidence);
                service.post(memberId, item);
                return service.search(1, ""); // SELECT the saved data again.
            }, listings -> {
                filterCategory.getSelectionModel().selectFirst();
                searchText.clear();
                showListings(listings);
                table.getSelectionModel().selectLast();
                title.clear();
                price.clear();
                status.setText("Listing saved. " + listings.size() + " listing(s).");
            });
        } catch (NumberFormatException error) {
            message("Enter a numeric price, such as 250.00.");
        }
    }

    private Listing makeListing(long seller, long category, String title, BigDecimal price,
                                String type, String evidence) {
        if (type.equals("TEXTBOOK")) {
            WearLevel markings = WearLevel.NONE;
            if (evidence.equals("Light markings")) markings = WearLevel.LIGHT;
            if (evidence.equals("Heavy markings")) markings = WearLevel.HEAVY;
            // One missing page represents the chosen observation in this skeleton form.
            return new Textbook(0, seller, category, title, price, "PHP", "AVAILABLE",
                    evidence.equals("Missing pages") ? 1 : 0, markings, evidence.equals("Damaged cover"));
        }
        if (type.equals("ELECTRONICS")) {
            return new Electronics(0, seller, category, title, price, "PHP", "AVAILABLE",
                    !evidence.equals("Does not power on"), evidence.equals("Functional defect"), evidence.equals("Cosmetic damage"));
        }
        WearLevel wear = WearLevel.NONE;
        if (evidence.equals("Light wear")) wear = WearLevel.LIGHT;
        if (evidence.equals("Heavy wear")) wear = WearLevel.HEAVY;
        return new Furniture(0, seller, category, title, price, "PHP", "AVAILABLE",
                !evidence.equals("Unstable"), evidence.equals("Structural damage"), wear);
    }

    private void showListings(List<Listing> listings) {
        table.getItems().setAll(listings);
        status.setText(listings.size() + " listing(s).");
        if (!listings.isEmpty()) table.getSelectionModel().selectFirst();
    }

    private void viewDetails() {
        Listing item = table.getSelectionModel().getSelectedItem();
        if (item == null) { message("Choose a listing first."); return; }
        long memberId = memberBox.getValue().getId();
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
        long memberId = memberBox.getValue().getId();
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
        if (!ready || busy) return;
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
            if (!ready || busy) return;
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
        VBox box = new VBox(10, new Label("Import previous sales, not new listings."),
                new HBox(8, choose, cancelImport), importProgress, history);
        VBox.setVgrow(history, Priority.ALWAYS); box.setPadding(new Insets(10));
        return box;
    }

    private void startImport(java.nio.file.Path file) {
        if (!ready || busy) return;
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
            if (!ready || busy) return;
            String selectedCurrency = currency.getEditor().getText().trim().toUpperCase(java.util.Locale.ROOT);
            if (!selectedCurrency.matches("[A-Z]{3}")) { message("Enter a three-letter currency."); return; }
            runTask(() -> List.of(service.prices(selectedCurrency), service.ratings()), rows -> {
                ((NumberAxis) priceChart.getYAxis()).setLabel("Price (" + selectedCurrency + ")");
                fillChart(priceChart, rows.get(0)); fillChart(ratingChart, rows.get(1));
                if (rows.get(1).isEmpty()) ratingSummary.setText("No campus reviews yet. Categories without reviews are unrated.");
                else {
                    double best = rows.get(1).getFirst().average();
                    String text = "Best-rated: ";
                    for (var row : rows.get(1)) if (Double.compare(row.average(), best) == 0)
                        text += row.category() + " " + String.format(java.util.Locale.ROOT, "%.2f", best)
                                + "/5 (" + row.count() + " reviews)  ";
                    ratingSummary.setText(text);
                }
            });
        });
        VBox box = new VBox(8, new HBox(8, new Label("Price currency:"), currency, refresh), priceChart, ratingChart, ratingSummary);
        VBox.setVgrow(priceChart, Priority.ALWAYS); VBox.setVgrow(ratingChart, Priority.ALWAYS);
        box.setPadding(new Insets(10)); return box;
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
        if (!ready || busy || item == null) { message("Choose a listing first."); return; }
        long member = memberBox.getValue().getId();
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
    public void close() { cancelRequested.set(true); worker.shutdownNow(); }
}
