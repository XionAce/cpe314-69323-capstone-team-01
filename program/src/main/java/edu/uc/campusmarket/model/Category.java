package edu.uc.campusmarket.model;

/** A category with an optional parent, used by recursive search. */
public class Category {
    private final long id;
    private final Long parentId;
    private final String name;

    /** Creates this component from the supplied values or dependencies. */
    public Category(long id, Long parentId, String name) {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A category needs a positive ID and a name.");
        }
        if (parentId != null && (parentId <= 0 || parentId == id)) {
            throw new IllegalArgumentException("A category needs a different, positive parent ID.");
        }
        this.id = id;
        this.parentId = parentId; // null means this category is a root.
        this.name = name.strip();
    }

    /** Returns the id. */
    public long getId() {
        return id;
    }

    /** Returns the parent id. */
    public Long getParentId() {
        return parentId;
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
