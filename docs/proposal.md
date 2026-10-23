# UC-B Campus Marketplace with Reviews — Project Proposal

CPE 314 — Software Design & Software Design Laboratory

Group-1 / Team 01 · BSCPE 3B · EDP 69323

**Group Name / No.:** Group 1 | BSCPE 3B | EDP 69323
UC-B Campus Marketplace with Reviews

**Members:** John Benedict Alaiza; Acexion Mahigugmaon; Glyde V. Arreglado; Tristan Urot; Joshua Brian S. Enovero; Eman De Paz

**Repository URL:** https://github.com/XionAce/cpe314-69323-capstone-team-01

**Chosen Theme (circle one):** J — Campus Marketplace with Reviews (assigned)

**API You Will Use:** FakeStoreAPI — https://fakestoreapi.com (no key required)

## 1. One-Paragraph Pitch

We propose UC-B Campus Marketplace with Reviews, a Java desktop application where students, faculty, school units, and department committees can post and browse goods. It retains textbook, electronics, and furniture listings with type-specific condition rules, recursive category search, buyer-interest queues, browsing history, and reviews. SQLite/JDBC persistence, a responsive background sales-CSV import, FakeStoreAPI suggestions, and category price/rating bar charts integrate the required lessons. Uniforms, department shirts, and intramural merchandise are a proposed apparel extension for instructor approval; online payments are outside scope.

## 2. Entity-Relationship Sketch

PK = primary key; FK = foreign key. Text notation follows the preliminary ER model.

Seller(id PK, memberId FK, displayName, sellerKind); Member(id PK, name)

Category(id PK, parentId FK → Category.id, name)

Listing(id PK, sellerId FK, categoryId FK, type, title, priceMinor, currency, status)

Review(id PK, listingId FK, reviewerId FK → Member.id, rating, comment)

BuyerInterest(id PK, listingId FK, buyerId FK → Member.id, joiningOrder)

HistoricalSale(id PK, categoryId FK, sourceRecordId, priceMinor, currency, soldAt)

Member 1:M Seller; Seller 1:M Listing; Category 1:M Listing, child Category and HistoricalSale; Listing 1:M Review and BuyerInterest; Member 1:M Review and BuyerInterest. Each listing also has one matching subtype-details record for condition evidence.

## 3. Feature Checklist

Check off which mandatory requirement each planned feature will satisfy:

| Planned Feature | Requirement(s) It Satisfies |
|---|---|
| Abstract Listing; Textbook, Electronics and Furniture grading overrides. | Object-Oriented Design (Week 4) |
| FIFO interest Queue, LIFO history Stack behavior; recursive category-tree search. | Data Structures and Recursion (Week 3) |
| At least 6 JUnit 5 tests with normal and edge cases. | Automated Testing (Week 4) |
| Related SQLite tables; JDBC PreparedStatement and foreign keys. | Data Modeling and Database (Week 5) |
| Parse catalog products for editable category and price suggestions. | API Consumption (Week 5) |
| JavaFX post/search/review/import controls; background CSV/API work. | Event-Driven GUI and Concurrency (Week 6) |
| Mean category prices and review ratings; labeled bar charts. | Data Mining and Visualization (Week 6) |

## 4. Task Split

| Member | Primary Responsibility (this milestone) |
|---|---|
| John Benedict Alaiza | Sample listings, categories and condition examples. |
| Acexion Mahigugmaon | Architecture, implementation planning and integration. |
| Glyde V. Arreglado | ER sketch, attributes and relationship review. |
| Tristan Urot | GUI sketches, control labels and user steps. |
| Joshua Brian S. Enovero | Sample CSV and API/import validation cases. |
| Eman De Paz | Test cases and expected analytics calculations. |

Proposed roles for member confirmation; actual contributions will be recorded in each member’s own commits.

## 5. Public API You Plan to Use

FakeStoreAPI — GET https://fakestoreapi.com/products (no key): provides product titles, descriptions, categories and prices for editable category and reference-price suggestions. Docs: https://fakestoreapi.com/docs.

## 6. Risks / Open Questions

How will we keep the interface responsive while importing a large CSV file?

How will recursive searches include listings from every nested subcategory?

What should the application show when FakeStoreAPI has no comparable product?

How will we calculate category ratings and average prices accurately?

---

Submission copies, using the instructor template:

[Revised Word proposal](https://docs.google.com/document/d/1WKe4Z_dsyzu8EQerr_-1qp6eYKJjS0pf/edit) · [Revised PDF proposal](https://drive.google.com/file/d/1kU4UM0kmT_17ppp3_ONYVf3K2j1lJfxE/view)

Instructor sign-off remains pending. This document records proposed responsibilities, not completed contributions.
