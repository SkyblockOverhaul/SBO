package net.sbo.mod.test

import java.awt.GraphicsEnvironment

/** Check the startup property before querying AWT or constructing any image/font objects. */
internal fun requireHeadlessReview() {
    check(System.getProperty("java.awt.headless") == "true") {
        "UI review requires -Djava.awt.headless=true at JVM startup"
    }
    check(GraphicsEnvironment.isHeadless()) { "AWT must be headless for UI review" }
}
