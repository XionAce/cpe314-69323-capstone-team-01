package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.ListingController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.ReviewController;
import edu.uc.campusmarket.controller.TaskRunner;
import edu.uc.campusmarket.controller.WaitlistController;
import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
import edu.uc.campusmarket.service.ListingFactory;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Browsing and selected-item panels, with status actions in the owner's
 * archive.
 */
public class ListingTableView extends VBox {
    /** Builds controls and binds their state to the listing controller. */
    public ListingTableView(ListingController listings, ReviewController reviews,
            WaitlistController waitlists, TaskRunner tasks, RefreshController refresh,
            Supplier<Window> owner, Runnable sell) {
        super(14);
        CheckBox mine = new CheckBox("My listings");
        ComboBox<String> status = new ComboBox<>();
        HBox filters = filters(listings, tasks, mine, status);
        TableView<Listing> table = table(listings);
        FlowPane heading = new FlowPane(12, 8);
        StackPane browser = browser(listings, table, mine, status, heading);
        SplitPane split = details(browser, listings, reviews, waitlists, tasks, refresh, owner);
        HBox hero = hero(tasks, sell);
        getChildren().addAll(hero, filters, heading, split);
        getStyleClass().add("page-content");
    }

    private Button ownerAction(String text, String id, Runnable action, ListingController listings,
            TaskRunner tasks, RefreshController refresh, Supplier<Window> owner, String prompt) {
        Button button = UiTools.button(text, id, tasks, () -> {
            Listing item = listings.selectedProperty().get();
            if (item == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    prompt + "\n" + item.getTitle() + "\nReviews and the waitlist are retained.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(text);
            if (UiTools.dialog(confirm, owner.get(), refresh)
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {
                action.run();
            }
        });
        button.disableProperty().unbind();
        button.disableProperty().bind(tasks.busyProperty().or(listings.canManageProperty().not()));
        return button;
    }

    private void column(TableView<Listing> table, String title, Function<Listing, String> value) {
        TableColumn<Listing, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    private HBox filters(ListingController listings, TaskRunner tasks, CheckBox mine,
            ComboBox<String> status) {
        TextField search = new TextField();
        search.setId("search-box");
        search.setPromptText("Find textbooks, electronics, furniture...");
        search.textProperty().bindBidirectional(listings.queryProperty());
        ComboBox<Category> category = new ComboBox<>(listings.categories());
        category.setId("category-selector");
        category.valueProperty().bindBidirectional(listings.categoryProperty());
        category.setPrefWidth(190);
        Button find = UiTools.button("Search", "search-button", tasks, listings::refresh);
        find.getStyleClass().add("primary");
        search.setOnAction(event -> listings.refresh());
        HBox filters = new HBox(10, search, category, find);
        HBox.setHgrow(search, Priority.ALWAYS);
        mine.setId("my-listings");
        mine.selectedProperty().bindBidirectional(listings.ownProperty());
        mine.setOnAction(event -> listings.refresh());
        status.setId("status-filter");
        status.getItems().addAll("ALL", Listing.ACTIVE, Listing.SOLD, Listing.REMOVED);
        status.setValue("ALL");
        status.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return UiTools.statusText(value == null ? "ALL" : value);
            }

            @Override
            public String fromString(String text) {
                return "ALL";
            }
        });
        status.valueProperty().bindBidirectional(listings.statusFilterProperty());
        status.disableProperty().bind(listings.ownProperty().not());
        status.setOnAction(event -> listings.refresh());
        return filters;
    }

    private TableView<Listing> table(ListingController listings) {
        TableView<Listing> table = new TableView<>(listings.items());
        table.setId("listing-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No items match these filters."));
        column(table, "Listing", Listing::getTitle);
        column(table, "Price", item -> item.getCurrency() + " " + item.getPrice());
        column(table, "Condition", Listing::getConditionLabel);
        column(table, "Status", item -> UiTools.statusText(item.getStatus()));
        column(table, "Listed", UiTools::listedDate);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) {
                listings.selectedProperty().set(b);
            }
        });
        listings.selectedProperty().addListener((o, a, b) -> table.getSelectionModel().select(b));
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                listings.viewDetails();
            }
        });
        return table;
    }

    private StackPane browser(ListingController listings, TableView<Listing> table, CheckBox mine,
            ComboBox<String> status, FlowPane heading) {
        ScrollPane cards = new ScrollPane(new ListingCards(listings));
        cards.setId("listing-cards");
        cards.setFitToWidth(true);
        cards.getStyleClass().add("page-scroll");
        ToggleButton cardMode = new ToggleButton("Cards");
        cardMode.setId("cards-mode-button");
        ToggleButton listMode = new ToggleButton("List");
        listMode.setId("list-mode-button");
        ToggleGroup mode = new ToggleGroup();
        cardMode.setToggleGroup(mode);
        listMode.setToggleGroup(mode);
        cardMode.setSelected(true);
        table.visibleProperty().bind(listMode.selectedProperty());
        table.managedProperty().bind(table.visibleProperty());
        cards.visibleProperty().bind(listMode.selectedProperty().not());
        cards.managedProperty().bind(cards.visibleProperty());
        Label count = UiTools.label("", "muted");
        count.textProperty().bind(Bindings.size(listings.items()).asString("%d items"));
        Label headingText = UiTools.label("", "section-title");
        headingText.textProperty().bind(Bindings.when(listings.ownProperty()).then("Your listings")
                .otherwise("Explore the marketplace"));
        heading.getChildren().setAll(headingText, count, mine, status, cardMode, listMode);
        heading.setAlignment(Pos.CENTER_LEFT);
        StackPane browser = new StackPane(cards, table);
        browser.setMinWidth(300);
        return browser;
    }

    private Button restoreButton(ListingController listings, TaskRunner tasks,
            RefreshController refresh, Supplier<Window> owner) {
        Button restore = UiTools.button("Restore missing details", "restore-details-button", tasks,
                () -> listings.repairable(rows -> {
                    if (rows.isEmpty()) {
                        UiTools.message("No owned listings have missing details.", owner.get(),
                                refresh);
                        return;
                    }
                    ChoiceDialog<ListingForRepair> choose = new ChoiceDialog<>(rows.getFirst(),
                            rows);
                    choose.setHeaderText("Choose your listing to restore.");
                    UiTools.dialog(choose, owner.get(), refresh).ifPresent(item -> {
                        ChoiceDialog<String> evidence = new ChoiceDialog<>(null,
                                ListingFactory.observations(item.type()));
                        evidence.setSelectedItem(null);
                        evidence.setHeaderText("Select the condition actually observed.");
                        evidence.getDialogPane().lookupButton(ButtonType.OK).disableProperty()
                                .bind(evidence.selectedItemProperty().isNull());
                        UiTools.dialog(evidence, owner.get(), refresh)
                                .ifPresent(value -> listings.restore(item, value));
                    });
                }));
        return restore;
    }

    private FlowPane ownerActions(ListingController listings, TaskRunner tasks,
            RefreshController refresh, Supplier<Window> owner) {
        Button sold = ownerAction("Mark as sold", "sold-button", listings::markSold, listings,
                tasks, refresh, owner, "Mark this item as sold?");
        sold.getStyleClass().add("success-button");
        Button remove = ownerAction("Remove listing", "withdraw-button", listings::withdraw,
                listings, tasks, refresh, owner, "Remove this item from public search?");
        remove.getStyleClass().add("danger-button");
        Button edit = UiTools.button("Edit title / price", "edit-listing-button", tasks,
                () -> edit(listings, refresh, owner));
        edit.disableProperty().unbind();
        edit.disableProperty().bind(tasks.busyProperty().or(listings.canManageProperty().not()));
        FlowPane ownerActions = new FlowPane(8, 8, edit, sold, remove);
        ownerActions.visibleProperty().bind(listings.ownProperty());
        ownerActions.managedProperty().bind(ownerActions.visibleProperty());
        return ownerActions;
    }

    private void edit(ListingController listings, RefreshController refresh,
            Supplier<Window> owner) {
        Listing item = listings.selectedProperty().get();
        if (item == null) {
            return;
        }
        TextField title = new TextField(item.getTitle());
        title.setId("edit-title");
        TextField price = new TextField(item.getPrice().toPlainString());
        price.setId("edit-price");
        javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Edit listing");
        dialog.getDialogPane()
                .setContent(new VBox(12, new Label("Title"), title,
                        new Label("Price (" + item.getCurrency() + ")"), price,
                        UiTools.label(
                                "Type, condition, creation date and saved reviews are retained.",
                                "muted")));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (UiTools.dialog(dialog, owner.get(), refresh)
                .orElse(ButtonType.CANCEL) == ButtonType.OK) {
            listings.update(item, title.getText(), price.getText());
        }
    }

    private SplitPane details(StackPane browser, ListingController listings,
            ReviewController reviews, WaitlistController waitlists, TaskRunner tasks,
            RefreshController refresh, Supplier<Window> owner) {
        ReviewsView selected = new ReviewsView(listings, reviews, waitlists, tasks, refresh, owner);
        VBox.setVgrow(selected, Priority.ALWAYS);
        FlowPane ownerActions = ownerActions(listings, tasks, refresh, owner);
        Button restore = restoreButton(listings, tasks, refresh, owner);
        restore.getStyleClass().add("quiet");
        Label help = UiTools.label("Use My listings to mark your item sold or remove it. "
                + "Your records stay saved.", "muted");
        VBox detailPane = new VBox(10, selected, ownerActions, restore, help);
        detailPane.setMinWidth(250);
        ScrollPane detailScroll = new ScrollPane(detailPane);
        detailScroll.setFitToWidth(true);
        detailScroll.setFitToHeight(true);
        detailScroll.setMinWidth(260);
        detailScroll.getStyleClass().add("page-scroll");
        SplitPane split = new SplitPane(browser, detailScroll);
        split.setId("marketplace-split");
        split.setDividerPositions(1);
        split.getItems().remove(detailScroll);
        Button closeDetails = new Button("Close details  ×");
        closeDetails.getStyleClass().add("quiet");
        closeDetails.setOnAction(event -> listings.selectedProperty().set(null));
        detailPane.getChildren().addFirst(closeDetails);
        listings.selectedProperty().addListener((o, before, item) -> {
            if (item == null) {
                split.getItems().remove(detailScroll);
            } else if (!split.getItems().contains(detailScroll)) {
                split.getItems().add(detailScroll);
                split.setDividerPositions(.67);
            }
        });
        split.getStyleClass().add("marketplace-split");
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    private HBox hero(TaskRunner tasks, Runnable sell) {
        VBox heroCopy = new VBox(5, UiTools.label("Good finds. New beginnings.", "hero-title"),
                UiTools.label("Give campus essentials their next chapter.", "hero-description"));
        HBox.setHgrow(heroCopy, Priority.ALWAYS);
        HBox hero = new HBox(20, heroCopy,
                UiTools.button("+ Sell an item", "sell-shortcut", tasks, sell));
        hero.getStyleClass().add("market-hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        return hero;
    }
}
