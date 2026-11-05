plugins {
    id("mr.library-conventions")
}

description = "mustache-react Spring MVC integration (@ReactPage, Mustache helpers). Works with Spring 5.3, 6.x and 7.x."

dependencies {
    api(project(":mustache-react-core"))

    // Compiled against the oldest supported Spring; users bring their own Spring version.
    compileOnly(platform(libs.spring.boot.dependencies.min))
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("com.samskivert:jmustache")

    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-context")
    testImplementation("com.samskivert:jmustache")
}
