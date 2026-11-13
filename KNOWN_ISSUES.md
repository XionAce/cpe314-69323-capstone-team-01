# Known issues and correction status

Campus Marketplace with Reviews. Updated 13 November 2026.

## Implemented corrections

- The user created and pushed `project/milestone-2`, preserving the existing member commits. The required replacement PR title is **Milestone 2 - Team 01**; PR creation remains a manual step.
- Root build instructions, this file, the requirement map and the editable design source are available.
- `SalesAnalysis.weeklyCounts()` groups imported sale dates in a Java Map, normalizes timestamps to UTC, sorts Monday-starting weeks and counts each sale. All tied busiest weeks are reported. A dedicated bar chart displays the Java result.
- Small views call feature controllers. Controllers own observable state and background actions. Views contain no JDBC imports, web client calls or worker creation. `App` composes dependencies and owns one worker's shutdown.
- Feature SQL is in DAOs. `Database` handles connections/schema/initial data and `AccountSetup` handles account migration. Result records are standalone model/DTO types. The old database query facade now exists only in the test fixture, not in production.
- Catalog downloading, JSON parsing and supported-category matching are separated. Unsupported categories remain manual; catalog prices are not silently converted to PHP.
- Registration normalizes unique usernames, hashes credentials and returns a one-use recovery code. The active session supplies the member identity to controller actions.
- Reviews now require waitlist membership as well as an existing member, another owner, an available item, rating 1-5, a comment of at most 1,000 characters and no earlier review by the same member/item.
- New classroom accounts receive random initial credentials, shown once locally. Existing account passwords and related records survive upgrades. Starting secrets were removed from the program README and production initialization constants.
- Explicit imports, braces, smaller screen files and public API comments improve readability.
- The editable design and PDF now describe the implemented flows and all 69 top-level Java types. The approved context, Appendix and version history are retained. Diagrams use exact schema field names, show all ten tables/50 fields/twelve foreign keys and explain both BuyerInterests composite uniqueness rules.

- Additional feedback is implemented: Listings.created_at, ACTIVE/SOLD/REMOVED states with ownership checks, old-database migration, status/date UI, two-thread waitlist tests and non-crashing incomplete-detail display. The schema now has 50 fields. Messages/offers are explicitly out of scope.
- Waitlist allocation and eligibility checks run in one atomic INSERT ... SELECT. Two simultaneous distinct buyers get distinct positions; simultaneous repeat joins save one row.
- The marketplace now has adjacent browse/details panels, archive status filtering and clearly labeled recovery-code fields. Fast filter changes retry after an older request finishes.

## Open corrections and verification gaps

| ID | Issue | Next action |
|---|---|---|
| KI-03 | A fresh live API response could not be captured on this session's connection (its local proxy refused access). Local HTTP-server tests pass, but this is not a new public API/network demonstration. | Capture an actual response when connected normally, retain a sanitized test fixture, and repeat the required live network-toggle check. Do not substitute a fabricated response as live evidence. |
| KI-04 | Refresh rereads available listing data or analysis inputs every three seconds on the active page. Its cost with very large databases has not been benchmarked. | Measure before claiming large-data performance. Consider incremental queries or a longer interval if measurements justify it. |
| KI-05 | A dedicated personal dashboard, listing editing and waitlist leave/position screens from the broader guidance are not implemented. | Keep this scope explicit when demonstrating. The current personal view is My listings with owner-checked withdrawal; registration and waitlist-before-review are implemented. |
| KI-06 | Local remote-tracking snapshots of `project/proposal` and `feature/milestone-1-prototype` still contain the old four-entity `docs/erd.png`. It is absent from the current submission branch. The instructor requested updating or removing those older images. | Recheck the branch contents on GitHub, then update or remove the stale image through a reviewed manual commit on each affected branch. Keep the branches and their existing history. No historical-branch cleanup has been performed here. |
| KI-07 | The nine previously reported long methods/constructors have been split into focused helpers. The subsequent Java syntax-tree audit found zero main-source methods over 50 lines, and compilation passed. Final formatting, unexplained numeric limits and public API documentation still need review. | Complete that standards pass in a follow-up. This commit checkpoint records implemented work and does not claim every instructor correction is finished. |

KI-01 (design synchronization) and KI-02 (ERD presentation) are resolved in this local correction. The PDF has 24 physical pages, with Introduction numbered 1, and was visually checked after export. These file updates still need the user's commit and push.

## Prototype limits

- Accounts and SQLite records are local to a database file. There is no shared cloud account/database service, email recovery or online payment.
- Each computer normally has its own database. Automatic refresh synchronizes windows using the same file; it does not synchronize separate laptops.
- Save newly issued credentials/recovery codes when displayed. Plaintext values are not retained for later retrieval. Older accounts keep their existing credentials rather than being silently reset.
- The public catalog has limited coverage and a configured 15-second timeout. Current comparable-product matching supports electronics. Reference prices have unspecified currency; final campus prices are entered manually.
- Recent-view stacks last for the service session and are not restored after app closure.
- Weekly bars show observed weeks only; zero-sale weeks are not plotted. The counts include sales across currencies and do not sum their monetary amounts. The separate current-listing price chart filters by currency.
- Historical imports are shared marketplace analysis records, not proof of a payment or completed in-app purchase.

## Recorded verification

On 13 November 2026, from the repository root:

```powershell
.\program\mvnw.cmd -o -f .\program\pom.xml verify
```

Used JDK 21 with the Java/Maven enforcer enabled: **119 JUnit tests passed, zero failures/errors/skips**, in eleven test classes. The test-only convenience fixture is not a test class. Coverage includes the retained model/DAO/import rules, registration, review eligibility, credential preservation, Java analysis, controller state, stale-result rejection and refresh policy. Databases are temporary; HTTP tests use local servers.

Separate actual-application checks passed **40 cases**, covering startup, registration, login, review rejection/acceptance, posting, My listings, owner withdrawal, JSON import, weekly chart data/labels, automatic refresh, password recovery, sold/remove confirmations, status filtering, missing electronics/furniture details, owner restoration, window resizing and large-import cancellation/rollback. These are counted separately from JUnit. No live personal database was used.

No assistant commit, push, PR creation or merge has been performed for these code changes. See [REQUIREMENT_MAP.md](REQUIREMENT_MAP.md) for the source and test locations.

The earlier 13 November required-file recheck was a source/document inspection. After applying the additional feedback, a fresh JDK 21 Maven verification passed 119 tests, and 40 separate application checks passed against temporary data. No production source edits followed those final passing checks. GitHub confirmed that `project/milestone-2` exists; a search for PRs with that head branch returned none. The earlier test results remain the recorded evidence until new code changes justify repeating checks.

UI follow-up verification: the 119-test JDK 21 run passed at 06:00 Manila on 13 November. The final popup-position/chart-spacing presentation changes were then compiled and packaged successfully and exercised by all 40 actual-application checks. These checks include maximized startup, top navigation, review cards/right-side placement, colored bars and active-import loading indicators.

Current progress checkpoint: after extracting the long methods into helpers, JDK 21 Maven verification passed all 119 tests again at 08:53 Manila on 13 November, with zero failures/errors/skips. All 40 temporary-database application checks also passed again. The remaining items above are still unfinished; this verification supports committing current progress, not claiming final completion of the review.
