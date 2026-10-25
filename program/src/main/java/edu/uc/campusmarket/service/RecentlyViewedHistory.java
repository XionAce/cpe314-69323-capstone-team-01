// This service owns each member's session-only recently viewed collection.
package edu.uc.campusmarket.service;

// Use the standard double-ended queue as a stack and return snapshots as Lists.
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Bounded session history, with the most recently viewed listing at the stack top. */
public final class RecentlyViewedHistory {
    // private hides internal state; final means this maximum cannot change after construction.
    private final int capacity;
    // A Deque allows both ends to be used. push() uses the front as the stack's top.
    // <Long> holds listing IDs; ArrayDeque is the concrete collection implementation.
    private final Deque<Long> listingIds = new ArrayDeque<>();

    // The caller chooses how many distinct recent listings this history remembers.
    public RecentlyViewedHistory(int capacity) {
        // A history with no allowed entries or a negative size would be invalid.
        if (capacity <= 0) {
            throw new IllegalArgumentException("History capacity must be positive.");
        }
        // this.capacity is the object's field; capacity on the right is the argument.
        this.capacity = capacity;
    }

    // Record a view without saving it to SQLite; closing the app discards this session history.
    public void record(long listingId) {
        // Recently viewed should only refer to real saved listings, whose IDs are positive.
        if (listingId <= 0) {
            throw new IllegalArgumentException("Listing ID must be positive.");
        }
        // Remove a previous occurrence so opening the same listing does not create duplicates.
        listingIds.remove(listingId);
        // Put this item on top: the last item viewed becomes the first displayed (LIFO behavior).
        listingIds.push(listingId);
        // Keep memory bounded. Repeatedly discard the oldest item if capacity is exceeded.
        while (listingIds.size() > capacity) {
            listingIds.removeLast();
        }
    }

    /** Returns an immutable snapshot in newest-to-oldest (LIFO) order. */
    public List<Long> ids() {
        // A copied unmodifiable List prevents external code from altering the internal stack.
        return List.copyOf(listingIds);
    }
}
