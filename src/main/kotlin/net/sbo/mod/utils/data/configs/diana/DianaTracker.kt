package net.sbo.mod.utils.data.configs.diana

import net.sbo.mod.utils.data.DataManager
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val itemProperties: Map<String, KProperty1<DianaItemsData, *>> =
    DianaItemsData::class.memberProperties.associateBy { it.name }

interface DianaTracker {
    var items: DianaItemsData
    var mobs: DianaMobsData
    /** The file was changed outside of SBO and has no usable seal to undo that, shown until the tracker is reset. */
    var edited: Boolean

    fun reset(): DianaTracker {
        items = DianaItemsData()
        mobs = DianaMobsData()
        edited = false
        if (this is DianaTrackerMayorData) year = 0
        return this
    }

    fun getAmountOf(itemId: String): Int {
        return (itemProperties[itemId]?.get(items) as? Number)?.toInt() ?: 0
    }
}
