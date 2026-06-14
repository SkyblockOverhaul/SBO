package net.sbo.mod.utils.waypoint

import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.render.RenderUtils3D
import net.sbo.mod.utils.game.World
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.awt.Color

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext

/**
 * @class Waypoint
 * @description A class to create waypoints in the game.
 * @param text The text to display on the waypoint.
 * @param x The x-coordinate of the waypoint.
 * @param y The y-coordinate of the waypoint.
 * @param z The z-coordinate of the waypoint.
 * @param r The red color component of the waypoint.
 * @param g The green color component of the waypoint.
 * @param b The blue color component of the waypoint.
 * @param ttl The time to live for the waypoint in seconds (0 for infinite).
 * @param type The type of the waypoint for customization.
 * @param line Whether to draw a line to the waypoint.
 * @param distance Whether to display the distance in meters (blocks) to the waypoint.
 */
class Waypoint(
    var text: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val ttl: Int = 1800,
    val type: String = "normal",
    var line: Boolean = false,
    var distance: Boolean = true
) {
    var pos: SboVec = SboVec(this.x, this.y, this.z)
    val alpha: Double = 0.5
    var hidden: Boolean = false
    val creation: Long = System.currentTimeMillis()
    var formatted: Boolean = false
    var distanceRaw: Double = 0.0
    var distanceText: String = ""
    var formattedText: String = ""
    var isClosest = false

    fun distanceToPlayer(): Double {
        val playerPos = Player.getLastPosition()
        return sqrt((playerPos.x - this.pos.x).pow(2) + (playerPos.y - this.pos.y).pow(2) + (playerPos.z - this.pos.z).pow(2))
    }

    private fun setWarpText() {
        if (isClosest) {
            val closest = WaypointManager.getClosestWarp(this.pos)

            this.formattedText = closest?.let {
                "$text§7 (warp $it)${this.distanceText}"
            } ?: "${this.text}${this.distanceText}"

            val title = Diana.showTitleWhenWarpAvailable
            if (title && closest != null && World.getWorld() == "Hub" && Helper.hasSpade) {
                 val warpName = closest.replaceFirstChar(Char::titlecase)
                 Helper.showTitle("§bWarp §e$warpName$distanceText", "", 0, 1, 0) // 1 ticks because next tick this will be called again
            }
        } else {
            this.formattedText = "${this.text}${this.distanceText}"
        }
    }

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
        this.distanceText = if (distance) " §b[${distanceRaw.roundToInt()}m]" else ""

        when (this.type) {
            "guess", "arrow" -> {
                this.line = Diana.guessLine && inqWaypoints.isEmpty() && isClosest

                setWarpText()
            }
            "burrow" -> {
                this.line = Diana.burrowLine && inqWaypoints.isEmpty() && isClosest

                setWarpText()
            }
            "rareMob" -> {
                val newest = inqWaypoints.lastOrNull() == this

                if (newest) {
                    setWarpText()
                }

                this.line = newest && Diana.inqLine
            }
            else -> {
                this.formattedText = "$text$distanceText"
            }
        }
        formatted = true
    }

    fun hide(): Waypoint {
        this.hidden = true
        return this
    }

    fun show(): Waypoint {
        this.hidden = false
        return this
    }

    fun render(context: WorldRenderContext) {
        if (!this.formatted || this.hidden) return
        if (this.type == "guess" && this.distanceRaw <= Diana.removeGuessDistance) return

        val rgbAndHex = getRgbAndHex()

        RenderUtils3D.renderWaypoint(
            context,
            this.formattedText,
            this.pos,
            rgbAndHex.rgb,
            rgbAndHex.hex,
            this.alpha.toFloat(),
            true,
            this.line,
            Diana.dianaLineWidth.toFloat(),
            Diana.showBeaconBeam
        )
    }
}
