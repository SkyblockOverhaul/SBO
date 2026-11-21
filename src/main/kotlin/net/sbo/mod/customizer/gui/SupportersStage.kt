package net.sbo.mod.customizer.gui

import com.mojang.authlib.GameProfile
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.entity
import net.sbo.guilib.fabric.entity.FakePlayer
import net.sbo.mod.customizer.SboCustomizer
import net.sbo.mod.customizer.CustomizerColors
import net.sbo.mod.customizer.CustomizerEffect
import net.sbo.mod.customizer.CustomizerLook
import net.sbo.mod.customizer.CustomizerMotion
import net.sbo.mod.customizer.CustomizerPlaces
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.customizer.PartColor
import net.sbo.mod.customizer.PlayerCustomizer
import net.sbo.mod.customizer.TeamTagPlace
import java.util.UUID
import kotlin.math.roundToInt

/** [changes] only makes the stage draw again after a setting changed. */
internal data class SupportersStageProps(val textSizes: TextSizes, val changes: Int)

private const val SAMPLE_NAME = "SkyblockOverhaul"
private const val SAMPLE_SIZE = 1.25f

/** Moving colors, letters, symbols, bold and a size, so every setting on Supporters changes something. */
private val SAMPLE_CUSTOMIZER = PlayerCustomizer(
    uuid = "00000000-0000-4000-8000-00000000005b",
    name = SAMPLE_NAME,
    rank = "supporter",
    color = "#55FFFF",
    label = "Mythos Kills",
    value = "12,345",
    places = CustomizerPlaces(
        logo = TeamTagPlace.ALL.toList(), name = TeamTagPlace.ALL.toList(), level = TeamTagPlace.LEVEL.toList(),
        rank = TeamTagPlace.ALL.toList(), stat = listOf(TeamTagPlace.HEAD),
    ),
    colors = CustomizerColors(
        stat = PartColor("#FFAA00"),
        name = PartColor("#55FFFF", to = "#FF55FF", effect = CustomizerEffect.WAVE, motion = CustomizerMotion.WAVY, bold = true, before = "★", after = "★"),
        level = PartColor("#FFFF55"),
        rank = PartColor("#FF5555", effect = CustomizerEffect.PULSE),
    ),
    look = CustomizerLook(size = SAMPLE_SIZE),
)

internal val SupportersStage = component<SupportersStageProps>("CustomizerSupportersStage") { (textSizes, _) ->
    val model = useMemo { FakePlayer.of(GameProfile(UUID.fromString(SAMPLE_CUSTOMIZER.uuid), SAMPLE_NAME)) }
    val frame = useState(0L)
    val data = DataManager.sboData
    val colors = data.customizerAnimationsOfOthers
    useInterval(FRAME_MS) { if (colors || data.customizerMotionOfOthers) frame.value = System.currentTimeMillis() / FRAME_MS }

    val customizer = if (data.customizerMotionOfOthers) SAMPLE_CUSTOMIZER else SboCustomizer.withoutMotion(SAMPLE_CUSTOMIZER)
    val size = if (data.customizerSizesOfOthers) SAMPLE_SIZE else 1f
    fun base(after: String = "") = legacyText("§8[§b350§8] $SAMPLE_RANK§b$SAMPLE_NAME$after")
    /** Colors and letters move apart, as in game: Moving colors only stops the colors. */
    val head = SboCustomizer.previewLine(customizer, base(), TeamTagPlace.HEAD, null, animate = colors, moveChars = true)
    val tab = if (data.customizerInTab) SboCustomizer.previewLine(customizer, base(), TeamTagPlace.TAB, null, animate = colors && data.customizerAnimationsInTab) else base()
    val chatAllowed = data.customizerAnimationsInChat == SboCustomizer.NAMES_ALL
    val chatMoves = chatAllowed && colors
    val chat = if (data.customizerInChat) {
        SboCustomizer.previewLine(customizer, base("§f: Hello!"), TeamTagPlace.CHAT, null, animate = chatMoves, moveChars = chatAllowed)
    } else {
        base("§f: Hello!")
    }

    /** Names inside a message get the supporter's look only for "Everyone". */
    val namesMode = data.customizerNamesInChat
    fun message(text: String) = legacyText("§2Guild > §7Steve§f: $text")
    val messageStart = "Guild > Steve: ".length
    val mention = if (namesMode == SboCustomizer.NAMES_ALL && data.customizerInChat) {
        SboCustomizer.previewMention(customizer, message("gg $SAMPLE_NAME"), SAMPLE_NAME, messageStart, animate = chatMoves, moveChars = chatAllowed)
    } else {
        message("gg $SAMPLE_NAME")
    }
    val elsewhereText = "§eParty Leader: $SAMPLE_RANK§b$SAMPLE_NAME"
    val elsewhere = if (data.customizerReplaceEverywhere && namesMode == SboCustomizer.NAMES_ALL && data.customizerInChat) {
        SboCustomizer.previewMention(customizer, legacyText(elsewhereText), SAMPLE_NAME, 0, animate = colors, everywhere = true)
    } else {
        legacyText(elsewhereText)
    }

    div(className = "cz-stage") {
        div(className = "cz-scene") {
            div(className = "cz-figure") {
                if (SboCustomizer.hasHeadLine(customizer)) {
                    div(className = "cz-nametag", style = textSize(textSizes.tag)) { game(SboCustomizer.line(customizer, animate = colors, moveChars = true)) }
                }
                div(className = "cz-nametag", style = textSize(textSizes.name)) { game(head) }
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
            div(className = "cz-line cz-line-tab") { game(tab) }
            div(className = "cz-line cz-line-chat") { game(chat) }
            if (namesMode != SboCustomizer.NAMES_OFF) div(className = "cz-line cz-line-chat") { game(mention) }
            if (namesMode != SboCustomizer.NAMES_OFF && data.customizerReplaceEverywhere) div(className = "cz-line") { game(elsewhere) }
        }
        div(className = "cz-hint cz-status") { +"A sample supporter as you see other players." }
    }
}
