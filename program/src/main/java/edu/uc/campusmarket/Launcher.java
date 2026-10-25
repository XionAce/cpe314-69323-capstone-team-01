// This starting class belongs to the same package as App.
package edu.uc.campusmarket;

// Import JavaFX's helper that starts its UI thread and manages the application lifecycle.
import javafx.application.Application;

/** Ordinary main class for Eclipse and Maven launches without a module descriptor. */
public final class Launcher {
    // final prevents subclasses. A private constructor prevents creating unnecessary Launcher objects.
    private Launcher() { }

    public static void main(String[] args) {
        // public: the Java runtime can call it; static: no Launcher object is needed.
        // void: no value is returned; args: command-line arguments supplied when launching.
        // Start App through JavaFX; JavaFX will create it and call its start(Stage) method.
        Application.launch(App.class, args);
    }
}
