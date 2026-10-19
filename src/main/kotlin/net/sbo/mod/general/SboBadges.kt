package net.sbo.mod.general

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
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeListResponse
import net.sbo.mod.utils.data.BadgeMotion
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
import kotlin.math.PI
import kotlin.math.cos

object SboBadges {
    const val COMMAND = "sbobadge"

    // Switches for other players' badges in the tab list and chat in /sbobadge; false hides them and always shows badges.
    // They only hide supporters and contributors, the team (moderators and above) always shows; above heads always shows
    const val VIEWER_TOGGLES = true
    private val showInTab: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesInTab
    private val showInChat: Boolean get() = !VIEWER_TOGGLES || DataManager.sboData.badgesInChat

    private const val LIST_REFRESH_MINUTES = 10

    private const val LOGO = ""
    private val LOGO_FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "badge"))
    // White, a colored logo would be tinted
    private val LOGO_STYLE = Style.EMPTY.withFont(LOGO_FONT).withColor(0xFFFFFF)

    private const val WAVE_MS = 3000L
    private const val PULSE_MS = 2000L
    private const val CHROMA_MS = 4000L
    // Share of the rainbow one text spans, so short names still show several colors
    private const val CHROMA_SPREAD = 0.6f
    // A pulse without a second color fades this far towards white
    private const val PULSE_WHITE = 0.6f
    // Shine: a gleam this wide (share of the text) crosses in the first 1 / SHINE_REST of the period
    private const val SHINE_MS = 3000L
    private const val SHINE_REST = 2.5f
    private const val SHINE_WIDTH = 0.25f
    private const val SHINE_STRENGTH = 0.75f
    private const val FLASH_MS = 1000L
    // Sparkle: every slot about one char in SPARKLE_ONE_IN twinkles
    private const val SPARKLE_MS = 150L
    private const val SPARKLE_ONE_IN = 7

    private const val WAVY_MS = 1500L
    // Phase between two chars of the wavy motion, and its depth in pixels
    private const val WAVY_STEP = 0.12f
    private const val WAVY_DEPTH = 2
    private const val BOUNCE_MS = 2200L
    // Chars of rest after the hop ran over the text
    private const val BOUNCE_REST = 4
    private const val SHAKE_MS = 90L
    private const val GLITCH_MS = 80L
    // Glitch: about one char in GLITCH_SPREAD lines scrambles per slot
    private const val GLITCH_SPREAD = 1
    // The default font 1 to 3 pixels lower (assets/sbo/font/shift*.json), fonts cannot rise above the line
    private val SHIFT_FONTS = (1..3).map { FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "shift$it")) }

    // A player's name starts the line or follows the level or rank: "[296] Name ♲", "[MVP+] Name"
    private val NAME = Regex("(?:^|] )([A-Za-z0-9_]{1,16})")
    private val LEVEL = Regex("""\[\d+]\s*$""")
    // Hypixel's rank right before the name: "[MVP++] Name", "[YOUTUBE] Name"
    private val RANK = Regex("""\[[A-Z]+\+*]\s*$""")
    private const val TAG_BRACKETS = 0xAAAAAA
    // The sender stands before the first ": " of a chat line: "[296] [MVP+] Name: hi", "Party > Name: hi", "From Name: hi"
    private const val MAX_SENDER_LENGTH = 80
    // Mods rebuild Hypixel's § colors as rgb colors, so they are compared by value
    private val CHAT_COLORS: Set<Int> = ChatFormatting.entries.mapNotNull { TextColor.fromLegacyFormat(it)?.value }.toSet()
    private val WORD = Regex("[A-Za-z0-9_]{1,16}")

    @Volatile private var byUuid: Map<UUID, SboBadge> = emptyMap()
    @Volatile private var byName: Map<String, SboBadge> = emptyMap()
    // Lines without moving colors are built once per list
    @Volatile private var fixedLines: Map<UUID, Component> = emptyMap()
    @Volatile private var team: Map<UUID, TeamMember> = emptyMap()
    @Volatile private var teamNames: Set<String> = emptySet()
    @Volatile private var ranks: Map<String, BadgeRank> = emptyMap()
    // Team tags in front of the name: above heads by uuid,
    // tab list and chat by lowercase name since their lines only carry the name
    @Volatile private var tags: Map<UUID, TagLook> = emptyMap()
    @Volatile private var tabTags: Map<String, TagLook> = emptyMap()
    @Volatile private var chatTags: Map<String, TagLook> = emptyMap()
    // Lowercase names whose name tag or tab line has moving colors, built again every frame instead of cached
    @Volatile private var movingAtHead: Set<String> = emptySet()
    @Volatile private var movingInTab: Set<String> = emptySet()
    @Volatile private var lastList: BadgeListResponse? = null
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
        teamNames = members.values.mapTo(HashSet()) { it.name.lowercase() }
        // The tag takes the member's tag color, the rank's while their badge is off
        val tagged = members.filter { ranks[it.value.rank]?.tag != null }.map { (uuid, member) ->
            val color = holders[uuid]?.colorOf(BadgePart.TAG)
            Triple(uuid, member, TagLook(member.rank, color, animates(uuid)))
        }
        fun byName(place: String) = tagged.filter { place in it.second.tagPlaces }.associate { (_, member, tag) -> member.name.lowercase() to tag }
        tags = tagged.filter { TeamTagPlace.HEAD in it.second.tagPlaces }.associate { (uuid, _, tag) -> uuid to tag }
        tabTags = byName(TeamTagPlace.TAB)
        chatTags = byName(TeamTagPlace.CHAT)
        fun movingAt(place: String, tagsAt: Map<String, TagLook>) = holders.filter { (uuid, badge) ->
            animates(uuid) && listOf(BadgePart.NAME, BadgePart.LEVEL, BadgePart.RANK).any { badge.shows(it, place) && movesAt(badge.colorOf(it), it, place) }
        }.mapTo(HashSet()) { it.value.name.lowercase() } +
            tagsAt.filterValues { tag -> tag.animated && tag.color?.let { movesAt(it, BadgePart.TAG, place) } == true }.keys
        movingAtHead = movingAt(TeamTagPlace.HEAD, byName(TeamTagPlace.HEAD))
        movingInTab = movingAt(TeamTagPlace.TAB, tabTags)
        fixedLines = holders.filter { (uuid, badge) -> !moves(badge.colorOf(BadgePart.STAT), BadgePart.STAT) || !animates(uuid) }
            .mapValues { line(it.value, animate = false) }
        byUuid = holders
        byName = badges.associateBy { it.name.lowercase() }
        version = response.version
        lastList = response
        synchronized(tabCache) { tabCache.clear() }
        synchronized(recolorCache) { recolorCache.clear() }
    }

    /** Builds the badges again after the viewer turned animations of others on or off. */
    fun relook() {
        lastList?.let { mc.execute { apply(it) } }
    }

    /** Your own badge always moves, other players' only while the viewer allows it. */
    private fun animates(uuid: UUID): Boolean = DataManager.sboData.badgeAnimationsOfOthers || uuid == uuidOf(Player.accountUuid())

    /** A team tag: the rank gives its text, the member's badge its colors (the rank's while their badge is off). */
    private class TagLook(val rank: String, val color: PartColor?, val animated: Boolean)

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

    /** The viewer's switch for a place hides supporters and contributors, never the team. */
    private fun shown(enabled: Boolean, player: String) = enabled || player.lowercase() in teamNames

    fun nameTagLine(uuid: UUID): Component? = safe(null) { nameTagLineOf(uuid) }

    private fun nameTagLineOf(uuid: UUID): Component? {
        val badge = byUuid[uuid]?.takeIf { it.shows(BadgePart.LOGO, TeamTagPlace.HEAD) } ?: return null
        return fixedLines[uuid] ?: line(badge, animate = true)
    }

    fun colorNameTag(uuid: UUID, nameTag: Component): Component = safe(nameTag) { colorNameTagOf(uuid, nameTag) }

    private fun colorNameTagOf(uuid: UUID, nameTag: Component): Component {
        val badge = byUuid[uuid]
        val tag = tags[uuid]
        if (badge == null && tag == null) return nameTag
        val player = badge?.name ?: team[uuid]?.name ?: return nameTag
        val colored = recolored(nameTag, player, TeamTagPlace.HEAD, tag)
        // A name tag without the player's name (another mod changed it) still gets the tag in front
        if (colored !== nameTag || tag == null) return colored
        return tagOf(tag.rank, tag.color)?.let { Component.empty().append(it).append(nameTag) } ?: nameTag
    }

    /** Tag color of a player (uuid without dashes) for the party finder, null while their badge is off. */
    fun badgeColor(uuid: String): String? = uuidOf(uuid)?.let { byUuid[it]?.colorOf(BadgePart.TAG)?.color }

    /** Team rank of a player (uuid without dashes), for the party finder. */
    fun teamRank(uuid: String): BadgeRank? = uuidOf(uuid)?.let { team[it] }?.let { ranks[it.rank] }?.takeIf { it.tag != null }

    /** Whether a player (uuid without dashes) supports SBO and has their badge on, for the party finder. */
    fun isSupporter(uuid: String): Boolean = uuidOf(uuid)?.let { byUuid[it] }?.rank == "supporter"

    /** "[SBO Dev] " in the given color or the rank's, null for ranks without a tag. */
    fun tagOf(rank: String, partColor: PartColor? = null): Component? {
        val info = ranks[rank] ?: return null
        val text = info.tag ?: return null
        fun bracket(hex: String?) = Style.EMPTY.withColor(hex?.let(::parseHex) ?: TAG_BRACKETS)
        return Component.empty()
            .append(Component.literal("[").withStyle(bracket(partColor?.open)))
            .append(colored(text, partColor ?: PartColor(info.color, info.to), BadgePart.TAG, animate = false))
            .append(Component.literal("] ").withStyle(bracket(partColor?.close)))
    }

    /** Your own name tag shows in third person when you have a badge or a team tag. */
    fun showOwnNameTag(): Boolean {
        if (!DataManager.sboData.badgeOwnInF5) return false
        val uuid = uuidOf(Player.accountUuid()) ?: return false
        return uuid in byUuid || uuid in tags
    }

    fun line(badge: SboBadge, animate: Boolean): MutableComponent {
        // The logo is a child, the text after it would take over its font
        val line = Component.empty().append(logo())
        if (badge.label != null && badge.value != null) {
            line.append(" ").append(colored("${badge.label}: ${badge.value}", badge.colorOf(BadgePart.STAT), BadgePart.STAT, animate))
        }
        return line
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component = safe(line) { decorateTabOf(info, line) }

    private fun decorateTabOf(info: PlayerInfo, line: Component): Component {
        if (skyHanniReading || (!showInTab && team.isEmpty()) || (byUuid.isEmpty() && tabTags.isEmpty())) return line
        val player = tabPlayerOf(info)?.takeIf { shown(showInTab, it) } ?: return line
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
    fun decorateSkyHanniTab(line: Component?): Component? = safe(null) { decorateSkyHanniTabOf(line) }

    private fun decorateSkyHanniTabOf(line: Component?): Component? {
        if (line == null || (!showInTab && team.isEmpty()) || (byName.isEmpty() && tabTags.isEmpty())) return null
        val plain = ChatFormatting.stripFormatting(line.string) ?: return null
        val player = tabPlayerIn(plain)?.takeIf { shown(showInTab, it) } ?: return null
        return decorateTab(line, player).takeIf { it !== line }
    }

    private fun tabPlayerIn(text: String): String? =
        NAME.findAll(text.trim()).map { it.groupValues[1] }.firstOrNull { it.lowercase() in byName || it.lowercase() in tabTags }

    private fun decorateTab(line: Component, player: String): Component {
        val decorated = recolored(line, player, TeamTagPlace.TAB, tabTags[player.lowercase()])
        val logo = byName[player.lowercase()]?.shows(BadgePart.LOGO, TeamTagPlace.TAB) == true
        return if (logo) decorated.copy().append(" ").append(logo()) else decorated
    }

    /** The sender of a chat line: team tag in front, badge color and the SBO logo behind the name. */
    fun decorateChat(line: Component): Component = safe(line) { decorateChatOf(line) }

    private fun decorateChatOf(line: Component): Component {
        if ((!showInChat && team.isEmpty()) || (byName.isEmpty() && chatTags.isEmpty())) return line
        val plain = ChatFormatting.stripFormatting(line.string) ?: return line
        if (LOGO in plain) return line
        val colon = plain.indexOf(": ")
        if (colon !in 1..MAX_SENDER_LENGTH) return line
        val sender = WORD.findAll(plain.substring(0, colon)).map { it.value }
            .firstOrNull { (it.lowercase() in byName || it.lowercase() in chatTags) && shown(showInChat, it) } ?: return line
        val badge = byName[sender.lowercase()]
        return recolor(
            line, sender, badge, TeamTagPlace.CHAT,
            tag = chatTags[sender.lowercase()],
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

    private fun recolored(line: Component, player: String, place: String, tag: TagLook?): Component {
        val lower = player.lowercase()
        // Moving colors change every frame, so those lines skip the cache
        if (lower in (if (place == TeamTagPlace.TAB) movingInTab else movingAtHead)) {
            return recolor(line, player, byName[lower], place, tag, animate = true)
        }
        val key = Triple(line, lower, place)
        synchronized(recolorCache) { recolorCache[key]?.let { return it } }
        val result = recolor(line, player, byName[lower], place, tag)
        synchronized(recolorCache) { recolorCache[key] = result }
        return result
    }

    private class StyledChar(val char: Char, val style: Style)

    // Hypixel mixes styles and § codes, so the line is read char by char and built again.
    // The team tag goes in as chars too (in front of the whole name tag above heads, in front of the name elsewhere),
    // so it can share a chain with the parts next to it
    private fun recolor(
        line: Component,
        playerName: String,
        badge: SboBadge?,
        place: String,
        tag: TagLook? = null,
        afterName: Component? = null,
        animate: Boolean = false,
    ): Component {
        val read = ArrayList<StyledChar>()
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
                read += StyledChar(text[i], current)
                i++
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)

        val readPlain = String(CharArray(read.size) { read[it].char })
        val foundName = Regex("(?<![A-Za-z0-9_])${Regex.escape(playerName)}(?![A-Za-z0-9_])", RegexOption.IGNORE_CASE)
            .find(readPlain) ?: return line
        // Name, level and rank each take their own color where the badge shows them; without a badge (team tag only) nothing is colored
        val nameColor = badge?.takeIf { it.shows(BadgePart.NAME, place) }?.colorOf(BadgePart.NAME)
        val levelColor = badge?.takeIf { it.shows(BadgePart.LEVEL, place) }?.colorOf(BadgePart.LEVEL)
        val rankColor = badge?.takeIf { it.shows(BadgePart.RANK, place) }?.colorOf(BadgePart.RANK)
        val tagInfo = tag?.let { ranks[it.rank] }
        val tagText = tagInfo?.tag
        if (nameColor == null && levelColor == null && rankColor == null && tagText == null && afterName == null) return line

        // A name another mod already colored (not one of the 16 § colors Hypixel uses) keeps its color, only the tag and logo go in
        val foreign = foundName.range.any { i -> read[i].style.color?.let { it.value !in CHAT_COLORS } == true }
        if (foreign && tagText == null && afterName == null) return line

        // "[SBO Dev] " goes in as chars, everything behind it moves along
        val insertAt = if (place == TeamTagPlace.HEAD) 0 else foundName.range.first
        val tagChars = tagText?.let { "[$it] " }.orEmpty()
        val chars = ArrayList<StyledChar>(read.size + tagChars.length)
        chars.addAll(read.subList(0, insertAt))
        tagChars.forEach { chars += StyledChar(it, Style.EMPTY) }
        chars.addAll(read.subList(insertAt, read.size))
        fun moved(range: IntRange) = if (range.first >= insertAt) range.first + tagChars.length..range.last + tagChars.length else range
        val plain = String(CharArray(chars.size) { chars[it].char })
        val name = moved(foundName.range)

        // Found in the line as it came, so a tag never reads as a rank. In chat and for some ranks in the tab list
        // Hypixel's rank stands between level and name
        val rankStart = RANK.find(readPlain.substring(0, foundName.range.first))?.range?.first
        val levelEnd = rankStart ?: foundName.range.first
        val levelRange = levelColor?.let { LEVEL.find(readPlain.substring(0, levelEnd))?.range?.first }?.let { moved(it until levelEnd) }
        val rankRange = rankColor?.let { rankStart }?.let { moved(it until foundName.range.first) }
        // Without the space behind it, so the gap check below sees it
        val tagRange = tagText?.let { insertAt until insertAt + tagChars.length - 1 }
        val tagColor = tag?.color ?: tagInfo?.let { PartColor(it.color, it.to) }

        val now = System.currentTimeMillis()
        val segments = listOfNotNull(
            levelColor?.let { color -> levelRange?.takeUnless { foreign }?.let { Segment(BadgePart.LEVEL, color, it) } },
            rankColor?.let { color -> rankRange?.takeUnless { foreign }?.let { Segment(BadgePart.RANK, color, it) } },
            nameColor?.takeUnless { foreign }?.let { Segment(BadgePart.NAME, it, name) },
            tagColor?.let { color -> tagRange?.let { Segment(BadgePart.TAG, color, it) } },
        ).sortedBy { it.range.first }
        if (segments.isEmpty() && afterName == null) return line
        // Only spaces may stand between chained parts
        val paints = paints(segments) { before, next -> (before.range.last + 1 until next.range.first).all { plain[it] == ' ' } }
        // Brackets and the rank's plus can have their own color; the tag's brackets are gray without one,
        // unless the tag flows in a line with the parts next to it
        fun colorOf(k: Int, i: Int, char: Char): Int {
            val segment = segments[k]
            val color = segment.color
            val tagBracket = segment.part == BadgePart.TAG && color.chain != true && (i == segment.range.first || i == segment.range.last)
            return when {
                char == '[' && color.open != null -> parseHex(color.open)
                char == ']' && color.close != null -> parseHex(color.close)
                tagBracket -> TAG_BRACKETS
                char == '+' && color.plus != null -> parseHex(color.plus)
                else -> paints[k].at(i, animate, now)
            }
        }
        val out = Component.empty()
        var run = StringBuilder()
        var runStyle: Style? = null
        fun flush() {
            runStyle?.let { out.append(Component.literal(run.toString()).withStyle(it)) }
            run = StringBuilder()
        }
        chars.forEachIndexed { i, c ->
            val k = segments.indexOfFirst { i in it.range }
            val colored = if (k < 0) c.style else c.style.withColor(colorOf(k, i, c.char))
            // Chars only move above heads
            val style = if (k < 0 || place != TeamTagPlace.HEAD) colored else paints[k].moved(colored, i, c.char, animate, now)
            if (style != runStyle) {
                flush()
                runStyle = style
            }
            run.append(c.char)
            if (afterName != null && i == name.last) {
                flush()
                runStyle = null
                out.append(afterName)
            }
        }
        flush()
        return out
    }

    @Volatile private var loggedError = false

    // These run inside the game's and other mods' rendering and chat, a bug here must never cost the line or the frame
    private inline fun <T> safe(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        if (!loggedError) {
            loggedError = true
            SBOKotlin.logger.error("[SboBadges] decorating failed, showing the line without badge", e)
        }
        fallback
    }

    fun logo(): MutableComponent = Component.literal(LOGO).withStyle(LOGO_STYLE)

    fun colored(text: String, color: PartColor, part: String, animate: Boolean): MutableComponent {
        if (color.to == null && color.effectOf(part) == BadgeEffect.STATIC && (!animate || color.motion.isStill())) {
            return Component.literal(text).withStyle { it.withColor(parseHex(color.color)) }
        }
        val now = System.currentTimeMillis()
        val out = Component.empty()
        text.forEachIndexed { i, char ->
            out.append(Component.literal(char.toString()).withStyle {
                moved(it.withColor(colorAt(color, part, i, text.length, animate, now)), color.motion, i, text.length, char, animate, now)
            })
        }
        return out
    }

    /** A colored part of a line and its chars. */
    class Segment(val part: String, val color: PartColor, val range: IntRange)

    /** The colors and motion of a part, counted over the chars of its whole chain. */
    class Paint(val color: PartColor, val part: String, val motion: String?, val range: IntRange, private val colorRange: IntRange) {
        private val length get() = range.last - range.first + 1
        fun at(i: Int, animate: Boolean, now: Long): Int =
            colorAt(color, part, i - colorRange.first, colorRange.last - colorRange.first + 1, animate, now)
        fun moved(style: Style, i: Int, char: Char, animate: Boolean, now: Long): Style = SboBadges.moved(style, motion, i - range.first, length, char, animate, now)
        fun dropAt(i: Int, now: Long): Int = SboBadges.dropAt(motion, i - range.first, length, now)
        fun glitchAt(i: Int, now: Long): Boolean = SboBadges.glitchAt(motion, i - range.first, length, now)
    }

    /**
     * Paints of [segments] in text order: a part with `chain` flows on from the one before it while [touching] says
     * the two stand right next to each other. Every part keeps its own colors, effect and motion; a chain only counts
     * their chars over all of its parts, so a wave on level and name runs on as one wave instead of starting again.
     * Colors that stand still keep their gradient over the part itself.
     */
    fun paints(segments: List<Segment>, touching: (Segment, Segment) -> Boolean): List<Paint> {
        val starts = IntArray(segments.size)
        for (k in segments.indices) {
            val joins = k > 0 && segments[k].color.chain == true && touching(segments[k - 1], segments[k])
            starts[k] = if (joins) starts[k - 1] else k
        }
        return segments.indices.map { k ->
            val chain = segments.indices.filter { starts[it] == starts[k] }
            val own = segments[k]
            val range = segments[chain.first()].range.first..segments[chain.last()].range.last
            Paint(own.color, own.part, own.color.motion, range, if (colorMoves(own.color, own.part)) range else own.range)
        }
    }

    /** Whether the part's colors or chars change over time. */
    fun moves(color: PartColor, part: String): Boolean = colorMoves(color, part) || !color.motion.isStill()

    /** Whether the part changes over time at [place]; chars only move above heads, the tab list and chat only change colors. */
    private fun movesAt(color: PartColor, part: String, place: String): Boolean =
        if (place == TeamTagPlace.HEAD) moves(color, part) else colorMoves(color, part)

    private fun colorMoves(color: PartColor, part: String): Boolean = when (color.effectOf(part)) {
        BadgeEffect.WAVE -> color.to != null
        BadgeEffect.STATIC -> false
        else -> true
    }

    private fun String?.isStill(): Boolean = this == null || this == BadgeMotion.NONE

    /**
     * Color of char [i] of a text [length] chars long; with [animate] false moving effects stand still:
     * they show the plain gradient, chroma a fixed rainbow.
     */
    fun colorAt(color: PartColor, part: String, i: Int, length: Int, animate: Boolean, now: Long = System.currentTimeMillis()): Int {
        val from = parseHex(color.color)
        val to = color.to?.let(::parseHex)
        val t = i / (length - 1).coerceAtLeast(1).toFloat()
        val still = to?.let { mix(from, it, t) } ?: from
        if (!animate) {
            return if (color.effectOf(part) == BadgeEffect.CHROMA) hue(i / length.toFloat() * CHROMA_SPREAD) else still
        }
        return when (color.effectOf(part)) {
            BadgeEffect.WAVE -> {
                if (to == null) return from
                // There and back again, so the moving gradient has no seam
                val pos = (i / length.toFloat() + phase(now, WAVE_MS)) % 1f
                mix(from, to, if (pos < 0.5f) pos * 2f else (1f - pos) * 2f)
            }
            BadgeEffect.PULSE -> {
                val fade = (1f - cos(phase(now, PULSE_MS) * 2f * PI.toFloat())) / 2f
                mix(from, to ?: mix(from, 0xFFFFFF, PULSE_WHITE), fade)
            }
            // Runs from the end of the text to its start, like the wave
            BadgeEffect.CHROMA -> hue((i / length.toFloat() * CHROMA_SPREAD + 1f - phase(now, CHROMA_MS)) % 1f)
            BadgeEffect.SHINE -> {
                // The gleam crosses in the first part of the period, then the text rests
                val center = phase(now, SHINE_MS) * SHINE_REST - SHINE_WIDTH
                val distance = kotlin.math.abs((i + 0.5f) / length - center)
                if (distance >= SHINE_WIDTH) still else mix(still, 0xFFFFFF, (1f - distance / SHINE_WIDTH) * SHINE_STRENGTH)
            }
            BadgeEffect.FLASH -> if (phase(now, FLASH_MS) < 0.5f) still else to?.let { mix(it, from, t) } ?: mix(from, 0xFFFFFF, PULSE_WHITE)
            BadgeEffect.SPARKLE -> if (noise(i, now / SPARKLE_MS) % SPARKLE_ONE_IN == 0) mix(still, 0xFFFFFF, SHINE_STRENGTH) else still
            else -> still
        }
    }

    /** The style with the char moved by [motion]; only plain chars move, the shifted fonts only have those. */
    private fun moved(style: Style, motion: String?, i: Int, length: Int, char: Char, animate: Boolean, now: Long): Style {
        if (!animate || motion.isStill() || char !in ' '..'~') return style
        if (glitchAt(motion, i, length, now)) return style.withObfuscated(true)
        val drop = dropAt(motion, i, length, now)
        return if (drop > 0) style.withFont(SHIFT_FONTS[drop - 1]) else style
    }

    /** How many pixels char [i] sits lower, 0 to [SHIFT_FONTS] size. */
    fun dropAt(motion: String?, i: Int, length: Int, now: Long): Int = when (motion) {
        BadgeMotion.WAVY -> {
            val wave = (1f - cos((phase(now, WAVY_MS) - i * WAVY_STEP) * 2f * PI.toFloat())) / 2f
            kotlin.math.round(wave * WAVY_DEPTH).toInt()
        }
        BadgeMotion.BOUNCE -> {
            // One hop runs over the text, then a short rest
            val hop = phase(now, BOUNCE_MS) * (length + BOUNCE_REST) - i
            if (hop in 0f..1f) kotlin.math.round(kotlin.math.sin(hop * PI.toFloat()) * SHIFT_FONTS.size).toInt() else 0
        }
        BadgeMotion.SHAKE -> if (noise(i, now / SHAKE_MS) % 3 == 0) 1 else 0
        else -> 0
    }

    /** Whether char [i] shows scrambled right now. */
    fun glitchAt(motion: String?, i: Int, length: Int, now: Long): Boolean =
        motion == BadgeMotion.GLITCH && noise(i, now / GLITCH_MS) % (length * GLITCH_SPREAD).coerceAtLeast(GLITCH_SPREAD) == 0

    /** Same number for the same char and time slot, so a frame drawn twice looks the same. */
    private fun noise(i: Int, slot: Long): Int {
        var h = slot * 0x9E3779B97F4A7C15uL.toLong() + i * 0xBF58476D1CE4E5B9uL.toLong()
        h = (h xor (h ushr 31)) * 0x94D049BB133111EBuL.toLong()
        return ((h xor (h ushr 29)) and 0x7FFFFFFF).toInt()
    }
    private fun phase(now: Long, periodMs: Long): Float = (now % periodMs) / periodMs.toFloat()

    /** Full, bright color of a hue from 0 to 1. */
    private fun hue(h: Float): Int {
        val x = h * 6f
        fun channel(offset: Float): Int {
            val k = (offset + x) % 6f
            return ((1f - maxOf(0f, minOf(k, 4f - k, 1f))) * 255).toInt()
        }
        return (channel(5f) shl 16) or (channel(3f) shl 8) or channel(1f)
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
