import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.lang.module.ModuleDescriptor.Version
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSetContainer

@CacheableTask
abstract class ReplaceMinecraftSources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    // Flat alternating token/replacement values keep the full ordered replacement list
    // in Gradle's task inputs, so changes invalidate the task and its build-cache entry.
    @get:Input
    abstract val replacements: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun replaceSources() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()

        val pairs = replacements.get().chunked(2).map { it[0] to it[1] }
        val sourceTrees = sources.asFileTree
        sourceTrees.visit(object : FileVisitor {
            override fun visitDir(details: FileVisitDetails) = Unit

            override fun visitFile(details: FileVisitDetails) {
                val destination = output.resolve(details.relativePath.pathString)
                destination.parentFile.mkdirs()
                val content = details.file.readText(Charsets.UTF_8)
                val replaced = pairs.fold(content) { text, (token, replacement) -> text.replace(token, replacement) }
                destination.writeText(replaced, Charsets.UTF_8)
            }
        })
    }
}

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.4.21"
    alias(libs.plugins.loom)
    id("dev.deftu.gradle.multiversion")
    alias(libs.plugins.ksp)
}

private val projectVersion = project.property("mod.version")?.toString() ?: throw AssertionError("missing mod version property")

// Sets the artifact version
version = projectVersion

private val mcProject: String = project.name
private val mcVersion: String = mcProject.replace("-fabric", "")

private fun versionedProperty(name: String, fallbackName: String? = null): String =
    project.findProperty("${name}.${mcVersion}")?.toString()
        ?: project.findProperty(name)?.toString()
        ?: fallbackName?.let { versionedProperty(it) }
        ?: throw AssertionError("build.gradle.kts needs updating for $mcProject")

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

private val versionedReplacements = buildList {
    if (isMCVersionGreaterOrEqualTo("26.3")) {
        addAll(listOf(
            "com.mojang.blaze3d.pipeline.RenderPipeline" to "com.mojang.renderpearl.api.pipeline.RenderPipeline",
            "com.mojang.blaze3d.pipeline.DepthStencilState" to "com.mojang.renderpearl.api.pipeline.DepthStencilState",
            "com.mojang.blaze3d.platform.CompareOp" to "com.mojang.renderpearl.api.pipeline.CompareOp",
            "Util.getPlatform().openUri(url)" to "com.mojang.blaze3d.Blaze3D.openUri(java.net.URI(url))",
            "Util.getPlatform().openPath(" to "com.mojang.blaze3d.Blaze3D.openPath(",
            "Util.getPlatform().openFile(folder)" to "com.mojang.blaze3d.Blaze3D.openPath(folder.toPath())",
            // The legacy packet constructor copies particle speed to all three axes in 26.3.
            ".maxSpeed" to ".xMaxSpeed()",
            "InputConstants.Type.KEYSYM" to "InputConstants.Type.KEYBOARD",
            "import org.lwjgl.glfw.GLFW;" to "",
            "import org.lwjgl.glfw.GLFW" to "",
            "InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), GLFW.GLFW_CURSOR_NORMAL," to "InputConstants.releaseMouse(this.minecraft.getWindow(),",
            "GLFW.GLFW_KEY_UNKNOWN" to "com.mojang.blaze3d.platform.InputConstants.UNKNOWN.value",
            "GLFW.GLFW_KEY_ESCAPE" to "com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE",
            "GLFW.GLFW_KEY_UP" to "com.mojang.blaze3d.platform.InputConstants.KEY_UP",
            "GLFW.GLFW_KEY_DOWN" to "com.mojang.blaze3d.platform.InputConstants.KEY_DOWN",
            "GLFW.GLFW_KEY_LEFT" to "com.mojang.blaze3d.platform.InputConstants.KEY_LEFT",
            "GLFW.GLFW_KEY_RIGHT" to "com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT",
            "GLFW.GLFW_KEY_EQUAL" to "com.mojang.blaze3d.platform.InputConstants.KEY_EQUALS",
            "GLFW.GLFW_KEY_KP_ADD" to "com.mojang.blaze3d.platform.InputConstants.KEY_ADD",
            "GLFW.GLFW_KEY_MINUS" to "com.mojang.blaze3d.platform.InputConstants.KEY_MINUS",
            "GLFW.GLFW_KEY_KP_SUBTRACT" to "org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS",
            "click.buttonInfo().button" to "net.sbo.mod.utils.InputCompatibility.mouseButton(click.buttonInfo().button)",
            "mulPose(Axis.YP.rotationDegrees(-cameraYaw))" to "rotateDegrees(Axis.YP, -cameraYaw)",
            "mulPose(Axis.XP.rotationDegrees(cameraPitch))" to "rotateDegrees(Axis.XP, cameraPitch)",
            "DefaultTooltipPositioner.INSTANCE, null)" to "DefaultTooltipPositioner.INSTANCE, null, false)"
        ))
    }
    if (isMCVersionGreaterOrEqualTo("26.2")) {
        addAll(listOf(
            "mc.screen" to "mc.gui.screen()",
            "mc.setScreen(" to "mc.gui.setScreen(",
            "mc.toastManager" to "mc.gui.toastManager()",
            "mc.gui.setTimes" to "mc.gui.hud.setTimes",
            "mc.gui.setTitle" to "mc.gui.hud.setTitle",
            "mc.gui.setSubtitle" to "mc.gui.hud.setSubtitle",
            "mc.gui.titleTime" to "mc.gui.hud.titleTime",
            "mc.gui.title" to "mc.gui.hud.title",
            "mc.gui.subtitle" to "mc.gui.hud.subtitle",
            "mc.gui.tabList" to "mc.gui.hud.tabList",
            "SystemToast.multiline(mc, " to "SystemToast(",
            "mc.gui.chat.addClientSystemMessage" to "mc.gui.hud.chat.addClientSystemMessage",
            "formatting?.char" to "formatting?.code",
            "mc.options.hideGui" to "mc.gui.hud.isHidden()",
            "gameRenderer().mainCamera" to "gameRenderer().mainCamera()"
        ))
    }
}.flatMap { (token, replacement) -> listOf(token, replacement) }

val mainSourceSet = extensions.getByType<SourceSetContainer>().named("main")
val mainKotlinSources = mainSourceSet.get().extensions.getByName("kotlin") as SourceDirectorySet

fun registerSourceReplacement(name: String, sources: SourceDirectorySet, outputPath: String) = tasks.register<ReplaceMinecraftSources>(name) {
    this.sources.from(sources)
    replacements.set(versionedReplacements)
    outputDirectory.set(layout.buildDirectory.dir(outputPath))
}

val replaceJavaSources = registerSourceReplacement("replaceMinecraftJavaSources", mainSourceSet.get().java, "generated/bloom/main/java")
val replaceKotlinSources = registerSourceReplacement("replaceMinecraftKotlinSources", mainKotlinSources, "generated/bloom/main/kotlin")

tasks.named<JavaCompile>("compileJava") {
    dependsOn(replaceJavaSources)
    setSource(replaceJavaSources.map { it.outputDirectory })
}
tasks.named<KotlinJvmCompile>("compileKotlin") {
    dependsOn(replaceKotlinSources)
    setSource(replaceKotlinSources.map { it.outputDirectory })
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
        forRepository {
            maven("https://maven.azureaaron.net/releases")
        }

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
            includeModule("maven.modrinth", "sodium")
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
    val fabricLoaderConstraint = project.property("fabricloader.constraint")

    val fabricApiVersion = versionedProperty("fabricapi.version")
    val fabricApiConstraint = versionedProperty("fabricapi.version.constraint", "fabricapi.version")
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

    // TODO: remove when dropping 26.1 support
    val sodiumExtraConstraint = versionedProperty("sodium.extra.breaks.constraint")

    inputs.property("mod_name", modName)
    inputs.property("mod_description", modDescription)
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)
    inputs.property("mod_group", modGroup)

    inputs.property("mc_version_constraint", mcVersionConstraint)

    inputs.property("fabric_loader_version", fabricLoaderVersion)
    inputs.property("fabric_loader_constraint", fabricLoaderConstraint)
    inputs.property("fabric_api_version", fabricApiVersion)
    inputs.property("fabric_api_constraint", fabricApiConstraint)
    inputs.property("fabric_language_kotlin_version", fabricLanguageKotlinVersion)
    inputs.property("java_version_major", javaVersionMajor)

    inputs.property("hm_api_version", hmApiVersion)
    inputs.property("guilib_version", project.property("guilib.version"))
    inputs.property("modmenu_version", modMenuVersion)

    inputs.property("sodium_extra_breaks_constraint", sodiumExtraConstraint)

    val expandProperties = mapOf(
        "expanded_files" to expandedFiles,

        "mod_name" to modName,
        "mod_description" to modDescription,
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_group" to modGroup,
        "mc_version" to mcVersion,

        "fabric_loader_version" to fabricLoaderVersion,
        "fabric_loader_constraint" to fabricLoaderConstraint,
        "fabric_api_version" to fabricApiVersion,
        "fabric_api_constraint" to fabricApiConstraint,
        "fabric_language_kotlin_version" to fabricLanguageKotlinVersion,
        "java_version_major" to javaVersionMajor,

        "hm_api_version" to hmApiVersion,
        "guilib_version" to project.property("guilib.version"),
        "modmenu_version" to modMenuVersion,

        "sodium_extra_breaks_constraint" to sodiumExtraConstraint
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

    // GuiLib (see settings.gradle.kts for building it from a local checkout)
    implementation(include("net.sbo:guilib-$mcProject:${property("guilib.version")}")!!)

    implementation(include("net.azureaaron:hm-api:${versionedProperty("hmapi.version")}")!!)
    implementation("com.terraformersmc:modmenu:${versionedProperty("modmenu.version")}")

    implementation(include("com.github.trilarion:java-vorbis-support:${property("vorbis.version")}")!!)
    implementation(include("com.googlecode.soundlibs:jlayer:${property("jlayer.version")}")!!)

    when (mcProject) {
        "26.3-fabric" -> {
            implementation(include("net.azureaaron:render-chest:${versionedProperty("renderchest.version")}")!!)
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.3-fabric")
            compileOnly("maven.modrinth:sodium:mc26.3-${versionedProperty("sodium.version")}-fabric")
        }
        "26.2-fabric" -> {
            // TODO Move out of conditional block when dropping 26.1.2 support, add it to fabric.mod.json dependencies and remove the legacy glow of ours (remove EntityMixin, EntityAccessor and clean up RareMobHighlight)
            implementation(include("net.azureaaron:render-chest:${versionedProperty("renderchest.version")}")!!)

            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.2-fabric")
            compileOnly("maven.modrinth:sodium:mc26.2-${versionedProperty("sodium.version")}-fabric")
        }
        "26.1.2-fabric" -> {
            compileOnly("maven.modrinth:iris:${versionedProperty("iris.version")}+26.1-fabric")
            compileOnly("maven.modrinth:sodium:mc26.1.2-${versionedProperty("sodium.version")}-fabric")
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

