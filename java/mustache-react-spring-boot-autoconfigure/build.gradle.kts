plugins {
    id("mr.library-conventions")
}

description = "mustache-react Spring Boot auto-configuration (Spring Boot 2.7, 3.x, 4.x)."

dependencies {
    api(project(":mustache-react-spring"))

    compileOnly(platform(libs.spring.boot.dependencies.min))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("com.samskivert:jmustache")
    compileOnly("org.thymeleaf:thymeleaf")
    annotationProcessor(platform(libs.spring.boot.dependencies.min))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-mustache")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf")
}
