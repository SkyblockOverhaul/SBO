package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.SBOKotlin

object Credits : CategoryKt("Credits") {
    init {
        button {
            title = "SkyHanni"
            description = "Spade Guess (bloxigus) and Arrow Guess (SidOfThe7Cs)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/hannibal002/SkyHanni")
            }
        }

        button {
            title = "ResourcefulConfig"
            description = "Config library and Config UI (TeamResourceful)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/Team-Resourceful/Resourceful-Config")
            }
        }

        button {
            title = "Elementa"
            description = "UI library (SparkUniverse)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/SparkUniverse/Elementa")
            }
        }

        button {
            title = "UniversalCraft"
            description = "Compatibility layer (SparkUniverse)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/SparkUniverse/UniversalCraft")
            }
        }

        button {
            title = "hm-api"
            description = "Hypixel Mod API wrapper (AzureAaron)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/AzureAaron/hm-api")
            }
        }

        button {
            title = "RenderChest"
            description = "Glow API (AzureAaron)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/AzureAaron/RenderChest")
            }
        }

        button {
            title = "Fabric API"
            description = "Modding API (FabricMC)"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/FabricMC/fabric-api")
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
