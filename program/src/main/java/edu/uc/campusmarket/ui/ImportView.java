package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.TaskRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** File selection, real import activity and the last completed result. */
public class ImportView extends ScrollPane {
    private final ObjectProperty<Path> selected = new SimpleObjectProperty<>();
    private final BooleanProperty completed = new SimpleBooleanProperty();
    private final Label filename = UiTools.label("No file selected", "selected-file");
    private final Label fileError = UiTools.label("", "error-text");
    private final Label saved = UiTools.label("—", "stat-value");
    private final Label invalid = UiTools.label("—", "stat-value");
    private final Label duplicates = UiTools.label("—", "stat-value");

    /** Binds all import controls to the existing transactional controller. */
    public ImportView(AnalyticsController analytics, TaskRunner tasks, RefreshController refresh,
            Supplier<Window> owner) {
        fileError.setId("import-file-error");
        filename.setId("selected-import-file");
        saved.setId("import-saved-count");
        invalid.setId("import-invalid-count");
        duplicates.setId("import-duplicate-count");
        VBox drop = dropZone(tasks, refresh, owner);
        Button start = UiTools.button("Import selected file", "import-start", tasks,
                () -> analytics.importFile(selected.get()));
        start.getStyleClass().add("primary");
        start.disableProperty().unbind();
        start.disableProperty().bind(tasks.busyProperty().or(selected.isNull()));
        Button cancel = new Button("Cancel import");
        cancel.setId("cancel-import");
        cancel.disableProperty().bind(analytics.importingProperty().not());
        cancel.setOnAction(event -> analytics.cancelImport());
        Label progress = UiTools.label("", "muted");
        progress.setId("import-status");
        progress.textProperty().bind(analytics.progressProperty());
        VBox operation = new VBox(14, new HBox(10, start, cancel), loading(analytics), progress);
        operation.getStyleClass().addAll("card", "import-operation");
        FlowPane stats = new FlowPane(16, 16, stat("Saved", saved),
                stat("Invalid rows skipped", invalid), stat("Duplicates skipped", duplicates));
        stats.getStyleClass().add("summary-stats");
        VBox body = new VBox(18,
                UiTools.label("Bring your previous sales into one place. Import JSON records "
                        + "to build the weekly sales chart; marketplace listings stay separate.",
                        "muted"),
                drop, operation, UiTools.label("Last completed import", "section-title"), stats,
                result(analytics));
        body.getStyleClass().addAll("page-content", "import-page");
        setContent(body);
        setFitToWidth(true);
        getStyleClass().add("page-scroll");
        analytics.importTextProperty().addListener((o, before, text) -> totals(text));
        analytics.progressProperty()
                .addListener((o, before, text) -> completed.set(text.equals("Import committed.")));
    }

    private VBox dropZone(TaskRunner tasks, RefreshController refresh, Supplier<Window> owner) {
        Button choose = UiTools.button("Choose JSON file", "import-button", tasks, () -> {
            FileChooser picker = new FileChooser();
            picker.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
            refresh.setDialogOpen(true);
            try {
                var file = picker.showOpenDialog(owner.get());
                if (file != null) {
                    selectFile(file.toPath());
                }
            } finally {
                refresh.setDialogOpen(false);
            }
        });
        choose.getStyleClass().add("primary");
        FlowPane selection = new FlowPane(12, 8, choose, filename);
        selection.setAlignment(Pos.CENTER);
        VBox drop = new VBox(8,
                UiTools.label("Drop a JSON sales-history file here", "section-title"), selection,
                fileError, UiTools.label(
                        "Choose or drop one JSON file, then select Import when ready.", "muted"));
        drop.setId("sales-drop-zone");
        drop.setAlignment(Pos.CENTER);
        drop.getStyleClass().add("file-drop-zone");
        drop.setOnDragOver(event -> {
            if (!tasks.busyProperty().get() && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                drop.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drag-over"),
                        true);
            }
            event.consume();
        });
        drop.setOnDragExited(event -> drop.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("drag-over"), false));
        drop.setOnDragDropped(event -> {
            var files = event.getDragboard().getFiles();
            boolean accepted = !tasks.busyProperty().get() && files.size() == 1
                    && selectFile(files.getFirst().toPath());
            if (files.size() != 1) {
                fileError.setText("Choose one JSON file at a time.");
                selected.set(null);
                filename.setText("No file selected");
            }
            drop.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drag-over"), false);
            event.setDropCompleted(accepted);
            event.consume();
        });
        return drop;
    }

    private boolean selectFile(Path file) {
        completed.set(false);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)
                || !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
            fileError.setText("Choose a readable .json sales-history file.");
            selected.set(null);
            filename.setText("No file selected");
            return false;
        }
        selected.set(file);
        filename.setText(file.getFileName().toString());
        fileError.setText("");
        return true;
    }

    private VBox loading(AnalyticsController analytics) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setId("import-spinner");
        spinner.setMaxSize(32, 32);
        HBox active = new HBox(12, spinner,
                new VBox(4, UiTools.label("Reading and validating records", "section-title"),
                        UiTools.label("You can keep using the app. Cancel rolls back this import.",
                                "muted")));
        active.setId("import-loading");
        active.visibleProperty().bind(analytics.importingProperty());
        active.managedProperty().bind(active.visibleProperty());
        ProgressBar bar = new ProgressBar(0);
        bar.setId("import-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.progressProperty()
                .bind(Bindings.when(analytics.importingProperty()).then(-1.0)
                        .otherwise(Bindings
                                .when(completed.and(analytics.importTextProperty().isNotEmpty()))
                                .then(1.0).otherwise(0.0)));
        return new VBox(12, active, bar);
    }

    private VBox result(AnalyticsController analytics) {
        TextArea history = new TextArea();
        history.setId("history-results");
        history.setEditable(false);
        history.setWrapText(true);
        history.setPrefRowCount(10);
        history.setMinHeight(180);
        history.textProperty().bind(analytics.importTextProperty());
        Label hint = UiTools.label(
                "Your result and the latest saved sales appear here after an import. "
                        + "Required fields: sourceRecordId, categoryPath, price, currency and soldAt.",
                "muted");
        VBox box = new VBox(12, UiTools.label("Import result & saved records", "section-title"),
                hint, history);
        box.getStyleClass().add("card");
        return box;
    }

    private VBox stat(String title, Label value) {
        VBox card = new VBox(6, UiTools.label(title, "muted"), value);
        card.setPrefWidth(220);
        card.getStyleClass().add("stat-card");
        return card;
    }

    private void totals(String text) {
        var result = Pattern.compile("^Saved: (\\d+), invalid: (\\d+), duplicates: (\\d+)")
                .matcher(text);
        boolean found = result.find();
        saved.setText(found ? result.group(1) : "—");
        invalid.setText(found ? result.group(2) : "—");
        duplicates.setText(found ? result.group(3) : "—");
    }
}
