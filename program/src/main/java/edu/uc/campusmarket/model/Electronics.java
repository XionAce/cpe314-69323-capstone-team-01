package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import java.time.Instant;

/** Grades electronics using power, functional defects and cosmetic evidence. */
public class Electronics extends Listing {
    private final boolean powersOn;
    private final boolean functionalDefect;
    private final boolean cosmeticDamage;

    /** Creates this component from the supplied values or dependencies. */
    public Electronics(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, boolean powersOn, boolean functionalDefect,
            boolean cosmeticDamage) {
        this(id, sellerId, categoryId, title, price, currency, status, powersOn, functionalDefect,
                cosmeticDamage, id == 0 ? Instant.now() : null);
    }

    /** Reconstructs the saved subtype with its original creation time. */
    public Electronics(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, boolean powersOn, boolean functionalDefect,
            boolean cosmeticDamage, Instant createdAt) {
        super(id, sellerId, categoryId, title, price, currency, status, createdAt);
        this.powersOn = powersOn;
        this.functionalDefect = functionalDefect;
        this.cosmeticDamage = cosmeticDamage;
    }

    /** Provides the is powers on operation for this component. */
    public boolean isPowersOn() {
        return powersOn;
    }

    /** Provides the has functional defect operation for this component. */
    public boolean hasFunctionalDefect() {
        return functionalDefect;
    }

    /** Provides the has cosmetic damage operation for this component. */
    public boolean hasCosmeticDamage() {
        return cosmeticDamage;
    }

    /** Returns the type. */
    @Override
    public String getType() {
        return "ELECTRONICS";
    }

    /** Calculates the condition grade using this listing subtype's evidence. */
    @Override
    public ConditionGrade gradeCondition() {
        if (!powersOn || functionalDefect) {
            return ConditionGrade.POOR;
        }
        if (cosmeticDamage) {
            return ConditionGrade.GOOD;
        }
        return ConditionGrade.LIKE_NEW;
    }
}
