package net.sbo.mod.diana.guide.menus

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.item.ItemStack
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.guide.RuleMatch
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent
import net.sbo.mod.utils.game.ItemLookup

/**
 * One rule of the Hypixel Autopet menu.
 *
 * @property trigger the item name like "On Enter Combat"
 * @property condition the line under "When:" like "You enter combat."
 * @property except the line under "Except if:" or null without one
 * @property pet the pet name without its level like "Griffin"
 * @property heldItem the held item name like "Hephaestus Relic" or null without one
 */
internal data class AutopetRule(
    val trigger: String,
    val condition: String,
    val except: String?,
    val pet: String,
    val heldItem: String?,
    val enabled: Boolean
)

/** Whether a guide pet rule is in the Autopet menu of the player. */
internal enum class RuleState { SET_UP, DISABLED, MISSING, UNKNOWN }

/**
 * Reads the pet rules of the player from the Autopet menu (/pets and then Autopet). It reads every time the menu opens.
 * That costs one title check per menu and one pass over the menu items. The rules are kept until the game closes.
 */
internal object PetRulesReader {
    private const val MENU_ITEMS_DELAY_MS = 500L
    private const val MENU_TITLE = "Autopet"
    private const val RULE_LINE = "Autopet Rule #"

    @Volatile
    var rules: List<AutopetRule>? = null
        private set

    @Volatile
    var readAt: Long? = null
        private set

    /** The items arrive a moment after the menu opens. */
    @SboEvent
    fun onMenuOpen(event: GuiOpenEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        if (!screen.title.string.removeFormatting().startsWith(MENU_TITLE)) return
        sleep(MENU_ITEMS_DELAY_MS) { read(screen) }
    }

    /** A menu without any items has not loaded yet. It is skipped so it does not count as having no rules. */
    private fun read(screen: AbstractContainerScreen<*>) {
        val own = mc.player?.inventory
        val items = screen.menu.slots.filter { it.container !== own && !it.item.isEmpty }.map { it.item }
        if (items.isEmpty()) return
        rules = items.mapNotNull(::parse)
        readAt = System.currentTimeMillis()
    }

    private fun parse(stack: ItemStack): AutopetRule? {
        val lore = ItemLookup(stack).cleanLoreList.map { it.trim() }
        if (lore.none { it.startsWith(RULE_LINE) }) return null
        val equip = lore.firstOrNull { it.startsWith("Equip:") }?.substringAfter("Equip:")?.trim() ?: return null
        return AutopetRule(
            trigger = stack.hoverName.string.removeFormatting().trim(),
            condition = after(lore, "When:").orEmpty(),
            except = after(lore, "Except if:"),
            pet = equip.substringAfter("] ").trimEnd(' ', '✦'),
            heldItem = lore.firstOrNull { it.startsWith("Held Item:") }?.substringAfter("Held Item:")?.trim(),
            enabled = lore.none { it.contains("RULE IS DISABLED") }
        )
    }

    private fun after(lore: List<String>, label: String): String? =
        lore.indexOfFirst { it.startsWith(label) }.takeIf { it >= 0 }?.let { lore.getOrNull(it + 1) }?.takeIf { it.isNotEmpty() }

    /** The state of a guide rule. Unknown until the Autopet menu was opened once. */
    fun state(want: RuleMatch): RuleState {
        val read = rules ?: return RuleState.UNKNOWN
        val found = read.filter { rule ->
            rule.condition.contains(want.condition, ignoreCase = true) &&
                rule.pet.contains(want.pet, ignoreCase = true) &&
                (want.heldItem == null || rule.heldItem?.contains(want.heldItem, ignoreCase = true) == true) &&
                (want.except == null || rule.except?.contains(want.except, ignoreCase = true) == true)
        }
        return when {
            found.any { it.enabled } -> RuleState.SET_UP
            found.isNotEmpty() -> RuleState.DISABLED
            else -> RuleState.MISSING
        }
    }
}
