# Requirement-to-code and test map

Campus Marketplace with Reviews. Updated 13 November 2026.

Main types are under `program/src/main/java/edu/uc/campusmarket/`; tests are under the corresponding `program/src/test/java/` package tree. Checked implementation is not a substitute for the instructor's live demonstration.

| Requirement | Current implementation | Evidence and remaining work |
|---|---|---|
| 1. Public API and meaningful JSON | `api/ProductCatalogClient.fetch()`, `api/ProductCatalogParser.parse()`, `dto/CatalogProduct`, `service/CatalogMapper.matches()`, `service/CatalogService.suggest()`, `controller/ListingController.suggest()` | Validated products supply supported electronics references. `api/ProductCatalogClientTest` covers shape/type validation, matching, HTTP failure and timeout. Public-network toggle demonstration and a freshly captured real response remain (KI-03). |
| 2. Background work and responsiveness | `controller/FxTaskRunner.run()`; `controller/AnalyticsController.importFile()/cancelImport()`; `service/SalesHistoryImporter.importFile()`; `App.stop()` | One named worker, JavaFX Task result/error handlers, UI-thread progress dispatch, a cancel flag and transaction rollback. Importer tests cover progress, malformed documents and cancellation; the app check exercises the wired controls. |
| 3. Collections and recursion | `service/MarketplaceService.categoryIdsUnder()/collectCategoryIds()/validateCategories()/getWaitlist()/view()/recent()` | Recursive descendants with cycle/parent validation; ArrayDeque/Queue FIFO; per-member Stack LIFO. `MarketplaceServiceTest` covers deep paths, invalid hierarchies and ordering. Prepare the manual recursion trace at demo. |
| 4. Java analysis | `dao/HistoryDao.soldDates()` -> `service/SalesAnalysis.weeklyCounts()/summary()` -> `service/AnalyticsService.load()` | Java TreeMap groups UTC timestamps into Monday-starting weeks, counts and sorts them, then identifies tied busiest weeks. `SalesAnalysisTest` covers offsets, year/week boundaries, ties, ordering and empty history. `HistoryAnalysisTest` changes actual saved rows and proves the result changes. |
| 5. Charts and labels | `controller/AnalyticsController.refresh()` -> `ui/ChartsView` | Weekly frequency bars plus price and rating bars. Axes state week/timezone, sales count, category, currency or rating. Snapshot equality avoids unnecessary redraws. App checks verify weekly data/labels. Empty weeks are not invented. |
| 6. Interactive GUI | `ui/LoginView`, `RegistrationView`, `PasswordResetView`, `ListingTableView`, `ListingFormView`, `ReviewsView`, `ImportView`, `ChartsView` call their feature controllers | Login, register, reset, personal home, search, post, edit, review, join, leave, withdraw, import, cancel and chart-refresh actions are wired. Small views contain controls/bindings/dialogs; services and DAOs handle rules/storage. |
| 7. JUnit and edge cases | Twelve test classes listed below | **128 tests passed** in the recorded JDK 21 verification, zero failures/errors/skips. Repeat relevant tests after any further code change. GUI and real-network checks are separate evidence. |
| 8. Contribution and understanding | Existing Git history plus individual explanations | Earlier member commits are retained. Each person must understand and explain their actual contribution. Assigned ownership and passing tests do not prove individual understanding. |
| 9. Known issues and mapping | Root `KNOWN_ISSUES.md` and this file | Current corrections, limitations and remaining verification are explicit. Update status from evidence, not from planned work. |
| 10. Design Sections 5-7 | `docs/design-document.md`, `docs/design-document.pdf` and `tools/export_design.py` | Source and PDF describe the separated API layers, controllers, background import, Java weekly analysis and charts. Earlier context, account/review rules, Appendix and all 75 Java types are included. All ten tables/50 fields/twelve FKs match the schema; an overview and three detail views use exact field names. The exported layout was checked visually. |

## Tests

| Test class | Main coverage |
|---|---|
| `api/ProductCatalogClientTest` | Strict JSON shape/types, valid/empty/invalid responses, compatible matching, HTTP errors and timeout. |
| `model/ListingTest` | Subclass condition rules, polymorphism, exact money, identity/currency/status validation. |
| `model/ListingValidationTest` | Category/member validation, title trimming and the storage price boundary. |
| `persistence/DatabaseTest` | Ten-table initialization, account upgrades, hashes, wrong passwords, recovery/rollback, saved subtypes, ownership and condition restoration. It uses generated credentials from the test database. Additional cases cover creation dates, version-3 migration and rollback, sold-state preservation, missing subtype rows and two-thread waitlist races. |
| `persistence/ReviewAnalyticsTest` | Review validation/duplicates, weighted ratings, currency separation, empty summaries and keeping historical sales out of current listing averages. Successful-review fixtures explicitly join the waitlist. Sold/removed rows are excluded from current listing means. |
| `service/MarketplaceServiceTest` | Category recursion, FIFO/LIFO, ownership, unknown members, session separation and cross-session reads. |
| `service/SalesHistoryImporterTest` | Valid/invalid/duplicate rows, malformed JSON, timestamps, progress, cancellation and rollback. |
| `service/AccountAndReviewTest` | First-start credentials shown once, preserved accounts, registration uniqueness/validation, service login/reset and waitlist eligibility for reviews. |
| `service/SalesAnalysisTest` | Java weekly grouping, UTC boundaries, chronological ordering, ties and immutable/empty results. |
| `service/HistoryAnalysisTest` | Real JSON import and changed stored rows producing changed Java analysis without altering current-price means. |
| `service/PersonalFeaturesTest` | Member-scoped home data and ratings, edit ownership/status/money validation, related-row preservation, leave/rejoin position changes and review retention. |
| `controller/ControllerTest` | Service-to-session login/registration, secret clearing, active-member posting, observable results, selection preservation, stale-result rejection and busy/dialog/logout/close refresh policy. Also covers incomplete rows, archive status filters and retrying a quick filter change after an older request. Also verifies deferred home loading and rejection of home results after logout. Uses a fake clock/runner; opens no window. |

## Account and data flow

- `App` constructs the database, DAOs, services, controllers and views and owns the shared worker. `UserSession` supplies the active member identity.
- `HomeController` loads `HomeService`/`HomeDao` account-filtered dashboard data. `HomeItem`/`HomeSummary` carry the snapshot; logout clears it.
- `AccountController` calls `MemberService`, which uses `MemberDao` for login, registration and password reset. `CredentialSecurity` implements salted PBKDF2. `AccountSetup` adds missing account fields without erasing existing data.
- `ListingController` calls `ListingService`; `ListingFactory` creates the chosen subtype. `MarketplaceService` preserves category/queue/stack and ownership rules. `ListingDao`, `SellerDao` and `CategoryDao` execute the queries. ListingDao records created_at and applies owner-checked ACTIVE-to-SOLD/REMOVED transitions; the controller exposes archive filters and safe incomplete-detail state.
- `WaitlistController` uses `WaitlistService`; `InterestDao` validates and saves joining order, removes only the active buyer's interest and counts the current position. The schema retains `UNIQUE(listingId,buyerId)` and `UNIQUE(listingId,joiningOrder)`.
- `ReviewController` uses `ReviewService`; `ReviewDao.addReview()` rechecks owner, member, status, interest and duplicates in its write transaction.
- `AnalyticsController` uses `AnalyticsService`, `AnalyticsDao`, `HistoryDao` and `SalesHistoryImporter`. Java computes the weekly frequencies; the chart only renders the returned records.
- `RefreshController.tick()` applies the polling policy. `FxRefreshSchedule` supplies the production timer; tests inject a deterministic clock. `FxTaskRunner` exposes busy/status/error properties; views bind them without knowing JDBC.

## Remaining submission checks

Capture a real API response into `program/src/test/resources` and use it in a parser contract test, then repeat the live network-toggle demonstration. The existing local-server tests use constructed responses; they are not a captured public response (KI-03).

The standards follow-up is complete (KI-07). The instructor's request to clean up stale ERD images on older branches remains (KI-06). Prepare the recursion trace and each member's explanation. Review and commit the local follow-up, then push `project/milestone-2` to update existing PR #14, **Milestone 2 - Team 01**. Preserve the existing branches and member history. No PR creation or merge is claimed here. The 46 actual-application checks are separate from the 128 JUnit tests.

All four Additional B1 requests are implemented. Personal home, owner title/price editing, waitlist leave/current positions, the standards pass and a bounded data-read measurement are now checked. The actual API response/network demonstration, older-branch image cleanup and very large UI-rendering measurement remain open.
## Additional feedback mapping

| Additional request | Implementation and evidence |
|---|---|
| Listing creation date and lifecycle | Listings.created_at; model/Listing.getCreatedAt(); AccountSetup.upgradeListings(); ListingDao.markSold()/withdrawListing(); ListingController.markSold()/statusFilterProperty(); UI cards/table/details and owner confirmations. DatabaseTest covers persistence, preservation and migration. |
| Messaging/offer scope | Explicitly out of scope in design Section 1 and program README. No message table or chat behavior is claimed. |
| Simultaneous waitlist joins | InterestDao.addInterest() performs one atomic INSERT ... SELECT with MAX(joiningOrder)+1 and eligibility predicates. DatabaseTest.concurrentBuyersReceiveDistinctAtomicWaitlistPositions() and simultaneousRepeatJoinSavesOnlyOneInterest() run two threads on independent connections. |
| Missing electronics/furniture details | ListingDao returns Listing.incomplete() with common fields and a warning; getConditionLabel() avoids invented grades. Controller refresh, cards, list and details stay usable. DatabaseTest/MarketplaceServiceTest/ControllerTest and the application checks cover missing rows and owner restoration. |

## Answers to the instructor's four questions

| Question | Current factual answer |
|---|---|
| Branch name | The earlier submission used `feature/milestone-2-program`. The required `project/milestone-2` branch and PR #14 now exist; the latest follow-up needs committing/pushing to update that PR. |
| What is the Java analysis? | `SalesAnalysis.weeklyCounts()` groups saved historical sale timestamps into Monday-starting UTC weeks with a TreeMap. `summary()` finds the total and tied busiest weeks. The weekly bar chart displays those counts. Category SQL averages remain separate. |
| How does a member receive the recovery code? | Registration shows it once in a read-only field. First-time seeded credentials appear in a local startup dialog. A successful password reset shows a replacement code and consumes the previous code. The member must save the code; there is no email delivery. |
| Why is the program in a subfolder? | The existing Eclipse/Antigravity Maven layout is retained. Root README commands explicitly use `./program/mvnw.cmd -f ./program/pom.xml` to build or run from the repository root. |

UI presentation follow-up: App.start() maximizes the stage; MarketplaceView builds top navigation; ReviewsView uses ReviewController.load() for saved star/comment cards in a right-side popup; ImportView binds its loader to importingProperty(); ChartsView spaces/colors bars and shows tooltips. The final application harness passed 46 checks, separately from JUnit.

## Personal-feature follow-up mapping

| Feature | Code and evidence |
|---|---|
| Personal home | HomeView -> HomeController.load() -> HomeService.loadFor() -> HomeDao.loadFor(); HomeItem/HomeSummary carry immutable results. Member-filtered queries share one read transaction. PersonalFeaturesTest verifies account isolation and weighted received-rating averages; ControllerTest rejects old-session results. |
| Owner editing | ListingTableView edit dialog -> ListingController.update() -> ListingService.update() -> ListingDao.update(). Active/owner predicates run in the UPDATE. PersonalFeaturesTest covers stale status, other owners, invalid money and retained relations. |
| Leave/current position | WaitlistController.leave() -> WaitlistService -> MarketplaceService -> InterestDao.leave()/position(). Details and My home display the count-based position. Tests cover leave, advancement, tail rejoin, another buyer and retained reviews. |
| Standards | Eclipse formatting with four spaces and 100-column wrapping where possible; named rule limits. Java syntax-tree inspection confirms no main methods above 50 lines or missing public type/non-overriding-method comments, and no unbraced blocks/wildcard imports/tabs/lines over 120. |
| Measured data reads | Temporary SQLite: 10,003 listings, 10,000 interests, 50,000 sales; five warm runs. Search 80.2-348.6 ms; analysis 169.9-286.4 ms; home 49.5-63.0 ms. Rendering, cold startup and multiple simultaneous users excluded. |
