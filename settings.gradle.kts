pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mustache-react"

val javaModules = listOf(
    "mustache-react-core",
    "mustache-react-spring",
    "mustache-react-spring-boot-autoconfigure",
    "mustache-react-spring-boot-starter",
    "mustache-react-processor",
)
javaModules.forEach { name ->
    include(name)
    project(":$name").projectDir = file("java/$name")
}

include(":examples:basic")
