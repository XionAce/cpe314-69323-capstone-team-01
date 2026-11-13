package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record WeeklySales(java.time.LocalDate weekStarting, int count) {
}
