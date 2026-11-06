// Group and version come from the main build's gradle.properties, so all artifacts are released together.
val mainProperties = java.util.Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}

allprojects {
    group = mainProperties.getProperty("group")
    version = mainProperties.getProperty("version")
}
