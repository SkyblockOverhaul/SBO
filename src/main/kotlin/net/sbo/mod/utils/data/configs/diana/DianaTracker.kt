package net.sbo.mod.utils.data.configs.diana

import net.sbo.mod.utils.data.DataManager
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val itemProperties: Map<String, KProperty1<DianaItemsData, *>> =
    DianaItemsData::class.memberProperties.associateBy { it.name }

interface DianaTracker {
    var items: DianaItemsData
    var mobs: DianaMobsData

    fun reset(): DianaTracker {
        items = DianaItemsData()
        mobs = DianaMobsData()
        if (this is DianaTrackerMayorData) year = 0
        return this
    }

    fun getAmountOf(itemId: String): Int {
        return (itemProperties[itemId]?.get(items) as? Number)?.toInt() ?: 0
    }
}
