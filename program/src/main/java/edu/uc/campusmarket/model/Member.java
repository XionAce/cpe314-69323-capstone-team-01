package edu.uc.campusmarket.model;

public class Member {
    private final long id;
    private final String name;

    public Member(long id, String name) {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new IllegalArgumentException("A member needs a positive ID and a name.");
        }
        this.id = id;
        this.name = name.strip();
    }

    public long getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}
