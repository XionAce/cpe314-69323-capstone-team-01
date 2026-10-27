package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.model.*;
import edu.uc.campusmarket.service.MarketplaceService;
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

/** One screen for the Week 9 skeleton: load, search, post and inspect listings. */
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
        root.setCenter(new HBox(20, listings, createPostForm()));
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
        view.setOnAction(event -> viewDetails());
        Button join = new Button("Join waitlist");
        join.setId("join-button");
        join.setOnAction(event -> joinWaitlist());
        details.setId("listing-details");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(5);
        details.setMinHeight(100);
        VBox panel = new VBox(9, new Label("Category (includes subcategories)"), filterCategory,
                searchRow, table, new HBox(8, view, join), details);
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    private VBox createPostForm() {
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
        post.getStyleClass().add("primary");
        // This is the main Week 9 demonstration: click -> save -> reload the table.
        post.setOnAction(event -> postListing());
        Label note = new Label("Choose one condition observation. The Java subclass calculates its grade.");
        note.setWrapText(true);
        note.getStyleClass().add("muted");
        VBox form = new VBox(8, heading, new Label("Title"), title, new Label("Price (PHP)"), price,
                new Label("Type"), type, new Label("Category"), postCategory,
                new Label("Condition observation"), observation, post, note);
        form.setPrefWidth(245);
        form.setMinWidth(245);
        form.setMaxWidth(245);
        return form;
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
        if (!ready) return;
        long categoryId = filterCategory.getValue().getId();
        String keyword = searchText.getText();
        runTask(() -> service.search(categoryId, keyword), this::showListings);
    }

    private void postListing() {
        if (!ready) return;
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
                    + "\nCondition: " + item.gradeCondition()
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
        root.setDisable(true);
        status.setText("Working...");
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> { root.setDisable(false); status.setText("Ready."); onSuccess.accept(task.getValue()); });
        task.setOnFailed(event -> { root.setDisable(false); status.setText("Action could not be completed."); message(task.getException().getMessage()); });
        worker.execute(task);
    }

    private void message(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setHeaderText(null);
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
    public void close() { worker.shutdownNow(); }
}
