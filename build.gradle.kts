import java.nio.charset.StandardCharsets
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import net.fabricmc.loom.task.RemapJarTask

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.21"
    id("gg.essential.loom") version "1.15.50"
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools")
    id("dev.deftu.gradle.tools.bloom")
    id("com.google.devtools.ksp") version "2.3.7"
}

loom {
    // Temporarily disabled because of build failure on latest versions:
    // java.lang.IllegalStateException: Javadoc provided by mod (fabric-content-registries-v0) must be have an intermediary source namespace
    enableModProvidedJavadoc = false

    // Some stuff were made private / package-private in later versions, so we need this.
    accessWidenerPath = file("src/main/resources/sbo-kotlin.accesswidener")
}

bloom {
    if ("26.1-fabric" == project.name) {
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
        replacement("method = \"render\"", "method = \"extractRenderState\"")

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
    }
}

private fun versionedProperty(name: String): String {
    return project.property("${name}.${project.name.replace("-fabric", "")}")?.toString() ?: throw AssertionError("build.gradle.kts needs updating for ${project.name}")
}

kotlin {
    // This improves build performance as it supports incremental compilation among other things with the BTA API
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion = "2.3.0"
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.essential.gg/repository/maven-public")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.azureaaron.net/releases")
    maven("https://api.modrinth.com/maven")
}

afterEvaluate {
    val newBuildDestinationDirectory by lazy {
        rootProject.layout.buildDirectory.asFile.get().resolve("versions")
    }

    tasks {
        jar {
            destinationDirectory.set(newBuildDestinationDirectory)
        }

        if ("26.1-fabric" != project.name) {
            named<RemapJarTask>("remapJar") {
                destinationDirectory.set(newBuildDestinationDirectory)
            }
        }
    }
}

tasks.withType<JavaCompile> {
  options.release = Integer.parseInt(versionedProperty("java.version"))
  options.encoding = StandardCharsets.UTF_8.toString()
}

tasks.withType<AbstractArchiveTask> {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}

tasks.matching { it.name.contains("Test") }.configureEach {
    // One of the tasks create problems since preprocessTestCode reads output of kspTestKotlin without depending on it
    // We don't have any tests anyway; so this OK to disable to workaround the error.
    enabled = false
}

tasks.named<ProcessResources>("processResources") {
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

    val mcVersion = project.name.replace("-fabric", "")

    inputs.property("mod_name", modName)
    inputs.property("mod_description", modDescription)
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("mod_group", modGroup)

    inputs.property("mc_version", mcVersion)

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

    filesMatching(
        listOf(
            "fabric.mod.json",
            "sbo-kotlin.mixins.json"
        )
    ) {
        expand(expandProperties)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.name.replace("-fabric", "")}")

    val mappingsConfig = configurations.findByName("mappings")

    if (null != mappingsConfig) {
        dependencies.add(
            mappingsConfig.name,
            loom.officialMojangMappings()
        )
    }

    implementation("net.fabricmc:fabric-loader:${property("fabricloader.version")}")

    implementation("net.fabricmc:fabric-language-kotlin:${property("fabriclanguagekotlin.version")}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    implementation(include("gg.essential:elementa:${property("elementa.version")}")!!)

    val modImplementationConfig = configurations.findByName("modImplementation")

    fun DependencyHandler.maybeModImplementation(dependencyNotation: Any) {
        if (null != modImplementationConfig) {
            add(modImplementationConfig.name, dependencyNotation)
        } else {
            implementation(dependencyNotation)
        }
    }

    when (project.name) {
        "26.1-fabric" -> {
            implementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
            implementation("net.fabricmc.fabric-api:fabric-api:${versionedProperty("fabricapi.version")}")
            implementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")
            implementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-26.1:${versionedProperty("rconfig.version")}")!!)
            implementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-26.1-rc-1:${versionedProperty("rconfigkt.version")}")!!)
            implementation(include("gg.essential:universalcraft-26.1-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.1-fabric")
        }
        "1.21.11-fabric" -> {
            maybeModImplementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
            maybeModImplementation("net.fabricmc.fabric-api:fabric-api:${versionedProperty("fabricapi.version")}")
            maybeModImplementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")
            maybeModImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.11:${versionedProperty("rconfig.version")}")!!)
            maybeModImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.11:${versionedProperty("rconfigkt.version")}")!!)
            maybeModImplementation(include("gg.essential:universalcraft-1.21.11-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+1.21.11-fabric")
        }
        "1.21.10-fabric" -> {
            maybeModImplementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
            maybeModImplementation("net.fabricmc.fabric-api:fabric-api:${versionedProperty("fabricapi.version")}")
            maybeModImplementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")
            maybeModImplementation(include("com.teamresourceful.resourcefulconfig:resourcefulconfig-fabric-1.21.9:${versionedProperty("rconfig.version")}")!!) // .9 works on .10
            maybeModImplementation(include("com.teamresourceful.resourcefulconfigkt:resourcefulconfigkt-fabric-1.21.5:${versionedProperty("rconfigkt.version")}")!!)  // .5 works on .10
            maybeModImplementation(include("gg.essential:universalcraft-1.21.9-fabric:${property("universalcraft.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+1.21.10-fabric")
        }
        else -> throw AssertionError("build.gradle.kts needs updating for ${project.name}")
    }

    implementation(project(":event-processor"))
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (project.name) {
        "26.1-fabric" -> dependsOn(":1.21.11-fabric:kspKotlin")
        "1.21.11-fabric" -> dependsOn(":1.21.10-fabric:kspKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for ${project.name}")
    }
}
