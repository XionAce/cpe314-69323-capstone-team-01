# Learn and demonstrate the simplified prototype

This is a local Java desktop application for Milestone 1. Its single screen contains a listing table, a small posting form, category search and a details area. A member can browse, post and join another member's waitlist. Demo members Alex, Bea and Carlo can all buy or sell; the selector is not a real login. A purchase or payment is not implemented.

The Week 9 GUI requirement is to open a real window and click a working control. Our main demonstration is the `Post listing` button: clicking it saves an item and updates the visible table. `Search / refresh`, `View details` and `Join waitlist` provide small demonstrations of the other requirements. There are no separate Marketplace, My listings or Recently viewed pages in this version.

## Read the 12 Java files in this order

All main Java files are under `program/src/main/java/edu/uc/campusmarket/`.

| Order | File | What to understand |
|---|---|---|
| 1 | `model/Listing.java` | Common fields, validation and abstract grading method. |
| 2 | `model/Textbook.java` | Inherits common fields; checks pages, markings and cover. |
| 3 | `model/Electronics.java` | Checks power, functional defects and cosmetic damage. |
| 4 | `model/Furniture.java` | Checks stability, structural damage and surface wear. |
| 5 | `model/ConditionGrade.java` | The four allowed grading results. |
| 6 | `model/WearLevel.java` | The three allowed wear/marking levels. |
| 7 | `model/Member.java` | A person's ID and name; that person may buy and sell. |
| 8 | `model/Category.java` | A category's ID, name and optional parent ID. |
| 9 | `persistence/Database.java` | Opens SQLite connections and runs the SQL. |
| 10 | `service/MarketplaceService.java` | Connects actions, rules, category recursion, queue and stack. |
| 11 | `ui/MarketplaceView.java` | Builds controls, reads forms and displays results. |
| 12 | `App.java` | Creates the service and window, then starts JavaFX. |

Start with the first four files. You do not need to memorize every JavaFX layout setting before understanding how a listing works.

## Follow one action through the whole program

Example: posting a Java textbook for PHP 350.50.

1. Enter the title, price, type, category and condition observation in the small form on the same screen. Click `Post listing`.
2. It creates a `Textbook`. The `Listing` constructor validates the shared fields.
3. The screen calls `MarketplaceService.post(memberId, listing)` on its database worker.
4. The service checks that the selected member owns the seller profile.
5. `Database.insertListing()` inserts one `Listings` row and one `TextbookDetails` row in one transaction.
6. `commit()` keeps both rows. If a step fails, `rollback()` prevents a half-saved item.
7. After posting, the screen calls `search()` for All finds, which reads `Database.getListings()` again through JDBC `SELECT` and updates the table.
8. Database rows become concrete `Textbook`, `Electronics` or `Furniture` objects.
9. The screen displays those results on the JavaFX application thread.

The lasting data is in the SQLite `.db` file, not in the visible table. Closing and reopening the application should reload saved listings. Recent views are deliberately kept only for the current session.

The button is connected to its handler with `post.setOnAction(event -> postListing())`. Read this as: "when the button is clicked, run `postListing()`." The method reads and validates the form, then asks the service to save the new listing.

## Concepts you should be able to explain

**Inheritance and abstraction.** `Textbook extends Listing` means a textbook shares the parent's common fields and methods. `Listing` is abstract because an item needs a concrete type to determine its condition.

**Runtime polymorphism.** A variable typed `Listing` can refer to any of the three subclasses. Calling `listing.gradeCondition()` runs the method for the object's actual type. The screen uses this shared method when displaying a condition; it does not need separate grading rules for every row.

**Condition rules.** A textbook with missing pages is POOR even if everything else looks good. Electronics with no power or a functional defect are POOR. Unstable or structurally damaged furniture is POOR. Cosmetic evidence matters only after these serious checks pass.

The small form lets you choose one real observation, such as `No visible damage`, `Heavy markings` or `Does not power on`. The screen converts that observation into the matching subclass's condition fields. It does not let you choose a grade directly. In this skeleton, `Missing pages` represents one missing page; the form does not ask for an exact count. The model still supports full counts and combined evidence, computes the grade, and has tests for combinations that the small form does not enter together.

**Category recursion.** Choose a category from the dropdown and click `Search / refresh`. `categoryIdsUnder()` calls `collectCategoryIds()`. The helper adds the current ID, finds its children and calls itself for each child. A leaf has no children, so it makes no further call and returns: this is the base case. Searching Electronics therefore also includes Computers > Laptops > Gaming Laptops. The category data still forms a tree even though the small GUI uses a dropdown. Validation rejects missing parents and cycles first.

**Queue: first in, first out (FIFO).** `getWaitlist()` rebuilds a `Queue<Long>` from database entries ordered by `joiningOrder`. `offer()` adds at the tail; `poll()` removes the head for display. The first buyer who joined appears first. Polling this temporary queue does not delete the saved waitlist or complete a sale.

**Stack: last in, first out (LIFO).** `view()` puts a listing ID on a member's `Stack<Long>` with `push()`. `recent()` uses `pop()` on a copy to read newest first. Reopening an item moves it to the top instead of duplicating it. Each member keeps at most 20 IDs; closing the app clears these stacks.

**Map.** `Map<Long, Stack<Long>>` associates a member ID with that member's history. Switching demo members therefore does not mix their recent views.

**JDBC and prepared statements.** JDBC connects Java to SQLite. SQL has `?` placeholders; `setLong()` and `setString()` supply values separately. This handles values such as an apostrophe in a title without building SQL from typed text. `executeUpdate()` changes rows; `executeQuery()` reads a `ResultSet`. Try-with-resources closes connections, statements and results.

**Tables and foreign keys.** `schema.sql` preserves the ten-table design: Members, Sellers, Categories, Listings, Reviews, BuyerInterests, HistoricalSales and three subtype-detail tables. A foreign key links related IDs. One listing has one matching details row. Reviews and HistoricalSales are reserved for later features; their tables do not imply those screens exist yet.

**Money.** `BigDecimal` represents the entered decimal price exactly. PHP 350.50 is stored as the integer `35050` plus currency `PHP`. The model rejects fractional cents and overflowing values instead of silently rounding. Binary floating-point `double` is unnecessary here.

**JavaFX and CSS.** JavaFX supplies the window and controls. CSS supplies colors, spacing and appearance; it does not turn this program into a website. The Java event handlers still perform the actions.

**Background work.** The screen sends database work to one worker so database activity does not freeze the window. The result returns to the JavaFX thread for display. This is a small supporting mechanism; the full background JSON importer remains later work.

## Rehearse the instructor's five-minute demo

Before class, import `program` as an existing Maven project in Eclipse, select JDK 21 and use the supplied **Run-Campus-Marketplace** Java Application profile. In Antigravity, open the `program` folder and use **Run Campus Marketplace** in Run and Debug after installing the Java extensions. The run guide explains the one-time setup; no Maven launch goal is needed for these profiles. Rehearse every action. Keep the source, test results and database viewer ready. Use the actual database path shown by the app; the default on this computer is `C:\Users\HP\.ucb-campus-marketplace-simple\marketplace.db`.

| Time | What to demonstrate |
|---|---|
| 0:00-1:00 | Open Listing and its three subclasses. Explain one condition rule and the shared `gradeCondition()` call. |
| 1:00-2:00 | Run the app. Post a textbook named `Milestone demo book`, then find it in the marketplace. Explain the working button handler. |
| 2:00-3:30 | Open the actual SQLite file, show related tables and execute the query below live. Point to the prepared INSERT and bound SELECT in Database.java. |
| 3:30-4:30 | Run the JUnit tests live. Open one edge case, explain its expected result and show the current passing output. |
| 4:30-5:00 | Answer the instructor's member question. State exactly which later features remain. |

In a SQLite viewer, this read-only query shows saved data; Java itself uses prepared statements:

```sql
SELECT id, title, type, priceMinor, currency, status
FROM Listings
ORDER BY id DESC;
```

For a queue rehearsal outside the timed demo: choose a listing owned by another member, click `Join waitlist`, switch to a second non-owner and join again. The details area should show the earlier buyer first. For recursion: search Electronics and explain why its deeply nested laptop appears. For the stack: select an available item and click `View details`; repeat for a second item and check the recent-view preview in the details area. It shows up to three recent titles, newest first, from the member's 20-item stack. Selecting a row alone does not add a recent view.

The commands below remain optional alternatives in PowerShell. For normal app use, press the configured editor Run button. To run tests in Eclipse without Maven Build, select `src/test/java` and choose **Run As > JUnit Test**.

```powershell
cd C:\Users\HP\Documents\Codex\campus-marketplace-work\program
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

## Practice questions and short answers

| Instructor might ask | A concise answer |
|---|---|
| Why not make one Listing class with all the grading code? | Each type has different evidence and rules. Inheritance keeps common fields together while each subclass owns its condition method. |
| Where is polymorphism actually used? | The screen receives Listing objects and calls `gradeCondition()` through that shared type; Java chooses the subclass method. |
| What stops the recursion? | A leaf category has no children, so the helper makes no more recursive calls and returns. |
| Why a queue? | Buyer interest has a joining order. The first buyer who joined is displayed first. |
| Why a stack? | Recently viewed items must show the last item opened first. |
| What if someone joins twice? | The database action rejects the duplicate; its unique constraint also protects the stored entries. |
| Why is there no remove button or My listings page now? | This version keeps the GUI small for Week 9. The tested backend ownership/withdrawal methods remain, but those controls are not on this screen. |
| What happens if saving the subtype fails? | The transaction rolls back, so the shared listing row is not left behind. |
| Where is the database? | In the writable user data folder shown by the app, separate from Java source and resources. |
| Is this a live online marketplace? | It is a local desktop prototype using a local SQLite file. Networking, authentication and payments are outside this milestone. |
| Have you finished FakeStoreAPI or the JSON import? | No. API suggestions, background JSON sales import, review entry, analytics and charts are planned for Milestone 2. |

Do not claim a fixed test count from memory. Show the output from the version you actually run. Each member should understand a real change they reviewed or made; assigning a file name does not demonstrate understanding.
