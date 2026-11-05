// Example app. The Spring Boot version follows -PspringBootVersion, so the same app is run on Boot 2.7, 3.x and 4.x.
//
//   ./gradlew :examples:basic:run
//   ./gradlew :examples:basic:run -PspringBootVersion=4.1.2

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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mustache")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
