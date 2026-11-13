package edu.uc.campusmarket.model;

/** Immutable data passed between the application layers. */
public record Registration(Member member, String username, String recoveryCode) {
}
