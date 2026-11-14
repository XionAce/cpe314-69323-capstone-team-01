package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record Seller(long id, long memberId, String displayName, String sellerKind) {
}
