package net.sbo.mod.guis.partyfinder.pages

import net.sbo.mod.guis.HubGui
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.PartyFinderGUI

class SettingsPage(private val parent: PartyFinderGUI) : PartyPage {
    override val pageName: String = "Settings"
    override val partyType: String = ""
    override val listDisplayName: String = ""
    override val pageOrder: Int get() = 102

    private fun openSettings() {
        mc.schedule {
            HubGui.openSettings()
        }
    }

    override fun render() {
        openSettings()
    }
}
