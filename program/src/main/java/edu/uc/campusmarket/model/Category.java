// Each category is one node of the marketplace's category tree.
package edu.uc.campusmarket.model;

// record generates immutable component fields and accessors such as id(), parentId(), and name().
// Long (capital L) can be null, unlike primitive long: null means this category is a root.
public record Category(long id, Long parentId, String name) {
    // This compact constructor checks the values before the generated record assignment.
    public Category {
        // Categories used by listings must have a saved, positive ID.
        ModelValidation.positive(id, "Category ID");
        // Validate a parent only when there is one; a root has no parent to check.
        if (parentId != null) {
            ModelValidation.positive(parentId, "Parent category ID");
            // This catches a direct self-link; CategoryService catches longer cycles across nodes.
            if (parentId == id) {
                throw new IllegalArgumentException("A category cannot be its own parent.");
            }
        }
        // Strip surrounding whitespace and reject a blank/null category name.
        name = ModelValidation.text(name, "Category name");
    }
}
