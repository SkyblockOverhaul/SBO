# Building SBO for Minecraft 26.3

Use the committed Gradle wrapper and a Java 25 JDK. The genuine multiversion target is `:26.3-fabric`; older targets remain part of its source-generation chain. The mod version stays 0.6.0 and the production output is `build/versions/SBO-0.6.0+26.3-fabric.jar`.

The original publisher repository at `https://maven.azureaaron.net/releases` remains the default for HM API and RenderChest. `-PportDependencyRepository=...` optionally adds a caller-selected Maven repository ahead of that publisher, restricted to the same two modules. Relative paths resolve against the SBO root; absolute paths or file URIs are suitable for an external local repository. No workspace repository is assumed.

## Preparing RenderChest

SBO 26.3 requires `net.azureaaron:render-chest:1.0.3+26.3`. Its official Maven release was not available when this port was prepared. Use the original publisher's 26.3 source/build, or its verified CI binary; the 26.2 binary is unsuitable.

The pinned original source is [AzureAaron/RenderChest 0512b13ae5ee780b82a0be80535a8856ac8431f8](https://github.com/AzureAaron/RenderChest/tree/0512b13ae5ee780b82a0be80535a8856ac8431f8). Choose external source, repository and JDK directories, then build and publish only to that local directory:

```sh
export JAVA_HOME="$JDK25"
git clone https://github.com/AzureAaron/RenderChest.git "$RENDER_CHEST_SOURCE"
git -C "$RENDER_CHEST_SOURCE" checkout 0512b13ae5ee780b82a0be80535a8856ac8431f8
cd "$RENDER_CHEST_SOURCE"
./gradlew --no-daemon --max-workers=4 build publishMavenJavaPublicationToMavenLocal \
  -Dmaven.repo.local="$PORT_DEPENDENCIES"
```

Use an absolute filesystem directory for `PORT_DEPENDENCIES` in this recipe; `maven.repo.local` expects a directory path.

`publishMavenJavaPublicationToMavenLocal` is the original source's real Maven publication task; `maven.repo.local` redirects it to the chosen directory. It does not invoke the publisher's remote publication task. Building the pinned source may produce a different ZIP hash from CI; verify the resulting native 26.3 metadata and preserve its own provenance.

The original [successful CI run 35052254428](https://github.com/AzureAaron/RenderChest/actions/runs/35052254428), artifact 10429656413, also supplied the genuine binary. Its archive digest was `52dd5f9ea0f792aa2c5b5a038431cffbe0312011fc0fcb5491304150a05a6521`; the contained `render-chest-1.0.3+26.3.jar` SHA256 was `6df5281fd4d89f236281468b9612c84a7e53df72d7648df42fb5189925ee434e`. CI artifacts may expire. Obtain it from that publisher/run, verify both digests, and retain its Apache 2.0 license and source reference. These hashes identify that build, not a future Maven release.

For an already verified CI JAR, create a separate temporary Gradle project containing the following `build.gradle.kts`. This installs the original unchanged binary into the chosen local Maven layout; it does not rebuild it or change its Fabric metadata:

```kotlin
plugins { `maven-publish` }
val renderChestJar = providers.gradleProperty("renderChestJar").get()
publishing {
    publications {
        create<MavenPublication>("renderChest") {
            groupId = "net.azureaaron"
            artifactId = "render-chest"
            version = "1.0.3+26.3"
            artifact(file(renderChestJar))
            pom {
                name.set("Render Chest")
                description.set("Original AzureAaron Minecraft 26.3 publisher CI build")
                url.set("https://github.com/AzureAaron/RenderChest")
                licenses { license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                } }
                scm { url.set("https://github.com/AzureAaron/RenderChest") }
            }
        }
    }
    repositories { maven {
        name = "portDependencies"
        url = uri(providers.gradleProperty("portDependencyRepository").get())
    } }
}
```

Run it with the SBO wrapper, pointing `RENDER_CHEST_INSTALLER` to that temporary project and `RENDER_CHEST_JAR` to the verified binary:

```sh
./gradlew -p "$RENDER_CHEST_INSTALLER" --no-daemon --max-workers=4 \
  publishRenderChestPublicationToPortDependenciesRepository \
  -PrenderChestJar="$RENDER_CHEST_JAR" \
  -PportDependencyRepository="$PORT_DEPENDENCIES"
```

This repository only needs the unchanged runtime JAR and its coordinate/POM for SBO. No publisher credentials are needed. When the publisher releases the required coordinate publicly, the optional repository argument can be omitted.

## Building and checking SBO

From the SBO source root, with the external repository prepared:

```sh
export JAVA_HOME="$JDK25"
./gradlew --no-daemon --max-workers=4 \
  :26.3-fabric:jar :26.3-fabric:test \
  -PportDependencyRepository="$PORT_DEPENDENCIES"
```

The test task includes the upstream headless Fabric mixin audit and native input checks. It does not launch a client or verify rendered gameplay/Hypixel behavior. Client and combined-pack tests require their separate isolated runtime workflow. No player configuration, account data, DevAuth setup or desktop session is required to build.
