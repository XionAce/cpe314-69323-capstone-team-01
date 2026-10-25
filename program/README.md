# UC-B Campus Marketplace — Milestone 1 program

This folder is one portable **Java 21 + JavaFX + Maven + SQLite/JDBC** project for CPE 314, Option J, Team 01. Instructor approval was confirmed by the user on 25 October 2026. The Maven build and all 31 tests pass, including SQLite/JDBC integration tests. The buyer-first interface also passed 20 JavaFX flow checks; Eclipse import and the presentation-machine rehearsal remain to be verified. See [actual verification status](../docs/milestone-1-status.md).

## Run from this folder

Install JDK 21 first. PowerShell:

```powershell
Set-Location 'C:\Users\HP\Documents\Codex\campus-marketplace-work\program'
./mvnw.cmd -version
./mvnw.cmd clean verify
./mvnw.cmd javafx:run
```

Git Bash:

```bash
cd /c/Users/HP/Documents/Codex/campus-marketplace-work/program
./mvnw -version
./mvnw clean verify
./mvnw javafx:run
```

`clean verify` compiles and runs tests. `javafx:run` launches the GUI. Maven uses the pinned project dependencies; a separate JavaFX SDK, hand-added JARs, system Maven installation or SQL server is unnecessary. The first build needs internet. Cached dependencies are required for later offline use.

## Import into Eclipse

1. Configure JDK 21 under **Window > Preferences > Java > Installed JREs**, and bind **JavaSE-21** under **Execution Environments**.
2. Choose **File > Import > Maven > Existing Maven Projects**.
3. Select this **`program` folder**, check its `pom.xml`, and finish.
4. Wait for downloads; use **Maven > Update Project** if needed.
5. Use **Run As > Maven build...**, with Goals **`clean verify`** and JRE **JDK 21**.
6. Make a second Maven launch with Goals **`javafx:run`**, also using JDK 21.

Do not create a separate Java Project or copy only the `.java` files. Keep the wrapper, hidden `.mvn` folder, `pom.xml`, resources and tests together. Eclipse's workspace can remain elsewhere. See [the full setup guide](../SETUP.md).

## Folder map

```text
program/
  pom.xml                         Shared build and dependency versions
  mvnw, mvnw.cmd, .mvn/wrapper/    Official Maven Wrapper
  src/main/java/edu/uc/campusmarket/
    model/                        Listings, subtype evidence, records and validation
    service/                      Category recursion, FIFO queue and LIFO history
    persistence/                  SQLite connections and repositories
    ui/                           JavaFX window and event handlers
  src/main/resources/
    db/                           Schema/bootstrap resources
    ui/                           UI resources
  src/test/java/                  JUnit tests; no public internet dependency
  target/                         Generated build output; do not edit or commit
```

## What this checkpoint demonstrates

### Buyer and seller screens

The app opens on **Marketplace** with a demo buyer selected. Browse item cards, search titles, or choose a category; category searches include all nested subcategories. Open a card to see its stored price, calculated condition, seller and buyer-interest list. Choose **I'm interested** to join the queue. The **Browsing as** selector changes the demo member; it is a prototype selector, not an authentication system.

Category branches start collapsed. Expand **All finds** to see the main categories, then use each branch's arrow to reveal its immediate subcategories. Selecting a category still searches all its descendants, including branches that are not expanded. The initial sample hierarchy is deepest under Electronics; Textbooks and Furniture are currently leaf categories.

**Recently viewed** opens that buyer's history with the newest listing first. Cards can be reopened from history. **Sell an item** opens a separate seller workspace; complete the form and choose **Publish listing** to return to the marketplace. Item illustrations represent categories and are not seller-uploaded photographs. The interface adapts to shorter windows and uses vertical scrolling for longer forms.

### Required behavior

- Abstract `Listing` with `Textbook`, `Electronics` and `Furniture`. Runtime code calls `gradeCondition()` through `Listing` references.
- A category tree searched recursively, including the selected category and every descendant. A leaf is the base case; missing parents and cycles are rejected.
- An actual `Queue` backed by `ArrayDeque` for buyer interest. It serves the earliest joining order first and rejects duplicate active buyers.
- A `Deque` used as a stack for bounded recently viewed IDs: newest first, repeat views moved to the top.
- An ER-derived SQLite database and bound JDBC INSERT/SELECT operations, exposed through a small JavaFX listing workflow.
- Meaningful JUnit tests for normal behavior and edge cases. Consult actual test reports rather than assuming the suite passed.

The condition rules are explicit:

| Type | POOR | Otherwise |
|---|---|---|
| Textbook | Any missing pages | Heavy markings: FAIR; light markings or damaged cover: GOOD; otherwise LIKE_NEW. |
| Electronics | Does not power on, or has a functional defect | Cosmetic damage: GOOD; otherwise LIKE_NEW. |
| Furniture | Unstable, or has structural damage | Heavy surface wear: FAIR; light wear: GOOD; otherwise LIKE_NEW. |

Prices use integer minor units plus a currency code: `35000` with `PHP` means PHP 350.00. Fictional seed data is for demonstration and does not represent real marketplace accounts or transactions.

## Database and moving computers

Default database: **`<user.home>/.ucb-campus-marketplace/marketplace.db`**. Demonstration data is seeded only when the database is first created; restarting should retain saved listings.

Override the data directory with environment variable **`CAMPUS_MARKET_DATA_DIR`**, or the application JVM property **`-Dcampusmarket.dataDir=...`**. For an Eclipse Maven launch, the Environment tab is the simplest way to set the environment variable. A Maven command-line property does not automatically become a property of every forked application JVM.

Transfer this complete folder or clone the repository on the other computer, install JDK 21 and import the same `pom.xml`. The source is portable; the user's database and Maven cache are separate. To preserve demonstration data, close the app before transferring the database into the other computer's selected data folder. Do not put the live database in source resources or Git.

## Viewing your own items

For beginner explanations of the source comments and a suggested reading order, open [the code reading guide](../docs/code-reading-guide.md). Start with Listing and Textbook, then follow one Publish click through the layers. The comments explain meaningful statements and logical groups; closing braces and repeated imports do not need separate explanations.

Choose a member under **Browsing as**, then open **My listings** in the sidebar. Only items belonging to that member's seller profiles appear. Open a card to inspect the item and its buyer-interest queue. **Sell an item** uses that same member: their seller profile is created on first use and reused later. **Posting as** is read-only, and the service rejects posting through another member's profile. A member can both buy and sell. Login/account registration remains deferred.

Earlier listings saved through the old separate seller selector keep their original ownership; this update does not reassign saved items. A new posting as Demo Buyer A will now appear in both the public marketplace and Demo Buyer A's My listings.

To remove your item, open **My listings**, open the item's card, then scroll to **Your listing** and choose **Remove listing**. Choose **Keep listing** to cancel, or **Confirm removal** to withdraw it. Removal is available only to its owner. The item disappears from Marketplace, searches and recent-view cards, and no longer accepts buyer interest. It stays in your My listings archive labeled **Withdrawn**, preserving its database record, subtype details, reviews and saved buyer queue. This is a withdrawal, not a permanent database deletion.

## Deferred work

FakeStoreAPI suggestions, background historical-sales **JSON** import, review entry, analytics and bar charts belong to the next milestone. The instructor-directed format is JSON rather than the original CSV. These features must not be presented as implemented yet.

Members should review and make their own real changes, then explain them. Giving someone a finished file to commit does not establish that they wrote it. No automatic Git publishing is part of running this program.

See [the navigation and demo guide](../docs/navigation.md), [acceptance requirements](../docs/requirements.md) and [work plan](../docs/work-plan.md). Official setup references: [OpenJFX](https://openjfx.io/openjfx-docs/), [Maven Wrapper](https://maven.apache.org/tools/wrapper/), [Eclipse Maven integration](https://eclipse.dev/m2e/documentation/m2e-documentation.html).
