package net.sbo.mod.utils.data.configs.diana

data class DianaTrackerSessionData(
    override var items: DianaItemsData = DianaItemsData(),
    override var mobs: DianaMobsData = DianaMobsData(),
) : DianaTracker
