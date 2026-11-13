package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.service.ListingFactory;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Posting and catalog-reference form; condition and money rules live in
 * services/models.
 */
public class ListingFormView extends ScrollPane {
    /** Creates this component from the supplied values or dependencies. */
    public ListingFormView(ListingController listings, TaskRunner tasks, Runnable saved) {
        TextField title = new TextField();
        title.setId("post-title");
        title.setPromptText("e.g. Java textbook or laptop");
        TextField price = new TextField();
        price.setId("post-price");
        price.setPromptText("e.g. 250.00");
        ComboBox<String> type = new ComboBox<>();
        type.setId("post-type");
        type.getItems().setAll("TEXTBOOK", "ELECTRONICS", "FURNITURE");
        type.setValue("TEXTBOOK");
        ComboBox<Category> category = new ComboBox<>();
        category.setId("post-category");
        ComboBox<String> evidence = new ComboBox<>();
        evidence.setId("post-condition");
        title.disableProperty().bind(tasks.busyProperty());
        price.disableProperty().bind(tasks.busyProperty());
        type.disableProperty().bind(tasks.busyProperty());
        category.disableProperty().bind(tasks.busyProperty());
        evidence.disableProperty().bind(tasks.busyProperty());
        bindCategories(listings, type, category, evidence);
        Button post = UiTools.button("Post listing", "post-save", tasks,
                () -> listings.post(title.getText(), price.getText(), type.getValue(),
                        category.getValue(), evidence.getValue(), () -> {
                            title.clear();
                            price.clear();
                            saved.run();
                        }));
        post.getStyleClass().add("primary");
        post.setMaxWidth(Double.MAX_VALUE);
        VBox suggestions = suggestions(listings, tasks, title, type);
        VBox form = new VBox(12, UiTools.label("Create your listing", "page-title"),
                UiTools.label("A few details will help your next buyer find the right item.",
                        "muted"),
                new Label("Title"), title, new Label("Price (PHP)"), price, new Label("Type"), type,
                new Label("Category"), category, new Label("Condition observation"), evidence,
                UiTools.label("Choose the condition you actually observed.", "muted"), post);
        form.getStyleClass().add("card");
        form.setPrefWidth(650);
        HBox.setHgrow(form, javafx.scene.layout.Priority.ALWAYS);
        type.setMaxWidth(Double.MAX_VALUE);
        category.setMaxWidth(Double.MAX_VALUE);
        evidence.setMaxWidth(Double.MAX_VALUE);
        HBox content = new HBox(22, form, suggestions);
        content.getStyleClass().add("page-content");
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    private void bindCategories(ListingController listings, ComboBox<String> type, ComboBox<Category> category, ComboBox<String> evidence) {
        Runnable observations = () -> {
            evidence.getItems().setAll(ListingFactory.observations(type.getValue()));
            evidence.getSelectionModel().selectFirst();
            String defaultName = type.getValue().equals("TEXTBOOK") ? "Textbooks"
                    : type.getValue().equals("ELECTRONICS") ? "Electronics" : "Furniture";
            category.getItems().stream().filter(c -> c.getName().equals(defaultName)).findFirst()
                    .ifPresent(category::setValue);
        };
        listings.categories().addListener((ListChangeListener<Category>) change -> {
            category.getItems()
                    .setAll(listings.categories().filtered(c -> c.getParentId() != null));
            observations.run();
        });
        type.setOnAction(event -> observations.run());
        observations.run();
    }

    private VBox suggestions(ListingController listings, TaskRunner tasks, TextField title, ComboBox<String> type) {
        Label result = UiTools.label("", "muted");
        result.setId("catalog-result");
        result.textProperty().bind(listings.suggestionProperty());
        Button suggest = UiTools.button("Suggest category / price", "suggest-button", tasks,
                () -> listings.suggest(title.getText(), type.getValue()));
        javafx.scene.layout.StackPane preview = new javafx.scene.layout.StackPane(
                ItemArtwork.draw(type.getValue()));
        preview.getStyleClass().add("sell-preview");
        type.valueProperty().addListener(
                (o, before, value) -> preview.getChildren().setAll(ItemArtwork.draw(value)));
        VBox suggestions = new VBox(16, preview,
                UiTools.label("Need a price reference?", "section-title"),
                UiTools.label(
                        "Electronics suggestions use the live public catalog. Final PHP prices are entered manually.",
                        "muted"),
                suggest, result);
        suggestions.getStyleClass().addAll("card", "suggestion-card");
        suggestions.setPrefWidth(320);
        suggestions.setMinWidth(260);
        return suggestions;
    }
}
