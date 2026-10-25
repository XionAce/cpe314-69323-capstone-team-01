// Keep textbook-specific data and grading in the model layer, independent of the screen.
package edu.uc.campusmarket.model;

// extends inherits Listing's shared data/methods; final prevents further subclasses of Textbook.
public final class Textbook extends Listing {
    // Each private final field stores one piece of condition evidence, set at construction.
    private final int missingPages;
    // WearLevel is an enum: the allowed markings are NONE, LIGHT, or HEAVY.
    private final WearLevel markingLevel;
    // boolean stores a true/false answer to whether the cover is damaged.
    private final boolean coverDamaged;

    // The constructor receives shared listing information followed by textbook-only evidence.
    public Textbook(long id, long sellerId, long categoryId, String title,
                    long priceMinor, String currency, String status,
                    int missingPages, WearLevel markingLevel, boolean coverDamaged) {
        // super(...) calls Listing's constructor to initialize and validate the inherited fields.
        super(id, sellerId, categoryId, title, priceMinor, currency, status);
        // A page count may be zero, but a negative count makes no sense.
        if (missingPages < 0) {
            throw new IllegalArgumentException("Missing pages must not be negative.");
        }
        // this distinguishes object fields on the left from parameters on the right.
        this.missingPages = missingPages;
        // Reject null: a condition decision needs a known marking level.
        this.markingLevel = ModelValidation.required(markingLevel, "Marking level");
        this.coverDamaged = coverDamaged;
    }

    // Getter methods let the interface and repository read evidence without changing this object.
    public int missingPages() {
        return missingPages;
    }

    // Read the selected marking level.
    public WearLevel markingLevel() {
        return markingLevel;
    }

    // Read the cover-damage flag.
    public boolean coverDamaged() {
        return coverDamaged;
    }

    // @Override lets the compiler check that this implements a method declared in Listing.
    @Override
    public ListingType type() {
        // Give the interface/database an explicit item type.
        return ListingType.TEXTBOOK;
    }

    // Rules are ordered from the most serious defect to the best condition.
    @Override
    public ConditionGrade gradeCondition() {
        // Missing even one page takes priority over all cosmetic evidence.
        if (missingPages > 0) {
            return ConditionGrade.POOR;
        }
        // This runs only when there are no missing pages; == compares enum constants.
        if (markingLevel == WearLevel.HEAVY) {
            return ConditionGrade.FAIR;
        }
        // || means OR: light markings or a damaged cover is enough for GOOD.
        if (markingLevel == WearLevel.LIGHT || coverDamaged) {
            return ConditionGrade.GOOD;
        }
        // No earlier defect matched, so the book receives the best available grade.
        return ConditionGrade.LIKE_NEW;
    }
}
