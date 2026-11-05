// Published library modules (run.iknow:mustache-react-*).

plugins {
    id("mr.java-conventions")
    `java-library`
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "run.iknow.mustachereact." +
                project.name.removePrefix("mustache-react-").replace("-", "."),
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set(provider { project.description ?: project.name })
                url.set("https://github.com/inho-team/mustache-react")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/inho-team/mustache-react")
                    connection.set("scm:git:https://github.com/inho-team/mustache-react.git")
                }
            }
        }
    }
}
