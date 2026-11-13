package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record CategorySummary(String category, String currency, double average, int count) {
}
