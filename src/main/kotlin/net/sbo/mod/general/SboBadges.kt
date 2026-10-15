package net.sbo.mod.general

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.event.Event
import net.minecraft.ChatFormatting
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.BadgeGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeListResponse
import net.sbo.mod.utils.data.BadgeRank
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.data.TeamMember
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

    // Switches for other players' badges in /sbobadge; false hides them and always shows badges
    const val VIEWER_TOGGLES = true
    private val showAboveHeads: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesAboveHeads
    private val showInTab: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesInTab
    private val showInChat: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesInChat

    private const val LIST_REFRESH_MINUTES = 10

    private const val LOGO = ""
    private val LOGO_FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "badge"))
    // White, a colored logo would be tinted
    private val LOGO_STYLE = Style.EMPTY.withFont(LOGO_FONT).withColor(0xFFFFFF)

    private const val SHIMMER_MS = 3000L

    // A player's name starts the line or follows the level or rank: "[296] Name ♲", "[MVP+] Name"
    private val NAME = Regex("(?:^|] )([A-Za-z0-9_]{1,16})")
    private val LEVEL = Regex("""\[\d+]\s*$""")
    // The sender stands before the first ": " of a chat line: "[296] [MVP+] Name: hi", "Party > Name: hi", "From Name: hi"
    private const val MAX_SENDER_LENGTH = 80
    // Runs after the default phase: mods like SkyHanni rebuild chat lines from § codes there,
    // which would drop the hex colors and the logo font
    private val CHAT_PHASE = Identifier.fromNamespaceAndPath("sbo", "badges")
    // Mods rebuild Hypixel's § colors as rgb colors, so they are compared by value
    private val CHAT_COLORS: Set<Int> = ChatFormatting.entries.mapNotNull { TextColor.fromLegacyFormat(it)?.value }.toSet()
    private val WORD = Regex("[A-Za-z0-9_]{1,16}")

    @Volatile private var byUuid: Map<UUID, SboBadge> = emptyMap()
    @Volatile private var byName: Map<String, SboBadge> = emptyMap()
    // Lines without a moving gradient are built once per list
    @Volatile private var fixedLines: Map<UUID, Component> = emptyMap()
    @Volatile private var team: Map<UUID, TeamMember> = emptyMap()
    @Volatile private var ranks: Map<String, BadgeRank> = emptyMap()
    // Team tags in front of the name, built once per list
    @Volatile private var tags: Map<UUID, Component> = emptyMap()
    // The same tags by lowercase name, chat lines only carry the name
    @Volatile private var tagsByName: Map<String, Component> = emptyMap()
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
        // After ALLOW_GAME, so the chat features still read the line as Hypixel sent it
        ClientReceiveMessageEvents.MODIFY_GAME.addPhaseOrdering(Event.DEFAULT_PHASE, CHAT_PHASE)
        ClientReceiveMessageEvents.MODIFY_GAME.register(CHAT_PHASE) { message, overlay ->
            if (overlay) message else decorateChat(message)
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
                if (!response.unchanged) apply(response)
                refreshOwn()
            }
            .error {
                fetching = false
                SBOKotlin.logger.debug("[SboBadges] list failed: ${it.message}")
            }
    }

    private fun apply(response: BadgeListResponse) {
        val badges = response.badges
        val holders = badges.mapNotNull { badge -> uuidOf(badge.uuid)?.let { it to badge } }.toMap()
        val members = response.team.mapNotNull { member -> uuidOf(member.uuid)?.let { it to member } }.toMap()
        ranks = response.ranks
        team = members
        val tagged = members.filterValues { it.nameTag }.mapNotNull { (uuid, member) -> tagOf(member.rank)?.let { Triple(uuid, member.name, it) } }
        tags = tagged.associate { (uuid, _, tag) -> uuid to tag }
        tagsByName = tagged.associate { (_, name, tag) -> name.lowercase() to tag }
        fixedLines = holders.filterValues { it.to == null }.mapValues { line(it.value) }
        byUuid = holders
        byName = badges.associateBy { it.name.lowercase() }
        version = response.version
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
        val colored = byUuid[uuid]?.let { recolored(nameTag, it) } ?: nameTag
        val tag = tags[uuid] ?: return colored
        return Component.empty().append(tag).append(colored)
    }

    /** Team rank of a player (uuid without dashes), for the party finder. */
    fun teamRank(uuid: String): BadgeRank? = uuidOf(uuid)?.let { team[it] }?.let { ranks[it.rank] }?.takeIf { it.tag != null }

    /** "[Moderator] " in the rank colors, null for ranks without a tag. */
    fun tagOf(rank: String): Component? {
        val info = ranks[rank] ?: return null
        val text = info.tag ?: return null
        val gray = Style.EMPTY.withColor(0xAAAAAA)
        return Component.empty()
            .append(Component.literal("[").withStyle(gray))
            .append(colored(text, info.color, info.to, shimmer = false))
            .append(Component.literal("] ").withStyle(gray))
    }

    /** Your own name tag shows in third person when you have a badge or a team tag. */
    fun showOwnNameTag(): Boolean {
        if (!DataManager.sboData.badgeOwnInF5) return false
        val uuid = uuidOf(Player.accountUuid()) ?: return false
        return uuid in byUuid || uuid in tags
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

    /** The sender of a chat line: team tag in front, badge color and the SBO logo behind the name. */
    fun decorateChat(line: Component): Component {
        if (!showInChat || (byName.isEmpty() && tagsByName.isEmpty())) return line
        val plain = ChatFormatting.stripFormatting(line.string) ?: return line
        if (LOGO in plain) return line
        val colon = plain.indexOf(": ")
        if (colon !in 1..MAX_SENDER_LENGTH) return line
        val sender = WORD.findAll(plain.substring(0, colon)).map { it.value }
            .firstOrNull { it.lowercase() in byName || it.lowercase() in tagsByName } ?: return line
        val badge = byName[sender.lowercase()]
        return recolor(
            line, sender, badge,
            beforeName = tagsByName[sender.lowercase()],
            afterName = badge?.let { Component.literal(" ").append(logo()) },
        )
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
        val result = recolor(line, badge.name, badge)
        synchronized(recolorCache) { recolorCache[key] = result }
        return result
    }

    private class StyledChar(val char: Char, val style: Style)

    // Hypixel mixes styles and § codes, so the line is read char by char and built again
    private fun recolor(
        line: Component,
        playerName: String,
        badge: SboBadge?,
        beforeName: Component? = null,
        afterName: Component? = null,
    ): Component {
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
        val name = Regex("(?<![A-Za-z0-9_])${Regex.escape(playerName)}(?![A-Za-z0-9_])", RegexOption.IGNORE_CASE)
            .find(plain) ?: return line
        val levelStart = if (badge?.level == true) LEVEL.find(plain.substring(0, name.range.first))?.range?.first else null
        val from = levelStart ?: name.range.first
        // Without a badge (team tag only) nothing is colored
        val until = if (badge == null || badge.plainName == true) name.range.first else name.range.last + 1
        val inserts = beforeName != null || afterName != null
        if (from >= until && !inserts) return line

        // A name another mod already colored (not one of the 16 § colors Hypixel uses) keeps its color
        val foreign = name.range.any { i -> chars[i].style.color?.let { it.value !in CHAT_COLORS } == true }
        if (foreign && !inserts) return line

        val start = badge?.let { parseHex(it.color) } ?: 0
        val end = badge?.to?.let(::parseHex)
        val out = Component.empty()
        var run = StringBuilder()
        var runStyle: Style? = null
        fun flush() {
            runStyle?.let { out.append(Component.literal(run.toString()).withStyle(it)) }
            run = StringBuilder()
        }
        fun insert(part: Component) {
            flush()
            runStyle = null
            out.append(part)
        }
        chars.forEachIndexed { i, c ->
            if (beforeName != null && i == name.range.first) insert(beforeName)
            val style = when {
                foreign || i !in from until until -> c.style
                end == null -> c.style.withColor(start)
                else -> c.style.withColor(mix(start, end, (i - from) / (until - from - 1).coerceAtLeast(1).toFloat()))
            }
            if (style != runStyle) {
                flush()
                runStyle = style
            }
            run.append(c.char)
            if (afterName != null && i == name.range.last) insert(afterName)
        }
        flush()
        return out
    }

    fun logo(): MutableComponent = Component.literal(LOGO).withStyle(LOGO_STYLE)

    fun colored(text: String, color: String, to: String?, shimmer: Boolean = true): MutableComponent {
        val from = parseHex(color)
        val end = to?.let(::parseHex) ?: return Component.literal(text).withStyle { it.withColor(from) }
        val phase = (System.currentTimeMillis() % SHIMMER_MS) / SHIMMER_MS.toFloat()
        val out = Component.empty()
        text.forEachIndexed { i, char ->
            val t = if (!shimmer) i / (text.length - 1).coerceAtLeast(1).toFloat() else {
                // There and back again, so the moving gradient has no seam
                val pos = (i / text.length.toFloat() + phase) % 1f
                if (pos < 0.5f) pos * 2f else (1f - pos) * 2f
            }
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
