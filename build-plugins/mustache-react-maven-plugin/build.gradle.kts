plugins {
    id("mr.java-conventions")
    `java-library`
    id("mr.publishing-conventions")
    // Generates META-INF/maven/plugin.xml from the @Mojo annotations, as maven-plugin-plugin would.
    id("de.benediktritter.maven-plugin-development") version "0.4.3"
}

description = "Maven plugin for mustache-react: installs Node, builds the frontend into the resources."

val mavenVersion = "3.9.11"

dependencies {
    implementation(project(":mustache-react-build-tools"))
    compileOnly("org.apache.maven:maven-plugin-api:$mavenVersion")
    compileOnly("org.apache.maven:maven-core:$mavenVersion")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.15.1")
}

mavenPlugin {
    artifactId.set("mustache-react-maven-plugin")
    goalPrefix.set("mustache-react")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                packaging = "maven-plugin"
            }
        }
    }
}
