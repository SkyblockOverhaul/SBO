import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.lang.module.ModuleDescriptor.Version

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.4.0"
    id("net.fabricmc.fabric-loom-remap") version "1.17.12"
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools.bloom")
    id("com.google.devtools.ksp") version "2.3.9"
}

private val mcProject: String = project.name
private val mcVersion: String = mcProject.replace("-fabric", "")

private fun versionedProperty(name: String): String = project.property("${name}.${mcVersion}")?.toString() ?: throw AssertionError("build.gradle.kts needs updating for $mcProject")

private fun isUnobfuscatedMCVersion(): Boolean = isMCVersionGreaterOrEqualTo("26.1")

private fun isMCVersionGreaterOrEqualTo(version: String): Boolean = Version.parse(mcVersion) >= Version.parse(version)

loom {
    // Some stuff were made private / package-private in later versions, so we need this.
    accessWidenerPath = file("src/main/resources/sbo.classtweaker")

    runs.configureEach {
        generateRunConfig.set(true)
    }
}

bloom {
    if (isMCVersionGreaterOrEqualTo("26.1")) {
        // GuiGraphics --> GuiGraphicsExtractor
        replacement("GuiGraphics", "GuiGraphicsExtractor")

        // KeyBinding --> KeyMapping
        replacement("net.fabricmc.fabric.api.client.keybinding", "net.fabricmc.fabric.api.client.keymapping")
        replacement("KeyBindingHelper", "KeyMappingHelper")
        replacement("registerKeyBinding", "registerKeyMapping")

        // ClientCommandManager --> ClientCommands
        replacement("ClientCommandManager", "ClientCommands")

        // World --> Level
        replacement("net.fabricmc.fabric.api.client.rendering.v1.world", "net.fabricmc.fabric.api.client.rendering.v1.level")
        replacement("ClientWorldEvents", "ClientLevelEvents")
        replacement("WorldRenderEvents", "LevelRenderEvents")
        replacement("WorldRenderContext", "LevelRenderContext")
        replacement("AFTER_CLIENT_WORLD_CHANGE", "AFTER_CLIENT_LEVEL_CHANGE")

        // Render --> Extract
        replacement("ScreenEvents.afterRender", "ScreenEvents.afterExtract")
        replacement("override fun render(", "override fun extractRenderState(")
        replacement("super.render(", "super.extractRenderState(")
        replacement("this.renderMenuBackground", "this.extractMenuBackground")
        replacement("drawContext.renderTooltip(", "drawContext.tooltip(")

        // Render --> Extract (Hacky way to fix Mixins, too lazy for versioned Mixins)
        replacement("@Inject(method = \"render\", at = @At(\"HEAD\"))", "@Inject(method = \"extractRenderState\", at = @At(\"HEAD\"))")
        replacement("Lnet/minecraft/client/gui/Gui;render(", "Lnet/minecraft/client/gui/Gui;extractRenderState(")
        replacement("@Inject(method = \"render\", at = @At(value = \"INVOKE\", target = ", "@Inject(method = \"extractGui\", at = @At(value = \"INVOKE\", target = ")
        replacement("private void afterHudRender(@NonNull DeltaTracker tickCounter, boolean tick, ", "private void afterHudRender(@NonNull DeltaTracker tickCounter, boolean tick, boolean resourcesLoaded, ")

        // drawString --> text
        replacement("drawContext.drawString(", "drawContext.text(")

        // renderOutline --> outline
        replacement("drawContext.renderOutline(", "drawContext.outline(")

        // consumers --> bufferSource
        replacement("context.consumers", "context.bufferSource")
        replacement("ctx.consumers", "ctx.bufferSource")

        // matrices --> poseStack
        replacement("matrices()", "poseStack()")

        // BEFORE_TRANSLUCENT --> BEFORE_TRANSLUCENT_TERRAIN
        replacement("BEFORE_TRANSLUCENT", "BEFORE_TRANSLUCENT_TERRAIN")
        replacement("BeforeTranslucent", "BeforeTranslucentTerrain")
        replacement("override fun beforeTranslucent(", "override fun beforeTranslucentTerrain(")

        // addMessage --> addClientSystemMessage
        replacement("chat.addMessage(", "chat.addClientSystemMessage(")
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
        "26.1.2-fabric" -> {
            implementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-26.1:${versionedProperty("rconfig.version")}")!!)
            implementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-26.1-rc-1:${versionedProperty("rconfigkt.version")}")!!)
            implementation(include("gg.essential:universalcraft-26.1-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.1-fabric")
        }
        "1.21.11-fabric" -> {
            maybeModImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.11:${versionedProperty("rconfig.version")}")!!)
            maybeModImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.11:${versionedProperty("rconfigkt.version")}")!!)
            maybeModImplementation(include("gg.essential:universalcraft-1.21.11-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+1.21.11-fabric")
        }
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (mcProject) {
        "26.1.2-fabric" -> dependsOn(":1.21.11-fabric:kspKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }
}
