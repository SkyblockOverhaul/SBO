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
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.BadgeRank
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.data.TeamMember
import net.sbo.mod.utils.data.TeamTagPlace
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
import java.util.function.Supplier

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
    // Hypixel's rank right before the name: "[MVP++] Name", "[YOUTUBE] Name"
    private val RANK = Regex("""\[[A-Z]+\+*]\s*$""")
    private const val TAG_BRACKETS = 0xAAAAAA
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
    // Team tags in front of the name, built once per list: above heads by uuid,
    // tab list and chat by lowercase name since their lines only carry the name
    @Volatile private var tags: Map<UUID, Component> = emptyMap()
    @Volatile private var tabTags: Map<String, Component> = emptyMap()
    @Volatile private var chatTags: Map<String, Component> = emptyMap()
    @Volatile private var version: String? = null
    @Volatile private var fetching = false

    @Volatile private var ownRefreshed = false

    private class TabEntry(val display: Component, val listVersion: String?, val player: String?)
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
        // The tag takes the member's tag color, the rank's while their badge is off
        val tagged = members.mapNotNull { (uuid, member) ->
            tagOf(member.rank, holders[uuid]?.colorOf(BadgePart.TAG))?.let { Triple(uuid, member, it) }
        }
        fun byName(place: String) = tagged.filter { place in it.second.tagPlaces }.associate { (_, member, tag) -> member.name.lowercase() to tag }
        tags = tagged.filter { TeamTagPlace.HEAD in it.second.tagPlaces }.associate { (uuid, _, tag) -> uuid to tag }
        tabTags = byName(TeamTagPlace.TAB)
        chatTags = byName(TeamTagPlace.CHAT)
        fixedLines = holders.filterValues { it.colorOf(BadgePart.STAT).to == null }.mapValues { line(it.value) }
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
        val badge = byUuid[uuid]?.takeIf { it.shows(BadgePart.LOGO, TeamTagPlace.HEAD) } ?: return null
        return fixedLines[uuid] ?: line(badge)
    }

    fun colorNameTag(uuid: UUID, nameTag: Component): Component {
        if (!showAboveHeads) return nameTag
        val colored = byUuid[uuid]?.let { recolored(nameTag, it.name, TeamTagPlace.HEAD) } ?: nameTag
        val tag = tags[uuid] ?: return colored
        return Component.empty().append(tag).append(colored)
    }

    /** Tag color of a player (uuid without dashes) for the party finder, null while their badge is off. */
    fun badgeColor(uuid: String): String? = uuidOf(uuid)?.let { byUuid[it]?.colorOf(BadgePart.TAG)?.color }

    /** Team rank of a player (uuid without dashes), for the party finder. */
    fun teamRank(uuid: String): BadgeRank? = uuidOf(uuid)?.let { team[it] }?.let { ranks[it.rank] }?.takeIf { it.tag != null }

    /** "[SBO Moderator] " in the given color or the rank's, null for ranks without a tag. */
    fun tagOf(rank: String, partColor: PartColor? = null): Component? {
        val color = partColor?.color
        val to = partColor?.to
        val info = ranks[rank] ?: return null
        val text = info.tag ?: return null
        fun bracket(hex: String?) = Style.EMPTY.withColor(hex?.let(::parseHex) ?: TAG_BRACKETS)
        return Component.empty()
            .append(Component.literal("[").withStyle(bracket(partColor?.open)))
            .append(if (color != null) colored(text, color, to, shimmer = false) else colored(text, info.color, info.to, shimmer = false))
            .append(Component.literal("] ").withStyle(bracket(partColor?.close)))
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
            val stat = badge.colorOf(BadgePart.STAT)
            line.append(" ").append(colored("${badge.label}: ${badge.value}", stat.color, stat.to))
        }
        return line
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component {
        if (skyHanniReading || !showInTab || (byUuid.isEmpty() && tabTags.isEmpty())) return line
        val player = tabPlayerOf(info) ?: return line
        return decorateTab(line, player)
    }

    // SkyHanni's compact tab list reads the lines while this is set
    private var skyHanniReading = false

    /** SkyHanni rebuilds tab lines from § codes and would lose hex colors and the logo font, so it reads them undecorated. */
    fun <T> undecoratedTab(read: Supplier<T>): T {
        skyHanniReading = true
        try {
            return read.get()
        } finally {
            skyHanniReading = false
        }
    }

    /** A line SkyHanni's compact tab list draws, decorated like ours; null when it has no badge holder or tagged member. */
    fun decorateSkyHanniTab(line: Component?): Component? {
        if (line == null || !showInTab || (byName.isEmpty() && tabTags.isEmpty())) return null
        val plain = ChatFormatting.stripFormatting(line.string) ?: return null
        val player = tabPlayerIn(plain) ?: return null
        return decorateTab(line, player).takeIf { it !== line }
    }

    private fun tabPlayerIn(text: String): String? =
        NAME.findAll(text.trim()).map { it.groupValues[1] }.firstOrNull { it.lowercase() in byName || it.lowercase() in tabTags }

    private fun decorateTab(line: Component, player: String): Component {
        val decorated = recolored(line, player, TeamTagPlace.TAB)
        val logo = byName[player.lowercase()]?.shows(BadgePart.LOGO, TeamTagPlace.TAB) == true
        return if (logo) decorated.copy().append(" ").append(logo()) else decorated
    }

    /** The sender of a chat line: team tag in front, badge color and the SBO logo behind the name. */
    fun decorateChat(line: Component): Component {
        if (!showInChat || (byName.isEmpty() && chatTags.isEmpty())) return line
        val plain = ChatFormatting.stripFormatting(line.string) ?: return line
        if (LOGO in plain) return line
        val colon = plain.indexOf(": ")
        if (colon !in 1..MAX_SENDER_LENGTH) return line
        val sender = WORD.findAll(plain.substring(0, colon)).map { it.value }
            .firstOrNull { it.lowercase() in byName || it.lowercase() in chatTags } ?: return line
        val badge = byName[sender.lowercase()]
        return recolor(
            line, sender, badge, TeamTagPlace.CHAT,
            beforeName = chatTags[sender.lowercase()],
            afterName = badge?.takeIf { it.shows(BadgePart.LOGO, TeamTagPlace.CHAT) }?.let { Component.literal(" ").append(logo()) },
        )
    }

    /** Name of the badge holder or tagged team member a tab entry shows, null for everyone else. */
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

    private const val RECOLOR_CACHE_SIZE = 256
    // Badge and tag follow from the name and the list, which clears the cache
    private val recolorCache = object : LinkedHashMap<Triple<Component, String, String>, Component>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Triple<Component, String, String>, Component>) = size > RECOLOR_CACHE_SIZE
    }

    /** Above heads the tag goes in front of the whole name tag, so only the tab list inserts it here. */
    private fun recolored(line: Component, player: String, place: String): Component {
        val key = Triple(line, player.lowercase(), place)
        synchronized(recolorCache) { recolorCache[key]?.let { return it } }
        val tag = if (place == TeamTagPlace.TAB) tabTags[player.lowercase()] else null
        val result = recolor(line, player, byName[player.lowercase()], place, beforeName = tag)
        synchronized(recolorCache) { recolorCache[key] = result }
        return result
    }

    private class StyledChar(val char: Char, val style: Style)

    // Hypixel mixes styles and § codes, so the line is read char by char and built again
    private fun recolor(
        line: Component,
        playerName: String,
        badge: SboBadge?,
        place: String,
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
        // Name, level and rank each take their own color where the badge shows them; without a badge (team tag only) nothing is colored
        val nameColor = badge?.takeIf { it.shows(BadgePart.NAME, place) }?.colorOf(BadgePart.NAME)
        val levelColor = badge?.takeIf { it.shows(BadgePart.LEVEL, place) }?.colorOf(BadgePart.LEVEL)
        val rankColor = badge?.takeIf { it.shows(BadgePart.RANK, place) }?.colorOf(BadgePart.RANK)
        // In chat and for some ranks in the tab list Hypixel's rank stands between level and name
        val rankStart = RANK.find(plain.substring(0, name.range.first))?.range?.first
        val levelEnd = rankStart ?: name.range.first
        val levelRange = levelColor?.let { LEVEL.find(plain.substring(0, levelEnd))?.range?.first }?.let { it until levelEnd }
        val rankRange = rankColor?.let { rankStart }?.let { it until name.range.first }
        val inserts = beforeName != null || afterName != null
        if (nameColor == null && levelRange == null && rankRange == null && !inserts) return line

        // A name another mod already colored (not one of the 16 § colors Hypixel uses) keeps its color
        val foreign = name.range.any { i -> chars[i].style.color?.let { it.value !in CHAT_COLORS } == true }
        if (foreign && !inserts) return line

        fun colorAt(color: PartColor, range: IntRange, i: Int): Int {
            val start = parseHex(color.color)
            val end = color.to?.let(::parseHex) ?: return start
            return mix(start, end, (i - range.first) / (range.last - range.first).coerceAtLeast(1).toFloat())
        }
        // Level and rank: brackets and the rank's plus can have their own color
        fun partColorAt(color: PartColor, range: IntRange, i: Int, char: Char): Int = when {
            char == '[' && color.open != null -> parseHex(color.open)
            char == ']' && color.close != null -> parseHex(color.close)
            char == '+' && color.plus != null -> parseHex(color.plus)
            else -> colorAt(color, range, i)
        }
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
                foreign -> c.style
                levelColor != null && levelRange != null && i in levelRange -> c.style.withColor(partColorAt(levelColor, levelRange, i, c.char))
                rankColor != null && rankRange != null && i in rankRange -> c.style.withColor(partColorAt(rankColor, rankRange, i, c.char))
                nameColor != null && i in name.range -> c.style.withColor(colorAt(nameColor, name.range, i))
                else -> c.style
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
