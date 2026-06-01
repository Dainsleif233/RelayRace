plugins {
    id("java-library")
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
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
