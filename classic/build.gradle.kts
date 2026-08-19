plugins {
    java
}

dependencies {
    implementation(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.16.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:13.0")
}