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
    id("com.google.devtools.ksp") version "2.3.3"
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

dependencies {
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    modImplementation(include("gg.essential:elementa:${property("elementa.version")}")!!)
    modImplementation(include("net.azureaaron:hm-api:${property("hmapi.version")}")!!)
    modImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${property("rconfig.version.1.21.5")}")!!)

    // modImplementation(include("xyz.meowing:vexel-${mcData}:${property("vexel.version")}")!!)
    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_10 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:0.138.3+1.21.10")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.10")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${property("rconfig.version.1.21.10")}")!!)
            modImplementation(include("gg.essential:universalcraft-1.21.9-fabric:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.10")}+1.21.7-fabric")
        }
        MinecraftVersions.VERSION_1_21_7 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.7")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${mcData.version}:${property("rconfig.version.1.21.7")}")!!)
            modImplementation(include("gg.essential:universalcraft-$mcData:${property("uc.version")}")!!)
            compileOnly("maven.modrinth:iris:${property("iris.version.1.21.7")}+1.21.7-fabric")
        }
        MinecraftVersions.VERSION_1_21_5 -> {
            modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
            modImplementation("com.terraformersmc:modmenu:${property("modmenu.version.1.21.5")}")
            modImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-${mcData.version}:${property("rconfig.version.1.21.5")}")!!)
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
        MinecraftVersions.VERSION_1_21_10 -> dependsOn(":1.21.7-fabric:kspKotlin")
        MinecraftVersions.VERSION_1_21_7 -> dependsOn(":1.21.5-fabric:kspKotlin")
        else -> {}
    }
}
