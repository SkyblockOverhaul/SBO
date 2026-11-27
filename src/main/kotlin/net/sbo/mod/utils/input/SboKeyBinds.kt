package net.sbo.mod.utils.input

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guide.DianaGuideGui
import net.sbo.mod.diana.sphinx.SphinxSolver
import net.sbo.mod.guis.HubGui
import net.sbo.mod.settings.categories.Keybinds
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.TickEvent
import net.sbo.mod.utils.waypoint.WaypointManager

/** The keys from the Keybinds settings that work outside of menus, each fires once per press. */
object SboKeyBinds {
    /**
     * @property key the name of the bound key, read from the settings every tick
     * @property cooldownMs the least time between two runs, also when pressed again
     */
    private class Action(val key: () -> String, val cooldownMs: Long = 0L, val run: () -> Unit) {
        var held = false
        var lastRun = 0L
    }

    private val actions = listOf(
        Action({ Keybinds.rareMobWarpKey }) { WaypointManager.warpToRareMob() },
        Action({ Keybinds.guessWarpKey }) { WaypointManager.warpToGuess() },
        Action({ Keybinds.generalWarpKey }) { WaypointManager.warpBoth() },
        Action({ Keybinds.sendCoordsKey }, cooldownMs = 500L) {
            val playerPos = Player.getLastPosition()
            Chat.say("x: ${playerPos.x.toInt()}, y: ${playerPos.y.toInt() - 1}, z: ${playerPos.z.toInt()} ") // patcher sendcoords format
        },
        Action({ Keybinds.sphinxSolverKey }) { SphinxSolver.solve() },
        Action({ Keybinds.clearRareMobWorldWaypointsKey }) { WaypointManager.removeRareMobAndWorldWaypoints() },
        Action({ Keybinds.dianaGuideKey }) { if (mc.screen == null) DianaGuideGui.open() },
    )

    @SboEvent
    fun onTick(event: TickEvent) {
        for (action in actions) {
            val down = KeyInput.isDown(action.key())
            if (down && !action.held) {
                val now = System.currentTimeMillis()
                if (now - action.lastRun >= action.cooldownMs) {
                    action.lastRun = now
                    action.run()
                }
            }
            action.held = down
        }
    }

    /**
     * The button KeyBindsScreenMixin adds to Minecraft's Key Binds screen, the SBO keys used to be there.
     *
     * @param screen the Key Binds screen, back in the settings returns to it
     */
    @JvmStatic
    fun controlsButton(screen: Screen): Button =
        Button.builder(Component.translatable("sbo.controls.keybinds")) { HubGui.openSettings(Keybinds) { mc.setScreen(screen) } }
            .tooltip(Tooltip.create(Component.translatable("sbo.controls.keybinds.tooltip")))
            .build()
}
