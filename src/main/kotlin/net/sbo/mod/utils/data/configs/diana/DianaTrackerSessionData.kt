package net.sbo.mod.utils.data.configs.diana

data class DianaTrackerSessionData(
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
    override var edited: Boolean = false,
) : DianaTracker
