package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.burrows.BurrowDetector
import net.sbo.mod.diana.guesses.ArrowGuessBurrow
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.diana.BurrowDugEvent
import net.sbo.mod.utils.events.impl.game.PlayerInteractEvent
import net.sbo.mod.utils.game.ItemLookup
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.waypoint.WaypointManager
import net.minecraft.world.InteractionResult
import net.sbo.mod.utils.math.SboVec.Companion.toSboVec
import java.util.regex.Pattern

object DianaEvents {
    private var lastBlockClicked: SboVec? = null
    var lastWaypointClicked: SboVec? = null
    private var lastBurrowClicked: SboVec? = null

    internal fun init() {
        registerBurrowDug()
        AttackBlockCallback.EVENT.register { player, _, _, pos, _ ->
            if (player === SBOKotlin.mc.player && World.isInHub()) {
                val blockPos = pos.toSboVec()
                ArrowGuessBurrow.recentClickedBlocks.add(blockPos)
                processBlockInteraction(blockPos)
            }
            InteractionResult.PASS
        }
    }

    @SboEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != "useBlock") return
        if (!World.isInHub()) return
        val item = event.player.mainHandItem
        if (item.isEmpty) return
        val sbId = ItemLookup(item).sbId
        if (sbId != "DEIFIC_SPADE" && sbId != "ARCHAIC_SPADE" && sbId != "ANCESTRAL_SPADE") return
        if (event.pos ==  null) return

        processBlockInteraction(event.pos.toSboVec())
    }

    private fun processBlockInteraction(pos: SboVec) {
        val waypoint = WaypointManager.getWaypointAt(pos, "burrow") ?: WaypointManager.getWaypointAt(pos, "arrow") ?: WaypointManager.getWaypointAt(pos, "guess") ?: WaypointManager.getWaypointAt(pos, "subGuess")
        if (waypoint != null) {
            waypoint.userInteractedWith = true
            lastWaypointClicked = waypoint.pos
        }

        val posString = "${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
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
