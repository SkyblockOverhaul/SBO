pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://maven.deftu.dev/releases")
        maven("https://maven.deftu.dev/snapshots")

        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")

        maven("https://repo.essential.gg/repository/maven-public")
    }
}

include("event-processor")

listOf(
    "26.1.2-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}

rootProject.buildFileName = "root.gradle.kts"
