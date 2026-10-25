// A member is a person who can browse, express interest, and post items.
package edu.uc.campusmarket.model;

// record generates final component fields and accessors id()/name(), plus equality helpers.
// This prototype switches between demo members; this record does not implement authentication.
public record Member(long id, String name) {
    // The compact constructor runs validation before assigning the generated fields.
    public Member {
        // Members referenced by saved records must have an ID greater than zero.
        ModelValidation.positive(id, "Member ID");
        // Keep a nonblank name without extra spaces at either end.
        name = ModelValidation.text(name, "Member name");
    }
}
