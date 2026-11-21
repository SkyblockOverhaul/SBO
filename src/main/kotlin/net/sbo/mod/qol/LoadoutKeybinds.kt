package net.sbo.mod.qol

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.ContainerInput
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.config.KeybindEntry
import net.sbo.mod.settings.categories.Keybinds

/**
 * Keys to equip a loadout or change the page in Hypixel's Loadouts menu, without moving the mouse.
 *
 * Slot layout and page buttons like Odin's Loadout Keybinds (https://github.com/odtheking/Odin), skipping locked
 * and not yet customized loadouts and the 200 ms click cooldown like SkyHanni's (https://github.com/hannibal002/SkyHanni).
 */
object LoadoutKeybinds {
    /** The menu's title, "(1/3) Loadouts": the current page and the page count. */
    private val TITLE = Regex("""\((\d+)/(\d+)\) Loadouts?""")

    /** The loadout icons of a page: 3 per row, 4 rows on the right side of the menu. */
    private val LOADOUT_SLOTS = intArrayOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43)
    private const val PREVIOUS_PAGE_SLOT = 17
    private const val NEXT_PAGE_SLOT = 44
    private const val CLICK_COOLDOWN_MS = 200L

    private val LOCKED = Regex("""Loadout \d+ Locked""")
    private const val UNUSED = "You must customize this loadout"

    private var lastClick = 0L

    fun init() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is AbstractContainerScreen<*>) return@register
            // Returning false keeps the key from also doing its normal thing (hotbar swap, closing...)
            ScreenKeyboardEvents.allowKeyPress(screen).register { _, event -> !press(screen, InputConstants.getKey(event)) }
            ScreenMouseEvents.allowMouseClick(screen).register { _, event ->
                !press(screen, InputConstants.Type.MOUSE.getOrCreate(event.button()))
            }
        }
    }

    /**
     * Clicks the loadout or page button [pressed] is bound to.
     *
     * @return true when [pressed] is a loadout key and [screen] the Loadouts menu, the key then does nothing else
     */
    private fun press(screen: AbstractContainerScreen<*>, pressed: InputConstants.Key): Boolean {
        if (!Keybinds.loadoutKeybinds || pressed == InputConstants.UNKNOWN) return false
        val page = TITLE.find(screen.title.string) ?: return false
        val current = page.groupValues[1].toIntOrNull() ?: return false
        val max = page.groupValues[2].toIntOrNull() ?: return false
        fun matches(keyName: String) = KeybindEntry.keyOf(keyName) == pressed

        val slot = when {
            matches(Keybinds.loadoutNextPageKey) -> if (current < max) NEXT_PAGE_SLOT else return true
            matches(Keybinds.loadoutPreviousPageKey) -> if (current > 1) PREVIOUS_PAGE_SLOT else return true
            else -> {
                val index = Keybinds.loadoutKeys.indexOfFirst(::matches)
                if (index == -1) return false
                LOADOUT_SLOTS[index].takeIf { usable(screen, it) } ?: return true
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastClick < CLICK_COOLDOWN_MS) return true
        lastClick = now

        val player = mc.player ?: return true
        mc.gameMode?.handleContainerInput(screen.menu.containerId, slot, 0, ContainerInput.PICKUP, player)
        return true
    }

    /** False for an empty, locked or not yet customized loadout in [slot]. */
    private fun usable(screen: AbstractContainerScreen<*>, slot: Int): Boolean {
        val stack = screen.menu.slots.getOrNull(slot)?.item ?: return false
        if (stack.isEmpty || LOCKED.containsMatchIn(stack.hoverName.string)) return false
        val lore = stack.get(DataComponents.LORE)?.lines ?: return true
        return lore.none { UNUSED in it.string }
    }
}
