package edu.uc.campusmarket.controller;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/** Injectable background execution; tests can use a deterministic runner. */
public interface TaskRunner {
    <T> boolean run(Callable<T> work, Consumer<T> success, boolean quiet);

    default <T> boolean run(Callable<T> work, Consumer<T> success) {
        return run(work, success, false);
    }

    ReadOnlyBooleanProperty busyProperty();

    ReadOnlyStringProperty statusProperty();

    ReadOnlyStringProperty errorProperty();
}
