plugins {
    id("gg.essential.loom") version "1.15.50" apply false
    id("dev.deftu.gradle.multiversion-root") version "2.73.0"
}

preprocess {
    strictExtraMappings.set(true)

    val fabric261 = createNode("26.1-fabric", 26_01_00, "official")
    val fabric12111 = createNode("1.21.11-fabric", 1_21_11, "official")
    val fabric12110 = createNode("1.21.10-fabric", 1_21_10, "official")

    fabric261.link(fabric12111)
    fabric12111.link(fabric12110)
}
