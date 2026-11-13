# Known issues and correction status

Campus Marketplace with Reviews. Updated 13 November 2026.

## Implemented corrections

- Submission branch: `project/milestone-2`. Existing PR #14 is titled **Milestone 2 - Team 01**. Pushing this branch updates that PR; keep the earlier branches and member history.
- Root build instructions use JDK 21 and the existing `program/` Maven module. This file, the requirement map and editable design are available.
- Small views bind controllers; services apply rules; DAOs execute SQL. `App` creates dependencies and owns one named worker. Models/results/DTOs are separate from persistence and UI.
- API download, parsing and comparable-product matching are separated. Reference currency is unspecified; final campus prices remain manual PHP inputs.
- Registration, salted credential hashing, one-use recovery codes and random first-start credentials are implemented. Existing credentials/data survive migration. Reviews require waitlist membership, another owner, active status, rating/comment limits and no duplicate.
- Java weekly analysis groups UTC sale timestamps into Monday-starting weeks using a TreeMap, counts them and reports all tied busiest weeks. Labeled colored bars display the result separately from category means.
- Version-4 migration adds creation dates and ACTIVE/SOLD/REMOVED states. Owner-confirmed status changes retain related rows. Missing/conflicting subtype rows display a safe warning.
- Waitlist joining allocates order and checks eligibility in one atomic INSERT ... SELECT. Two-thread tests cover distinct buyers and repeated joins.
- Personal home opens after login: active listings, waiting buyer names, reviews received/average and the member's own queue positions. Member-filtered queries share a read transaction. Old-session results are rejected and logout clears the snapshot.
- Owner-only title/price editing retains ID, type, condition, currency, date, reviews and interests. SQL rechecks active status and ownership at save time.
- Leaving deletes only the signed-in buyer's interest, including on archived items, and retains saved reviews. Displayed positions count remaining earlier entries; rejoining goes to the tail.
- The design/PDF document all 75 top-level Java types and ten tables/50 fields/twelve foreign keys. Overview/detail ERDs use exact field names and both BuyerInterests unique pairs.
- The standards pass fixed helper indentation/wrapping, named business-rule limits and missing security/interface API comments. Syntax inspection found zero unbraced controls, wildcard imports, tabs, lines over 120 characters, undocumented public types/non-overriding methods or main methods over 50 lines. This is a focused inspection, not an external style certification.

KI-01 (design synchronization), KI-02 (ERD presentation), KI-05 (personal home/edit/queue controls) and KI-07 (final standards pass) are resolved in the current local source.

## Open corrections and verification gaps

| ID | Issue | Next action |
|---|---|---|
| KI-03 | The normal public API request still fails through this session's configured proxy. Local-server tests do not prove public connectivity. No fresh real-response fixture is present. | Capture an actual response on a working connection, save it as a test resource and add its parser contract test. Repeat the instructor's live network on/off demonstration. Never label constructed JSON as a captured response. |
| KI-04 | Data loading has been measured, but thousands of card controls, cold startup and simultaneous users have not. | Measure full UI rendering before claiming very large-data capacity. |
| KI-06 | Earlier branch snapshots of `project/proposal` and `feature/milestone-1-prototype` contain stale `docs/erd.png`. It is absent from this submission branch. | Recheck GitHub and remove/update only that image with manual reviewed commits on the affected branches. Keep the branches and history. No cleanup of those branches is claimed here. |

## Prototype limits

- Accounts/records are local to one SQLite file. Refresh synchronizes windows reading that file, not separate laptops. There is no cloud service, email recovery or payment processing.
- Save passwords and recovery codes when displayed; plaintext is not retained. Existing accounts are not silently reset.
- Catalog coverage is limited, matching currently supports electronics and requests have a 15-second timeout. Reference prices have unspecified currency.
- Recent-view stacks last for the service session. Historical imports are analysis records, not proof of an in-app payment. Marking sold does not create an imported sale.
- Weekly bars show observed weeks only; missing weeks are not plotted. Counts combine currencies without summing money; the price chart filters by currency.
- Messages and offers remain explicitly out of scope. Editing currently changes title and price only; other listing fields stay unchanged.

## Recorded verification

JDK 21, enforcer enabled, from the repository root:

```powershell
.\program\mvnw.cmd -o -f .\program\pom.xml verify
```

**128 JUnit tests in twelve classes passed, zero failures/errors/skips.** Tests use temporary databases and local HTTP servers. Nine new cases cover personal scope, received ratings, owner edit validation/preservation, queue changes and stale dashboard results. Earlier model, migration, concurrency, import, API and analysis checks remain.

**46 separate application checks passed** against temporary data, including startup/registration/login, personal home, editing, leaving/current position, reviews, post/sold/remove, archive filters, imports/charts, cancellation rollback, automatic refresh, recovery, missing-detail restoration and resizing. GUI checks are separate from JUnit and from public-network evidence.

Five warm JDK 21 runs with **10,003 listings, 10,000 interests and 50,000 historical sales** measured search at 80.2-348.6 ms, analysis at 169.9-286.4 ms and personal-home reads at 49.5-63.0 ms. The temporary-data measurement excludes control rendering and cold startup; it is not a capacity guarantee.

The earlier published checkpoint had 119 JUnit tests and 40 application checks. The latest follow-up remains local until the user commits/pushes; the assistant has not staged, committed, pushed, changed branches or modified a PR. Live API checks, older-branch image cleanup, the recursion trace and each member's explanation remain submission tasks. See [REQUIREMENT_MAP.md](REQUIREMENT_MAP.md).
