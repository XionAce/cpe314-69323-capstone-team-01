package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.Category;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.ListingForRepair;
import edu.uc.campusmarket.service.CatalogService;
import edu.uc.campusmarket.service.ListingService;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** Observable listing state and the actions used by listing views. */
public class ListingController {
    private final ListingService service;
    private final CatalogService catalog;
    private final TaskRunner tasks;
    private final UserSession session;
    private final ObservableList<Listing> items = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObjectProperty<Listing> selected = new SimpleObjectProperty<>();
    private final StringProperty details = new SimpleStringProperty(
            "Select an item to view its details.");
    private final StringProperty suggestion = new SimpleStringProperty("");
    private final StringProperty query = new SimpleStringProperty("");
    private final StringProperty statusFilter = new SimpleStringProperty("ALL");
    private final BooleanProperty own = new SimpleBooleanProperty();
    private final ObjectProperty<Category> category = new SimpleObjectProperty<>();

    /** Creates this component from the supplied values or dependencies. */
    public ListingController(ListingService service, CatalogService catalog, TaskRunner tasks,
            UserSession session) {
        this.service = service;
        this.catalog = catalog;
        this.tasks = tasks;
        this.session = session;
        selected.addListener((observable, previous, current) -> {
            if (previous == null || current == null || !values(previous).equals(values(current))) {
                details.set("Select an item, then click View details.");
            }
        });
        session.memberProperty().addListener((o, before, after) -> {
            items.clear();
            selected.set(null);
            details.set("");
            query.set("");
            own.set(false);
            statusFilter.set("ALL");
            suggestion.set("");
            if (after != null) {
                loadCategories();
            }
        });
    }

    /** Provides the load categories operation for this component. */
    public void loadCategories() {
        tasks.run(service::categories, result -> {
            categories.setAll(result);
            if (!result.isEmpty()) {
                category.set(result.getFirst());
                refresh();
            }
        });
    }

    /**
     * Requests current data and updates the observable state when results are
     * applicable.
     */
    public void refresh() {
        refresh(false);
    }

    /**
     * Requests current data and updates the observable state when results are
     * applicable.
     */
    public void refresh(boolean quiet) {
        if (session.member() == null || category.get() == null) {
            return;
        }
        long generation = session.generation(), member = session.memberId(),
                categoryId = category.get().getId();
        String text = query.get();
        boolean mine = own.get();
        String status = statusFilter.get();
        tasks.run(() -> service.search(member, categoryId, text, mine, status), result -> {
            if (generation != session.generation() || category.get() == null) {
                return;
            }
            if (category.get().getId() != categoryId || !query.get().equals(text)
                    || own.get() != mine || !statusFilter.get().equals(status)) {
                // A newer filter request may have arrived while the worker was busy.
                refresh(quiet);
                return;
            }
            List<?> before = items.stream().map(this::values).toList();
            List<?> after = result.stream().map(this::values).toList();
            if (!before.equals(after)) {
                long id = selected.get() == null ? -1 : selected.get().getId();
                items.setAll(result);
                selected.set(result.stream().filter(p -> p.getId() == id).findFirst().orElse(null));
            }
        }, quiet);
    }

    private List<?> values(Listing item) {
        return List.of(item.getId(), item.getTitle(), item.getPrice(), item.getCurrency(),
                item.getType(), item.getStatus(), item.getConditionLabel(),
                item.getConditionIssue(), String.valueOf(item.getCreatedAt()), item.getSellerId(),
                item.getCategoryId());
    }

    /** Provides the view details operation for this component. */
    public void viewDetails() {
        Listing item = selected.get();
        if (item == null || session.member() == null) {
            return;
        }
        long member = session.memberId(), generation = session.generation();
        tasks.run(() -> service.details(member, item), text -> {
            if (generation == session.generation() && selected.get() != null
                    && selected.get().getId() == item.getId()) {
                details.set(text);
            }
        });
    }

    /** Creates a listing using the signed-in member's seller identity. */
    public void post(String title, String price, String type, Category category, String observation,
            Runnable saved) {
        long member = session.memberId();
        if (category == null) {
            return;
        }
        tasks.run(() -> service.post(member, title, price, type, category.getId(), observation),
                id -> {
                    saved.run();
                    refresh();
                });
    }

    /** Saves an owner-only title/price edit and reloads the current filter. */
    public void update(Listing item, String title, String price) {
        long member = session.memberId(), generation = session.generation();
        tasks.run(() -> {
            service.update(member, item, title, price);
            return true;
        }, done -> {
            if (generation == session.generation()) {
                refresh();
            }
        });
    }

    /** Withdraws an available listing only for its owner. */
    public void withdraw() {
        Listing item = selected.get();
        if (item == null) {
            return;
        }
        long member = session.memberId();
        tasks.run(() -> {
            service.withdraw(member, item.getId());
            return true;
        }, done -> {
            details.set("Listing removed. Your saved records are retained.");
            refresh();
        });
    }

    /**
     * Records a sale for the selected owned active item and refreshes the archive.
     */
    public void markSold() {
        Listing item = selected.get();
        if (item == null) {
            return;
        }
        long member = session.memberId();
        tasks.run(() -> {
            service.markSold(member, item.getId());
            return true;
        }, done -> {
            details.set("Listing marked sold. It is hidden from public search.");
            refresh();
        });
    }

    /** True when an active item is selected in the signed-in member's archive. */
    public javafx.beans.binding.BooleanBinding canManageProperty() {
        return javafx.beans.binding.Bindings.createBooleanBinding(
                () -> session.member() != null && own.get() && selected.get() != null
                        && Listing.ACTIVE.equals(selected.get().getStatus()),
                session.memberProperty(), own, selected);
    }

    /**
     * Current archive status filter; public search always shows ACTIVE items only.
     */
    public StringProperty statusFilterProperty() {
        return statusFilter;
    }

    /** Provides the repairable operation for this component. */
    public void repairable(Consumer<List<ListingForRepair>> result) {
        long member = session.memberId();
        tasks.run(() -> service.repairable(member), result);
    }

    /**
     * Restores condition evidence only for an owned listing with missing details.
     */
    public void restore(ListingForRepair item, String evidence) {
        long member = session.memberId();
        tasks.run(() -> {
            service.restore(member, item, evidence);
            return true;
        }, done -> {
            details.set("Condition details restored.");
            refresh();
        });
    }

    /**
     * Requests supported catalog references without treating their prices as PHP.
     */
    public void suggest(String title, String type) {
        suggestion.set("Requesting catalog...");
        tasks.run(() -> catalog.suggest(title, type), suggestion::set);
    }

    /** Provides the items operation for this component. */
    public ObservableList<Listing> items() {
        return items;
    }

    /** Provides the categories operation for this component. */
    public ObservableList<Category> categories() {
        return categories;
    }

    /** Returns observable selected state for view binding. */
    public ObjectProperty<Listing> selectedProperty() {
        return selected;
    }

    /** Returns observable query state for view binding. */
    public StringProperty queryProperty() {
        return query;
    }

    /** Returns observable own state for view binding. */
    public BooleanProperty ownProperty() {
        return own;
    }

    /** Returns observable category state for view binding. */
    public ObjectProperty<Category> categoryProperty() {
        return category;
    }

    /** Returns observable details state for view binding. */
    public ReadOnlyStringProperty detailsProperty() {
        return details;
    }

    /** Returns observable suggestion state for view binding. */
    public ReadOnlyStringProperty suggestionProperty() {
        return suggestion;
    }
}
