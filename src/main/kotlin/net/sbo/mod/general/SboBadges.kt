package net.sbo.mod.general

import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.BadgeGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeListResponse
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.SboApi
import java.util.Optional
import java.util.UUID
import java.util.WeakHashMap

object SboBadges {
    const val COMMAND = "sbobadge"

    // Badges can't be turned off for now; true brings back the two switches in /sbobadge
    const val VIEWER_TOGGLES = false
    private val showAboveHeads: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesAboveHeads
    private val showInTab: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesInTab

    private const val LIST_REFRESH_MINUTES = 10

    private const val LOGO = ""
    private val LOGO_FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "badge"))
    // White, a colored logo would be tinted
    private val LOGO_STYLE = Style.EMPTY.withFont(LOGO_FONT).withColor(0xFFFFFF)

    private const val SHIMMER_MS = 3000L

    // A player's name starts the line or follows the level or rank: "[296] Name ♲", "[MVP+] Name"
    private val NAME = Regex("(?:^|] )([A-Za-z0-9_]{1,16})")
    private val LEVEL = Regex("""\[\d+]\s*$""")

    @Volatile private var byUuid: Map<UUID, SboBadge> = emptyMap()
    @Volatile private var byName: Map<String, SboBadge> = emptyMap()
    // Lines without a moving gradient are built once per list
    @Volatile private var fixedLines: Map<UUID, Component> = emptyMap()
    @Volatile private var version: String? = null
    @Volatile private var fetching = false

    @Volatile private var ownRefreshed = false

    private class TabEntry(val display: Component, val listVersion: String?, val holder: SboBadge?)
    private val tabCache = WeakHashMap<PlayerInfo, TabEntry>()

    fun init() {
        Register.onTick(20 * 60 * LIST_REFRESH_MINUTES) { _ ->
            if (mc.level != null && World.isInSkyblock()) fetch()
        }
        Register.command(COMMAND) {
            mc.schedule { BadgeGui.open() }
        }
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (version == null) fetch()
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        synchronized(tabCache) { tabCache.clear() }
    }

    fun fetch() {
        if (fetching) return
        fetching = true
        SboApi.badges(version)
            .toJson<BadgeListResponse>(ignoreUnknownKeys = true) { response ->
                fetching = false
                if (!response.success) return@toJson
                if (!response.unchanged) apply(response.badges, response.version)
                refreshOwn()
            }
            .error {
                fetching = false
                SBOKotlin.logger.debug("[SboBadges] list failed: ${it.message}")
            }
    }

    private fun apply(badges: List<SboBadge>, newVersion: String?) {
        val holders = badges.mapNotNull { badge -> uuidOf(badge.uuid)?.let { it to badge } }.toMap()
        fixedLines = holders.filterValues { it.to == null }.mapValues { line(it.value) }
        byUuid = holders
        byName = badges.associateBy { it.name.lowercase() }
        version = newVersion
        synchronized(tabCache) { tabCache.clear() }
        synchronized(recolorCache) { recolorCache.clear() }
    }

    private fun refreshOwn() {
        if (ownRefreshed || own() == null) return
        ownRefreshed = true
        SboApi.ownBadge()
            .toJsonObject { if (it.getBoolean("Success")) fetch() }
            .error { SBOKotlin.logger.debug("[SboBadges] own badge failed: ${it.message}") }
    }

    fun hasAccess(): Boolean? = if (own() != null) true else DataManager.sboData.badgeAccess[Player.accountUuid()]

    fun rememberAccess(access: Boolean) {
        val account = Player.accountUuid()
        if (DataManager.sboData.badgeAccess[account] == access) return
        DataManager.sboData.badgeAccess[account] = access
        DataManager.sboData.save()
    }

    fun own(): SboBadge? = uuidOf(Player.accountUuid())?.let { byUuid[it] }

    fun nameTagLine(uuid: UUID): Component? {
        if (!showAboveHeads) return null
        fixedLines[uuid]?.let { return it }
        return byUuid[uuid]?.let(::line)
    }

    fun colorNameTag(uuid: UUID, nameTag: Component): Component {
        if (!showAboveHeads) return nameTag
        val badge = byUuid[uuid] ?: return nameTag
        return recolored(nameTag, badge)
    }

    fun line(badge: SboBadge): MutableComponent {
        // The logo is a child, the text after it would take over its font
        val line = Component.empty().append(logo())
        if (badge.label != null && badge.value != null) {
            line.append(" ").append(colored("${badge.label}: ${badge.value}", badge.color, badge.to))
        }
        return line
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component {
        if (!showInTab || byUuid.isEmpty()) return line
        val badge = holderOf(info) ?: return line
        return recolored(line, badge).copy().append(" ").append(logo())
    }

    private fun holderOf(info: PlayerInfo): SboBadge? {
        byUuid[info.profile.id]?.let { return it }
        val display = info.tabListDisplayName ?: return null
        val listVersion = version
        synchronized(tabCache) {
            tabCache[info]?.let { if (it.display === display && it.listVersion == listVersion) return it.holder }
        }
        val holder = NAME.findAll(display.string.trim()).firstNotNullOfOrNull { byName[it.groupValues[1].lowercase()] }
        synchronized(tabCache) { tabCache[info] = TabEntry(display, listVersion, holder) }
        return holder
    }

    private const val RECOLOR_CACHE_SIZE = 256
    private val recolorCache = object : LinkedHashMap<Pair<Component, SboBadge>, Component>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Component, SboBadge>, Component>) = size > RECOLOR_CACHE_SIZE
    }

    private fun recolored(line: Component, badge: SboBadge): Component {
        val key = line to badge
        synchronized(recolorCache) { recolorCache[key]?.let { return it } }
        val result = recolor(line, badge)
        synchronized(recolorCache) { recolorCache[key] = result }
        return result
    }

    private class StyledChar(val char: Char, val style: Style)

    // Hypixel mixes styles and § codes, so the line is read char by char and built again
    private fun recolor(line: Component, badge: SboBadge): Component {
        val chars = ArrayList<StyledChar>()
        line.visit({ style, text ->
            var current = style
            var i = 0
            while (i < text.length) {
                val format = if (text[i] == '§' && i + 1 < text.length) ChatFormatting.getByCode(text[i + 1]) else null
                if (format != null) {
                    current = if (format == ChatFormatting.RESET) style else current.applyLegacyFormat(format)
                    i += 2
                    continue
                }
                chars += StyledChar(text[i], current)
                i++
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)

        val plain = String(CharArray(chars.size) { chars[it].char })
        val name = Regex("(?<![A-Za-z0-9_])${Regex.escape(badge.name)}(?![A-Za-z0-9_])", RegexOption.IGNORE_CASE)
            .find(plain) ?: return line
        val levelStart = if (badge.level == true) LEVEL.find(plain.substring(0, name.range.first))?.range?.first else null
        val plainName = badge.plainName == true
        val from = levelStart ?: if (plainName) return line else name.range.first
        val until = if (plainName) name.range.first else name.range.last + 1

        val start = parseHex(badge.color)
        val end = badge.to?.let(::parseHex)
        val out = Component.empty()
        var run = StringBuilder()
        var runStyle: Style? = null
        fun flush() {
            runStyle?.let { out.append(Component.literal(run.toString()).withStyle(it)) }
            run = StringBuilder()
        }
        chars.forEachIndexed { i, c ->
            val style = when {
                i !in from until until -> c.style
                end == null -> c.style.withColor(start)
                else -> c.style.withColor(mix(start, end, (i - from) / (until - from - 1).coerceAtLeast(1).toFloat()))
            }
            if (style != runStyle) {
                flush()
                runStyle = style
            }
            run.append(c.char)
        }
        flush()
        return out
    }

    fun logo(): MutableComponent = Component.literal(LOGO).withStyle(LOGO_STYLE)

    fun colored(text: String, color: String, to: String?): MutableComponent {
        val from = parseHex(color)
        val end = to?.let(::parseHex) ?: return Component.literal(text).withStyle { it.withColor(from) }
        val phase = (System.currentTimeMillis() % SHIMMER_MS) / SHIMMER_MS.toFloat()
        val out = Component.empty()
        text.forEachIndexed { i, char ->
            val pos = (i / text.length.toFloat() + phase) % 1f
            val t = if (pos < 0.5f) pos * 2f else (1f - pos) * 2f
            out.append(Component.literal(char.toString()).withStyle { it.withColor(mix(from, end, t)) })
        }
        return out
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt()
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun parseHex(hex: String): Int = hex.removePrefix("#").toIntOrNull(16) ?: 0xFFFFFF

    private fun uuidOf(raw: String): UUID? = runCatching {
        val s = raw.replace("-", "")
        UUID(s.substring(0, 16).toULong(16).toLong(), s.substring(16, 32).toULong(16).toLong())
    }.getOrNull()
}
