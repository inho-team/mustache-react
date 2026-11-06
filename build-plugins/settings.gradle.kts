// Build tool integrations: a separate build, so that the example in the main build can apply the Gradle plugin
// from source (pluginManagement.includeBuild in ../settings.gradle.kts).
pluginManagement {
    includeBuild("../build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "mustache-react-build-plugins"

include("mustache-react-build-tools", "mustache-react-gradle-plugin", "mustache-react-maven-plugin")
