package net.sbo.mod.diana

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.reflect.full.memberProperties
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.data.configs.diana.DianaItemsData
import net.sbo.mod.utils.data.configs.diana.DianaMobsData
import net.sbo.mod.utils.data.configs.diana.DianaTracker as DianaTrackerDataClass

/** Diana numbers: drop and mob percentages, drop chances, burrows and mobs per hour, Magic Find. */
object DianaRates {
    /**
     * The Magic Find in a drop message without formatting, e.g. "(+566 ✯ Magic Find)". Ignores the symbol, the MF
     * rework (Alpha Network, Nov 2026) shows it as a resource pack glyph.
     */
    private val MF_REGEX = Regex("""\(\+([\d,]+)[^)]*Magic Find\)""")

    /**
     * Calculate percentage of one property to another.
     * If [mobName] is provided, it calculates the percentage of [propertyName] from [items] to [mobName] from [mobs].
     * If [mobName] is null, it calculates the percentage of [propertyName] from [mobs] to total mobs.
     */
    fun calcPercentOneReflectively(items: DianaItemsData, mobs: DianaMobsData, propertyName: String, mobName: String? = null): String {
        return if (mobName != null) {
            calcPercentOne(getItemCount(items, propertyName), getMobCount(mobs, mobName))
        } else {
            calcPercentOne(getMobCount(mobs, propertyName), mobs.TOTAL_MOBS)
        }
    }

    private fun getItemCount(items: DianaItemsData, propertyName: String): Int =
        ITEM_COUNT_GETTERS[propertyName]?.invoke(items)
            ?: ITEM_PROPERTIES[propertyName]?.call(items) as? Int
            ?: 0

    private fun getMobCount(mobs: DianaMobsData, propertyName: String): Int =
        MOB_COUNT_GETTERS[propertyName]?.invoke(mobs)
            ?: MOB_PROPERTIES[propertyName]?.call(mobs) as? Int
            ?: 0

    private val ITEM_PROPERTIES = DianaItemsData::class.memberProperties.associateBy { it.name }
    private val MOB_PROPERTIES = DianaMobsData::class.memberProperties.associateBy { it.name }

    private val ITEM_COUNT_GETTERS: Map<String, (DianaItemsData) -> Int> = mapOf(
        "GRIFFIN_FEATHER" to { it.GRIFFIN_FEATHER }, "MYTHOS_FRAGMENT" to { it.MYTHOS_FRAGMENT },
        "CROWN_OF_GREED" to { it.CROWN_OF_GREED }, "WASHED_UP_SOUVENIR" to { it.WASHED_UP_SOUVENIR },
        "SHIMMERING_WOOL" to { it.SHIMMERING_WOOL }, "SHIMMERING_WOOL_LS" to { it.SHIMMERING_WOOL_LS },
        "MANTI_CORE" to { it.MANTI_CORE }, "MANTI_CORE_LS" to { it.MANTI_CORE_LS },
        "CHIMERA" to { it.CHIMERA }, "CHIMERA_LS" to { it.CHIMERA_LS },
        "BRAIN_FOOD" to { it.BRAIN_FOOD }, "BRAIN_FOOD_LS" to { it.BRAIN_FOOD_LS },
        "LOOTING_7" to { it.LOOTING_7 }, "LOOTING_7_LS" to { it.LOOTING_7_LS },
        "FATEFUL_STINGER" to { it.FATEFUL_STINGER }, "FATEFUL_STINGER_LS" to { it.FATEFUL_STINGER_LS },
        "BRAIDED_GRIFFIN_FEATHER" to { it.BRAIDED_GRIFFIN_FEATHER }, "DAEDALUS_STICK" to { it.DAEDALUS_STICK },
        "CRETAN_URN" to { it.CRETAN_URN }, "DWARF_TURTLE_SHELMET" to { it.DWARF_TURTLE_SHELMET },
        "ANTIQUE_REMEDIES" to { it.ANTIQUE_REMEDIES }, "CROCHET_TIGER_PLUSHIE" to { it.CROCHET_TIGER_PLUSHIE },
        "ENCHANTED_ANCIENT_CLAW" to { it.ENCHANTED_ANCIENT_CLAW }, "ANCIENT_CLAW" to { it.ANCIENT_CLAW },
        "MINOS_RELIC" to { it.MINOS_RELIC }, "ENCHANTED_GOLD" to { it.ENCHANTED_GOLD },
        "HILT_OF_REVELATIONS" to { it.HILT_OF_REVELATIONS }, "KING_MINOS_SHARD" to { it.KING_MINOS_SHARD },
        "SPHINX_SHARD" to { it.SPHINX_SHARD }, "MINOTAUR_SHARD" to { it.MINOTAUR_SHARD },
        "CRETAN_BULL_SHARD" to { it.CRETAN_BULL_SHARD }, "HARPY_SHARD" to { it.HARPY_SHARD },
        "MYTHOLOGICAL_DYE" to { it.MYTHOLOGICAL_DYE }, "MYTH_THE_FISH" to { it.MYTH_THE_FISH }
    )

    private val MOB_COUNT_GETTERS: Map<String, (DianaMobsData) -> Int> = mapOf(
        "KING_MINOS" to { it.KING_MINOS }, "MANTICORE" to { it.MANTICORE },
        "MINOS_INQUISITOR" to { it.MINOS_INQUISITOR }, "SPHINX" to { it.SPHINX },
        "MINOS_CHAMPION" to { it.MINOS_CHAMPION }, "MINOTAUR" to { it.MINOTAUR },
        "GAIA_CONSTRUCT" to { it.GAIA_CONSTRUCT }, "HARPY" to { it.HARPY },
        "CRETAN_BULL" to { it.CRETAN_BULL }, "STRANDED_NYMPH" to { it.STRANDED_NYMPH },
        "SIAMESE_LYNXES" to { it.SIAMESE_LYNXES }, "MINOS_HUNTER" to { it.MINOS_HUNTER },
        "TOTAL_MOBS" to { it.TOTAL_MOBS },
        "MINOS_INQUISITOR_LS" to { it.MINOS_INQUISITOR_LS }, "KING_MINOS_LS" to { it.KING_MINOS_LS },
        "MANTICORE_LS" to { it.MANTICORE_LS }, "SPHINX_LS" to { it.SPHINX_LS }
    )

    fun calcPercentOne(itemCount: Int, mobCount: Int): String {
        val result = if (mobCount <= 0) {
            0.0
        } else {
            itemCount.toDouble() / mobCount * 100
        }

        return PERCENT_FORMAT.format(result)
    }

    private val PERCENT_FORMAT = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US)).apply {
        roundingMode = RoundingMode.HALF_UP
    }

    /** The Magic Find of a drop message, 0 if it has none (the mob was killed by someone else). Colors don't matter. */
    fun getMagicFind(mf: String): Int {
        val mfMatch = MF_REGEX.find(mf.removeFormatting()) ?: return 0
        return mfMatch.groupValues[1].replace(",", "").toIntOrNull() ?: 0
    }

    internal fun getBurrowsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.0) return 0.0
        val totalBurrows = tracker.items.TOTAL_BURROWS.toDouble()
        val burrowsPerHr = totalBurrows / hours
        return BigDecimal.valueOf(burrowsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    internal fun getMobsPerHr(tracker: DianaTrackerDataClass, timer: SboTimerManager.SBOTimer): Double {
        val hours = timer.getHourTime()
        if (hours <= 0.0) return 0.0
        val totalMobs = tracker.mobs.TOTAL_MOBS.toDouble()
        val mobsPerHr = totalMobs / hours
        return BigDecimal.valueOf(mobsPerHr).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /** Drop chances in percent without Magic Find, by griffin rarity and drop. */
    private val BASE_CHANCES_PERCENT = mapOf(
        "epic" to mapOf("stick" to 0.3, "relic" to 0.05),
        "legendary" to mapOf("chim" to 2.0, "stick" to 0.35, "relic" to 0.1, "food" to 0.5),
        "mythic" to mapOf("chim" to 2.5, "stick" to 0.4, "relic" to 0.15, "food" to 1.0, "wool" to 0.75, "core" to 0.75, "stinger" to 1.5)
    )

    /**
     * The drop chances (0..1) of a griffin of [rarity] with [mf] Magic Find, by drop ("chim", "stick"...).
     *
     * @param lootshare the chances when looting a mob someone else killed, a fifth of the normal ones
     */
    fun getChance(mf: Int, rarity: String, lootshare: Boolean = false): Map<String, Double> {
        val multiplier = 1.0 + mf / 100.0
        val factor = if (lootshare) multiplier / 5.0 else multiplier
        return BASE_CHANCES_PERCENT[rarity.lowercase().trim()].orEmpty().mapValues { (_, chance) -> chance / 100.0 * factor }
    }

    fun formatChances(chance: Double, label: String): String {
        val percent = String.format(Locale.ROOT, "%.2f", chance * 100)
        val fraction = " §7(§b1/${(1 / chance).roundToInt()}§7)"
        return "§eChance: §b$percent%$fraction $label"
    }

    fun formatMagicFind(mf: Int): String = "§7[MF:$mf]"
}
