package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Grades furniture using stability, structural damage and surface wear. */
public class Furniture extends Listing {
    private final boolean stable;
    private final boolean structuralDamage;
    private final WearLevel surfaceWear;

    /** Creates this component from the supplied values or dependencies. */
    public Furniture(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, boolean stable, boolean structuralDamage,
            WearLevel surfaceWear) {
        this(id, sellerId, categoryId, title, price, currency, status, stable, structuralDamage,
                surfaceWear, id == 0 ? Instant.now() : null);
    }

    /** Reconstructs the saved subtype with its original creation time. */
    public Furniture(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, boolean stable, boolean structuralDamage,
            WearLevel surfaceWear, Instant createdAt) {
        super(id, sellerId, categoryId, title, price, currency, status, createdAt);
        if (surfaceWear == null) {
            throw new IllegalArgumentException("Choose a surface wear level.");
        }
        this.stable = stable;
        this.structuralDamage = structuralDamage;
        this.surfaceWear = surfaceWear;
    }

    /** Provides the is stable operation for this component. */
    public boolean isStable() {
        return stable;
    }

    /** Provides the has structural damage operation for this component. */
    public boolean hasStructuralDamage() {
        return structuralDamage;
    }

    /** Returns the surface wear. */
    public WearLevel getSurfaceWear() {
        return surfaceWear;
    }

    /** Returns the type. */
    @Override
    public String getType() {
        return "FURNITURE";
    }

    /** Calculates the condition grade using this listing subtype's evidence. */
    @Override
    public ConditionGrade gradeCondition() {
        if (!stable || structuralDamage) {
            return ConditionGrade.POOR;
        }
        if (surfaceWear == WearLevel.HEAVY) {
            return ConditionGrade.FAIR;
        }
        if (surfaceWear == WearLevel.LIGHT) {
            return ConditionGrade.GOOD;
        }
        return ConditionGrade.LIKE_NEW;
    }
}
