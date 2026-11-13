package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.model.Listing;
import java.util.Locale;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/** Card presentation of the same observable list used by the table. */
public class ListingCards extends TilePane {
    private final ListingController listings;

    /** Creates this component from the supplied values or dependencies. */
    public ListingCards(ListingController listings) {
        super(16, 16);
        this.listings = listings;
        listings.items().addListener((ListChangeListener<Listing>) change -> rebuild());
        listings.selectedProperty().addListener((o, a, b) -> selection());
        setPrefColumns(2);
        setPrefTileWidth(228);
        setPadding(new Insets(2));
        setAlignment(Pos.TOP_LEFT);
    }

    private void rebuild() {
        getChildren().clear();
        for (Listing item : listings.items()) {
            var status = UiTools.label(UiTools.statusText(item.getStatus()), "status-badge");
            status.getStyleClass().add("status-" + item.getStatus().toLowerCase(Locale.ROOT));
            StackPane.setAlignment(status, Pos.TOP_RIGHT);
            StackPane.setMargin(status, new Insets(9));
            StackPane art = new StackPane(ItemArtwork.draw(item.getType()), status);
            art.getStyleClass().addAll("item-art",
                    "type-" + item.getType().toLowerCase(Locale.ROOT));
            var name = UiTools.label(item.getTitle().replace(" (demo)", ""), "item-name");
            name.setMinHeight(34);
            name.setMaxWidth(200);
            var grade = UiTools.label(item.getConditionLabel(), "condition-badge");
            if (!item.hasConditionDetails()) {
                grade.getStyleClass().add("condition-warning");
            }
            var price = UiTools.label(
                    item.getCurrency() + " " + String.format(Locale.ROOT, "%,.2f", item.getPrice()),
                    "item-price");
            HBox footer = new HBox(8, price);
            footer.setAlignment(Pos.CENTER_LEFT);
            VBox info = new VBox(7, UiTools.label(item.getType(), "item-type"), name, footer, grade,
                    UiTools.label(UiTools.listedDate(item), "item-date"));
            info.getStyleClass().add("item-info");
            VBox card = new VBox(art, info);
            card.setPrefWidth(228);
            card.getStyleClass().add("item-card");
            card.setUserData(item.getId());
            card.setOnMouseClicked(event -> {
                listings.selectedProperty().set(item);
                listings.viewDetails();
            });
            getChildren().add(card);
        }
        if (listings.items().isEmpty()) {
            getChildren().add(UiTools.label("No items match your search.", "muted"));
        }
        selection();
    }

    private void selection() {
        Listing selected = listings.selectedProperty().get();
        for (var card : getChildren()) {
            card.getStyleClass().remove("selected-card");
            if (selected != null && Long.valueOf(selected.getId()).equals(card.getUserData())) {
                card.getStyleClass().add("selected-card");
            }
        }
    }
}
