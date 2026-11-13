package edu.uc.campusmarket.model;

import java.math.BigDecimal;

/** Immutable data passed between the application layers. */
public record ListingForRepair(long id, long sellerId, long categoryId, String title,
        BigDecimal price, String currency, String status, String type) {
    /** Returns the readable label used when displaying this object. */
    @Override
    public String toString() {
        return "#" + id + " - " + title + " (" + type + ")";
    }
}
