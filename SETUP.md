# Set up and verify Campus Marketplace

Use the current checkout containing `program/pom.xml`. The approved project uses Java 21, JavaFX, SQLite/JDBC, JUnit 5 and JSON sales history. Maven manages its libraries; no CSV library or standalone SQLite server is required.

## Open the correct project

In Antigravity, open this checkout's `CampusMarketplace.code-workspace` or `program` folder. In Eclipse, import `program/pom.xml` through Existing Maven Projects. Select JDK 21 for the project and Maven launch. Avoid opening a different older copy at the same time.

## Check Java and build

From the repository root in PowerShell:

```powershell
java -version
.\program\mvnw.cmd -version
.\program\mvnw.cmd -f .\program\pom.xml clean verify
.\program\mvnw.cmd -f .\program\pom.xml javafx:run
```

Both version checks must report Java 21. Keep the enforcer enabled. The first build needs internet for Maven/dependencies; after they are cached, add `-o` for offline builds. A fresh checkout must retain the hidden `program/.mvn` directory and both wrapper scripts. A build does not prove public API access.

## Use separate presentation data

The default database is outside Git, under `.ucb-campus-marketplace-simple` in the user's home folder. Startup creates its schema and initial data automatically. Existing databases retain their credentials and records. Do not delete an existing database to refresh the UI.

For an isolated rehearsal, choose a new data folder and launch:

```powershell
.\program\mvnw.cmd -f .\program\pom.xml '-Dcampusmarket.dataDir=C:\CampusMarketplaceDemo' javafx:run
```

Save the random first-start credentials and recovery codes privately when shown, or register a new account. Close and restart the app to load changed code. A different database does not change the UI version.

## Rehearse the requirements

1. Sign in, post an item, search its category and view its condition. Try an empty title or invalid price and show the error without a crash.
2. Use another account to join the waitlist and review. Show a rejected self-review or duplicate, My home and the buyer's queue position.
3. Import `program/samples/sales-history.json` through the file chooser. With only this sample imported, weekly counts are 3, 2 and 1. A repeat skips duplicates. Prepare a larger JSON file to demonstrate responsiveness and cancellation.
4. Show Analytics, then import `program/samples/sales-history-followup.json`. Starting with only the first sample, counts change from 3, 2, 1 to 3, 2, 4; the last observed week becomes busiest. These are synthetic demonstration records. Marking a listing sold does not create historical sales.
5. Request an electronics catalog reference online. Repeat with the network unavailable: show the error and continue manual posting. Restore the connection afterward. Backup screenshots support, but do not replace, a live demo.
6. Keep the IDE ready on `Listing.gradeCondition()`, its three subclasses, `MarketplaceService.collectCategoryIds()`, a DAO, `FxTaskRunner`, `SalesAnalysis` and a meaningful test. Keep the exact demo database path ready for schema inspection after closing the app.

For the instructor's real-response fixture follow-up, run `./tools/capture-catalog.ps1` on a working connection. It saves a real HTTP response and timestamp/hash metadata under `program/src/test/resources/api`, without replacing an existing capture. The Java parser contract test must then be added against that actual file. The helper does not claim the response was captured or the live network demo completed.

## Final submission

The final-defense checklist grades **main**, after Milestone 2 is merged. It does not require another branch. Keep the existing branches/history. The user handles commits, pushes and merging; an open PR alone does not put code on main. After merging, verify a fresh clone of main on the presentation machine with the commands above. Every member reviews their own commits and prepares to explain actual changed lines.

The design, requirement map and known issues must describe the same program. The revised Word document and matching PDF were rendered and checked on 14 November: 30 pages, 50 contents entries, 82 Java types and eleven database tables. Verified copies replace the existing files in SirNos/WEEK11 file. See [README.md](README.md), [REQUIREMENT_MAP.md](REQUIREMENT_MAP.md) and [KNOWN_ISSUES.md](KNOWN_ISSUES.md).


## Demonstrate buyer-seller chat

Open two PowerShell terminals in this repository. In each, run `.\program\mvnw.cmd -f .\program\pom.xml javafx:run`. Both use the same default data folder. Sign in with two different existing accounts. As buyer, open the other account's active item and choose **Open chat**. Send a message. As seller, open **Messages**, select that buyer and reply. Keep both Messages pages open; saved replies appear on the three-second refresh. Chat does not require internet, reserve the item or change its waitlist. Separate laptops use separate local databases and do not share chats.
