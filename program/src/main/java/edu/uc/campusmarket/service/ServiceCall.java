package edu.uc.campusmarket.service;

import java.sql.SQLException;

/** Converts data-access failures at the service boundary. */
public final class ServiceCall {
    private ServiceCall() {
    }

    @FunctionalInterface
    public interface Query<T> {
        T execute() throws SQLException;
    }

    /** Returns the . */
    public static <T> T get(Query<T> action) {
        try {
            return action.execute();
        } catch (SQLException error) {
            throw new AppException("Database operation could not finish: " + error.getMessage(),
                    error);
        }
    }
}
