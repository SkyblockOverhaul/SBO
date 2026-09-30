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
    "26.1.2-fabric",
    "26.2-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}

rootProject.buildFileName = "root.gradle.kts"

// GuiLib (web-style UI library) lives in its own repository. It normally comes from the SkyblockOverhaul Maven repo;
// when ../SBO-GuiLib is checked out it is built from source instead (composite build). Disable with -Pguilib.local=false.
val guiLibDir = file("../SBO-GuiLib")
if (guiLibDir.isDirectory && providers.gradleProperty("guilib.local").orNull != "false") {
    includeBuild(guiLibDir) {
        dependencySubstitution {
            substitute(module("net.sbo:guilib-26.1.2-fabric")).using(project(":26.1.2-fabric"))
            substitute(module("net.sbo:guilib-26.2-fabric")).using(project(":26.2-fabric"))
        }
    }
}
