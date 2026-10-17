# Project Proposal — working draft

Working title: Campus Marketplace with Reviews

Course: CPE 314 — Software Design & Software Design Laboratory

Group/team number: Group 1 / Team 01

EDP: 69323

Members and GitHub usernames: PENDING — user will add teammates later.

Repository URL: https://github.com/XionAce/cpe314-69323-capstone-team-01

Repository name: cpe314-69323-capstone-team-01

Theme: Option J; class sign-up availability PENDING

Public API: FakeStoreAPI, https://fakestoreapi.com — products endpoint https://fakestoreapi.com/products

## 1. One-paragraph pitch

Campus Marketplace with Reviews is a Java desktop application for students buying and selling used textbooks, electronics, and furniture. Sellers post items with type-specific condition assessments, while buyers browse nested categories, search listings, join an interest waitlist, and leave reviews. The application stores its data in SQLite, imports historical sales without freezing the interface, and uses a public product catalog to suggest categories and reference price ranges when suitable comparisons exist. Category charts help students compare asking prices and review ratings, with limitations and sample counts displayed clearly.

## 2. Preliminary ER sketch

Member(id PK, displayName, email)

Category(id PK, parentId FK -> Category.id, name)

Listing(id PK, sellerId FK -> Member.id, categoryId FK -> Category.id, type, title, description, priceMinor, currency, status, createdAt)

TextbookDetails / ElectronicsDetails / FurnitureDetails(listingId PK/FK -> Listing.id, type-specific grading evidence)

Review(id PK, listingId FK -> Listing.id, reviewerId FK -> Member.id, rating, comment, createdAt)

BuyerInterest(id PK, listingId FK -> Listing.id, buyerId FK -> Member.id, joinedAt)

ImportBatch(id PK, sourceName, status, startedAt, finishedAt)

HistoricalSale(id PK, batchId FK -> ImportBatch.id, sourceRecordId, categoryId FK -> Category.id, title, salePriceMinor, currency, soldAt)

Member 1:M Listing; Category 1:M Listing; Category 1:M child Category; Listing 1:M Review; Member 1:M Review; Listing 1:M BuyerInterest; Member 1:M BuyerInterest; ImportBatch 1:M HistoricalSale; Category 1:M HistoricalSale. Each Listing has exactly one detail record of its selected type, enforced by service transactions and mapping tests.

## 3. Feature checklist

| Planned feature | Requirement |
|---|---|
| Abstract Listing with TextbookListing, ElectronicsListing, FurnitureListing grading overrides | OOD and SRP |
| Per-item FIFO waitlist, LIFO recent history | Data structures |
| Recursive Java traversal collecting listings under nested categories | Recursion |
| JUnit 5 normal and edge-case tests, at least six | Automated testing |
| Related SQLite tables accessed with JDBC PreparedStatement | Data model/database |
| Parse real catalog response and use suitable products in editable suggestions | API consumption |
| Browse, search, post, review, waitlist, import and dashboard controls | Event-driven GUI |
| Background CSV/API operations with progress and failure handling | Concurrency |
| Average asking prices and average ratings by category | Data mining |
| Separate labeled price and rating bar charts | Visualization |

## 4. Proposed task split — assign names before submission

| Owner | Responsibility |
|---|---|
| PENDING member 1 | Domain model, condition grading, associated tests |
| PENDING member 2 | SQLite schema, repositories, persistence tests |
| PENDING member 3 | Recursive categories, waitlists, recent history, tests |
| PENDING member 4 | Browse/post/review GUI integration |
| PENDING member 5 | Catalog client, CSV import, concurrency, tests |
| PENDING member 6 | Analytics/charts, integration/demo coordination |

For a five-member group, redistribute the sixth responsibility explicitly. Every owner updates their design-document sections and commits their own work.

## 5. Public API plan

FakeStoreAPI provides sample product title, description, category and price data. The application will request products through Java HttpClient, parse JSON, and rank relevant products for editable category and reference-price suggestions. A successful real call must be demonstrated; fixture data is for deterministic tests and labeled offline fallback only.

Actual Java reachability and catalog coverage are unverified. Previous browsing attempts were blocked; that does not establish whether Java requests will succeed. Product currency must be resolved before converting remote values into local PHP suggestions. Unsupported listings return no comparable result rather than a fabricated match.

## 6. Risks and open questions

| Risk/question | Proposed response | Instructor response |
|---|---|---|
| Catalog may not cover textbooks/furniture | Demonstrate supported matches; keep manual entry; ask whether supplementary catalog data/API is required | PENDING |
| Remote price currency not established | Confirm convention; document any demo assumption; use no silent conversion | PENDING |
| Large CSV can freeze GUI or contend for SQLite writes | JavaFX Task, streaming parser, serialized writes, bounded transactions, progress and cancellation | PENDING |
| Different developer/presentation configurations | Maven Wrapper, pinned versions, Java 21, early Eclipse import and fresh-checkout test | PENDING |
| Category cycles or repeated import records | Reject cycles; stable source IDs and duplicate constraints | PENDING |

This draft is not ready for submission until administrative fields, roles, API decisions and instructor responses are completed. Instructor review/sign-off: PENDING.
