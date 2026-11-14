package edu.uc.campusmarket.controller;

import edu.uc.campusmarket.model.HomeSummary;
import edu.uc.campusmarket.service.HomeService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Loads the signed-in member's home in one background task and rejects stale
 * results.
 */
public class HomeController {
    private final HomeService service;
    private final TaskRunner tasks;
    private final UserSession session;
    private boolean pending;
    private final ObjectProperty<HomeSummary> summary = new SimpleObjectProperty<>();

    /**
     * Receives dependencies and clears private home data whenever the account
     * changes.
     */
    public HomeController(HomeService service, TaskRunner tasks, UserSession session) {
        this.service = service;
        this.tasks = tasks;
        this.session = session;
        session.memberProperty().addListener((o, before, after) -> {
            pending = false;
            summary.set(null);
        });
        tasks.busyProperty().addListener((o, before, busy) -> {
            if (!busy && pending) {
                pending = false;
                load(true);
            }
        });
    }

    /**
     * Refreshes only this session's data; quiet polling does not change the status
     * message.
     */
    public void load(boolean quiet) {
        if (session.member() == null) {
            return;
        }
        if (tasks.busyProperty().get()) {
            pending = true;
            return;
        }
        long member = session.memberId(), generation = session.generation();
        tasks.run(() -> service.loadFor(member), value -> {
            if (generation == session.generation()
                    && !java.util.Objects.equals(summary.get(), value)) {
                summary.set(value);
            }
        }, quiet);
    }

    /** Exposes the latest immutable home snapshot for view binding. */
    public ReadOnlyObjectProperty<HomeSummary> summaryProperty() {
        return summary;
    }
}
