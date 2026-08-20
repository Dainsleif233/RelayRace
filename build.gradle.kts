plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
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

// Merge the version modules into a single jar. The classic (1.16.1) branch
// runs on CommandAPI 5.12, which is shaded in here so that servers do not need
// to install the CommandAPI plugin separately. The library is also relocated
// into our own namespace (top.syshub.relayrace.libs.commandapi) so it can never
// clash with another plugin that shades its own copy of CommandAPI.
tasks.shadowJar {
    dependsOn(":common:jar", ":latest:jar", ":classic:jar")

    archiveBaseName.set("RelayRace")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(project(":common").tasks.jar.map { zipTree(it.archiveFile) })
    from(project(":latest").tasks.jar.map { zipTree(it.archiveFile) })
    from(project(":classic").tasks.jar.map { zipTree(it.archiveFile) })
    // CommandAPI 5.12 runtime classes, vendored from the official 5.12 release
    // jar with plugin-only files removed and unreachable classes trimmed away
    // (re-seeding from the classic platform's own classes). Shaded in here so
    // no external CommandAPI plugin is required on 1.16.1 servers.
    from(zipTree(file("libs/commandapi-shade-5.12.jar")))

    // Relocate CommandAPI into our own package. Shadow rewrites every bytecode
    // reference (including the references from our classic classes), and the
    // CommandAPI never self-loads a class by the "dev.jorel.commandapi" literal,
    // so relocation is safe at runtime.
    relocate("dev.jorel.commandapi", "top.syshub.relayrace.libs.commandapi")

    // CommandAPI ships NMS adapters for MC 1.13 - 1.16.4, but the classic
    // platform only ever runs on 1.16.1. Drop every adapter except NMS_1_16_R1
    // to slim the shaded jar: the version dispatch is a compile-time number
    // switch, so the dropped classes are never resolved at runtime. Note: shadow
    // filters by the SOURCE entry path (before relocation), hence dev/jorel/...
    exclude("dev/jorel/commandapi/nms/NMS_1_16_R2*.class")
    exclude("dev/jorel/commandapi/nms/NMS_1_16_R3*.class")
    exclude("dev/jorel/commandapi/nms/NMS_1_15*.class")
    exclude("dev/jorel/commandapi/nms/NMS_1_14*.class")
    exclude("dev/jorel/commandapi/nms/NMS_1_13*.class")

    manifest {
        attributes("Implementation-Title" to "RelayRace", "Implementation-Version" to version)
    }
}

// The plain jar would be empty (root project has no own sources); shadowJar is
// now the single deployable artifact.
tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
