plugins {
    id("net.fabricmc.fabric-loom") version "1.17.12" apply false
    id("dev.deftu.gradle.multiversion-root") version "2.73.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric262 = createNode("26.2-fabric", 26_00_00, "srg")
    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, "srg")

    fabric262.link(fabric2612)
}
