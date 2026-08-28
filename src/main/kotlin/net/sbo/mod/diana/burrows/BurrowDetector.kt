package net.sbo.mod.diana.burrows

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.DianaEvents
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier
import net.minecraft.core.particles.ParticleTypes as MCParticleTypes

object BurrowDetector {
    internal val burrows = ConcurrentHashMap<String, Burrow>()
    private var lastDugOutBurrowPos: SboVec = SboVec(0.0, 0.0, 0.0)
    private val toRemove = ConcurrentHashMap<Waypoint, BooleanSupplier>()

    private val RECENTLY_REMOVED_DURATION_NS = TimeUnit.SECONDS.toNanos(1)
    private val recentlyRemoved = ConcurrentHashMap<String, Long>()

    private val CHAIN_DURATION_NS = TimeUnit.MINUTES.toNanos(30L)

    private val chainExpirations = ArrayDeque<Long>()

    var pendingUseSpadeTitle: String? = null

    val chains: Int
        get() {
            purgeExpiredChains()
            return chainExpirations.size
        }

    private fun purgeExpiredChains() {
        val now = System.nanoTime()

        while (chainExpirations.isNotEmpty() && chainExpirations.first() <= now) {
            chainExpirations.removeFirst()
        }
    }

    private fun markRecentlyRemoved(pos: SboVec) {
        val key = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
        recentlyRemoved[key] = System.nanoTime()
    }

    private fun wasRecentlyRemoved(posString: String): Boolean {
        val removedAt = recentlyRemoved[posString] ?: return false

        val elapsed = System.nanoTime() - removedAt
        if (elapsed <= RECENTLY_REMOVED_DURATION_NS) {
            return true
        }

        // Expired; clean it up so the map doesn't grow forever.
        recentlyRemoved.remove(posString, removedAt)
        return false
    }

    fun init() {
        Register.command("sboclearburrows", "sbocb") {
            WaypointManager.removeAllOfType("world")
            WaypointManager.removeAllOfType("guess")
            WaypointManager.removeAllOfType("arrow")
            WaypointManager.removeAllOfType("subGuess")
            WaypointManager.removeAllOfType("rareMob")
            WaypointManager.removeAllOfType("burrow")
            WaypointManager.removeAllOfType("debug")
            burrows.clear()
            recentlyRemoved.clear()
            ArrowGuessBurrow.allGuesses.clear()
            chainExpirations.clear()
            Chat.chat("§6[SBO] §4Burrow Waypoints Cleared!")
        }

        Register.command("sbodebugburrows") { // TODO: Maybe remove at some point once we're fully sure internal state cannot bug out
            for (known in burrows.values) {
                if (!WaypointManager.waypointExists("burrow", known.pos).first) {
                    val wayp = Waypoint("Internal Known (Debug)", known.pos.x, known.pos.y, known.pos.z, ttl = 30, type = "debug")
                    wayp.preventInvalidRemoval = true

                    WaypointManager.addWaypoint(wayp)
                }
            }

            for (arrow in ArrowGuessBurrow.allGuesses) {
                val curr = arrow.getCurrent()
                if (!WaypointManager.waypointExists("arrow", curr).first) {
                    val wayp = Waypoint("Internal Arrow (Debug)", curr.x, curr.y, curr.z, ttl = 30, type = "debug")
                    wayp.preventInvalidRemoval = true

                    WaypointManager.addWaypoint(wayp)
                }
            }
        }

        Register.onChatMessage(Regex("""^§c ☠ §7You .+$""")) { message, matchResult ->
            if ("Hub" != World.getWorld()) return@onChatMessage
            refreshBurrows(deathOriginating = true, expectedTimesDug = 1)
        }

        Register.onChatMessage(Regex("""^§eYou finished the Griffin burrow chain!.*$""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // The chain finished message does not trigger it.
            chainFinish()

            // We need to update lastdugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(deathOriginating = false, expectedTimesDug = 2)

            val anyClose = WaypointManager.getAllGuessesAndBurrows().filter { it.distanceToPlayer() < 90 }
            if (Diana.showTitleWhenChainEnds && anyClose.isEmpty()) requestSpade("chain")
        }

        Register.onChatMessage(Regex(""".*§eYou (?:just )?dug out(?!.*\(\d+/\d+\)$).*""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // Mob spawns, feather drops, and Myth the Fish use different chat messages.

            // We need to update lastDugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(
                deathOriginating = false,
                expectedTimesDug = 1,
                burrowType = parseTypeFromChatMsg(message.string)
            )
        }

        Register.onTick(1) {
            pendingUseSpadeTitle?.let {
                Helper.showTitle(it, "", 0, 1, 0)
            }
        }
    }

    private fun chainFinish() {
        purgeExpiredChains()

        if (chainExpirations.isNotEmpty()) {
            chainExpirations.removeFirst()
        }
    }

    private fun chainStart() {
        purgeExpiredChains()
        chainExpirations.addLast(System.nanoTime() + CHAIN_DURATION_NS)
    }

    private fun refreshChainTimer() {
        purgeExpiredChains()

        if (chainExpirations.isEmpty()) {
            return
        }

        chainExpirations.removeFirst()
        chainExpirations.addLast(System.nanoTime() + CHAIN_DURATION_NS)
    }

    private fun parseTypeFromChatMsg(message: String): String {
        if ("Griffin Feather" in message || " coins!" in message || "Mythos Fragment" in message || "Braided Griffin Feather" in message || "Myth the Fish" in message) {
            return "Treasure"
        }

        return "Mob" // assume mob if not a known treasure drop
    }

    fun requestSpade(reason: String) {
        if (!Diana.spadeGuess || !Helper.hasSpade || World.getWorld() != "Hub") return

        val color = if (reason == "failure") "c" else "e"

        pendingUseSpadeTitle = "§${color}Use Spade!"
        Chat.chat("§6[SBO] §${color}Use spade!")
    }

    fun removeFromInternalState(pos: SboVec) {
        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
        burrows.remove(posString)
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        when {
            event.currentBurrow == 1 -> chainStart()

            event.currentBurrow != event.maxBurrow -> refreshChainTimer()

            // last burrow: do nothing
            // chainFinish() is already handled by chat
        }

        if (!Diana.closeBurrowDetection) return
        lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
        refreshBurrows(deathOriginating = false, expectedTimesDug = 2)
    }

    @SboEvent
    fun onParticleReceive(event: PacketReceiveEvent) {
        val packet = event.packet
        if (packet !is ClientboundLevelParticlesPacket) return
        if (!Diana.closeBurrowDetection) return
        if (World.getWorld() != "Hub") return

        if (packet.particle.type == MCParticleTypes.LARGE_SMOKE && packet.maxSpeed == 0.01f && packet.xDist == 0.0f && packet.yDist == 0.0f && packet.zDist == 0.0f) {
            val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down()

            markRecentlyRemoved(pos)
            removeFromInternalState(pos)

            WaypointManager.removeWaypointAt(pos, "burrow")
            WaypointManager.removeWaypointAt(pos, "arrow")
            WaypointManager.removeWaypointAt(pos, "subGuess")
            WaypointManager.removeWaypointAt(pos, "guess")
            WaypointManager.removeWaypointAt(pos, "rareMob")
            WaypointManager.removeWaypointAt(pos, "world")
            WaypointManager.removeWaypointAt(pos, "debug")

            ArrowGuessBurrow.removeSubGuessFromInternalState(pos)
            ArrowGuessBurrow.removeOrMoveFromInternalState(pos)

            return
        }

        burrowDetect(packet)
    }

    private fun burrowDetect(packet: ClientboundLevelParticlesPacket) {
        val particleType = ParticleTypes.getParticleType(packet) ?: return
        val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down()
        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"

        if (wasRecentlyRemoved(posString)) return

        val type = when (particleType) {
            "FOOTSTEP" -> {
                burrows.getOrPut("${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}") { Burrow(pos) }.hasFootstep = true
                return
            }
            "ENCHANT" -> {
                burrows.getOrPut("${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}") { Burrow(pos) }.hasEnchant = true
                return
            }
            "EMPTY" -> "Start"
            "MOB" -> "Mob"
            "TREASURE" -> "Treasure"
            else -> return
        }

        registerBurrow(pos, type, "particle")
    }

    private fun registerBurrow(
        pos: SboVec,
        type: String,
        source: String,
        carriedTimesDug: Int? = null
    ) {
        if (!Diana.closeBurrowDetection) return

        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"

        if (WaypointManager.waypointExists("burrow", pos).first) return

        val burrow = burrows.getOrPut(posString) {
            Burrow(pos)
        }

        burrow.type = type

        ArrowGuessBurrow.removeArrowGuessFromSubGuess(pos)
        ArrowGuessBurrow.removeFromInternalState(pos)
        WaypointManager.removeWaypointAt(pos, "guess")

        val existingTimesDug =
            carriedTimesDug
                ?: burrows[posString]?.waypoint?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "burrow")?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "arrow")?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "guess")?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "subGuess")?.timesDug
                ?: 0

        val existing = burrow.waypoint

        if (existing != null) {
            existing.timesDug = existingTimesDug
            return
        }

        val waypoint = Waypoint(
            type,
            pos.x, pos.y, pos.z,
            type = "burrow"
        )

        waypoint.timesDug = existingTimesDug

        burrow.waypoint = waypoint
        WaypointManager.addWaypoint(waypoint, source == "particle")
    }

    fun queueRemoval(waypoint: Waypoint, condition: BooleanSupplier) {
        toRemove[waypoint] = condition
    }

    private fun flushRemovals() {
        toRemove.forEach { (waypoint, condition) ->
            if (condition.asBoolean) {
                WaypointManager.removeWaypoint(waypoint)
            }
        }
    }

    private fun refreshBurrows(deathOriginating: Boolean, expectedTimesDug: Int, burrowType: String? = null) {
        val pos = lastDugOutBurrowPos

        val knownWaypoint = WaypointManager.getWaypointAt(pos, "burrow")

        // Try known burrow first, then arrow, then precise
        val dugWaypoint = knownWaypoint ?: WaypointManager.getWaypointAt(pos, "arrow") ?: WaypointManager.getWaypointAt(pos, "guess") ?: WaypointManager.getWaypointAt(pos, "subGuess")

        if (null != dugWaypoint) {
            // Will only work if known burrow
            val startBurrow = dugWaypoint.text == "Start"
            val mobBurrow = dugWaypoint.text == "Mob" || burrowType == "Mob"

            // Count if not death originating and not start burrow (will still count if was a guess and not known burrow)
            val shouldCount = !deathOriginating && !startBurrow

            if (shouldCount) {
                dugWaypoint.timesDug++
            }

            if (dugWaypoint.timesDug != expectedTimesDug) {
                // Fix up the times dug from chat message in case it ever desyncs
                dugWaypoint.timesDug = expectedTimesDug
            }

            // Remove if:
            // 1. Not death originating (was called from BurrowDugEvent) and the burrow type is Start (Start burrows only need to be dug once)
            // 2. Not death originating (was called from BurrowDugEvent) and times dug is 2 or more (Treasure/Mob burrow that was fully dug out)
            // 3. Death originating (was called after self player dies) and last dug burrows times dug is 1 or more, and it was a Mob burrow (A mob burrow was dug once and the player died to the mob)
            val death = deathOriginating && dugWaypoint.timesDug >= 1 && mobBurrow
            val removalCondition = !deathOriginating && startBurrow || !deathOriginating && dugWaypoint.timesDug >= 2 || death

            if (removalCondition) {
                if (death) {
                    chainFinish() // dying finishes (fails) a chain
                    Chat.chat("§6[SBO] §eRemoved Mob burrow waypoint since you died.")
                }

                markRecentlyRemoved(dugWaypoint.pos)

                if (knownWaypoint != null) {
                    removeFromInternalState(knownWaypoint.pos)
                }

                ArrowGuessBurrow.removeArrowGuessFromSubGuess(dugWaypoint.pos)
                ArrowGuessBurrow.removeFromInternalState(dugWaypoint.pos)

                WaypointManager.removeWaypoint(dugWaypoint)
            }
        }

        // Counted timesDug above already
        flushRemovals()

        if (dugWaypoint != null && (dugWaypoint.type == "guess" || dugWaypoint.type == "arrow" || dugWaypoint.type == "subGuess") && burrowType != null) {
            // The user dug a Guess waypoint before particles updated it into a real burrow type.
            // We promote it into a real burrow and preserve progression state.
            registerBurrow(
                pos = pos,
                type = burrowType,
                source = "chat",
                carriedTimesDug = expectedTimesDug
            )

            markRecentlyRemoved(dugWaypoint.pos)
            WaypointManager.removeWaypoint(dugWaypoint)
        }
    }
}
