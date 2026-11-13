package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record Review(long id, long listingId, long reviewerId, String reviewerName, int rating,
        String comment) {
}
