package net.sbo.mod.customizer

import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.sbo.mod.customizer.CustomizerRecolor.recolored
import net.sbo.mod.customizer.SboCustomizer.byName
import net.sbo.mod.customizer.SboCustomizer.byUuid
import net.sbo.mod.customizer.SboCustomizer.logo
import net.sbo.mod.customizer.SboCustomizer.own
import net.sbo.mod.customizer.SboCustomizer.safe
import net.sbo.mod.customizer.SboCustomizer.showInTab
import net.sbo.mod.customizer.SboCustomizer.shown
import net.sbo.mod.customizer.SboCustomizer.tabTags
import net.sbo.mod.customizer.SboCustomizer.team
import net.sbo.mod.customizer.SboCustomizer.version
import java.util.WeakHashMap
import java.util.function.Supplier

/** Customizers in the tab list: ours and the compact ones of SkyHanni and SkyblockAddons. */
internal object CustomizerTab {
    /** A player's name starts the line or follows the level or rank: "[296] Name ♲", "[MVP+] Name". */
    private val NAME = Regex("(?:^|] )([A-Za-z0-9_]{1,16})")

    private class TabEntry(val display: Component, val listVersion: String?, val player: String?)
    private val tabCache = WeakHashMap<PlayerInfo, TabEntry>()

    fun clearCache() {
        synchronized(tabCache) { tabCache.clear() }
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component = safe(line) { decorateTabOf(info, line) }

    private fun decorateTabOf(info: PlayerInfo, line: Component): Component {
        if (compactReading || !showInTab && team.isEmpty() && own() == null || byUuid.isEmpty() && tabTags.isEmpty()) return line
        val player = tabPlayerOf(info)?.takeIf { shown(showInTab, it) } ?: return line
        return decorateTab(line, player)
    }

    /** Set while SkyHanni's or SkyblockAddons' compact tab list reads the lines. */
    private var compactReading = false

    /**
     * SkyHanni and SkyblockAddons rebuild tab lines from § codes and would lose hex colors and the logo font,
     * so they read them undecorated.
     */
    fun <T> undecoratedTab(read: Supplier<T>): T {
        compactReading = true
        try {
            return read.get()
        } finally {
            compactReading = false
        }
    }

    /** A § coded line SkyblockAddons' compact tab list draws, decorated like ours; null when it has no customizer holder or tagged member. */
    fun decorateSbaTab(line: String): Component? = if (line.isEmpty()) null else decorateSkyHanniTab(Component.literal(line))

    /** A line SkyHanni's compact tab list draws, decorated like ours; null when it has no customizer holder or tagged member. */
    fun decorateSkyHanniTab(line: Component?): Component? = safe(null) { decorateSkyHanniTabOf(line) }

    private fun decorateSkyHanniTabOf(line: Component?): Component? {
        if (line == null || !showInTab && team.isEmpty() && own() == null || byName.isEmpty() && tabTags.isEmpty()) return null
        val plain = ChatFormatting.stripFormatting(line.string) ?: return null
        val player = tabPlayerIn(plain)?.takeIf { shown(showInTab, it) } ?: return null
        return decorateTab(line, player).takeIf { it !== line }
    }

    private fun tabPlayerIn(text: String): String? =
        NAME.findAll(text.trim()).map { it.groupValues[1] }.firstOrNull { it.lowercase() in byName || it.lowercase() in tabTags }

    private fun decorateTab(line: Component, player: String): Component {
        val decorated = recolored(line, player, TeamTagPlace.TAB, tabTags[player.lowercase()])
        val logo = byName[player.lowercase()]?.shows(CustomizerPart.LOGO, TeamTagPlace.TAB) == true
        return if (logo) decorated.copy().append(" ").append(logo()) else decorated
    }

    /** Name of the customizer holder or tagged team member a tab entry shows, null for everyone else. */
    private fun tabPlayerOf(info: PlayerInfo): String? {
        byUuid[info.profile.id]?.let { return it.name }
        team[info.profile.id]?.let { if (it.name.lowercase() in tabTags) return it.name }
        val display = info.tabListDisplayName ?: return null
        val listVersion = version
        synchronized(tabCache) {
            tabCache[info]?.let { if (it.display === display && it.listVersion == listVersion) return it.player }
        }
        val player = tabPlayerIn(display.string)
        synchronized(tabCache) { tabCache[info] = TabEntry(display, listVersion, player) }
        return player
    }
}
