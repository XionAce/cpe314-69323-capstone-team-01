package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.AnalyticsController;
import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.TaskRunner;
import java.util.function.Supplier;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** JSON file picker and progress presentation. */
public class ImportView extends VBox {
    /** Creates this component from the supplied values or dependencies. */
    public ImportView(AnalyticsController analytics, TaskRunner tasks, RefreshController refresh,
            Supplier<Window> owner) {
        super(16);
        Button choose = chooseButton(analytics, tasks, refresh, owner);
        Button cancel = new Button("Cancel import");
        cancel.setId("cancel-import");
        cancel.disableProperty().bind(analytics.importingProperty().not());
        cancel.setOnAction(event -> analytics.cancelImport());
        Label progress = UiTools.label("", "muted");
        progress.textProperty().bind(analytics.progressProperty());
        TextArea history = new TextArea();
        history.setId("history-results");
        history.setEditable(false);
        history.setWrapText(true);
        history.textProperty().bind(analytics.importTextProperty());
        VBox.setVgrow(history, Priority.ALWAYS);
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setId("import-spinner");
        spinner.setMaxSize(38, 38);
        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(-1);
        bar.setId("import-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        VBox loadingCopy = new VBox(7, UiTools.label("Import in progress", "section-title"),
                UiTools.label("Reading and validating your records. Cancel rolls back this import.",
                        "muted"),
                bar);
        HBox.setHgrow(loadingCopy, Priority.ALWAYS);
        HBox loading = new HBox(18, spinner, loadingCopy);
        loading.setId("import-loading");
        loading.getStyleClass().addAll("card", "loading-card");
        loading.visibleProperty().bind(analytics.importingProperty());
        loading.managedProperty().bind(loading.visibleProperty());
        choose.getStyleClass().add("primary");
        getChildren().addAll(UiTools.label("Your sales records", "section-title"), UiTools.label(
                "Import previous sales, not new marketplace listings. Valid saved rows feed weekly activity analysis.",
                "muted"), new HBox(8, choose, cancel), loading, progress, history);
        getStyleClass().addAll("page-content", "import-page");
    }

    private Button chooseButton(AnalyticsController analytics, TaskRunner tasks, RefreshController refresh, Supplier<Window> owner) {
        Button choose = UiTools.button("Import JSON sales history", "import-button", tasks, () -> {
            FileChooser picker = new FileChooser();
            picker.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
            refresh.setDialogOpen(true);
            try {
                var file = picker.showOpenDialog(owner.get());
                if (file != null) {
                    analytics.importFile(file.toPath());
                }
            } finally {
                refresh.setDialogOpen(false);
            }
        });
        return choose;
    }
}
