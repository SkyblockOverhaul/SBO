package net.sbo.mod.customizer.gui

import net.minecraft.network.chat.Component
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.entity
import net.sbo.guilib.fabric.entity.FakePlayer
import net.sbo.mod.customizer.CustomizerEffects
import net.sbo.mod.customizer.SboCustomizer
import net.sbo.mod.utils.Player
import net.sbo.mod.customizer.CustomizerColors
import net.sbo.mod.customizer.CustomizerPart
import net.sbo.mod.customizer.CustomizerPlaces
import net.sbo.mod.customizer.CustomizerSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.customizer.OwnCustomizerResponse
import net.sbo.mod.customizer.PlayerCustomizer
import net.sbo.mod.customizer.TeamTagPlace
import java.util.Locale
import kotlin.math.roundToInt

/** The preview's rank, the own Hypixel rank is not known. */
internal const val SAMPLE_RANK = "§b[MVP§c+§b] "
internal const val FRAME_MS = 20L
/** Height of the model at the largest size; smaller sizes shrink from there, so the size shows as it is. */
internal const val MODEL_HEIGHT = 150f
internal const val MODEL_MAX_SIZE = 1.75f

internal data class StageProps(val customizer: OwnCustomizerResponse?, val settings: CustomizerSettings?, val textSizes: TextSizes)

/**
 * Its own component, so moving colors only draw the stage again. The text sizes come from Supporters, the name size
 * only for players with a customizer, as in game.
 */
internal val Stage = component<StageProps>("CustomizerStage") { (customizer, settings, textSizes) ->
    val model = useMemo { FakePlayer.ofLocalPlayer() }
    /** A new frame number draws the stage again; the colors read the time themselves. */
    val frame = useState(0L)
    val moving = settings != null && ALL_PARTS.any { CustomizerEffects.moves(settings.colorOf(it), it) }
    useInterval(FRAME_MS) { if (moving) frame.value = System.currentTimeMillis() / FRAME_MS }

    val preview = if (customizer != null && settings != null) previewOf(customizer, settings) else null
    val name = Player.accountName()
    val size = settings?.look?.scale ?: 1f
    val prefix = customizer?.level?.let { "§8[§b$it§8] " }.orEmpty() + SAMPLE_RANK
    fun base(after: String) = legacyText("$prefix§b$name$after")
    fun shown(place: String, after: String = ""): Component =
        preview?.let { SboCustomizer.previewLine(it.customizer, base(after), place, it.tagAt(place), it.animates(place)) } ?: base(after)

    div(className = "cz-stage") {
        div(className = "cz-scene") {
            div(className = "cz-figure") {
                if (preview != null && SboCustomizer.hasHeadLine(preview.customizer)) {
                    div(className = "cz-nametag", style = textSize(textSizes.tag)) { game(SboCustomizer.line(preview.customizer, animate = true)) }
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
                customizer == null -> ""
                customizer.hiddenByServer -> "Hidden by the SBO server, nobody sees it right now."
                settings?.enabled == false -> "Nothing shows anywhere, nobody sees your customizer."
                size != 1f -> "Size %.2fx".format(Locale.ROOT, size)
                else -> "This is how others with SBO see you."
            }
        }
    }
}

/** The draft as a customizer from the list, so the stage is built like in game. */
private class Preview(val customizer: PlayerCustomizer, private val response: OwnCustomizerResponse, private val settings: CustomizerSettings) {
    fun tagAt(place: String): String? = response.rank?.takeIf { hasTag(response) && place in settings.teamTagPlaces }

    /** Your own customizer always moves above heads and in the tab list; in chat while chat animations are on. */
    fun animates(place: String): Boolean = place != TeamTagPlace.CHAT || DataManager.sboData.customizerAnimationsInChat != SboCustomizer.NAMES_OFF
}

/** Null while others with SBO wouldn't get the customizer: it shows nowhere or the server hides it. */
private fun previewOf(response: OwnCustomizerResponse, settings: CustomizerSettings): Preview? {
    if (!settings.enabled || response.hiddenByServer) return null
    val stat = response.stats.firstOrNull { it.id == settings.stat }?.takeIf { it.id != "none" }
    val value = response.values[settings.stat] ?: response.preview?.value?.takeIf { settings.stat == response.settings?.stat }
    fun places(part: String) = TeamTagPlace.ALL.filter { it in settings.placesOf(part) }
    val customizer = PlayerCustomizer(
        uuid = Player.accountUuid(),
        name = Player.accountName(),
        rank = response.rank ?: "supporter",
        color = settings.color,
        label = stat?.label,
        value = if (stat != null) value ?: "..." else null,
        places = CustomizerPlaces(
            places(CustomizerPart.LOGO), places(CustomizerPart.NAME), places(CustomizerPart.LEVEL), places(CustomizerPart.RANK), stat = places(CustomizerPart.STAT),
        ),
        colors = CustomizerColors(
            settings.colorOf(CustomizerPart.STAT), settings.colorOf(CustomizerPart.NAME), settings.colorOf(CustomizerPart.LEVEL),
            settings.colorOf(CustomizerPart.TAG), settings.colorOf(CustomizerPart.RANK),
        ),
        look = settings.look,
    )
    return Preview(customizer, response, settings)
}
