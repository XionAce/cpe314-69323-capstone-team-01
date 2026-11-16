# Known issues and correction status

Campus Marketplace with Reviews. Updated 15 November 2026.

## Implemented corrections

- Integration branch: `project/milestone-2`. PR #14, **Milestone 2 - Team 01**, was merged into main on 14 November. The current correction group is local and awaits reviewed publication. Keep earlier branches and member history.
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
- The design/PDF document all 85 top-level Java types and eleven tables/56 fields/fifteen foreign keys. Overview/detail ERDs use exact field names and both BuyerInterests unique pairs.
- The standards pass fixed helper indentation/wrapping, named business-rule limits and missing security/interface API comments. Syntax inspection found zero unbraced controls, wildcard imports, tabs, lines over 120 characters, undocumented public types/non-overriding methods or main methods over 50 lines. This is a focused inspection, not an external style certification.

KI-01 (design synchronization), KI-02 (ERD presentation), KI-05 (personal home/edit/queue controls) and KI-07 (final standards pass) are resolved in the current local source.

## Open corrections and verification gaps

| ID | Issue | Next action |
|---|---|---|
| KI-03 | The team laptop captured HTTP 200 and 20 public products; response bytes/hash/timestamp and a parser/mapper test are present. The production Java client and real selling screen also returned HTTP 200 and displayed reference prices correctly; the offline/reconnection demonstration is still unverified. | Repeat online/offline behavior in the real app on the presentation machine; the saved fixture and local-server tests are separate evidence. |
| KI-04 | Data loading has been measured, but thousands of card controls, cold startup and simultaneous users have not. | Measure full UI rendering before claiming very large-data capacity. |
| KI-06 | Earlier branch snapshots of `project/proposal` and `feature/milestone-1-prototype` contain stale `docs/erd.png`. It is absent from this submission branch. | Recheck GitHub and remove/update only that image with manual reviewed commits on the affected branches. Keep the branches and history. No cleanup of those branches is claimed here. |

## Prototype limits

- Accounts/records are local to one SQLite file. Refresh synchronizes windows reading that file, not separate laptops. There is no cloud service, email recovery or payment processing.
- Save passwords and recovery codes when displayed; plaintext is not retained. Existing accounts are not silently reset.
- Catalog coverage is limited, matching currently supports electronics and requests have a 15-second timeout. Reference prices have unspecified currency.
- Recent-view stacks last for the service session. Historical imports are analysis records, not proof of an in-app payment. Marking sold does not create an imported sale.
- Weekly bars show observed weeks only; missing weeks are not plotted. Counts combine currencies without summing money; the price chart filters by currency.
- Private listing messages are implemented; offers and payment processing remain out of scope. Editing currently changes title and price only; other listing fields stay unchanged.

## Recorded verification

JDK 21, enforcer enabled, from the repository root:

```powershell
.\program\mvnw.cmd -o -f .\program\pom.xml verify
```

**159 JUnit tests in fourteen classes passed, zero failures/errors/skips.** Tests use temporary databases and local HTTP servers. The earlier nine personal-feature cases cover personal scope, received ratings, owner edit validation/preservation, queue changes and stale dashboard results. Earlier model, migration, concurrency, import, API and analysis checks remain.

**50 separate application checks passed** against temporary data, including startup/registration/login, personal home, editing, leaving/current position, reviews, post/sold/remove, archive filters, imports/charts, cancellation rollback, automatic refresh, recovery, missing-detail restoration and resizing. GUI checks are separate from JUnit and from public-network evidence.

Five warm JDK 21 runs with **10,003 listings, 10,000 interests and 50,000 historical sales** measured search at 80.2-348.6 ms, analysis at 169.9-286.4 ms and personal-home reads at 49.5-63.0 ms. The temporary-data measurement excludes control rendering and cold startup; it is not a capacity guarantee.

The published e80c4cf checkpoint had 128 JUnit tests and 46 application checks. The 15 November correction group remains local until the user commits/pushes; the assistant has not staged, committed, pushed, changed branches or modified a PR. The live GUI network check, older-branch image cleanup, the recursion trace and each member's explanation remain submission tasks. See [REQUIREMENT_MAP.md](REQUIREMENT_MAP.md).

## Final-defense readiness follow-up

- ListingFactory rejects unknown types and missing/mismatched condition observations instead of silently creating Furniture/default grades. Two regression tests cover it.
- ProductCatalogClientTest covers the actual captured 20-product response and electronics matching. Capture metadata retains the original script-reported count of one and explains its correction to the decoded array length; original response bytes/timestamp/hash are unchanged. The capture script now counts the array correctly.
- SETUP.md uses the current Java 21/JSON/module commands. TEAM_INFO.md matches the existing README roster without inventing Joshua's missing student number. The requirement map now lists all nine original technical requirements separately from scoring items.
- The synthetic sample plus follow-up was imported through the production importer/DAO/analysis: counts changed 3,2,1 to 3,2,4; a repeat saved zero and skipped three duplicates.
- A clean Git archive of published e80c4cf passed its 128 tests. This was not a clone of main. An earlier readiness checkpoint passed 131 tests and 50 application checks on 13 November. The private-chat follow-up on 14 November passed 142 tests in thirteen classes. The current 15 November correction group passed 159 tests in fourteen classes.
- PR #14 was merged into main on 14 November. After publishing the latest corrections, verify a fresh clone of main on the presentation machine. No new PR, branch switch or history rewrite was performed for this local correction group. The presentation-machine network-toggle demonstration and each member's live explanation remain course requirements.

Six additional live public-API screen checks passed using temporary data: HTTP 200, comparable references, currency warning, retained entered PHP price, busy-button state and available navigation. These are additional to the 50 broader application checks, and are not an offline network-toggle check. The editable Word document is synchronized with the design master; its contents field and matching PDF are checked after rendering.


## Private listing chat

Open an item and choose **Open chat**. The buyer writes first; the seller replies in **Messages**. Only that buyer and the item's seller can read or send through the app. A different buyer gets a separate conversation. Messages are plain text, 1-2000 characters, saved with UTC timestamps and ordered by database ID. They do not change waitlists, reserve items or process payments.

To demonstrate automatic updates, run the current program twice against the same data folder and sign in with two different accounts. Keep both Messages pages open. The existing three-second refresh displays saved replies without a manual refresh and preserves unsent text. Separate laptops do not share local messages. Existing sold-item chats stay usable for pickup; removed-item chats retain read-only history.

Schema version 5 adds Messages and indexes while preserving earlier accounts and records. The current design includes eleven tables, 56 columns, fifteen foreign keys and 85 main Java types. Eight chat service tests and three chat-controller tests were added. A real two-process GUI test passed seventeen assertions, separately from JUnit.


## Instructor follow-up, 15 November 2026

Implemented and verified locally:

- Invalid posting/edit prices have a clear message; valid surrounding spaces are accepted.
  Null review comments have a distinct message, and SQL details are logged rather than
  included in the user-facing database error.
- Quiet reads do not set foreground busy state. One user action can queue behind a quiet
  read on the same serial worker. Repeated quiet ticks cannot accumulate. A rejected
  catalog request shows a retry message, and CatalogSuggestion distinguishes OK/EMPTY/FAILED.
- DatabaseChanges keeps one observer connection and checks SQLite data_version before
  full listing/analytics reloads. Only successful reads update the saved page version.
- ListingService, MemberService, ReviewService and WaitlistService own their operations;
  CategoryTree owns recursion. The production catch-all MarketplaceService was removed.
  MarketplaceFixture exists only under test sources for legacy integration assertions.
- Invalid saved listing/message dates retain the record with an unknown date. History
  previews display friendly UTC dates. SavedDates never substitutes a fabricated time.
- Cards support keyboard focus and Enter/Space; labels are linked to controls. Invalid
  edits keep the dialog open. Forms show inline errors, text contrast/sizes are improved,
  the usable monitor bounds contain the initial window, and compact layouts retain
  readable controls. The normal footer hides the full database path.
- Clean verification: 159 tests / fourteen classes, zero failures/errors/skips. Separate
  current GUI evidence: 60 broad assertions and 17 two-process chat assertions. All use
  temporary data. No personal database was used for this verification.

Still separate from these local fixes: KI-06 old-branch image cleanup; very large card
rendering/cold-start measurements (KI-04); publication and fresh-clone verification of
main; final live network demonstration, rehearsal, individual Q&A and private peer forms.
The catalog description/image fields were explicitly optional. No new feature is claimed
for them. Later corrections do not change the professor's earlier grading cutoff.
