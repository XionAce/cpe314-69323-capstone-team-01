package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Grades a textbook using missing pages, markings and cover damage. */
public class Textbook extends Listing {
    private final int missingPages;
    private final WearLevel markingLevel;
    private final boolean coverDamaged;

    /** Creates this component from the supplied values or dependencies. */
    public Textbook(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, int missingPages, WearLevel markingLevel,
            boolean coverDamaged) {
        this(id, sellerId, categoryId, title, price, currency, status, missingPages, markingLevel,
                coverDamaged, id == 0 ? Instant.now() : null);
    }

    /** Reconstructs the saved subtype with its original creation time. */
    public Textbook(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, int missingPages, WearLevel markingLevel,
            boolean coverDamaged, Instant createdAt) {
        super(id, sellerId, categoryId, title, price, currency, status, createdAt);
        if (missingPages < 0 || markingLevel == null) {
            throw new IllegalArgumentException(
                    "Missing pages cannot be negative; choose a marking level.");
        }
        this.missingPages = missingPages;
        this.markingLevel = markingLevel;
        this.coverDamaged = coverDamaged;
    }

    /** Returns the missing pages. */
    public int getMissingPages() {
        return missingPages;
    }

    /** Returns the marking level. */
    public WearLevel getMarkingLevel() {
        return markingLevel;
    }

    /** Provides the is cover damaged operation for this component. */
    public boolean isCoverDamaged() {
        return coverDamaged;
    }

    /** Returns the type. */
    @Override
    public String getType() {
        return "TEXTBOOK";
    }

    /** Calculates the condition grade using this listing subtype's evidence. */
    @Override
    public ConditionGrade gradeCondition() {
        // Serious defects take priority, so check them first.
        if (missingPages > 0) {
            return ConditionGrade.POOR;
        }
        if (markingLevel == WearLevel.HEAVY) {
            return ConditionGrade.FAIR;
        }
        if (markingLevel == WearLevel.LIGHT || coverDamaged) {
            return ConditionGrade.GOOD;
        }
        return ConditionGrade.LIKE_NEW;
    }
}
