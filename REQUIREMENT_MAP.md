# Requirement-to-code and test map

Campus Marketplace with Reviews. Source checked on 12 November 2026.

All main Java files listed below are under `program/src/main/java/edu/uc/campusmarket/`. Test files are under `program/src/test/java/edu/uc/campusmarket/`. Method names describe the current implementation and must be updated after refactoring.

## Implementation map

| Checklist requirement | Current class and method | Evidence and remaining work |
|---|---|---|
| 1. Public API and meaningful JSON use | `api/ProductCatalogClient.fetch()`, `parse()`, `matches()`; `api/CatalogProduct`; `ui/MarketplaceView.suggest()` | Downloads products, validates JSON and displays matching electronics references. `api/ProductCatalogClientTest` covers parsing, matching, HTTP errors and timeout. Parser/mapper separation is pending. Repeat the live API check with the required network toggle at the demo. |
| 2. Background processing and responsiveness | `ui/MarketplaceView.runTask()`, `runAutoRefresh()`, `startImport()`, `autoRefresh()`, `logout()`, `close()`; `service/SalesHistoryImporter.importFile()` | One named worker runs slow work. JavaFX Task handlers update UI state; import progress uses `Platform.runLater`. A flag requests cancellation; logout/close stop the timer. `service/SalesHistoryImporterTest` covers cancellation, progress and rollback. Automated timer/controller checks and a large-import responsiveness demonstration remain. |
| 3. Collections and recursion | `service/MarketplaceService.categoryIdsUnder()`, `collectCategoryIds()`, `validateCategories()`, `getWaitlist()`, `view()`, `recent()` | Recursively searches descendants; ArrayDeque/Queue preserves FIFO; per-member Stack preserves LIFO. `service/MarketplaceServiceTest` covers deep descendants, cycles, missing parents, waitlist order and bounded history. Prepare a manual category-recursion trace for the demo. |
| 4. Data analysis | `persistence/Database.priceSummary()`, `ratingSummary()`; `service/MarketplaceService.prices()`, `ratings()` | Current analysis uses SQL AVG/COUNT. `persistence/ReviewAnalyticsTest` checks arithmetic, review weighting, currencies and empty results. The instructor-requested Java grouping/frequency analysis of historical sales is still open (KI-01). |
| 5. Charts and labels | `ui/MarketplaceView.createAnalytics()`, `showAnalytics()`, `chart()`, `fillChart()` | Two bar charts show average listing price and review rating by category, with titles and axes. Update the view for the new analysis and verify its data, labels and empty state. A JUnit test of summary arithmetic is not proof of chart rendering. |
| 6. Interactive GUI controls | `ui/MarketplaceView.login()`, `postListing()`, `refresh()`, `reviewListing()`, `joinWaitlist()`, `startImport()`, `createListings()` | Login, search, post, review, waitlist, import and owner withdrawal have handlers. Previous separate application checks were recorded; repeat the required demonstrations after the refactor. |
| 7. JUnit and edge cases | Seven existing test classes, listed below | Prior verification recorded 81 passing tests. Add direct service login/reset tests, Java-analysis tests and controller state/timer coverage after implementation. Run the complete suite on JDK 21 without bypassing the enforcer. |
| 8. Contribution and understanding | Git history plus individual explanations | Preserve each member's actual commits. The supplied pre-check records at least one commit for all six members. Each member must explain their own contribution live; assignments and passing tests do not establish that understanding. |
| 9. Known issues and requirement map | Root `KNOWN_ISSUES.md` and this file | These files identify current behavior, open corrections and remaining checks. Keep both consistent with the code and actual results. |
| 10. Design document, Sections 5-7 | `docs/design-document.md` and `docs/design-document.pdf` | Section 5 covers API requests, response shape and use; Section 6 covers worker/tasks, JSON import and refresh; Section 7 covers current summaries and charts. Update the source/PDF for the parser split, MVC and Java analysis after they are implemented. |

## Existing test classes

| File | Examples already covered |
|---|---|
| `api/ProductCatalogClientTest.java` | Valid/invalid JSON, empty responses, matching order, decimal prices, failed HTTP status and timeout. |
| `model/ListingTest.java` | Subclass condition rules, polymorphism, prices, currency, identities and status. |
| `model/ListingValidationTest.java` | Category/member validation, title trimming and the maximum representable price. |
| `persistence/DatabaseTest.java` | Tables and initialization, wrong passwords, recovery-code consumption, migrations, salted hashes, ownership, rollback and restoration of missing condition details. |
| `persistence/ReviewAnalyticsTest.java` | Invalid/self/duplicate reviews, weighted category averages, currency separation, empty results and exclusion of historical sales from current listing averages. |
| `service/MarketplaceServiceTest.java` | Category recursion, FIFO/LIFO, account ownership, unknown members, separate sessions and search seeing another session's changes. |
| `service/SalesHistoryImporterTest.java` | Valid/invalid records, duplicates, malformed JSON, missing files, timestamps, progress, cancellation and transaction rollback. |

These are coverage descriptions from reading the tests, not a claim that a fresh test run happened during this documentation step.

## Accounts, schema and ownership

- `App.start()` connects `persistence/Database`, `service/MarketplaceService` and `ui/MarketplaceView`.
- `Database.initialize()` loads/upgrades the schema; `initializeAccounts()` creates account fields/indexes and initializes classroom credentials.
- `MarketplaceService.login()` and `resetPassword()` call `Database.authenticate()` and `resetPassword()`.
- `MarketplaceService.post()` and `myListings()` use the member's seller profile. `Database.withdrawListing()` rechecks listing ownership.
- `MarketplaceService.review()` validates member existence; `Database.addReview()` checks rating/comment, owner, listing status and duplicate reviews in its transaction.
- `Database.addInterest()` validates the buyer and item and stores joining order. `program/src/main/resources/db/schema.sql` enforces both BuyerInterests composite unique rules.

## Verification to complete after corrections

- Run the documented Maven command using JDK 21; report the actual test count and failures/skips.
- Launch the app and exercise sign-in, recovery, posting, ownership, reviews, import, charts and logout with temporary data.
- Demonstrate API access and failure recovery with the required network change.
- Demonstrate a large import, responsive controls, progress and cancellation rollback.
- Trace one recursive category search and explain FIFO/LIFO using actual records.
- Change the analysis inputs and show that the Java result and chart change correctly; check empty data and week boundaries.
- Compare the final design's class diagram, ERDs, fields and processes with the final source. Close known issues only when the corresponding evidence exists.
