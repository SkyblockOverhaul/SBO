package net.sbo.mod.overlays

import net.minecraft.client.Minecraft
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import java.util.*

object Legion {
    var legionCount: Int = 0
    val overlay: Overlay = Overlay("legionOverlay", 10.0f, 10.0f, 1.0f)
    val overlayText: OverlayTextLine = OverlayTextLine("")

    fun init () {
        overlay.init()
        overlay.setCondition { General.legionOverlay }
        overlay.addLine(overlayText)
        Register.onTick(20) {
            if (!General.legionOverlay || !World.isInSkyblock()) return@onTick
            val player = mc.player ?: return@onTick
            val world = mc.level ?: return@onTick
            val nearbyPlayers = world.players()
                .filter { otherPlayer ->
                    otherPlayer != player &&

                    (otherPlayer.uuid.version() == 4 || otherPlayer.uuid.version() == 1) &&
                    getPlayerPing(mc, otherPlayer.uuid) > 0 &&
                    otherPlayer.distanceTo(player) <= 30
                }
                .distinctBy { it.uuid }
            legionCount = nearbyPlayers.size
            overlayText.text = "§e§lLegion: §b§l$legionCount"
        }
    }

    fun getPlayerPing(client: Minecraft, uuid: UUID): Int {
        return client.connection?.getPlayerInfo(uuid)?.latency ?: 0
    }
}