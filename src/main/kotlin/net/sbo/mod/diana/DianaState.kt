package net.sbo.mod.diana

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.diana.tracker.Tracker
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.InventoryUtils.playerHasItem
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import net.sbo.mod.utils.waypoint.WaypointManager.removeNearbyRareMobWaypointAt

private const val T3_SPADE = "DEIFIC_SPADE"
private const val T2_SPADE = "ARCHAIC_SPADE"
private const val T1_SPADE = "ANCESTRAL_SPADE"

fun ItemLookup.isCrownOfAvarice(): Boolean {
    val sbId = this.sbId

    return sbId == "CROWN_OF_AVARICE" || sbId == "CROWN_OF_AVARICE_CELEBRATION"
}

fun ItemStack?.isCrownOfAvarice(): Boolean {
    if (this == null || this.isEmpty()) return false

    return ItemLookup(this).isCrownOfAvarice()
}

fun ItemLookup.isDianaSpade(): Boolean {
    val sbId = this.sbId

    return sbId == T3_SPADE || sbId == T2_SPADE || sbId == T1_SPADE
}

/**
 * Helper#hasSpade checks if player has (anywhere in the inventory) or held spade in the last 20 ticks.
 * This checks if player is holding spade in main hand, right now.
 */
fun isHoldingSpadeRightNow(): Boolean {
    val item = mc.player?.mainHandItem ?: return false
    if (item.isEmpty) return false

    return ItemLookup(item).isDianaSpade()
}

/** Diana state while hunting: the spade in the inventory, lootshares, recent mob deaths and sack tracking. */
object DianaState {
    private var lastLootShare: Long = 0L
    var allowSackTracking: Boolean = true
    var hasSpade: Boolean = false
    private var lastDianaMobDeath: Long = 0L
    private var lastInqDeath: Long = 0L
    private var lastKingDeath: Long = 0L
    private var lastSphinxDeath: Long = 0L
    private var lastMantiDeath: Long = 0L

    private var hasTrackedInq: Boolean = false
    private var hasTrackedKing: Boolean = false
    private var hasTrackedSphinx: Boolean = false
    private var hasTrackedManti: Boolean = false

    private fun onLootShare() {
        lastLootShare = System.nanoTime()
    }

    private fun notifyUserOfLs(mob: String) {
        if (Trackers.assumeAllLS) {
            Chat.chat("§6[SBO] §eRegistered Lootshare $mob!", true)
        }
    }

    fun init() {
        Register.onChatMessageCancelable(Pattern.compile("^§e§lLOOT SHARE §fYou received .*? for assisting .*?!?$")) { _, _ ->
            onLootShare()
            true
        }

        Register.onTick(20) { // maybe better way to register this
            hasSpade = playerHasItem(T3_SPADE) || playerHasItem(T2_SPADE) || playerHasItem(T1_SPADE)
        }

        /*Register.command("sbotestlschimdrop") {
            handleDianaMobDeath("Minos Inquisitor", 30.0f)
            RareDrops.onChatDrop("Enchanted Book (Chimera 1) §b(+§b566 ✯ Magic Find)")
        }

        Register.command("sbotestdrops") {
            RareDrops.onChatDrop("Daedalus Stick §b(+§b483 ✯ Magic Find)")
            RareDrops.onChatDrop("Minos Relic §b(+§b483 ✯ Magic Find)")
            RareDrops.onChatDrop("Crown of Greed")
            RareDrops.onChatDrop("Myth the Fish")
            RareDrops.onChatDrop("Daedalus Stick")
            RareDrops.onChatDrop("Shimmering Wool §b(+§b483 ✯ Magic Find)")
        }

        Register.command("sbotestbraideddrop") {
            RareDrops.onBraidedFeather()
        }

        Register.command("sbotestchimdrop") {
            RareDrops.onChatDrop("Enchanted Book (Chimera 1) §b(+§b566 ✯ Magic Find)")
        }*/
    }

    @SboEvent
    fun onDianaMobDeath(event: DianaMobDeathEvent) {
        if (LootshareDisplay.consumeConfirmedLootshare(event.name)) onLootShare()
        handleDianaMobDeath(event.name, event.entity.distanceTo(mc.player!!), event.entity.blockPosition().toSboVec())
    }

    private fun handleDianaMobDeath(name: String, dist: Float, pos: SboVec) {
        val nearby = dist <= 30
        val last = Tracker.lastSpawnedMob
        val lsOverride = Trackers.assumeAllLS && nearby && (last == null || !name.contains(last)) // we need to check if dying mob is not spawned by user by comparing to last spawned mob to avoid counting self-mob as lootshare
        when {
            name.contains("Minos Inquisitor") -> {
                RareMobKillTime.onMobDeath("Minos Inquisitor", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedInq) {
                    hasTrackedInq = true
                    notifyUserOfLs("Minos Inquisitor")
                    Tracker.trackItem("MINOS_INQUISITOR_LS", 1)
                    sleep(2000) {
                        hasTrackedInq = false
                    }
                }
                lastInqDeath = System.nanoTime()
            }
            name.contains("King Minos") -> {
                RareMobKillTime.onMobDeath("King Minos", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedKing) {
                    hasTrackedKing = true
                    notifyUserOfLs("King Minos")
                    Tracker.trackItem("KING_MINOS_LS", 1)
                    sleep(2000) {
                        hasTrackedKing = false
                    }
                }
                lastKingDeath = System.nanoTime()
            }
            name.contains("Sphinx") -> {
                RareMobKillTime.onMobDeath("Sphinx", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (gotLootShareRecently() && !hasTrackedSphinx) {
                    hasTrackedSphinx = true
                    notifyUserOfLs("Sphinx")
                    Tracker.trackItem("SPHINX_LS", 1)
                    sleep(2000) {
                        hasTrackedSphinx = false
                    }
                }
                lastSphinxDeath = System.nanoTime()
            }
            name.contains("Manticore") -> {
                RareMobKillTime.onMobDeath("Manticore", nearby)
                removeNearbyRareMobWaypointAt(pos)
                if (lsOverride) onLootShare() // makes the gotLootShareRecently condition below always pass
                if (gotLootShareRecently() && !hasTrackedManti) {
                    hasTrackedManti = true
                    notifyUserOfLs("Manticore")
                    Tracker.trackItem("MANTICORE_LS", 1)
                    sleep(2000) {
                        hasTrackedManti = false
                    }
                }
                lastMantiDeath = System.nanoTime()
            }
        }

        if (nearby) {
            allowSackTracking = true
            lastDianaMobDeath = System.nanoTime()
        }
    }

    @SboEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        sleep(200) {
            if (event.screen.title.string == "Sack of Sacks") allowSackTracking = false
        }
    }

    /**
     * Checks if the player has received loot share recently.
     * @param timeframe The timeframe in seconds to check against. Default is 2 seconds.
     */
    fun gotLootShareRecently(timeframe: Long = 2): Boolean = getSecondsPassedSinceNano(lastLootShare) <= timeframe

    fun dianaMobDiedRecently(seconds: Long = 2): Boolean = getSecondsPassedSinceNano(lastDianaMobDeath) <= seconds

    private fun getSecondsPassedSinceNano(timestamp: Long): Long = (System.nanoTime() - timestamp) / TimeUnit.SECONDS.toNanos(1L)
}
