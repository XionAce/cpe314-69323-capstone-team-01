# V0.3 implementation note: Milestone 2

Date: 3 November 2026. This records the implemented behavior corresponding to Sections 5–7 of the cumulative Week 10 design. The earlier Drive PDF described the plan before implementation; it has not been republished by this code task.

## 5. API integration

ProductCatalogClient uses Java HttpClient to GET https://fakestoreapi.com/products, with a 15-second request timeout. Gson parses the response array into CatalogProduct records (ID, title, category, BigDecimal price). Unexpected JSON/fields and HTTP errors show a failure with manual entry/retry available.

The Suggest button makes a fresh worker-thread request. For ELECTRONICS, normalized meaningful title words are compared with electronics catalog titles. The three best positive matches supply a reference min/max price and the Electronics category; the seller may choose a child category. A changed form does not receive an outdated suggestion. PHP price is never overwritten: the API response has no currency field. Books/furniture or unrelated titles may have no match. API product ratings do not enter campus review analytics.

Verified from Java and through the actual Suggest button: HTTP 200, 20 products parsed, matching products displayed and category selected. Offline network toggling remains a live rehearsal check; local-server unit tests verify the HTTP-success/error paths without depending on the internet.

## 6. Concurrency and JSON import

The existing single ExecutorService runs JavaFX Tasks. Form values are captured on the JavaFX thread. Completion/failure callbacks update controls. The root pane remains enabled: tabs, loaded table, form inputs and About work while conflicting operation buttons are disabled.

SalesHistoryImporter streams a strict UTF-8 JSON array, checking sourceRecordId, full categoryPath, exact positive minor-unit price, uppercase currency and timestamp with timezone. Prepared inserts use one transaction. Duplicate IDs and semantic-invalid rows are counted/skipped; only five representative invalid-row reasons are retained. Syntax, SQL or cancellation failure rolls back the import's new rows. Existing sales remain. The UI Cancel button sets a cooperative flag; checks occur between rows and before commit. App shutdown also interrupts the worker. Progress is provisional until commit.

After a successful import, the UI displays counts and up to 20 recent imported sales. A preview error does not change the already committed import status. Importing past sales does not create available listings or affect asking-price summaries.

An actual 100,000-record import was started through the view's import workflow with an isolated database. The UI remained usable, About fired during the import, and Cancel rolled back the new rows. The small provided sample then saved three records. File-picker interaction should still be rehearsed manually.

## 7. Aggregation and charts

Database.priceSummary groups AVAILABLE Listings by category and selected currency using SQL AVG/COUNT. Database.ratingSummary separately groups campus Reviews joined to AVAILABLE Listings, avoiding price duplication by review joins. Highest mean identifies best-rated categories; ties and review counts are shown. No reviews means unrated.

Two labeled JavaFX bar charts compare categories: asking-price means and review means. The currency filter defaults to PHP. Ratings use a 0–5 axis; each chart contains one series so no legend is required. Empty results show no-data titles. Direct category membership is used. Historical sale prices and external catalog ratings are excluded.

The actual GUI check posted a PHP 100 textbook alongside the seeded PHP 350 book, producing a PHP 225 bar. Posting another PHP 300 book and refreshing changed that bar to PHP 250. A campus review produced the correct rating bar and count.

## Tests and remaining verification

On 3 November 2026 at 23:48 (Asia/Manila), the normal wrapper passed `mvnw.cmd -o clean verify` with **40 JUnit tests**: model 10 (including two ListingValidationTest cases), database 10, service 9, API 3, importer 5, review/analytics 3; zero failures/errors/skips. This final run used the normal dependency cache. Earlier verification used a private cache while dependencies were being resolved; no private build settings or machine-specific dependency path are included in the project.

Eighteen actual-App GUI checks passed. Tests and GUI checks used temporary databases, not the user's saved demo database. See MILESTONE_2.md for the source map, known limits, launch/demo guide and manual Git submission steps. Editor Run-button rehearsal, live network toggling, actual member contribution records/private peer evaluations and final cumulative-document evidence still need the group's review.
