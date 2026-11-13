package edu.uc.campusmarket.service;

import edu.uc.campusmarket.dao.HomeDao;
import edu.uc.campusmarket.model.HomeSummary;

/**
 * Provides an account-scoped home snapshot without exposing database errors to
 * views.
 */
public class HomeService {
    private final HomeDao home;

    /** Receives the dashboard DAO from the composition root. */
    public HomeService(HomeDao home) {
        this.home = home;
    }

    /** Reads the requested existing member's home data. */
    public HomeSummary loadFor(long member) {
        return ServiceCall.get(() -> home.loadFor(member));
    }
}
