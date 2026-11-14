package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record InitialAccount(String username, String password, String recoveryCode) {
}
