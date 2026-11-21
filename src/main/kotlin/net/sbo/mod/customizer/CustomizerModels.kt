package net.sbo.mod.customizer

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Style

@Serializable
data class PlayerCustomizer(
    val uuid: String,
    val name: String,
    val rank: String,
    val color: String,
    val to: String? = null,
    val level: Boolean? = null,
    private val plainName: Boolean? = null,
    val label: String? = null,
    val value: String? = null,
    /** Places and colors per part; backends before it only send the fields above. */
    @SerialName("in")
    val places: CustomizerPlaces? = null,
    val colors: CustomizerColors? = null,
    /** Size and symbols; null is the normal look. */
    val look: CustomizerLook? = null,
) {
    /** Whether [part] shows at [place]; before the stat had places of its own it showed with the logo above the head. */
    fun shows(part: String, place: String): Boolean = places?.let { place in it[part] } ?: when (part) {
        CustomizerPart.LOGO -> true
        CustomizerPart.STAT -> place == TeamTagPlace.HEAD
        CustomizerPart.NAME -> plainName != true
        CustomizerPart.LEVEL -> level == true && place in TeamTagPlace.LEVEL
        else -> false
    }

    fun colorOf(part: String): PartColor = colors?.get(part) ?: PartColor(color, to)
}

/**
 * Parts of a customizer: logo (with the stat above the head), name, level, Hypixel's rank and team tag show per place;
 * stat, name, level, tag and rank have a color.
 */
object CustomizerPart {
    const val LOGO = "logo"
    const val STAT = "stat"
    const val NAME = "name"
    const val LEVEL = "level"
    const val TAG = "tag"
    /** Hypixel's rank in front of the name, e.g. [MVP+]. */
    const val RANK = "rank"
    /** Parts whose brackets can take their own color. */
    val BRACKETS = setOf(LEVEL, TAG, RANK)
    /** Parts of the name line that can flow on from the part in front of them; the stat is a line of its own. */
    val CHAINS = setOf(LEVEL, RANK, TAG, NAME)
}

@Serializable
data class PartColor(
    val color: String,
    val to: String? = null,
    /** Colors of the opening [ and the closing ]; null keeps the old look: the part's color, gray for the team tag. */
    val open: String? = null,
    val close: String? = null,
    /** Color of the rank's plus, e.g. the + of [MVP+]; null gives it the part's color. */
    val plus: String? = null,
    /** A CustomizerEffect; null keeps the old look, see [effectOf]. */
    val effect: String? = null,
    /** Level, rank, tag and name: flows on from the part right in front, so their effects run on as one (Name Line). */
    val chain: Boolean? = null,
    /** A CustomizerMotion of the chars; null stands still. */
    val motion: String? = null,
    /** Text styles of the part; null is plain. */
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strike: Boolean? = null,
    /** Symbols in front of and behind the part; null is none. */
    val before: String? = null,
    val after: String? = null,
) {
    /** The part's text styles on top of [style]; [keepWidth] leaves out bold, which draws wider. */
    fun styled(style: Style, keepWidth: Boolean = false): Style {
        var out = style
        if (bold == true && !keepWidth) out = out.withBold(true)
        if (italic == true) out = out.withItalic(true)
        if (underline == true) out = out.withUnderlined(true)
        if (strike == true) out = out.withStrikethrough(true)
        return out
    }

    val hasStyle: Boolean get() = bold == true || italic == true || underline == true || strike == true

    /** Customizers from before effects: a gradient stat waves, everything else stands still. */
    fun effectOf(part: String): String = effect
        ?: if (part == CustomizerPart.STAT && to != null) CustomizerEffect.WAVE else CustomizerEffect.STATIC
}

/** How a part's colors move. */
object CustomizerEffect {
    const val STATIC = "static"
    /** The gradient flows along the text, needs a second color. */
    const val WAVE = "wave"
    /** The whole text fades between both colors, or towards white with one color. */
    const val PULSE = "pulse"
    /** A rainbow running along the text, the colors are not used. */
    const val CHROMA = "chroma"
    /** A white gleam sweeps over the text now and then. */
    const val SHINE = "shine"
    /** Jumps between the two colors, or to a light version of one color. */
    const val FLASH = "flash"
    /** Single chars twinkle white. */
    const val SPARKLE = "sparkle"
    val ALL = listOf(STATIC, WAVE, PULSE, CHROMA, SHINE, FLASH, SPARKLE)
}

/** How a part's chars move, on top of its colors. Chars only move down, Minecraft fonts cannot rise above the line. */
object CustomizerMotion {
    const val NONE = "none"
    /** The chars ride a wave along the text. */
    const val WAVY = "wavy"
    /** One char after the other hops. */
    const val BOUNCE = "bounce"
    /** The chars jitter. */
    const val SHAKE = "shake"
    /** Single chars flicker as scrambled ones, like §k. */
    const val GLITCH = "glitch"
    val ALL = listOf(NONE, WAVY, BOUNCE, SHAKE, GLITCH)
}

/** Always sent: a part left out keeps its saved places, so an empty list must reach the backend. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CustomizerPlaces(
    @EncodeDefault val logo: List<String> = emptyList(),
    @EncodeDefault val name: List<String> = emptyList(),
    @EncodeDefault val level: List<String> = emptyList(),
    @EncodeDefault val rank: List<String> = emptyList(),
    /** Only above the head; null on backends before it, then the stat shows with the logo above the head. */
    val stat: List<String>? = null,
) {
    operator fun get(part: String): List<String> = when (part) {
        CustomizerPart.LOGO -> logo
        CustomizerPart.STAT -> stat ?: listOfNotNull(TeamTagPlace.HEAD.takeIf { it in logo })
        CustomizerPart.NAME -> name
        CustomizerPart.LEVEL -> level
        CustomizerPart.RANK -> rank
        else -> emptyList()
    }

    fun with(part: String, places: List<String>): CustomizerPlaces = when (part) {
        CustomizerPart.LOGO -> copy(logo = places)
        CustomizerPart.STAT -> copy(stat = places)
        CustomizerPart.NAME -> copy(name = places)
        CustomizerPart.LEVEL -> copy(level = places)
        CustomizerPart.RANK -> copy(rank = places)
        else -> this
    }
}

@Serializable
data class CustomizerColors(
    val stat: PartColor? = null,
    val name: PartColor? = null,
    val level: PartColor? = null,
    val tag: PartColor? = null,
    val rank: PartColor? = null,
) {
    operator fun get(part: String): PartColor? = when (part) {
        CustomizerPart.STAT -> stat
        CustomizerPart.NAME -> name
        CustomizerPart.LEVEL -> level
        CustomizerPart.TAG -> tag
        CustomizerPart.RANK -> rank
        else -> null
    }

    fun with(part: String, color: PartColor): CustomizerColors = when (part) {
        CustomizerPart.STAT -> copy(stat = color)
        CustomizerPart.NAME -> copy(name = color)
        CustomizerPart.LEVEL -> copy(level = color)
        CustomizerPart.TAG -> copy(tag = color)
        CustomizerPart.RANK -> copy(rank = color)
        else -> this
    }
}

@Serializable
data class CustomizerListResponse(
    @SerialName("Success")
    val success: Boolean = false,
    val version: String? = null,
    val unchanged: Boolean = false,
    /** Still named badges in the backend's JSON. */
    @SerialName("badges")
    val customizers: List<PlayerCustomizer> = emptyList(),
    /** Moderators and above, also with their customizer turned off; for the party finder and the name tag. */
    val team: List<TeamMember> = emptyList(),
    val ranks: Map<String, CustomizerRank> = emptyMap(),
)

/** How a rank looks, sent by the backend so it can change without a mod update. */
@Serializable
data class CustomizerRank(
    val label: String,
    /** Text of the team tag, null for ranks without one. */
    val tag: String? = null,
    val color: String = "#FFFFFF",
    val to: String? = null,
)

@Serializable
data class TeamMember(
    val uuid: String,
    val name: String,
    val rank: String,
    /** Shows the team tag somewhere; `tagIn` says where, backends before it only send this. */
    private val nameTag: Boolean = false,
    private val tagIn: List<String>? = null,
) {
    val tagPlaces: Set<String> get() = tagIn?.toSet() ?: if (nameTag) TeamTagPlace.ALL else emptySet()
}

/** Where a team tag shows in front of the name. */
object TeamTagPlace {
    const val HEAD = "head"
    const val TAB = "tab"
    const val CHAT = "chat"
    val ALL = setOf(HEAD, TAB, CHAT)
    /** Where the level is colored on customizers saved before the places per part: not in chat. */
    val LEVEL = setOf(HEAD, TAB)
}

/** Body of `POST /customizer` */
@Serializable
data class CustomizerSettings(
    val enabled: Boolean,
    val stat: String,
    val color: String,
    /** Second gradient color, only for ranks that may pick a gradient. */
    private val colorTo: String? = null,
    private val colorName: Boolean = true,
    private val colorLevel: Boolean = false,
    private val teamTag: Boolean = true,
    /** Places of the team tag (TeamTagPlace); null on backends without it, then `teamTag` means everywhere. */
    private val teamTagIn: List<String>? = null,
    /** Places and colors per part; null on backends without them, then the single fields above apply everywhere. */
    @SerialName("in")
    val places: CustomizerPlaces? = null,
    val colors: CustomizerColors? = null,
    /** Size and symbols; null on backends without it, then nothing is sent and the saved look stays. */
    val look: CustomizerLook? = null,
) {
    val teamTagPlaces: Set<String> get() = teamTagIn?.toSet() ?: if (teamTag) TeamTagPlace.ALL else emptySet()

    fun withTeamTagPlaces(places: Set<String>): CustomizerSettings =
        copy(teamTag = places.isNotEmpty(), teamTagIn = TeamTagPlace.ALL.filter { it in places })

    fun placesOf(part: String): Set<String> = places?.get(part)?.toSet() ?: when (part) {
        CustomizerPart.LOGO -> if (enabled) TeamTagPlace.ALL else emptySet()
        CustomizerPart.STAT -> if (enabled) setOf(TeamTagPlace.HEAD) else emptySet()
        CustomizerPart.NAME -> if (enabled && colorName) TeamTagPlace.ALL else emptySet()
        CustomizerPart.LEVEL -> if (enabled && colorLevel) TeamTagPlace.LEVEL else emptySet()
        else -> emptySet()
    }

    fun colorOf(part: String): PartColor = colors?.get(part) ?: PartColor(color, colorTo)

    fun withPlaces(part: String, places: Set<String>): CustomizerSettings =
        withStyle(styledPlaces().with(part, TeamTagPlace.ALL.filter { it in places }), styledColors())

    fun withColor(part: String, color: PartColor): CustomizerSettings = withStyle(styledPlaces(), styledColors().with(part, color))

    private fun styledPlaces() = CustomizerPlaces(
        TeamTagPlace.ALL.filter { it in placesOf(CustomizerPart.LOGO) },
        TeamTagPlace.ALL.filter { it in placesOf(CustomizerPart.NAME) },
        TeamTagPlace.ALL.filter { it in placesOf(CustomizerPart.LEVEL) },
        TeamTagPlace.ALL.filter { it in placesOf(CustomizerPart.RANK) },
        stat = TeamTagPlace.ALL.filter { it in placesOf(CustomizerPart.STAT) },
    )

    private fun styledColors() = CustomizerColors(
        colorOf(CustomizerPart.STAT), colorOf(CustomizerPart.NAME), colorOf(CustomizerPart.LEVEL), colorOf(CustomizerPart.TAG), colorOf(CustomizerPart.RANK),
    )

    /** The single fields follow the parts, for mods that only read those. */
    private fun withStyle(places: CustomizerPlaces, colors: CustomizerColors): CustomizerSettings {
        val name = colors.name ?: PartColor(color, colorTo)
        return copy(
            places = places,
            colors = colors,
            enabled = places.logo.isNotEmpty() || places.stat.orEmpty().isNotEmpty() || places.name.isNotEmpty() ||
                places.level.isNotEmpty() || places.rank.isNotEmpty(),
            colorName = places.name.isNotEmpty(),
            colorLevel = places.level.isNotEmpty(),
            color = name.color,
            colorTo = name.to,
        )
    }
}

@Serializable
data class CustomizerChoice(
    val id: String,
    val label: String,
    val hex: String? = null,
    val to: String? = null,
)

@Serializable
data class OwnCustomizerResponse(
    @SerialName("Success")
    val success: Boolean = false,
    @SerialName("Error")
    val error: String? = null,
    @SerialName("Code")
    val code: String? = null,
    val rank: String? = null,
    val rankInfo: CustomizerRank? = null,
    val settings: CustomizerSettings? = null,
    val preview: PlayerCustomizer? = null,
    val hiddenByServer: Boolean = false,
    val values: Map<String, String> = emptyMap(),
    val level: Int? = null,
    val stats: List<CustomizerChoice> = emptyList(),
    /** The rank may pick a gradient instead of one color. */
    val gradient: Boolean = false,
    /** The rank has a team tag it can show above the head. */
    val team: Boolean = false,
    /** Symbols that may stand next to the name; empty on backends without the look. */
    val symbols: List<String> = emptyList(),
    /** Range of the model size; null on backends without the look. */
    val sizes: CustomizerSizes? = null,
)

@Serializable
data class CustomizerSizes(val min: Float = 0.1f, val max: Float = 1.75f, val step: Float = 0.05f)

/** Model size; text styles and symbols are per part, see [PartColor]. `before`/`after` are the name's symbols on backends before that. */
@Serializable
data class CustomizerLook(
    /** Model size others with SBO see; null is 1. */
    val size: Float? = null,
    val before: String? = null,
    val after: String? = null,
) {
    val scale: Float get() = size ?: 1f
}
