# Milestone 2: run and explain the prototype

This continues the simplified Week 9 program. Java 21, JavaFX, SQLite/JDBC and Maven remain the shared setup. The three listing subclasses, category recursion, waitlist queue and recent-view stack remain.

## Instructor corrections - 7 November 2026

The instructor's feedback reviews Week 8 and Week 9, not a Milestone 2 score. These corrections carry the relevant fixes into the current Milestone 2 program and documents.

- `design-document.md` is now the complete design master, with Sections 1-7. The PDF is exported from it. Sections 3-4 are no longer side notes.
- The design and proposal each show all ten actual tables and all columns in three readable ERD views. The design includes the actual CREATE TABLE statements. The report says both ten and eleven tables; clarify the discrepancy using schema.sql instead of adding an unused table.
- Proposal risks now cover catalog currency/mismatch, duplicated prices from review joins, repeat/cancelled JSON imports and missing subtype details. These are questions; no instructor answer is invented.
- The required JDK 21 and version checks are prominent in the READMEs. The normal Maven build was run with the enforcer enabled.
- Owners can use **Restore missing details** to re-enter observed condition for a listing with no detail record. Members load before listing search, so recovery is available even after a search error. Existing details cannot be overwritten. The operation preserves the listing, reviews and waitlist and rolls back on failure.

Verification on 7 November: `mvnw.cmd -o clean verify`, followed by `mvnw.cmd -o verify` after the final UI correction, passed **43 tests, 0 failures, 0 errors, 0 skipped** on Temurin JDK 21.0.12.1 and Maven 3.9.11. Database tests cover restoration of all three types, incorrect owner/type, existing details and rollback without lost data.

Eleven checks against the actual JavaFX App and an isolated temporary database passed: recovery instructions, member selection after an error, owner-only candidates, cancellation, explicit condition selection with no default, restored grading, preserved review/waitlist, table reload, selective disabling and one save after repeated handler calls. The public API was not called again for this correction; its earlier live verification is historical evidence below.

Still requiring team/instructor follow-up: Emanuelle's write access and genuine contribution, oral explanations, the exact Milestone 1 checkpoint branch name after PR #5 was merged, and the still-open original proposal PR #2. See work-plan.md. This update does not claim those administrative or individual requirements are complete.

## Run it

In Antigravity, open the existing CampusMarketplace workspace. Wait for Maven to resolve the added Gson 2.13.2 JSON library, then use **Run Campus Marketplace**. In Eclipse, refresh the project, choose **Maven > Update Project**, then run the existing **Run-Campus-Marketplace** Java Application profile.

Optional PowerShell check, from the repository:

```powershell
cd C:\Users\HP\Documents\Codex\campus-marketplace-work\program
.\mvnw.cmd clean verify
```

The normal project dependency remains portable: no Eclipse installation path is stored in pom.xml or application code.

## What to demonstrate

1. **Marketplace:** post a textbook, select another demo member, view it and join its waitlist. Click **Review listing**, enter 1–5 and a comment. Self-review and repeat reviews are rejected.
2. **Live API:** set the listing type to ELECTRONICS and enter `monitor`. Click **Suggest category / price**. A fresh FakeStoreAPI request displays its timestamp/status, matches, Electronics category and catalog price range. Choose your final PHP price manually because the API response does not state its currency. Textbook/furniture searches may return no comparable product; manual entry remains available.
3. **Sales history:** click **Import JSON sales history** and choose `program/samples/sales-history.json`. Confirm three saved rows on a fresh database. Repeat it: existing source IDs count as duplicates. These are fictional demo sales, not actual campus transactions.
4. **Background operation:** the three-row file is too quick for a responsiveness demo. Generate a large fictional log using the command below. Start importing it, switch tabs, scroll/select the loaded table or open **About this project** while import runs. Click **Cancel import** to roll back new rows. Existing saved records remain.
5. **Analytics:** click **Refresh analytics**. Show both charts and review counts. Post another listing in the same category, refresh, and explain how the average changed. Price and review summaries use actual saved Listings/Reviews, with separate queries to avoid duplicate prices. Imported historical sale prices are not current listing prices and do not change that chart.
6. Run the JUnit suite live and explain a meaningful edge case.

Generate the large demo file from program:

```powershell
.\samples\Create-LargeSalesHistory.ps1
```

The script prints the generated file path in your temporary folder. It defaults to 100,000 rows. Use `-Count 200000` on a fast computer if necessary. No artificial delay is added to production code. Delete the generated demo file through File Explorer when finished.

## JSON format and import rule

See the small sample for exact field names. Category paths must uniquely follow the existing tree starting at All finds. IDs must be nonblank; currency is three uppercase letters; price is positive with at most two decimal places; soldAt is ISO-8601 with a timezone.

The importer streams one record at a time and uses one transaction. Invalid field values are counted/skipped with a brief reason. Duplicate sourceRecordId values are skipped. Malformed JSON, database failure or cancellation rolls back all new rows from the current import. Existing rows are not erased. Progress counts are provisional until commit.

## Where the nine requirements live

Paths below are relative to program/src/main/java/edu/uc/campusmarket unless stated otherwise.

| Requirement | Source |
|---|---|
| OOD/polymorphism | model/Listing.java and Textbook/Electronics/Furniture; UI calls gradeCondition() through Listing |
| Collections and recursion | service/MarketplaceService.java: Queue, Map of Stacks, collectCategoryIds() with a leaf base case |
| Automated tests | program/src/test/java: seven test files, normal and edge cases |
| Database/JDBC | persistence/Database.java; program/src/main/resources/db/schema.sql |
| REST API consumption | api/ProductCatalogClient.java; api/CatalogProduct.java; MarketplaceView.suggest() |
| Event-driven GUI | ui/MarketplaceView.java: post/search/view/waitlist/review/suggest/import/analytics handlers |
| Concurrency | MarketplaceView.runTask()/startImport(); service/SalesHistoryImporter.java |
| Data mining | Database.priceSummary()/ratingSummary(); service forwards summaries |
| Visualization | MarketplaceView.createAnalytics()/fillChart(): labeled JavaFX bar charts |

## Verification and practical limits

On 3 November 2026 at 23:48 (Asia/Manila), the normal project wrapper passed `mvnw.cmd -o clean verify` using the normal dependency cache: **40 tests, zero failures, zero errors, zero skipped**. This includes the two ListingValidationTest cases. Earlier verification used a separate writable cache while dependencies were being resolved; no private verification settings or machine-specific dependency path are included in the project.

Eighteen checks of the running actual App passed against isolated temporary databases: startup, posting, details/history, joining, reviewing, charts, a changed-data chart update, real live API suggestions, category mapping, currency handling, active-import interactivity, About handler, cancellation rollback and sample import. The public API returned HTTP 200 with 20 products; the Suggest button displayed real matching products. These checks do not replace the instructor's live demo, including toggling the network and rerunning through Eclipse's Run button.

- Any demo member can buy/sell/review; there is no authentication or purchase/payment flow.
- API suggestions are rough catalog references with limited product coverage and unspecified currency. They are not verified fair PHP resale prices.
- Recent history is session-only; waitlists, listings, reviews and imported sales persist locally per computer.
- Long tasks share one worker. Conflicting actions are disabled while it is occupied; loaded table selection, tabs, form editing and About remain usable.
- Analytics groups direct categories. Price charts use the chosen currency; rating charts show mean campus reviews and counts. Categories without reviews are unrated.
- Review eligibility is based on demo identity, ownership and uniqueness, not proof of purchase. One review per member/listing is enforced in the app's serialized transaction path; the existing Reviews table has no new unique constraint/migration.
- A file selection dialog and the presentation machine's actual editor Run buttons should still be rehearsed manually.

## Git submission is manual

The shared Milestone 2 branch is **feature/milestone-2-program**; PR #8 is open against main. The 7 November corrections are published separately on **fix/instructor-feedback-week8-10**, based on that program branch. XionAce creates the correction PR manually; use feature/milestone-2-program as its base to review only these corrections. No PR is created or merged by the correction work. Keep milestone branches. Confirm any instructor-specific checkpoint naming before further submission actions. Actual member work and private peer evaluations remain separate requirements.
