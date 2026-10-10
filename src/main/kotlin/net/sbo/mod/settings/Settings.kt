package net.sbo.mod.settings

import com.google.gson.JsonObject
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
        category(Themes)
        category(QOL)
        category(Debug)
    }

    // The Party Finder category moved into the party finder window, PartyFinderManager takes its old values over once
    var legacyPartyFinder: JsonObject? = null

    override fun load(json: JsonObject) {
        super.load(json)
        legacyPartyFinder = (json.get("Party Finder") ?: json.get("PartyFinder")) as? JsonObject
    }
}
