package edu.uc.campusmarket.model;

import java.math.BigDecimal;

public class Furniture extends Listing {
    private final boolean stable;
    private final boolean structuralDamage;
    private final WearLevel surfaceWear;

    public Furniture(long id, long sellerId, long categoryId, String title,
                     BigDecimal price, String currency, String status,
                     boolean stable, boolean structuralDamage, WearLevel surfaceWear) {
        super(id, sellerId, categoryId, title, price, currency, status);
        if (surfaceWear == null) {
            throw new IllegalArgumentException("Choose a surface wear level.");
        }
        this.stable = stable;
        this.structuralDamage = structuralDamage;
        this.surfaceWear = surfaceWear;
    }

    public boolean isStable() { return stable; }
    public boolean hasStructuralDamage() { return structuralDamage; }
    public WearLevel getSurfaceWear() { return surfaceWear; }

    @Override
    public String getType() { return "FURNITURE"; }

    @Override
    public ConditionGrade gradeCondition() {
        if (!stable || structuralDamage) { return ConditionGrade.POOR; }
        if (surfaceWear == WearLevel.HEAVY) { return ConditionGrade.FAIR; }
        if (surfaceWear == WearLevel.LIGHT) { return ConditionGrade.GOOD; }
        return ConditionGrade.LIKE_NEW;
    }
}
