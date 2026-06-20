import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.lang.module.ModuleDescriptor.Version

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.4.0"
    id("net.fabricmc.fabric-loom-remap") version "1.17.11"
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools.bloom")
    id("com.google.devtools.ksp") version "2.3.9"
}

private val mcProject: String = project.name
private val mcVersion: String = mcProject.replace("-fabric", "")

private fun versionedProperty(name: String): String {
    return project.property("${name}.${mcVersion}")?.toString() ?: throw AssertionError("build.gradle.kts needs updating for $mcProject")
}

private fun isUnobfuscatedMCVersion(): Boolean {
    return isMCVersionGreaterOrEqualTo("26.1")
}

private fun isMCVersionGreaterOrEqualTo(version: String): Boolean {
    return Version.parse(mcVersion) >= Version.parse(version)
}

loom {
    // Some stuff were made private / package-private in later versions, so we need this.
    accessWidenerPath = file("src/main/resources/sbo.classtweaker")

    runs.configureEach {
        generateRunConfig.set(true)
    }
}

bloom {
    if (isMCVersionGreaterOrEqualTo("26.2")) {
        replacement("mc.screen", "mc.gui.screen()")
        replacement("mc.setScreen(", "mc.gui.setScreen(")
        replacement("mc.toastManager", "mc.gui.toastManager()")
        replacement("mc.gui.setTimes", "mc.gui.hud.setTimes")
        replacement("mc.gui.setTitle", "mc.gui.hud.setTitle")
        replacement("mc.gui.setSubtitle", "mc.gui.hud.setSubtitle")
        replacement("SystemToast.multiline(mc, ", "SystemToast(")
        replacement("mc.gui.chat.addClientSystemMessage", "mc.gui.hud.chat.addClientSystemMessage")
        replacement("formatting?.char", "formatting?.code")
        replacement("mc.options.hideGui", "mc.gui.hud.isHidden()")
        replacement("gameRenderer().mainCamera", "gameRenderer().mainCamera()")
        replacement("com.mojang.blaze3d.vertex.VertexFormat.Mode", "com.mojang.blaze3d.PrimitiveTopology")
        replacement("net.minecraft.client.renderer.MultiBufferSource", "")
        replacement("MultiBufferSource", "VertexConsumer")
    }
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

        moduleName.set("sbo-${mcVersion}") // default is project name which becomes e.g 1.21.11-fabric or 26.1.2-fabric without the sbo naming; The module name is used when generating the mangled name for internal visibility items and the .kotlin_module file in the META-INF directory.
    }
}

kotlin {
    // This improves build performance as it supports incremental compilation among other things with the BTA API
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion = "2.3.0"
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
        }

        filter {
            includeGroup("me.djtheredstoner")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://repo.essential.gg/repository/maven-public")
        }

        filter {
            includeGroup("gg.essential")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://maven.teamresourceful.com/repository/maven-public/")
        }

        filter {
            includeGroup("com.teamresourceful.resourcefulconfig")
            includeGroup("com.teamresourceful.resourcefulconfigkt")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://maven.terraformersmc.com/")
        }

        filter {
            includeModule("com.terraformersmc", "modmenu")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://maven.azureaaron.net/releases")
        }

        filter {
            includeModule("net.azureaaron", "hm-api")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }

        filter {
            includeModule("maven.modrinth", "iris")
        }
    }
}

val jarName = project.property("mod.name").toString() + "-" + project.property("mod.version").toString() + "+" + mcProject

afterEvaluate {
    val newBuildDestinationDirectory by lazy {
        rootProject.layout.buildDirectory.asFile.get().resolve("versions")
    }

    tasks {
        jar {
            destinationDirectory.set(newBuildDestinationDirectory)
            archiveBaseName.set(jarName)
        }

        if (!isUnobfuscatedMCVersion()) {
            named<RemapJarTask>("remapJar") {
                destinationDirectory.set(newBuildDestinationDirectory)
                archiveBaseName.set(jarName)
            }
        }
    }
}

tasks.withType<JavaCompile> {
  options.release = Integer.parseInt(versionedProperty("java.version"))
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(versionedProperty("java.version")))
}

tasks.matching { it.name.contains("Test") || it.name.contains("test") }.configureEach {
    // One of the tasks create problems since preprocessTestCode reads output of kspTestKotlin without depending on it,
    // We don't have any tests anyway; so this OK to disable to work around the error.
    enabled = false
}

tasks.named<ProcessResources>("processResources") {
    val expandedFiles = listOf(
        "fabric.mod.json",
        "sbo.mixins.json"
    )

    inputs.property("expanded_files", expandedFiles)

    val fabricLoaderVersion = project.property("fabricloader.version")
    val fabricApiVersion = versionedProperty("fabricapi.version")
    val fabricLanguageKotlinVersion = project.property("fabriclanguagekotlin.version")
    val javaVersionMajor = Integer.parseInt(versionedProperty("java.version"))

    val elementaVersion = project.property("elementa.version")
    val hmApiVersion = versionedProperty("hmapi.version")
    val resourcefulConfigVersion = versionedProperty("rconfig.version")
    val resourcefulConfigKtVersion = versionedProperty("rconfigkt.version")
    val universalCraftVersion = project.property("universalcraft.version")

    val modName = project.property("mod.name")
    val modDescription = project.property("mod.description")
    val modId = project.property("mod.id")
    val modVersion = project.property("mod.version")
    val modGroup = project.property("mod.group")

    // 26.1 onwards switched back to proper SemVer so we use ~ to accept compatible patch updates; for older versions do an exact requirement as even patch level version updates (e.g 1.21.10 to 1.21.11) are incompatible with each other.
    val mcVersionConstraint = if (isMCVersionGreaterOrEqualTo("26.1")) "~$mcVersion" else "=$mcVersion"

    inputs.property("mod_name", modName)
    inputs.property("mod_description", modDescription)
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("mod_group", modGroup)

    inputs.property("mc_version_constraint", mcVersionConstraint)

    inputs.property("fabric_loader_version", fabricLoaderVersion)
    inputs.property("fabric_api_version", fabricApiVersion)
    inputs.property("fabric_language_kotlin_version", fabricLanguageKotlinVersion)
    inputs.property("java_version_major", javaVersionMajor)

    inputs.property("elementa_version", elementaVersion)
    inputs.property("hm_api_version", hmApiVersion)
    inputs.property("resourcefulconfig_version", resourcefulConfigVersion)
    inputs.property("resourcefulconfigkt_version", resourcefulConfigKtVersion)
    inputs.property("universalcraft_version", universalCraftVersion)

    val expandProperties = mapOf(
        "expanded_files" to expandedFiles,

        "mod_name" to modName,
        "mod_description" to modDescription,
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_group" to modGroup,
        "mc_version" to mcVersion,

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

    filesMatching(expandedFiles) {
        expand(expandProperties)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")

    val mappingsConfig = configurations.findByName("mappings")

    if (null != mappingsConfig) {
        dependencies.add(
            mappingsConfig.name,
            loom.officialMojangMappings()
        )
    }

    val modImplementationConfig = configurations.findByName("modImplementation")

    fun DependencyHandler.maybeModImplementation(dependencyNotation: Any) {
        if (null != modImplementationConfig) {
            add(modImplementationConfig.name, dependencyNotation)
        } else {
            implementation(dependencyNotation)
        }
    }

    maybeModImplementation("net.fabricmc:fabric-loader:${property("fabricloader.version")}")
    maybeModImplementation("net.fabricmc.fabric-api:fabric-api:${versionedProperty("fabricapi.version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabriclanguagekotlin.version")}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    implementation(include("gg.essential:elementa:${property("elementa.version")}")!!)

    maybeModImplementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
    maybeModImplementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")

    when (mcProject) {
        "26.2-fabric" -> {
            implementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-26.2:${versionedProperty("rconfig.version")}")!!)
            implementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-26.1-rc-1:${versionedProperty("rconfigkt.version")}")!!)
            implementation(include("gg.essential:universalcraft-26.2-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.2-fabric")
        }
        "26.1.2-fabric" -> {
            implementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-26.1:${versionedProperty("rconfig.version")}")!!)
            implementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-26.1-rc-1:${versionedProperty("rconfigkt.version")}")!!)
            implementation(include("gg.essential:universalcraft-26.1-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.1-fabric")
        }
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (mcProject) {
        "26.2-fabric" -> dependsOn(":26.1.2-fabric:kspKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }
}
