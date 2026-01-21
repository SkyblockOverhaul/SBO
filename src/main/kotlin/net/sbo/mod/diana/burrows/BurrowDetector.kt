package net.sbo.mod.diana.burrows

import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.util.math.BlockPos
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.events.Register
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.collection.EvictingQueue
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.minecraft.particle.ParticleTypes as MCParticleTypes
import net.sbo.mod.utils.waypoint.Waypoint
import java.awt.Color
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.game.World
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object BurrowDetector {
    internal val burrows = ConcurrentHashMap<String, Burrow>()
    internal var removePos: SboVec = SboVec(0.0, 0.0, 0.0)
    internal val burrowsHistory = EvictingQueue<String>(2)

    fun init() {
        Register.command("sboclearburrows", "sbocb") {
            WaypointManager.removeAllOfType("world")
            WaypointManager.removeAllOfType("guess")
            WaypointManager.removeAllOfType("arrow")
            WaypointManager.guessWp?.hide()
            resetBurrows()
            Chat.chat("§6[SBO] §4Burrow Waypoints Cleared!")
        }

        Register.onChatMessageCancable(Pattern.compile("^ ☠ You (.*?)$", Pattern.DOTALL)) { message, matchResult ->
            if (World.getWorld() != "Hub") return@onChatMessageCancable true
            refreshBurrows()
            true
        }
    }

    @SboEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (!Diana.dianaBurrowDetect) return
        if (event.burrowPos == null) return
        removePos = event.burrowPos
        refreshBurrows()
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!Diana.dianaBurrowDetect) return
        resetBurrows()
    }

    @SboEvent
    fun onParticleReceive(event: PacketReceiveEvent) {
        val packet = event.packet
        if (packet !is ParticleS2CPacket) return
        if (!Diana.dianaBurrowDetect) return
        if (World.getWorld() != "Hub") return

        if (packet.parameters.type == MCParticleTypes.LARGE_SMOKE && packet.speed == 0.01f && packet.offsetX == 0.0f && packet.offsetY == 0.0f && packet.offsetZ == 0.0f) {
            val pos = SboVec(packet.x, packet.y, packet.z).roundLocationToBlock().down(1.0)
            WaypointManager.removeWaypointAt(pos, "burrow")
            WaypointManager.removeWaypointAt(pos, "rareMob")
        }
        burrowDetect(packet)
    }

    private fun getRGB(type: String): Color {
        return when (type) {
            "Start" -> Color(Customization.StartColor)
            "Mob" -> Color(Customization.MobColor)
            "Treasure" -> Color(Customization.TreasureColor)
            else -> Color(255, 255, 255)
        }
    }

    private fun burrowDetect(packet: ParticleS2CPacket) {
        val particleType = ParticleTypes.getParticleType(packet) ?: return
        val pos = SboVec(packet.x, packet.y - 1.0, packet.z).roundLocationToBlock()
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
            val color = getRGB(burrow.type!!)
            burrow.waypoint = Waypoint(
                burrow.type!!,
                pos.x, pos.y, pos.z,
                color.red.toFloat() / 255, color.green.toFloat() / 255, color.blue.toFloat() / 255,
                type = "burrow"
            )
            WaypointManager.addWaypoint(burrow.waypoint!!)
        }
    }

    fun refreshBurrows() {
        WaypointManager.removeWaypointAt(removePos, "burrow")
        val playerPos = Player.getLastPosition()
        if (WaypointManager.guessWp != null && WaypointManager.guessWp!!.pos.distanceTo(playerPos) < 4) {
            WaypointManager.guessWp?.hide()
        }
    }

    fun resetBurrows() {
        WaypointManager.removeAllOfType("burrow")
        burrows.clear()
        burrowsHistory.clear()
    }
}