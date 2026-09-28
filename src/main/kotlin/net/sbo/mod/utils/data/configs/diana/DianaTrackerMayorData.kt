package net.sbo.mod.utils.data.configs.diana

import net.sbo.mod.utils.game.Mayor

data class DianaTrackerMayorData(
    var year: Int = Mayor.mayorElectedYear,
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
) : DianaTracker {
    fun snapshot(): DianaTrackerMayorData {
        return DianaTrackerMayorData(
            year = this.year,
            items = this.items.copy(),
            mobs = this.mobs.copy(),
        )
    }
}
