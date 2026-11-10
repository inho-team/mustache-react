plugins {
    id("mr.java-conventions")
    `java-gradle-plugin`
    id("mr.publishing-conventions")
    // Publishes to the Gradle Plugin Portal: ./gradlew -p build-plugins publishPlugins (GRADLE_PUBLISH_KEY/SECRET).
    id("com.gradle.plugin-publish") version "2.2.1"
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

description = "Gradle plugin for mustache-react: installs Node, builds the frontend into the resources, wires the type generator."

dependencies {
    implementation(project(":mustache-react-build-tools"))
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/inho-team/mustache-react")
    vcsUrl.set("https://github.com/inho-team/mustache-react")
    plugins {
        create("mustacheReact") {
            id = "run.iknow.mustache-react"
            implementationClass = "run.iknow.mustachereact.gradle.MustacheReactPlugin"
            displayName = "mustache-react"
            description = project.description
            tags.set(listOf("spring-boot", "react", "vite", "mustache", "frontend"))
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
    // An unpacked Gradle 7.6.4 (CI downloads it with retries and caches it); without it TestKit downloads one.
    providers.gradleProperty("testGradle7Home").orNull?.let { systemProperty("mustacheReact.gradle7Home", it) }
}
