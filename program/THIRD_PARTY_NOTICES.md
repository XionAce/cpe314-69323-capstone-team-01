# Third-party components

Gson 2.13.2 is used to parse catalog JSON and stream the sales-history file. It is a Maven dependency, licensed under Apache License 2.0: https://github.com/google/gson. No Gson binary is included in the repository.

The `mvnw` and `mvnw.cmd` files are Apache Maven Wrapper 3.3.4 only-script templates from the Apache Software Foundation. Only the template's version placeholder was filled in. The original license headers are retained.

- Source: https://github.com/apache/maven-wrapper/tree/maven-wrapper-3.3.4/maven-wrapper-distribution/src/resources
- License and notice: `.mvn/wrapper/LICENSE.txt` and `.mvn/wrapper/NOTICE.txt`

Application dependencies and plugins are declared with exact versions in `pom.xml`. Maven downloads them; their binaries are not vendored into this repository. Their respective licenses remain applicable. This notice does not assign a new license to the team's application source.
