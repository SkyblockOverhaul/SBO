import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.lang.module.ModuleDescriptor.Version
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.4.21"
    alias(libs.plugins.loom)
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools.bloom")
    alias(libs.plugins.ksp)
}

private val projectVersion = project.property("mod.version")?.toString() ?: throw AssertionError("missing mod version property")

// Sets the artifact version
version = projectVersion

private val mcProject: String = project.name
private val mcVersion: String = mcProject.replace("-fabric", "")

private fun versionedProperty(name: String): String = project.property("${name}.${mcVersion}")?.toString() ?: throw AssertionError("build.gradle.kts needs updating for $mcProject")

private fun isMCVersionGreaterOrEqualTo(version: String): Boolean = Version.parse(mcVersion) >= Version.parse(version)

loom {
    // Some stuff were made private / package-private in later versions, so we need this.
    accessWidenerPath = file("src/main/resources/sbo.classtweaker")

    runs.configureEach {
        generateRunConfig.set(true)
        preferGradleTask = true
    }

    uncompressNestedJars = true
}

bloom {
    if (isMCVersionGreaterOrEqualTo("26.3")) {
        replacement("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
        replacement("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState")
        replacement("com.mojang.blaze3d.platform.CompareOp", "com.mojang.renderpearl.api.pipeline.CompareOp")
        replacement("Util.getPlatform().openUri(url)", "com.mojang.blaze3d.Blaze3D.openUri(java.net.URI(url))")
        replacement("Util.getPlatform().openPath(", "com.mojang.blaze3d.Blaze3D.openPath(")
        replacement("Util.getPlatform().openFile(folder)", "com.mojang.blaze3d.Blaze3D.openPath(folder.toPath())")
        // Hypixel's legacy particle speed is copied to all three axes by the 26.3 packet constructor.
        replacement(".maxSpeed", ".xMaxSpeed()")
        replacement("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
        replacement("import org.lwjgl.glfw.GLFW", "")
        replacement("GLFW.GLFW_KEY_UNKNOWN", "com.mojang.blaze3d.platform.InputConstants.UNKNOWN.value")
        // Elementa delivers UniversalCraft keycodes; native Screen events deliver physical key IDs.
        replacement("GLFW.GLFW_KEY_BACKSPACE", "gg.essential.universal.UKeyboard.KEY_BACKSPACE")
        replacement("GLFW.GLFW_KEY_DELETE", "gg.essential.universal.UKeyboard.KEY_DELETE")
        replacement("GLFW.GLFW_KEY_UP", "com.mojang.blaze3d.platform.InputConstants.KEY_UP")
        replacement("GLFW.GLFW_KEY_DOWN", "com.mojang.blaze3d.platform.InputConstants.KEY_DOWN")
        replacement("GLFW.GLFW_KEY_LEFT", "com.mojang.blaze3d.platform.InputConstants.KEY_LEFT")
        replacement("GLFW.GLFW_KEY_RIGHT", "com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT")
        replacement("GLFW.GLFW_KEY_ESCAPE", "com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE")
        replacement("InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL, ", "InputConstants.releaseMouse(this.minecraft.getWindow(), ")
        replacement("GLFW.GLFW_KEY_EQUAL", "com.mojang.blaze3d.platform.InputConstants.KEY_EQUALS")
        replacement("GLFW.GLFW_KEY_KP_ADD", "com.mojang.blaze3d.platform.InputConstants.KEY_ADD")
        replacement("GLFW.GLFW_KEY_MINUS", "com.mojang.blaze3d.platform.InputConstants.KEY_MINUS")
        replacement("GLFW.GLFW_KEY_KP_SUBTRACT", "org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS")
        replacement("click.buttonInfo().button", "net.sbo.mod.utils.InputCompatibility.mouseButton(click.buttonInfo().button)")
        replacement("mulPose(Axis.YP.rotationDegrees(-cameraYaw))", "rotateDegrees(Axis.YP, -cameraYaw)")
        replacement("mulPose(Axis.XP.rotationDegrees(cameraPitch))", "rotateDegrees(Axis.XP, cameraPitch)")
        replacement("DefaultTooltipPositioner.INSTANCE, null)", "DefaultTooltipPositioner.INSTANCE, null, false)")
    }
    if (isMCVersionGreaterOrEqualTo("26.2")) {
        replacement("mc.screen", "mc.gui.screen()")
        replacement("mc.setScreen(", "mc.gui.setScreen(")
        replacement("mc.toastManager", "mc.gui.toastManager()")
        replacement("mc.gui.setTimes", "mc.gui.hud.setTimes")
        replacement("mc.gui.setTitle", "mc.gui.hud.setTitle")
        replacement("mc.gui.setSubtitle", "mc.gui.hud.setSubtitle")
        replacement("mc.gui.titleTime", "mc.gui.hud.titleTime")
        replacement("mc.gui.title", "mc.gui.hud.title")
        replacement("mc.gui.subtitle", "mc.gui.hud.subtitle")
        replacement("SystemToast.multiline(mc, ", "SystemToast(")
        replacement("mc.gui.chat.addClientSystemMessage", "mc.gui.hud.chat.addClientSystemMessage")
        replacement("formatting?.char", "formatting?.code")
        replacement("mc.options.hideGui", "mc.gui.hud.isHidden()")
        replacement("gameRenderer().mainCamera", "gameRenderer().mainCamera()")
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

        moduleName.set("sbo-${projectVersion}-${mcVersion}") // default is project name which becomes e.g 1.21.11-fabric or 26.1.2-fabric without the sbo naming; The module name is used when generating the mangled name for internal visibility items and the .kotlin_module file in the META-INF directory.
    }
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
            maven("https://skyblockoverhaul.github.io/maven")
        }

        filter {
            includeGroup("net.sbo")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://maven.terraformersmc.com/releases")
            maven("https://maven.operationpotato.com/mirror")
        }

        filter {
            includeModule("com.terraformersmc", "modmenu")
        }
    }

    exclusiveContent {
        // Optional caller-supplied repository for publisher builds awaiting a Maven release.
        val portRepository = providers.gradleProperty("portDependencyRepository").orNull?.let { repository ->
            require(repository.isNotBlank()) { "portDependencyRepository must be a repository URI or path" }
            maven(rootProject.uri(repository))
        }
        val publisherRepository = maven("https://maven.azureaaron.net/releases")
        forRepositories(*listOfNotNull(portRepository, publisherRepository).toTypedArray())

        filter {
            includeModule("net.azureaaron", "hm-api")
            includeModule("net.azureaaron", "render-chest")
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
            archiveVersion.set("")
        }
    }
}

tasks.withType<JavaCompile> {
  options.release = Integer.parseInt(versionedProperty("java.version"))
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(versionedProperty("java.version")))
}

val runDirectory = rootProject.file("run")
runDirectory.mkdirs()

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStackTraces = true
        //showStandardStreams = true // enable if troubleshooting failures
        exceptionFormat = TestExceptionFormat.FULL
    }
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    workingDir(file(runDirectory))
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        // Tests start NPE-ing without this on Java 25
        "-Dnet.bytebuddy.experimental=true",
    )
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_26)) {
        jvmArgs(
            // Resolves warning: "Final field mappings in class org.spongepowered.asm.mixin.refmap.ReferenceMapper has been mutated reflectively by class org.spongepowered.include.com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1 in unnamed module"
            // Ideally Mixin would make the field not final or use a different GSON serilization path, but here we are
            "--enable-final-field-mutation=ALL-UNNAMED",
        )
    }
}

val mixinTestRuntime = configurations.create("mixinTestRuntime") {
    isCanBeConsumed = false
    extendsFrom(configurations.testRuntimeClasspath.get())
}

val mixinTest = tasks.register<Test>("mixinTest") {
    description = "Audits mixin application under Fabric Loader."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().output + sourceSets.main.get().output + mixinTestRuntime
    filter {
        includeTestsMatching("net.sbo.mod.test.MixinTest")
    }
}

tasks.test {
    dependsOn(mixinTest)
    exclude("net/sbo/mod/test/MixinTest.class")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Sealed" to "true",
            "Automatic-Module-Name" to "sbo",
            "Specification-Title" to "SBO",
            "Specification-Version" to project.version.toString(),
            "Specification-Vendor" to "SBO",
            "Implementation-Title" to "SBO",
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to "SBO"
        )
    }

    from(rootProject.file("LICENSE")) {
        rename { "LICENSE_sbo" }
    }
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

    val hmApiVersion = versionedProperty("hmapi.version")
    val modMenuVersion = versionedProperty("modmenu.version")

    val modName = project.property("mod.name")
    val modDescription = project.property("mod.description")
    val modId = project.property("mod.id")
    val modVersion = project.property("mod.version")
    val modGroup = project.property("mod.group")

    val mcVersionConstraint = project.findProperty("mc$mcVersion.constraint")?.toString() ?: "~$mcVersion"

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

    inputs.property("hm_api_version", hmApiVersion)
    inputs.property("guilib_version", project.property("guilib.version"))
    inputs.property("modmenu_version", modMenuVersion)

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

        "hm_api_version" to hmApiVersion,
        "guilib_version" to project.property("guilib.version"),
        "modmenu_version" to modMenuVersion,
    ) + inputs.properties

    filesMatching(expandedFiles) {
        expand(expandProperties)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")

    implementation("net.fabricmc:fabric-loader:${property("fabricloader.version")}")

    mixinTestRuntime("net.fabricmc:fabric-loader-junit:${property("fabricloader.version")}")
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.launcher)

    implementation("net.fabricmc.fabric-api:fabric-api:${versionedProperty("fabricapi.version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabriclanguagekotlin.version")}")

    ksp(project(":event-processor"))
    ksp("dev.zacsweers.autoservice:auto-service-ksp:${property("autoservice.version")}")

    // GuiLib (see settings.gradle.kts for building it from a local checkout). Not used by any code yet.
    implementation(include("net.sbo:guilib-$mcProject:${property("guilib.version")}")!!)

    implementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
    implementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")

    implementation(include("com.github.trilarion:java-vorbis-support:${property("vorbis.version")}")!!)
    implementation(include("com.googlecode.soundlibs:jlayer:${property("jlayer.version")}")!!)

    when (mcProject) {
        "26.3-fabric" -> {
            implementation(include("net.azureaaron:render-chest:${versionedProperty("renderchest.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.3-fabric")
        }
        "26.2-fabric" -> {
            // TODO Move out of conditional block when dropping 26.1.2 support, add it to fabric.mod.json dependencies and remove the legacy glow of ours (remove EntityMixin, EntityAccessor and clean up RareMobHighlight)
            implementation(include("net.azureaaron:render-chest:${versionedProperty("renderchest.version")}")!!)

            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.2-fabric")
        }
        "26.1.2-fabric" -> {
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.1-fabric")
        }
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
}

tasks.findByName("preprocessCode")?.apply {
    when (mcProject) {
        "26.3-fabric" -> dependsOn(":26.2-fabric:kspKotlin")
        "26.2-fabric" -> dependsOn(":26.1.2-fabric:kspKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }
}

tasks.findByName("preprocessTestCode")?.apply {
    when (mcProject) {
        "26.3-fabric" -> dependsOn(":26.2-fabric:kspTestKotlin")
        "26.2-fabric" -> dependsOn(":26.1.2-fabric:kspTestKotlin")
        else -> throw AssertionError("build.gradle.kts needs updating for $mcProject")
    }
}
