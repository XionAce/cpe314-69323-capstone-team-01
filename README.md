# UC-B Campus Marketplace with Reviews

**Repository name:** `cpe314-69323-capstone-team-01`

**GitHub repository:** [XionAce/cpe314-69323-capstone-team-01](https://github.com/XionAce/cpe314-69323-capstone-team-01)

## Project information

| Detail | Information |
|---|---|
| Course | CPE 314 — Software Design & Software Design Laboratory |
| Section | BSCPE 3B |
| EDP | 69323 |
| Group | Group 1 / Team 01 |
| Assigned theme | Option J — Campus Marketplace with Reviews |
| Technology | Java 21, JavaFX, Maven, SQLite/JDBC and JUnit 5 |

## Team members

| Member | Student number | GitHub username |
|---|---|---|
| Acexion B. Mahigugmaon | 25227422 | XionAce |
| Glyde V. Arreglado | 25248675 | Arreglado-A |
| Tristan Dominic Urot | 25243742 | Tristan-urot |
| John Benedict Alaiza | 25268475 | JohnAlaiza |
| Joshua Brian S. Enovero | PENDING | enovsjbs |
| Emanuelle De Paz | PENDING | emanuelledepaz-cyber |

Details marked PENDING will be completed by the respective members.

## Project overview

UC-B Campus Marketplace with Reviews is a Java desktop prototype for posting and browsing campus goods. It includes textbook, electronics and furniture listings, condition grading, nested category search, buyer-interest waitlists, recently viewed listings, and SQLite persistence.

Milestone 2 adds reviews, FakeStoreAPI suggestions, background JSON sales-history import, analytics and charts. Instructor corrections add owner-controlled restoration of missing listing condition details.

## Project files

- [Working prototype program](program/README.md)
- [Project proposal master](docs/proposal.md) and [exported PDF](docs/proposal.pdf)
- [Project design master](docs/design-document.md) and [exported PDF](docs/design-document.pdf)
- [Milestone 2 demonstration and correction status](docs/MILESTONE_2.md)

The Markdown files are the editable document masters. Their PDFs are exports of the same content. Run `python docs/export_documents.py` after editing them (Python, ReportLab and Calibri fonts required). The exporter draws the ERD from the Mermaid definitions in the master; do not maintain a separate Word or Google Docs design source.

The Java project requires **JDK 21**. Check both `java -version` and `program/mvnw.cmd -version`; a newer Java version is intentionally rejected by the current build guard. See the program README for Eclipse and Antigravity setup. Milestone work is reviewed through branches and pull requests; the team lead creates the correction PR.
