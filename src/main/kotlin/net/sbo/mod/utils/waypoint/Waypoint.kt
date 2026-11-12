package net.sbo.mod.utils.waypoint

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.util.FormattedCharSequence
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.RareMob
import net.sbo.mod.diana.burrows.ChainTracker
import net.sbo.mod.settings.categories.Burrows
import net.sbo.mod.settings.categories.Warps
import net.sbo.mod.settings.categories.Waypoints
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.render.RenderUtils3D
import java.awt.Color
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val MIN_OPACITY = 0.2f
private const val MAX_OPACITY = 1.0f
private const val FADE_START_DISTANCE = 4.5
private const val FADE_END_DISTANCE = 100.0

internal fun calculateDynamicOpacity(distance: Double): Float = when {
    !distance.isFinite() -> MAX_OPACITY
    Waypoints.lookAlike -> (0.1f + 0.005f * (distance * distance).toFloat()).coerceIn(MIN_OPACITY, MAX_OPACITY)
    distance <= FADE_START_DISTANCE -> MIN_OPACITY
    distance >= FADE_END_DISTANCE -> MAX_OPACITY
    else -> MIN_OPACITY + (MAX_OPACITY - MIN_OPACITY) * ((distance - FADE_START_DISTANCE) / (FADE_END_DISTANCE - FADE_START_DISTANCE)).toFloat()
}

/**
 * @class Waypoint
 * @description A class to create waypoints in the game.
 * @param text The text to display on the waypoint.
 * @param x The x-coordinate of the waypoint.
 * @param y The y-coordinate of the waypoint.
 * @param z The z-coordinate of the waypoint.
 * @param type The type of the waypoint for customization.
 * @param ttl The time to live for the waypoint in seconds (0 for infinite).
 * @param line Whether to draw a line to the waypoint.
 * @param rareMobType The type of the rare mob, null if this not a rare mob waypoint.
 * @param rareMobSentBy The name of the player that sent or owns the rare mob, null if this not a rare mob waypoint or if the player is unknown.
 */
class Waypoint(
    var text: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val type: String,
    val ttl: Long = 1800,
    var line: Boolean = false,
    val rareMobType: RareMob? = null,
    val rareMobSentBy: String? = null
) {
    var pos: SboVec = SboVec(this.x, this.y, this.z)
    var hidden: Boolean = false
    val creationNs: Long = System.nanoTime()
    private var formatted: Boolean = false
    private var distanceRaw: Double = 0.0
    private var distanceText: String = ""
    private var component: Component = Component.nullToEmpty(text)
    private var formattedText: String = text
        set(value) {
            field = value
            hasText = value.isNotEmpty()

            component = Component.nullToEmpty(value)

            visualOrderText = component.visualOrderText
            textWidth = mc.font.width(visualOrderText)
        }
    // Font.width may lazily bake and upload glyphs, which is render-thread only.
    // A waypoint is sometimes constructed while handling a network packet, before
    // format() runs on the client thread and fills this cache.
    private var textWidth = 0
    private var hasText = text.isNotEmpty()

    var isClosest = false

    private companion object {
        /** The warp title and subtitle set last, so the next tick knows the title on screen is its own and may replace it. */
        var lastWarpTitle = ""
        var lastWarpSubtitle = ""
    }
    var timesDug = 0
    var userInteractedWith = false
    private var dynamicOpacity = 1.0f
    var preventInvalidRemoval = false
    var rareMobMissingTicks: Int = 0
    var preventStaleRareMobRemoval = false

    private var visualOrderText = Component.nullToEmpty(text).visualOrderText

    private var secondaryText: Component? = null
    private var secondaryTextWidth = 0
    private var secondaryVisualOrderText: FormattedCharSequence? = null

    fun hasStrongerStateThan(other: Waypoint): Boolean =
        this.timesDug > other.timesDug || this.userInteractedWith && !other.userInteractedWith

    fun carryOverState(other: Waypoint) {
        val otherTimesDug = other.timesDug

        if (otherTimesDug > this.timesDug) {
            this.timesDug = otherTimesDug
        }

        val otherInteractedWith = other.userInteractedWith

        if (otherInteractedWith && !this.userInteractedWith) {
            this.userInteractedWith = true
        }
    }

    fun distanceToPlayer(): Double {
        val playerPos = Player.getLastPosition()
        val dx = playerPos.x - this.pos.x
        val dy = playerPos.y - this.pos.y
        val dz = playerPos.z - this.pos.z

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun setDistanceText() {
        val dist = distanceRaw.roundToInt()
        val showDistance = Waypoints.showDistanceCutoff <= 0 || dist > Waypoints.showDistanceCutoff

        distanceText = if (showDistance) {
            if (Waypoints.lookAlike) "§e${dist}m" else " §b[${dist}m]"
        } else {
            ""
        }

        clearSecondaryText()
    }

    private fun getWaypointDistanceText(): String = if (Waypoints.lookAlike) "" else distanceText

    private fun setSecondaryDistanceText() {
        if (!Waypoints.lookAlike || distanceText.isEmpty()) return

        val component = Component.nullToEmpty(distanceText.trim())

        secondaryText = component
        val visualOrderText = component.visualOrderText

        secondaryTextWidth = mc.font.width(visualOrderText)
        secondaryVisualOrderText = visualOrderText
    }

    private fun clearSecondaryText() {
        secondaryText = null
        secondaryTextWidth = 0
        secondaryVisualOrderText = null
    }

    private fun setWarpText() {
        val showTimesDug = Waypoints.showTimesDug && this.type == "burrow" && this.text != "Start" && this.text != "Guess"
        val timesDug = this.timesDug
        val dist = Waypoints.showDistanceCutoff == 0 || this.distanceToPlayer() < Waypoints.showDistanceCutoff
        // A treasure burrow that ends its chain is dug out with one click
        val digsNeeded = if (this.text == "Treasure" && ChainTracker.isChainEnd(pos, this.type)) 1 else 2
        val timesDugText = if (showTimesDug && dist) " §7[§" + (if (timesDug >= 1) "6" else "e") + timesDug + "§7/§a$digsNeeded§7]" else ""

        val waypointDistanceText = getWaypointDistanceText()
        val chainStepText = getChainStepText()

        if (isClosest) {
            val closest = if (this.type == "rareMob") WaypointManager.getFinalClosestWarpToFixedTarget(this.pos) else WaypointManager.getFinalClosestWarp(this.pos)

            this.formattedText = closest?.let {
                "$text§7 (warp $it)$waypointDistanceText$chainStepText$timesDugText"
            } ?: "${this.text}$waypointDistanceText$chainStepText$timesDugText"

            val title = Warps.showTitleWhenWarpAvailable
            if (title && closest != null && World.isInHub() && Helper.hasSpade) {
                val warpName = closest.replaceFirstChar(Char::titlecase)

                val text = "§" + (if (this.type == "rareMob") "d" else "b") + "Warp §e$warpName${if (Waypoints.lookAlike) " §b[${distanceRaw.roundToInt()}m]" else distanceText}"
                val asSubtitle = Warps.warpTitleAsSubtitle
                val title = if (asSubtitle) "" else text
                val subtitle = if (asSubtitle) text else ""

                // Another title (Use Spade, rare mob, drops, Hypixel) is showing: wait until it is gone, so the warp text is not mixed into it.
                // A title we set ourselves last tick, an empty one, or one whose time ran out is overwritten, which resets its times to 1 tick every tick.
                // A times packet arriving while a title shows (Hypixel) changes its time: with a stay of 0 the title is left with time 0 but is never cleared,
                // so it counted as "showing" forever and the warp title never came back; with a long fade in it stayed almost invisible.
                val currentTitle = mc.gui.title?.string.orEmpty()
                val currentSubtitle = mc.gui.subtitle?.string.orEmpty()
                val isEmpty = currentTitle.isEmpty() && currentSubtitle.isEmpty()
                val isOurs = currentTitle == lastWarpTitle && currentSubtitle == lastWarpSubtitle
                val titleBusy = mc.gui.title != null && mc.gui.titleTime > 0 && !isEmpty && !isOurs

                if (!titleBusy) {
                    Helper.showTitle(title, subtitle, 0, 1, 0) // 1 tick because next tick this will be called again
                    lastWarpTitle = title
                    lastWarpSubtitle = subtitle
                }
            }
        } else {
            this.formattedText = "${this.text}$waypointDistanceText$chainStepText$timesDugText"
        }
    }

    /** Which burrow of the chain a spade or arrow guess is, like " (3/10)", on the main line with and without Look Alike. */
    private fun getChainStepText(): String {
        if (!Burrows.showChainStep || this.type != "guess" && this.type != "arrow") return ""
        val step = ChainTracker.stepAt(this.pos) ?: return ""
        return " §7${step.step}/${step.max}"
    }

    private fun updateDynamicOpacity(): Float =
        calculateDynamicOpacity(distanceRaw)

    private fun getColor(): Color {
        if (Burrows.highlightChainEnds && (this.type == "guess" || this.type == "arrow" || this.type == "burrow") && ChainTracker.isChainEnd(this.pos, this.type)) {
            return Color(Burrows.ChainEndColor)
        }
        when (this.type) {
            "guess", "arrow" -> {
                if (isClosest) {
                    return Color(Waypoints.ClosestGuessColor)
                }
                return Color(Waypoints.OtherGuessColor)
            }
            "burrow" -> {
                if (this.text == "Guess") {
                    return if (isClosest) Color(Waypoints.ClosestGuessColor) else Color(Waypoints.OtherGuessColor)
                }
                return when (this.text) {
                    "Start" -> Color(Waypoints.StartColor)
                    "Mob" -> Color(Waypoints.MobColor)
                    "Treasure" -> Color(Waypoints.TreasureColor)
                    else -> Color(255, 255, 255) // shouldn't happen
                }
            }
            "rareMob" -> {
                return Color(Waypoints.RareMobColor)
            }
            "world" -> {
                return Color(Waypoints.OtherWaypointColor)
            }
            "subGuess" -> {
                return Color(Waypoints.SubGuessColor)
            }
            "debug" -> {
                return Color(255, 255, 255)
            }
        }
        return Color(255, 255, 255) // shouldn't happen
    }

    private class RgbAndHex(val rgb: FloatArray, val hex: Int)

    private fun getRgbAndHex(): RgbAndHex {
        val color = getColor()

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f

        return RgbAndHex(floatArrayOf(r, g, b), color.rgb)
    }

    fun format(
        inqWaypoints: List<Waypoint>
    ) {
        this.distanceRaw = distanceToPlayer()
        this.dynamicOpacity = if (Waypoints.dynamicWaypointOpacity) updateDynamicOpacity() else (Waypoints.waypointOpacity / 100.0).toFloat().coerceIn(0.2f, 1.0f)

        setDistanceText()

        when (this.type) {
            "guess", "arrow", "burrow" -> {
                if (isClosest && !inqWaypoints.isEmpty()) isClosest = false
                this.line = Waypoints.guessAndBurrowLine && isClosest

                setWarpText()
                setSecondaryDistanceText()
            }

            "rareMob" -> {
                val newest = inqWaypoints.last() == this

                if (newest) isClosest = true
                this.line = newest && Waypoints.inqLine && this.distanceRaw >= 8.0

                if (newest) {
                    setWarpText()
                } else {
                    this.formattedText = "$text${getWaypointDistanceText()}"
                }

                setSecondaryDistanceText()
            }

            "world" -> {
                this.line = false
                this.formattedText = "$text${getWaypointDistanceText()}"
                setSecondaryDistanceText()
            }

            "subGuess", "debug" -> {
                this.line = false
                this.formattedText = text
            }

            else -> {
                this.line = false
                this.formattedText = "$text${getWaypointDistanceText()}"
                setSecondaryDistanceText()
            }
        }

        formatted = true
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return color and 0x00FFFFFF or (clampedAlpha shl 24)
    }

    fun isOlderThan(duration: Duration): Boolean = this.creationNs + duration.toNanos() < System.nanoTime()

    fun render(context: LevelRenderContext) {
        if (!this.formatted || this.hidden) return

        val rgbAndHex = getRgbAndHex()

        val waypointOpacity = this.dynamicOpacity
        val waypointTextOpacity = (Waypoints.waypointTextOpacity / 100.0).toFloat()

        RenderUtils3D.renderWaypoint(
            context,
            this.hasText,
            //#if MC < 26.2
            this.component,
            //#endif
            this.textWidth,
            //#if MC > 26.1
            //$$ visualOrderText,
            //#endif
            //#if MC < 26.2
            this.secondaryText,
            //#endif
            this.secondaryTextWidth,
            //#if MC > 26.1
            //$$ secondaryVisualOrderText,
            //#endif
            this.pos,
            rgbAndHex.rgb,
            applyAlpha(rgbAndHex.hex, waypointTextOpacity),
            waypointOpacity,
            this.line,
            Waypoints.dianaLineWidth.toFloat(),
            if (type == "subGuess") false else Waypoints.showBeaconBeam
        )
    }
}
