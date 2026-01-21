pluginManagement {
    repositories {
        maven("https://maven.deftu.dev/releases")
        maven("https://maven.deftu.dev/snapshots")

        maven("https://jitpack.io/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")

        maven("https://maven.terraformersmc.com/")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://server.bbkr.space/artifactory/libs-release/")

        mavenLocal()
        mavenCentral()

        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version("2.2.21")
        id("dev.deftu.gradle.multiversion-root") version("2.69.0")
    }
}

rootProject.buildFileName = "root.gradle.kts"
include("event-processor")

listOf(
    "1.21.5-fabric",
    "1.21.7-fabric",
    "1.21.10-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}
