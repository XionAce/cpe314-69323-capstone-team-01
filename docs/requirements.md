# Requirement traceability and acceptance plan

Course minimums come from the Project Specification. The proposal was approved after the Week 8 revision. The behavior below describes the current Milestone 2 prototype and its 7 November instructor corrections.

| ID | Requirement | Evidence in the prototype | Acceptance check |
|---|---|---|---|
| R01 | OOD with abstraction and 2+ implementations | Listing plus Textbook/Electronics/Furniture subclasses | Different grading behavior through Listing references; threshold/defect tests |
| R02 | Purposeful collections | Queue per listing; stack-style recently viewed history | FIFO, empty queue, duplicates; LIFO and bounded history tests |
| R03 | Genuine recursion | Java category subtree traversal | Deep descendants included; siblings excluded; cycles rejected |
| R04 | At least six JUnit 5 tests | Core logic suite | Normal and edge cases; actual reports from Maven |
| R05 | 3+ related entities, SQLite/JDBC | Ten-table ERD, schema.sql and Database.java | Save/reload, foreign keys, transaction rollback; PreparedStatement review |
| R06 | Meaningful real REST consumption | Product catalog suggestions | Java request succeeds; parsed result changes editable suggestion; fake client unit tests |
| R07 | JavaFX/Swing GUI, 3+ interactive controls | JavaFX search/post/review/import | Controls trigger visible use cases |
| R08 | Background long operation | JSON import task and HTTP worker | Large input while UI tick/control remains responsive; failure/cancel recovery |
| R09 | Meaningful aggregation | Price and review summaries | Known fixture gives exact expected counts/averages without join fanout |
| R10 | Labeled chart | Two category bar charts | Category labels, currency/ratings axis, no-data behavior |

## Current behavior decisions

- Each member can buy, sell and review using an active-member selector. Full authentication is outside initial scope unless requested by the instructor.
- A review concerns the listing/item. One review per reviewer/listing; no self-review; rating 1–5.
- Condition is computed from subtype evidence, not independently editable text. Missing textbook pages, failed electronics function or structural furniture damage produce POOR. Other implemented thresholds are described in Section 3 of the design master.
- Each listing belongs to one category. Categories can nest with no fixed application level count; cycles forbidden. Recursion has normal runtime stack limits.
- Waitlists persist in joiningOrder and reject duplicate buyers. Recent views are bounded session history per active member; repeated entries are deduplicated.
- Store money as integer minor units and currency. Use BigDecimal calculations. FakeStoreAPI does not identify currency; catalog amounts are labeled as unspecified-currency references and do not overwrite the PHP price.
- Begin analytics at direct category membership. Do not mix currencies or average category averages. Show rating sample counts; unrated is not zero stars.
- JSON is a UTF-8 array with sourceRecordId, categoryPath, price, currency and soldAt. Gson streams one record at a time. sourceRecordId is globally unique across imports.
- One import uses one transaction. Malformed JSON, cancellation or database failure rolls back its new rows. Semantic-invalid rows and duplicate IDs are skipped/countable. Existing saved records remain.
- Keep writes serialized and connections per operation; read GUI inputs before background execution. No controls accessed from worker code.
- Missing detail rows are reported, not silently graded. The owner can explicitly restore observed condition through the GUI. The existing listing and related data remain; wrong owner, wrong subtype or existing details are rejected, and failed saves roll back.

## Test coverage to demonstrate

Grading thresholds for all types; collection order and empties; four-plus category levels; temporary-DB subtype round trips and FK failures; review constraints; malformed/trailing JSON and invalid records; duplicate reimports; failed-batch rollback; API timeout/malformed/no-match cases; currency policy; aggregation fixture with multiple reviews on one listing.
