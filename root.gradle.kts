plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.12" apply false
    id("dev.deftu.gradle.multiversion-root") version "2.73.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, "srg")
    val fabric12111 = createNode("1.21.11-fabric", 1_21_11, "srg")

    fabric2612.link(fabric12111)
}
