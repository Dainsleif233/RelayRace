plugins {
    java
}

allprojects {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://maven.aliyun.com/repository/public")
    }
}

sourceSets.main {
    java.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
}

subprojects {
    pluginManager.apply("java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:-options")
    }

    tasks.jar {
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

tasks.jar {
    dependsOn(":common:jar", ":latest:jar", ":classic:jar")
    archiveBaseName.set("RelayRace")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(project(":common").tasks.jar.map { zipTree(it.archiveFile) })
    from(project(":latest").tasks.jar.map { zipTree(it.archiveFile) })
    from(project(":classic").tasks.jar.map { zipTree(it.archiveFile) })
    from(layout.projectDirectory.file("LICENSE"))

    manifest {
        attributes("Implementation-Title" to "RelayRace", "Implementation-Version" to version)
    }
}
