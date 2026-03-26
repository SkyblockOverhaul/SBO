plugins {
    id("dev.deftu.gradle.multiversion-root")
}

preprocess {
    strictExtraMappings.set(true)

    "1.21.11-fabric"(1_21_11, "yarn") {
        "1.21.10-fabric"(1_21_10, "yarn")
    }
}
