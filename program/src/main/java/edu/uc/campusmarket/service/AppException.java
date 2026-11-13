package edu.uc.campusmarket.service;

/** An operation failed; controllers expose its message without knowing JDBC. */
public class AppException extends RuntimeException {
    /** Creates this component from the supplied values or dependencies. */
    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
