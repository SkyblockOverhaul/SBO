package net.sbo.mod.utils.data.configs.overlay

import net.sbo.mod.utils.data.DataManager

data class OverlayData(
    var overlays: MutableMap<String, OverlayValues> = mutableMapOf()
) {
    fun save() = DataManager.save("OverlayData")
}
