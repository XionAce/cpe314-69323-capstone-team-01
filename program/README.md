# Run and understand this program

This folder is the complete Java 21 + JavaFX 21 + SQLite/JDBC Maven project. Keep `pom.xml`, `src`, `mvnw.cmd`, `mvnw` and the hidden `.mvn` folder together. Maven supplies JavaFX and the SQLite driver; do not add JARs by hand.

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
4. Enter `Demo Java book` in the search box and click **Search / refresh**. The table shows the match.

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

The member selector is demo data, not login/authentication. Any selected member may post or join another member's waitlist. Queue entries persist; recent-view history belongs to one member and lasts only until the app closes. Owner withdrawal and My listings remain tested backend methods without buttons on this smaller screen. There are no online payments or shared multi-computer server.
