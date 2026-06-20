package net.sbo.mod.utils

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.waypoint.WaypointManager
import org.lwjgl.glfw.GLFW

object SboKeyBinds {
    private data class KeyPressState(var isHeldDown: Boolean = false, var lastActivation: Long = 0)
    private val keyStates = mutableMapOf<String, KeyPressState>()
    private val SBO_CATEGORY = KeyMapping.Category(SBOKotlin.id(owner = "sbo-kotlin", path = "keybinds"))

    fun init() {
        register()
        registerKeyBindListener()
    }

    val guessWarpKey: KeyMapping = KeyMapping(
        "key.sbo-kotlin.guess_warp",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        SBO_CATEGORY
    )

    val inqWarpKey: KeyMapping = KeyMapping(
        "key.sbo-kotlin.inq_warp",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        SBO_CATEGORY
    )

    val generalWarpKey: KeyMapping = KeyMapping(
        "key.sbo-kotlin.general_warp",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        SBO_CATEGORY
    )

    val sendCoordsKey: KeyMapping = KeyMapping(
        "key.sbo-kotlin.send_coords",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        SBO_CATEGORY
    )

    fun register() {
        KeyMappingHelper.registerKeyMapping(guessWarpKey)
        KeyMappingHelper.registerKeyMapping(inqWarpKey)
        KeyMappingHelper.registerKeyMapping(generalWarpKey)
        KeyMappingHelper.registerKeyMapping(sendCoordsKey)
    }

    private fun handlePressAction(keyBinding: KeyMapping, action: () -> Unit) {
        handlePressAction(keyBinding, 0L, action)
    }

    private fun handlePressAction(keyBinding: KeyMapping, cooldownMillis: Long, action: () -> Unit) {
        val state = keyStates.getOrPut(keyBinding.name) { KeyPressState() }

        if (keyBinding.isDown) {
            val currentTime = System.currentTimeMillis()
            if (!state.isHeldDown && currentTime - state.lastActivation > cooldownMillis) {
                action()
                state.lastActivation = currentTime
                state.isHeldDown = true
            }
        } else {
            state.isHeldDown = false
        }
    }

    fun registerKeyBindListener() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { _: Minecraft ->
            handlePressAction(guessWarpKey) {
                WaypointManager.warpToGuess()
            }

            handlePressAction(inqWarpKey) {
                WaypointManager.warpToInq()
            }

            handlePressAction(generalWarpKey) {
                WaypointManager.warpBoth()
            }

            handlePressAction(sendCoordsKey, 500L) {
                val playerPos = Player.getLastPosition()
                Chat.say("x: ${playerPos.x.toInt()}, y: ${playerPos.y.toInt() - 1}, z: ${playerPos.z.toInt()} ") // patcher sendcoords format
            }
        })
    }
}
