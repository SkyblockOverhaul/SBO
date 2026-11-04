package net.sbo.mod.utils.data.configs.diana

data class DianaTrackerTotalData(
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
    override var edited: Boolean = false,
) : DianaTracker
