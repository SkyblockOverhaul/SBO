pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlin.version").get()
    val preprocessorVersion = providers.gradleProperty("preprocessor.version").get()

    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion

        id("com.replaymod.preprocess-root") version preprocessorVersion
    }

    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
        maven("https://jitpack.io")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.replaymod.preprocess-root") {
                useModule("com.github.replaymod:preprocessor:$preprocessorVersion")
            }
        }
    }
}

rootProject.name = "SBO"
rootProject.buildFileName = "root.gradle.kts"

include("event-processor")

listOf(
    "26.1.2-fabric",
    "26.2-fabric",
    "26.3-fabric"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}

// GuiLib (web-style UI library) lives in its own repository. It normally comes from the SkyblockOverhaul Maven repo;
// when ../SBO-GuiLib is checked out it is built from source instead (composite build). Disable with -Pguilib.local=false.
val guiLibDir = file("../SBO-GuiLib")
if (guiLibDir.isDirectory && providers.gradleProperty("guilib.local").orNull != "false") {
    includeBuild(guiLibDir) {
        dependencySubstitution {
            substitute(module("net.sbo:guilib-26.1.2-fabric")).using(project(":26.1.2-fabric"))
            substitute(module("net.sbo:guilib-26.2-fabric")).using(project(":26.2-fabric"))
            substitute(module("net.sbo:guilib-26.3-fabric")).using(project(":26.3-fabric"))
        }
    }
}
