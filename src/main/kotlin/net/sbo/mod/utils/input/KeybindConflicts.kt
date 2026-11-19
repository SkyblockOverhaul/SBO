package net.sbo.mod.utils.input

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.config.KeybindEntry
import net.sbo.mod.settings.categories.Keybinds

/**
 * Keys bound twice, shown on the key in the SBO settings and in Minecraft's Key Binds.
 *
 * Keys that work in the world clash with Minecraft's keys and with each other. Keys that only work in a menu (the
 * loadout keys) only clash with each other, so they can share the hotbar keys.
 */
object KeybindConflicts {
    private val keys: List<KeybindEntry> get() = Keybinds.entries.filterIsInstance<KeybindEntry>()

    /** Gives every key in [Keybinds] its conflicts for the settings screen. */
    fun init() {
        keys.forEach { entry -> entry.conflicts = { conflictsOf(entry) } }
    }

    /** The names of everything else bound to the key of [entry], SBO's keys first. */
    private fun conflictsOf(entry: KeybindEntry): List<String> {
        val key = boundKey(entry) ?: return emptyList()
        val sbo = keys.filter { it !== entry && it.menuOnly == entry.menuOnly && boundKey(it) == key }.map { it.name }
        if (entry.menuOnly) return sbo
        val minecraft = mc.options.keyMappings.filter { it.saveString() == key }.map { Component.translatable(it.name).string }
        return sbo + minecraft
    }

    /** The key name [entry] is bound to, null while unbound or switched off. */
    private fun boundKey(entry: KeybindEntry): String? =
        KeybindEntry.keyOf(entry.get()).takeUnless { it == InputConstants.UNKNOWN }?.name

    /**
     * Marks [button] of [mapping] in Minecraft's Key Binds like Minecraft marks its own double keys, when an SBO key
     * that works in the world has the same key. Called by KeyBindsListMixin after Minecraft marked its own.
     *
     * @return true when the key is marked
     */
    @JvmStatic
    fun markMinecraftKey(mapping: KeyMapping, button: Button): Boolean {
        if (mapping.isUnbound) return false
        val sbo = keys.filter { !it.menuOnly && boundKey(it) == mapping.saveString() }.map { "SBO ${it.name}" }
        if (sbo.isEmpty()) return false
        // Minecraft's own double keys, the way it finds them, so the tooltip lists all of them
        val minecraft = mc.options.keyMappings
            .filter { it !== mapping && mapping.same(it) && !(it.isDefault && mapping.isDefault) }
            .map { Component.translatable(it.name).string }
        button.message = Component.literal("[ ")
            .append(mapping.translatedKeyMessage.copy().withStyle(ChatFormatting.WHITE))
            .append(" ]")
            .withStyle(ChatFormatting.RED)
        button.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", (minecraft + sbo).joinToString(", "))))
        return true
    }
}
