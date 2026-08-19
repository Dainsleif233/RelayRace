pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin/")
        maven("https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "RelayRace"
include("common", "latest", "classic")
