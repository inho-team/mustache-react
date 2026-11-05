plugins {
    id("mr.library-conventions")
}

description = "Spring Boot starter for mustache-react. Add this one dependency to use React pages from Spring MVC."

dependencies {
    api(project(":mustache-react-spring-boot-autoconfigure"))

    // Brings Spring MVC and JMustache. The version is the minimum supported one; the application's Spring Boot
    // dependency management (Maven BOM or Gradle plugin/platform) replaces it with the application's version.
    api("org.springframework.boot:spring-boot-starter-web:${libs.versions.springBootMin.get()}")
    api("org.springframework.boot:spring-boot-starter-mustache:${libs.versions.springBootMin.get()}")
}
