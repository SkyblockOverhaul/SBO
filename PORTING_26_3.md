# Building SBO for Minecraft 26.3

This contribution targets `Diana-V2` and retains all three genuine multiversion
targets: `:26.1.2-fabric`, `:26.2-fabric` and `:26.3-fabric`. Use the committed
Gradle wrapper and a Java 25 JDK. The older targets remain part of the source
generation chain. Mod version 0.6.0 produces JARs under `build/versions/`, including
`SBO-0.6.0+26.3-fabric.jar`.

## Dependencies

GuiLib is required and bundled on every target. Current `Diana-V2` uses it for the
Achievements, Events, Sounds, Cloud and Party Finder GUIs. The genuine 26.3 coordinate is
`net.sbo:guilib-26.3-fabric:0.12.5`; its POM, Gradle module metadata and JAR are
available from the existing [SkyblockOverhaul Maven repository](https://skyblockoverhaul.github.io/maven).
The published 26.3 JAR has SHA-256
`a8309665c8f929004f74c7d16e886751467c499b3d60f4c18fcc1b8d2876c90c`.

When a sibling `../SBO-GuiLib` source checkout is present, the existing composite
build substitutes its matching projects for all three GuiLib artifacts. Use
`-Pguilib.local=false` to resolve the published artifacts instead.

The 26.3 target requires `net.azureaaron:render-chest:1.0.3+26.3`. HM API and
RenderChest continue to use the original publisher repository at
`https://maven.azureaaron.net/releases`. The publisher's 26.3 source port has been
merged and [CI run 37227109252](https://github.com/AzureAaron/RenderChest/actions/runs/37227109252)
succeeded. As checked on 2026-10-06, the publisher's Maven metadata still lists
only versions through `1.0.3+26.2`, and the 26.3 POM returns HTTP 404.
The default build and GitHub CI therefore still await that Maven publication.

Local verification used that run's original `Artifacts` download (artifact ID
`11312024345`), with archive SHA-256
`a908699f7f5ad0a9a5b5c8d2e5f537663588702c8c294f50cf3cae9064891f24`.
Its unmodified `render-chest-1.0.3+26.3.jar` has SHA-256
`1958d97fc815bf77fabc1fe6a2f9d56f8f03edd01f6a9f2e0d91727a5877fe88`.

The optional `-PportDependencyRepository=...` adds a caller-selected Maven
repository ahead of that publisher, restricted to HM API and RenderChest.
Relative paths resolve against the SBO root; absolute paths and file URIs are
also accepted. The build does not assume a workspace-local repository.

## Build and validation

From the source root, with Java 25 selected:

```sh
./gradlew --no-daemon --max-workers=4 \
  :26.1.2-fabric:build :26.2-fabric:build :26.3-fabric:build
```

Until RenderChest is published, place the verified CI JAR and a minimal Maven
POM at `net/azureaaron/render-chest/1.0.3+26.3/` in a local Maven repository,
then add `-PportDependencyRepository=/path/to/maven-repo` to that command.
Use `-Pguilib.local=false` to verify the published GuiLib artifact.

The test tasks include the upstream headless Fabric mixin audit and native input
checks. They do not launch a client or verify rendered gameplay or Hypixel
behavior. All three targets passed their build tasks and nine tests each using
Java 25 and the verified RenderChest CI artifact. No player configuration,
account data or DevAuth setup is required to build.

Native offline/software-GPU checks passed separately and with 76 production mods:
Achievements text filtering, Backspace/Delete, grouping and scrolling; Events
details/back navigation; Sounds searchable selection and saved selection on
reopen; Cloud Sync local text input and backups navigation; and Party Finder's
local appearance controls. Fresh native typing still worked after a resource
reload. Cloud keys were synthetic and unsaved; no cloud upload or party join ran.

Actual through-wall line rendering passed in fresh offline worlds, including
514 recorded frames separately and 485 with the full pack, with completed
resource reloads. A separate full-pack run using copied settings/resource packs
also passed 196 pipeline checks and three mixin audits.
Network isolation produced an expected Noamm startup downloader error; its raw
log was retained and reviewed separately from rendering failures.
These checks do not cover authenticated Hypixel gameplay, physical GPUs,
Vulkan, shader packs or backend cloud/party operations.
