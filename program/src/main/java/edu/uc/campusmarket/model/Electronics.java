package edu.uc.campusmarket.model;

import java.math.BigDecimal;

public class Electronics extends Listing {
    private final boolean powersOn;
    private final boolean functionalDefect;
    private final boolean cosmeticDamage;

    public Electronics(long id, long sellerId, long categoryId, String title,
                       BigDecimal price, String currency, String status,
                       boolean powersOn, boolean functionalDefect, boolean cosmeticDamage) {
        super(id, sellerId, categoryId, title, price, currency, status);
        this.powersOn = powersOn;
        this.functionalDefect = functionalDefect;
        this.cosmeticDamage = cosmeticDamage;
    }

    public boolean isPowersOn() { return powersOn; }
    public boolean hasFunctionalDefect() { return functionalDefect; }
    public boolean hasCosmeticDamage() { return cosmeticDamage; }

    @Override
    public String getType() { return "ELECTRONICS"; }

    @Override
    public ConditionGrade gradeCondition() {
        if (!powersOn || functionalDefect) { return ConditionGrade.POOR; }
        if (cosmeticDamage) { return ConditionGrade.GOOD; }
        return ConditionGrade.LIKE_NEW;
    }
}
