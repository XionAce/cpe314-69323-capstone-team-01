package edu.uc.campusmarket.model;

public class Category {
    private final long id;
    private final Long parentId;
    private final String name;

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

    public long getId() { return id; }
    public Long getParentId() { return parentId; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}
