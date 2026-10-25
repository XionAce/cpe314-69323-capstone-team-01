// Model records hold values that the services and database exchange.
package edu.uc.campusmarket.model;

/** An ID of zero identifies an interest that has not been persisted yet. */
// record automatically supplies private final fields, a constructor, getters, equals, and toString.
// Getter names match the components, for example buyerId(). This is data, not the queue itself.
// listingId identifies the item; buyerId identifies the member; joiningOrder determines FIFO order.
public record BuyerInterest(long id, long listingId, long buyerId, long joiningOrder) {
    // A compact record constructor validates parameters before Java assigns them to the fields.
    public BuyerInterest {
        // Unsaved records can use zero; negative record IDs are never valid.
        ModelValidation.nonNegative(id, "Buyer interest ID");
        // A waitlist entry must refer to saved listing/member IDs and a positive order number.
        ModelValidation.positive(listingId, "Listing ID");
        ModelValidation.positive(buyerId, "Buyer ID");
        ModelValidation.positive(joiningOrder, "Joining order");
    }
}
