// This model describes electronic listings without depending on JavaFX or SQL.
package edu.uc.campusmarket.model;

// This concrete, final subclass inherits the common Listing fields and supplies electronic rules.
public final class Electronics extends Listing {
    // private protects the fields; final fixes them after construction; boolean means true/false.
    private final boolean powersOn;
    private final boolean hasFunctionalDefect;
    private final boolean hasCosmeticDamage;

    // Constructors create a valid object from shared data and this type's condition evidence.
    public Electronics(long id, long sellerId, long categoryId, String title,
                       long priceMinor, String currency, String status,
                       boolean powersOn, boolean hasFunctionalDefect, boolean hasCosmeticDamage) {
        // Let the parent validate and store all the common fields first.
        super(id, sellerId, categoryId, title, priceMinor, currency, status);
        // this.field identifies this object's field rather than the equally named parameter.
        this.powersOn = powersOn;
        this.hasFunctionalDefect = hasFunctionalDefect;
        this.hasCosmeticDamage = hasCosmeticDamage;
    }

    // Public getters expose condition evidence to display/save code without setters.
    public boolean powersOn() {
        return powersOn;
    }

    // A functional defect means a feature does not work properly.
    public boolean hasFunctionalDefect() {
        return hasFunctionalDefect;
    }

    // Cosmetic damage concerns appearance, such as a scratch.
    public boolean hasCosmeticDamage() {
        return hasCosmeticDamage;
    }

    // @Override checks that this implements the abstract method in Listing.
    @Override
    public ListingType type() {
        return ListingType.ELECTRONICS;
    }

    // A Listing reference uses this rule when its actual object is Electronics (polymorphism).
    @Override
    public ConditionGrade gradeCondition() {
        // ! means NOT and || means OR: failure to power on or a functional defect gives POOR.
        if (!powersOn || hasFunctionalDefect) {
            return ConditionGrade.POOR;
        }
        // The ?: expression chooses GOOD for cosmetic damage, otherwise LIKE_NEW.
        return hasCosmeticDamage ? ConditionGrade.GOOD : ConditionGrade.LIKE_NEW;
    }
}
