plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("run.iknow.mustache-react")
}

val mustacheReactVersion: String by project

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenLocal() // only needed to try unreleased versions (scripts/consumer-test.sh)
    mavenCentral()
}

dependencies {
    implementation("run.iknow:mustache-react-spring-boot-starter:$mustacheReactVersion")
}

mustacheReact {
    ssr.set(true) // server-side rendering bundle, used with mustache-react.ssr=auto
}
