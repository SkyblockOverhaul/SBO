plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17.11" apply false
    id("dev.deftu.gradle.multiversion-root") version "2.73.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, "srg")
}
