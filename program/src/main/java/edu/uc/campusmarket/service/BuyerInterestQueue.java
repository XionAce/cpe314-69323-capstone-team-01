// Services implement actions and collection rules independently of JavaFX controls.
package edu.uc.campusmarket.service;

// Import the waitlist-entry data model and standard Java collection types used below.
import edu.uc.campusmarket.model.BuyerInterest;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/** A FIFO waitlist for one listing. The database remains the source of persisted entries. */
// final prevents subclassing; this object enforces queue rules rather than displaying a screen.
public final class BuyerInterestQueue {
    // Queue is the contract; ArrayDeque is its concrete implementation. Add at the tail, read/remove at the head.
    // <BuyerInterest> restricts elements to buyer-interest records; <> infers the same type on the right.
    private final Queue<BuyerInterest> entries = new ArrayDeque<>();
    // A Set keeps each buyer ID once and supports quick duplicate-membership checks.
    private final Set<Long> activeBuyers = new HashSet<>();
    // Zero initially means no listing has been associated with this queue yet.
    private long listingId;
    // Remember the greatest accepted position so later entries cannot jump ahead.
    private long lastJoiningOrder;

    /** Input is defensively sorted by joining order; duplicate orders are rejected. */
    public BuyerInterestQueue(List<BuyerInterest> orderedEntries) {
        // stream() processes the list; the lambda entry -> ... checks each element for null.
        // || short-circuits so a null list is rejected without calling a method on it.
        if (orderedEntries == null || orderedEntries.stream().anyMatch(entry -> entry == null)) {
            throw new IllegalArgumentException("Waitlist entries must not be null or contain null entries.");
        }
        // Method reference BuyerInterest::joiningOrder reads each entry's position for sorting.
        // this::enqueue passes each sorted entry through the same validations used for new entries.
        orderedEntries.stream().sorted(Comparator.comparingLong(BuyerInterest::joiningOrder))
                .forEach(this::enqueue);
    }

    // enqueue means join the back of this in-memory queue; it does not itself write to SQLite.
    public void enqueue(BuyerInterest entry) {
        // An entry is required before its IDs/order can be read safely.
        if (entry == null) {
            throw new IllegalArgumentException("A buyer interest is required.");
        }
        // && means AND: once initialized, reject entries belonging to a different listing.
        if (listingId != 0 && listingId != entry.listingId()) {
            throw new IllegalArgumentException("A waitlist can contain only one listing.");
        }
        // One buyer cannot hold multiple active positions for the same item.
        if (activeBuyers.contains(entry.buyerId())) {
            throw new IllegalArgumentException("This buyer is already waiting for the listing.");
        }
        // Reject repeated or decreasing order numbers rather than allowing a buyer to cut in line.
        if (entry.joiningOrder() <= lastJoiningOrder) {
            throw new IllegalArgumentException("New entries must have a larger joining order.");
        }
        // Update queue metadata only after every validation has passed.
        listingId = entry.listingId();
        lastJoiningOrder = entry.joiningOrder();
        // Queue.add appends at the back: previously waiting buyers remain ahead.
        entries.add(entry);
        // Record this buyer in the duplicate-detection Set too.
        activeBuyers.add(entry.buyerId());
    }

    // peek reads the first waiting buyer without removing them.
    public Optional<BuyerInterest> peek() {
        // Optional represents either an entry or an empty result when nobody is waiting.
        return Optional.ofNullable(entries.peek());
    }

    // poll removes/returns the front entry, demonstrating first in, first out (FIFO).
    // This changes only this queue object; it is not a completed sale or a database delete.
    public Optional<BuyerInterest> poll() {
        BuyerInterest first = entries.poll();
        // A nonempty removal must also release that buyer from the active-buyer Set.
        if (first != null) {
            activeBuyers.remove(first.buyerId());
        }
        // Return Optional.empty() if the queue was already empty.
        return Optional.ofNullable(first);
    }

    // Expose a read-only snapshot in front-to-back order so callers cannot bypass enqueue checks.
    public List<BuyerInterest> entries() {
        return List.copyOf(entries);
    }
}
