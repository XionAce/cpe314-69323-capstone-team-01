// Item types are separate from the category tree: Electronics can have many nested categories.
package edu.uc.campusmarket.model;

// enum restricts this value to the three supported OOP listing types.
public enum ListingType {
    // The interface selects a form and the repository reconstructs a subtype using this value.
    TEXTBOOK,
    ELECTRONICS,
    FURNITURE
}
