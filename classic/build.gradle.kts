plugins {
    java
}

dependencies {
    implementation(project(":common"))
    // CommandAPI 5.12 (Java 8 compatible), vendored from the official 5.12
    // release jar with plugin-only files removed. It is shaded into the final
    // RelayRace jar by the root shadowJar task.
    implementation(files("$rootDir/libs/commandapi-shade-5.12.jar"))
    compileOnly("org.spigotmc:spigot-api:1.16.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:13.0")
}