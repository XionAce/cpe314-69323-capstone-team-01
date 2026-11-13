package edu.uc.campusmarket.ui;

import edu.uc.campusmarket.controller.RefreshController;
import edu.uc.campusmarket.controller.TaskRunner;
import java.util.Optional;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import edu.uc.campusmarket.model.Listing;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.stage.Window;

/**
 * Small presentation helpers; no database, networking or background execution.
 */
public final class UiTools {
    private static final DateTimeFormatter LISTING_DATE = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private UiTools() {
    }

    /** Formats stored status codes for badges and the owner's archive filter. */
    public static String statusText(String status) {
        return switch (status) {
        case "ACTIVE" -> "Active";
        case "SOLD" -> "Sold";
        case "REMOVED" -> "Removed";
        default -> "All statuses";
        };
    }

    /** Formats the recorded creation date; old unknown dates stay explicit. */
    public static String listedDate(Listing item) {
        return item.getCreatedAt() == null ? "Date not recorded"
                : "Listed " + LISTING_DATE.format(item.getCreatedAt());
    }

    /** Provides the label operation for this component. */
    public static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return label;
    }

    /** Provides the button operation for this component. */
    public static Button button(String text, String id, TaskRunner tasks, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.disableProperty().bind(tasks.busyProperty());
        button.setOnAction(event -> action.run());
        return button;
    }

    /** Provides the dialog operation for this component. */
    public static <T> Optional<T> dialog(Dialog<T> dialog, Window owner,
            RefreshController refresh) {
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
        refresh.setDialogOpen(true);
        try {
            return dialog.showAndWait();
        } finally {
            refresh.setDialogOpen(false);
        }
    }

    /** Provides the message operation for this component. */
    public static void message(String text, Window owner, RefreshController refresh) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        alert.setHeaderText(null);
        dialog(alert, owner, refresh);
    }
}
