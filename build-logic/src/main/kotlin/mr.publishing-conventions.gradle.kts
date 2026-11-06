// Publishing of every released Java artifact: POM metadata required by Maven Central, signatures, and a local
// staging repository that scripts/publish-central.sh uploads to the Central Portal.
//
//   ./gradlew publishAllPublicationsToStagingRepository                     (main build)
//   ./gradlew -p build-plugins publishAllPublicationsToStagingRepository    (build plugins)
//
// Signing uses an ASCII-armored key from -PsigningKey / SIGNING_KEY (and -PsigningPassword / SIGNING_PASSWORD);
// without one, artifacts are not signed, which is fine for local builds and mavenLocal.

plugins {
    `maven-publish`
    signing
}

// Both builds (the main one and build-plugins) stage into <repository>/build/staging-deploy.
val repositoryRoot: File = generateSequence(rootDir) { it.parentFile }.first { File(it, "gradlew").exists() }

publishing {
    repositories {
        maven {
            name = "staging"
            url = uri(repositoryRoot.resolve("build/staging-deploy"))
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(provider { project.name })
            description.set(provider { project.description ?: project.name })
            url.set("https://github.com/inho-team/mustache-react")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("inho-team")
                    name.set("inho-team")
                    url.set("https://github.com/inho-team")
                }
            }
            scm {
                url.set("https://github.com/inho-team/mustache-react")
                connection.set("scm:git:https://github.com/inho-team/mustache-react.git")
                developerConnection.set("scm:git:ssh://git@github.com/inho-team/mustache-react.git")
            }
            issueManagement {
                system.set("GitHub")
                url.set("https://github.com/inho-team/mustache-react/issues")
            }
        }
    }
}

// Blank counts as absent: GitHub Actions passes an unset secret as an empty string.
val signingKey = providers.gradleProperty("signingKey").orElse(providers.environmentVariable("SIGNING_KEY"))
    .filter { it.isNotBlank() }
val signingPassword = providers.gradleProperty("signingPassword").orElse(providers.environmentVariable("SIGNING_PASSWORD"))
    .filter { it.isNotBlank() }

signing {
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications)
    }
}

// com.gradle.plugin-publish signs its publications whenever the signing plugin is applied: skip without a key.
tasks.withType<Sign>().configureEach {
    onlyIf("a signing key is configured") { signingKey.isPresent }
}
