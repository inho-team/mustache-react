// Example app. The Spring Boot version follows -PspringBootVersion, so the same app is run on Boot 2.7, 3.x and 4.x.
//
//   pnpm install                       (once, at the repository root)
//   ./gradlew :examples:basic:run
//   ./gradlew :examples:basic:run -PspringBootVersion=4.1.2
//   ./gradlew :examples:basic:run --args=--spring.profiles.active=dev    (Vite dev server with HMR, started by Spring)
//
// -PmustacheReact.skipFrontend skips the frontend build (the tests use a fixed manifest and do not need it).

plugins {
    id("mr.java-conventions")
    application
    id("run.iknow.mustache-react")
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
    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// An application outside this repository needs no mustacheReact block. Inside it, the example's frontend belongs to
// the repository's pnpm workspace: dependencies are installed once at the root and the @mustache-react/* packages
// come from js/, not from npm.
mustacheReact {
    install.set(false)
    nodeDownload.set(false)
    processorDependency = project(":mustache-react-processor")
}

val buildWorkspacePackages by tasks.registering(Exec::class) {
    description = "Builds the @mustache-react/* packages the example frontend uses from the workspace."
    workingDir = rootDir
    commandLine("pnpm", "--filter", "@mustache-react-examples/basic^...", "run", "build")
    inputs.files(fileTree(rootDir.resolve("js")) { include("*/src/**", "*/package.json") }).withPropertyName("packages")
    outputs.dirs(listOf("client", "server", "vite-plugin").map { rootDir.resolve("js/$it/dist") })
}

tasks.named("mustacheReactBuild") {
    dependsOn(buildWorkspacePackages)
}
buildWorkspacePackages {
    onlyIf { mustacheReact.frontendBuild.get() }
}
