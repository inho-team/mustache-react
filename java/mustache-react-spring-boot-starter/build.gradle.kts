plugins {
    id("mr.library-conventions")
}

description = "Spring Boot starter for mustache-react. Add this one dependency to use React pages from Spring MVC."

dependencies {
    api(project(":mustache-react-spring-boot-autoconfigure"))
}
