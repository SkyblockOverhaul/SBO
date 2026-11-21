package net.sbo.mod.diana

import java.util.Locale
import net.sbo.mod.diana.DianaRates.calcPercentOne
import net.sbo.mod.settings.categories.Drops
import net.sbo.mod.utils.TextUtil.joinLines
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.game.ItemPrices.getItemPriceFormatted

/** The players' own drop and rare mob spawn messages with their placeholders filled in. */
object DropMessages {
    fun checkCustomDropMessage(dropName: String, magicFind: Int, isLootshare: Boolean, amountOverride: Int? = null): Pair<Boolean, String> {
        val info = getDropInfo(dropName) ?: return false to ""

        if (!info.isEnabled) return false to ""

        val resultText = info.template
            .replace("{amount}", (amountOverride ?: info.totalAmount).toString())
            .replace("{percentage}", "%.2f".format(Locale.ROOT, info.percentage) + "%")
            .replace("{mf}", if (magicFind > 0) "$magicFind" else "")
            .replace("{lstext}", if (isLootshare) " (LS)" else "")
            .replace("{price}", getItemPriceFormatted(info.itemId))
            .replace('&', '§')
            .replace("{since}", getSinceDrop(dropName, isLootshare))
            .replace("+ ✯ Magic Find ", "") // prevent nonsense magic find when hypixel doesn't put it into the message (mob killed by someone else)

        return true to resultText
    }

    data class DropInfo(
        val template: String,
        val isEnabled: Boolean,
        val totalAmount: Int,
        val itemId: String,
        private val mobCount: Int,
        private val dropCount: Int
    ) {
        val percentage: Double
            get() = if (mobCount > 0) dropCount.toDouble() / mobCount * 100 else 0.0
    }

    private fun getCustomMessage(message: Array<out String>): String = joinLines(message)

    private fun getSinceDrop(dropName: String, isLootshare: Boolean): String {
        val data = DataManager.sboData
        return when (dropName.lowercase()) {
            "wool" -> if (isLootshare) data.kingSinceLsWool.toString() else data.kingSinceWool.toString()
            "core" -> if (isLootshare) data.mantiSinceLsCore.toString() else data.mantiSinceCore.toString()
            "stinger" -> if (isLootshare) data.mantiSinceLsCore.toString() else data.mantiSinceCore.toString()
            "chimera" -> if (isLootshare) data.inqsSinceLsChim.toString() else data.inqsSinceChim.toString()
            "brain food" -> if (isLootshare) data.sphinxSinceLsFood.toString() else data.sphinxSinceFood.toString()
            "looting" -> if (isLootshare) data.sphinxSinceLsLooting.toString() else data.sphinxSinceLooting.toString()
            else -> "ErrorGettingSince"
        }
    }

    private fun getDropInfo(dropName: String): DropInfo? {
        val tracker = DataManager.dianaTrackerMayorData
        val items = tracker.items
        val mobs = tracker.mobs

        return when (dropName.lowercase()) {
            "chimera" -> {
                val message = getCustomMessage(Drops.customChimeraMessage)
                DropInfo(message, message.isNotEmpty(), items.CHIMERA + items.CHIMERA_LS, "CHIMERA", mobs.MINOS_INQUISITOR, items.CHIMERA)
            }

            "core" -> {
                val message = getCustomMessage(Drops.customManticoreMessage)
                DropInfo(message, message.isNotEmpty(), items.MANTI_CORE + items.MANTI_CORE_LS, "MANTI_CORE", mobs.MANTICORE, items.MANTI_CORE)
            }

            "stinger" -> {
                val message = getCustomMessage(Drops.customFatefulStingerMessage)
                DropInfo(message, message.isNotEmpty(), items.FATEFUL_STINGER + items.FATEFUL_STINGER_LS, "FATEFUL_STINGER", mobs.MANTICORE, items.FATEFUL_STINGER)
            }

            "brain food" -> {
                val message = getCustomMessage(Drops.customBrainFoodMessage)
                DropInfo(message, message.isNotEmpty(), items.BRAIN_FOOD + items.BRAIN_FOOD_LS, "BRAIN_FOOD", mobs.SPHINX, items.BRAIN_FOOD)
            }

            "looting" -> {
                val message = getCustomMessage(Drops.customLootingMessage)
                DropInfo(message, message.isNotEmpty(), items.LOOTING_7 + items.LOOTING_7_LS, "LOOTING_7", mobs.SPHINX, items.LOOTING_7)
            }

            "wool" -> {
                val message = getCustomMessage(Drops.customShimmeringWoolMessage)
                DropInfo(message, message.isNotEmpty(), items.SHIMMERING_WOOL + items.SHIMMERING_WOOL_LS, "SHIMMERING_WOOL", mobs.KING_MINOS, items.SHIMMERING_WOOL)
            }

            else -> null
        }
    }

    fun getSpawnMessage(message: String, mob: String): String {
        val mobs = DataManager.dianaTrackerMayorData.mobs
        val sboData = DataManager.sboData

        val kingPercent = calcPercentOne(mobs.KING_MINOS, mobs.TOTAL_MOBS)
        val manticorePercent = calcPercentOne(mobs.MANTICORE, mobs.TOTAL_MOBS)
        val inqPercent = calcPercentOne(mobs.MINOS_INQUISITOR, mobs.TOTAL_MOBS)
        val sphinxPercent = calcPercentOne(mobs.SPHINX, mobs.TOTAL_MOBS)

        when (mob.lowercase()) {
            "minos inquisitor", "inq" -> {
                val since = sboData.mobsSinceInq
                return message.replace("{since}", since.toString()).replace("{chance}", inqPercent)
            }

            "king minos", "king" -> {
                val since = sboData.mobsSinceKing
                return message.replace("{since}", since.toString()).replace("{chance}", kingPercent)
            }

            "sphinx" -> {
                val since = sboData.mobsSinceSphinx
                return message.replace("{since}", since.toString()).replace("{chance}", sphinxPercent)
            }

            "manticore", "manti" -> {
                val since = sboData.mobsSinceManti
                return message.replace("{since}", since.toString()).replace("{chance}", manticorePercent)
            }
            else -> return ""
        }
    }
}
