package edu.uc.campusmarket.model;

/**
 * Public member identity; credentials remain in the account data-access layer.
 */
public class Member {
    private final long id;
    private final String name;

    /** Creates this component from the supplied values or dependencies. */
    public Member(long id, String name) {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A member needs a positive ID and a name.");
        }
        this.id = id;
        this.name = name.strip();
    }

    /** Returns the id. */
    public long getId() {
        return id;
    }

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Returns the readable label used when displaying this object. */
    @Override
    public String toString() {
        return name;
    }
}
