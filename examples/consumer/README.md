# Consumer example

A Spring Boot application that uses mustache-react the way any project outside this repository does: the Gradle
plugin, the starter, and the npm packages, nothing else. It is not part of the repository's Gradle build or pnpm
workspace.

`scripts/consumer-test.sh` builds it against the artifacts of the current sources (published to the local Maven
repository and packed as npm tarballs) and checks the result: Node.js is downloaded by the plugin, the npm packages
are installed from the registry, `bootJar` produces one executable jar, and the page is rendered on the server.

After a release, the same project works with the published versions:

```bash
./gradlew bootRun
```
