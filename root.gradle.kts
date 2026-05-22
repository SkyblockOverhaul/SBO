plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.16.2" apply false
    id("dev.deftu.gradle.multiversion-root") version "2.73.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, "official")
    val fabric12111 = createNode("1.21.11-fabric", 1_21_11, "official")
    val fabric12110 = createNode("1.21.10-fabric", 1_21_10, "official")

    fabric2612.link(fabric12111)
    fabric12111.link(fabric12110)
}
