package net.sbo.mod.diana.burrows

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.collection.EvictingQueue
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
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import net.minecraft.core.particles.ParticleTypes as MCParticleTypes

object BurrowDetector {
    internal val burrows = ConcurrentHashMap<String, Burrow>()
    internal var lastDugOutBurrowPos: SboVec = SboVec(0.0, 0.0, 0.0)
    internal val burrowsHistory = EvictingQueue<String>(2)

    fun init() {
        Register.command("sboclearburrows", "sbocb") {
            WaypointManager.removeAllOfType("world")
            WaypointManager.removeAllOfType("guess")
            WaypointManager.removeAllOfType("arrow")
            WaypointManager.removeAllOfType("rareMob")
            resetBurrows()
            Chat.chat("§6[SBO] §4Burrow Waypoints Cleared!")
        }

        Register.onChatMessage(Regex("""^ ☠ You .+$"""), noFormatting = true) { message, matchResult ->
            if ("Hub" != World.getWorld()) return@onChatMessage
            Chat.chat("§6[SBO] §eRemoved Mob burrow waypoint since you died.")
            refreshBurrows(true)
        }

        Register.onChatMessage(Regex("""^§eYou finished the Griffin burrow chain!.*$""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // The chain finished message does not trigger it.

            // We need to update lastdugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastBurrowClicked ?: DianaEvents.lastBlockClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(false)

            val anyClose = WaypointManager.getAllGuessesAndBurrows().filter { it.distanceToPlayer() < 90 }
            if (Diana.showTitleWhenChainEnd && anyClose.isEmpty()) requestSpade()
        }

        Register.onChatMessage(Regex(""".*§eYou (?:just )?dug out(?!.*\(\d+/\d+\)$).*""")) { message, matchResult ->
            // BurrowDugEvent only triggers when you get the "You dug out a Griffin Burrow!" message.
            // Mob spawns, feather drops, and Myth the Fish use different chat messages.

            // We need to update lastdugOutBurrowPos manually here since BurrowDugEvent does not set it since it is not triggered.
            lastDugOutBurrowPos = DianaEvents.lastBurrowClicked ?: DianaEvents.lastBlockClicked ?: SboVec(0.0, 0.0, 0.0)
            refreshBurrows(false)
        }
    }

    fun requestSpade() {
        Helper.showTitle("§c Use Spade!", "", 0, 30, 0)
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (!Diana.closeBurrowDetection) return
        if (event.burrowPos == null) return
        lastDugOutBurrowPos = event.burrowPos
        refreshBurrows(false)
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!Diana.closeBurrowDetection) return
        resetBurrows()
    }

    @SboEvent
    fun onParticleReceive(event: PacketReceiveEvent) {
        val packet = event.packet
        if (packet !is ClientboundLevelParticlesPacket) return
        if (!Diana.closeBurrowDetection) return
        if (World.getWorld() != "Hub") return

        if (packet.particle.type == MCParticleTypes.LARGE_SMOKE && packet.maxSpeed == 0.01f && packet.xDist == 0.0f && packet.yDist == 0.0f && packet.zDist == 0.0f) {
            val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down(1.0)
            WaypointManager.removeWaypointAt(pos, "burrow")
            WaypointManager.removeWaypointAt(pos, "rareMob")
        }
        burrowDetect(packet)
    }

    private fun burrowDetect(packet: ClientboundLevelParticlesPacket) {
        val particleType = ParticleTypes.getParticleType(packet) ?: return
        val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down()
        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"

        if (burrowsHistory.contains(posString)) return
        if (!burrows.containsKey(posString)) burrows[posString] = Burrow(pos)

        when (particleType) {
            "FOOTSTEP" -> burrows[posString]?.hasFootstep = true
            "ENCHANT" -> burrows[posString]?.hasEnchant = true
            "EMPTY" -> burrows[posString]?.type = "Start"
            "MOB" -> burrows[posString]?.type = "Mob"
            "TREASURE" -> burrows[posString]?.type = "Treasure"
        }

        val burrow = burrows[posString]
        if (burrow?.type != null && burrow.waypoint == null) {
            burrowsHistory.add(posString)
            burrow.waypoint = Waypoint(
                burrow.type!!,
                pos.x, pos.y, pos.z,
                type = "burrow"
            )
            WaypointManager.addWaypoint(burrow.waypoint!!)
        }
    }

    fun refreshBurrows(deathOriginating: Boolean) {
        val dugWaypoint = WaypointManager.getWaypointAt(lastDugOutBurrowPos, "burrow") ?: return
        val startBurrow = dugWaypoint.text == "Start"
        val mobBurrow = dugWaypoint.text == "Mob"
        val shouldCount = !deathOriginating && !startBurrow

        if (shouldCount) {
            dugWaypoint.timesDug++
        }

        // Remove if:
        // 1. Not death originating (was called from BurrowDugEvent) and the burrow type is Start (Start burrows only need to be dug once)
        // 2. Not death originating (was called from BurrowDugEvent) and times dug is 2 or more (Treasure/Mob burrow that was fully dug out)
        // 3. Death originating (was called after self player dies) and last dug burrows times dug is 1 or more, and it was a Mob burrow (A mob burrow was dug once and the player died to the mob)
        val removalCondition = (!deathOriginating && startBurrow) || (!deathOriginating && dugWaypoint.timesDug >= 2) || (deathOriginating && dugWaypoint.timesDug >= 1 && mobBurrow)

        if (removalCondition) {
            WaypointManager.removeWaypoint(dugWaypoint)
        }
    }

    fun resetBurrows() {
        WaypointManager.removeAllOfType("burrow")
        burrows.clear()
        burrowsHistory.clear()
    }
}
