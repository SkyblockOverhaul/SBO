package net.sbo.mod.guis.customizer

import com.mojang.authlib.GameProfile
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.entity
import net.sbo.guilib.fabric.entity.FakePlayer
import net.sbo.mod.general.SboBadges
import net.sbo.mod.utils.data.BadgeColors
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeLook
import net.sbo.mod.utils.data.BadgeMotion
import net.sbo.mod.utils.data.BadgePlaces
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.data.TeamTagPlace
import java.util.UUID
import kotlin.math.roundToInt

// changes only makes the stage draw again after a setting changed
internal data class SupportersStageProps(val textSizes: TextSizes, val changes: Int)

private const val SAMPLE_NAME = "SkyblockOverhaul"
private const val SAMPLE_TEAM_NAME = "SboDev"
private const val SAMPLE_SIZE = 1.25f

// Moving colors, letters, symbols, bold and a size, so every setting on Supporters changes something
private val SAMPLE_BADGE = SboBadge(
    uuid = "00000000-0000-4000-8000-00000000005b",
    name = SAMPLE_NAME,
    rank = "supporter",
    color = "#55FFFF",
    label = "Mythos Kills",
    value = "12,345",
    places = BadgePlaces(
        logo = TeamTagPlace.ALL.toList(), name = TeamTagPlace.ALL.toList(), level = TeamTagPlace.LEVEL.toList(),
        rank = TeamTagPlace.ALL.toList(), stat = listOf(TeamTagPlace.HEAD),
    ),
    colors = BadgeColors(
        stat = PartColor("#FFAA00"),
        name = PartColor("#55FFFF", to = "#FF55FF", effect = BadgeEffect.WAVE, motion = BadgeMotion.WAVY, bold = true, before = "★", after = "★"),
        level = PartColor("#FFFF55"),
        rank = PartColor("#FF5555", effect = BadgeEffect.PULSE),
    ),
    look = BadgeLook(size = SAMPLE_SIZE),
)

internal val SupportersStage = component<SupportersStageProps>("CustomizerSupportersStage") { (textSizes, _) ->
    val model = useMemo { FakePlayer.of(GameProfile(UUID.fromString(SAMPLE_BADGE.uuid), SAMPLE_NAME)) }
    val frame = useState(0L)
    val data = DataManager.sboData
    val colors = data.badgeAnimationsOfOthers
    useInterval(FRAME_MS) { if (colors || data.badgeMotionOfOthers) frame.value = System.currentTimeMillis() / FRAME_MS }

    val badge = if (data.badgeMotionOfOthers) SAMPLE_BADGE else SboBadges.withoutMotion(SAMPLE_BADGE)
    val size = if (data.badgeSizesOfOthers) SAMPLE_SIZE else 1f
    fun base(after: String = "") = legacyText("§8[§b350§8] $SAMPLE_RANK§b$SAMPLE_NAME$after")
    // Colors and letters move apart, as in game: Moving colors only stops the colors
    val head = SboBadges.previewLine(badge, base(), TeamTagPlace.HEAD, null, animate = colors, moveChars = true)
    val tab = if (data.badgesInTab) SboBadges.previewLine(badge, base(), TeamTagPlace.TAB, null, animate = colors && data.badgeAnimationsInTab) else base()
    val chatAllowed = data.badgeAnimationsInChat == SboBadges.NAMES_ALL
    val chatMoves = chatAllowed && colors
    val chat = if (data.badgesInChat) {
        SboBadges.previewLine(badge, base("§f: Hello!"), TeamTagPlace.CHAT, null, animate = chatMoves, moveChars = chatAllowed)
    } else {
        base("§f: Hello!")
    }

    // Names inside a message, one line each so they fit: the supporter only for "Everyone", the team member with its tag while that is on
    val namesMode = data.badgeNamesInChat
    fun message(text: String) = legacyText("§2Guild > §7Steve§f: $text")
    val messageStart = "Guild > Steve: ".length
    val mention = if (namesMode == SboBadges.NAMES_ALL && data.badgesInChat) {
        SboBadges.previewMention(badge, message("gg $SAMPLE_NAME"), SAMPLE_NAME, messageStart, null, animate = chatMoves, moveChars = chatAllowed)
    } else {
        message("gg $SAMPLE_NAME")
    }
    val teamRank = SboBadges.sampleTeamRank()
    val teamMention = teamRank?.let { rank ->
        val line = message("ty $SAMPLE_TEAM_NAME")
        if (data.badgeNamesTag) SboBadges.previewMention(null, line, SAMPLE_TEAM_NAME, messageStart, rank, animate = false) else line
    }
    val elsewhereText = "§eParty Leader: $SAMPLE_RANK§b$SAMPLE_NAME"
    val elsewhere = if (data.badgeReplaceEverywhere && namesMode == SboBadges.NAMES_ALL && data.badgesInChat) {
        SboBadges.previewMention(badge, legacyText(elsewhereText), SAMPLE_NAME, 0, null, animate = colors, everywhere = true)
    } else {
        legacyText(elsewhereText)
    }

    div(className = "cz-stage") {
        div(className = "cz-scene") {
            div(className = "cz-figure") {
                if (SboBadges.hasHeadLine(badge)) {
                    div(className = "cz-nametag", style = textSize(textSizes.tag)) { game(SboBadges.line(badge, animate = colors, moveChars = true)) }
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
            if (namesMode != SboBadges.NAMES_OFF) {
                div(className = "cz-line cz-line-chat") { game(mention) }
                teamMention?.let { line -> div(className = "cz-line cz-line-chat") { game(line) } }
            }
            if (namesMode != SboBadges.NAMES_OFF && data.badgeReplaceEverywhere) div(className = "cz-line") { game(elsewhere) }
        }
        div(className = "cz-hint cz-status") { +"A sample supporter as you see other players." }
    }
}
