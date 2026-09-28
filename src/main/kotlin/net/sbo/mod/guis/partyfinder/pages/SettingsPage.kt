package net.sbo.mod.guis.partyfinder.pages

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import gg.essential.universal.UScreen.Companion.displayScreen
import net.sbo.mod.SBOKotlin.MOD_ID
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.PartyFinderGUI

class SettingsPage(private val parent: PartyFinderGUI) : PartyPage {
    override val pageName: String = "Settings"
    override val partyType: String = ""
    override val listDisplayName: String = ""
    override val pageOrder: Int get() = 102

    private fun openSettings() {
        mc.schedule {
            displayScreen(ResourcefulConfigScreen.getFactory(MOD_ID).apply(null))
        }
    }

    override fun render() {
        openSettings()
    }
}
