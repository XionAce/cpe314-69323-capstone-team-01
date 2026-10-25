-- Schema version 1: derived from the approved V0.2 design.
-- MarketplaceRepository creates this only for a new empty database.
-- Foreign keys are enabled on every connection by Database.open().

-- CREATE TABLE defines a table's columns and rules, not the sample records themselves.
-- PRIMARY KEY gives each record a unique ID. NOT NULL means a value is required.
-- CHECK rejects values that break a rule. REFERENCES links a value to another table's ID.

-- A member is a person using the marketplace and can act as either buyer or seller.
CREATE TABLE Members (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0)
);

-- A seller profile belongs to a member. Its id and memberId are different identifiers.
-- Listings reference the seller profile; buyer interest references the member directly.
CREATE TABLE Sellers (
    id INTEGER PRIMARY KEY,
    memberId INTEGER NOT NULL REFERENCES Members(id),
    displayName TEXT NOT NULL CHECK (length(trim(displayName)) > 0),
    sellerKind TEXT NOT NULL CHECK (length(trim(sellerKind)) > 0)
);

-- parentId points back to this same table, forming a category tree of arbitrary depth.
-- A NULL parentId means a top-level category. The CHECK prevents a direct self-parent.
-- CategoryService also rejects longer cycles when building/traversing the Java tree.
CREATE TABLE Categories (
    id INTEGER PRIMARY KEY,
    parentId INTEGER REFERENCES Categories(id),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    CHECK (parentId IS NULL OR parentId <> id)
);

-- Shared fields for every Listing subclass are stored together in this parent table.
-- sellerId and categoryId must point to existing Sellers and Categories records.
CREATE TABLE Listings (
    id INTEGER PRIMARY KEY,
    sellerId INTEGER NOT NULL REFERENCES Sellers(id),
    categoryId INTEGER NOT NULL REFERENCES Categories(id),
    -- type tells the repository which Java subclass and matching detail table to use.
    type TEXT NOT NULL CHECK (type IN ('TEXTBOOK', 'ELECTRONICS', 'FURNITURE')),
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    -- Money uses integer minor units: priceMinor 25000 with PHP means PHP 250.00.
    priceMinor INTEGER NOT NULL
        CHECK (typeof(priceMinor) = 'integer' AND priceMinor > 0),
    -- A three-letter uppercase currency records which money unit that integer represents.
    currency TEXT NOT NULL
        CHECK (length(currency) = 3 AND currency NOT GLOB '*[^A-Z]*'),
    -- Withdrawal sets this to WITHDRAWN instead of deleting the listing and its history.
    status TEXT NOT NULL CHECK (length(trim(status)) > 0)
);

-- This table supports the planned review workflow. Its existence does not add a review UI.
-- Each review belongs to one listing and records the member who submitted it.
CREATE TABLE Reviews (
    id INTEGER PRIMARY KEY,
    listingId INTEGER NOT NULL REFERENCES Listings(id),
    reviewerId INTEGER NOT NULL REFERENCES Members(id),
    rating INTEGER NOT NULL
        CHECK (typeof(rating) = 'integer' AND rating BETWEEN 1 AND 5),
    comment TEXT
);

-- Durable waitlist entries. Java rebuilds the FIFO Queue in ascending joiningOrder.
CREATE TABLE BuyerInterests (
    id INTEGER PRIMARY KEY,
    listingId INTEGER NOT NULL REFERENCES Listings(id),
    buyerId INTEGER NOT NULL REFERENCES Members(id),
    joiningOrder INTEGER NOT NULL
        CHECK (typeof(joiningOrder) = 'integer' AND joiningOrder > 0),
    -- A member may join each listing once; no two entries may share a position in that listing.
    UNIQUE (listingId, buyerId),
    UNIQUE (listingId, joiningOrder)
);

-- Reserved for the later background JSON sales-history import and category analytics.
-- A unique sourceRecordId helps reject reimported source records.
CREATE TABLE HistoricalSales (
    id INTEGER PRIMARY KEY,
    categoryId INTEGER NOT NULL REFERENCES Categories(id),
    sourceRecordId TEXT NOT NULL UNIQUE
        CHECK (length(trim(sourceRecordId)) > 0),
    priceMinor INTEGER NOT NULL
        CHECK (typeof(priceMinor) = 'integer' AND priceMinor > 0),
    currency TEXT NOT NULL
        CHECK (length(currency) = 3 AND currency NOT GLOB '*[^A-Z]*'),
    soldAt TEXT NOT NULL CHECK (length(trim(soldAt)) > 0)
);

-- The next three tables hold subclass-specific evidence, separate from common listing fields.
-- listingId is both the detail row's PRIMARY KEY and a FOREIGN KEY to Listings.
-- Therefore one detail table cannot contain two rows for the same listing.
-- Repository code ensures each listing has exactly one row across all three detail tables.
-- ON DELETE CASCADE removes details if a parent is physically deleted; withdrawal uses UPDATE.
CREATE TABLE TextbookDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    missingPages INTEGER NOT NULL
        CHECK (typeof(missingPages) = 'integer' AND missingPages >= 0),
    -- Store WearLevel enum names as text and Java booleans as integer 0 (false) or 1 (true).
    markingLevel TEXT NOT NULL CHECK (markingLevel IN ('NONE', 'LIGHT', 'HEAVY')),
    coverDamaged INTEGER NOT NULL
        CHECK (typeof(coverDamaged) = 'integer' AND coverDamaged IN (0, 1))
);

-- Electronics evidence determines power, functional defects, and cosmetic-only damage.
CREATE TABLE ElectronicsDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    powersOn INTEGER NOT NULL
        CHECK (typeof(powersOn) = 'integer' AND powersOn IN (0, 1)),
    hasFunctionalDefect INTEGER NOT NULL
        CHECK (typeof(hasFunctionalDefect) = 'integer' AND hasFunctionalDefect IN (0, 1)),
    hasCosmeticDamage INTEGER NOT NULL
        CHECK (typeof(hasCosmeticDamage) = 'integer' AND hasCosmeticDamage IN (0, 1))
);

-- Furniture evidence determines stability, structural damage, and surface wear.
CREATE TABLE FurnitureDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    structurallyStable INTEGER NOT NULL
        CHECK (typeof(structurallyStable) = 'integer' AND structurallyStable IN (0, 1)),
    hasStructuralDamage INTEGER NOT NULL
        CHECK (typeof(hasStructuralDamage) = 'integer' AND hasStructuralDamage IN (0, 1)),
    surfaceWear TEXT NOT NULL CHECK (surfaceWear IN ('NONE', 'LIGHT', 'HEAVY'))
);

-- Indexes help SQLite find related records without scanning every row each time.
-- These indexes add no new data rules; PRIMARY KEY and UNIQUE create their own indexes as needed.
CREATE INDEX idx_Sellers_memberId ON Sellers(memberId);
CREATE INDEX idx_Categories_parentId ON Categories(parentId);
CREATE INDEX idx_Listings_sellerId ON Listings(sellerId);
CREATE INDEX idx_Listings_categoryId ON Listings(categoryId);
CREATE INDEX idx_Reviews_listingId ON Reviews(listingId);
CREATE INDEX idx_Reviews_reviewerId ON Reviews(reviewerId);
CREATE INDEX idx_BuyerInterests_buyerId ON BuyerInterests(buyerId);
CREATE INDEX idx_HistoricalSales_categoryId ON HistoricalSales(categoryId);


