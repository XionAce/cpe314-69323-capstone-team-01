# Run and understand Campus Marketplace

This is the Java 21 + JavaFX + SQLite program for `project/milestone-2`.
The `App` entry point stays the same. Keep this folder, its `pom.xml`, `src`, Maven Wrapper and hidden `.mvn` folder together.

## Start the application

Use JDK 21. From the repository root in PowerShell:

```powershell
java -version
.\program\mvnw.cmd -version
.\program\mvnw.cmd -f .\program\pom.xml clean verify
.\program\mvnw.cmd -f .\program\pom.xml javafx:run
```

Both version commands must report Java 21. Do not bypass the enforcer. `clean verify` compiles and runs JUnit; `javafx:run` opens the window. First-time dependencies need a connection. After downloading them, `-o` can be used for an offline build.

## First startup and accounts

A new database creates Person A, Person B and Person C as classroom accounts. Their usernames are `person_a`, `person_b` and `person_c`. Each receives a newly generated password and recovery code. A local startup dialog shows newly issued credentials once: save them privately before closing it. No starter password or recovery code is published here. Only salted PBKDF2 hashes are stored in SQLite.

Existing accounts keep their passwords, IDs, listings, reviews and waitlists. Opening an existing current database does not generate replacement credentials. An older database that needs a missing login/recovery field is upgraded without deleting its records.

You can also click **Create an account**. Enter a display name, a unique username of 3-30 letters/digits/underscores, and an 8-128 character password. Usernames are normalized to lowercase. Save the recovery code shown after registration, then sign in. Accounts belong to the local database, not a cloud server.

**Forgot password?** verifies the saved recovery code. A successful reset changes the password and replaces the code. Save the replacement; the old code cannot be reused. There is no email-recovery service.

## A short demonstration

1. Sign in as one account. In **Sell an item**, post a textbook with a title, a positive PHP price, a category and an observed condition. **My listings** shows that account's items.
2. Sign out and use another account. Select the item and try to review it before joining its waitlist: the app rejects the review. Click **Join waitlist**, then review it with a rating from 1 to 5. Self-reviews and duplicate reviews are still rejected.
3. In **My listings**, select an active item and use **Mark as sold** or **Remove listing**. Confirm to set SOLD or REMOVED. Both leave public search and remain in the owner's archive with reviews/waitlists. Use **All statuses / Active / Sold / Removed** to filter the archive. Another member cannot change its status.
4. In **Sales history**, import `samples/sales-history.json`. It contains six demonstration sales over three weeks. A repeated import skips matching source IDs. Cancel requests rollback of the unfinished import.
5. In **Analytics**, view the weekly sales counts and busiest-week summary, then the current-listing price and review charts. With only the six sample sales imported, the weekly counts are 3, 2 and 1. Existing history may change these totals.
6. For catalog suggestions, enter an electronics title such as `laptop` and click **Suggest category / price**. This makes a new HTTP request. Catalog prices have unspecified currency; enter the final PHP price manually. Unsupported categories retain manual entry.

Weekly analysis is written in Java: convert each sold timestamp to UTC, find its Monday, increment a `Map` count, sort the weeks and report all tied busiest weeks. Weeks with no sales are not plotted. This chart counts sales across currencies; it does not add their monetary values. The separate current-price chart filters by currency.

Marketplace and Analytics check the same local database every three seconds while signed in. Polling skips busy work and dialogs, preserves filters/selection and avoids redrawing unchanged results. It stops at sign-out or application close. This is synchronization between windows reading the same file, not between separate laptops.

Listings record `created_at` as UTC on insertion. Cards and the list show the date; details show a readable UTC time. Existing databases upgrade to schema version 4 without deleting accounts or records. Old AVAILABLE/WITHDRAWN statuses become ACTIVE/REMOVED. Older records without creation dates show **Date not recorded**. Marking sold does not automatically import a historical sale or process payment.

Messages and offers are out of scope. There is no chat screen or message/offer table.

## Run buttons in Antigravity and Eclipse

In Antigravity, open this `program` folder, wait for Java's Maven import, then select **Run and Debug > Run Campus Marketplace**. You can also use the existing workspace file at the repository root. Open only this active project, rather than the whole Documents/Codex folder containing other copies.

In Eclipse, use **File > Import > Maven > Existing Maven Projects**, select this folder's `pom.xml`, and use JDK 21. For an already imported copy, use **Refresh** and **Maven > Update Project** after the refactor. Use the existing **Run-Campus-Marketplace** Java Application launch profile, or the Maven command above.

Maven manages JavaFX, Gson and the SQLite driver. Do not copy JARs by hand.

## Where the work happens

| Package/file | Responsibility |
|---|---|
| `App.java` | Creates DAOs, services, controllers and views; owns the shared worker and shutdown. |
| `model/` | Plain domain objects, condition rules and immutable result records. No GUI or SQL. |
| `dto/` | The catalog JSON data transferred from the parser. |
| `api/` | The HTTP client and separate catalog parser. |
| `dao/` | Prepared SQL statements, result reading and transactional writes. |
| `service/` | Account/listing/review rules, catalog matching, JSON import and Java sales analysis. |
| `controller/` | Account/session state, observable lists, background actions, cancellation and refresh policy. |
| `ui/` | Small JavaFX screens, bindings, illustrations, dialogs and chart rendering. No SQL, HTTP client or worker creation. |
| `persistence/` | Database connection, schema creation, initial data and account migration. |
| `src/main/resources/db/schema.sql` | The ten table definitions and storage constraints. |
| `src/main/resources/ui/marketplace.css` | Colors, spacing and styles used by the JavaFX controls. |
| `src/test/java/` | Model, DAO, service and controller tests with temporary data and local HTTP servers. |

The usual action path is **view -> controller -> service -> DAO -> SQLite**. Results return through controller properties to the bound screen. `MarketplaceService` retains the recursive category traversal, FIFO waitlist and LIFO recent history; the feature services call those existing rules.

## Database and missing condition details

The default file is `.ucb-campus-marketplace-simple/marketplace.db` under the current user's home folder. The footer shows its exact path. It is created at startup and is ignored by Git. Close the application before manually inspecting it with a SQLite viewer.

To use a separate demonstration database from the repository root:

```powershell
.\program\mvnw.cmd -f .\program\pom.xml -Dcampusmarket.dataDir=C:\your\demo-folder javafx:run
```

If a listing lacks its subtype evidence, it remains visible with **Details unavailable** and no invented grade; healthy listings still load. Sign in as its owner and click **Restore missing details**. Select the listing and the condition actually observed. Cancellation changes nothing. Restoration preserves its ID, common fields, reviews and waitlist; conflicting detail rows need maintainer review. Do not delete the live database as a repair shortcut.

## Submission records

See the root [known issues](../KNOWN_ISSUES.md) and [requirement map](../REQUIREMENT_MAP.md) for actual status and verification. The [editable design](../docs/design-document.md) and [design PDF](../docs/design-document.pdf) describe the refactor, registration/review rules, weekly analysis and all current Java types. The [optional PDF exporter](../tools/export_design.py) requires Python, ReportLab and Calibri; it is separate from running the application. Recorded verification is 119 passing JUnit tests and 40 separate application checks. A fresh public-network/API demonstration remains outstanding.

The application opens maximized. Navigation is across the top; saved reviews open as star/comment cards in a right-side popup. The selling screen includes a type illustration, imports show an indeterminate spinner/bar during actual work, and analytics use spaced colored bars with value tooltips.
