package net.sbo.mod.settings

import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.ConfigKt
import net.minecraft.Util
import net.sbo.mod.SBOKotlin
import net.sbo.mod.settings.categories.General
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.PartyCommands
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.QOL
import net.sbo.mod.settings.categories.Credits
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.settings.categories.PartyFinder

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
                Util.platform.openUri("https://github.com/SkyblockOverhaul/SBO-Kotlin/releases")
            }
        }

        button {
            title = "Discord"
            description = "Get support and updates on Discord"
            text = "Join"
            onClick {
                Util.platform.openUri("https://discord.gg/QvM6b9jsJD")
            }
        }

        button {
            title = "Patreon"
            description = "Support our development and keep the server running ☕"
            text = "Support"
            onClick {
                Util.platform.openUri("https://www.patreon.com/Skyblock_Overhaul")
            }
        }

        button {
            title = "Website"
            description = "Explore our website for tracking Magic Find upgrades"
            text = "Visit"
            onClick {
                Util.platform.openUri("https://skyblockoverhaul.com/")
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