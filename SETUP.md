# Computer and repository setup

Status: preparation only. Tool installation and compilation have not yet been verified.

The automated official JDK download failed in this session with a Windows TLS credentials error using both PowerShell and curl. Install from the official page below in your browser; installation is still pending.

## 1. Install JDK 21

Official source: https://adoptium.net/temurin/releases/?version=21

Choose Windows, x64, JDK, version 21 LTS. Download the maintained JDK 21 patch available when installing. Use the installer options for JAVA_HOME and PATH, or set them explicitly. JAVA_HOME points to the JDK directory, not its bin directory. Reopen terminals and coding tools after changing environment variables.

Verify in a new PowerShell terminal:

```powershell
java -version
javac -version
```

Both must report major version 21. Later, `./mvnw.cmd -version` must report that same project JDK. Eclipse itself may use a newer bundled runtime; its project and Maven launches still use JDK 21.

## 2. Install Eclipse IDE for Java Developers

Official source: https://www.eclipse.org/downloads/packages/

Select the maintained Eclipse IDE for Java Developers Windows x86_64 package. It includes Maven integration. Keep the Eclipse workspace outside the Git repository.

In Eclipse, use Window > Preferences > Java > Installed JREs > Add > Standard VM and select the JDK 21 installation. Select JavaSE-21 for the project execution environment.

## 3. Git and Antigravity

Git 2.54.0.windows.1 was found. An Antigravity IDE installation directory was found. Launch and login state were not tested. Keep them; reinstalling is unnecessary based on this audit.

Every teammate needs their own GitHub account. Confirm local commit identity before the first commit:

```powershell
git config user.name
git config user.email
```

Set missing identity to the real contributor's values. Do not use another teammate's identity.

## 4. Team remote repository

EDP 69323 and Group 1 are confirmed. The user created https://github.com/XionAce/cpe314-69323-capstone-team-01; it is Public. The user will add teammates later. Add members as contributors once their accounts are available and include the instructor as the course requires.

This preparation folder can become the local checkout. Before regular development, keep a durable checkout outside the synced ChatGPT project mirror, for example a user-chosen Projects folder. Transfer by cloning the eventual remote, preserving commits. Do not make several independently edited copies.

Follow the provided proposal workflow: `project/proposal` integration branch; smaller feature branches feed into it; submission PR into main titled `Proposal — Team 01`. Leave that checkpoint PR open for grading. Resolve the initial main/proposal bootstrap against any instructor-provided repository template before publishing these drafts.

## 5. Maven bootstrap after proposal sign-off

One maintainer can use a temporary system Maven installation or Eclipse's Maven tooling to create the initial build and generate the official Maven Wrapper. Commit mvnw, mvnw.cmd and .mvn/wrapper files. Subsequent contributors use the wrapper. Do not handwrite replacement wrapper scripts.

https://maven.apache.org/tools/wrapper/

Configure compiler release 21, UTF-8, exact dependency/plugin versions, JUnit 5, JavaFX Maven plugin, and a JDK Enforcer rule. Select JavaFX 21-compatible dependencies rather than copying latest-version examples blindly.

Dependencies: javafx-controls, sqlite-jdbc, a JSON parser, Commons CSV, JUnit Jupiter (test scope). JDBC, HttpClient and Java collections come from the JDK. A standalone SQLite server or manual JavaFX SDK download is unnecessary for this Maven design.

Future shared commands, from repository root:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

The wrapper pins Maven, not the JDK. First build needs internet to download Maven and dependencies. Offline launches need previously cached dependencies plus application API fallback behavior.

## 6. First Eclipse acceptance check

After the minimal application exists: File > Import > Maven > Existing Maven Projects; select pom.xml's folder; allow dependency resolution. Maven > Update Project refreshes configuration. Create Maven Build launches for clean verify and javafx:run, using JDK 21. Check this before adding marketplace features.

Accept only after a fresh checkout builds, a minimal JavaFX window opens, and tests run in both the shared command workflow and Eclipse. Repeat on the actual presentation computer.

## Preparation completion checklist

- [x] Existing tool audit recorded.
- [x] Planning documents and repository guidance prepared.
- [ ] JDK 21 installed; java and javac verified.
- [ ] Eclipse installed and project JDK configured.
- [x] EDP 69323 and Group 1 confirmed.
- [ ] Team roster, roles and remote confirmed; teammates will be added later.
- [ ] Option J availability confirmed.
- [ ] Proposal and initial design document reviewed by the group.
- [ ] Public API accessibility/coverage and currency convention resolved.
- [ ] Required proposal submitted and instructor sign-off recorded.
- [ ] After sign-off: Maven Wrapper, dependency resolution, minimal JavaFX launch and Eclipse import verified.
