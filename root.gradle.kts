plugins {
    alias(libs.plugins.loom) apply false
    id("dev.deftu.gradle.multiversion-root") version "2.80.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric263 = createNode("26.3-fabric", 26_03_00, "srg")
    val fabric262 = createNode("26.2-fabric", 26_02_00, "srg")
    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, "srg")

    fabric263.link(fabric262)
    fabric262.link(fabric2612)
}
