package buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Applies the shared build behavior without adding buildSrc to every project's classpath. */
public final class SboBuildLogicPlugin implements Plugin<Project> {
    @Override
    public final void apply(final Project project) {
        final var enabled = project.getProviders().gradleProperty("enableTests")
                .map(Boolean::parseBoolean).getOrElse(false);
        final var explicitlyRequested = project.getGradle().getStartParameter().getTaskNames().stream()
                .anyMatch(name -> !name.substring(name.lastIndexOf(':') + 1).equalsIgnoreCase("mixinTest")
                        && Pattern.compile("(^|:)[A-Za-z0-9_-]*(test|check)$", Pattern.CASE_INSENSITIVE)
                                .matcher(name).matches());
        final var infrastructureExplicitlyRequested = project.getGradle().getStartParameter().getTaskNames().stream()
                .map(name -> name.substring(name.lastIndexOf(':') + 1))
                .anyMatch(Set.of("kspTestKotlin", "preprocessTestCode", "compileTestKotlin",
                        "compileTestJava", "preprocessTestResources", "processTestResources", "testClasses")::contains);
        project.afterEvaluate(evaluatedProject -> {
            final var mixinFingerprint = configureMixinTestFingerprint(evaluatedProject);
            configureTestTasks(evaluatedProject, enabled, explicitlyRequested,
                    infrastructureExplicitlyRequested, mixinFingerprint);
        });
    }

    private final void configureTestTasks(final Project project, final boolean enabled,
                                          final boolean explicitlyRequested,
                                          final boolean infrastructureExplicitlyRequested,
                                          final FingerprintOutputs mixinFingerprint) {
        final var root = project.getRootProject();
        final var testSourceFingerprint = project.getTasks().register(
                "testSourceFingerprint", InputFilesFingerprint.class, task -> {
                    final var rootTestSources = root.fileTree("src/test");
                    final var versionTestSources = project.fileTree("src/test");
                    task.getRelevantFiles().from(rootTestSources, versionTestSources);
                    task.getRelevantProperties().set(List.of(project.getName()));
                    task.getFingerprintFile().set(project.getLayout().getBuildDirectory()
                            .file("test-source-gate/fingerprint.txt"));
                });
        final var fingerprintFile = testSourceFingerprint.flatMap(InputFilesFingerprint::getFingerprintFile);
        final var testTaskNames = project.getTasks().withType(Test.class).getNames().stream()
                .filter(name -> !name.equals("mixinTest")).toList();
        final var testSuccessMarkers = testTaskNames.stream()
                .map(name -> project.getLayout().getBuildDirectory()
                        .file("test-source-gate/" + name + "/last-successful-fingerprint.txt"))
                .toList();

        project.getTasks().withType(Test.class).configureEach(test -> {
            if (!test.getName().equals("mixinTest")) {
                final var successMarker = project.getLayout().getBuildDirectory()
                        .file("test-source-gate/" + test.getName() + "/last-successful-fingerprint.txt");
                test.dependsOn(testSourceFingerprint);
                test.getInputs().file(fingerprintFile);
                test.getOutputs().file(successMarker);
                test.onlyIf("tests are enabled, explicitly requested, or changed since their last successful run",
                        new TestsRequestedOrChanged(enabled, explicitlyRequested, fingerprintFile, successMarker));
                test.doLast(new RecordFingerprintSuccess(successMarker, fingerprintFile));
            }
        });

        final var testInfrastructureTasks = Set.of(
                "kspTestKotlin", "preprocessTestCode", "compileTestKotlin",
                "compileTestJava", "preprocessTestResources", "processTestResources");
        final var forceTestInfrastructure = enabled || explicitlyRequested || infrastructureExplicitlyRequested;
        project.getTasks().configureEach(task -> {
            if (testInfrastructureTasks.contains(task.getName())) {
                task.dependsOn(testSourceFingerprint);
                if (mixinFingerprint != null) {
                    task.dependsOn(mixinFingerprint.task());
                }
                task.onlyIf("test compilation is needed by a changed test suite",
                        new TestInfrastructureNeeded(forceTestInfrastructure, fingerprintFile,
                                testSuccessMarkers,
                                mixinFingerprint == null ? null : mixinFingerprint.fingerprint(),
                                mixinFingerprint == null ? null : mixinFingerprint.successMarker()));
            }
        });
    }

    private final FingerprintOutputs configureMixinTestFingerprint(final Project project) {
        final var mixinTest = project.getTasks().findByName("mixinTest");
        final var runtime = project.getConfigurations().findByName("mixinTestRuntime");
        if (!(mixinTest instanceof Test) || runtime == null) {
            return null;
        }

        final var root = project.getRootProject();
        final var fingerprint = project.getTasks().register(
                "mixinTestFingerprint", InputFilesFingerprint.class, task -> {
                    final var mixinSources = root.fileTree("src/main/java");
                    mixinSources.include("net/sbo/mod/mixin/**", "net/sbo/mod/mixinbase/**");
                    final var mixinTestSource = root.fileTree("src/test");
                    mixinTestSource.include("**/MixinTest.kt");
                    final var rootResources = root.fileTree("src/main/resources");
                    rootResources.include("fabric.mod.json", "sbo.mixins.json", "sbo.classtweaker");
                    final var versionResources = project.fileTree("src/main/resources");
                    versionResources.include("fabric.mod.json", "sbo.mixins.json", "sbo.classtweaker");
                    final var buildFiles = root.fileTree(root.getProjectDir());
                    buildFiles.include("build.gradle.kts", "root.gradle.kts", "settings.gradle.kts",
                            "gradle.properties", "gradle/libs.versions.toml", "versions/mainProject");

                    task.getRelevantFiles().from(mixinSources, mixinTestSource, rootResources,
                            versionResources, buildFiles, runtime);
                    task.getRelevantProperties().set(List.of(
                            project.getName(),
                            project.getVersion().toString(),
                            project.getProviders().gradleProperty("mod.version").getOrElse(""),
                            project.getProviders().gradleProperty("minecraft_version").getOrElse(""),
                            project.getProviders().gradleProperty("kotlin.version").getOrElse("")
                    ));
                    task.getFingerprintFile().set(project.getLayout().getBuildDirectory()
                            .file("mixin-test/fingerprint.txt"));
                });

        final var fingerprintFile = fingerprint.flatMap(InputFilesFingerprint::getFingerprintFile);
        final var successMarker = project.getLayout().getBuildDirectory()
                .file("mixin-test/last-successful-fingerprint.txt");
        final var testTask = (Test) mixinTest;
        testTask.dependsOn(fingerprint);
        testTask.getInputs().file(fingerprintFile);
        testTask.getOutputs().file(successMarker);
        testTask.onlyIf("mixin-relevant inputs have changed",
                new InputFingerprintChanged(successMarker, fingerprintFile));
        testTask.doLast(new RecordFingerprintSuccess(successMarker, fingerprintFile));
        return new FingerprintOutputs(fingerprint, fingerprintFile, successMarker);
    }

    private record FingerprintOutputs(TaskProvider<InputFilesFingerprint> task,
                                      Provider<RegularFile> fingerprint,
                                      Provider<RegularFile> successMarker) {
    }
}
