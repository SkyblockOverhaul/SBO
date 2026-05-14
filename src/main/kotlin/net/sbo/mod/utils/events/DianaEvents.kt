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
import java.util.regex.Pattern

object DianaEvents {
    private var lastBlockClicked: SboVec? = null
    private var lastBurrowClicked: SboVec? = null

    internal fun init() {
        registerBurrowDug()
    }

    @SboEvent
    fun onPlayerActionSend(event: PacketSendEvent) {
        val packet = event.packet
        if (packet !is ServerboundPlayerActionPacket) return
        if (World.getWorld() != "Hub") return
        if (packet.action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return

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
        Register.onChatMessageCancable(Pattern.compile("^§eYou (.*?) Griffin [Bb]urrow(.*?) §7\\((.*?)/(.*?)\\)$", Pattern.DOTALL)) { message, matchResult ->
            val currentBurrow = matchResult.group(3).toIntOrNull() ?: return@onChatMessageCancable true
            val maxBurrow = matchResult.group(4).toIntOrNull() ?: return@onChatMessageCancable true
            SBOEvent.emit(BurrowDugEvent(
                lastBurrowClicked,
                lastBlockClicked,
                currentBurrow,
                maxBurrow
            ))
            true
        }
    }
}