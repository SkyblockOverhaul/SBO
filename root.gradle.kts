plugins {
    id("dev.deftu.gradle.multiversion-root")
}

preprocess {
    strictExtraMappings.set(true)

    "1.21.11-fabric"(1_21_11, "official") {
        "1.21.10-fabric"(1_21_10, "official")
    }
}
