// This package groups the objects that represent marketplace data and business rules.
package edu.uc.campusmarket.model;

/** Common immutable listing data; each concrete type owns its grading rules. */
// abstract means Listing is a parent blueprint: create a Textbook, Electronics, or Furniture instead.
public abstract class Listing {
    // private hides fields from other classes; final prevents reassignment after construction.
    // long is a whole-number type; ID 0 means this new listing has not been saved yet.
    private final long id;
    // These IDs link this object to a seller profile and a category in the database.
    private final long sellerId;
    private final long categoryId;
    // String stores text, such as the item title shown on its card.
    private final String title;
    // Store money as whole minor units: 25000 PHP represents PHP 250.00.
    private final long priceMinor;
    // Keep the currency so amounts are not accidentally treated as the same currency.
    private final String currency;
    // The status includes ACTIVE or WITHDRAWN; withdrawal keeps the saved record.
    private final String status;

    // A constructor initializes an object. protected lets subclasses call this through super(...).
    protected Listing(long id, long sellerId, long categoryId, String title,
                      long priceMinor, String currency, String status) {
        // this.id means the field of this object; id on the right is the constructor parameter.
        this.id = ModelValidation.nonNegative(id, "Listing ID");
        // A saved seller/category must have an ID above zero.
        this.sellerId = ModelValidation.positive(sellerId, "Seller ID");
        this.categoryId = ModelValidation.positive(categoryId, "Category ID");
        // Validate the title and remove extra whitespace from its ends.
        this.title = ModelValidation.text(title, "Title");
        // Reject a zero or negative asking price before any database write.
        this.priceMinor = ModelValidation.positive(priceMinor, "Price in minor units");
        // || means OR. The regular expression requires exactly three uppercase A-Z letters.
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            // throw stops construction and reports why the supplied information is invalid.
            throw new IllegalArgumentException("Currency must contain three uppercase ASCII letters.");
        }
        // Store the checked currency and nonblank status.
        this.currency = currency;
        this.status = ModelValidation.text(status, "Status");
    }

    // These public getter methods expose values without allowing callers to replace them.
    // final on a method means a subclass cannot change its implementation.
    public final long id() {
        // return gives this value back to the caller.
        return id;
    }

    // The seller profile's ID is different from the member ID behind that profile.
    public final long sellerId() {
        return sellerId;
    }

    // The category determines where recursive category browsing finds this listing.
    public final long categoryId() {
        return categoryId;
    }

    // Read the title for cards, searches, and database saving.
    public final String title() {
        return title;
    }

    // Read the exact stored minor-unit amount; the interface formats it for display.
    public final long priceMinor() {
        return priceMinor;
    }

    // Read the amount's currency code, such as PHP.
    public final String currency() {
        return currency;
    }

    // Services use this value to hide withdrawn listings from public browsing.
    public final String status() {
        return status;
    }

    // An abstract method has no body here: each concrete subclass must supply its own.
    public abstract ListingType type();

    // Polymorphism: a Listing reference calls the grading implementation of its actual subtype.
    public abstract ConditionGrade gradeCondition();
}
