package edu.uc.campusmarket.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

/**
 * Draws the existing category illustrations without loading external images.
 */
public final class ItemArtwork {
    private ItemArtwork() {
    }

    /** Draws a category illustration using JavaFX shapes. */
    public static Canvas draw(String type) {
        Canvas canvas = new Canvas(170, 100);
        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#d9d4ed"));
        g.fillOval(24, 75, 125, 13);
        if (type.equals("TEXTBOOK")) {
            g.setFill(Color.web("#f0b875"));
            g.fillRoundRect(39, 16, 73, 65, 6, 6);
            g.setFill(Color.web("#7962ba"));
            g.fillRoundRect(57, 8, 73, 72, 6, 6);
            g.setFill(Color.web("#ebe4ff"));
            g.fillRect(66, 8, 4, 72);
            g.setFill(Color.WHITE);
            g.fillRoundRect(80, 24, 36, 5, 3, 3);
            g.fillRoundRect(80, 36, 23, 4, 3, 3);
        } else if (type.equals("ELECTRONICS")) {
            g.setFill(Color.web("#49445f"));
            g.fillRoundRect(35, 10, 103, 66, 7, 7);
            g.setFill(Color.web("#b5c4f0"));
            g.fillRoundRect(41, 16, 91, 53, 3, 3);
            g.setFill(Color.web("#e8edff"));
            g.fillOval(71, 23, 34, 34);
            g.setFill(Color.web("#8b87a8"));
            g.fillRoundRect(24, 76, 126, 8, 5, 5);
        } else {
            g.setFill(Color.web("#d6a069"));
            g.fillRoundRect(56, 8, 61, 43, 12, 12);
            g.setFill(Color.web("#8e6851"));
            g.fillRoundRect(49, 48, 75, 12, 5, 5);
            g.fillRect(55, 59, 7, 24);
            g.fillRect(111, 59, 7, 24);
        }
        return canvas;
    }
}
