package edu.uc.campusmarket.model;

/**
 * A catalog outcome with an explicit status and a safe message for the form.
 */
public record CatalogSuggestion(Status status, String message) {
    /** Distinguishes useful matches, valid empty results and failed requests. */
    public enum Status {
        OK, EMPTY, FAILED
    }
}
