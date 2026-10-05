package net.sbo.mod.settings

import net.sbo.mod.SBOKotlin
import net.sbo.mod.config.Config
import net.sbo.mod.settings.categories.*

object Settings : Config("sbo/config") {
    override val name: String
        get() = "SBO ${SBOKotlin.version} for MC ${SBOKotlin.mcVersion}"
    override val description = "Diana and the SBO Party Finder for Hypixel Skyblock, plus trackers and QOL features."

    init {
        category(General)
        category(Diana)
        category(Medal)
        category(PartyCommands)
        category(Customization)
        category(PartyFinder)
        category(QOL)
        category(Debug)
    }
}
