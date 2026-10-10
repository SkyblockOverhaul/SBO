package net.sbo.mod.compat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.sbo.mod.guis.HubGui

object ModMenuCompat : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> = ConfigScreenFactory { _ -> HubGui.settingsScreen() }
}
