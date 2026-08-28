package net.sbo.mod.utils.waypoint

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.ChatUtils
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

internal fun calculateDynamicOpacity(distance: Double): Float {
    if (!distance.isFinite()) {
        return MAX_OPACITY
    }

    if (distance <= FADE_START_DISTANCE) {
        return MIN_OPACITY
    }

    if (distance >= FADE_END_DISTANCE) {
        return MAX_OPACITY
    }

    val progress = (
        (distance - FADE_START_DISTANCE) /
            (FADE_END_DISTANCE - FADE_START_DISTANCE)
    ).toFloat()

    return (
        MIN_OPACITY +
            (MAX_OPACITY - MIN_OPACITY) * progress
    ).coerceIn(MIN_OPACITY, MAX_OPACITY)
}

/**
 * @class Waypoint
 * @description A class to create waypoints in the game.
 * @param text The text to display on the waypoint.
 * @param x The x-coordinate of the waypoint.
 * @param y The y-coordinate of the waypoint.
 * @param z The z-coordinate of the waypoint.
 * @param ttl The time to live for the waypoint in seconds (0 for infinite).
 * @param type The type of the waypoint for customization.
 * @param line Whether to draw a line to the waypoint.
 */
class Waypoint(
    var text: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val ttl: Long = 1800,
    val type: String = "normal",
    var line: Boolean = false
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
            textWidth = mc.font.width(value)
            hasText = value.isNotEmpty()

            component = ChatUtils.fromLegacy(value)
            visualOrderText = component.visualOrderText
        }
    private var textWidth = mc.font.width(text)
    private var hasText = text.isNotEmpty()

    var isClosest = false
    var timesDug = 0
    var userInteractedWith = false
    private var dynamicOpacity = 1.0f
    var preventInvalidRemoval = false
    var rareMobMissingTicks: Int = 0

    private var visualOrderText = ChatUtils.fromLegacy(text).visualOrderText

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

    fun distanceToPlayerIgnoringY(): Double {
        val playerPos = Player.getLastPosition()
        val dx = playerPos.x - this.pos.x
        val dz = playerPos.z - this.pos.z

        return sqrt(dx * dx + dz * dz)
    }

    private fun setWarpText() {
        val showTimesDug = Customization.showTimesDug && this.type == "burrow" && this.text != "Start"
        val timesDug = this.timesDug
        val dist = Customization.showDistanceCutoff == 0 || this.distanceToPlayer() < Customization.showDistanceCutoff
        val timesDugText = if (showTimesDug && dist) " §7[§" + (if (timesDug >= 1) "6" else "e") + timesDug + "§7/§a2§7]" else ""

        if (isClosest) {
            val closest = if (this.type == "rareMob") WaypointManager.getFinalClosestWarpToFixedTarget(this.pos) else WaypointManager.getFinalClosestWarp(this.pos)

            this.formattedText = closest?.let {
                "$text§7 (warp $it)${this.distanceText}$timesDugText"
            } ?: "${this.text}${this.distanceText}$timesDugText"

            val title = Diana.showTitleWhenWarpAvailable
            if (title && closest != null && World.getWorld() == "Hub" && Helper.hasSpade) {
                val warpName = closest.replaceFirstChar(Char::titlecase)

                val text = "§" + (if (this.type == "rareMob") "d" else "b") + "Warp §e$warpName$distanceText"
                val asSubtitle = Customization.warpTitleAsSubtitle

                val titleBusy = !mc.gui.title?.string.isNullOrEmpty() && mc.gui.titleTime > 0 // When asSubtitle is disabled, Helper.showTitle checks internally if busy or not; but when its subtitle, it appends warp subtitle inside another one, e.g. Use Spade one, with higher duration, causing warp title to keep showing as subtitle even after warping till the main title (e.g., Use Spade one) expires; this makes it delay warp title till the original title disappears which fixes the issue.

                if (!asSubtitle || !titleBusy) {
                    val title = if (asSubtitle) "" else text
                    val subtitle = if (asSubtitle) text else null

                    Helper.showTitle(title, subtitle, 0, 1, 0, overwrite = false) // 1 ticks because next tick this will be called again. Overwrite false to not wipe rare mob title or use spade title.
                }
            }
        } else {
            this.formattedText = "${this.text}${this.distanceText}$timesDugText"
        }
    }

    private fun updateDynamicOpacity(): Float =
        calculateDynamicOpacity(distanceRaw)

    private fun getColor(): Color {
        when (this.type) {
            "guess", "arrow" -> {
                if (isClosest) {
                    return Color(Customization.ClosestGuessColor)
                }
                return Color(Customization.OtherGuessColor)
            }
            "burrow" -> {
                return when (this.text) {
                    "Start" -> Color(Customization.StartColor)
                    "Mob" -> Color(Customization.MobColor)
                    "Treasure" -> Color(Customization.TreasureColor)
                    else -> Color(255, 255, 255) // shouldn't happen
                }
            }
            "rareMob" -> {
                return Color(Customization.RareMobColor)
            }
            "world" -> {
                return Color(Customization.OtherWaypointColor)
            }
            "subGuess" -> {
                return Color(Customization.SubGuessColor)
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
        this.dynamicOpacity = if (Customization.dynamicWaypointOpacity) updateDynamicOpacity() else (Customization.waypointOpacity / 100.0).toFloat().coerceIn(0.2f, 1.0f)

        val dist = distanceRaw.roundToInt()

        val showDistance = Customization.showDistanceCutoff <= 0 || dist > Customization.showDistanceCutoff
        this.distanceText = if (showDistance) " §b[${dist}m]" else ""

        when (this.type) {
            "guess", "arrow", "burrow" -> {
                if (isClosest && !inqWaypoints.isEmpty()) isClosest = false
                this.line = Diana.guessAndBurrowLine && isClosest

                setWarpText()
            }

            "rareMob" -> {
                val newest = inqWaypoints.lastOrNull() == this

                if (newest) isClosest = true
                this.line = newest && Diana.inqLine && this.distanceRaw >= 8.0

                if (newest) {
                    setWarpText()
                } else {
                    this.formattedText = "$text$distanceText"
                }
            }

            "world" -> {
                this.line = false
                this.formattedText = "$text$distanceText"
            }

            "subGuess", "debug" -> {
                this.line = false
                this.formattedText = text
            }

            else -> {
                this.line = false
                this.formattedText = "$text$distanceText"
            }
        }

        formatted = true
    }

    private fun applyAlpha(color: Int, alpha: Float): Int {
        val clampedAlpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return color and 0x00FFFFFF or (clampedAlpha shl 24)
    }

    fun isOlderThan(duration: Duration): Boolean {
        return this.creationNs + duration.toNanos() < System.nanoTime()
    }

    fun render(context: LevelRenderContext) {
        if (!this.formatted || this.hidden) return

        val rgbAndHex = getRgbAndHex()

        val waypointOpacity = this.dynamicOpacity
        val waypointTextOpacity = (Customization.waypointTextOpacity / 100.0).toFloat()

        RenderUtils3D.renderWaypoint(
            context,
            this.hasText,
            this.component,
            this.textWidth,
            this.visualOrderText,
            this.pos,
            rgbAndHex.rgb,
            applyAlpha(rgbAndHex.hex, waypointTextOpacity),
            waypointOpacity,
            this.line,
            Diana.dianaLineWidth.toFloat(),
            if (type == "subGuess") false else Diana.showBeaconBeam
        )
    }
}
