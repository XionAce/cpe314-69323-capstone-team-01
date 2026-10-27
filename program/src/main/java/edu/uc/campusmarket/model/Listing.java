package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

// The parent stores data common to every item. It cannot be instantiated directly.
public abstract class Listing {
    private final long id;
    private final long sellerId;
    private final long categoryId;
    private final String title;
    private final BigDecimal price;
    private final String currency;
    private final String status;

    public Listing(long id, long sellerId, long categoryId, String title,
                   BigDecimal price, String currency, String status) {
        if (id < 0 || sellerId <= 0 || categoryId <= 0) {
            throw new IllegalArgumentException("Use ID 0 for a new item and positive seller/category IDs.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Enter an item title.");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        try {
            // Store exact cents: 350.50 becomes 35050 in SQLite. Never silently round money.
            this.price = price.setScale(2, RoundingMode.UNNECESSARY);
            this.price.movePointRight(2).longValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Use a price with at most two decimals within the storage limit.");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Use a three-letter uppercase currency, such as PHP.");
        }
        if (!"AVAILABLE".equals(status) && !"WITHDRAWN".equals(status)) {
            throw new IllegalArgumentException("Status must be AVAILABLE or WITHDRAWN.");
        }
        this.id = id;
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.title = title.strip();
        this.currency = currency;
        this.status = status;
    }

    public long getId() { return id; }
    public long getSellerId() { return sellerId; }
    public long getCategoryId() { return categoryId; }
    public String getTitle() { return title; }
    public BigDecimal getPrice() { return price; }
    public long getPriceMinor() { return price.movePointRight(2).longValueExact(); }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }

    public abstract String getType();
    public abstract ConditionGrade gradeCondition();
}
