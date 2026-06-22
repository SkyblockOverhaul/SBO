package net.sbo.mod.diana.burrows

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.DianaEvents
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.Waypoint
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import java.util.function.BooleanSupplier
import net.minecraft.core.particles.ParticleTypes as MCParticleTypes

object BurrowDetector {
    internal val burrows = ConcurrentHashMap<String, Burrow>()
    internal var lastDugOutBurrowPos: SboVec = SboVec(0.0, 0.0, 0.0)
    internal val toRemove = ConcurrentHashMap<Waypoint, BooleanSupplier>()

    fun init() {
        Register.command("sboclearburrows", "sbocb") {
            WaypointManager.removeAllOfType("world")
            WaypointManager.removeAllOfType("guess")
            WaypointManager.removeAllOfType("arrow")
            WaypointManager.removeAllOfType("rareMob")
            WaypointManager.removeAllOfType("burrow")
            burrows.clear()
            ArrowGuessBurrow.allGuesses.clear()
            Chat.chat("§6[SBO] §4Burrow Waypoints Cleared!")
        }

        Register.onChatMessage(Regex("""^§c ☠ §7You .+$""")) { message, matchResult ->
            if ("Hub" != World.getWorld()) return@onChatMessage
            refreshBurrows(true, 1)
        }

        Register.onChatMessage(Regex("""^§eYou finished the Griffin burrow chain!.*$""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // The chain finished message does not trigger it.

            // We need to update lastdugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(false, 2)

            val anyClose = WaypointManager.getAllGuessesAndBurrows().filter { it.distanceToPlayer() < 90 }
            if (Diana.showTitleWhenChainEnd && anyClose.isEmpty()) requestSpade()
        }

        Register.onChatMessage(Regex(""".*§eYou (?:just )?dug out(?!.*\(\d+/\d+\)$).*""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // Mob spawns, feather drops, and Myth the Fish use different chat messages.

            // We need to update lastdugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(false, 1, parseTypeFromChatMsg(message.getString()))
        }
    }

    private fun parseTypeFromChatMsg(message: String): String {
        if (message.contains("Griffin Feather") || message.contains(" coins!") || message.contains("Mythos Fragment") || message.contains("Braided Griffin Feather") || message.contains("Myth the Fish")) {
            return "Treasure"
        }

        return "Mob" // assume mob if not a known treasure drop
    }

    fun requestSpade() {
        Helper.showTitle("§c Use Spade!", "", 0, 30, 0)
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (!Diana.closeBurrowDetection) return
        lastDugOutBurrowPos = DianaEvents.lastWaypointClicked ?: SboVec(0.0, 0.0, 0.0)
        refreshBurrows(false, 2)
    }

    @SboEvent
    fun onParticleReceive(event: PacketReceiveEvent) {
        val packet = event.packet
        if (packet !is ClientboundLevelParticlesPacket) return
        if (!Diana.closeBurrowDetection) return
        if (World.getWorld() != "Hub") return

        if (packet.particle.type == MCParticleTypes.LARGE_SMOKE && packet.maxSpeed == 0.01f && packet.xDist == 0.0f && packet.yDist == 0.0f && packet.zDist == 0.0f) {
            val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down()
            val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
            burrows.remove(posString)

            WaypointManager.removeWaypointAt(pos, "burrow")
            WaypointManager.removeWaypointAt(pos, "arrow")
            WaypointManager.removeWaypointAt(pos, "guess")
            WaypointManager.removeWaypointAt(pos, "rareMob")
            WaypointManager.removeWaypointAt(pos, "world")
        }
        burrowDetect(packet)
    }

    private fun burrowDetect(packet: ClientboundLevelParticlesPacket) {
        val particleType = ParticleTypes.getParticleType(packet) ?: return
        val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down()

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

        registerBurrow(pos, type)
    }

    private fun registerBurrow(
        pos: SboVec,
        type: String,
        source: String = "particle",
        carriedTimesDug: Int? = null
    ) {
        if (!Diana.closeBurrowDetection) return

        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"

        if (WaypointManager.waypointExists("burrow", pos).first) return

        val burrow = burrows.getOrPut(posString) {
            Burrow(pos)
        }

        burrow.type = type

        val existingTimesDug =
            carriedTimesDug
                ?: burrows[posString]?.waypoint?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "burrow")?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "arrow")?.timesDug
                ?: WaypointManager.getWaypointAt(pos, "guess")?.timesDug
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
        WaypointManager.addWaypoint(waypoint)
    }

    fun queueRemoval(waypoint: Waypoint, condition: BooleanSupplier) {
        toRemove.put(waypoint, condition)
    }

    private fun flushRemovals() {
        toRemove.forEach { waypoint, condition ->
            if (condition.getAsBoolean()) {
                WaypointManager.removeWaypoint(waypoint)
            }
        }
    }

    fun refreshBurrows(deathOriginating: Boolean, expectedTimesDug: Int, burrowType: String? = null) {
        val pos = lastDugOutBurrowPos

        val knownWaypoint = WaypointManager.getWaypointAt(pos, "burrow")

        // Try known burrow first, then arrow, then precise
        val dugWaypoint = knownWaypoint ?: WaypointManager.getWaypointAt(pos, "arrow") ?: WaypointManager.getWaypointAt(pos, "guess")

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
            val removalCondition = (!deathOriginating && startBurrow) || (!deathOriginating && dugWaypoint.timesDug >= 2) || (death)

            if (removalCondition) {
                if (death) {
                    Chat.chat("§6[SBO] §eRemoved Mob burrow waypoint since you died.")
                }

                if (knownWaypoint != null) {
                    val posString = "${knownWaypoint.pos.x.toInt()} ${knownWaypoint.pos.y.toInt()} ${knownWaypoint.pos.z.toInt()}"
                    burrows.remove(posString)

                    val containList = ArrowGuessBurrow.allGuesses.filter { guessEntry ->
                        val current = guessEntry.getCurrent().roundLocationToBlock()
                        val interacted = knownWaypoint.pos.roundLocationToBlock()

                        current.x == interacted.x && current.y == interacted.y && current.z == interacted.z
                    }

                    ArrowGuessBurrow.allGuesses.removeAll(containList.toSet())
                }

                WaypointManager.removeWaypoint(dugWaypoint)
            }
        }

        // Counted timesDug above already
        flushRemovals()

        if (dugWaypoint?.type != "burrow" && burrowType != null) {
            // The user dug a Guess waypoint before particles updated it into a real burrow type.
            // We promote it into a real burrow and preserve progression state.
            registerBurrow(
                pos = pos,
                type = burrowType,
                source = "chat",
                carriedTimesDug = dugWaypoint?.timesDug ?: expectedTimesDug
            )

            if (dugWaypoint != null) {
                WaypointManager.removeWaypoint(dugWaypoint)
            }
        }
    }
}
