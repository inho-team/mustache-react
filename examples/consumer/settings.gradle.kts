pluginManagement {
    val mustacheReactVersion: String by settings
    repositories {
        mavenLocal() // only needed to try unreleased versions (scripts/consumer-test.sh)
        gradlePluginPortal()
    }
    plugins {
        id("run.iknow.mustache-react") version mustacheReactVersion
    }
}

rootProject.name = "consumer"
