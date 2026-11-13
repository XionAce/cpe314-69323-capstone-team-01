package edu.uc.campusmarket.service;

import java.sql.SQLException;

/** Converts data-access failures at the service boundary. */
public final class ServiceCall {
    private ServiceCall() {
    }

    /**
     * A database operation whose checked SQL failure becomes a user-facing
     * AppException.
     */
    @FunctionalInterface
    public interface Query<T> {
        /**
         * Executes the prepared database operation and returns its value.
         *
         * @return the operation result
         * @throws SQLException if database access fails
         */
        T execute() throws SQLException;
    }

    /**
     * Executes a data operation and translates SQL failures into a user-facing
     * error.
     *
     * @param action database work to execute
     * @param <T>    result type
     * @return the operation result
     * @throws AppException if database access fails
     */
    public static <T> T get(Query<T> action) {
        try {
            return action.execute();
        } catch (SQLException error) {
            throw new AppException("Database operation could not finish: " + error.getMessage(),
                    error);
        }
    }
}
