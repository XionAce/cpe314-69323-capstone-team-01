# Requirement traceability and acceptance plan

Course minimums come from the Project Specification. Additional behavior below is a proposed team design choice until approved.

| ID | Requirement | Planned evidence | Acceptance check |
|---|---|---|---|
| R01 | OOD with abstraction and 2+ implementations | Listing plus Textbook/Electronics/Furniture subclasses | Different grading behavior through Listing references; threshold/defect tests |
| R02 | Purposeful collections | Queue per listing; stack-style recently viewed history | FIFO, empty queue, duplicates; LIFO and bounded history tests |
| R03 | Genuine recursion | Java category subtree traversal | Deep descendants included; siblings excluded; cycles rejected |
| R04 | At least six JUnit 5 tests | Core logic suite | Normal and edge cases; actual reports from Maven |
| R05 | 3+ related entities, SQLite/JDBC | Reviewed ERD, migrations, repositories | Save/reload, foreign keys, transaction rollback; PreparedStatement review |
| R06 | Meaningful real REST consumption | Product catalog suggestions | Java request succeeds; parsed result changes editable suggestion; fake client unit tests |
| R07 | JavaFX/Swing GUI, 3+ interactive controls | JavaFX search/post/review/import | Controls trigger visible use cases |
| R08 | Background long operation | CSV task and HTTP worker | Large input while UI tick/control remains responsive; failure/cancel recovery |
| R09 | Meaningful aggregation | Price and review summaries | Known fixture gives exact expected counts/averages without join fanout |
| R10 | Labeled chart | Two category bar charts | Category labels, currency/ratings axis, no-data behavior |

## Proposed behavior decisions

- Each member can buy, sell and review using an active-member selector. Full authentication is outside initial scope unless requested by the instructor.
- A review concerns the listing/item. One review per reviewer/listing; no self-review; rating 1–5.
- Condition is computed from subtype evidence, not independently editable text. Proposed overriding defects: missing textbook pages, failed essential electronics function, structural furniture damage -> POOR. Remaining thresholds need team agreement.
- Each listing belongs to one category. Categories can nest with no fixed application level count; cycles forbidden. Recursion has normal runtime stack limits.
- Waitlists persist in increasing entry-ID order and reject duplicate active buyers. Recent views are bounded session history per active member; deduplicate repeated entries.
- Store money as integer minor units and currency. Use BigDecimal calculations. API currency semantics remain pending.
- Begin analytics at direct category membership. Do not mix currencies or average category averages. Show rating sample counts; unrated is not zero stars.
- CSV is UTF-8 with stable source_record_id, title, category_path, sale_price, currency, sold_at. Stream with Commons CSV. Duplicate detection is scoped to a documented source namespace and record ID, independent of import batch.
- Completed import batches remain saved on cancellation; current uncommitted batch rolls back. Report accepted/rejected/duplicate counts and partial completion honestly.
- Keep writes serialized and connections per operation; read GUI inputs before background execution. No controls accessed from worker code.

## Later test targets

Grading thresholds for all types; collection order and empties; four-plus category levels; temporary-DB subtype round trips and FK failures; review constraints; quoted CSV/newlines; duplicate reimports; failed-batch rollback; API timeout/malformed/no-match cases; currency policy; aggregation fixture with multiple reviews on one listing.
