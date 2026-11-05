// Example app. The Spring Boot version follows -PspringBootVersion, so the same app is run on Boot 2.7, 3.x and 4.x.
//
//   pnpm install                       (once, at the repository root)
//   ./gradlew :examples:basic:run
//   ./gradlew :examples:basic:run -PspringBootVersion=4.1.2
//
// -PskipFrontend skips the frontend build (the tests use a fixed manifest and do not need it).

plugins {
    id("mr.java-conventions")
    application
}

val springBootVersion: String by extra
val testRelease: Int by extra

// Boot 2.7 apps may target Java 8; Boot 3+ needs Java 17.
tasks.named<JavaCompile>("compileJava") {
    options.release.set(testRelease)
}

application {
    mainClass.set("run.iknow.mustachereact.examples.basic.BasicApplication")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(project(":mustache-react-spring-boot-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// --- Frontend build ------------------------------------------------------------------------------------------------
// Stand-in until the mustache-react Gradle plugin (M4): runs `vite build` through pnpm and adds the output
// (static/mustache-react/**) to the application resources.

val skipFrontend = providers.gradleProperty("skipFrontend").isPresent
val frontendOutput = layout.buildDirectory.dir("mustache-react")

val buildFrontend by tasks.registering(Exec::class) {
    description = "Builds src/main/frontend with Vite."
    group = "build"
    enabled = !skipFrontend
    workingDir = rootDir
    // The trailing "..." also builds the workspace packages the example depends on (@mustache-react/*).
    commandLine("pnpm", "--filter", "@mustache-react-examples/basic...", "run", "build")
    inputs.files(fileTree("src/main/frontend") { exclude("node_modules/**") }).withPropertyName("sources")
    inputs.files(fileTree(rootDir.resolve("js")) { include("*/src/**", "*/package.json") }).withPropertyName("packages")
    outputs.dir(frontendOutput.map { it.dir("static") })
}

tasks.processResources {
    if (!skipFrontend) {
        from(buildFrontend.map { frontendOutput.get() })
    }
}
