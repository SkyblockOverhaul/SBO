package net.sbo.mod.customizer

import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.customizer.CustomizerEffects.changesAt
import net.sbo.mod.customizer.CustomizerEffects.movesAt
import net.sbo.mod.customizer.CustomizerRecolor.colored
import net.sbo.mod.customizer.CustomizerRecolor.recolor
import net.sbo.mod.customizer.CustomizerRecolor.recolored
import net.sbo.mod.customizer.CustomizerRecolor.stylesShown
import net.sbo.mod.customizer.CustomizerRecolor.symbolsOf
import net.sbo.mod.customizer.CustomizerRecolor.tagOf
import net.sbo.mod.customizer.gui.CustomizerGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.game.WorldChangeEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.SboApi
import java.util.UUID
import java.util.function.Supplier

/**
 * SBO customizers and team tags: the list from the backend, the line above heads, sizes and the entry points the mixins call.
 * Tab list, chat, recoloring and effects live in [CustomizerTab], [CustomizerChat], [CustomizerRecolor] and [CustomizerEffects].
 */
object SboCustomizer {
    const val COMMAND = "sbocustomizer"

    /**
     * The viewer's switch for other supporters' and contributors' customizers in the tab list, [showInChat] the same for chat.
     * The team (moderators and above) and your own customizer always show, above heads always shows.
     */
    internal val showInTab: Boolean get() = DataManager.sboData.customizerInTab
    internal val showInChat: Boolean get() = DataManager.sboData.customizerInChat

    /** Whose names inside a chat message (not only the sender's) get their customizer look: nobody's, only your own ([NAMES_OWN]) or everyone's ([NAMES_ALL]). */
    const val NAMES_OFF = "off"
    const val NAMES_OWN = "own"
    const val NAMES_ALL = "all"

    private const val LIST_REFRESH_MINUTES = 10

    /** The SBO logo: this char in the sbo:customizer font. */
    const val LOGO = ""
    private val LOGO_FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath("sbo", "customizer"))
    /** White, a colored logo would be tinted. */
    private val LOGO_STYLE = Style.EMPTY.withFont(LOGO_FONT).withColor(0xFFFFFF)

    @Volatile internal var byUuid: Map<UUID, PlayerCustomizer> = emptyMap()
        private set
    @Volatile internal var byName: Map<String, PlayerCustomizer> = emptyMap()
        private set
    /** Head lines without moving colors, built once per list. */
    @Volatile private var fixedLines: Map<UUID, Component> = emptyMap()
    @Volatile internal var team: Map<UUID, TeamMember> = emptyMap()
        private set
    @Volatile private var teamNames: Set<String> = emptySet()
    @Volatile internal var ranks: Map<String, CustomizerRank> = emptyMap()
        private set
    /** Team tags above heads by uuid; [tabTags] and [chatTags] go by lowercase name since those lines only carry the name. */
    @Volatile private var tags: Map<UUID, TagLook> = emptyMap()
    @Volatile internal var tabTags: Map<String, TagLook> = emptyMap()
        private set
    @Volatile internal var chatTags: Map<String, TagLook> = emptyMap()
        private set
    /** Lowercase names whose name tag has moving colors, built again every frame instead of cached; [movingInTab] and [movingEverywhere] the same. */
    @Volatile internal var movingAtHead: Set<String> = emptySet()
        private set
    @Volatile internal var movingInTab: Set<String> = emptySet()
        private set
    @Volatile internal var movingEverywhere: Set<String> = emptySet()
        private set
    @Volatile private var lastList: CustomizerListResponse? = null
    @Volatile internal var version: String? = null
        private set
    @Volatile private var fetching = false

    @Volatile private var ownRefreshed = false

    fun init() {
        Register.onTick(20 * 60 * LIST_REFRESH_MINUTES) { _ ->
            if (mc.level != null && World.isInSkyblock()) fetch()
        }
        Register.command(COMMAND) {
            mc.schedule { CustomizerGui.open() }
        }
    }

    @SboEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (version == null) fetch()
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        CustomizerTab.clearCache()
    }

    fun fetch() {
        if (fetching) return
        fetching = true
        SboApi.customizers(version)
            .toJson<CustomizerListResponse>(ignoreUnknownKeys = true) { response ->
                fetching = false
                if (!response.success) return@toJson
                if (!response.unchanged) apply(response)
                refreshOwn()
            }
            .error {
                fetching = false
                SBOKotlin.logger.debug("[SboCustomizer] list failed: ${it.message}")
            }
    }

    /**
     * Takes a new list. A team tag takes the member's tag color, the rank's while their customizer is off.
     * Names recolored everywhere move like names inside chat messages, with their rank always colored.
     */
    private fun apply(response: CustomizerListResponse) {
        val customizers = response.customizers.map { customizer -> if (uuidOf(customizer.uuid)?.let(::movesChars) != false) customizer else customizer.still() }
        val holders = customizers.mapNotNull { customizer -> uuidOf(customizer.uuid)?.let { it to customizer } }.toMap()
        val members = response.team.mapNotNull { member -> uuidOf(member.uuid)?.let { it to member } }.toMap()
        ranks = response.ranks
        team = members
        teamNames = members.values.mapTo(HashSet()) { it.name.lowercase() }
        val tagged = members.asSequence().filter { ranks[it.value.rank]?.tag != null }.map { (uuid, member) ->
            val color = holders[uuid]?.colorOf(CustomizerPart.TAG)
            Triple(uuid, member, TagLook(member.rank, color, animates(uuid)))
        }.toList()
        fun byName(place: String) = tagged.asSequence().filter { place in it.second.tagPlaces }.associate { (_, member, tag) -> member.name.lowercase() to tag }
        tags = tagged.asSequence().filter { TeamTagPlace.HEAD in it.second.tagPlaces }.associate { (uuid, _, tag) -> uuid to tag }
        tabTags = byName(TeamTagPlace.TAB)
        chatTags = byName(TeamTagPlace.CHAT)
        fun movingAt(place: String, tagsAt: Map<String, TagLook>) = holders.filter { (uuid, customizer) ->
            listOf(CustomizerPart.NAME, CustomizerPart.LEVEL, CustomizerPart.RANK).any { customizer.shows(it, place) && changesAt(customizer.colorOf(it), it, place, animates(uuid)) }
        }.mapTo(HashSet()) { it.value.name.lowercase() } +
            tagsAt.filterValues { tag -> tag.animated && tag.color?.let { movesAt(it, CustomizerPart.TAG, place) } == true }.keys
        movingAtHead = movingAt(TeamTagPlace.HEAD, byName(TeamTagPlace.HEAD))
        movingInTab = if (DataManager.sboData.customizerAnimationsInTab) movingAt(TeamTagPlace.TAB, tabTags) else emptySet()
        movingEverywhere = holders.filter { (uuid, customizer) ->
            animates(uuid) && listOf(CustomizerPart.NAME, CustomizerPart.LEVEL, CustomizerPart.RANK).any {
                (it == CustomizerPart.RANK || customizer.shows(it, TeamTagPlace.CHAT)) && movesAt(customizer.colorOf(it), it, TeamTagPlace.CHAT)
            }
        }.mapTo(HashSet()) { it.value.name.lowercase() }
        fixedLines = holders.filter { (uuid, customizer) -> !changesAt(customizer.colorOf(CustomizerPart.STAT), CustomizerPart.STAT, TeamTagPlace.HEAD, animates(uuid)) }
            .mapValues { line(it.value, animate = false) }
        byUuid = holders
        byName = customizers.associateBy { it.name.lowercase() }
        version = response.version
        lastList = response
        CustomizerTab.clearCache()
        CustomizerRecolor.clearCache()
        CustomizerChat.clearCache()
    }

    /** Builds the customizers again after the viewer turned animations of others on or off. */
    fun relook() {
        lastList?.let { mc.execute { apply(it) } }
    }

    /** Your own customizer always moves, other players' only while the viewer allows it. */
    internal fun animates(uuid: UUID): Boolean = DataManager.sboData.customizerAnimationsOfOthers || uuid == uuidOf(Player.accountUuid())

    /** Your own customizer's chars always move, other players' only while the viewer allows it; their colors still move. */
    private fun movesChars(uuid: UUID): Boolean = DataManager.sboData.customizerMotionOfOthers || uuid == uuidOf(Player.accountUuid())

    /** The customizer with its chars standing still. */
    private fun PlayerCustomizer.still(): PlayerCustomizer {
        val parts = colors ?: return this
        fun PartColor?.still() = this?.copy(motion = null)
        return copy(colors = CustomizerColors(parts.stat.still(), parts.name.still(), parts.level.still(), parts.tag.still(), parts.rank.still()))
    }

    /** [customizer] with its chars standing still, for the customizer's sample supporter while Moving letters is off. */
    fun withoutMotion(customizer: PlayerCustomizer): PlayerCustomizer = customizer.still()

    private fun refreshOwn() {
        if (ownRefreshed || own() == null) return
        ownRefreshed = true
        SboApi.ownCustomizer()
            .toJsonObject { if (it.getBoolean("Success")) fetch() }
            .error { SBOKotlin.logger.debug("[SboCustomizer] own customizer failed: ${it.message}") }
    }

    fun hasAccess(): Boolean? = if (own() != null) true else DataManager.sboData.customizerAccess[Player.accountUuid()]

    fun rememberAccess(access: Boolean) {
        val account = Player.accountUuid()
        if (DataManager.sboData.customizerAccess[account] == access) return
        DataManager.sboData.customizerAccess[account] = access
        DataManager.sboData.save()
    }

    fun own(): PlayerCustomizer? = uuidOf(Player.accountUuid())?.let { byUuid[it] }

    internal fun isOwn(customizer: PlayerCustomizer): Boolean = customizer.uuid.replace("-", "").equals(Player.accountUuid().replace("-", ""), ignoreCase = true)

    /**
     * Model size of a player, 1 without a customizer size or while the viewer turned sizes of others off.
     * Kept within what the backend sends, so a broken value never makes a giant.
     */
    fun sizeOf(uuid: UUID): Float {
        if (byUuid.isEmpty()) return 1f
        val customizer = byUuid[uuid] ?: return 1f
        val size = customizer.look?.size ?: return 1f
        if (!DataManager.sboData.customizerSizesOfOthers && !isOwn(customizer)) return 1f
        return size.coerceIn(MIN_SIZE, MAX_SIZE)
    }

    /** Set while a menu (the inventory) builds a player model: no customizer, name tag or size there. Render thread only. */
    var menuModel = false

    private const val MIN_SIZE = 0.1f
    private const val MAX_SIZE = 1f

    /** Text size of the customizer line above heads, the viewer's own choice for every customizer. */
    fun lineSize(): Float = textSize(DataManager.sboData.customizerLineSize)

    /** Text size of the name line above heads, the viewer's own choice. */
    fun nameSize(): Float = textSize(DataManager.sboData.customizerNameSize)

    /** Name size for a player: only SBO customizer holders and the team (the customizer's players) get it, everyone else stays 1. */
    fun nameSizeOf(uuid: UUID): Float = if (uuid in byUuid || uuid in team) nameSize() else 1f

    private fun textSize(size: Float): Float = size.takeIf { it.isFinite() }?.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE) ?: 1f

    const val MIN_TEXT_SIZE = 0.2f
    const val MAX_TEXT_SIZE = 1.2f
    const val TEXT_SIZE_STEP = 0.05f

    /** The viewer's switch for a place hides other supporters and contributors, never the team or your own customizer. */
    internal fun shown(enabled: Boolean, player: String) =
        enabled || player.lowercase() in teamNames || player.equals(own()?.name, ignoreCase = true)

    fun nameTagLine(uuid: UUID): Component? = safe(null) { nameTagLineOf(uuid) }

    private fun nameTagLineOf(uuid: UUID): Component? {
        val customizer = byUuid[uuid]?.takeIf(::hasHeadLine) ?: return null
        return fixedLines[uuid] ?: line(customizer, animate = animates(uuid), moveChars = true)
    }

    fun colorNameTag(uuid: UUID, nameTag: Component): Component = safe(nameTag) { colorNameTagOf(uuid, nameTag) }

    /** A name tag without the player's name (another mod changed it) still gets the tag in front. */
    private fun colorNameTagOf(uuid: UUID, nameTag: Component): Component {
        val customizer = byUuid[uuid]
        val tag = tags[uuid]
        if (customizer == null && tag == null) return nameTag
        val player = customizer?.name ?: team[uuid]?.name ?: return nameTag
        val colored = recolored(nameTag, player, TeamTagPlace.HEAD, tag)
        if (colored !== nameTag || tag == null) return colored
        return tagOf(tag.rank, tag.color)?.let { Component.empty().append(it).append(nameTag) } ?: nameTag
    }

    /** Tag color of a player (uuid without dashes) for the party finder, null while their customizer is off. */
    fun customizerColor(uuid: String): String? = uuidOf(uuid)?.let { byUuid[it]?.colorOf(CustomizerPart.TAG)?.color }

    /** Team rank of a player (uuid without dashes), for the party finder. */
    fun teamRank(uuid: String): CustomizerRank? = uuidOf(uuid)?.let { team[it] }?.let { ranks[it.rank] }?.takeIf { it.tag != null }

    /** Whether a player (uuid without dashes) supports SBO and has their customizer on, for the party finder. */
    fun isSupporter(uuid: String): Boolean = uuidOf(uuid)?.let { byUuid[it] }?.rank == "supporter"

    /** Your own name tag shows in third person when you have a customizer or a team tag. */
    fun showOwnNameTag(): Boolean {
        if (!DataManager.sboData.customizerOwnInF5) return false
        val uuid = uuidOf(Player.accountUuid()) ?: return false
        return uuid in byUuid || uuid in tags
    }

    /**
     * A line as [customizer] makes it at [place], for the customizer's preview: [base] is the line as Hypixel sends it,
     * [tagRank] the team rank whose tag shows there (null for none). Built by the same code as in game.
     */
    fun previewLine(
        customizer: PlayerCustomizer, base: Component, place: String, tagRank: String?, animate: Boolean,
        moveChars: Boolean = animate && place != TeamTagPlace.TAB,
    ): Component = safe(base) {
        val tag = tagRank?.let { TagLook(it, customizer.colorOf(CustomizerPart.TAG), animated = true) }
        val logoAfterName = place == TeamTagPlace.CHAT && customizer.shows(CustomizerPart.LOGO, place)
        val line = recolor(
            base, customizer.name, customizer, place, tag,
            afterName = if (logoAfterName) Component.literal(" ").append(logo()) else null,
            animate = animate,
            moveChars = moveChars,
        )
        if (place == TeamTagPlace.TAB && customizer.shows(CustomizerPart.LOGO, place)) line.copy().append(" ").append(logo()) else line
    }

    /**
     * A name inside a message (or [everywhere]: menus, scoreboard) as the customizer's preview of other players shows it:
     * [name] from char [from] on. [everywhere] keeps the width and colors the rank in front too, like [replaceEverywhere].
     * Built by the same code as in game.
     */
    fun previewMention(
        customizer: PlayerCustomizer, base: Component, name: String, from: Int, animate: Boolean,
        moveChars: Boolean = animate, everywhere: Boolean = false,
    ): Component =
        safe(base) {
            recolor(
                base, name, customizer, TeamTagPlace.CHAT,
                from = from, nameOnly = !everywhere, forceRank = everywhere, keepWidth = everywhere,
                animate = animate, moveChars = moveChars && !everywhere,
            )
        }

    /** Whether the customizer has a line above the head: the logo, the stat or both. */
    fun hasHeadLine(customizer: PlayerCustomizer): Boolean =
        customizer.shows(CustomizerPart.LOGO, TeamTagPlace.HEAD) || customizer.shows(CustomizerPart.STAT, TeamTagPlace.HEAD) && customizer.label != null && customizer.value != null

    /** The line above the head: logo and stat where the customizer shows them. The logo is its own child, the text after it would take over its font. */
    fun line(customizer: PlayerCustomizer, animate: Boolean, moveChars: Boolean = animate): MutableComponent {
        val line = Component.empty()
        val withLogo = customizer.shows(CustomizerPart.LOGO, TeamTagPlace.HEAD)
        if (withLogo) line.append(logo())
        if (customizer.shows(CustomizerPart.STAT, TeamTagPlace.HEAD) && customizer.label != null && customizer.value != null) {
            if (withLogo) line.append(" ")
            val (before, after) = symbolsOf(customizer, CustomizerPart.STAT)
            line.append(colored("$before${customizer.label}: ${customizer.value}$after", customizer.colorOf(CustomizerPart.STAT), CustomizerPart.STAT, animate, stylesShown(customizer), moveChars))
        }
        return line
    }

    fun decorateTab(info: PlayerInfo, line: Component): Component = CustomizerTab.decorateTab(info, line)
    fun <T> undecoratedTab(read: Supplier<T>): T = CustomizerTab.undecoratedTab(read)
    fun decorateSbaTab(line: String): Component? = CustomizerTab.decorateSbaTab(line)
    fun decorateSkyHanniTab(line: Component?): Component? = CustomizerTab.decorateSkyHanniTab(line)
    fun decorateChat(line: Component): Component = CustomizerChat.decorateChat(line)
    fun splitChatLines(content: Component, width: Int, lines: List<FormattedCharSequence>) = CustomizerChat.splitChatLines(content, width, lines)
    fun animatedChatLine(text: FormattedCharSequence?): FormattedCharSequence? = CustomizerChat.animatedChatLine(text)
    fun replaceEverywhere(text: FormattedCharSequence?): FormattedCharSequence? = CustomizerChat.replaceEverywhere(text)

    @Volatile internal var loggedError = false

    /** [block], or [fallback] when it throws: this runs inside the game's and other mods' rendering and chat, a bug must never cost the line or the frame. */
    internal inline fun <T> safe(fallback: T, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        if (!loggedError) {
            loggedError = true
            SBOKotlin.logger.error("[SboCustomizer] decorating failed, showing the line without customizer", e)
        }
        fallback
    }

    fun logo(): MutableComponent = Component.literal(LOGO).withStyle(LOGO_STYLE)

    internal fun uuidOf(raw: String): UUID? = runCatching {
        val s = raw.replace("-", "")
        UUID(s.take(16).toULong(16).toLong(), s.substring(16, 32).toULong(16).toLong())
    }.getOrNull()
}
