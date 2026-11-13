package edu.uc.campusmarket.model;

import java.util.List;

/** Immutable account-scoped dashboard data from one database snapshot. */
public record HomeSummary(List<HomeItem> listings, List<HomeItem> waitlists, int reviewCount,
        double averageRating) {
    /** Copies the lists so background results cannot be modified after delivery. */
    public HomeSummary {
        listings = List.copyOf(listings);
        waitlists = List.copyOf(waitlists);
    }
}
