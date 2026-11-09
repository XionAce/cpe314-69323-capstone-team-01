# Run and understand this program

## Sign in

The application opens on a login page. The three class accounts are:

| Account | Username | Starting password |
|---|---|---|
| Person A | `person_a` | `CampusA123!` |
| Person B | `person_b` | `CampusB123!` |
| Person C | `person_c` | `CampusC123!` |

Use the sidebar to open Marketplace, Sell an item, Sales history and Analytics.
While signed in, the visible Marketplace or Analytics page checks the shared SQLite database
every three seconds on the background worker. Multiple windows using the same database
pick up posted and withdrawn listings and updated chart averages automatically.
An unchanged result keeps the current table rows and selection. Search and My listings
filters are preserved. Polling waits during another action or an open dialog and stops at sign-out.
Marketplace has a **My listings** filter and an owner-checked **Withdraw my listing** action.
Sign out before signing in as another person. These predefined accounts support the classroom prototype.
Passwords are verified using salted PBKDF2 hashes stored in the local SQLite database.
On first startup, existing databases upgrade to version 3. Members now stores username,
passwordSalt, passwordHash, recoverySalt and recoveryHash. Account IDs and related records are preserved.

## Password recovery

Click **Forgot password?** on the sign-in screen. Enter the username, its recovery code,
and a new password of 8 to 128 characters. Enter the new password again to confirm it.

| Account | Starting recovery code |
|---|---|
| person_a | `RESET-A-2026` |
| person_b | `RESET-B-2026` |
| person_c | `RESET-C-2026` |

The starter codes are for these three classroom accounts. A successful reset consumes that code,
updates the password and displays a new random recovery code. Copy and save the new code before
returning to sign in. Further resets require the replacement code. Passwords and recovery codes
are stored as salted PBKDF2 hashes. Failed verification keeps the saved credentials unchanged.

Marketplace opens with illustrated cards. Use **List** to switch to the table.
Click an item card to load its details, reviews and waitlist.

The current working version is **Milestone 2**. See [the Milestone 2 guide](../docs/MILESTONE_2.md) for API suggestions, reviews, JSON import, charts, sample data, test evidence and remaining demo checks. The source map and Week 9 demo below describe the retained foundation; Milestone 2 adds `api/CatalogProduct.java`, `api/ProductCatalogClient.java` and `service/SalesHistoryImporter.java`.

This folder is the complete Java 21 + JavaFX 21 + SQLite/JDBC Maven project. Keep `pom.xml`, `src`, `mvnw.cmd`, `mvnw` and the hidden `.mvn` folder together. Maven supplies JavaFX and the SQLite driver; do not add JARs by hand.

## Required Java version

Use **JDK 21**, including for Eclipse's Maven launch. In this folder run `java -version` and `.\mvnw.cmd -version`; both must show Java 21. The enforcer range `[21,22)` deliberately rejects JDK 17 and 25. Select the installed JDK 21 in your editor and JAVA_HOME before reopening the terminal. Do not skip the enforcer to report a normal build. Once the version checks match, run `.\mvnw.cmd clean verify`.

## Restore missing condition details

If search reports that a listing has no condition details, sign in as that listing's owner. Click **Restore missing details**, choose the affected listing, then select the condition actually observed. No condition is selected automatically. Confirm to save and reload the listings. Cancel leaves the record unchanged.

Restoration keeps the listing ID, title, price, reviews and waitlist. The database rechecks ownership and type, and rejects existing/conflicting details. A failure rolls back the restoration. Conflicting subtype rows require database-maintainer review rather than automatic replacement. Never delete the live database as a repair shortcut.

## Antigravity: Run button

1. Choose **File > Open Workspace from File...** and open **`C:\Users\HP\Documents\Codex\campus-marketplace-work\CampusMarketplace.code-workspace`**. This workspace contains only `program`. The window title should identify **CampusMarketplace**, rather than **Untitled (Workspace)**. Alternatively, use File > Open Folder to open `program` by itself. Opening the whole Documents/Codex folder imports archived Maven copies and creates duplicate-project errors.
2. Install **Language Support for Java(TM) by Red Hat** (`redhat.java`) and **Debugger for Java** (`vscjava.vscode-java-debug`) if they are missing. They are already installed on the current presentation computer. Reload the editor if requested.
3. Wait for Java to finish importing the Maven project. Use JDK 21. Maven still resolves the project's libraries; no Maven launch command is needed.
4. Open **Run and Debug**, select **Run Campus Marketplace**, and click the green triangle or press **F5**.
5. On this computer, you can also open `App.java` and click **Run** above `main()`. The local `.vscode/settings.json` supplies the same JavaFX options to that inline Run button.

The portable Windows profile is `.vscode/launch.json`; it finds the pinned JavaFX 21.0.8 JARs in your normal Maven cache through USERPROFILE. It resolves the Java project from the main class rather than assuming an editor-specific project name. The local settings file contains this computer's cache path for inline Run and is ignored by Git. On another Windows computer, use the portable Run and Debug profile after the dependencies have downloaded. The profile does not run a Maven goal before launching; the Java editor builds the source.

## Eclipse: Java Application Run button

1. Under **Window > Preferences > Java > Installed JREs**, select **JDK 21**. Set **JavaSE-21** to use it under Execution Environments.
2. If this same folder is already imported, right-click the project and choose **Refresh**, then **Maven > Update Project**. If Eclipse instead points to another copy, remove that workspace entry with **Delete project contents on disk unchecked**, then use **File > Import > Maven > Existing Maven Projects**.
3. Root directory: **`C:\Users\HP\Documents\Codex\campus-marketplace-work\program`**. Select its `pom.xml`, then Finish. Keep the project name **campus-marketplace**, which the supplied launch profile uses.
4. Wait for Maven dependency resolution and Eclipse's automatic build. If needed, right-click the project > **Maven > Update Project**. This imports/builds the project; launching uses a regular Java Application.
5. Select **Run > Run Configurations > Java Application > Run-Campus-Marketplace**, then **Run**. The supplied `Run-Campus-Marketplace.launch` contains the JavaFX options and is also added to Run favorites.
6. After that, choose **Run-Campus-Marketplace** from the green Run button's dropdown.

If Eclipse has not discovered the profile, choose **File > Import > Run/Debug > Launch Configurations** and select this program folder, then import `Run-Campus-Marketplace.launch`. This is a Java Application configuration, not a Maven Build configuration.

The editor configurations are for **Windows**, using the pinned libraries already managed by `pom.xml`. They keep JavaFX on its required module path. No Java source, schema or UI behavior was changed for this setup. The direct Java launch can be verified separately from the actual editor buttons; finish the one-time editor import/extension steps before using them.

## Optional terminal build and tests (PowerShell)

Open **File > Open Folder** and choose `C:\Users\HP\Documents\Codex\campus-marketplace-work`. Open **Terminal > New Terminal**, then run:

```powershell
cd C:\Users\HP\Documents\Codex\campus-marketplace-work\program
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

`clean verify` compiles the code and runs JUnit. `javafx:run` opens the window; close it before entering another command. First-time dependencies need a connection. With everything already downloaded, `.\mvnw.cmd -o verify` runs offline.

The terminal build and both editors use the same `pom.xml`. No `module-info.java` is needed. The packaged JAR is a build artifact; use the saved editor profile or `javafx:run` to launch the app.

## Where each part is

```text
src/main/java/edu/uc/campusmarket/
  App.java                      Starts the JavaFX window
  model/
    Listing.java                Shared fields and abstract grading method
    Textbook.java                Textbook condition rules
    Electronics.java             Electronics condition rules
    Furniture.java               Furniture condition rules
    ConditionGrade.java          Four allowed condition grades
    WearLevel.java               Three allowed wear levels
    Member.java                  Demo member ID/name
    Category.java                Category ID/name/parent ID
  service/MarketplaceService.java Search, queue, stack and ownership rules
  persistence/Database.java       JDBC connections, SQL and initial data
  ui/MarketplaceView.java         Controls and button event handlers
src/main/resources/
  db/schema.sql                   The ten planned tables
  ui/marketplace.css              JavaFX colors, spacing and text styles
src/test/java/                    JUnit tests (temporary test databases)
```

## Week 9: the working GUI control

The window has one listing table and one inline posting form. To demonstrate the required event handler:

1. Launch with the saved editor Run profile and wait for the sample listings.
2. In the form, enter `Demo Java book`, price `150.00`, type TEXTBOOK, category Textbooks and an observation.
3. Click **Post listing**. The new row appears selected in the table and the status says it was saved.
4. Enter `Demo Java book` in the search box and click **Search**. The table shows the match.

In `MarketplaceView.java`, `post.setOnAction(event -> postListing())` wires the click to the handler. `postListing()` reads the form, creates a concrete listing, saves through the service/database and reloads the table with a SELECT. Search has its own explicit `setOnAction` handler.

**View details** also records the selected item on the member's recently-viewed stack and shows the last three. **Join waitlist** demonstrates the queue for another member's item. The form takes one condition observation at a time; the model still supports all of its condition evidence and grading rules.

## Your database

Default file: `.ucb-campus-marketplace-simple/marketplace.db` inside the current user's home folder. On the presentation computer this is `C:\Users\HP\.ucb-campus-marketplace-simple\marketplace.db`.

It is created when the app first starts. It is separate from the older prototype database. Close the app and use **DB Browser for SQLite > Open Database** to open this file. In Browse Data choose `Listings`, `Sellers` or `Categories`. Under Execute SQL:

```sql
SELECT Listings.id, Listings.title, Sellers.displayName AS seller,
       Categories.name AS category, Listings.priceMinor / 100.0 AS price,
       Listings.currency, Listings.status
FROM Listings
JOIN Sellers ON Sellers.id = Listings.sellerId
JOIN Categories ON Categories.id = Listings.categoryId;
```

This is a read-only demonstration query. The Java code uses `PreparedStatement` with `?` placeholders for changing values. For Maven, add `-Dcampusmarket.dataDir=C:\your\folder` to the command. For an Eclipse Java Application profile, add it under **Arguments > VM arguments**. Tests use temporary directories and do not touch your demo records.

The [study guide](../docs/STUDY_GUIDE.md) follows the code step by step. The [implementation note](../docs/DESIGN_V0.2_IMPLEMENTATION.md) maps the current classes and database to the Week 9 design requirements.

## Prototype limits

Each signed-in member may post items, review another member’s listing and join its waitlist. My listings shows that account’s items, including withdrawn listings. Withdraw my listing checks ownership before saving the status change. Queue entries persist. Recent-view history belongs to one member and lasts until the app closes. The app uses local predefined accounts and a local SQLite database.
