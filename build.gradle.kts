import java.nio.charset.StandardCharsets
import dev.deftu.gradle.utils.version.MinecraftVersions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.21"
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools")
    id("dev.deftu.gradle.tools.resources")
    id("dev.deftu.gradle.tools.bloom")
    id("dev.deftu.gradle.tools.shadow")
    id("dev.deftu.gradle.tools.minecraft.loom")
    id("dev.deftu.gradle.tools.minecraft.releases")
    id("com.google.devtools.ksp") version "2.3.6"
}

loom {
    // Temporarily disabled because of build failure on latest versions:
    // java.lang.IllegalStateException: Javadoc provided by mod (fabric-content-registries-v0) must be have an intermediary source namespace
    enableModProvidedJavadoc = false

    // Some stuff were made private / package-private in later versions, so we need this.
    accessWidenerPath = file("src/main/resources/sbo-kotlin.accesswidener")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        val args = mutableListOf<String>()
        args.addAll(freeCompilerArgs.get())

        args.addAll(
            listOf(
                "-Xbackend-threads=0", // 0 means use 1 thread per core. Default value is 1 which is single threaded and doesn't scale, often bottlenecks compilation
                "-jvm-default=no-compatibility", // this not a library mod or API, no need to generate additional DefaultImpls classes (which is bigger jar size and more compile time)
            )
        )

        freeCompilerArgs = args
    }
}

kotlin {
    // This improves build performance as it supports incremental compilation among other things with the BTA API
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion = "2.2.10"
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.azureaaron.net/releases")
    maven("https://api.modrinth.com/maven")
}

toolkitMultiversion {
    moveBuildsToRootProject.set(true)
}

tasks.withType<JavaCompile> {
  options.encoding = StandardCharsets.UTF_8.toString()
}

tasks.withType<AbstractArchiveTask> {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}

tasks.named<ProcessResources>("processResources") {
    val fabricLoaderVersion = mcData.dependencies.fabric.fabricLoaderVersion
    val fabricApiVersion = mcData.dependencies.fabric.fabricApiVersion
    val fabricLanguageKotlinVersion = mcData.dependencies.fabric.fabricLanguageKotlinVersion
    val javaVersionMajor = mcData.version.javaVersion.majorVersion

    val elementaVersion = project.property("elementa.version")
    val hmApiVersion = when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> project.property("hmapi.version.1.21.11")
        MinecraftVersions.VERSION_1_21_10 -> project.property("hmapi.version.1.21.10")
        else -> throw AssertionError("build.gradle.kts needs updating for ${mcData.version}")
    }
    val resourcefulConfigVersion = when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> project.property("rconfig.version.1.21.11")
        MinecraftVersions.VERSION_1_21_10 -> project.property("rconfig.version.1.21.10")
        else -> throw AssertionError("build.gradle.kts needs updating for ${mcData.version}")
    }
    val resourcefulConfigKtVersion = when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> project.property("rconfigkt.version.1.21.11")
        MinecraftVersions.VERSION_1_21_10 -> project.property("rconfigkt.version.1.21.10")
        else -> throw AssertionError("build.gradle.kts needs updating for ${mcData.version}")
    }
    val universalCraftVersion = project.property("uc.version")

    inputs.property("fabric_loader_version", fabricLoaderVersion)
    inputs.property("fabric_api_version", fabricApiVersion)
    inputs.property("fabric_language_kotlin_version", fabricLanguageKotlinVersion)
    inputs.property("java_version_major", javaVersionMajor)

    inputs.property("elementa_version", elementaVersion)
    inputs.property("hm_api_version", hmApiVersion)
    inputs.property("resourcefulconfig_version", resourcefulConfigVersion)
    inputs.property("resourcefulconfigkt_version", resourcefulConfigKtVersion)
    inputs.property("universalcraft_version", universalCraftVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "fabric_loader_version" to fabricLoaderVersion,
                "fabric_api_version" to fabricApiVersion,
                "fabric_language_kotlin_version" to fabricLanguageKotlinVersion,
                "java_version_major" to javaVersionMajor,

                "elementa_version" to elementaVersion,
                "hm_api_version" to hmApiVersion,
                "resourcefulconfig_version" to resourcefulConfigVersion,
                "resourcefulconfigkt_version" to resourcefulConfigKtVersion,
                "universalcraft_version" to universalCraftVersion
            ) + inputs.properties
        )
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    modImplementation(include("gg.essential:elementa:${property("elementa.version")}")!!)

    // modImplementation(include("xyz.meowing:vexel-${mcData}:${property("vexel.version")}")!!)
    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> {
            modImplementation(include("net.azureaaron:hm-api:${property("hmapi.version.1.21.11")}")!!)
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.11")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.11:${property("rconfig.version.1.21.11")}")!!)
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.11:${property("rconfigkt.version.1.21.11")}")!!)
            modImplementation(include("gg.essential:universalcraft-1.21.11-fabric:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.11")}+1.21.11-fabric")
        }
        MinecraftVersions.VERSION_1_21_10 -> {
            modImplementation(include("net.azureaaron:hm-api:${property("hmapi.version.1.21.10")}")!!)
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.10")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${property("rconfig.version.1.21.10")}")!!) // .9 works on .10
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${property("rconfigkt.version.1.21.10")}")!!)  // .5 works on .10
            modImplementation(include("gg.essential:universalcraft-1.21.9-fabric:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.10")}+1.21.10-fabric")
        }
        else -> throw AssertionError("build.gradle.kts needs updating for ${mcData.version}")
    }

    implementation(project(":event-processor"))
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> dependsOn(":1.21.10-fabric:kspKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for ${mcData.version}")
    }
}
