package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.ReviewController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.controller.WaitlistController;
import edu.uc.campusmarket.model.Listing;
import java.util.function.Supplier;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** The selected item's review and waitlist controls. */
public class ReviewsView extends VBox {
    /** Creates this component from the supplied values or dependencies. */
    public ReviewsView(ListingController listings, ReviewController reviews,
            WaitlistController waitlists, TaskRunner tasks, RefreshController refresh,
            Supplier<Window> owner, Consumer<Listing> openChat) {
        super(8);
        TextArea details = new TextArea();
        details.setId("listing-details");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(12);
        details.setMinHeight(120);
        VBox.setVgrow(details, Priority.ALWAYS);
        details.textProperty().bind(listings.detailsProperty());
        Button view = UiTools.button("View details", "view-button", tasks, listings::viewDetails);
        Button join = UiTools.button("Join waitlist", "join-button", tasks,
                () -> waitlists.join(listings.selectedProperty().get(), listings::viewDetails));
        Button review = reviewButton(listings, reviews, tasks, refresh, owner);
        view.disableProperty().unbind();
        view.disableProperty().bind(tasks.busyProperty().or(listings.selectedProperty().isNull()));
        var inactive = javafx.beans.binding.Bindings.createBooleanBinding(
                () -> listings.selectedProperty().get() == null
                        || !Listing.ACTIVE.equals(listings.selectedProperty().get().getStatus()),
                listings.selectedProperty());
        join.disableProperty().unbind();
        join.disableProperty().bind(tasks.busyProperty().or(inactive));
        review.disableProperty().unbind();
        review.disableProperty().bind(tasks.busyProperty().or(inactive));
        Button read = UiTools.button("Read reviews", "read-reviews-button", tasks, () -> {
            Listing item = listings.selectedProperty().get();
            reviews.load(item, () -> {
                if (listings.selectedProperty().get() == item) {
                    showReviews(item, reviews, owner.get(), refresh);
                }
            });
        });
        read.disableProperty().unbind();
        read.disableProperty().bind(tasks.busyProperty().or(listings.selectedProperty().isNull()));
        Button leave = UiTools.button("Leave waitlist", "leave-waitlist-button", tasks,
                () -> waitlists.leave(listings.selectedProperty().get(), listings::viewDetails));
        leave.disableProperty().unbind();
        leave.disableProperty().bind(tasks.busyProperty().or(listings.selectedProperty().isNull()));
        Button chat = UiTools.button("Open chat", "open-chat-button", tasks,
                () -> openChat.accept(listings.selectedProperty().get()));
        chat.disableProperty().unbind();
        chat.disableProperty().bind(tasks.busyProperty().or(listings.selectedProperty().isNull()));
        FlowPane actions = new FlowPane(8, 8, view, chat, read, join, leave, review);
        getChildren().addAll(UiTools.label("Item details", "section-title"), actions,
                UiTools.label("Join the waitlist before reviewing another member's item.", "muted"),
                details);
        getStyleClass().addAll("card", "detail-panel");
    }

    private void showReviews(Listing item, ReviewController reviews, Window owner,
            RefreshController refresh) {
        VBox cards = new VBox(14, UiTools.label(item.getTitle(), "section-title"));
        cards.setPrefWidth(330);
        if (reviews.items().isEmpty()) {
            cards.getChildren().add(UiTools
                    .label("No reviews yet. Be the first to share your experience.", "muted"));
        }
        for (var row : reviews.items()) {
            VBox card = new VBox(8, UiTools.label(row.reviewerName(), "section-title"),
                    UiTools.label("★".repeat(row.rating()) + "☆".repeat(5 - row.rating()),
                            "review-stars"),
                    UiTools.label(
                            row.comment() == null || row.comment().isBlank() ? "No written comment."
                                    : row.comment(),
                            "review-comment"));
            card.getStyleClass().add("card");
            cards.getChildren().add(card);
        }
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(400);
        scroll.setPrefViewportWidth(350);
        scroll.getStyleClass().add("page-scroll");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reviews");
        dialog.getDialogPane().setId("reviews-popup");
        dialog.getDialogPane().getStyleClass().add("review-dialog");
        dialog.getDialogPane().setPrefWidth(400);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setOnShown(event -> javafx.application.Platform.runLater(() -> {
            dialog.setX(owner.getX() + Math.max(0, owner.getWidth() - dialog.getWidth() - 24));
            dialog.setY(owner.getY() + 100);
        }));
        UiTools.dialog(dialog, owner, refresh);
    }

    private Button reviewButton(ListingController listings, ReviewController reviews,
            TaskRunner tasks, RefreshController refresh, Supplier<Window> owner) {
        Button review = UiTools.button("Review listing", "review-button", tasks, () -> {
            Listing item = listings.selectedProperty().get();
            if (item == null) {
                return;
            }
            Spinner<Integer> stars = new Spinner<>(1, 5, 5);
            TextArea comment = new TextArea();
            comment.setPromptText("Optional comment (up to 1000 characters)");
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Review: " + item.getTitle());
            dialog.getDialogPane().getStyleClass().add("review-dialog");
            dialog.getDialogPane().setPrefWidth(380);
            dialog.setOnShown(event -> javafx.application.Platform.runLater(() -> {
                Window window = owner.get();
                dialog.setX(
                        window.getX() + Math.max(0, window.getWidth() - dialog.getWidth() - 24));
                dialog.setY(window.getY() + 115);
            }));
            dialog.getDialogPane()
                    .setContent(new VBox(14, UiTools.label(item.getTitle(), "section-title"),
                            UiTools.label("Share your experience with this item.", "muted"),
                            new Label("Your rating (1-5 stars)"), stars, new Label("Your review"),
                            comment));
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            if (UiTools.dialog(dialog, owner.get(), refresh)
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {
                reviews.submit(item, stars.getValue(), comment.getText(), listings::viewDetails);
            }
        });
        return review;
    }
}
