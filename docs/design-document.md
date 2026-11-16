CPE 314 - Software Design

University of Cebu

Computer Engineering Department

# Campus Marketplace with Reviews

Project Design Document

Table 1: Group information

| Field | Details |
|---|---|
| Group Name / No. | Group 1 / Team 01 - BSCPE 3B - EDP 69323 |
| Members | John Benedict Alaiza |
|  | Acexion B. Mahigugmaon |
|  | Glyde V. Arreglado |
|  | Tristan Dominic Urot |
|  | Joshua Brian S. Enovero |
|  | Emanuelle De Paz |
| Instructor | Richyll Son |
| Term | First Semester, S.Y. 2026-2027 |
| Document Version / Date | V 1.0 / 16 November 2026 |

<!-- pagebreak -->

## Table of Contents

<!-- toc -->

<!-- pagebreak -->

## Revision History

Table 2: Document revisions

| Version | Date | Author | Changes |
|---|---|---|---|
| 0.1 | 23 Oct 2026 | Acexion B. Mahigugmaon | Initial project overview, ER sketch and responsibilities. |
| 0.2 | 8 Nov 2026 | Acexion B. Mahigugmaon | Documented the listing classes, named the IDs and clarified the table connections. |
| 0.3 | 15 Nov 2026 | Acexion B. Mahigugmaon | Registration, MVC and Java analysis; listing dates/status and safer detail handling. Compiled Sections 1-9 with synchronized services, private chat, safe refresh, themed screens, explicit JSON import and analytics breakdowns. Verified diagrams, current UI evidence and page navigation. |
| 1.0 | 16 Nov 2026 | Acexion B. Mahigugmaon | Revised the project design document: corrected the version sequence, named the author for each revision and updated the current issue date. |

<!-- pagebreak -->

## 1. Introduction & Project Overview

Campus Marketplace with Reviews lets students, faculty and school groups buy and sell textbooks, electronics and furniture. Members search categories, join waitlists, review listings and compare category prices and ratings. Each item type calculates its own condition grade.

The JavaFX app stores records in SQLite and imports JSON sales history. Members register or use the initial local accounts, then sign in to Marketplace, Sell an item, Sales history, Analytics and Messages. Messages lets a buyer and seller discuss an item and arrange a meeting. Messages are local to the shared SQLite file; separate laptops are not synchronized. Offers and payment processing remain outside the application.

## 2. Preliminary ER Sketch & Team Roles

Each box is a table. Lines join primary keys (PK) to foreign keys (FK). The fork marks the many side of 1:M. Section 4 shows all fields, including login and recovery data.

![Ownership and review relationships](assets/Figure_1_Preliminary_ER_Sketch.png)

[Editable diagram](assets/Figure_1_Preliminary_ER_Sketch.drawio)

Figure 1: Preliminary ownership and review relationships

Table 3: Team responsibilities

| Member and role | Responsibility |
|---|---|
| John Benedict Alaiza - Backend Developer (Listing Models) | Listing classes, condition rules and validation. |
| Acexion B. Mahigugmaon - Project Leader / Integration | Connect the components, maintain documents and coordinate fixes. |
| Glyde V. Arreglado - Database Admin | SQLite tables, saved records and summary queries. |
| Tristan Dominic Urot - UI/GUI Developer | JavaFX controls, event handlers and charts. |
| Joshua Brian S. Enovero - Backend & API Developer | Catalog requests, JSON parsing and import. |
| Emanuelle De Paz - QA & Testing | Test cases, expected calculations and error checks. |

<!-- pagebreak -->

## 3. System Architecture & Object-Oriented Design

Models hold item rules. Views show controls, controllers coordinate actions, services apply rules and DAOs run feature SQL. Database opens SQLite and initializes records. App connects these parts when the program starts.

### Class hierarchy

```mermaid
classDiagram
    class Listing {
        <<abstract>>
        -long id
        -long sellerId
        -long categoryId
        -String title
        -BigDecimal price
        -String currency
        -String status
        -Instant createdAt
        +getType() String*
        +gradeCondition() ConditionGrade*
        +getPriceMinor() long
        +getCreatedAt() Instant
        +hasConditionDetails() boolean
    }
    class Textbook {
        -int missingPages
        -WearLevel markingLevel
        -boolean coverDamaged
        +getMissingPages() int
        +getMarkingLevel() WearLevel
        +isCoverDamaged() boolean
        +getType() String
        +gradeCondition() ConditionGrade
    }
    class Electronics {
        -boolean powersOn
        -boolean functionalDefect
        -boolean cosmeticDamage
        +isPowersOn() boolean
        +hasFunctionalDefect() boolean
        +hasCosmeticDamage() boolean
        +getType() String
        +gradeCondition() ConditionGrade
    }
    class Furniture {
        -boolean stable
        -boolean structuralDamage
        -WearLevel surfaceWear
        +isStable() boolean
        +hasStructuralDamage() boolean
        +getSurfaceWear() WearLevel
        +getType() String
        +gradeCondition() ConditionGrade
    }
    Listing <|-- Textbook
    Listing <|-- Electronics
    Listing <|-- Furniture
```

Figure 2: Listing class hierarchy and type-specific condition rules

The hollow triangle points to the parent class. A minus sign marks a private field and a plus sign marks a public method. An asterisk marks an abstract method. Each subclass inherits the shared listing data and public getters, then supplies its own item type and condition grade.

<!-- pagebreak -->

### Class responsibilities

The shared Listing fields are ID, seller/category IDs, title, price, currency, status and creation time. The abstract methods getType() and gradeCondition() are implemented by each subclass. A List<Listing> can hold all three types. Calling gradeCondition() through Listing runs the actual object's method; this is runtime polymorphism.

Table 4: Model responsibilities and Single Responsibility Principle

| Class | Responsibility and reason to change |
|---|---|
| Listing | Shared listing values and validation. Changes when common listing rules change. |
| Textbook | Textbook evidence and condition grade. Changes when textbook condition rules change. |
| Electronics | Electronics evidence and condition grade. Changes when electronics condition rules change. |
| Furniture | Furniture evidence and condition grade. Changes when furniture condition rules change. |
| ConditionGrade / WearLevel | Allowed grade and wear values. Changes when these allowed values change. |
| Member / Category | Member identity and category parent data. Changes when these records change. |

Keeping item rules separate from SQL and controls follows the Single Responsibility Principle. A changed grading rule belongs in its item class.

### Condition rules

- Textbook: missing pages = POOR; heavy markings = FAIR; light markings or cover damage = GOOD; otherwise LIKE_NEW.
- Electronics: no power or a functional defect = POOR; cosmetic damage = GOOD; otherwise LIKE_NEW.
- Furniture: unstable or structurally damaged = POOR; heavy wear = FAIR; light wear = GOOD; otherwise LIKE_NEW.

<!-- pagebreak -->

### Supporting components

Table 5: Application layers and responsibilities

| Layer | Responsibility and reason to change |
|---|---|
| App | Creates the database, DAOs, services, controllers and views. Owns the shared worker and shutdown. |
| Views in ui | Draw controls, bind state, show dialogs and forward actions. Contain no SQL, API calls or worker creation. |
| Controllers | Hold observable state, obtain the active member from UserSession and request background work. |
| Services | Apply account, listing, review and analysis rules. Convert data-access failures before they reach the screen. |
| DAOs | Execute prepared SQL and return records. Recheck storage-sensitive rules in write transactions. |
| Database / AccountSetup / DatabaseChanges | Open connections, initialize and migrate records; observe committed changes without reloading unchanged lists. |
| ProductCatalogClient / ProductCatalogParser | Download the catalog and validate its JSON in separate classes. |
| CatalogProduct / CatalogResponse | Carry the parsed product subset and HTTP information in dto. |
| CatalogMapper / CatalogService | Select comparable products and return CatalogSuggestion with OK, EMPTY or FAILED status. |
| ChatController / ChatService / ChatDao | Load private conversations, validate messages and save participant-filtered results. |
| SalesHistoryImporter / SalesAnalysis | Stream and validate JSON imports; group saved timestamps and count weekly activity in Java. |

The action path is View -> Controller -> Service -> DAO -> SQLite. MemberService, ListingService, ReviewService and WaitlistService own focused operations; there is no shared MarketplaceService facade in the application. CategoryTree owns hierarchy validation and recursion. Background results update controller properties on the JavaFX thread; bound views display them. Models contain no GUI or SQL. The Appendix lists every top-level Java type and the process map.

<!-- pagebreak -->

### Collections and recursion

Waitlists use Queue with ArrayDeque: offer() adds at the tail and poll() reads the first buyer. WaitlistService reads saved joiningOrder values in FIFO order. Leaving deletes only that buyer's row; positions count the remaining earlier rows without rewriting saved orders. InterestDao assigns MAX(joiningOrder)+1 and inserts in one atomic INSERT ... SELECT, with eligibility rechecked in that same statement. SQLite serializes writers; ties do not receive the same position. ListingService keeps a Map of each member's Stack of recently viewed IDs. push() adds the latest view; pop() reads a copy, preserving session history. At most 20 unique views are kept per member.

CategoryTree.collectCategoryIds() adds a category ID and recursively visits each child. A category without children is the base case. Validation rejects missing parents, duplicate IDs and cycles before traversal. Search includes the selected category and all descendants.

### Application screens and state

MarketplaceView is the application shell. Cards accept Enter or Space, field labels link to controls, and forms show inline validation. The footer shows local-storage status, with the database path in a tooltip. Table 13 maps screens to their controllers and services.

Controllers coordinate feature state through UserSession and FxTaskRunner. UserSession identifies the signed-in member and tracks session changes; FxTaskRunner exposes busy, status and error properties. Section 6 explains background work and refresh.

Result records carry action results and screen snapshots without adding database columns. Table 14 lists every Java type, and Table 13 shows the process flow. ItemArtwork draws category illustrations; UiTools provides window and dialog helpers.

### Buyer-seller conversations

A buyer starts a listing conversation with its seller through Open chat on an ACTIVE item. Sellers reply to buyers who have already written. Each listing/buyer pair stays private. Chat does not join waitlists, reserve items, mark sales or process payments.

ChatView shows named message bubbles and local times. Send message or Ctrl+Enter sends plain text. ChatController takes the sender from UserSession; ChatService trims the text and requires 1-2000 characters. ChatDao uses prepared statements, rechecks participants and listing status before saving, orders messages by SQLite ID and stores sentAt in UTC.

Existing conversations remain usable after a listing becomes SOLD; new conversations cannot start on sold items. REMOVED listings retain read-only history. Section 6 explains automatic updates, draft preservation and session isolation.

### Sign in, registration and password recovery

Registration requires a display name of 1-80 characters, a username of 3-30 letters/digits/underscores and a password of 8-128 characters. MemberDao strips and lowercases the username and rejects duplicates through the unique index. It stores salted PBKDF2 hashes and returns a recovery code once. The member saves that code and then signs in; registration does not automatically sign in.

A new database creates Person A, Person B and Person C with usernames person_a, person_b and person_c. Each receives a random password and recovery code. A local startup dialog shows newly issued credentials once. They are not published here or stored as plaintext in SQLite. Existing current accounts retain their credentials when reopened.

Login verifies the password hash and sets UserSession. Forgot password verifies the saved recovery code and atomically replaces the password hash and recovery-code hash. The screen displays a replacement code to save; the old code cannot be reused. Failed resets preserve the previous credentials. This is local recovery, not email delivery.

Hashes use PBKDF2WithHmacSHA256, 210,000 iterations and 256 bits. Random salts use 16 bytes; recovery values use 18 random bytes encoded to 24 URL-safe characters. CredentialSecurity holds these settings. Controllers clear submitted password/recovery character arrays after use.

<!-- pagebreak -->

### Marketplace screens and ownership

The app fills the usable work area with top navigation and the logo's navy, teal and gold theme. The title bar, controls, dialogs and errors share this styling. UiTools.WindowFrame and UiTools.WindowResize handle window movement, sizing and closing. My home shows active items, waiting buyers, received reviews and personal waitlist positions. HomeController loads member-filtered HomeService/HomeDao data in one background task; logout clears the dashboard and older-session results are rejected.

Marketplace places its saying below the title, above search and scrollable cards. Cards and List share records, dates and statuses. Selection opens a closable details panel on the right; narrow windows stack the panels. Read reviews shows names, initials, stars and wrapped comments; Write a review opens its form. View details loads waitlist and recent views. Sell an item groups item and condition fields beside a live title, price and condition preview; service validation remains authoritative.

Each member may buy or sell. Edit title / price is available for an owned active listing. The service validates the title and exact positive price; SQL rechecks owner and active status at save time. The ID, type, condition, currency, creation date, reviews and waitlist stay unchanged. Posting uses the member's seller profile, created when needed. My listings filters to that owner and can show All statuses, Active, Sold or Removed. Mark as sold and Remove listing require confirmation and recheck ownership in SQL. Records, condition rows, reviews and waitlists are retained. Public search and current-listing means include ACTIVE items only.

Join waitlist accepts an existing member different from the seller and saves joiningOrder. Repeated joins are rejected. Leave waitlist deletes only the signed-in buyer's interest row, including for archived items; saved reviews remain. Current position counts remaining buyers up to the member's joining order, so gaps in saved numbers do not leave gaps in displayed positions. Rejoining goes to the tail. Details and My home display the position. Reviews require membership in that listing's waitlist, another owner and no earlier review by the same member/item. Ratings are 1-5; optional comments have at most 1,000 characters. The item must remain ACTIVE. Waitlist membership demonstrates interest, not proof of payment.

Restore missing details lets an owner choose a listing and the condition actually observed. No condition is selected automatically. Saving rechecks owner, type and missing subtype rows; it preserves the listing ID, common fields, reviews and waitlist. Cancel changes nothing. Conflicting existing details require maintainer review.

<!-- pagebreak -->

### Listing types and sample records

Listing defines the shared item data. The application accepts three concrete types: Textbook, Electronics and Furniture. ListingFactory rejects unsupported types and missing or mismatched condition observations instead of creating a default furniture item or inventing a condition grade. Database seeds one example of each when it creates the demo data. The user can post additional items of any supported type.

Table 6: Listings supplied with the prototype

| Listing and type | Category and price | Saved observations and result |
|---|---|---|
| Java textbook (demo) / TEXTBOOK | Textbooks / PHP 350.00 | No missing pages, light markings, intact cover. Grade: GOOD. |
| Gaming laptop (demo) / ELECTRONICS | Gaming Laptops / PHP 18,000.00 | Powers on, no functional defect, cosmetic damage. Grade: GOOD. |
| Study chair (demo) / FURNITURE | Furniture / PHP 650.00 | Stable, no structural damage, light surface wear. Grade: GOOD. |

### Categories and listing rules

All finds is the root category. Its direct children are Textbooks, Electronics and Furniture. Under Electronics, Computers contains Laptops, which contains Gaming Laptops. Searching a parent category includes items in its descendants.

Each listing requires a seller profile, category, nonblank title, positive exact minor-unit price and three-letter uppercase currency. A post starts ACTIVE under the signed-in seller. ListingDao saves created_at as a UTC timestamp when inserting. The owner can change ACTIVE to SOLD or REMOVED; both leave public search. No payment or historical sale is created by marking an item sold.

<!-- pagebreak -->

## 4. Data Model & Database Design

### Finalized entity relationships

The schema defines eleven application tables with 56 stored fields and fifteen foreign-key connections. The overview shows every table and its PK/FK connections. Four detail views show every attribute at a larger size; Listings repeats to connect the views. Field names match SQLite exactly. The SQL below specifies types, nullability and checks.


```mermaid
erDiagram
    Members {
        INTEGER id PK
    }
    Sellers {
        INTEGER id PK
        INTEGER memberId FK
    }
    Listings {
        INTEGER id PK
        INTEGER sellerId FK
        INTEGER categoryId FK
    }
    Reviews {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER reviewerId FK
    }
    BuyerInterests {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER buyerId FK
    }
    Categories {
        INTEGER id PK
        INTEGER parentId FK
    }
    HistoricalSales {
        INTEGER id PK
        INTEGER categoryId FK
    }
    TextbookDetails {
        INTEGER listingId PK,FK
    }
    ElectronicsDetails {
        INTEGER listingId PK,FK
    }
    FurnitureDetails {
        INTEGER listingId PK,FK
    }
    Messages {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER buyerId FK
        INTEGER senderId FK
    }
    Members ||--o{ Messages : buyer
    Members ||--o{ Messages : sender
    Listings ||--o{ Messages : discusses
    Members ||--o{ Sellers : owns
    Members ||--o{ Reviews : writes
    Members ||--o{ BuyerInterests : joins
    Sellers ||--o{ Listings : posts
    Categories o|--o{ Categories : contains
    Categories ||--o{ Listings : classifies
    Categories ||--o{ HistoricalSales : groups
    Listings ||--o{ Reviews : receives
    Listings ||--o{ BuyerInterests : queues
    Listings ||--o| TextbookDetails : has
    Listings ||--o| ElectronicsDetails : has
    Listings ||--o| FurnitureDetails : has
```

Figure 3: Eleven-table overview with PK/FK connections

The overview shows keys only. The detail views and SQL show all remaining attributes. A small bridge at a crossing means the lines do not join.

<!-- pagebreak -->


Read 1:M as one parent to many children. A parent can have no children. M:N (also called M:M) means many-to-many. Members and Listings have this relationship through BuyerInterests for waitlists and through Reviews for ratings. Each connecting table has two 1:M links. Each individual listing subtype uses 1:1; only the matching subtype has a row.

```mermaid
erDiagram
    Members {
        INTEGER id PK
        TEXT name
        TEXT username UK
        TEXT passwordSalt
        TEXT passwordHash
        TEXT recoverySalt
        TEXT recoveryHash
    }
    Sellers {
        INTEGER id PK
        INTEGER memberId FK
        TEXT displayName
        TEXT sellerKind
    }
    Listings {
        INTEGER id PK
        INTEGER sellerId FK
        INTEGER categoryId FK
        TEXT type
        TEXT title
        INTEGER priceMinor
        TEXT currency
        TEXT status
        TEXT created_at
    }
    Reviews {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER reviewerId FK
        INTEGER rating
        TEXT comment
    }
    BuyerInterests {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER buyerId FK
        INTEGER joiningOrder
    }
    Members ||--o{ Sellers : owns
    Members ||--o{ Reviews : writes
    Members ||--o{ BuyerInterests : joins
    Sellers ||--o{ Listings : posts
    Listings ||--o{ Reviews : receives
    Listings ||--o{ BuyerInterests : queues
```

Figure 4: Members, sellers, listings, reviews and buyer interests

<!-- pagebreak -->

### Categories and historical sales

```mermaid
erDiagram
    Categories {
        INTEGER id PK
        INTEGER parentId FK
        TEXT name
    }
    Listings {
        INTEGER id PK
        INTEGER sellerId FK
        INTEGER categoryId FK
        TEXT type
        TEXT title
        INTEGER priceMinor
        TEXT currency
        TEXT status
        TEXT created_at
    }
    HistoricalSales {
        INTEGER id PK
        INTEGER categoryId FK
        TEXT sourceRecordId UK
        INTEGER priceMinor
        TEXT currency
        TEXT soldAt
    }
    Categories o|--o{ Categories : parents
    Categories ||--o{ Listings : classifies
    Categories ||--o{ HistoricalSales : groups
```

Figure 5: Categories, current listings and imported historical sales

A root category has NULL parentId. A non-root category has one parent, and a parent can have zero or more children. HistoricalSales contains imported past sales, not new marketplace listings. sourceRecordId prevents duplicate imports.

<!-- pagebreak -->

### Listing condition details

```mermaid
erDiagram
    Listings {
        INTEGER id PK
        INTEGER sellerId FK
        INTEGER categoryId FK
        TEXT type
        TEXT title
        INTEGER priceMinor
        TEXT currency
        TEXT status
        TEXT created_at
    }
    TextbookDetails {
        INTEGER listingId PK,FK
        INTEGER missingPages
        TEXT markingLevel
        INTEGER coverDamaged
    }
    ElectronicsDetails {
        INTEGER listingId PK,FK
        INTEGER powersOn
        INTEGER hasFunctionalDefect
        INTEGER hasCosmeticDamage
    }
    FurnitureDetails {
        INTEGER listingId PK,FK
        INTEGER structurallyStable
        INTEGER hasStructuralDamage
        TEXT surfaceWear
    }
    Listings ||--o| TextbookDetails : has
    Listings ||--o| ElectronicsDetails : has
    Listings ||--o| FurnitureDetails : has
```

Figure 6: Shared listing fields and type-specific condition evidence

A valid listing needs exactly one matching detail row overall. It has zero or one row in each individual detail table, depending on type. A detail row's listingId is both its primary key and its foreign key. The app saves the listing and detail row in one transaction, and checks the type when reading. Foreign keys alone do not guarantee exactly one matching subtype. If details are absent or inconsistent, Listing.incomplete() returns a read-only placeholder with the saved type and common fields. Cards, tables and details show Details unavailable; no grade or boolean flag is invented and healthy items still load. Owners can restore missing rows; conflicting rows require maintainer review.

Some Java names are shorter than their SQL columns. Electronics.functionalDefect maps to hasFunctionalDefect, and cosmeticDamage maps to hasCosmeticDamage. Furniture.stable maps to structurallyStable, and structuralDamage maps to hasStructuralDamage. ListingDao converts boolean values to SQLite 0/1 and wear enums to their text names.

<!-- pagebreak -->

### Private message records

```mermaid
erDiagram
    Members {
        INTEGER id PK
        TEXT name
        TEXT username UK
        TEXT passwordSalt
        TEXT passwordHash
        TEXT recoverySalt
        TEXT recoveryHash
    }
    Sellers {
        INTEGER id PK
        INTEGER memberId FK
        TEXT displayName
        TEXT sellerKind
    }
    Listings {
        INTEGER id PK
        INTEGER sellerId FK
        INTEGER categoryId FK
        TEXT type
        TEXT title
        INTEGER priceMinor
        TEXT currency
        TEXT status
        TEXT created_at
    }
    Messages {
        INTEGER id PK
        INTEGER listingId FK
        INTEGER buyerId FK
        INTEGER senderId FK
        TEXT body
        TEXT sentAt
    }
    Members ||--o{ Sellers : owns
    Sellers ||--o{ Listings : posts
    Listings ||--o{ Messages : discusses
    Members ||--o{ Messages : buyer
    Members ||--o{ Messages : sender
```

Figure 7: Listing messages and the buyer, sender and seller identities

Messages.listingId identifies the item and buyerId identifies the buyer whose conversation this is. senderId identifies the member who wrote that individual message; it can be the buyer or the listing's owner. The seller is found through Listings.sellerId and Sellers.memberId, so a second seller ID is not copied into Messages. A conversation is the set of rows sharing listingId and buyerId. Each row stores one plain-text body and a UTC sentAt timestamp.

### Whose ID is it?

A primary key (PK) identifies one row in its own table. A foreign key (FK) identifies a related row. The name id belongs to its containing table: Members.id identifies a member, Sellers.id a seller profile and Listings.id an item. Boxes use exact column names; the table below explains them.

Table 7: IDs used to connect marketplace records

| SQLite field | What it identifies |
|---|---|
| Sellers.memberId | The member who owns the seller profile. Links to Members.id. |
| Listings.sellerId | The seller profile that posted this item. Links to Sellers.id. |
| Listings.categoryId | The item's category. Links to Categories.id. |
| Categories.parentId | The parent category. Links to Categories.id; empty for a root category. |
| Reviews.listingId | The listing being reviewed. Links to Listings.id. |
| Reviews.reviewerId | The member who wrote the review. Links to Members.id. |
| BuyerInterests.listingId | The listing whose waitlist the buyer joined. Links to Listings.id. |
| BuyerInterests.buyerId | The member who joined the waitlist. Links to Members.id. |
| Messages.listingId | The item discussed in this message. Links to Listings.id. |
| Messages.buyerId | The buyer participating in this private conversation. Links to Members.id. |
| Messages.senderId | The member who wrote this message, buyer or listing owner. Links to Members.id. |
| HistoricalSales.categoryId | The category of the imported sale. Links to Categories.id. |
| TextbookDetails.listingId, ElectronicsDetails.listingId, FurnitureDetails.listingId | In each table, this is both PK and FK. It identifies the listing whose condition is described and links to Listings.id. |

Example: Reviews.id = 7, Reviews.listingId = 12 and Reviews.reviewerId = 3 mean that member 3 wrote review 7 about listing 12.

HistoricalSales.id identifies the locally saved sale. HistoricalSales.sourceRecordId is the unique ID supplied by the JSON file to recognize a repeat import.

### Account fields and database upgrades

Members.id identifies the member. username is marked UK because its unique index keeps login names distinct. passwordSalt and passwordHash verify the entered password. recoverySalt and recoveryHash verify a recovery code. Salts and hashes are saved as text. Member objects passed to the screen contain only id and name.

Database.initialize() calls AccountSetup.upgrade() to migrate versions 1-4 to version 5 in one transaction. It preserves credentials, IDs and related records; adds missing account fields, the username index and Listings.created_at; and maps old AVAILABLE/WITHDRAWN values to ACTIVE/REMOVED. The version-5 step creates Messages and its indexes from the schema definitions. Versions 1-4 contain ten tables; version 5 contains eleven. PRAGMA user_version records version 5.

Listings.created_at maps to Java Listing.createdAt (Instant) and records UTC creation time on insert. Legacy rows keep NULL because their original date is unknown; the UI shows Date not recorded. Status uses ACTIVE, SOLD or REMOVED. This adds one field, not another table.

<!-- pagebreak -->

### Relationships and storage rules

Table 8: Relationship cardinalities

| Parent | Child | Relationship |
|---|---|---|
| Members | Sellers | 1:M |
| Members | Reviews | 1:M |
| Members | BuyerInterests | 1:M |
| Sellers | Listings | 1:M |
| Categories | Listings | 1:M |
| Categories | HistoricalSales | 1:M |
| Categories | Categories | 1:M; a root has no parent |
| Listings | Reviews | 1:M |
| Listings | BuyerInterests | 1:M |
| Listings | Messages | 1:M |
| Members | Messages as buyer | 1:M |
| Members | Messages as sender | 1:M |
| Listings | Each detail table | 1:1; only the matching subtype applies |

Prices use integer minor units: PHP 350.50 is stored as priceMinor 35050 and currency PHP. Java uses BigDecimal. The app uses ACTIVE, SOLD and REMOVED status values. Required names are nonblank; review ratings range from 1 to 5. ReviewDao also rejects self-review, repeat reviews and members who have not joined the listing's waitlist inside its write transaction.

BuyerInterests has UNIQUE(listingId, buyerId) to prevent repeated interest and UNIQUE(listingId, joiningOrder) to prevent tied positions within a waitlist. Members and Listings have an M:N interest relationship implemented through BuyerInterests.

Foreign keys are enabled on every SQLite connection. PreparedStatement binds input values to SQL. DAO data operations open their own connections. Saving a listing with its detail record and restoring missing details both use transactions and roll back on failure.

The following CREATE TABLE statements match program/src/main/resources/db/schema.sql. Each snippet defines one table. The username index is created during AccountSetup.upgrade().

### Members

```sql
CREATE TABLE Members (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    username TEXT,
    passwordSalt TEXT,
    passwordHash TEXT,
    recoverySalt TEXT,
    recoveryHash TEXT
);
```

AccountSetup.upgrade() also runs this index statement.

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_Members_username ON Members(username);
```

### Sellers

```sql
CREATE TABLE Sellers (
    id INTEGER PRIMARY KEY,
    memberId INTEGER NOT NULL REFERENCES Members(id),
    displayName TEXT NOT NULL CHECK (length(trim(displayName)) > 0),
    sellerKind TEXT NOT NULL CHECK (length(trim(sellerKind)) > 0)
);
```

### Categories

```sql
CREATE TABLE Categories (
    id INTEGER PRIMARY KEY,
    parentId INTEGER REFERENCES Categories(id),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    CHECK (parentId IS NULL OR parentId <> id)
);
```

### Listings

```sql
CREATE TABLE Listings (
    id INTEGER PRIMARY KEY,
    sellerId INTEGER NOT NULL REFERENCES Sellers(id),
    categoryId INTEGER NOT NULL REFERENCES Categories(id),
    type TEXT NOT NULL CHECK (type IN ('TEXTBOOK', 'ELECTRONICS', 'FURNITURE')),
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    priceMinor INTEGER NOT NULL
       
        CHECK (typeof(priceMinor) = 'integer' AND priceMinor > 0),
    currency TEXT NOT NULL
       
        CHECK (length(currency) = 3 AND currency NOT GLOB '*[^A-Z]*'),
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'SOLD', 'REMOVED')),
    created_at TEXT CHECK (created_at IS NULL OR length(trim(created_at)) > 0)
);
```

### Reviews

```sql
CREATE TABLE Reviews (
    id INTEGER PRIMARY KEY,
    listingId INTEGER NOT NULL REFERENCES Listings(id),
    reviewerId INTEGER NOT NULL REFERENCES Members(id),
    rating INTEGER NOT NULL
       
        CHECK (typeof(rating) = 'integer' AND rating BETWEEN 1 AND 5),
    comment TEXT
);
```

### BuyerInterests

```sql
CREATE TABLE BuyerInterests (
    id INTEGER PRIMARY KEY,
    listingId INTEGER NOT NULL REFERENCES Listings(id),
    buyerId INTEGER NOT NULL REFERENCES Members(id),
    joiningOrder INTEGER NOT NULL
       
        CHECK (typeof(joiningOrder) = 'integer' AND joiningOrder > 0),
    UNIQUE (listingId, buyerId),
    UNIQUE (listingId, joiningOrder)
);
```

### HistoricalSales

```sql
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
```

### TextbookDetails

```sql
CREATE TABLE TextbookDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    missingPages INTEGER NOT NULL
       
        CHECK (typeof(missingPages) = 'integer' AND missingPages >= 0),
    markingLevel TEXT NOT NULL CHECK (markingLevel IN ('NONE', 'LIGHT', 'HEAVY')),
    coverDamaged INTEGER NOT NULL
       
        CHECK (typeof(coverDamaged) = 'integer' AND coverDamaged IN
        (0, 1))
);
```

### ElectronicsDetails

```sql
CREATE TABLE ElectronicsDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    powersOn INTEGER NOT NULL
       
        CHECK (typeof(powersOn) = 'integer' AND powersOn IN
        (0, 1)),
    hasFunctionalDefect INTEGER NOT NULL
       
        CHECK (typeof(hasFunctionalDefect) = 'integer' AND hasFunctionalDefect IN
        (0, 1)),
    hasCosmeticDamage INTEGER NOT NULL
       
        CHECK (typeof(hasCosmeticDamage) = 'integer' AND hasCosmeticDamage IN
        (0, 1))
);
```

### FurnitureDetails

```sql
CREATE TABLE FurnitureDetails (
    listingId INTEGER PRIMARY KEY REFERENCES Listings(id) ON DELETE CASCADE,
    structurallyStable INTEGER NOT NULL
       
        CHECK (typeof(structurallyStable) = 'integer' AND structurallyStable IN
        (0, 1)),
    hasStructuralDamage INTEGER NOT NULL
       
        CHECK (typeof(hasStructuralDamage) = 'integer' AND hasStructuralDamage IN
        (0, 1)),
    surfaceWear TEXT NOT NULL CHECK (surfaceWear IN ('NONE', 'LIGHT', 'HEAVY'))
);
```

<!-- pagebreak -->

### Messages

```sql
CREATE TABLE IF NOT EXISTS Messages (
    id INTEGER PRIMARY KEY,
    listingId INTEGER NOT NULL REFERENCES Listings(id),
    buyerId INTEGER NOT NULL REFERENCES Members(id),
    senderId INTEGER NOT NULL REFERENCES Members(id),
    body TEXT NOT NULL CHECK (length(trim(body)) BETWEEN 1 AND 2000),
    sentAt TEXT NOT NULL CHECK (length(trim(sentAt)) > 0)
);
CREATE INDEX IF NOT EXISTS idx_Messages_thread ON Messages(listingId, buyerId, id);
CREATE INDEX IF NOT EXISTS idx_Messages_buyerId ON Messages(buyerId);
```

## 5. API Integration Design

### Catalog request and response

The public endpoint is GET https://fakestoreapi.com/products. It returns a JSON array and needs no API key. Suggest category / price makes a fresh request.

Example response shape (illustrative values, not a captured live response):

```json
[
  {
    "id": 1, "title": "Example monitor", "price": 120.50,
    "category": "electronics", "description": "Product details",
    "image": "https://example.com/product.png",
    "rating": {"rate": 4.2, "count": 25}
  }
]
```

ProductCatalogClient sends the request with a 15-second timeout, checks HTTP 200 and records request time/status. ProductCatalogParser validates one complete array with Gson: id must be a positive whole number, price a positive number, and title/category nonblank text. Trailing data, invalid shapes or missing required fields fail. CatalogProduct carries id, title, category and BigDecimal price. CatalogResponse carries products and request information. Unused description, image and rating fields are ignored.

### Mapping and listing-form use

CatalogMapper.matches() selects compatible electronics products sharing title words with the listing. It ignores short/common words, ranks by shared-word count, breaks ties by product ID and returns up to three references. CatalogService returns titles, a minimum/maximum reference price and an explicit outcome to ListingController. The member chooses the campus category and final PHP price manually.

The API does not state currency, so prices are labeled currency unspecified. Unsupported clothing/jewelry categories are not converted into campus listing types. Textbooks, furniture and unmatched queries retain manual entry. Posting inputs are disabled during a request, preventing a stale response from being applied to changed inputs.

### Errors and API verification

CatalogSuggestion distinguishes OK, EMPTY and FAILED. HTTP failure, timeout or invalid JSON returns a retry/manual-entry message in the suggestion panel. Rejected requests show a busy message rather than leaving a loading label. It does not save a listing or erase entered values. Parser/client tests use local HTTP servers and the saved real HTTP-200 response containing 20 products. The team captured it on a working connection; its metadata records timestamp and SHA-256. A live GUI network toggle remains a separate check; the illustrative JSON above is not the captured response.

<!-- pagebreak -->

## 6. Concurrency Design

### Worker and controller state

App creates one named ExecutorService, marketplace-database, shared by the controllers. Views create no threads and call no DAOs or HTTP clients. FxTaskRunner.run() submits callable work to that serial worker and dispatches results through Platform.runLater. Only foreground actions set busy; one user action can queue behind a quiet refresh. At most one quiet read is pending, so timer ticks cannot build up a backlog.

Initialization, registration, login, reset, listing actions, catalog requests, JSON import and analytics run on this worker. Controllers capture inputs and active member before starting. UserSession's generation number lets listing/analysis results reject a signed-out or changed session.

TaskRunner is injectable: controller tests use a deterministic runner; the app uses FxTaskRunner. Services convert data-access failures into AppException, so views do not handle JDBC exceptions.

### Background JSON import

SalesHistoryImporter streams a UTF-8 JSON array one record at a time. Each record contains sourceRecordId, categoryPath, price, currency and soldAt. It validates required text, an unambiguous existing category path, positive exact minor-unit price, uppercase currency and an offset timestamp.

HistoryDao supplies a prepared insert. One transaction commits valid new rows after the file ends successfully. Invalid field values and duplicate source IDs are skipped and counted. Broken JSON, database failure or cancellation rolls back that import's new rows and preserves previous history.

ImportView accepts one readable JSON file through Choose JSON file or the drop area. Selection alone writes nothing; Import selected file starts AnalyticsController.importFile(). AnalyticsController reports progress after the first row and every 1,000 rows. A spinner and indeterminate bar show actual work; the bar fills only after a committed result and resets on a new selection or failure. Cancel requests rollback between records. The last completed result shows actual saved, invalid and duplicate totals, up to five validation messages and the latest 20 rows. Preview failure after commit still reports a successful save.

### Automatic updates between windows

A three-second Timeline from FxRefreshSchedule calls RefreshController.tick() for signed-in active pages when no dialog or foreground task is running. DatabaseChanges reads SQLite PRAGMA data_version; Marketplace and Analytics reload after a committed change or page entry. Successful reads update the observed version. Session, category, query and ownership checks reject stale results; unchanged rows and chart snapshots preserve selection and avoid redraw.

The timer stops on logout and close; failed reads show a message and allow retry. Processes sharing one SQLite file see committed updates, while separate laptop databases are not synchronized. Details, reviews and waitlists reload through their actions. ChatController refreshes the selected conversation and inbox without clearing unsent text. Changing conversations clears the draft; logout clears private chat state. Account or selection changes invalidate older results.

### Responsiveness and shutdown

Foreground work disables its work buttons and posting inputs; quiet refresh leaves them available. Navigation, loaded-view scrolling and Cancel import remain usable. App.stop() stops refresh, requests cancellation and shuts down the worker. DAOs close operation resources, and the change observer closes its reused connection. Appendix timings cover data reads; very large screen rendering remains unmeasured.

<!-- pagebreak -->

## 7. Data Mining & Visualization Design

### Weekly sales frequency in Java

The analysis asks how many imported sales occurred each week and which observed weeks were busiest. HistoryDao.soldDates() reads HistoricalSales.soldAt. SalesAnalysis.weeklyCounts() converts timestamps to UTC, finds the previous-or-same Monday and increments a TreeMap count. The map sorts the weeks. WeeklySales records carry the results, and summary() finds the total, largest count and all tied busiest weeks.

This is Java aggregation, not SQL AVG/COUNT. Counts include all currencies because they count sales, not money. Duplicate source IDs are not inserted twice. Weeks without sales are not plotted; empty history has a no-history message. UTC grouping also handles offset and year boundaries consistently.

The six-record sample produces weekly counts 3, 2 and 1 for 19 October, 26 October and 2 November 2026. The three-record follow-up changes these to 3, 2 and 4. A separate sales-history-realistic-demo.json contains 1,200 explicitly fictional PHP sales across twelve weeks from 24 August to 14 November, with supported categories and varied prices. Its actual importer check saved 1,200 rows with no invalid records; a repeat skipped all 1,200 duplicates. Existing saved history combines with any sample and changes the totals.

### Category averages

Table 9: Independent category calculations

| Result | Calculation and data |
|---|---|
| Average listing price | ACTIVE Listings in one selected currency: AVG(priceMinor) / 100.0 and COUNT(*), grouped by category. |
| Average review rating | Individual Reviews on ACTIVE Listings: AVG(rating) and COUNT(*), grouped by category. Tied best-rated categories show review counts. |

AnalyticsDao runs separate queries so reviews do not multiply listing prices. Currencies remain separate. Historical sales and external API values do not enter these means. Categories without reviews are unrated, not zero stars.

### Charts and why they fit

Analytics displays one of three bar charts at a time through Weekly sales, Listing prices and Review ratings. Separated navy, teal and gold bars, labeled axes, light dashed grid lines and exact-value tooltips support reading the data:

- Weekly frequency: X-axis Week starting Monday (UTC); Y-axis Sales.
- Listing means: X-axis Category; Y-axis Price in the selected currency, initially PHP.
- Review means: X-axis Category; Y-axis Rating on a 0-5 scale.

AnalyticsService returns an AnalyticsSnapshot; AnalyticsController publishes it and ChartsView renders the selected chart and its breakdown. Weekly rows show exact counts and shares of imported sales. Price rows show category means, currency and listing counts; their bars compare with the highest mean, not a market share. Rating rows show means out of five and review counts. Summary cards show counts from the same snapshot. Small windows stack the chart and breakdown within a scrollable page; refresh and idle polling read current data.

### Checking that the analysis changes

HistoryAnalysisTest imports, deletes and reimports saved rows and checks changed counts. SalesAnalysisTest checks ordering, UTC/year boundaries, ties, empty data and immutable results. Current listing-price means remain independent of historical frequencies.

<!-- pagebreak -->

## 8. Testing Summary

### Recorded JUnit result

The recorded JDK 21 Maven clean verify on 15 November 2026 passed 159 JUnit tests in fourteen test classes, with zero failures, errors or skipped tests and the Java/Maven enforcer enabled. New regressions cover clear price/database messages, rejected catalog requests, foreground actions during refresh, committed-change detection and invalid saved dates. TestDatabase and MarketplaceFixture are shared test helpers, not additional test classes.

Tests use temporary SQLite files and local HTTP servers, leaving the personal database unchanged. The Appendix gives commands for repeating verification.

### Coverage and meaningful edge cases

Table 10: JUnit coverage and edge cases

| Test class and count | What it checks | Meaningful edge case |
|---|---|---|
| ProductCatalogClientTest (12) | Catalog JSON parsing, matching, HTTP failures and timeout. | Reject numeric strings; timeouts stop waiting; the real 20-product capture supplies valid electronics references. |
| ControllerTest (19) | Session identity, background results, selection, refresh and personal home. | A search or home result arriving after logout cannot refill the screen. |
| FxTaskRunnerTest (6) | Worker ordering, UI dispatch, busy state, rejection and quiet refresh. | A user action queues behind a quiet read; a timer cannot queue repeated reads or overwrite the newer action's status. |
| ListingTest (9) | Three condition subclasses, lifecycle states and exact money. | One missing textbook page overrides otherwise good appearance; fractional cents are rejected. |
| ListingValidationTest (6) | Member/category identity, title cleanup and money limits. | Accept the largest stored price; reject invalid parents, unknown item types and mismatched condition observations. |
| DatabaseTest (33) | Initialization, migrations, credentials, subtype storage, recovery and concurrent queues. | Two buyers joining simultaneously receive distinct positions; migration failure rolls back schema/version changes. |
| ReviewAnalyticsTest (9) | Review rules, averages, currency separation and historical-data exclusion. | Weight every review equally instead of averaging per-item averages; separate different currencies. |

<!-- pagebreak -->

### Further test coverage

Table 11: Remaining JUnit coverage and edge cases

| Test class and count | What it checks | Meaningful edge case |
|---|---|---|
| AccountAndReviewTest (10) | Registration, recovery codes and waitlist-before-review rules. | A repeated normalized username inserts no extra member; a consumed recovery code cannot be reused. |
| HistoryAnalysisTest (1) | JSON import through saved data to Java weekly analysis. | Deleting and reimporting stored history changes weekly counts while current listing means remain unchanged. |
| MarketplaceServiceTest (17) | Recursive categories, FIFO waitlists, LIFO history and ownership. | Reject category cycles/missing parents; one member cannot read another member's private recent history. |
| PersonalFeaturesTest (10) | Account-scoped home, owner edits, leaving/rejoining and position changes. | Reject an edit after an item becomes sold; leaving advances remaining positions and retains saved reviews. |
| SalesAnalysisTest (6) | UTC Monday grouping, sorting, empty results and tied busiest weeks. | A timestamp offset can cross a Monday/year boundary; report all tied busiest weeks. |
| ChatServiceTest (9) | Private buyer/seller messages, message limits, saved order and version-4 migration. | An unrelated buyer cannot read/send to another conversation; concurrent replies remain ordered; existing account hashes survive upgrade. |
| SalesHistoryImporterTest (12) | Streaming validation, duplicates, progress and transaction rollback. | Malformed JSON or cancellation after a valid row rolls back the unfinished import and preserves earlier data. |

### Application checks and remaining verification

The 15 November UI revision passed 93 application assertions on temporary databases: 60 workflow checks and 33 layout, file-selection, import-result and analytics-breakdown checks. They covered keyboard use, linked labels, inline errors, ownership/reviews, scrolling, readable review cards, explicit import start, duplicate/failure handling, chart switching and small-window layouts. Separate branding checks covered moving, sizing and closing windows/dialogs. These checks are separate from JUnit. Unknown saved dates show as unavailable; sale previews use readable UTC dates.

A separate two-process chat check passed seventeen GUI assertions using a temporary database. Buyer and seller signed in through independent JVMs, exchanged three saved messages and received replies through the normal three-second refresh. Checks confirmed that new bubbles scrolled into view, incoming replies preserved drafts, and another buyer's message did not change the selected recipient, clear its draft or enter another conversation.

Earlier GUI checks passed 50 assertions on 13 November using temporary data. These historical results are separate from the current JUnit suite; their workflows are covered in the feature descriptions and test tables.

A public HTTP-200 response with 20 products was captured on 13 November 2026 at 15:45 UTC. Test resources retain the response and timestamp/hash metadata; ProductCatalogClientTest checks parsing and electronics matching. Six live API screen checks also passed. The offline/reconnection demonstration, older-branch ERD cleanup and very large screen-rendering measurements remain open.

<!-- pagebreak -->

## 9. Individual Contributions

### Contributions linked to repository history

The contribution table links each member to changes recorded under their Git identity. Section 2 lists assigned roles. Later integration, MVC, analysis and personal-feature corrections are recorded under Acexion's account. Members should explain the referenced changes during individual questions.

Table 12: Member contributions recorded in Git

| Member and responsibility | Recorded change and commit references |
|---|---|
| Joshua Brian S. Enovero - Backend and API | Catalog product data (3445979), HTTP catalog requests/parsing (ac9a812), and JSON sales-history importer (ca7b4d7). Later client/importer formatting: 321dfda; captured-response verification and samples: e1c9ef2. |
| Glyde V. Arreglado - Database | Review persistence and category summaries (e44d82d); schema description (9c5f51d); account login/recovery and database updates (10d57ed); message persistence and migration (368351d). |
| Tristan Dominic Urot - UI and GUI | Connected review, import, suggestion and chart actions (d71e56b); redesigned login/recovery screens, styles and automatic refresh (507f2de); messaging UI (57a7a68). |
| John Benedict Alaiza - Listing models | Listing title/money-boundary validation tests (f66a8d3); textbook-model formatting (665d922). The formatting commit is not described as new condition logic. Later factory/model-message work: 8bacc0e and 3b9af6c. |
| Emanuelle De Paz - QA and testing | Expanded account, import, API, analytics, model-validation and service tests across six files (5a8d3c3). Earlier README team-member update: 7855ef9; chat/controller regression coverage: 41dc439. |
| Acexion B. Mahigugmaon - Project leader and integration | Application/service integration (98f2509); MVC/domain separation (7c2554c, e7ff854, 935659a); Java weekly analysis (c00274c); lifecycle/atomic queue persistence (ec36b77); personal home/edit/queue corrections (65ef65b, 4dc1a9c, a0208ff); follow-up tests (72088f5) and design synchronization (e80c4cf); chat integration (b7a2ee9), final mapping (aea412c) and editable design/contents (63e6d72). |

### Finding and explaining a contribution

For individual questions, open a referenced commit's Files changed view and explain the input, rule or action, result and supporting test or screen. Use the current type inventory when refactoring has changed a filename.

The commit references were checked against repository history. They record changes, not peer ratings or proof of individual understanding.

<!-- pagebreak -->

## Appendix - References and reproduction

design-document.md is the editable master. Its Mermaid blocks define the hierarchy and database diagrams; assets/ contains the editable draw.io sketch and PNG. From the repository root, `python tools/export_design.py` rebuilds the PDF using Python, ReportLab and Calibri. Set CALIBRI_DIR for another font folder.

Use JDK 21 with the version enforcer enabled. From program/, run `java -version`, `./mvnw.cmd -version` and `./mvnw.cmd clean verify`. Both version checks must show Java 21. From the repository root, `./program/mvnw.cmd -o -f ./program/pom.xml verify` repeats the suite after dependencies are available. First-time downloads need a connection. The existing Eclipse and Antigravity Run profiles remain available.

### Account setup and recovery

Account creation and recovery are described in Section 3. Save issued passwords and recovery codes privately when displayed. Existing accounts keep their credentials; a reset replaces the recovery code.

### Verification

Section 8 contains the dated JUnit, GUI and API results. Repeat the build and tests on the presentation machine; demonstrate API offline/reconnection behavior separately.

### Limitations and requirement mapping

Accounts and private chats use one local SQLite file; cloud sync, email recovery and payments are excluded. Chat history is available to participants through the app, including across two processes on the same file. Recent views last for the session. Five warm runs with 10,003 listings, 10,000 interests and 50,000 sales measured search at 80.2-348.6 ms, analysis at 169.9-286.4 ms and home at 49.5-63.0 ms. These laptop data-read timings exclude rendering and cold startup; they are not a capacity guarantee.

KNOWN_ISSUES.md and REQUIREMENT_MAP.md in the repository root list limits and map checklist requirements to classes, methods and tests.

- [Team repository](https://github.com/XionAce/cpe314-69323-capstone-team-01)
- [Project proposal](proposal.pdf)
- [FakeStoreAPI documentation](https://fakestoreapi.com/docs)
- [Project Design Document Template](https://drive.google.com/file/d/1sRJ5rjciHBeYgBQmUbpJB12kWyD_sCkO/view)
- [Final design document checklist](https://drive.google.com/file/d/1LywRsBWB2ol6xxJS_RbutuDbKJI7k7tc/view)

<!-- pagebreak -->


### Program process map

All types are in edu.uc.campusmarket or the listed subpackage.

Table 13: Implemented process flow

| Process | Screen and controller | Service and storage result |
|---|---|---|
| Startup | App.start(); AccountController.initialize() | MemberService initializes Database/AccountSetup and returns newly issued credentials once. |
| Register | RegistrationView; AccountController.register() | MemberDao saves a unique account and returns a recovery code. |
| Login / reset | LoginView / PasswordResetView; AccountController.login()/reset() | MemberDao verifies or replaces hashes; UserSession holds active identity. |
| Personal home | HomeView; HomeController.load() | HomeDao reads member-filtered active items/buyers, reviews received and personal queue positions in one transaction. |
| Cards / search | ListingTableView / ListingCards; ListingController.refresh() | ListingService searches listings; CategoryTree validates and recursively includes descendants. |
| Post / edit / sold / remove | ListingFormView / ListingTableView; ListingController.post()/update()/markSold()/withdraw() | ListingFactory creates a subtype; ListingDao records created_at or rechecks owner before title/price edits and ACTIVE-to-SOLD/REMOVED changes. |
| History / waitlist | ListingController.viewDetails(); WaitlistController.join()/leave() | ListingService keeps bounded LIFO history; WaitlistService reads FIFO order; InterestDao allocates joining order atomically, removes only the requesting buyer and counts current queue position. |
| Review | ReviewsView; ReviewController.submit() | ReviewDao checks owner, status, interest, rating and duplicate before saving. |
| Restore evidence | ListingTableView; ListingController.repairable()/restore() | ListingDao checks owner, type and missing details and saves observed evidence. |
| API reference | ListingFormView; ListingController.suggest() | CatalogService -> client/parser -> DTOs -> CatalogMapper prepares references. |
| Private chat | ChatView; ChatController.open()/select()/send()/refresh() | ChatService validates text; ChatDao filters participants and saves to Messages. |
| Import | ImportView; AnalyticsController.importFile()/cancelImport() | Importer validates rows; HistoryDao supplies the insert; cancellation/failure rolls back. |
| Analysis / charts | ChartsView; AnalyticsController.refresh() | AnalyticsService combines DAO means and Java weekly counts in AnalyticsSnapshot. |
| Refresh | RefreshController.tick(); FxRefreshSchedule | Checks committed changes before full list/chart reads; session/filter guards reject stale results. |
| Sign out / close | AccountController.logout(); App.stop() | Clears session, stops timer, requests cancellation and shuts down the worker. |

<!-- pagebreak -->

### Complete Java type inventory

The current source contains 85 top-level Java types. Every one is listed below; the database contains eleven tables. A DTO, controller or view is not an additional database table.

Table 14: Java types by package

| Package | Types |
|---|---|
| base | App |
| model | AnalyticsSnapshot, CatalogSuggestion, Category, CategorySummary, ChatMessage, ChatSnapshot, ChatThread, ConditionGrade, Electronics, Furniture, HomeItem, HomeSummary, ImportResult, InitialAccount, Listing, ListingForRepair, Member, Registration, Review, Seller, Textbook, WearLevel, WeeklySales |
| dto | CatalogProduct, CatalogResponse |
| api | ProductCatalogClient, ProductCatalogParser |
| dao | AnalyticsDao, CategoryDao, ChatDao, HistoryDao, HomeDao, InterestDao, ListingDao, MemberDao, ReviewDao, SellerDao |
| persistence | AccountSetup, Database, DatabaseChanges, SavedDates |
| service | AnalyticsService, AppException, CatalogMapper, CatalogService, CategoryTree, ChatService, CredentialSecurity, HomeService, ListingFactory, ListingService, MemberService, ReviewService, SalesAnalysis, SalesHistoryImporter, ServiceCall, WaitlistService |
| controller | AccountController, AnalyticsController, ChatController, FxRefreshSchedule, FxTaskRunner, HomeController, ListingController, RefreshController, RefreshSchedule, ReviewController, TaskRunner, UserSession, WaitlistController |
| ui | ChartsView, ChatView, HomeView, ImportView, ItemArtwork, ListingCards, ListingFormView, ListingTableView, LoginView, MarketplaceView, PasswordResetView, RegistrationView, ReviewsView, UiTools |

ServiceCall.Query is a nested SQL-operation interface. CatalogSuggestion.Status is a nested outcome enum. FxTaskRunner dispatches serial worker results to the JavaFX thread. Listing.incomplete() creates an anonymous read-only fallback for unavailable condition evidence; it is not a new posted item type or table.

<!-- pagebreak -->

#### Current application screens

These screenshots show the implemented interface using temporary demonstration data. Accounts, listings, messages and imported sales in the pictures are examples; they are not evidence of real campus transactions.

![Application screen](assets/app-marketplace.png)

Figure 8: Marketplace cards and the selected item panel

![Application screen](assets/app-create-listing.png)

Figure 9: Listing fields and the live asking-price preview

<!-- pagebreak -->

![Application screen](assets/app-sales-history.png)

Figure 10: JSON selection, committed progress and actual import totals

![Application screen](assets/app-analytics.png)

Figure 11: Weekly sales with the corresponding breakdown
