// This file belongs to the JavaFX user-interface package.
package edu.uc.campusmarket.ui;

// Imports provide the listing enum and JavaFX containers, bindings, colors, shapes and effects.
import edu.uc.campusmarket.model.ListingType;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Local, scalable illustrations for listings that do not have product photos. */
// StackPane centers its child nodes; this reusable control draws category artwork using Java shapes.
public final class ListingArtwork extends StackPane {
    // These constants define the drawing coordinate system, before resizing to fit a card.
    private static final double ART_WIDTH = 360;
    private static final double ART_HEIGHT = 220;

    // Build one illustration based on the listing type; no image file, API or download is needed.
    public ListingArtwork(ListingType type) {
        // A drawing must have a type; fail immediately if the caller supplies null.
        Objects.requireNonNull(type, "Listing type is required");
        // The style class lets marketplace.css adjust illustration size in compact windows.
        getStyleClass().add("listing-artwork");
        // Preferred/minimum sizes guide JavaFX layout; MAX_VALUE lets the pane fill available width.
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        setMinHeight(100);
        setPrefSize(280, 160);

        // A switch expression selects a category color without altering listing data or grading.
        String background = switch (type) {
            case TEXTBOOK -> "#e9eee4";
            case ELECTRONICS -> "#e4edf0";
            case FURNITURE -> "#efe7dc";
        };
        // Inline JavaFX CSS assigns the selected background color and rounded corners.
        setStyle("-fx-background-color: " + background + "; -fx-background-radius: 16;");

        // Group layers shapes in drawing order; later children are painted on top of earlier ones.
        Group illustration = new Group();
        // A stable view box keeps scaling and centering identical for all three types.
        illustration.getChildren().add(rect(0, 0, ART_WIDTH, ART_HEIGHT, 0, "transparent"));
        // Delegate the actual shape coordinates to a method for the selected product type.
        switch (type) {
            case TEXTBOOK -> drawTextbooks(illustration);
            case ELECTRONICS -> drawLaptop(illustration);
            case FURNITURE -> drawChair(illustration);
        }
        // var infers the binding type. The lambda recalculates when width or height changes.
        var scale = Bindings.createDoubleBinding(
                // Fit both dimensions using the smaller ratio; clamp scale between 0.25 and 1.6.
                () -> Math.max(0.25, Math.min(1.6,
                        Math.min(getWidth() / ART_WIDTH, getHeight() / ART_HEIGHT))),
                widthProperty(), heightProperty());
        // Bind horizontal and vertical scale together so the drawing keeps its proportions.
        illustration.scaleXProperty().bind(scale);
        illustration.scaleYProperty().bind(scale);
        getChildren().add(illustration);

        // A bound rounded rectangle clips artwork that would otherwise extend outside its pane.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        setClip(clip);
        // Let clicks pass through the decorative shapes to the containing listing-card button.
        setMouseTransparent(true);
        // Describe the illustration for assistive technology; this is not a photograph of the item.
        setAccessibleText(switch (type) {
            case TEXTBOOK -> "Illustration of textbooks";
            case ELECTRONICS -> "Illustration of a laptop";
            case FURNITURE -> "Illustration of a chair";
        });
    }

    // Layer circles/lines as decoration, then draw stacked books and an upright cover.
    private static void drawTextbooks(Group scene) {
        scene.getChildren().addAll(
                circle(188, 107, 82, "#dbe4d4"),
                circle(276, 58, 5, "#b7c5ad"),
                line(69, 155, 95, 155, "#a5b79a", 2),
                line(82, 142, 82, 168, "#a5b79a", 2));

        // Each rect/line call supplies coordinates, dimensions and a color for one book part.
        Group stack = new Group();
        stack.getChildren().addAll(
                rect(111, 173, 156, 21, 5, "#ae7658"),
                rect(115, 176, 147, 13, 2, "#f6efdc"),
                line(119, 181, 258, 181, "#dcd3bf", 1),
                line(119, 185, 258, 185, "#dcd3bf", 1),
                rect(100, 151, 151, 24, 5, "#62765b"),
                rect(106, 155, 141, 14, 2, "#fbf7e9"),
                line(111, 161, 242, 161, "#dbd9c9", 1));
        stack.setEffect(shadow());
        scene.getChildren().add(stack);

        // A separate group lets the whole upright book rotate and cast one shadow.
        Group upright = new Group();
        upright.getChildren().addAll(
                rect(130, 33, 111, 135, 7, "#294e3c"),
                rect(136, 33, 8, 135, 0, "#41634d"),
                line(149, 43, 231, 43, "#738c70", 1),
                rect(155, 58, 61, 5, 2, "#e7ead8"),
                rect(155, 71, 45, 3, 1, "#b9c7aa"),
                circle(185, 110, 22, "#d2dbb9"),
                line(170, 110, 200, 110, "#456449", 2),
                line(185, 95, 185, 125, "#456449", 2),
                line(174, 99, 196, 121, "#456449", 1),
                line(174, 121, 196, 99, "#456449", 1),
                rect(155, 148, 43, 3, 1, "#a5b99a"));
        // Rotate the complete book by -9 degrees for a slightly tilted illustration.
        upright.setRotate(-9);
        upright.setEffect(shadow());
        scene.getChildren().add(upright);
    }

    // Draw the background decoration, laptop display, small screen details and keyboard/base.
    private static void drawLaptop(Group scene) {
        scene.getChildren().addAll(
                circle(183, 105, 83, "#d1e0e5"),
                circle(283, 63, 5, "#9eb9c2"),
                line(69, 96, 86, 96, "#9ab5be", 2),
                line(77, 88, 77, 104, "#9ab5be", 2));
        // Add rectangles/circles from the outer screen frame toward the inner display contents.
        Group computer = new Group();
        computer.getChildren().addAll(
                rect(72, 40, 216, 137, 9, "#354249"),
                rect(80, 48, 200, 120, 4, "#f6f6ec"),
                circle(180, 44, 1.4, "#a5b3ba"),
                rect(80, 48, 200, 19, 0, "#c0d0d1"),
                circle(91, 57, 2, "#6f8b90"),
                circle(99, 57, 2, "#8fa8aa"),
                circle(107, 57, 2, "#a3b7b8"),
                rect(96, 81, 62, 73, 5, "#dce5db"),
                rect(107, 92, 40, 40, 4, "#b4c8ad"),
                rect(107, 140, 29, 3, 1, "#7b957d"),
                rect(172, 84, 84, 6, 2, "#567565"),
                rect(172, 99, 65, 4, 2, "#bcc8bb"),
                rect(172, 109, 74, 4, 2, "#d1d8cb"),
                rect(172, 130, 55, 19, 6, "#547d65"));
        // Polygon connects the supplied x/y point pairs to make a slanted laptop base.
        Polygon base = new Polygon(72, 175, 288, 175, 315, 193, 45, 193);
        base.setFill(Color.web("#b0bec3"));
        // A second polygon creates the smaller keyboard area inside the base.
        Polygon keyboard = new Polygon(92, 178, 267, 178, 280, 185, 79, 185);
        keyboard.setFill(Color.web("#879ba3"));
        computer.getChildren().addAll(
                base, keyboard,
                rect(45, 191, 270, 7, 4, "#6d838e"),
                rect(158, 189, 44, 3, 1, "#dce3e3"));
        computer.setEffect(shadow());
        scene.getChildren().add(computer);
    }

    // Draw decorative shapes followed by chair legs, supports, backrest and seat.
    private static void drawChair(Group scene) {
        scene.getChildren().addAll(
                circle(180, 105, 82, "#e5d6c4"),
                circle(279, 70, 5, "#c5a98b"),
                line(75, 133, 94, 133, "#c7ac8b", 2),
                line(84, 124, 84, 142, "#c7ac8b", 2));
        // Lines represent legs/supports; rounded rectangles form the chair back and seat.
        Group chair = new Group();
        chair.getChildren().addAll(
                line(143, 124, 130, 196, "#8c6345", 9),
                line(219, 124, 235, 196, "#8c6345", 9),
                line(158, 131, 155, 191, "#b4865a", 8),
                line(209, 131, 212, 191, "#b4865a", 8),
                line(133, 166, 231, 166, "#9c7350", 5),
                line(137, 63, 147, 128, "#a57b54", 7),
                line(224, 63, 215, 128, "#a57b54", 7),
                rect(128, 38, 106, 71, 18, "#ad8b67"),
                rect(136, 43, 90, 57, 14, "#c1a180"),
                line(148, 57, 213, 57, "#d2b79a", 1.5),
                line(148, 70, 213, 70, "#d2b79a", 1.5),
                line(148, 83, 213, 83, "#d2b79a", 1.5),
                rect(119, 119, 125, 23, 10, "#83603f"),
                rect(119, 114, 125, 20, 10, "#b29a78"),
                rect(126, 115, 111, 10, 5, "#c1ab8c"));
        chair.setEffect(shadow());
        scene.getChildren().add(chair);
    }

    // Rectangle helper: x/y is the top-left, followed by size, corner rounding and color.
    private static Rectangle rect(double x, double y, double width, double height,
                                  double radius, String color) {
        Rectangle shape = new Rectangle(x, y, width, height);
        // Equal arc width/height gives the rectangle matching rounded corners.
        shape.setArcWidth(radius);
        shape.setArcHeight(radius);
        // Color.web parses hexadecimal CSS-style colors such as #294e3c.
        shape.setFill(Color.web(color));
        return shape;
    }

    // Circle helper: x/y is the center, radius controls size, and color fills the shape.
    private static Circle circle(double x, double y, double radius, String color) {
        return new Circle(x, y, radius, Color.web(color));
    }

    // Line helper: two endpoint pairs, a color and a stroke thickness describe one segment.
    private static Line line(double x1, double y1, double x2, double y2,
                             String color, double width) {
        Line shape = new Line(x1, y1, x2, y2);
        shape.setStroke(Color.web(color));
        shape.setStrokeWidth(width);
        // Rounded end caps make lines look softer than square-ended strokes.
        shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        return shape;
    }

    // Reuse one shadow recipe; 0.13 is its opacity, keeping the effect subtle.
    private static DropShadow shadow() {
        return new DropShadow(12, 0, 7, Color.web("#28382e", 0.13));
    }
}
