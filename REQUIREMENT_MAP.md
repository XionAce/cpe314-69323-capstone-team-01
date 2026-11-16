# Requirement-to-code and test map

Campus Marketplace with Reviews. Updated 16 November 2026.

Main types are under `program/src/main/java/edu/uc/campusmarket/`; tests are under the corresponding `program/src/test/java/` package tree. Checked implementation is not a substitute for the instructor's live demonstration.

## Final-defense checklist

This table follows Week11_Checklist. The numbered implementation map below retains the earlier checkpoint structure; the nine mandatory technical requirements have their own map. Recorded checks support preparation and do not award points or replace the live defense.

| Final-defense criterion | Evidence and remaining action |
|---|---|
| 1. End-to-end behavior and invalid input | The latest UI revision passed 93 application assertions; seventeen independent two-process chat assertions also passed using temporary data. Demonstrate the full flow and a rejected invalid input live on the presentation machine. |
| 2. All nine mandatory requirements | The separate nine-requirement table below maps each requirement to current source and a runtime demonstration. |
| 3. Hierarchy and meaningful polymorphism | Listing, Textbook, Electronics and Furniture use overridden condition grading through the base type. The design explains responsibilities; ListingTest exercises the different outcomes. Open these classes and demonstrate them. |
| 4. Schema and parameterized SQL | The verified design matches eleven tables, 56 fields and fifteen foreign keys. DAO values use PreparedStatement parameters. Open the actual presentation database to compare with the ERD. |
| 5. Background work and stability | FxTaskRunner handles blocking work; import cancellation/rollback and chat refresh/draft isolation were checked. Demonstrate navigation during a long import. |
| 6. Analysis and labeled charts | Production import and Java analysis changed sample weekly counts from 3,2,1 to 3,2,4. Charts state their units and week boundaries. Repeat with prepared demonstration data. |
| 7. Passing tests and meaningful edge cases | Recorded JDK 21 verification passed 159 tests in fourteen classes, with no failures/errors/skips. Section 8 gives an edge case for each class. Run the suite and open a real assertion at defense. |
| 8. Code quality and Git history | Formatting/comment/method-size inspection passed. Preserve real member history and attribute new commits to actual contributors; assigned responsibility alone does not establish authorship. Each member reviews their recorded changes. |
| 9. Final design compilation | Sections 1-9 and Appendix are retained. The rendered Word/PDF have 33 pages, fourteen editable Word tables and eleven figures. All fifty contents references and logical page numbers were checked. Section 9 references existing member commits. The live contents field was refreshed using LibreOffice; check/update it in desktop Word on the submission machine if the instructor requires that application specifically. |
| 10. Peer evaluation | Each member privately completes the required ratings of teammates. Do not fabricate ratings or commit private evaluations. |
| Individual Q&A | Each member prepares to explain their own recorded code, its rule, a meaningful test and what would break if it changed. This is a separate per-member defense requirement. |

PR #14 was merged on 14 November. The latest corrections and updated 24-slide presentation are committed and pushed to `project/weel-11` in [PR #15](https://github.com/XionAce/cpe314-69323-capstone-team-01/pull/15), awaiting review and merge into main. After merging, build/run a fresh clone of main on the presentation machine and complete the timed rehearsal. Keep the existing branches.

## Earlier checkpoint implementation map

| Requirement | Current implementation | Evidence and remaining work |
|---|---|---|
| 1. Public API and meaningful JSON | `api/ProductCatalogClient.fetch()`, `api/ProductCatalogParser.parse()`, `dto/CatalogProduct`, `service/CatalogMapper.matches()`, `service/CatalogService.suggest()`, `controller/ListingController.suggest()` | Validated products supply supported electronics references. `api/ProductCatalogClientTest` covers shape/type validation, matching, HTTP failure and timeout. A real HTTP-200/20-product response and hash/timestamp metadata are saved under test resources, with a passing parser/mapper contract case. The live GUI network toggle remains (KI-03). |
| 2. Background work and responsiveness | `FxTaskRunner.run()`; `AnalyticsController.importFile()/cancelImport()`; `SalesHistoryImporter.importFile()`; `App.stop()` | One named serial worker, Platform.runLater result/error delivery, UI-thread progress dispatch, cancellation and rollback. Quiet refresh keeps foreground actions available. |
| 3. Collections and recursion | `CategoryTree.categoryIdsUnder()/collectCategoryIds()/validateCategories()`; `WaitlistService.order()`; `ListingService.view()/recent()` | Recursive descendants with cycle/parent validation; ArrayDeque/Queue FIFO; per-member Stack LIFO. `MarketplaceServiceTest` covers the focused production services through a test-only adapter. Prepare the recursion trace at demo. |
| 4. Java analysis | `dao/HistoryDao.soldDates()` -> `service/SalesAnalysis.weeklyCounts()/summary()` -> `service/AnalyticsService.load()` | Java TreeMap groups UTC timestamps into Monday-starting weeks, counts and sorts them, then identifies tied busiest weeks. `SalesAnalysisTest` covers offsets, year/week boundaries, ties, ordering and empty history. `HistoryAnalysisTest` changes actual saved rows and proves the result changes. |
| 5. Charts and labels | `controller/AnalyticsController.refresh()` -> `ui/ChartsView` | Weekly frequency bars plus price and rating bars. Axes state week/timezone, sales count, category, currency or rating. Snapshot equality avoids unnecessary redraws. App checks verify weekly data/labels. Empty weeks are not invented. |
| 6. Interactive GUI | `ui/LoginView`, `RegistrationView`, `PasswordResetView`, `ListingTableView`, `ListingFormView`, `ReviewsView`, `ImportView`, `ChartsView` call their feature controllers | Login, register, reset, personal home, search, post, edit, review, join, leave, withdraw, import, cancel and chart-refresh actions are wired. Small views contain controls/bindings/dialogs; services and DAOs handle rules/storage. |
| 7. JUnit and edge cases | Fourteen test classes listed below | **159 tests passed** in the recorded JDK 21 verification, zero failures/errors/skips. Repeat relevant tests after any further code change. GUI and real-network checks are separate evidence. |
| 8. Contribution and understanding | Existing Git history plus individual explanations | Earlier member commits are retained. Each person must understand and explain their actual contribution. Assigned ownership and passing tests do not prove individual understanding. |
| 9. Known issues and mapping | Root `KNOWN_ISSUES.md` and this file | Current corrections, limitations and remaining verification are explicit. Update status from evidence, not from planned work. |
| 10. Earlier design Sections 5-7 | `docs/design-document.md`, `docs/design-document.pdf` and `tools/export_design.py` | Source and PDF describe the separated API layers, controllers, background import, Java weekly analysis and charts. Earlier context, account/review rules, Appendix and all 85 Java types are included. All eleven tables/56 fields/fifteen FKs match the schema; an overview and four detail views use exact field names. The exported layout was checked visually. |

## Nine mandatory technical requirements

This table follows the original project specification. It is separate from the earlier checkpoint scoring map above. Demonstrations must use the running application and current source.

| Requirement | Source to inspect | Runtime demonstration |
|---|---|---|
| 1. Object-oriented design | `model/Listing`, `Textbook`, `Electronics`, `Furniture`; `Listing.getConditionLabel()` calls overridden `gradeCondition()` through the base type. | Browse the three seeded listing types and explain why different condition evidence produces different grades. |
| 2. Data structure and recursion | `WaitlistService.order()`; `ListingService.view()/recent()`; `CategoryTree.categoryIdsUnder()/collectCategoryIds()` | Join a queue as two buyers; show FIFO order and LIFO recent views. Search Electronics and find descendants. Trace category IDs 3 -> 5 -> 6 -> 7, returning [3,5,6,7]; the leaf has no recursive child call. |
| 3. Automated testing | Fourteen JUnit classes in `program/src/test/java/` | Run Maven verification on the presentation machine and open an actual edge-case assertion. Historical passing results do not replace this live run. |
| 4. Data modeling and database | Eleven tables in `schema.sql`; `dao/*`; `Database.open()` | Inspect the exact demo `.db` against the ERD. Show prepared insert/read/update SQL; removal archives a listing, while waitlist leave uses a parameterized DELETE. |
| 5. API consumption | `ProductCatalogClient.fetch()`, `ProductCatalogParser.parse()`, `CatalogMapper.matches()`, `CatalogService.suggest()` | Request an electronics reference with HTTP details and a price range. Repeat offline and continue manual entry. The captured response is covered by a parser/mapper test; the live GUI network on/off check remains. |
| 6. Event-driven GUI | Small `ui/*` views and feature controllers | Use login, search, post, join/review, edit and import controls. Show rejected empty/invalid input without a crash. |
| 7. Concurrency | `FxTaskRunner.run()`, `AnalyticsController.importFile()/cancelImport()`, `SalesHistoryImporter.importFile()` | Import a large JSON file while scrolling/navigating; cancel and confirm rollback of that unfinished import. |
| 8. Data mining | `SalesAnalysis.weeklyCounts()/summary()`, `HistoryDao.soldDates()` | Import the six-record sample, then the follow-up: controlled weekly frequencies change from 3,2,1 to 3,2,4. |
| 9. Data visualization | `ChartsView`, `AnalyticsController.refresh()` | Show the weekly chart changing with stored data, its title, Monday/UTC X-axis and sales-count Y-axis; distinguish it from the currency-filtered price chart. |

The earlier six-record sample and three-record follow-up are synthetic classroom data, not captured API products or proof of purchases. Other already-imported history changes the displayed totals.

## Tests

| Test class | Main coverage |
|---|---|
| `api/ProductCatalogClientTest` | Strict JSON shape/types, valid/empty/invalid responses, compatible matching, HTTP errors and timeout. |
| `controller/FxTaskRunnerTest` | Six cases cover execution order, UI dispatch, foreground busy state, quiet reads, rejection and preserving a newer action's status. |
| `model/ListingTest` | Subclass condition rules, polymorphism, exact money, identity/currency/status validation. |
| `model/ListingValidationTest` | Category/member validation, title trimming and the storage price boundary. |
| `persistence/DatabaseTest` | Eleven-table initialization, account upgrades, hashes, wrong passwords, recovery/rollback, saved subtypes, ownership and condition restoration. It uses generated credentials from the test database. Additional cases cover creation dates, version-3 migration and rollback, sold-state preservation, missing subtype rows and two-thread waitlist races. |
| `persistence/ReviewAnalyticsTest` | Review validation/duplicates, weighted ratings, currency separation, empty summaries and keeping historical sales out of current listing averages. Successful-review fixtures explicitly join the waitlist. Sold/removed rows are excluded from current listing means. |
| `service/MarketplaceServiceTest` | Category recursion, FIFO/LIFO, ownership, unknown members, session separation and cross-session reads. |
| `service/SalesHistoryImporterTest` | Valid/invalid/duplicate rows, malformed JSON, timestamps, progress, cancellation and rollback. |
| `service/AccountAndReviewTest` | First-start credentials shown once, preserved accounts, registration uniqueness/validation, service login/reset and waitlist eligibility for reviews. |
| `service/SalesAnalysisTest` | Java weekly grouping, UTC boundaries, chronological ordering, ties and immutable/empty results. |
| `service/HistoryAnalysisTest` | Real JSON import and changed stored rows producing changed Java analysis without altering current-price means. |
| `service/PersonalFeaturesTest` | Member-scoped home data and ratings, edit ownership/status/money validation, related-row preservation, leave/rejoin position changes and review retention. |
| `service/ChatServiceTest` | Private participant filtering, separate buyers, trimmed message boundaries, saved ordering, sold/removed rules, preserved queue/history, version-4 upgrades and concurrent replies. |
| `controller/ControllerTest` | Service-to-session login/registration, secret clearing, active-member posting, observable results, selection preservation, stale-result rejection and busy/dialog/logout/close refresh policy. Also covers incomplete rows, archive status filters and retrying a quick filter change after an older request. Also verifies deferred home loading and rejection of home results after logout. Uses a fake clock/runner; opens no window. |

## Account and data flow

- `App` constructs the database, DAOs, services, controllers and views and owns the shared worker. `UserSession` supplies the active member identity.
- `HomeController` loads `HomeService`/`HomeDao` account-filtered dashboard data. `HomeItem`/`HomeSummary` carry the snapshot; logout clears it.
- `AccountController` calls `MemberService`, which uses `MemberDao` for login, registration and password reset. `CredentialSecurity` implements salted PBKDF2. `AccountSetup` adds missing account fields without erasing existing data.
- `ListingController` calls `ListingService`; `ListingFactory` creates the chosen subtype. `CategoryTree` supplies recursion, `ListingService` keeps recent-view stacks and `WaitlistService` manages FIFO order. DAOs execute the queries. ListingDao records created_at and applies owner-checked lifecycle transitions; the controller exposes archive filters and safe incomplete-detail state.
- `WaitlistController` uses `WaitlistService`; `InterestDao` validates and saves joining order, removes only the active buyer's interest and counts the current position. The schema retains `UNIQUE(listingId,buyerId)` and `UNIQUE(listingId,joiningOrder)`.
- `ReviewController` uses `ReviewService`; `ReviewDao.addReview()` rechecks owner, member, status, interest and duplicates in its write transaction.
- `AnalyticsController` uses `AnalyticsService`, `AnalyticsDao`, `HistoryDao` and `SalesHistoryImporter`. Java computes the weekly frequencies; the chart only renders the returned records.
- `RefreshController.tick()` applies the polling policy. `FxRefreshSchedule` supplies the production timer; tests inject a deterministic clock. `FxTaskRunner` exposes busy/status/error properties; views bind them without knowing JDBC.

## Final-defense submission checks

The actual HTTP-200 response is saved in `program/src/test/resources/api/fakestore-products.json`, with timestamp/hash metadata and a Java parser/mapper contract test. Repeat the GUI network-toggle demonstration on the presentation machine (KI-03). Local-server tests remain separate evidence.

The instructor's request to clean up stale ERD images on older branches remains KI-06. Prepare the recursion trace and each member's explanation. PR #14 was merged into main on 14 November. The latest corrections are committed and pushed to `project/weel-11` in PR #15, awaiting review and merge into main. After merging, verify a fresh clone of main on the presentation machine. Preserve branches and member history. The recorded 93 application assertions (60 workflow and 33 UI follow-up checks) and 17 independent two-process assertions are separate from the 159 JUnit tests; Maven clean verification repeated successfully on 16 November without rerunning those GUI checks.

The four earlier Additional_B1 feature requests are implemented. Personal home, owner title/price editing, waitlist leave/current positions, the standards pass and a bounded data-read measurement are checked. The live GUI network demonstration, older-branch image cleanup and very large UI-rendering measurements remain separate follow-ups.
## Additional feedback mapping

| Additional request | Implementation and evidence |
|---|---|
| Listing creation date and lifecycle | Listings.created_at; model/Listing.getCreatedAt(); AccountSetup.upgradeListings(); ListingDao.markSold()/withdrawListing(); ListingController.markSold()/statusFilterProperty(); UI cards/table/details and owner confirmations. DatabaseTest covers persistence, preservation and migration. |
| Messaging/offer scope | Private listing chats now use ChatView/ChatController/ChatService/ChatDao and Messages. Offers/payments remain out of scope. Two independent app processes exchanged replies on a temporary shared SQLite file. |
| Simultaneous waitlist joins | InterestDao.addInterest() performs one atomic INSERT ... SELECT with MAX(joiningOrder)+1 and eligibility predicates. DatabaseTest.concurrentBuyersReceiveDistinctAtomicWaitlistPositions() and simultaneousRepeatJoinSavesOnlyOneInterest() run two threads on independent connections. |
| Missing electronics/furniture details | ListingDao returns Listing.incomplete() with common fields and a warning; getConditionLabel() avoids invented grades. Controller refresh, cards, list and details stay usable. DatabaseTest/MarketplaceServiceTest/ControllerTest and the application checks cover missing rows and owner restoration. |

## Answers to the instructor's four questions

| Question | Current factual answer |
|---|---|
| Branch name | The earlier submission used `feature/milestone-2-program`. The required `project/milestone-2` branch was submitted through PR #14 and merged into main on 14 November. The latest corrections are committed and pushed to `project/weel-11` in PR #15, awaiting review and merge into main. |
| What is the Java analysis? | `SalesAnalysis.weeklyCounts()` groups saved historical sale timestamps into Monday-starting UTC weeks with a TreeMap. `summary()` finds the total and tied busiest weeks. The weekly bar chart displays those counts. Category SQL averages remain separate. |
| How does a member receive the recovery code? | Registration shows it once in a read-only field. First-time seeded credentials appear in a local startup dialog. A successful password reset shows a replacement code and consumes the previous code. The member must save the code; there is no email delivery. |
| Why is the program in a subfolder? | The existing Eclipse/Antigravity Maven layout is retained. Root README commands explicitly use `./program/mvnw.cmd -f ./program/pom.xml` to build or run from the repository root. |

Earlier UI presentation follow-up: App.start() maximizes the stage; MarketplaceView builds top navigation; ReviewsView uses ReviewController.load() for saved star/comment cards in a right-side popup; ImportView binds its loader to importingProperty(); ChartsView spaces/colors bars and shows tooltips. That earlier application harness passed 46 checks, separately from JUnit. The latest recorded UI revision passed 93 application assertions.

## Personal-feature follow-up mapping

| Feature | Code and evidence |
|---|---|
| Personal home | HomeView -> HomeController.load() -> HomeService.loadFor() -> HomeDao.loadFor(); HomeItem/HomeSummary carry immutable results. Member-filtered queries share one read transaction. PersonalFeaturesTest verifies account isolation and weighted received-rating averages; ControllerTest rejects old-session results. |
| Owner editing | ListingTableView edit dialog -> ListingController.update() -> ListingService.update() -> ListingDao.update(). Active/owner predicates run in the UPDATE. PersonalFeaturesTest covers stale status, other owners, invalid money and retained relations. |
| Leave/current position | WaitlistController.leave() -> WaitlistService -> InterestDao.leave()/position(). Details and My home display the count-based position. Tests cover leave, advancement, tail rejoin, another buyer and retained reviews. |
| Standards | Eclipse formatting with four spaces and 100-column wrapping where possible; named rule limits. Java syntax-tree inspection confirms no main methods above 50 lines or missing public type/non-overriding-method comments, and no unbraced blocks/wildcard imports/tabs/lines over 120. |
| Measured data reads | Temporary SQLite: 10,003 listings, 10,000 interests, 50,000 sales; five warm runs. Search 80.2-348.6 ms; analysis 169.9-286.4 ms; home 49.5-63.0 ms. Rendering, cold startup and multiple simultaneous users excluded. |

A readiness recheck found that ListingFactory accepted unknown types as furniture and unsupported observations as default condition evidence. It now rejects both with IllegalArgumentException; ListingValidationTest includes regression cases. SETUP.md and TEAM_INFO.md were synchronized with current Java/JSON setup and the existing README roster. The capture helper saves real public data only when its request succeeds; the actual captured response is now covered by ProductCatalogClientTest.capturedPublicResponseParsesAndProvidesComparableElectronics().

Live public API follow-up: the normal production client returned HTTP 200 and parsed 20 products. Six real selling-screen checks then passed (response, reference range, currency warning, preserved entered price, background busy state and usable navigation). Offline/reconnection behavior still requires the presentation-machine check.


## Private listing chat

Open an item and choose **Open chat**. The buyer writes first; the seller replies in **Messages**. Only that buyer and the item's seller can read or send through the app. A different buyer gets a separate conversation. Messages are plain text, 1-2000 characters, saved with UTC timestamps and ordered by database ID. They do not change waitlists, reserve items or process payments.

To demonstrate automatic updates, run the current program twice against the same data folder and sign in with two different accounts. Keep both Messages pages open. The existing three-second refresh displays saved replies without a manual refresh and preserves unsent text. Separate laptops do not share local messages. Existing sold-item chats stay usable for pickup; removed-item chats retain read-only history.

Schema version 5 adds Messages and indexes while preserving earlier accounts and records. The current design includes eleven tables, 56 columns, fifteen foreign keys and 85 main Java types. Eight chat service tests and three chat-controller tests were added. A real two-process GUI test passed seventeen assertions, separately from JUnit.


## Current instructor-correction evidence

| Correction | Code and verification |
|---|---|
| Quiet refresh and user actions | FxTaskRunner.run(), six FxTaskRunnerTest cases; a foreground action queues behind one quiet read without being dropped. |
| Avoid unchanged full reads | DatabaseChanges.version(), RefreshController.checkChanges(); ControllerTest and DatabaseTest cover unchanged reads, external commits and retry after failure. |
| Catalog outcomes | CatalogService.result() returns CatalogSuggestion OK/EMPTY/FAILED; ListingController.suggest() handles rejected scheduling. Client and controller regressions cover both. |
| Clear errors and retained records | ListingService, ServiceCall, ReviewDao and SavedDates; service/DAO tests cover invalid price/comment, SQL causes and malformed stored dates. |
| Focused services and recursion | MemberService, ListingService, ReviewService, WaitlistService and CategoryTree; the old MarketplaceServiceTest now uses a test-only MarketplaceFixture to execute these production services. |
| Accessible and responsive views | The initial 60 GUI assertions on 15 November include linked price labels, Enter card activation, inline form feedback, invalid edits staying open, 800x600 Sign out visibility and the initial title bar. A further 33 layout, file-selection, import-result and analytics-breakdown checks brought the UI revision total to 93 application assertions. |
| Private chat after worker changes | 17 independent two-process assertions passed on 15 November, preserving draft text, recipient selection, saved order and buyer privacy. |

Maven clean verification on 16 November passed 159 tests in fourteen classes, with zero
failures, errors or skipped tests. Source inventory is 85 top-level
types; the unchanged schema remains eleven tables, 56 columns and fifteen foreign keys.
See design Sections 3, 5, 6 and 8 for the revised behavior and actual verification.
