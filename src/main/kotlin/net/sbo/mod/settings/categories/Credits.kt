package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.SBOKotlin

object Credits : CategoryKt("Credits") {
    init {
        button {
            title = "Skyhanni"
            description = "Diana guess (bloxigus)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/hannibal002/SkyHanni")
            }
        }

        separator {
            title = "Special Thanks"
            description = """
                - to all our supporters and contributors
                - to the people who helped testing and gave feedback
                - to all open source Skyblock mods and libraries we used
            """.trimIndent()
        }
    }
}
