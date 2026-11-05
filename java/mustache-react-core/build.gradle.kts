plugins {
    id("mr.library-conventions")
}

description = "mustache-react core: rendering protocol shared by the Spring integration and the JS runtime. No Spring dependency."

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("META-INF/mustache-react/version.properties") {
        expand("version" to version)
    }
}
