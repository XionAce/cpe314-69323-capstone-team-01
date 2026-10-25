// All listing types return a value from this shared set of grades.
package edu.uc.campusmarket.model;

// enum defines fixed named choices; it prevents misspellings such as "GOD" instead of "GOOD".
public enum ConditionGrade {
    // Subclasses calculate one of these values from their own condition evidence.
    LIKE_NEW,
    GOOD,
    FAIR,
    POOR
}
