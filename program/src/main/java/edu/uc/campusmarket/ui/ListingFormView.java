package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.model.Category;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Posting and catalog-reference form; condition and money rules live in
 * services/models.
 */
public class ListingFormView extends ScrollPane {
    /** Creates this component from the supplied values or dependencies. */
    public ListingFormView(ListingController listings, TaskRunner tasks, Runnable saved) {
        TextField title = field("post-title", "e.g. Java textbook or laptop");
        TextField price = field("post-price", "e.g. 250.00");
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
        VBox suggestions = suggestions(listings, tasks, title, price, type, evidence);
        Label validation = feedback("post-validation-error", listings.formErrorProperty());
        Label saveError = feedback("post-save-error", tasks.errorProperty());
        VBox form = new VBox(12, UiTools.label("SELL SOMETHING GREAT", "eyebrow"),
                UiTools.label("Create your listing", "page-title"),
                UiTools.label("A few details will help your next buyer find the right item.",
                        "muted"),
                UiTools.label("1. Item details", "section-title"),
                UiTools.fieldLabel("Title", title), title, UiTools.fieldLabel("Price (PHP)", price),
                price, validation, UiTools.label("2. Category & condition", "section-title"),
                UiTools.fieldLabel("Type", type), type, UiTools.fieldLabel("Category", category),
                category, UiTools.fieldLabel("Condition observation", evidence), evidence,
                UiTools.label("Choose the condition you actually observed.", "muted"), saveError,
                post);
        form.getStyleClass().addAll("card", "listing-editor");
        form.setMinWidth(0);
        HBox.setHgrow(form, javafx.scene.layout.Priority.ALWAYS);
        type.setMaxWidth(Double.MAX_VALUE);
        category.setMaxWidth(Double.MAX_VALUE);
        evidence.setMaxWidth(Double.MAX_VALUE);
        arrange(form, suggestions);
    }

    private TextField field(String id, String hint) {
        TextField field = new TextField();
        field.setId(id);
        field.setPromptText(hint);
        return field;
    }

    private Label feedback(String id, javafx.beans.property.ReadOnlyStringProperty text) {
        Label label = UiTools.label("", "error-text");
        label.setId(id);
        label.textProperty().bind(text);
        label.managedProperty().bind(label.textProperty().isNotEmpty());
        return label;
    }

    private void arrange(VBox form, VBox suggestions) {
        FlowPane content = new FlowPane(22, 22, form, suggestions);
        content.prefWrapLengthProperty().bind(widthProperty());
        viewportBoundsProperty().addListener((o, before, bounds) -> {
            double available = Math.max(260, bounds.getWidth() - 58);
            boolean narrow = available < 850;
            form.setPrefWidth(narrow ? available : (available - 22) * .66);
            suggestions.setPrefWidth(narrow ? available : (available - 22) * .34);
        });
        content.getStyleClass().add("page-content");
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
    }

    private void bindCategories(ListingController listings, ComboBox<String> type,
            ComboBox<Category> category, ComboBox<String> evidence) {
        Runnable observations = () -> {
            evidence.getItems().setAll(listings.observations(type.getValue()));
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

    private VBox suggestions(ListingController listings, TaskRunner tasks, TextField title,
            TextField price, ComboBox<String> type, ComboBox<String> evidence) {
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
        Label previewTitle = UiTools.label("", "selected-item-title");
        previewTitle.textProperty()
                .bind(javafx.beans.binding.Bindings.createStringBinding(
                        () -> title.getText().isBlank() ? "Your next great find" : title.getText(),
                        title.textProperty()));
        Label previewPrice = UiTools.label("", "item-price");
        previewPrice.textProperty().bind(javafx.beans.binding.Bindings
                .createStringBinding(() -> previewPrice(price.getText()), price.textProperty()));
        Label previewCondition = UiTools.label("", "muted");
        previewCondition.textProperty().bind(evidence.valueProperty());
        VBox suggestions = new VBox(16, UiTools.label("LISTING PREVIEW", "eyebrow"), preview,
                previewTitle, previewPrice, previewCondition, new javafx.scene.control.Separator(),
                UiTools.label("Need a price reference?", "section-title"),
                UiTools.label("Electronics suggestions use the live public catalog. Final "
                        + "PHP prices are entered manually.", "muted"),
                suggest, result);
        suggestions.getStyleClass().addAll("card", "suggestion-card");
        suggestions.setPrefWidth(320);
        suggestions.setMinWidth(0);
        return suggestions;
    }

    private String previewPrice(String text) {
        try {
            if (text.length() > 80) {
                return "Add your asking price";
            }
            var value = new java.math.BigDecimal(text.strip());
            if (value.scale() < -18 || value.scale() > 2 || value.precision() > 19) {
                return "Add your asking price";
            }
            return value.signum() > 0
                    ? "PHP " + String.format(java.util.Locale.ROOT, "%,.2f", value)
                    : "Add your asking price";
        } catch (NumberFormatException error) {
            return "Add your asking price";
        }
    }
}
