package net.sbo.mod.settings

import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.ConfigKt
import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.*

object Settings : ConfigKt("sbo/config") {
    override val name: TranslatableValue
        get() = Literal("SBO ${SBOKotlin.version}")
    override val description = Literal("Mod for the Mythological Ritual event in hypixel skyblock and custom partyfinder")

    init {
        separator {
            title = "Welcome to Skyblock Overhaul!"
            description = "Made by D4rkSwift/RolexDE and contributors."
        }

        button {
            title = "Github"
            description = "Opens the GitHub releases page"
            text = "Open"
            onClick {
                SBOKotlin.openInBrowser("https://github.com/SkyblockOverhaul/SBO/releases")
            }
        }

        button {
            title = "Discord"
            description = "Get support and updates on Discord"
            text = "Join"
            onClick {
                SBOKotlin.openInBrowser("https://discord.gg/QvM6b9jsJD")
            }
        }

        button {
            title = "Patreon"
            description = "Support our development and keep the server running ☕"
            text = "Support"
            onClick {
                SBOKotlin.openInBrowser("https://www.patreon.com/Skyblock_Overhaul")
            }
        }

        button {
            title = "Website"
            description = "Explore our website for tracking Magic Find upgrades"
            text = "Visit"
            onClick {
                SBOKotlin.openInBrowser("https://skyblockoverhaul.com/")
            }
        }

        category(General)
        category(Diana)
        category(PartyCommands)
        category(Customization)
        category(PartyFinder)
        category(QOL)
        category(Debug)
        category(Credits)
    }

    fun save() = SBOKotlin.settings.save()
}
