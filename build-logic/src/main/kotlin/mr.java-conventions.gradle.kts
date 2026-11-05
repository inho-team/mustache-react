// Common Java setup for every JVM project in this repository.
//
// - Main code targets Java 8 bytecode, unless a project raises `mrMainRelease`.
// - Tests run against the Spring Boot version given by -PspringBootVersion, so the same
//   library binary is verified on Boot 2.7 (javax) and Boot 3.x/4.x (jakarta).
// - -PtestJavaVersion=8|17|21 runs tests on that JDK through Gradle toolchains.

plugins {
    java
}

repositories {
    mavenCentral()
}

val springBootVersion: String = providers.gradleProperty("springBootVersion").get()
val bootMajor = springBootVersion.substringBefore('.').toInt()

// Boot 3+ ships Java 17 class files, so test sources compiled against it need release 17.
val testRelease = if (bootMajor >= 3) 17 else 8

extra["springBootVersion"] = springBootVersion
extra["bootMajor"] = bootMajor
extra["testRelease"] = testRelease

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // "-options" hides the "source value 8 is obsolete" warning from newer JDKs.
    options.compilerArgs.addAll(listOf("-Xlint:all,-options,-processing,-serial", "-parameters"))
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(testRelease)
}

dependencies {
    "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testImplementation"("org.assertj:assertj-core")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    providers.gradleProperty("testJavaVersion").orNull?.let { version ->
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(version))
        })
    }
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
