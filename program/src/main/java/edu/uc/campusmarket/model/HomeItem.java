package edu.uc.campusmarket.model;

/**
 * A personal listing row; count is waiting buyers or the member's live
 * position.
 */
public record HomeItem(long listingId, String title, String status, int count, String buyers) {
}
