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
import javafx.scene.control.Tooltip;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Small presentation helpers; no database, networking or background execution.
 */
public final class UiTools {
    private static final DateTimeFormatter LISTING_DATE = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    private static final Image LOGO = new Image(
            UiTools.class.getResource("/ui/campus-marketplace-logo.png").toExternalForm());

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
        if (style.equals("error-text") || style.equals("login-error")) {
            label.setAccessibleRole(AccessibleRole.TEXT);
            label.visibleProperty().bind(label.textProperty().isNotEmpty());
            label.managedProperty().bind(label.visibleProperty());
            label.setMaxWidth(Double.MAX_VALUE);
            Label icon = new Label("!");
            icon.getStyleClass().add("feedback-icon");
            icon.setMinSize(22, 22);
            icon.setMaxSize(22, 22);
            icon.setAlignment(Pos.CENTER);
            label.setGraphic(icon);
            label.setGraphicTextGap(10);
        }
        return label;
    }

    /** Uses the supplied transparent logo without changing its artwork. */
    public static ImageView logo(double size) {
        ImageView image = new ImageView(LOGO);
        image.setFitWidth(size);
        image.setFitHeight(size);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.getStyleClass().add("app-logo");
        image.setAccessibleText("Campus Marketplace logo");
        return image;
    }

    /** Adds a themed, draggable window frame and the application's own icon. */
    public static Parent window(Stage stage, Parent content) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.getIcons().setAll(LOGO);
        return new WindowFrame(stage, content);
    }

    /** Fills the current monitor's work area, keeping the taskbar accessible. */
    public static void maximizeWindow(Stage stage) {
        if (!expanded(stage)) {
            stage.getProperties().put("campusmarket.restoreBounds", new javafx.geometry.Rectangle2D(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));
        }
        setBounds(stage, screen(stage));
    }

    private static javafx.geometry.Rectangle2D screen(Stage stage) {
        return javafx.stage.Screen
                .getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(),
                        stage.getHeight())
                .stream().findFirst().orElse(javafx.stage.Screen.getPrimary()).getVisualBounds();
    }

    private static boolean expanded(Stage stage) {
        var bounds = screen(stage);
        return Math.abs(stage.getX() - bounds.getMinX()) < 2
                && Math.abs(stage.getY() - bounds.getMinY()) < 2
                && Math.abs(stage.getWidth() - bounds.getWidth()) < 2
                && Math.abs(stage.getHeight() - bounds.getHeight()) < 2;
    }

    private static void toggleWindow(Stage stage) {
        if (expanded(stage) && stage.getProperties()
                .get("campusmarket.restoreBounds") instanceof javafx.geometry.Rectangle2D bounds) {
            setBounds(stage, bounds);
        } else {
            maximizeWindow(stage);
        }
    }

    private static void setBounds(Stage stage, javafx.geometry.Rectangle2D bounds) {
        stage.setMaximized(false);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    /**
     * Links a visible field label to its control for keyboard and assistive access.
     */
    public static Label fieldLabel(String text, javafx.scene.Node control) {
        Label label = label(text, "field-label");
        label.setLabelFor(control);
        return label;
    }

    /** Provides the button operation for this component. */
    public static Button button(String text, String id, TaskRunner tasks, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        button.disableProperty().bind(tasks.busyProperty());
        button.setOnAction(event -> action.run());
        return button;
    }

    /** Provides the dialog operation for this component. */
    public static <T> Optional<T> dialog(Dialog<T> dialog, Window owner,
            RefreshController refresh) {
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
        styleDialog(dialog);
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
        alert.setTitle("Campus Marketplace");
        alert.setHeaderText(null);
        dialog(alert, owner, refresh);
    }

    private static void styleDialog(Dialog<?> dialog) {
        var pane = dialog.getDialogPane();
        pane.getStyleClass().add("branded-dialog");
        Label title = label(dialog.getTitle().isBlank() ? "Campus Marketplace" : dialog.getTitle(),
                "dialog-title");
        VBox words = new VBox(4, title);
        if (pane.getHeaderText() != null && !pane.getHeaderText().isBlank()) {
            words.getChildren().add(label(pane.getHeaderText(), "muted"));
        }
        HBox.setHgrow(words, Priority.ALWAYS);
        Button close = windowButton("×", "dialog-close", "Close dialog", dialog::close);
        HBox header = new HBox(12, logo(38), words, close);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-heading");
        pane.setHeader(header);
        pane.sceneProperty().addListener((o, before, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((w, old, window) -> {
                    if (window instanceof Stage stage) {
                        dialogWindow(stage, pane);
                    }
                });
                if (scene.getWindow() instanceof Stage stage) {
                    dialogWindow(stage, pane);
                }
            }
        });
        if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage stage) {
            dialogWindow(stage, pane);
        }
        drag(header, () -> pane.getScene().getWindow());
        for (ButtonType type : pane.getButtonTypes()) {
            pane.lookupButton(type).getStyleClass()
                    .add(type.getButtonData().isCancelButton() ? "quiet" : "primary");
        }
    }

    private static void dialogWindow(Stage stage, javafx.scene.control.DialogPane pane) {
        stage.getIcons().setAll(LOGO);
        if (pane.getProperties().putIfAbsent("window-resize", Boolean.TRUE) == null) {
            new WindowResize(stage, pane);
        }
    }

    private static Button windowButton(String text, String id, String description,
            Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.setAccessibleText(description);
        button.setTooltip(new Tooltip(description));
        button.getStyleClass().add("window-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private static boolean overButton(MouseEvent event, Node bar) {
        Node node = event.getTarget() instanceof Node target ? target : null;
        while (node != null && node != bar) {
            if (node instanceof Button) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private static void drag(HBox bar, java.util.function.Supplier<Window> window) {
        double[] offset = new double[2];
        bar.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !overButton(event, bar)) {
                offset[0] = event.getScreenX() - window.get().getX();
                offset[1] = event.getScreenY() - window.get().getY();
            }
        });
        bar.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown() || overButton(event, bar)) {
                return;
            }
            Window target = window.get();
            if (target instanceof Stage stage && expanded(stage)) {
                toggleWindow(stage);
                offset[0] = Math.min(offset[0], target.getWidth() * .8);
            }
            target.setX(event.getScreenX() - offset[0]);
            target.setY(event.getScreenY() - offset[1]);
        });
    }

    /**
     * Window behavior stays in the presentation layer, outside business services.
     */
    private static final class WindowFrame extends BorderPane {
        private WindowFrame(Stage stage, Parent content) {
            getStyleClass().add("window-shell");
            setCenter(content);
            Label title = label("", "window-title");
            title.textProperty().bind(stage.titleProperty());
            title.setWrapText(false);
            title.setMinWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);
            Button minimize = windowButton("−", "window-minimize", "Minimize",
                    () -> stage.setIconified(true));
            Button maximize = windowButton("□", "window-maximize", "Maximize or restore",
                    () -> toggleWindow(stage));
            maximize.textProperty()
                    .bind(javafx.beans.binding.Bindings.createStringBinding(
                            () -> expanded(stage) ? "❐" : "□", stage.xProperty(), stage.yProperty(),
                            stage.widthProperty(), stage.heightProperty()));
            maximize.accessibleTextProperty()
                    .bind(javafx.beans.binding.Bindings.createStringBinding(
                            () -> expanded(stage) ? "Restore window" : "Maximize window",
                            stage.xProperty(), stage.yProperty(), stage.widthProperty(),
                            stage.heightProperty()));
            Button close = windowButton("×", "window-close", "Close application", () -> stage
                    .fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
            HBox bar = new HBox(10, logo(30), title, minimize, maximize, close);
            bar.setId("window-title-bar");
            bar.getStyleClass().add("window-title-bar");
            bar.setAlignment(Pos.CENTER_LEFT);
            setTop(bar);
            drag(bar, () -> stage);
            bar.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2
                        && !overButton(event, bar)) {
                    maximize.fire();
                }
            });
            new WindowResize(stage, this);
        }
    }

    /**
     * Restores edge and corner resizing after replacing operating-system borders.
     */
    private static final class WindowResize {
        private final Stage stage;
        private final javafx.scene.layout.Region root;
        private double x, y, width, height, mouseX, mouseY;
        private boolean west, east, north, south, resizing;

        private WindowResize(Stage stage, javafx.scene.layout.Region root) {
            this.stage = stage;
            this.root = root;
            root.addEventFilter(MouseEvent.MOUSE_MOVED, this::cursor);
            root.addEventFilter(MouseEvent.MOUSE_PRESSED, this::beginResize);
            root.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::resize);
            root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> resizing = false);
        }

        private void edges(MouseEvent event) {
            boolean allowed = stage.isResizable() && !expanded(stage);
            west = allowed && event.getSceneX() < 6;
            east = allowed && event.getSceneX() > root.getWidth() - 6;
            north = allowed && event.getSceneY() < 6;
            south = allowed && event.getSceneY() > root.getHeight() - 6;
        }

        private void cursor(MouseEvent event) {
            edges(event);
            Cursor cursor = Cursor.DEFAULT;
            if (north) {
                cursor = west ? Cursor.NW_RESIZE : east ? Cursor.NE_RESIZE : Cursor.N_RESIZE;
            } else if (south) {
                cursor = west ? Cursor.SW_RESIZE : east ? Cursor.SE_RESIZE : Cursor.S_RESIZE;
            } else if (west || east) {
                cursor = west ? Cursor.W_RESIZE : Cursor.E_RESIZE;
            }
            root.setCursor(cursor);
        }

        private void beginResize(MouseEvent event) {
            edges(event);
            resizing = event.getButton() == MouseButton.PRIMARY && (west || east || north || south);
            if (resizing) {
                x = stage.getX();
                y = stage.getY();
                width = stage.getWidth();
                height = stage.getHeight();
                mouseX = event.getScreenX();
                mouseY = event.getScreenY();
                event.consume();
            }
        }

        private void resize(MouseEvent event) {
            if (!resizing) {
                return;
            }
            double dx = event.getScreenX() - mouseX, dy = event.getScreenY() - mouseY;
            double newWidth = Math.max(stage.getMinWidth(), width + (west ? -dx : dx));
            double newHeight = Math.max(stage.getMinHeight(), height + (north ? -dy : dy));
            if (west || east) {
                stage.setWidth(newWidth);
                if (west) {
                    stage.setX(x + width - newWidth);
                }
            }
            if (north || south) {
                stage.setHeight(newHeight);
                if (north) {
                    stage.setY(y + height - newHeight);
                }
            }
            event.consume();
        }
    }
}
