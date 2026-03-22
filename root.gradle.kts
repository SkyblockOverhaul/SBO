plugins {
    id("dev.deftu.gradle.multiversion-root")
}

preprocess {
    strictExtraMappings.set(true)

    "1.21.11-fabric"(1_21_11, "yarn") {
        "1.21.10-fabric"(1_21_10, "yarn") {
            "1.21.7-fabric"(1_21_08, "yarn") {
                "1.21.5-fabric"(1_21_05, "yarn")
            }
        }
    }
}
