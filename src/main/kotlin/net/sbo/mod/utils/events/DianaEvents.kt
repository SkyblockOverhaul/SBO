package net.sbo.mod.utils.events

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.game.PlayerInteractEvent
import net.sbo.mod.utils.events.impl.packets.PacketSendEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import net.sbo.mod.utils.waypoint.WaypointManager
import java.util.regex.Pattern

object DianaEvents {
    var lastBlockClicked: SboVec? = null
    var lastWaypointClicked: SboVec? = null
    var lastBurrowClicked: SboVec? = null

    internal fun init() {
        registerBurrowDug()
    }

    @SboEvent
    fun onPlayerActionSend(event: PacketSendEvent) {
        val packet = event.packet
        if (packet !is ServerboundPlayerActionPacket) return
        if (World.getWorld() != "Hub") return
        if (packet.action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return

        val waypoint = WaypointManager.getWaypointAt(packet.pos.toSboVec(), "burrow") ?: WaypointManager.getWaypointAt(packet.pos.toSboVec(), "arrow") ?: WaypointManager.getWaypointAt(packet.pos.toSboVec(), "guess")
        if (waypoint != null) {
            waypoint.userInteractedWith = true
            lastWaypointClicked = waypoint.pos
        }

        val posString = "${packet.pos.x} ${packet.pos.y} ${packet.pos.z}"
        if (BurrowDetector.burrows.containsKey(posString)) {
            lastBurrowClicked = SboVec(
                packet.pos.x.toDouble(),
                packet.pos.y.toDouble(),
                packet.pos.z.toDouble()
            )
        }

        lastBlockClicked = SboVec(
            packet.pos.x.toDouble(),
            packet.pos.y.toDouble(),
            packet.pos.z.toDouble()
        )
    }

    @SboEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != "useBlock") return
        if (World.getWorld() != "Hub") return
        val player = SBOKotlin.mc.player
        val item = player?.mainHandItem
        if (item?.isEmpty == true) return
        if (item == null || !item.hoverName.string.contains("Spade")) return
        if (event.pos ==  null) return

        val waypoint = WaypointManager.getWaypointAt(event.pos.toSboVec(), "burrow") ?: WaypointManager.getWaypointAt(event.pos.toSboVec(), "arrow") ?: WaypointManager.getWaypointAt(event.pos.toSboVec(), "guess")
        if (waypoint != null) {
            waypoint.userInteractedWith = true
            lastWaypointClicked = waypoint.pos
        }

        val posString = "${event.pos.x} ${event.pos.y} ${event.pos.z}"
        if (BurrowDetector.burrows.containsKey(posString)) {
            lastBurrowClicked = SboVec(
                event.pos.x.toDouble(),
                event.pos.y.toDouble(),
                event.pos.z.toDouble()
            )
        }

        lastBlockClicked = SboVec(
            event.pos.x.toDouble(),
            event.pos.y.toDouble(),
            event.pos.z.toDouble()
        )
    }

    private fun registerBurrowDug() {
        Register.onChatMessageCancelable(Pattern.compile("^§eYou (.*?) Griffin [Bb]urrow(.*?) §7\\((.*?)/(.*?)\\)$", Pattern.DOTALL)) { message, matchResult ->
            val currentBurrow = matchResult.group(3).toIntOrNull() ?: return@onChatMessageCancelable true
            val maxBurrow = matchResult.group(4).toIntOrNull() ?: return@onChatMessageCancelable true
            SBOEvent.emit(BurrowDugEvent(
                lastBurrowClicked,
                lastWaypointClicked,
                lastBlockClicked,
                currentBurrow,
                maxBurrow
            ))
            true
        }
    }
}
