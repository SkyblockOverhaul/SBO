package net.sbo.mod.diana.tracker

import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.configs.diana.DianaItemsData
import net.sbo.mod.utils.data.configs.diana.DianaMobsData
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.data.configs.sbo.SboData
import kotlin.reflect.KMutableProperty1

internal fun String.toItemId() = uppercase().replace(" ", "_").replace("-", "_")

/** A since counter or b2b flag in sboData, read from the current sboData since it gets replaced on load. */
internal var <T> KMutableProperty1<SboData, T>.value: T
    get() = this.get(sboData)
    set(value) = this.set(sboData, value)

/**
 * The counters of the Diana trackers by item id. Property references instead of reflection over the fields, unbound
 * since the trackers get replaced on load.
 */
internal object Counters {
    private val items: Map<String, KMutableProperty1<DianaItemsData, Int>> = listOf(
        DianaItemsData::GRIFFIN_FEATHER, DianaItemsData::MYTHOS_FRAGMENT, DianaItemsData::CROWN_OF_GREED,
        DianaItemsData::WASHED_UP_SOUVENIR, DianaItemsData::SHIMMERING_WOOL, DianaItemsData::SHIMMERING_WOOL_LS,
        DianaItemsData::MANTI_CORE, DianaItemsData::MANTI_CORE_LS, DianaItemsData::CHIMERA, DianaItemsData::CHIMERA_LS,
        DianaItemsData::BRAIN_FOOD, DianaItemsData::BRAIN_FOOD_LS, DianaItemsData::FATEFUL_STINGER, DianaItemsData::FATEFUL_STINGER_LS,
        DianaItemsData::BRAIDED_GRIFFIN_FEATHER, DianaItemsData::DAEDALUS_STICK, DianaItemsData::CRETAN_URN,
        DianaItemsData::DWARF_TURTLE_SHELMET, DianaItemsData::ANTIQUE_REMEDIES, DianaItemsData::CROCHET_TIGER_PLUSHIE,
        DianaItemsData::ENCHANTED_ANCIENT_CLAW, DianaItemsData::ANCIENT_CLAW, DianaItemsData::MINOS_RELIC,
        DianaItemsData::ENCHANTED_GOLD, DianaItemsData::HILT_OF_REVELATIONS, DianaItemsData::TOTAL_BURROWS,
        DianaItemsData::KING_MINOS_SHARD, DianaItemsData::SPHINX_SHARD, DianaItemsData::MINOTAUR_SHARD,
        DianaItemsData::CRETAN_BULL_SHARD, DianaItemsData::HARPY_SHARD, DianaItemsData::MYTHOLOGICAL_DYE,
        DianaItemsData::MYTH_THE_FISH, DianaItemsData::FAKE_SHURIKEN,
    ).associateBy { it.name }
    private val coins: Map<String, KMutableProperty1<DianaItemsData, Long>> =
        listOf(DianaItemsData::COINS, DianaItemsData::SCAVENGER_COINS, DianaItemsData::FISH_COINS).associateBy { it.name }
    private val mobs: Map<String, KMutableProperty1<DianaMobsData, Int>> = listOf(
        DianaMobsData::KING_MINOS, DianaMobsData::MANTICORE, DianaMobsData::MINOS_INQUISITOR, DianaMobsData::SPHINX,
        DianaMobsData::MINOS_CHAMPION, DianaMobsData::MINOTAUR, DianaMobsData::GAIA_CONSTRUCT, DianaMobsData::HARPY,
        DianaMobsData::CRETAN_BULL, DianaMobsData::STRANDED_NYMPH, DianaMobsData::SIAMESE_LYNXES, DianaMobsData::MINOS_HUNTER,
        DianaMobsData::TOTAL_MOBS, DianaMobsData::MINOS_INQUISITOR_LS, DianaMobsData::KING_MINOS_LS,
        DianaMobsData::MANTICORE_LS, DianaMobsData::SPHINX_LS,
    ).associateBy { it.name }

    fun add(tracker: DianaTracker, item: String, amount: Int) {
        items[item]?.let { it.set(tracker.items, it.get(tracker.items) + amount) }
        coins[item]?.let { it.set(tracker.items, it.get(tracker.items) + amount) }
        mobs[item]?.let { it.set(tracker.mobs, it.get(tracker.mobs) + amount) }
    }

    /** The count in the mayor tracker, null for an item without a counter. */
    fun mayorCount(itemId: String): Int? = items[itemId]?.get(dianaTrackerMayorData.items)

    /** No mob or item counter is above 0, coins and times don't count. */
    fun isEmpty(tracker: DianaTracker): Boolean =
        mobs.values.all { it.get(tracker.mobs) == 0 } && items.values.all { it.get(tracker.items) == 0 }
}
