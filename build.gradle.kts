import dev.deftu.gradle.utils.version.MinecraftVersions

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
    if (mcData.version == MinecraftVersions.VERSION_1_21_11) {
        // Temporarily disabled because of build failure on 1.21.11:
        // java.lang.IllegalStateException: Javadoc provided by mod (fabric-content-registries-v0) must be have an intermediary source namespace
        enableModProvidedJavadoc = false

        // Some stuff were made private / package-private in 1.21.11, so we need this.
        accessWidenerPath = file("src/main/resources/sbo-kotlin.accesswidener")
    }
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

tasks.named<ProcessResources>("processResources") {
    val fabricLoaderVersion = mcData.dependencies.fabric.fabricLoaderVersion
    val fabricApiVersion = mcData.dependencies.fabric.fabricApiVersion
    val fabricLanguageKotlinVersion = mcData.dependencies.fabric.fabricLanguageKotlinVersion
    val javaVersionMajor = mcData.version.javaVersion.majorVersion

    inputs.property("fabric_loader_version", fabricLoaderVersion)
    inputs.property("fabric_api_version", fabricApiVersion)
    inputs.property("fabric_language_kotlin_version", fabricLanguageKotlinVersion)
    inputs.property("java_version_major", javaVersionMajor)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "fabric_loader_version" to fabricLoaderVersion,
                "fabric_api_version" to fabricApiVersion,
                "fabric_language_kotlin_version" to fabricLanguageKotlinVersion,
                "java_version_major" to javaVersionMajor
            ) + inputs.properties
        )
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    modImplementation(include("gg.essential:elementa:${property("elementa.version")}")!!)
    modImplementation(include("net.azureaaron:hm-api:${property("hmapi.version")}")!!)

    // modImplementation(include("xyz.meowing:vexel-${mcData}:${property("vexel.version")}")!!)
    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.11")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.11:${property("rconfig.version.1.21.11")}")!!)
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.11:${property("rconfigkt.version.1.21.11")}")!!)
            modImplementation(include("gg.essential:universalcraft-1.21.11-fabric:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.11")}+1.21.11-fabric")
        }
        MinecraftVersions.VERSION_1_21_10 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.10")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${property("rconfig.version.1.21.10")}")!!) // .9 works on .10
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${property("rconfigkt.version.1.21.5")}")!!)  // .5 works on .10
            modImplementation(include("gg.essential:universalcraft-1.21.9-fabric:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.10")}+1.21.10-fabric")
        }
        MinecraftVersions.VERSION_1_21_7 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.7")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${mcData.version}:${property("rconfig.version.1.21.7")}")!!)
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${property("rconfigkt.version.1.21.5")}")!!)  // .5 works on .7
            modImplementation(include("gg.essential:universalcraft-$mcData:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.7")}+1.21.8-fabric") // .8 is a bugfix version so itll work on .7
        }
        MinecraftVersions.VERSION_1_21_5 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.5")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${mcData.version}:${property("rconfig.version.1.21.5")}")!!)
            modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${property("rconfigkt.version.1.21.5")}")!!)
            modImplementation(include("gg.essential:universalcraft-$mcData:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.5")}+1.21.5-fabric")
        }
        else -> {}
    }

    implementation(project(":event-processor"))
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> dependsOn(":1.21.10-fabric:kspKotlin")
        MinecraftVersions.VERSION_1_21_10 -> dependsOn(":1.21.7-fabric:kspKotlin")
        MinecraftVersions.VERSION_1_21_7 -> dependsOn(":1.21.5-fabric:kspKotlin")
        else -> {}
    }
}
