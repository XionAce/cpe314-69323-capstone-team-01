# Campus Marketplace with Reviews

**Repository name:** `cpe314-69323-capstone-team-01`

**GitHub repository:** [XionAce/cpe314-69323-capstone-team-01](https://github.com/XionAce/cpe314-69323-capstone-team-01)

## Project information

| Detail | Information |
|---|---|
| Course | CPE 314 - Software Design & Software Design Laboratory |
| Section | BSCPE 3B |
| EDP | 69323 |
| Group | Group 1 / Team 01 |
| Theme | Campus Marketplace with Reviews |
| Technology | Java 21, JavaFX, Maven, SQLite/JDBC and JUnit 5 |

## Team members

| Member | Student number | GitHub username |
|---|---|---|
| Acexion B. Mahigugmaon | 25227422 | XionAce |
| Glyde V. Arreglado | 25248675 | Arreglado-A |
| Tristan Dominic Urot | 25243742 | Tristan-urot |
| John Benedict Alaiza | 25268475 | JohnAlaiza |
| Joshua Brian S. Enovero | PENDING | enovsjbs |
| Eman De Paz | 23261548 | emanuelledepaz-cyber |

Details marked PENDING will be completed by the respective members.

## Project overview

Campus Marketplace with Reviews is a Java desktop prototype for posting and browsing textbooks, electronics and furniture. It includes account login and recovery, condition grading, nested category search, buyer-interest waitlists, recently viewed listings and SQLite persistence.

The current implementation also provides listing reviews, FakeStoreAPI suggestions, background JSON sales-history import and category charts. The correction adds registration, waitlist eligibility for reviews, Java weekly-sales analysis and smaller MVC components. The editable design and PDF describe these implemented features and the current classes and schema.

The marketplace now uses top navigation and a closable right-side details panel, creation-date labels and Active/Sold/Removed badges. My home shows personal listings, waiting buyers, received ratings and queue positions. Buyers can leave their own waitlist rows; current positions advance. In My listings, owners can edit title/price, mark an item sold or remove it without deleting reviews or waitlists. Missing subtype rows show a warning while the remaining listings stay usable. Private listing chats are available in Messages; offers and payment processing remain outside the application.

## Build and run

Use **JDK 21** and Maven **3.9.x**. The supplied Maven Wrapper selects the Maven distribution. First-time dependencies need an internet connection. Keep the Java/Maven version checks enabled.

Open a PowerShell terminal in the repository root, then check both versions:

```powershell
java -version
.\program\mvnw.cmd -version
```

Both must report Java 21. From that same repository root:

```powershell
.\program\mvnw.cmd -f .\program\pom.xml clean verify
.\program\mvnw.cmd -f .\program\pom.xml javafx:run
```

`clean verify` compiles and runs JUnit tests. `javafx:run` opens the application; close the window before entering another command. The Maven project intentionally stays in `program/` to preserve the existing Eclipse and Antigravity project layout. For editor Run profiles, see the [program instructions](program/README.md).

The SQLite database is created outside the repository on first startup. Tests use temporary databases. Do not commit the live database.

## Verification status

JDK 21 Maven verification on 14 November 2026 passed **142 JUnit tests**, with zero failures, errors or skipped tests and the enforcer enabled. **50 earlier application checks passed on 13 November**, including registration, review eligibility, weekly charts and large-import cancellation/rollback. Temporary databases were used. The revised design was checked against the code and schema, and its PDF was visually reviewed. The real HTTP-200/20-product response is captured and tested; the live GUI network-toggle demonstration remains; see [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## Project files

- [Current program](program/)
- [Revised project proposal](docs/proposal.pdf)
- [Revised project design document](docs/design-document.pdf)
- [Editable project design document (Word)](docs/design-document.docx)
- [Final defense presentation (PowerPoint)](docs/Group1_Campus_Marketplace_Final_Defense.pptx)
- [Editable design master](docs/design-document.md)
- [Design PDF exporter](tools/export_design.py) - optional; requires Python, ReportLab and Calibri. Run `python tools/export_design.py` from the repository root. Set `CALIBRI_DIR` for a different font folder.
- [Known issues and correction status](KNOWN_ISSUES.md)
- [Requirement-to-code and test map](REQUIREMENT_MAP.md)

The current integration branch is `project/milestone-2`, with PR #14 titled **Milestone 2 - Team 01**. For final defense, the instructor grades **main after merging Milestone 2**. No new branch is required. Commit/push the verified corrections, review the existing PR, and merge manually before the defense. Then verify a fresh clone of main on the presentation machine. Keep the existing branches/history. See [SETUP.md](SETUP.md) for rehearsal and capture steps.

The application opens maximized. Navigation is across the top; saved reviews open as star/comment cards in a right-side popup. The selling screen includes a type illustration, imports show an indeterminate spinner/bar during actual work, and analytics use spaced colored bars with value tooltips.


## Private listing chat

Open an item and choose **Open chat**. The buyer writes first; the seller replies in **Messages**. Only that buyer and the item's seller can read or send through the app. A different buyer gets a separate conversation. Messages are plain text, 1-2000 characters, saved with UTC timestamps and ordered by database ID. They do not change waitlists, reserve items or process payments.

To demonstrate automatic updates, run the current program twice against the same data folder and sign in with two different accounts. Keep both Messages pages open. The existing three-second refresh displays saved replies without a manual refresh and preserves unsent text. Separate laptops do not share local messages. Existing sold-item chats stay usable for pickup; removed-item chats retain read-only history.

Schema version 5 adds Messages and indexes while preserving earlier accounts and records. The current design includes eleven tables, 56 columns, fifteen foreign keys and 82 main Java types. Eight chat service tests and three chat-controller tests were added. A real two-process GUI test passed seventeen assertions, separately from JUnit.
