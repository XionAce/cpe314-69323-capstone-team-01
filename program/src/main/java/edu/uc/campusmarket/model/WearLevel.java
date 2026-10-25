// Textbooks and furniture use these fixed choices to describe markings or surface wear.
package edu.uc.campusmarket.model;

// An enum is a small set of allowed constants; it is safer than arbitrary free-text labels.
public enum WearLevel {
    // These are evidence choices, not final grades; each listing type decides how to grade them.
    NONE,
    LIGHT,
    HEAVY
}
