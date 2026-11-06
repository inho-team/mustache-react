plugins {
    id("mr.java-conventions")
    `java-gradle-plugin`
    `maven-publish`
}

description = "Gradle plugin for mustache-react: installs Node, builds the frontend into the resources, wires the type generator."

dependencies {
    implementation(project(":mustache-react-build-tools"))
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("mustacheReact") {
            id = "run.iknow.mustache-react"
            implementationClass = "run.iknow.mustachereact.gradle.MustacheReactPlugin"
            displayName = "mustache-react"
            description = project.description
        }
    }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("META-INF/mustache-react/gradle-plugin.properties") {
        expand("version" to version)
    }
}

tasks.test {
    // Gradle 7.6 does not run on Java 21: give the compatibility test an older JDK when one is available,
    // e.g. -PtestGradle7JavaHome=/usr/lib/jvm/java-17 (CI sets it).
    providers.gradleProperty("testGradle7JavaHome").orNull?.let { systemProperty("mustacheReact.gradle7JavaHome", it) }
}
