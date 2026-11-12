package net.sbo.mod.general

import com.google.common.collect.MapMaker
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.ComponentRenderUtils
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.CustomizerGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeColors
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
    const val COMMAND = "sbocustomizer"
    // The old name keeps working
    private const val OLD_COMMAND = "sbobadge"

    // Switches for other players' badges in the tab list and chat in /sbocustomizer; false hides them and always shows badges.
    // They only hide other supporters and contributors, the team (moderators and above) and your own badge always show; above heads always shows
    private val showInTab: Boolean get() = DataManager.sboData.badgesInTab
    private val showInChat: Boolean get() = DataManager.sboData.badgesInChat

    // Names anywhere in a chat message (not only the sender's) in their badge look: nobody's, only your own, or everyone's
    const val NAMES_OFF = "off"
    const val NAMES_OWN = "own"
    const val NAMES_ALL = "all"

    private const val LIST_REFRESH_MINUTES = 10

    // The SBO logo is this char in the sbo:badge font
    const val LOGO = ""
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
    // Phase between two chars of the wavy motion
    private const val WAVY_STEP = 0.12f
    private const val BOUNCE_MS = 2200L
    // Chars of rest after the hop ran over the text
    private const val BOUNCE_REST = 4
    private const val SHAKE_MS = 90L
    private const val GLITCH_MS = 80L
    // Glitch: about one char in GLITCH_SPREAD lines scrambles per slot
    private const val GLITCH_SPREAD = 1
    // The default font 1 to 3 pixels lower (assets/sbo/font/shift*.json), fonts cannot rise above the line
    private val SHIFT_FONTS = (1..3).map { FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "shift$it")) }

    /** Pixels a char with this style sits lower through the motion fonts, 0 for every other style. */
    fun dropOf(style: Style): Int = SHIFT_FONTS.indexOf(style.font) + 1

    // A player's name starts the line or follows the level or rank: "[296] Name ♲", "[MVP+] Name"
    private val NAME = Regex("(?:^|] )([A-Za-z0-9_]{1,16})")
    // In chat the emblem stands between level and rank: "[466] ♙ [MVP+] Name"
    private val LEVEL = Regex("""(\[\d+])(?:\s*[^\[\]\sA-Za-z0-9_]+)?\s*$""")
    // Hypixel's rank right before the name: "[MVP++] Name", "[YOUTUBE] Name"
    private val RANK = Regex("""\[[A-Z]+\+*]\s*$""")
    private const val TAG_BRACKETS = 0xAAAAAA
    // The sender stands before the first ": " of a chat line: "[296] [MVP+] Name: hi", "Party > Name: hi", "From Name: hi"
    private const val MAX_SENDER_LENGTH = 80
    // Mods rebuild Hypixel's § colors as rgb colors, so they are compared by value
    private val CHAT_COLORS: Set<Int> = ChatFormatting.entries.mapNotNull { TextColor.fromLegacyFormat(it)?.value }.toSet()
    private val WORD = Regex("[A-Za-z0-9_]{1,16}")
    // A whole word, so the end of a longer word never reads as a name
    private val MENTION = Regex("[A-Za-z0-9_]+")

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
    @Volatile private var movingEverywhere: Set<String> = emptySet()
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
        Register.command(COMMAND, OLD_COMMAND) {
            mc.schedule { CustomizerGui.open() }
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
        val badges = response.badges.map { badge -> if (uuidOf(badge.uuid)?.let(::movesChars) != false) badge else badge.still() }
        val holders = badges.mapNotNull { badge -> uuidOf(badge.uuid)?.let { it to badge } }.toMap()
        val members = response.team.mapNotNull { member -> uuidOf(member.uuid)?.let { it to member } }.toMap()
        ranks = response.ranks
        team = members
        teamNames = members.values.mapTo(HashSet()) { it.name.lowercase() }
        // The tag takes the member's tag color, the rank's while their badge is off
        val tagged = members.asSequence().filter { ranks[it.value.rank]?.tag != null }.map { (uuid, member) ->
            val color = holders[uuid]?.colorOf(BadgePart.TAG)
            Triple(uuid, member, TagLook(member.rank, color, animates(uuid)))
        }.toList()
        fun byName(place: String) = tagged.asSequence().filter { place in it.second.tagPlaces }.associate { (_, member, tag) -> member.name.lowercase() to tag }
        tags = tagged.asSequence().filter { TeamTagPlace.HEAD in it.second.tagPlaces }.associate { (uuid, _, tag) -> uuid to tag }
        tabTags = byName(TeamTagPlace.TAB)
        chatTags = byName(TeamTagPlace.CHAT)
        fun movingAt(place: String, tagsAt: Map<String, TagLook>) = holders.filter { (uuid, badge) ->
            animates(uuid) && listOf(BadgePart.NAME, BadgePart.LEVEL, BadgePart.RANK).any { badge.shows(it, place) && movesAt(badge.colorOf(it), it, place) }
        }.mapTo(HashSet()) { it.value.name.lowercase() } +
            tagsAt.filterValues { tag -> tag.animated && tag.color?.let { movesAt(it, BadgePart.TAG, place) } == true }.keys
        movingAtHead = movingAt(TeamTagPlace.HEAD, byName(TeamTagPlace.HEAD))
        // Off, every tab line stands still, your own too
        movingInTab = if (DataManager.sboData.badgeAnimationsInTab) movingAt(TeamTagPlace.TAB, tabTags) else emptySet()
        // Names recolored everywhere look like names inside chat messages, with their rank always colored
        movingEverywhere = holders.filter { (uuid, badge) ->
            animates(uuid) && listOf(BadgePart.NAME, BadgePart.LEVEL, BadgePart.RANK).any {
                (it == BadgePart.RANK || badge.shows(it, TeamTagPlace.CHAT)) && movesAt(badge.colorOf(it), it, TeamTagPlace.CHAT)
            }
        }.mapTo(HashSet()) { it.value.name.lowercase() }
        fixedLines = holders.filter { (uuid, badge) -> !moves(badge.colorOf(BadgePart.STAT), BadgePart.STAT) || !animates(uuid) }
            .mapValues { line(it.value, animate = false) }
        byUuid = holders
        byName = badges.associateBy { it.name.lowercase() }
        version = response.version
        lastList = response
        synchronized(tabCache) { tabCache.clear() }
        synchronized(recolorCache) { recolorCache.clear() }
        synchronized(scans) { scans.clear() }
    }

    /** Builds the badges again after the viewer turned animations of others on or off. */
    fun relook() {
        lastList?.let { mc.execute { apply(it) } }
    }

    /** Your own badge always moves, other players' only while the viewer allows it. */
    private fun animates(uuid: UUID): Boolean = DataManager.sboData.badgeAnimationsOfOthers || uuid == uuidOf(Player.accountUuid())

    /** Your own badge's chars always move, other players' only while the viewer allows it; their colors still move. */
    private fun movesChars(uuid: UUID): Boolean = DataManager.sboData.badgeMotionOfOthers || uuid == uuidOf(Player.accountUuid())

    /** The badge with its chars standing still. */
    private fun SboBadge.still(): SboBadge {
        val parts = colors ?: return this
        fun PartColor?.still() = this?.copy(motion = null)
        return copy(colors = BadgeColors(parts.stat.still(), parts.name.still(), parts.level.still(), parts.tag.still(), parts.rank.still()))
    }

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

    private fun isOwn(badge: SboBadge): Boolean = badge.uuid.replace("-", "").equals(Player.accountUuid().replace("-", ""), ignoreCase = true)

    /**
     * Symbols around a part as the viewer sees them, with the space between them and the part: other players' only while
     * allowed, your own always; none where text keeps its width.
     */
    private fun symbolsOf(badge: SboBadge?, part: String, keepWidth: Boolean = false): Pair<String, String> {
        if (badge == null || keepWidth || !stylesShown(badge)) return NO_SYMBOLS
        val color = badge.colors?.get(part)
        // Backends before symbols per part only had the name's
        val look = badge.look.takeIf { part == BadgePart.NAME }
        val before = color?.before ?: look?.before
        val after = color?.after ?: look?.after
        return before?.let { "$it " }.orEmpty() to after?.let { " $it" }.orEmpty()
    }

    private val NO_SYMBOLS = "" to ""

    /** Whether the viewer sees the badge's text styles: other players' only while allowed, your own always. */
    private fun stylesShown(badge: SboBadge?): Boolean = badge != null && (DataManager.sboData.badgeLooksOfOthers || isOwn(badge))

    /** Model size of a player, 1 without a badge size or while the viewer turned sizes of others off. */
    fun sizeOf(uuid: UUID): Float {
        if (byUuid.isEmpty()) return 1f
        val badge = byUuid[uuid] ?: return 1f
        val size = badge.look?.size ?: return 1f
        if (!DataManager.sboData.badgeSizesOfOthers && !isOwn(badge)) return 1f
        // The backend only sends these, a broken value never makes a giant
        return size.coerceIn(MIN_SIZE, MAX_SIZE)
    }

    /** Set while a menu (the inventory) builds a player model: no badge, name tag or size there. Render thread only. */
    var menuModel = false

    private const val MIN_SIZE = 0.1f
    private const val MAX_SIZE = 1.75f

    /** The viewer's switch for a place hides other supporters and contributors, never the team or your own badge. */
    private fun shown(enabled: Boolean, player: String) =
        enabled || player.lowercase() in teamNames || player.equals(own()?.name, ignoreCase = true)

    fun nameTagLine(uuid: UUID): Component? = safe(null) { nameTagLineOf(uuid) }

    private fun nameTagLineOf(uuid: UUID): Component? {
        val badge = byUuid[uuid]?.takeIf(::hasHeadLine) ?: return null
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
    private fun tagOf(rank: String, partColor: PartColor? = null): Component? {
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

    /**
     * A line as [badge] makes it at [place], for the customizer's preview: [base] is the line as Hypixel sends it,
     * [tagRank] the team rank whose tag shows there (null for none). Built by the same code as in game.
     */
    fun previewLine(badge: SboBadge, base: Component, place: String, tagRank: String?, animate: Boolean): Component = safe(base) {
        val tag = tagRank?.let { TagLook(it, badge.colorOf(BadgePart.TAG), animated = true) }
        val logoAfterName = place == TeamTagPlace.CHAT && badge.shows(BadgePart.LOGO, place)
        val line = recolor(
            base, badge.name, badge, place, tag,
            afterName = if (logoAfterName) Component.literal(" ").append(logo()) else null,
            animate = animate,
            moveChars = animate && place != TeamTagPlace.TAB,
        )
        if (place == TeamTagPlace.TAB && badge.shows(BadgePart.LOGO, place)) line.copy().append(" ").append(logo()) else line
    }

    /** Whether the badge has a line above the head: the logo, the stat or both. */
    fun hasHeadLine(badge: SboBadge): Boolean =
        badge.shows(BadgePart.LOGO, TeamTagPlace.HEAD) || badge.shows(BadgePart.STAT, TeamTagPlace.HEAD) && badge.label != null && badge.value != null

    /** The line above the head: logo and stat where the badge shows them. */
    fun line(badge: SboBadge, animate: Boolean): MutableComponent {
        // The logo is a child, the text after it would take over its font
        val line = Component.empty()
        val withLogo = badge.shows(BadgePart.LOGO, TeamTagPlace.HEAD)
        if (withLogo) line.append(logo())
        if (badge.shows(BadgePart.STAT, TeamTagPlace.HEAD) && badge.label != null && badge.value != null) {
            if (withLogo) line.append(" ")
            val (before, after) = symbolsOf(badge, BadgePart.STAT)
            line.append(colored("$before${badge.label}: ${badge.value}$after", badge.colorOf(BadgePart.STAT), BadgePart.STAT, animate, stylesShown(badge)))
        }
        return line
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component = safe(line) { decorateTabOf(info, line) }

    private fun decorateTabOf(info: PlayerInfo, line: Component): Component {
        if (compactReading || !showInTab && team.isEmpty() && own() == null || byUuid.isEmpty() && tabTags.isEmpty()) return line
        val player = tabPlayerOf(info)?.takeIf { shown(showInTab, it) } ?: return line
        return decorateTab(line, player)
    }

    // SkyHanni's or SkyblockAddons' compact tab list reads the lines while this is set
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

    /** A § coded line SkyblockAddons' compact tab list draws, decorated like ours; null when it has no badge holder or tagged member. */
    fun decorateSbaTab(line: String): Component? = if (line.isEmpty()) null else decorateSkyHanniTab(Component.literal(line))

    /** A line SkyHanni's compact tab list draws, decorated like ours; null when it has no badge holder or tagged member. */
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
        val logo = byName[player.lowercase()]?.shows(BadgePart.LOGO, TeamTagPlace.TAB) == true
        return if (logo) decorated.copy().append(" ").append(logo()) else decorated
    }

    /**
     * The sender of a chat line: team tag in front, badge color and the SBO logo behind the name.
     * With [NAMES_OWN] or [NAMES_ALL] names inside the message get tag and color too.
     */
    fun decorateChat(line: Component): Component = safe(line) {
        val decorated = decorateChatOf(line, animate = false)
        // Lines with moving badges are built again while they are drawn, see animatedChatLine
        if (decorated !== line && movesInChat(line)) animatedMessages[decorated] = AnimatedMessage(line)
        decorated
    }

    /** Whether a name the chat line decorates has moving colors or chars that the viewer lets move in chat. */
    private fun movesInChat(line: Component): Boolean {
        if (DataManager.sboData.badgeAnimationsInChat == NAMES_OFF) return false
        val plain = ChatFormatting.stripFormatting(line.string) ?: return false
        return MENTION.findAll(plain).any { animatesInChat(it.value) }
    }

    /** Whose badge moves in chat: nobody's, only your own or everyone's whose animations the viewer allows. */
    private fun animatesInChat(name: String): Boolean {
        val lower = name.lowercase()
        val badge = byName[lower] ?: return false
        val uuid = uuidOf(badge.uuid) ?: return false
        val allowed = when (DataManager.sboData.badgeAnimationsInChat) {
            NAMES_ALL -> animates(uuid)
            NAMES_OWN -> uuid == uuidOf(Player.accountUuid())
            else -> false
        }
        if (!allowed) return false
        val parts = listOf(BadgePart.NAME, BadgePart.LEVEL, BadgePart.RANK).filter { badge.shows(it, TeamTagPlace.CHAT) } +
            listOfNotNull(BadgePart.TAG.takeIf { lower in chatTags })
        return parts.any { moves(badge.colorOf(it), it) }
    }

    private class AnimatedMessage(val original: Component) {
        var frame = -1L
        var width = -1
        var lines: List<FormattedCharSequence> = emptyList()
    }
    private class AnimatedLine(val message: AnimatedMessage, val width: Int, val index: Int, val count: Int)
    // By the objects themselves (weak, by identity), gone once the chat drops the message
    private val animatedMessages: MutableMap<Component, AnimatedMessage> = MapMaker().weakKeys().makeMap()
    private val animatedLines: MutableMap<FormattedCharSequence, AnimatedLine> = MapMaker().weakKeys().makeMap()
    private const val CHAT_FRAME_MS = 25L

    /** The chat split a message into lines; lines of a message with moving badges are remembered. */
    fun splitChatLines(content: Component, width: Int, lines: List<FormattedCharSequence>) {
        if (animatedMessages.isEmpty()) return
        val message = animatedMessages[content] ?: return
        lines.forEachIndexed { i, line -> animatedLines[line] = AnimatedLine(message, width, i, lines.size) }
    }

    /** A chat line with moving badges as it looks right now, null for every other text. */
    fun animatedChatLine(text: FormattedCharSequence?): FormattedCharSequence? {
        if (text == null || animatedLines.isEmpty()) return null
        val line = animatedLines[text] ?: return null
        return safe(null) {
            val message = line.message
            val frame = System.currentTimeMillis() / CHAT_FRAME_MS
            synchronized(message) {
                // Built once per frame for all lines of the message; colors and motion never change the width, so it splits the same
                if (message.frame != frame || message.width != line.width) {
                    val decorated = decorateChatOf(message.original, animate = true)
                    message.lines = ComponentRenderUtils.wrapComponents(decorated, line.width, mc.font)
                    message.frame = frame
                    message.width = line.width
                }
                message.lines.takeIf { it.size == line.count }?.get(line.index)
            }
        }
    }

    private fun decorateChatOf(line: Component, animate: Boolean): Component {
        val mentions = DataManager.sboData.badgeNamesInChat
        if (mentions != NAMES_OWN && !showInChat && team.isEmpty() && own() == null || byName.isEmpty() && chatTags.isEmpty()) return line
        val plain = ChatFormatting.stripFormatting(line.string) ?: return line
        if (LOGO in plain) return line
        val colon = plain.indexOf(": ").takeIf { it in 1..MAX_SENDER_LENGTH }
        val sender = colon?.let { end -> WORD.findAll(plain.take(end)).map { it.value }.firstOrNull(::shownInChat) }
        val wanted = wantedNames()
        var out = line
        // Right to left, so a tag put in never moves the names still to come; the sender stands before all of them
        if (wanted != null) {
            MENTION.findAll(plain, colon?.plus(2) ?: 0).filter { wanted(it.value) }.toList().asReversed()
                .forEach { out = decorateChatName(out, it.value, from = it.range.first, nameOnly = true, animate = animate) }
        }
        if (sender != null) out = decorateChatName(out, sender, animate = animate)
        return out
    }

    /** Which names inside a message get their look, null while names in messages are off. */
    private fun wantedNames(): ((String) -> Boolean)? = when (DataManager.sboData.badgeNamesInChat) {
        NAMES_ALL -> ::shownInChat
        NAMES_OWN -> ownName()?.let { own -> { name: String -> name.equals(own, ignoreCase = true) } }
        else -> null
    }

    private fun shownInChat(name: String): Boolean =
        name.length <= 16 && (name.lowercase() in byName || name.lowercase() in chatTags) && shown(showInChat, name)

    /** Your own name while you have a badge or a team tag in chat. */
    private fun ownName(): String? = own()?.name ?: uuidOf(Player.accountUuid())?.let { team[it] }?.name?.takeIf { it.lowercase() in chatTags }

    private fun decorateChatName(line: Component, name: String, from: Int = 0, nameOnly: Boolean = false, animate: Boolean = false): Component {
        val badge = byName[name.lowercase()]
        val moving = animate && animatesInChat(name)
        return recolor(
            line, name, badge, TeamTagPlace.CHAT,
            tag = chatTags[name.lowercase()]?.takeIf { !nameOnly || DataManager.sboData.badgeNamesTag },
            // The logo only behind the sender
            afterName = badge?.takeIf { !nameOnly && it.shows(BadgePart.LOGO, TeamTagPlace.CHAT) }?.let { Component.literal(" ").append(logo()) },
            from = from,
            nameOnly = nameOnly,
            animate = moving,
            moveChars = moving,
        )
    }

    /**
     * Recolors the badge holders' names that names in messages picks (and the level/rank in front of them) in any text that is drawn:
     * GUIs, titles, signs, scoreboards, books... Returns null when nothing changes.
     */
    fun replaceEverywhere(text: FormattedCharSequence?): FormattedCharSequence? {
        if (text == null || !DataManager.sboData.badgeReplaceEverywhere || byName.isEmpty()) return null
        return safe(null) { replaceEverywhereOf(text) }
    }

    private fun replaceEverywhereOf(text: FormattedCharSequence): FormattedCharSequence? {
        // Names in messages decide whose names are recolored, off recolors none
        val mode = DataManager.sboData.badgeNamesInChat
        if (mode == NAMES_OFF) return null
        val list = byName
        val inChat = showInChat
        // Every drawn text passes here (drt and SkyOcean scanning all of it cost players whole frames). Chat lines and most
        // component text come back as the same object every frame, so what was found in it is kept: text without a name
        // then costs one lookup, text whose colors stand still is built once
        val known = synchronized(scans) { scans[text] }?.takeIf { it.list === list && it.mode == mode && it.inChat == inChat }
        if (known != null) {
            if (known.found.isEmpty()) return null
            known.still?.let { return it }
        }
        val found = known?.found ?: namesIn(text, list, wantedNames() ?: return null)
        if (found.isEmpty()) {
            synchronized(scans) { scans[text] = Scan(list, mode, inChat, found, null) }
            return null
        }

        val runs = ArrayList<Pair<String, Style>>()
        val run = StringBuilder()
        var runStyle: Style? = null
        text.accept { _, style, codePoint ->
            if (style != runStyle) {
                runStyle?.let { runs += run.toString() to it }
                run.setLength(0)
                runStyle = style
            }
            run.appendCodePoint(codePoint)
            true
        }
        runStyle?.let { runs += run.toString() to it }

        var out: Component = Component.empty().also { line -> runs.forEach { (s, style) -> line.append(Component.literal(s).withStyle(style)) } }
        // Right to left, like names inside chat messages
        for (name in found.asReversed()) {
            out = recolor(
                out, name.name, list[name.name.lowercase()], TeamTagPlace.CHAT,
                // No tag or logo, so the text keeps its width and recoloring it again changes nothing
                animate = name.name.lowercase() in movingEverywhere, from = name.start, forceRank = true, keepWidth = true,
            )
        }
        val result = out.visualOrderText
        val still = found.none { it.name.lowercase() in movingEverywhere }
        synchronized(scans) { scans[text] = Scan(list, mode, inChat, found, result.takeIf { still }) }
        return result
    }

    /** Badge holders' names in the text, written exactly like theirs; empty when it holds none. */
    private fun namesIn(text: FormattedCharSequence, list: Map<String, SboBadge>, wanted: (String) -> Boolean): List<FoundName> {
        val plain = StringBuilder()
        text.accept { _, _, codePoint -> plain.appendCodePoint(codePoint); true }
        if (plain.indexOf(LOGO) >= 0) return emptyList()
        var found: MutableList<FoundName>? = null
        var start = -1
        // Whole words only, so the end of a longer word never reads as a name; no regex, this runs for every new text
        for (i in 0..plain.length) {
            val inWord = i < plain.length && isNameChar(plain[i])
            if (inWord) {
                if (start < 0) start = i
                continue
            }
            if (start >= 0 && i - start <= 16) {
                val word = plain.substring(start, i)
                if (list[word.lowercase()]?.name == word && wanted(word)) {
                    (found ?: ArrayList<FoundName>().also { found = it }).add(FoundName(word, start))
                }
            }
            start = -1
        }
        return found ?: emptyList()
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

    private class FoundName(val name: String, val start: Int)
    // What a drawn text held, valid for the list and settings it was found with; still is the finished text when its colors stand still
    private class Scan(val list: Map<String, SboBadge>, val mode: String, val inChat: Boolean, val found: List<FoundName>, val still: FormattedCharSequence?)
    // By the text object itself, gone once the game drops the text
    private val scans = WeakHashMap<FormattedCharSequence, Scan>()

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

    private fun findPlayerNameRange(
        text: String,
        playerName: String,
        from: Int = 0,
    ): IntRange? {
        if (playerName.isEmpty()) return null

        val nameLength = playerName.length
        val startFrom = from.coerceIn(0, text.length)

        for (start in startFrom..text.length - nameLength) {
            val end = start + nameLength

            // The character before the name must not be [A-Za-z0-9_].
            if (start > 0 && isNameChar(text[start - 1])) continue

            // The character after the name must not be [A-Za-z0-9_].
            if (end < text.length && isNameChar(text[end])) continue

            if (text.regionMatches(
                    ignoreCase = true,
                    thisOffset = start,
                    other = playerName,
                    otherOffset = 0,
                    length = nameLength,
                )
            ) {
                return start..<end
            }
        }

        return null
    }

    private fun isNameChar(char: Char): Boolean =
        char in 'A'..'Z' ||
            char in 'a'..'z' ||
            char in '0'..'9' ||
            char == '_'

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
        // The name is looked for from this char on; nameOnly leaves level and rank in front of it alone (names inside a message)
        from: Int = 0,
        nameOnly: Boolean = false,
        // The rank takes its color even where the badge doesn't show it (names recolored everywhere)
        forceRank: Boolean = false,
        // Names recolored everywhere keep their width: no symbols, no bold
        keepWidth: Boolean = false,
        // Chars move above heads, and in chat while chat animations are on
        moveChars: Boolean = place == TeamTagPlace.HEAD,
    ): Component {
        val read = ArrayList<StyledChar>()
        val readPlainBuilder = StringBuilder()
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
                readPlainBuilder.append(text[i])
                i++
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)

        val readPlain = readPlainBuilder.toString()
        var foundName = findPlayerNameRange(readPlain, playerName, from) ?: return line
        // Name, level and rank each take their own color where the badge shows them; without a badge (team tag only) nothing is colored
        val nameColor = badge?.takeIf { it.shows(BadgePart.NAME, place) }?.colorOf(BadgePart.NAME)
        val levelColor = badge?.takeIf { !nameOnly && it.shows(BadgePart.LEVEL, place) }?.colorOf(BadgePart.LEVEL)
        val rankColor = badge?.takeIf { !nameOnly && (forceRank || it.shows(BadgePart.RANK, place)) }?.colorOf(BadgePart.RANK)
        val tagInfo = tag?.let { ranks[it.rank] }
        val tagText = tagInfo?.tag
        if (nameColor == null && levelColor == null && rankColor == null && tagText == null && afterName == null) return line

        // A name another mod already colored (not one of the 16 § colors Hypixel uses) keeps its color, only the tag and logo go in
        val foreign = foundName.any { i ->
            read[i].style.color?.let { it.value !in CHAT_COLORS } == true
        }
        if (foreign && tagText == null && afterName == null) return line

        val styles = stylesShown(badge)

        // Found in the line as it came, so a tag never reads as a rank. In chat and for some ranks in the tab list
        // Hypixel's rank stands between level and name
        val rankMatch = RANK.find(readPlain.take(foundName.first))
        val levelEnd = rankMatch?.range?.first ?: foundName.first
        // Only the brackets and digits, an emblem behind them keeps its own color
        var levelRange = levelColor?.takeUnless { foreign }?.let { LEVEL.find(readPlain.take(levelEnd))?.groups?.get(1)?.range }
        // The rank without the spaces behind it
        var rankRange = rankColor?.takeUnless { foreign }?.let { rankMatch }?.let { it.range.first..<it.range.first + it.value.trimEnd().length }

        // Symbols go in as chars of their part, so they take its colors, effect and style. Left to right: each moves the parts behind it
        var shift = 0
        fun withSymbols(range: IntRange, part: String): IntRange {
            val at = range.first + shift..range.last + shift
            val (before, after) = symbolsOf(badge, part, keepWidth)
            if (before.isEmpty() && after.isEmpty()) return at
            val first = read[at.first].style
            val last = read[at.last].style
            read.addAll(at.last + 1, after.map { StyledChar(it, last) })
            read.addAll(at.first, before.map { StyledChar(it, first) })
            shift += before.length + after.length
            return at.first..at.last + before.length + after.length
        }
        levelRange = levelRange?.let { withSymbols(it, BadgePart.LEVEL) }
        rankRange = rankRange?.let { withSymbols(it, BadgePart.RANK) }
        foundName = if (nameColor != null && !foreign) withSymbols(foundName, BadgePart.NAME) else foundName.first + shift..foundName.last + shift

        // "[SBO Dev] " goes in as chars with its symbols, everything behind it moves along
        val insertAt = if (place == TeamTagPlace.HEAD) 0 else foundName.first
        val (tagBefore, tagAfter) = if (tagText != null) symbolsOf(badge, BadgePart.TAG, keepWidth) else NO_SYMBOLS
        val tagChars = tagText?.let { "$tagBefore[$it]$tagAfter " }.orEmpty()
        fun moved(range: IntRange) = if (range.first >= insertAt) range.first + tagChars.length..range.last + tagChars.length else range
        val name = moved(foundName)
        levelRange = levelRange?.let(::moved)
        rankRange = rankRange?.let(::moved)
        // Without the space behind it, so the gap check below sees it
        val tagRange = tagText?.let { insertAt..<insertAt + tagChars.length - 1 }
        val tagColor = tag?.color ?: tagInfo?.let { PartColor(it.color, it.to) }

        val segments = listOfNotNull(
            levelColor?.let { color -> levelRange?.let { Segment(BadgePart.LEVEL, color, it) } },
            rankColor?.let { color -> rankRange?.let { Segment(BadgePart.RANK, color, it) } },
            nameColor?.takeUnless { foreign }?.let { Segment(BadgePart.NAME, it, name) },
            tagColor?.let { color -> tagRange?.let { Segment(BadgePart.TAG, color, it) } },
        ).sortedBy { it.range.first }
        if (afterName == null && segments.isEmpty()) return line
        // Only spaces may stand between chained parts
        val paints = paints(segments) { before, next ->
            (before.range.last + 1..<next.range.first).all { i ->
                if (i in insertAt..<insertAt + tagChars.length) tagChars[i - insertAt] == ' '
                else read[if (i < insertAt) i else i - tagChars.length].char == ' '
            }
        }

        val now = System.currentTimeMillis()

        // Brackets and the rank's plus can have their own color; the tag's brackets are gray without one,
        // unless the tag flows in a line with the parts next to it
        fun colorOf(k: Int, i: Int, char: Char): Int {
            val segment = segments[k]
            val color = segment.color
            // By the char, symbols can stand outside the brackets
            val tagBracket = segment.part == BadgePart.TAG && color.chain != true && (char == '[' || char == ']')
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
        var segmentIndex = 0
        val outputLength = read.size + tagChars.length
        for (i in 0..<outputLength) {
            val inTag = i in insertAt..<insertAt + tagChars.length
            val sourceIndex = if (inTag) -1 else if (i < insertAt) i else i - tagChars.length
            val char = if (inTag) tagChars[i - insertAt] else read[sourceIndex].char
            val baseStyle = if (inTag) Style.EMPTY else read[sourceIndex].style
            while (segmentIndex < segments.size && i > segments[segmentIndex].range.last) segmentIndex++
            val k = segmentIndex.takeIf { it < segments.size && i in segments[it].range } ?: -1
            val painted = if (k < 0) baseStyle else baseStyle.withColor(colorOf(k, i, char))
            val colored = if (k >= 0 && styles) segments[k].color.styled(painted, keepWidth) else painted
            val style = if (k < 0 || !moveChars) colored else paints[k].moved(colored, i, char, animate, now)
            if (style != runStyle) {
                flush()
                runStyle = style
            }
            run.append(char)
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

    private fun colored(text: String, color: PartColor, part: String, animate: Boolean, styled: Boolean = false): MutableComponent {
        fun look(style: Style) = if (styled) color.styled(style) else style
        if (color.to == null && color.effectOf(part) == BadgeEffect.STATIC && (!animate || color.motion.isStill())) {
            return Component.literal(text).withStyle { look(it.withColor(parseHex(color.color))) }
        }
        val now = System.currentTimeMillis()
        val out = Component.empty()
        text.forEachIndexed { i, char ->
            out.append(Component.literal(char.toString()).withStyle {
                moved(look(it.withColor(colorAt(color, part, i, text.length, animate, now))), color.motion, i, text.length, char, animate, now)
            })
        }
        return out
    }

    /** A colored part of a line and its chars. */
    class Segment(val part: String, val color: PartColor, val range: IntRange)

    /** The colors and motion of a part, counted over the chars of its whole chain. */
    class Paint(val color: PartColor, val part: String, private val motion: String?, val range: IntRange, private val colorRange: IntRange) {
        private val length get() = range.last - range.first + 1
        fun at(i: Int, animate: Boolean, now: Long): Int =
            colorAt(color, part, i - colorRange.first, colorRange.last - colorRange.first + 1, animate, now)
        fun moved(style: Style, i: Int, char: Char, animate: Boolean, now: Long): Style =
            moved(style, motion, i - range.first, length, char, animate, now)
        fun dropAt(i: Int, now: Long): Int = dropAt(motion, i - range.first, length, now)
        fun glitchAt(i: Int, now: Long): Boolean = glitchAt(motion, i - range.first, length, now)
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

    /**
     * How many pixels char [i] sits lower, 0 to [SHIFT_FONTS] size. Fonts only move whole pixels and only down,
     * so every motion uses all steps and smooth curves to look as even as it can.
     */
    fun dropAt(motion: String?, i: Int, length: Int, now: Long): Int = when (motion) {
        BadgeMotion.WAVY -> depth(swing(phase(now, WAVY_MS) - i * WAVY_STEP))
        BadgeMotion.BOUNCE -> {
            // One hop runs over the text, then a short rest
            val hop = phase(now, BOUNCE_MS) * (length + BOUNCE_REST) - i
            if (hop in 0f..1f) depth(kotlin.math.sin(hop * PI.toFloat())) else 0
        }
        BadgeMotion.SHAKE -> if (noise(i, now / SHAKE_MS) % 3 == 0) 1 else 0
        else -> 0
    }

    /** 0 to 1 and back over one turn of [turns], eased at both ends. */
    private fun swing(turns: Float): Float = (1f - cos(turns * 2f * PI.toFloat())) / 2f

    /** Pixels for a depth from 0 to 1. */
    private fun depth(t: Float): Int = kotlin.math.round(t.coerceIn(0f, 1f) * SHIFT_FONTS.size).toInt()

    /** Whether char [i] shows scrambled right now. */
    fun glitchAt(motion: String?, i: Int, length: Int, now: Long): Boolean =
        motion == BadgeMotion.GLITCH && noise(i, now / GLITCH_MS) % (length * GLITCH_SPREAD).coerceAtLeast(GLITCH_SPREAD) == 0

    /** Same number for the same char and time slot, so a frame drawn twice looks the same. */
    private fun noise(i: Int, slot: Long): Int {
        var h = slot * 0x9E3779B97F4A7C15uL.toLong() + i * 0xBF58476D1CE4E5B9uL.toLong()
        h = (h xor (h ushr 31)) * 0x94D049BB133111EBuL.toLong()
        return (h xor (h ushr 29) and 0x7FFFFFFF).toInt()
    }
    private fun phase(now: Long, periodMs: Long): Float = now % periodMs / periodMs.toFloat()

    /** Full, bright color of a hue from 0 to 1. */
    private fun hue(h: Float): Int {
        val x = h * 6f
        fun channel(offset: Float): Int {
            val k = (offset + x) % 6f
            return ((1f - maxOf(0f, minOf(k, 4f - k, 1f))) * 255).toInt()
        }
        return channel(5f) shl 16 or (channel(3f) shl 8) or channel(1f)
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = ((a shr shift and 0xFF) * (1 - t) + (b shr shift and 0xFF) * t).toInt()
        return channel(16) shl 16 or (channel(8) shl 8) or channel(0)
    }

    fun parseHex(hex: String): Int = hex.removePrefix("#").toIntOrNull(16) ?: 0xFFFFFF

    private fun uuidOf(raw: String): UUID? = runCatching {
        val s = raw.replace("-", "")
        UUID(s.take(16).toULong(16).toLong(), s.substring(16, 32).toULong(16).toLong())
    }.getOrNull()
}
