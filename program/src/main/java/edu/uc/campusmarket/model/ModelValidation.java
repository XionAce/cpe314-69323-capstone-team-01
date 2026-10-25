// Reusable checks keep model constructors consistent and avoid repeating validation code.
package edu.uc.campusmarket.model;

// No public keyword means only this package can use this helper class; final prevents subclasses.
final class ModelValidation {
    // A private constructor prevents creating an unnecessary ModelValidation object.
    private ModelValidation() {
    }

    // static means call ModelValidation.positive(...) without constructing a helper instance.
    // The field argument supplies a useful name in the error message.
    static long positive(long value, String field) {
        // <= means less than or equal to: both zero and negative values are rejected.
        if (value <= 0) {
            // An exception stops the current invalid action and carries a readable explanation.
            throw new IllegalArgumentException(field + " must be positive.");
        }
        // Returning a valid value allows a constructor to validate and assign in one statement.
        return value;
    }

    // Unlike positive(), this permits zero for objects that have not been saved yet.
    static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative.");
        }
        return value;
    }

    // Validate text centrally so all model objects apply the same whitespace rule.
    static String text(String value, String field) {
        // || short-circuits: when value is null, Java does not try to call isBlank() on it.
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        // strip() removes leading/trailing whitespace but keeps meaningful spaces inside text.
        return value.strip();
    }

    // <T> is a generic type parameter: this method accepts and returns the same reference type.
    // It is useful for required enum choices such as WearLevel, without a helper for each type.
    static <T> T required(T value, String field) {
        // null means there is no object/value reference, so the required choice is missing.
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        // Preserve the original value and its type after the check succeeds.
        return value;
    }
}
