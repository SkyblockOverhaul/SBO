package net.sbo.mod.guis.customizer

import net.minecraft.network.chat.Component
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.entity
import net.sbo.guilib.fabric.entity.FakePlayer
import net.sbo.mod.general.SboBadges
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeColors
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.BadgePlaces
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.data.TeamTagPlace
import java.util.Locale
import kotlin.math.roundToInt

// The preview's rank, the own Hypixel rank is not known
internal const val SAMPLE_RANK = "§b[MVP§c+§b] "
internal const val FRAME_MS = 20L
// Height of the model at the largest size; smaller sizes shrink from there, so the size shows as it is
internal const val MODEL_HEIGHT = 150f
internal const val MODEL_MAX_SIZE = 1.75f

internal data class StageProps(val badge: OwnBadgeResponse?, val settings: BadgeSettings?, val textSizes: TextSizes)

// Its own component, so moving colors only draw the stage again
internal val Stage = component<StageProps>("CustomizerStage") { (badge, settings, textSizes) ->
    val model = useMemo { FakePlayer.ofLocalPlayer() }
    // A new frame number draws the stage again; the colors read the time themselves
    val frame = useState(0L)
    val moving = settings != null && ALL_PARTS.any { SboBadges.moves(settings.colorOf(it), it) }
    useInterval(FRAME_MS) { if (moving) frame.value = System.currentTimeMillis() / FRAME_MS }

    val preview = if (badge != null && settings != null) previewOf(badge, settings) else null
    val name = Player.accountName()
    val size = settings?.look?.scale ?: 1f
    val prefix = badge?.level?.let { "§8[§b$it§8] " }.orEmpty() + SAMPLE_RANK
    fun base(after: String) = legacyText("$prefix§b$name$after")
    fun shown(place: String, after: String = ""): Component =
        preview?.let { SboBadges.previewLine(it.badge, base(after), place, it.tagAt(place), it.animates(place)) } ?: base(after)

    div(className = "cz-stage") {
        div(className = "cz-scene") {
            div(className = "cz-figure") {
                // The text sizes from Supporters, the name size only for players with a badge as in game
                if (preview != null && SboBadges.hasHeadLine(preview.badge)) {
                    div(className = "cz-nametag", style = textSize(textSizes.tag)) { game(SboBadges.line(preview.badge, animate = true)) }
                }
                div(className = "cz-nametag", style = textSize(if (preview != null) textSizes.name else 1f)) { game(shown(TeamTagPlace.HEAD)) }
                if (model != null) {
                    entity(
                        model,
                        className = "cz-model",
                        style = "height: ${(MODEL_HEIGHT * size / MODEL_MAX_SIZE).roundToInt().coerceAtLeast(4)}px",
                        followMouse = true,
                    )
                }
            }
            div(className = "cz-ground")
        }
        div(className = "cz-lines") {
            div(className = "cz-line cz-line-tab") { game(shown(TeamTagPlace.TAB)) }
            div(className = "cz-line cz-line-chat") { game(shown(TeamTagPlace.CHAT, "§f: Hello!")) }
        }
        div(className = "cz-hint cz-status") {
            +when {
                badge == null -> ""
                badge.hiddenByServer -> "Hidden by the SBO server, nobody sees it right now."
                settings?.enabled == false -> "Nothing shows anywhere, nobody sees your badge."
                size != 1f -> "Size %.2fx".format(Locale.ROOT, size)
                else -> "This is how others with SBO see you."
            }
        }
    }
}

/** The draft as a badge from the list, so the stage is built like in game. */
private class Preview(val badge: SboBadge, private val response: OwnBadgeResponse, private val settings: BadgeSettings) {
    fun tagAt(place: String): String? = response.rank?.takeIf { hasTag(response) && place in settings.teamTagPlaces }

    // Your own badge always moves above heads and in the tab list; in chat while chat animations are on
    fun animates(place: String): Boolean = place != TeamTagPlace.CHAT || DataManager.sboData.badgeAnimationsInChat != SboBadges.NAMES_OFF
}

private fun previewOf(response: OwnBadgeResponse, settings: BadgeSettings): Preview? {
    // Others with SBO only get the badge while it shows somewhere and the server does not hide it
    if (!settings.enabled || response.hiddenByServer) return null
    val stat = response.stats.firstOrNull { it.id == settings.stat }?.takeIf { it.id != "none" }
    val value = response.values[settings.stat] ?: response.preview?.value?.takeIf { settings.stat == response.settings?.stat }
    fun places(part: String) = TeamTagPlace.ALL.filter { it in settings.placesOf(part) }
    val badge = SboBadge(
        uuid = Player.accountUuid(),
        name = Player.accountName(),
        rank = response.rank ?: "supporter",
        color = settings.color,
        label = stat?.label,
        value = if (stat != null) value ?: "..." else null,
        places = BadgePlaces(
            places(BadgePart.LOGO), places(BadgePart.NAME), places(BadgePart.LEVEL), places(BadgePart.RANK), stat = places(BadgePart.STAT),
        ),
        colors = BadgeColors(
            settings.colorOf(BadgePart.STAT), settings.colorOf(BadgePart.NAME), settings.colorOf(BadgePart.LEVEL),
            settings.colorOf(BadgePart.TAG), settings.colorOf(BadgePart.RANK),
        ),
        look = settings.look,
    )
    return Preview(badge, response, settings)
}
