package edu.uc.campusmarket.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

/** One shared worker; result/error handlers run on the JavaFX thread. */
public class FxTaskRunner implements TaskRunner {
    private final ExecutorService worker;
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final StringProperty status = new SimpleStringProperty("Opening database...");
    private final StringProperty error = new SimpleStringProperty("");

    /** Creates this component from the supplied values or dependencies. */
    public FxTaskRunner(ExecutorService worker) {
        this.worker = worker;
    }

    /**
     * Runs an operation if idle and delivers its result or error through observable
     * state.
     */
    @Override
    public <T> boolean run(Callable<T> work, Consumer<T> success, boolean quiet) {
        if (busy.get() || worker.isShutdown()) {
            return false;
        }
        busy.set(true);
        error.set("");
        if (!quiet) {
            status.set("Working...");
        }
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            busy.set(false);
            if (!quiet) {
                status.set("Ready.");
            }
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy.set(false);
            String message = task.getException().getMessage();
            error.set(message == null ? "Operation failed. Try again." : message);
            status.set("Action could not be completed.");
        });
        task.setOnCancelled(event -> {
            busy.set(false);
            status.set("Operation cancelled.");
        });
        try {
            worker.execute(task);
        } catch (RejectedExecutionException failure) {
            busy.set(false);
            error.set("The application is closing.");
            return false;
        }
        return true;
    }

    /** Returns observable busy state for view binding. */
    public ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    /** Returns observable status state for view binding. */
    public ReadOnlyStringProperty statusProperty() {
        return status;
    }

    /** Returns observable error state for view binding. */
    public ReadOnlyStringProperty errorProperty() {
        return error;
    }
}
