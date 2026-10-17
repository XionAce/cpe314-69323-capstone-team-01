# Campus Marketplace repository guidance

This is the team's CPE 314 Option J preparation repository.

- Current phase: preparation and proposal. Obtain the user's confirmation that instructor sign-off exists before beginning Milestone 1 application implementation. Do not infer sign-off from the existence of these documents.
- Use Java 21 and JavaFX 21 with a single Maven module. Pin exact tested dependency and plugin versions during bootstrap. Avoid preview language features.
- Maven is the shared build definition for Codex, Antigravity, Eclipse and automated checks. Commit Maven Wrapper. Use standard Maven source/resource/test folders.
- Target package: edu.uc.campusmarket. Begin nonmodular unless a documented requirement changes this decision.
- Keep GUI controllers thin. Plain Java domain/services own business rules. JDBC repositories own SQL. ProductCatalogClient isolates HTTP integration.
- Use PreparedStatement for data values and try-with-resources. Enable foreign keys on every SQLite connection. Never share a JDBC connection across worker threads.
- Keep mutable database files outside classpath resources. Resolve a stable configurable writable application-data directory.
- Money uses integer minor units in storage and BigDecimal for calculations; record currency. Do not silently treat an API amount as PHP.
- Category traversal must genuinely recurse in Java and support arbitrary nesting without hardcoded levels. Reject cycles.
- Waitlists demonstrate FIFO; recently viewed demonstrates LIFO. Document collection and persistence policies.
- File imports, HTTP requests and substantial database work run off the JavaFX application thread. UI changes belong on that thread. Handle failure/cancellation and shut down executors.
- Test core behavior with JUnit 5 and temporary databases. Tests must be deterministic and not depend on the public internet. At least six meaningful tests are required by the course.
- Use small feature branches and honest per-member commits. Do not invent contribution history. Course checkpoint submission PRs stay open for grading.
- Read docs/requirements.md and docs/work-plan.md before changes. Update the cumulative design document with implementation evidence as work lands.
- Do not modify any synced sources/ files outside this repository.
- No secrets, downloaded tool distributions, generated build output or live database files in Git.

After Maven bootstrap, the shared checks on Windows are `./mvnw.cmd clean verify` and `./mvnw.cmd javafx:run`. These commands are not available yet in the preparation repository.
