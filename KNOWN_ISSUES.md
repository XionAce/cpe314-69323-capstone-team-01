# Known issues and correction status

Campus Marketplace with Reviews. Reviewed against the local source on 12 November 2026.

This file distinguishes working features from open corrections. A planned change is not a completed feature.

## Open corrections

| ID | Issue and effect | Next action |
|---|---|---|
| KI-01 | Analytics uses SQL averages and counts. Imported historical sales are saved and previewed but do not feed an analysis chart. | Add a small Java analysis of historical sales, with a documented grouping rule and a chart. The proposed feature is weekly sale counts and the busiest week; it is not implemented yet. |
| KI-02 | `MarketplaceView` builds the screens, starts background work and handles many actions. It imports database result records and handles an SQL exception. `Database` contains the SQL for all ten tables. | Separate screens, controllers, services and DAOs in small steps. Move result records out of `Database`, preserve the shared worker and account ownership checks, and verify behavior after each step. |
| KI-03 | `ProductCatalogClient` downloads, parses and matches catalog products in one class. | Separate parsing and mapping/matching responsibilities. Keep unsupported categories out of the three listing types. |
| KI-04 | Fixed classroom passwords and recovery codes are present in the initialization code and published documents. They are unsuitable for real accounts. | Revise first-start credential delivery and remove published starting credentials together. Preserve existing users, passwords and data during upgrades. |
| KI-05 | The current PDF's final ERD matches the schema, but key captions do not always show the exact column names, and there is no single ten-table overview. | Use exact schema names in the diagram boxes, retain friendly explanations in a legend, add the overview and state `UNIQUE(listingId, buyerId)` and `UNIQUE(listingId, joiningOrder)` for BuyerInterests. |
| KI-06 | Login and reset have database tests, but no direct service-level login/reset tests. The timer has previous manual app checks, not automated controller coverage. | Add focused service and controller tests after the refactor. Wrong-password, malformed-import and empty-summary cases already exist; avoid adding duplicate tests. |
| KI-07 | Wildcard imports, missing braces, long methods and incomplete public Javadoc make parts difficult to explain and maintain. | Organize imports, add braces, name meaningful constants and split long methods while preserving behavior. |
| KI-08 | The PDF and restored Markdown describe the existing implementation, not the planned MVC and Java-analysis corrections. Their existing verification results are historical. | Update both after the code corrections are implemented and verified. Keep the approved Sections 1-4 and update affected diagrams and Sections 5-7. |

## Prototype limitations

- The app uses three predefined local accounts. Registration, email recovery, online payment and a shared cloud database are not implemented.
- FakeStoreAPI can be unavailable or take up to the configured 15-second timeout. The screen offers retry/manual entry. Its catalog is limited; current matching supplies electronics suggestions only.
- Catalog prices have unspecified currency. They are reference values, not PHP conversions.
- Current price charts use available listings in the selected currency. Historical sales are deliberately excluded from that chart. Review charts average individual saved reviews.
- Each computer keeps a separate SQLite database. Three-second refresh works between windows reading the same database, not between unrelated laptops.
- Recently viewed items use an in-memory stack per member for the service session. That history is not restored after the app closes.
- Refresh currently rereads listings or summaries every three seconds on the active page. Its cost with many rows still needs measurement. It skips busy work/dialogs and stops on sign-out or close in the current code.
- A review currently requires an existing member, a different listing owner, an available listing, rating 1-5, a comment of at most 1,000 characters and no earlier review by that member for that listing. Joining its waitlist is not currently required. The proposed interest-before-review rule remains a scope decision.

## Submission status

- `project/milestone-2` was created from the published Milestone 2 commit, retaining earlier member commits.
- The required replacement PR title is **Milestone 2 - Team 01**. Creating that PR is a manual submission step; this file does not claim it has been opened.
- The program intentionally remains in `program/`. The root [README](README.md) gives build/run commands from the repository root.
- The editable design master and its preliminary sketch files are restored under `docs/`. The existing proposal and design PDFs are retained.
- The [requirement map](REQUIREMENT_MAP.md) lists the current classes, methods, tests and remaining checks.

## Verification record

The prior project verification on 9 November recorded 81 passing JUnit tests in seven classes. The instructor's supplied pre-check also reports 81 passing tests, with its JDK enforcer skipped in that environment. A normal submission verification must use JDK 21 and keep the enforcer enabled.

No Java code changed and no build, JUnit run or GUI check was performed for this documentation-preparation step. After implementation changes, record the new command, date and actual results here. Preserve the live database and use temporary test databases.
