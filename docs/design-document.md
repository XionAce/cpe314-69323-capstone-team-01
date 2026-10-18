# UC-B Campus Marketplace with Reviews — Project Design Document

University of Cebu — Computer Engineering Department

CPE 314 — Software Design & Software Design Laboratory

Instructor: Richyll Son

First Semester, S.Y. 2026–2027

Group number: 1 (Team 01). EDP: 69323. Section: BSCPE 3B. Members: John Benedict Alaiza; Acexion Mahigugmaon; Glyde V. Arreglado; Tristan Urot; Joshua Brian S. Enovero; Eman De Paz.

Version 0.2 — preparation draft, 18 October 2026.

## Revision history

| Version | Date | Author | Changes |
|---|---|---|---|
| 0.1 | 2026-10-17 | Assistant-prepared draft; team review pending | Initial overview, ER sketch, proposed responsibility split |

## 1. Introduction and project overview

Campus Marketplace with Reviews helps students list and discover used textbooks, electronics, and furniture through a JavaFX desktop interface. Type-specific condition rules, buyer-interest waitlists, nested categories, reviews, and category analytics support informed browsing. SQLite provides local persistence, and background imports and product-catalog lookups keep interactions responsive. The selected theme is assigned Option J. Campus apparel and school/department sellers are proposed extensions for instructor review.

## 2. Preliminary ER sketch and team roles

Use docs/proposal.md Section 2 as the preliminary ER sketch. Members have Seller profiles; Sellers post Listings; Categories contain Listings and child Categories; Listings receive Reviews and BuyerInterests; ImportBatches contain HistoricalSales assigned to Categories. Listing subtype detail records store grading evidence.

The six names and proposed milestone responsibilities are in docs/proposal.md Section 4. Members must confirm their roles; actual contributions remain to be recorded.

## 3. System architecture and object-oriented design

Planned: JavaFX controllers call services; plain Java domain classes own rules; repository interfaces isolate JDBC; ProductCatalogClient isolates HTTP. Listing is abstract with three grading implementations. Final diagrams and implementation rationale will be added when built.

## 4. Data model and database design

Preliminary only: see proposal. Final ERD, reviewed keys/cardinality, exact schema and migration evidence pending.

## 5. API integration design

FakeStoreAPI products; JSON fields used for suggestions. Verified Java response, matching rules, currency policy, sample payload and fallback evidence pending.

## 6. Concurrency design

Planned JavaFX Task for streaming CSV import; background catalog work; UI callbacks for rendering; per-operation connections and serialized writes. Final thread flow and responsiveness evidence pending.

## 7. Data mining and visualization design

Planned separate calculations for average listing asking price and mean review rating by direct category. Separate bar charts with currency and 0–5 rating axes, sample counts, and no-data states. Final implementation and examples pending.

## 8. Testing summary

No tests written or executed yet. At least six meaningful JUnit 5 tests required. Planned cases are in docs/requirements.md. Add actual counts/results only after execution.

## 9. Individual contributions

PENDING actual member work and commit references. Do not assign assistant-authored planning material as evidence of a member's implementation.

## Appendix — final submission formatting

Use the supplied instructor template: Calibri 11, one-inch margins, Heading 1/2/3 styles and generated table of contents, revision history, table captions above tables, figure captions below figures, page numbers. Keep code excerpts short and link complete repository files. This Markdown draft is content preparation, not a claim that the final template formatting is complete.
