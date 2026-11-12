# Campus Marketplace with Reviews

**Repository name:** `cpe314-69323-capstone-team-01`

**GitHub repository:** [XionAce/cpe314-69323-capstone-team-01](https://github.com/XionAce/cpe314-69323-capstone-team-01)

## Project information

| Detail | Information |
|---|---|
| Course | CPE 314 — Software Design & Software Design Laboratory |
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
| Eman De Paz | 23261548 | emanuelledepa-cyber |

Details marked PENDING will be completed by the respective members.

## Project overview

Campus Marketplace with Reviews is a Java desktop prototype for posting and browsing textbooks, electronics and furniture. It includes account login and recovery, condition grading, nested category search, buyer-interest waitlists, recently viewed listings and SQLite persistence.

The current implementation also provides listing reviews, FakeStoreAPI suggestions, background JSON sales-history import and category charts. Instructor-requested Java analysis and MVC corrections are still in progress; see the known issues below.

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

The previous verification on 9 November 2026 recorded **81 passing JUnit tests**. This documentation update did not rerun the build or GUI. Repeat verification after changing the program and record the actual results in [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## Project files

- [Current program](program/)
- [Revised project proposal](docs/proposal.pdf)
- [Revised project design document](docs/design-document.pdf)
- [Editable design master](docs/design-document.md)
- [Known issues and correction status](KNOWN_ISSUES.md)
- [Requirement-to-code and test map](REQUIREMENT_MAP.md)

This submission uses `project/milestone-2`. The required pull request title is **Milestone 2 - Team 01**. Keep the existing milestone branches and their commit history. The current PDF remains the baseline until the code corrections and matching design revision have been verified.
