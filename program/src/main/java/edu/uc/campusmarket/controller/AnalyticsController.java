package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.AnalyticsSnapshot;
import edu.uc.campusmarket.service.AnalyticsService;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Controls charts and transactional JSON import on the shared worker. */
public class AnalyticsController {
    private final AnalyticsService analytics;
    private final TaskRunner tasks;
    private final UserSession session;
    private final ObjectProperty<AnalyticsSnapshot> snapshot = new SimpleObjectProperty<>();
    private final StringProperty currency = new SimpleStringProperty("PHP");
    private final StringProperty importText = new SimpleStringProperty("");
    private final StringProperty progress = new SimpleStringProperty(
            "Choose a JSON sales-history file.");
    private final BooleanProperty importing = new SimpleBooleanProperty();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Consumer<Runnable> ui;

    /** Creates this component from the supplied values or dependencies. */
    public AnalyticsController(AnalyticsService analytics, TaskRunner tasks, UserSession session) {
        this(analytics, tasks, session, Platform::runLater);
    }

    /** Creates this component from the supplied values or dependencies. */
    public AnalyticsController(AnalyticsService analytics, TaskRunner tasks, UserSession session,
            Consumer<Runnable> ui) {
        this.analytics = analytics;
        this.tasks = tasks;
        this.session = session;
        this.ui = ui;
        session.memberProperty().addListener((o, a, b) -> {
            snapshot.set(null);
            importText.set("");
        });
        tasks.busyProperty().addListener((o, a, b) -> {
            if (!b) {
                if (importing.get()) {
                    progress.set("Import ended. See the result or error message.");
                }
                importing.set(false);
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
        if (session.member() == null) {
            return;
        }
        String selected = currency.get().strip().toUpperCase(Locale.ROOT);
        long generation = session.generation();
        tasks.run(() -> analytics.load(selected), value -> {
            if (generation == session.generation()
                    && currency.get().strip().equalsIgnoreCase(selected)
                    && !value.equals(snapshot.get())) {
                snapshot.set(value);
            }
        }, quiet);
    }

    /**
     * Reads historical-sales JSON with progress, cancellation and transactional
     * writes.
     */
    public void importFile(Path file) {
        session.memberId();
        if (tasks.busyProperty().get()) {
            return;
        }
        cancelled.set(false);
        progress.set("Importing; records are not committed yet.");
        boolean started = tasks.run(() -> analytics.importHistory(file, cancelled::get, count -> {
            if (count == 1 || count % 1000 == 0) {
                ui.accept(
                        () -> progress.set("Processed " + count + " records (not committed yet)."));
            }
        }), result -> {
            importText.set(result);
            progress.set("Import committed.");
            importing.set(false);
        });
        importing.set(started && tasks.busyProperty().get());
    }

    /** Provides the cancel import operation for this component. */
    public void cancelImport() {
        cancelled.set(true);
        progress.set("Cancelling; waiting for rollback...");
    }

    /** Returns observable snapshot state for view binding. */
    public ObjectProperty<AnalyticsSnapshot> snapshotProperty() {
        return snapshot;
    }

    /** Returns observable currency state for view binding. */
    public StringProperty currencyProperty() {
        return currency;
    }

    /** Returns observable import text state for view binding. */
    public ReadOnlyStringProperty importTextProperty() {
        return importText;
    }

    /** Returns observable progress state for view binding. */
    public ReadOnlyStringProperty progressProperty() {
        return progress;
    }

    /** Returns observable importing state for view binding. */
    public ReadOnlyBooleanProperty importingProperty() {
        return importing;
    }
}
