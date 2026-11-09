plugins {
    id("mr.library-conventions")
}

description = "Shared by the mustache-react Gradle and Maven plugins: installs Node, runs the package manager and the frontend build."

// No dependencies: the Gradle and Maven plugins bundle it as is.

// The version the generated package.json uses for the @mustache-react/* packages (FrontendScaffold).
val writeVersion by tasks.registering {
    val output = layout.buildDirectory.dir("generated/version")
    val version = project.version.toString()
    inputs.property("version", version)
    outputs.dir(output)
    doLast {
        val file = output.get().file("run/iknow/mustachereact/build/mustache-react-build.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$version\n")
    }
}
sourceSets.main {
    resources.srcDir(writeVersion)
}
