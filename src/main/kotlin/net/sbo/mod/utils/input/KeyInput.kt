package net.sbo.mod.utils.input

import com.mojang.blaze3d.platform.InputConstants
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.config.KeybindEntry

/**
 * Whether a key from the Keybinds settings is held, like SkyHanni's KeyboardManager: keyboard keys are read from the
 * window, mouse buttons are remembered from what Minecraft passes to its own key mappings (KeyMappingMixin).
 * Only while no screen (chat, inventory...) is open, like Minecraft's own keys.
 */
object KeyInput {
    private val mouseDown = HashSet<Int>()

    /** Called by KeyMappingMixin for every key and mouse button Minecraft presses or releases outside of screens. */
    @JvmStatic
    fun onKeyMappingSet(key: InputConstants.Key, down: Boolean) {
        if (key.type != InputConstants.Type.MOUSE) return
        if (down) mouseDown += key.value else mouseDown -= key.value
    }

    /** Whether the key named [keyName] ("key.keyboard.g") is held, always false while a screen is open. */
    fun isDown(keyName: String): Boolean = isDown(KeybindEntry.keyOf(keyName))

    /** Whether [key] is held, always false while a screen is open. */
    fun isDown(key: InputConstants.Key): Boolean {
        // A button released while a screen was open never reaches the key mappings
        if (mc.screen != null) {
            mouseDown.clear()
            return false
        }
        if (key == InputConstants.UNKNOWN) return false
        return when (key.type) {
            InputConstants.Type.MOUSE -> key.value in mouseDown
            //#if MC >= 26.3
            //$$ InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(key.value)
            //#else
            InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(mc.window, key.value)
            //#endif
            //#if MC < 26.3
            else -> false
            //#endif
        }
    }
}
