// Furniture-specific condition evidence belongs with the model's grading rules.
package edu.uc.campusmarket.model;

// extends reuses Listing's common data; this final class cannot have another subclass.
public final class Furniture extends Listing {
    // private final fields can be read through getters but cannot be reassigned after construction.
    private final boolean structurallyStable;
    private final boolean hasStructuralDamage;
    // WearLevel limits the surface evidence to NONE, LIGHT, or HEAVY.
    private final WearLevel surfaceWear;

    // A constructor combines common listing data with furniture-only evidence.
    public Furniture(long id, long sellerId, long categoryId, String title,
                     long priceMinor, String currency, String status,
                     boolean structurallyStable, boolean hasStructuralDamage, WearLevel surfaceWear) {
        // Call the parent's constructor to validate and initialize inherited fields.
        super(id, sellerId, categoryId, title, priceMinor, currency, status);
        // Assign the supplied true/false evidence to this object.
        this.structurallyStable = structurallyStable;
        this.hasStructuralDamage = hasStructuralDamage;
        // Require a known wear level rather than letting null reach the switch below.
        this.surfaceWear = ModelValidation.required(surfaceWear, "Surface wear");
    }

    // Getter methods return evidence without allowing another class to change it.
    public boolean structurallyStable() {
        return structurallyStable;
    }

    // Read whether the item's structure has damage.
    public boolean hasStructuralDamage() {
        return hasStructuralDamage;
    }

    // Read the surface-wear choice.
    public WearLevel surfaceWear() {
        return surfaceWear;
    }

    // @Override implements the abstract type() contract declared by Listing.
    @Override
    public ListingType type() {
        return ListingType.FURNITURE;
    }

    // The actual furniture object supplies these rules even through a Listing reference.
    @Override
    public ConditionGrade gradeCondition() {
        // ! means NOT, || means OR. Structural problems override any surface-wear grade.
        if (!structurallyStable || hasStructuralDamage) {
            return ConditionGrade.POOR;
        }
        // This switch expression returns the grade corresponding to the selected enum value.
        return switch (surfaceWear) {
            // Each arrow supplies one result, so there is no fall-through to another case.
            case HEAVY -> ConditionGrade.FAIR;
            case LIGHT -> ConditionGrade.GOOD;
            case NONE -> ConditionGrade.LIKE_NEW;
        };
    }
}
