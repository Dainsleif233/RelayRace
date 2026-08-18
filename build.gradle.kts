plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
//     maven("https://mirrors.cqu.edu.cn/maven/")
//     maven("https://mirrors.cqu.edu.cn/maven/papermc/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.aliyun.com/repository/public")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
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
