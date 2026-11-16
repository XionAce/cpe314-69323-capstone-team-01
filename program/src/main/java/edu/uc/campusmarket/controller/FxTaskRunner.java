package edu.uc.campusmarket.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** One shared worker; result/error handlers run on the JavaFX thread. */
public class FxTaskRunner implements TaskRunner {
    private static final System.Logger LOG = System.getLogger(FxTaskRunner.class.getName());
    private final ExecutorService worker;
    private final Consumer<Runnable> ui;
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final StringProperty status = new SimpleStringProperty("Opening database...");
    private final StringProperty error = new SimpleStringProperty("");
    private boolean refreshing;

    /** Creates this component from the supplied values or dependencies. */
    public FxTaskRunner(ExecutorService worker) {
        this(worker, Platform::runLater);
    }

    /**
     * Injects UI dispatch for deterministic execution-order tests without a window.
     */
    public FxTaskRunner(ExecutorService worker, Consumer<Runnable> ui) {
        this.worker = worker;
        this.ui = ui;
    }

    /**
     * Runs an operation if idle and delivers its result or error through observable
     * state.
     */
    @Override
    public <T> boolean run(Callable<T> work, Consumer<T> success, boolean quiet) {
        if (busy.get() || worker.isShutdown() || (quiet && refreshing)) {
            return false;
        }
        if (quiet) {
            refreshing = true;
        } else {
            busy.set(true);
            error.set("");
            status.set("Working...");
        }
        try {
            worker.execute(() -> execute(work, success, quiet));
        } catch (RejectedExecutionException failure) {
            finish(quiet);
            if (!quiet) {
                error.set("The application is closing.");
                status.set("The application is closing.");
            }
            return false;
        }
        return true;
    }

    private <T> void execute(Callable<T> work, Consumer<T> success, boolean quiet) {
        try {
            T value = work.call();
            ui.accept(() -> {
                finish(quiet);
                if (!quiet) {
                    status.set("Ready.");
                }
                success.accept(value);
            });
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            ui.accept(() -> failed(failure, quiet));
        }
    }

    private void finish(boolean quiet) {
        if (quiet) {
            refreshing = false;
        } else {
            busy.set(false);
        }
    }

    private void failed(Exception failure, boolean quiet) {
        finish(quiet);
        LOG.log(System.Logger.Level.WARNING, "Background operation failed.", failure);
        // A quiet read must not overwrite the status of a newer user action.
        if (!quiet || !busy.get()) {
            String message = failure.getMessage();
            error.set(message == null ? "Operation failed. Please try again." : message);
            status.set(quiet ? "Could not refresh. Please try again."
                    : "Action could not be completed.");
        }
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
