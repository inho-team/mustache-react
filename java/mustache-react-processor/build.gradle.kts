plugins {
    id("mr.library-conventions")
}

description = "Annotation processor that writes TypeScript types for @ReactPage return values and @ReactType classes."

// Plain javac API, no dependencies: works with Gradle and Maven, on Java 8 and later.

dependencies {
    // Tests compile sample sources with javac; they need the annotations on the class path.
    testImplementation(project(":mustache-react-spring"))
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")
}
