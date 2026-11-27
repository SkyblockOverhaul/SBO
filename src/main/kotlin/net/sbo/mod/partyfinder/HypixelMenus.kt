package net.sbo.mod.partyfinder

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.Slot
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager

/** Reads Hypixel's chest menus and adds an SBO Party Finder button above some of them, like the Kuudra party finder and the Safari entry. */
internal object HypixelMenus {
    private const val CHEST_WIDTH = 176
    private const val CHEST_BASE_HEIGHT = 114
    private const val SLOT_SIZE = 18
    private const val ITEM_SIZE = 16
    private const val BUTTON_WIDTH = 100
    private const val BUTTON_HEIGHT = 20

    /** Names and lore lines of the menu's own items, without the player's inventory. */
    fun lines(screen: AbstractContainerScreen<*>): List<String> = items(screen).flatMap { slot ->
        listOf(slot.item.hoverName.removeFormatting()) + loreOf(slot)
    }

    fun loreOf(slot: Slot): List<String> = slot.item.get(DataComponents.LORE)?.lines.orEmpty().map { it.removeFormatting() }

    fun slotAt(screen: AbstractContainerScreen<*>, mouseX: Double, mouseY: Double): Slot? {
        val [left, top] = corner(screen) ?: return null
        return items(screen).firstOrNull { slot ->
            mouseX >= left + slot.x && mouseX < left + slot.x + ITEM_SIZE && mouseY >= top + slot.y && mouseY < top + slot.y + ITEM_SIZE
        }
    }

    /**
     * Adds a button above the menu that opens the parties of key. Hypixel fills the menu a moment after it opens,
     * so the button stays hidden until show is true for the items. onUse runs before the menu closes.
     * A right click on it turns the button off.
     */
    fun addPartyButton(
        screen: AbstractContainerScreen<*>, key: String, tooltip: String,
        onUse: () -> Unit = {}, show: (AbstractContainerScreen<*>) -> Boolean
    ) {
        if (!DataManager.partyFinderConfigState.menuButtons) return
        val [left, top] = corner(screen) ?: return
        val above = top - BUTTON_HEIGHT - 2
        val right = left + CHEST_WIDTH + 4
        // Above the menu's top left corner. Low windows have no room there for big menus, so it goes next to the menu instead.
        val [x, y] = when {
            above >= 0 -> left to above
            right + BUTTON_WIDTH <= screen.width -> right to top
            else -> left to 0
        }
        val button = Button.builder(Component.literal("SBO Party Finder")) {
            onUse()
            mc.player?.closeContainer()
            mc.schedule { PartyFinderGui.openParties(key) }
        }
            .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(
                Component.literal(tooltip)
                    .append(Component.literal("\n\nRight click to hide these buttons.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
            ))
            .build()
        button.visible = false
        Screens.getWidgets(screen).add(button)
        ScreenEvents.afterTick(screen).register {
            if (!button.visible && DataManager.partyFinderConfigState.menuButtons) button.visible = show(screen)
        }
        // The button only takes left clicks, so the right click is caught before the menu gets it
        ScreenMouseEvents.allowMouseClick(screen).register { _, click ->
            val hides = click.buttonInfo().button == InputConstants.MOUSE_BUTTON_RIGHT && button.visible && button.isMouseOver(click.x, click.y)
            if (hides) {
                button.visible = false
                turnOffButtons()
            }
            !hides
        }
    }

    private fun turnOffButtons() {
        val config = DataManager.partyFinderConfigState
        config.menuButtons = false
        config.save()
        Chat.chat("§6[SBO] §eSBO buttons in Hypixel menus are hidden. You can turn them back on in the settings of /sbopf.")
    }

    private fun items(screen: AbstractContainerScreen<*>): List<Slot> {
        val own = mc.player?.inventory
        return screen.menu.slots.filter { it.container !== own && !it.item.isEmpty }
    }

    // The screen's position fields are protected, so it is calculated the same way the screen does it
    private fun corner(screen: AbstractContainerScreen<*>): Pair<Int, Int>? {
        val rows = (screen.menu as? ChestMenu)?.rowCount ?: return null
        return (screen.width - CHEST_WIDTH) / 2 to (screen.height - CHEST_BASE_HEIGHT - rows * SLOT_SIZE) / 2
    }
}
