# Project structure and next steps

This preparation scaffold follows the Student Guide — Group Capstone Project (Weeks 8–11):
https://drive.google.com/file/d/195Y8cNaHnd0M7FpIwmKDYzfok5pbKZti/view

## Repository layout

```text
cpe314-69323-capstone-team-01/
├── TEAM_INFO.md
├── README.md
├── SETUP.md
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/edu/uc/campusmarket/
│   │   │   ├── domain/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── ui/
│   │   │   ├── integration/
│   │   │   └── analytics/
│   │   └── resources/
│   └── test/
│       ├── java/edu/uc/campusmarket/
│       └── resources/
└── docs/
    ├── proposal.md
    ├── design-document.md
    ├── requirements.md
    ├── work-plan.md
    ├── structure.md
    ├── erd.png
    ├── erd.svg
    └── erd-notes.md
```

The .gitkeep files preserve empty folders in Git. Replace them as real source files and resources are added.

## Responsibilities of each package

| Package | Planned responsibility |
|---|---|
| domain | Listings, condition grading, categories, seller and review objects |
| service | Use cases, validation, queue/stack behavior and recursive category traversal |
| repository | SQLite connections, SQL, JDBC mappings and transactions |
| ui | JavaFX views and thin event controllers |
| integration | Product catalog HTTP/JSON and CSV import components |
| analytics | Category price/rating calculations |
| src/test/java | JUnit tests for domain rules, services, integrations and persistence |
| src/main/resources | Stylesheets, static resources and future SQL migration scripts |
| src/test/resources | Small deterministic CSV/JSON/SQL test fixtures |

Keep the writable runtime SQLite database outside classpath resources and outside Git.

## Current state

This is a folder and documentation scaffold. There is no application entry point, runnable GUI, application dependency set, implemented database schema, or test suite yet. The preliminary ER image describes the existing proposal, not a deployed database.

The starter POM declares Java 21, UTF-8 and a pinned compiler plugin. Maven Wrapper, JavaFX, SQLite JDBC, JSON, CSV and JUnit dependencies will be configured and tested during bootstrap after instructor sign-off. No javafx:run command exists at this stage.

## Eclipse

Use File > Import > Maven > Existing Maven Projects and select the folder containing pom.xml. Configure the project JDK as Java 21 and let Maven resolve dependencies. A launchable application will be added after approval; importing a scaffold does not prove that a GUI runs.

## Checkpoint workflow

| Week | Integration branch | Checkpoint PR into main |
|---|---|---|
| 8 | project/proposal | Proposal — Team 01 |
| 9 | project/milestone-1 | Milestone 1 — Team 01 |
| 10 | project/milestone-2 | Milestone 2 — Team 01 |

Start each new checkpoint from up-to-date main after the prior checkpoint is integrated as instructed. Smaller feature branches feed the current checkpoint branch first. Open its PR before the deadline; the opening timestamp is the submission time.

Keep the checkpoint PR available for instructor grading and follow the instructor's merge direction. At defense time the instructor tags the main state after Milestone 2 is integrated. This structure PR is a feature PR, not a new checkpoint submission and not a replacement submission timestamp.

Use type: short description commit messages (feat, fix, test, docs, refactor, chore). Every member uses their own account and contributes actual work. Review the guide's contribution expectations before finalizing implementation ownership.

## Next development gate

Record instructor proposal sign-off and any scope changes before implementing marketplace features. Confirm the official private team repository and transfer policy first; do not treat the current personal repository as instructor-provisioned.
