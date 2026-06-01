plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    maven("https://mirrors.cqu.edu.cn/maven/")
    maven("https://mirrors.cqu.edu.cn/maven/papermc/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

tasks {
    processResources {
        from(rootProject.file("LICENSE"))
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
