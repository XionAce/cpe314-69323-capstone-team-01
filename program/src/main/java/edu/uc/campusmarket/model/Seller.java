// A seller profile belongs to a member; it does not make that member unable to buy.
package edu.uc.campusmarket.model;

// record generates final component fields and accessors, for example memberId() and displayName().
// id is the seller-profile ID; memberId identifies its owner. They are not interchangeable.
public record Seller(long id, long memberId, String displayName, String sellerKind) {
    // A compact record constructor validates/normalizes values before fields are assigned.
    public Seller {
        // A profile and its owning member both need valid positive database IDs.
        ModelValidation.positive(id, "Seller ID");
        ModelValidation.positive(memberId, "Seller member ID");
        // Text helpers reject null/blank inputs and strip spaces around accepted text.
        displayName = ModelValidation.text(displayName, "Seller display name");
        // sellerKind is descriptive text for the profile, not a restriction on buying.
        sellerKind = ModelValidation.text(sellerKind, "Seller kind");
    }
}
