plugins {
    java
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:13.0")
    // Soft-dependency API only — never shade FlashbackServer classes.
    // Runtime resolution: plugin.yml softdepend + check PluginManager before use.
    compileOnly(files(rootProject.file("libs/FlashbackServer-1.2.3.jar")))
}

tasks.processResources {
    val projectVersion = project.version.toString()
    inputs.property("version", projectVersion)
    filesMatching("plugin.yml") {
        expand("version" to projectVersion)
    }
}
