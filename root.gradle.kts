plugins {
    alias(libs.plugins.loom) apply false
    kotlin("jvm") apply false
    id("com.replaymod.preprocess-root")
}

preprocess {
    val fabric263 = createNode("26.3-fabric", 26_03_00, null)
    val fabric262 = createNode("26.2-fabric", 26_02_00, null)
    val fabric2612 = createNode("26.1.2-fabric", 26_01_02, null)

    fabric263.link(fabric262)
    fabric262.link(fabric2612)
}
