package edu.uc.campusmarket.model;

import java.math.BigDecimal;

public class Textbook extends Listing {
    private final int missingPages;
    private final WearLevel markingLevel;
    private final boolean coverDamaged;

    public Textbook(long id, long sellerId, long categoryId, String title,
                    BigDecimal price, String currency, String status,
                    int missingPages, WearLevel markingLevel, boolean coverDamaged) {
        super(id, sellerId, categoryId, title, price, currency, status);
        if (missingPages < 0 || markingLevel == null) {
            throw new IllegalArgumentException("Missing pages cannot be negative; choose a marking level.");
        }
        this.missingPages = missingPages;
        this.markingLevel = markingLevel;
        this.coverDamaged = coverDamaged;
    }

    public int getMissingPages() { return missingPages; }
    public WearLevel getMarkingLevel() { return markingLevel; }
    public boolean isCoverDamaged() { return coverDamaged; }

    @Override
    public String getType() { return "TEXTBOOK"; }

    @Override
    public ConditionGrade gradeCondition() {
        // Serious defects take priority, so check them first.
        if (missingPages > 0) { return ConditionGrade.POOR; }
        if (markingLevel == WearLevel.HEAVY) { return ConditionGrade.FAIR; }
        if (markingLevel == WearLevel.LIGHT || coverDamaged) { return ConditionGrade.GOOD; }
        return ConditionGrade.LIKE_NEW;
    }
}
