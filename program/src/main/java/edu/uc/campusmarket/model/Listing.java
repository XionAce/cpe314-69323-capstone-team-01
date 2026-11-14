package edu.uc.campusmarket.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

// The parent stores data common to every item. It cannot be instantiated directly.
/**
 * Common item identity, ownership, money and abstract condition-grading
 * behavior.
 */
public abstract class Listing {
    /** Number of decimal places in saved currency amounts. */
    public static final int MONEY_SCALE = 2;
    public static final String ACTIVE = "ACTIVE";
    public static final String SOLD = "SOLD";
    public static final String REMOVED = "REMOVED";
    private final Instant createdAt;
    private final long id;
    private final long sellerId;
    private final long categoryId;
    private final String title;
    private final BigDecimal price;
    private final String currency;
    private final String status;

    /** Creates this component from the supplied values or dependencies. */
    public Listing(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status) {
        this(id, sellerId, categoryId, title, price, currency, status,
                id == 0 ? Instant.now() : null);
    }

    /**
     * Loads shared listing fields; null creation time means a legacy unknown date.
     */
    public Listing(long id, long sellerId, long categoryId, String title, BigDecimal price,
            String currency, String status, Instant createdAt) {
        if (id < 0 || sellerId <= 0 || categoryId <= 0) {
            throw new IllegalArgumentException(
                    "Use ID 0 for a new item and positive seller/category IDs.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Enter an item title.");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        try {
            // Store exact cents: 350.50 becomes 35050 in SQLite. Never silently round
            // money.
            this.price = price.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
            this.price.movePointRight(MONEY_SCALE).longValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException(
                    "Use a price with at most two decimals within the storage limit.");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Use a three-letter uppercase currency, such as PHP.");
        }
        if (!ACTIVE.equals(status) && !SOLD.equals(status) && !REMOVED.equals(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE, SOLD or REMOVED.");
        }
        this.createdAt = createdAt;
        this.id = id;
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.title = title.strip();
        this.currency = currency;
        this.status = status;
    }

    /** Returns the id. */
    public long getId() {
        return id;
    }

    /** Returns the seller id. */
    public long getSellerId() {
        return sellerId;
    }

    /** Returns the category id. */
    public long getCategoryId() {
        return categoryId;
    }

    /** Returns the title. */
    public String getTitle() {
        return title;
    }

    /** Returns the price. */
    public BigDecimal getPrice() {
        return price;
    }

    /** Returns the price minor. */
    public long getPriceMinor() {
        return price.movePointRight(MONEY_SCALE).longValueExact();
    }

    /** Returns the currency. */
    public String getCurrency() {
        return currency;
    }

    /** Returns the status. */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the recorded UTC creation instant, or null for an unknown legacy
     * date.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** True when the listing has its one matching condition-detail row. */
    public boolean hasConditionDetails() {
        return true;
    }

    /** Describes a missing/conflicting condition row without inventing a grade. */
    public String getConditionIssue() {
        return "";
    }

    /** Returns a safe label for both normal and incomplete saved listings. */
    public String getConditionLabel() {
        return hasConditionDetails() ? gradeCondition().name().replace('_', ' ')
                : "Details unavailable";
    }

    /** Read-only fallback so one damaged detail row does not hide every listing. */
    public static Listing incomplete(long id, long seller, long category, String title,
            BigDecimal price, String currency, String status, Instant createdAt, String type,
            String issue) {
        return new Listing(id, seller, category, title, price, currency, status, createdAt) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public boolean hasConditionDetails() {
                return false;
            }

            @Override
            public String getConditionIssue() {
                return issue;
            }

            @Override
            public ConditionGrade gradeCondition() {
                throw new IllegalStateException("Condition evidence is unavailable.");
            }
        };
    }

    /** Returns the type. */
    public abstract String getType();

    /** Calculates the condition grade using this listing subtype's evidence. */
    public abstract ConditionGrade gradeCondition();
}
