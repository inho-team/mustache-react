# Releasing

Java artifacts (Maven Central), the Gradle plugin (Gradle Plugin Portal and Maven Central) and the npm packages are
released together, with one version.

## One-time setup

| What | Where |
|---|---|
| Maven Central namespace `run.iknow` | [central.sonatype.com](https://central.sonatype.com): add the namespace and verify it with the DNS TXT record the portal shows for `iknow.run` |
| `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` | A user token generated in the Central Portal (Account → Generate User Token) |
| `SIGNING_KEY`, `SIGNING_PASSWORD` | An ASCII-armored GPG private key (`gpg --armor --export-secret-keys <id>`) and its passphrase; publish the public key to `keys.openpgp.org` |
| `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` | [plugins.gradle.org](https://plugins.gradle.org) API keys. The plugin id `run.iknow.mustache-react` needs the portal's approval of the `run.iknow` prefix on first publication |
| `NPM_TOKEN` | An npm automation token of an account that owns the `@mustache-react` organization (create the organization first) |
| GitHub Pages | Settings → Pages → Source: GitHub Actions (for the documentation site) |

Add the secrets under Settings → Secrets and variables → Actions.

## Dry run

Actions → Release → Run workflow with **dry_run** checked. It runs all tests and the consumer test, stages and signs
the Java artifacts, validates the Gradle plugin and packs the npm packages, without uploading anything. To dry-run
a real version, run it on a branch where `scripts/set-version.mjs` has set that version.

## Release

```bash
node scripts/set-version.mjs 0.1.0
```

Update `CHANGELOG.md` (move "Unreleased" under `## [0.1.0] - <date>`), then commit, tag and push:

```bash
git commit -am "release: 0.1.0" && git tag v0.1.0 && git push origin main v0.1.0
```

The Release workflow checks that the tag matches `gradle.properties`, tests everything, publishes to Maven Central,
the Gradle Plugin Portal and npm (with provenance), and creates the GitHub release from the changelog section.
Maven Central takes a few minutes to show new versions; search.maven.org can take longer.

Afterwards, move to the next snapshot:

```bash
node scripts/set-version.mjs 0.2.0-SNAPSHOT && git commit -am "build: 0.2.0-SNAPSHOT" && git push
```
