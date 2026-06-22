package net.sbo.mod.utils.events

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
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

        val pos = packet.pos.toSboVec()

        ArrowGuessBurrow.recentClickedBlocks.add(pos)
        processBlockInteraction(pos)
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

        processBlockInteraction(event.pos.toSboVec())
    }

    private fun processBlockInteraction(pos: SboVec) {
        val waypoint = WaypointManager.getWaypointAt(pos, "burrow") ?: WaypointManager.getWaypointAt(pos, "arrow") ?: WaypointManager.getWaypointAt(pos, "guess")
        if (waypoint != null) {
            waypoint.userInteractedWith = true
            lastWaypointClicked = waypoint.pos
        }

        val posString = "${pos.x} ${pos.y} ${pos.z}"
        if (BurrowDetector.burrows.containsKey(posString)) {
            lastBurrowClicked = pos
        }
        lastBlockClicked = pos
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
