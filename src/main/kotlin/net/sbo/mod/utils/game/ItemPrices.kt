package net.sbo.mod.utils.game

import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong
import net.sbo.mod.SBOKotlin
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.utils.data.HypixelBazaarResponse
import net.sbo.mod.utils.data.configs.npcSellValueMap
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.http.SboApi
import net.sbo.mod.utils.math.NumberUtil.formatNumber

/** Item prices from the bazaar, the auction house and NPCs. */
object ItemPrices {
    private var priceDataAh: Map<String, Long> = emptyMap()
    private var lastAhFetch = 0L
    private var priceDataBazaar: HypixelBazaarResponse? = null

    fun init() {
        Register.onTick(20 * 60 * 5) {
            updateBazaarPrices()
        }

        Register.onTick(20 * 60 * 30) {
            updateAhPrices()
        }

        // Workaround to not timeout on the first requests since there is a lot of work being done that steals CPU time from our HTTP threads during game startup. Wait after at least 1 second of virtual thread carrier threads being available, and runAsync's ForkJoinPool task queue to become empty to be able to run our task.
        sleep(1000) {
            CompletableFuture.runAsync {
                updateItemPriceInfo()
            }
        }
    }

    // AH prices load once at startup and then every 30 minutes, so a failed first load is retried on the next join
    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (priceDataAh.isEmpty() && System.currentTimeMillis() - lastAhFetch > 60_000) updateAhPrices()
    }

    private fun updateItemPriceInfo() {
        updateBazaarPrices()
        updateAhPrices()
    }

    private fun updateBazaarPrices() {
        Http.sendGetRequest("https://api.hypixel.net/skyblock/bazaar?product")
            .toJson<HypixelBazaarResponse>(true) {
                priceDataBazaar = it
                DianaLoot.updateLines()
            }.error { error ->
                SBOKotlin.logger.error("Unexpected error while fetching Bazaar item prices", error)
            }
    }

    private fun updateAhPrices() {
        lastAhFetch = System.currentTimeMillis()
        SboApi.ahItems()
            .toJson<List<Map<String, Map<String, Long>>>>(true) { json ->
                priceDataAh = json.asSequence().flatMap { it.entries }.associate { it.key to it.value["price"]!! }
                DianaLoot.updateLines()
            }.error { error ->
                SBOKotlin.logger.error("Unexpected error while fetching AH item prices", error)
            }
    }

    fun getItemPrice(sbId: String, amount: Int = 1): Long {
        val id = when {
            sbId == "CHIMERA" -> "ENCHANTMENT_ULTIMATE_CHIMERA_1"
            sbId == "LOOTING_7" -> "ENCHANTMENT_LOOTING_7"
            sbId.endsWith("_SHARD") || sbId.endsWith("_DYE") -> {
                val suffix = sbId.substringAfterLast('_')   // "SHARD"
                val name = sbId.substringBeforeLast('_')   // "WITHER"
                "${suffix}_$name"
            }
            else -> sbId
        }

        val bzProduct = priceDataBazaar?.products?.get(id)
        val bazaarPrice = if (Trackers.bazaarSettingDiana == Trackers.BazaarPrice.INSTASELL) {
            bzProduct?.quick_status?.sellPrice ?: 0.0
        } else {
            bzProduct?.quick_status?.buyPrice ?: 0.0
        }

        val ahPrice = priceDataAh[id]?.toDouble() ?: 0.0
        val npcPrice = npcSellValueMap[id]?.toDouble() ?: 0.0

        val preferNpc =
            Trackers.npcOnlyPrices || Trackers.priceSource == Trackers.PriceSource.NPC_ITEMS && (sbId == "CRETAN_URN" || sbId == "DWARF_TURTLE_SHELMET" || sbId == "ANTIQUE_REMEDIES" || sbId == "WASHED_UP_SOUVENIR" || sbId == "CROCHET_TIGER_PLUSHIE" || sbId == "HILT_OF_REVELATIONS")
        val bestMarketPrice = kotlin.math.max(bazaarPrice, ahPrice)
        val bestUnitPrice = if (npcPrice > bestMarketPrice || preferNpc) npcPrice else bestMarketPrice

        return (bestUnitPrice * amount).roundToLong()
    }

    fun getItemPriceFormatted(sbId: String, amount: Int = 1): String {
        val price = getItemPrice(sbId, amount)
        return formatNumber(price)
    }
}
