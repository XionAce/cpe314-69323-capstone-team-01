package edu.uc.campusmarket.service;

import edu.uc.campusmarket.model.Electronics;
import edu.uc.campusmarket.model.Furniture;
import edu.uc.campusmarket.model.Listing;
import edu.uc.campusmarket.model.Textbook;
import edu.uc.campusmarket.model.WearLevel;
import java.math.BigDecimal;
import java.util.List;

/**
 * Converts a plain posting form and observed condition into the correct
 * subtype.
 */
public final class ListingFactory {
    private ListingFactory() {
    }

    /**
     * Creates the concrete listing subtype from the observed condition and common
     * fields.
     */
    public static Listing create(long id, long seller, long category, String title,
            BigDecimal price, String currency, String status, String type, String evidence) {
        if (type.equals("TEXTBOOK")) {
            WearLevel markings = WearLevel.NONE;
            if (evidence.equals("Light markings")) {
                markings = WearLevel.LIGHT;
            }
            if (evidence.equals("Heavy markings")) {
                markings = WearLevel.HEAVY;
            }
            // One missing page represents the chosen observation in this skeleton form.
            return new Textbook(id, seller, category, title, price, currency, status,
                    evidence.equals("Missing pages") ? 1 : 0, markings,
                    evidence.equals("Damaged cover"));
        }
        if (type.equals("ELECTRONICS")) {
            return new Electronics(id, seller, category, title, price, currency, status,
                    !evidence.equals("Does not power on"), evidence.equals("Functional defect"),
                    evidence.equals("Cosmetic damage"));
        }
        WearLevel wear = WearLevel.NONE;
        if (evidence.equals("Light wear")) {
            wear = WearLevel.LIGHT;
        }
        if (evidence.equals("Heavy wear")) {
            wear = WearLevel.HEAVY;
        }
        return new Furniture(id, seller, category, title, price, currency, status,
                !evidence.equals("Unstable"), evidence.equals("Structural damage"), wear);
    }

    /** Provides the observations operation for this component. */
    public static List<String> observations(String itemType) {
        if (itemType.equals("TEXTBOOK")) {
            return List.of("No visible damage", "Light markings", "Heavy markings", "Damaged cover",
                    "Missing pages");
        }
        if (itemType.equals("ELECTRONICS")) {
            return List.of("No visible damage", "Cosmetic damage", "Functional defect",
                    "Does not power on");
        }
        return List.of("No visible damage", "Light wear", "Heavy wear", "Structural damage",
                "Unstable");
    }
}
