package net.sbo.mod.diana

import it.unimi.dsi.fastutil.ints.IntSet
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.waypoint.WaypointManager

/** Tracks rare-mob combat time separately from the cocoon hatch animation. */
object RareMobKillTime {
    private const val HATCH_LOOKUP_TIMEOUT_NS = 20_000_000_000L
    private val mobTimers = mutableMapOf<String, MutableList<MobTimer>>()
    private val pendingCocoons = mutableListOf<PendingCocoon>()

    private data class MobTimer(val startNs: Long, val hatchMs: Long? = null, val cocoon: Boolean = false)
    internal data class KillTimeMessage(
        val prefix: String?,
        val player: String,
        val elapsed: String,
        val mob: String
    )

    internal val killTimeMessageRegex: Regex by lazy {
        val rareNamePattern = DianaMobDetect.rareMobNames.joinToString("|") { Regex.escape(it) }
        Regex("^(?<prefix>.*> )?(?<player>.+?): \\[SBO\\] Took (?<elapsed>.+?) to kill the (?<mob>$rareNamePattern)!.*$")
    }

    internal fun parseKillTimeMessage(message: String): KillTimeMessage? {
        val match = killTimeMessageRegex.matchEntire(message) ?: return null
        return KillTimeMessage(
            prefix = match.groups["prefix"]?.value,
            player = match.groups["player"]?.value ?: return null,
            elapsed = match.groups["elapsed"]?.value ?: return null,
            mob = match.groups["mob"]?.value ?: return null
        )
    }
    private data class PendingCocoon(
        val mob: String,
        val caughtNs: Long,
        val knownEntityIds: IntSet
    )

    fun init() {
        Register.onTick(1) {
            trackCocoonHatches()
        }

        Register.onChatMessage(killTimeMessageRegex, noFormatting = true) { _, match ->
            val mob = match.groups["mob"]?.value ?: return@onChatMessage
            val player = match.groups["player"]?.value?.removeFormatting()?.trim() ?: return@onChatMessage
            WaypointManager.removeRareMobWaypointBySpawner(player, mob)
        }
    }

    fun onMobSpawn(mob: String, fromCocoon: Boolean) {
        if (mob !in DianaMobDetect.rareMobNames || fromCocoon) return
        mobTimers[mob] = mutableListOf(MobTimer(System.nanoTime()))
    }

    internal fun onCocoonCaught(mob: String) {
        if (mob !in DianaMobDetect.rareMobNames) return
        val knownIds = DianaMobDetect.getKnownDianaMobEntityIds()
        pendingCocoons += PendingCocoon(mob, System.nanoTime(), knownIds)
    }

    private fun trackCocoonHatches() {
        if (!World.isInHub()) {
            pendingCocoons.clear()
            mobTimers.clear()
            return
        }

        val now = System.nanoTime()
        val tracked = DianaMobDetect.getTrackedRareMobEntities()
        val iterator = pendingCocoons.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            val hatched = tracked.firstOrNull { (stand, mob) ->
                mob == pending.mob && !pending.knownEntityIds.contains(stand.id)
            }
            if (hatched != null) {
                val hatchMs = ((now - pending.caughtNs).coerceAtLeast(0L) / 1_000_000L)
                mobTimers.getOrPut(pending.mob) { mutableListOf() }.add(MobTimer(now, hatchMs, cocoon = true))
                iterator.remove()
            } else if (now - pending.caughtNs >= HATCH_LOOKUP_TIMEOUT_NS) {
                // If the nametag never becomes trackable, retain a usable kill timer
                // from CAUGHT! and clearly mark the hatch duration as unavailable.
                mobTimers.getOrPut(pending.mob) { mutableListOf() }
                    .add(MobTimer(pending.caughtNs, cocoon = true))
                iterator.remove()
            }
        }
    }

    fun onMobDeath(mob: String, nearby: Boolean) {
        val timers = mobTimers[mob] ?: return
        if (timers.isEmpty()) return
        val timer = timers.removeAt(0)
        if (timers.isEmpty()) mobTimers.remove(mob)

        val elapsedNs = System.nanoTime() - timer.startNs
        if (!nearby || !Diana.sendKillTimeMessage || elapsedNs <= 0L) return

        val elapsed = Helper.formatTime(elapsedNs / 1_000_000L, decimalSeconds = true)
        val hatch = timer.hatchMs?.let { " (cocoon hatch: ${Helper.formatTime(it, decimalSeconds = true)})" }
            ?: if (timer.cocoon) " (cocoon hatch time unavailable)" else ""
        Chat.chat("§6[SBO] §eTook §6$elapsed §eto kill the $mob!$hatch")
        val shareType = when (mob) {
            DianaMobDetect.RareDianaMob.INQ.display -> Diana.ShareList.INQ
            DianaMobDetect.RareDianaMob.MANTI.display -> Diana.ShareList.MANTICORE
            DianaMobDetect.RareDianaMob.KING.display -> Diana.ShareList.KING
            DianaMobDetect.RareDianaMob.SPHINX.display -> Diana.ShareList.SPHINX
            else -> null
        }
        if (shareType != null && shareType in Diana.ShareMobs) {
            Chat.pc("[SBO] Took $elapsed to kill the $mob!$hatch")
        }
    }

}
