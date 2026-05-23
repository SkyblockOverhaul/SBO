package net.sbo.mod.utils.waypoint

import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.math.SboVec
import net.sbo.mod.utils.render.RenderUtils3D
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
 * @param beam Whether to draw a beam at the waypoint.
 * @param distance Whether to display the distance in meters (blocks) to the waypoint.
 */
class Waypoint(
    var text: String,
    val x: Double,
    val y: Double,
    val z: Double,
    var r: Float,
    var g: Float,
    var b: Float,
    val ttl: Int = 0,
    val type: String = "normal",
    var line: Boolean = false,
    var beam: Boolean = true,
    var distance: Boolean = true
) {
    var pos: SboVec = SboVec(this.x, this.y, this.z)
    var color: Color = Color(this.r, this.g, this.b)
    var hexCode: Int = this.color.rgb
    val alpha: Double = 0.5
    var hidden: Boolean = false
    val creation: Long = System.currentTimeMillis()
    var formatted: Boolean = false
    var distanceRaw: Double = 0.0
    var distanceText: String = ""
    var formattedText: String = ""
    var warp: String? = null

    fun distanceToPlayer(): Double {
        val playerPos = Player.getLastPosition()
        return sqrt((playerPos.x - this.pos.x).pow(2) + (playerPos.y - this.pos.y).pow(2) + (playerPos.z - this.pos.z).pow(2))
    }

    private fun setWarpText(isBestGuess: Boolean = true) {
        if (isBestGuess) {
            this.warp = WaypointManager.getClosestWarp(this.pos)
            this.formattedText = this.warp?.let {
                "$text§7 (warp $it)${this.distanceText}"
            } ?: "${this.text}${this.distanceText}"
        } else {
            this.formattedText = "${this.text}${this.distanceText}"
        }
    }

    fun format(
        inqWaypoints: List<Waypoint>,
        closestBurrowDistance: Double,
        isBestGuess: Boolean = false
    ) {
        this.distanceRaw = distanceToPlayer()
        this.distanceText = if (distance) " §b[${distanceRaw.roundToInt()}m]" else ""

        when (this.type) {
            "guess", "arrow" -> {
                this.color = Color(if (WaypointManager.getBestGuess() == this) Customization.closestColor else Customization.guessColor)
                this.line = Diana.guessLine && closestBurrowDistance > 60 && inqWaypoints.isEmpty() && isBestGuess
                this.r = color.red / 255f
                this.g = color.green / 255f
                this.b = color.blue / 255f
                this.hexCode = color.rgb

                WaypointManager.waypointExists("burrow", this.pos).let { (exists, wp) ->
                    if (exists && wp != null) this.hidden = wp.distanceToPlayer() < 60
                }
                setWarpText(isBestGuess)
            }
            "rareMob" -> {
                if (inqWaypoints.lastOrNull() == this) {
                    setWarpText()
                    this.line = Diana.inqLine
                }
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

    private fun extractColorComponents(color: Color): FloatArray {
        return floatArrayOf(color.red / 255f, color.green / 255f, color.blue / 255f)
    }

    fun render(context: WorldRenderContext) {
        if (!this.formatted || this.hidden) return
        if (this.type == "guess" && this.distanceRaw <= Diana.removeGuessDistance) return

        RenderUtils3D.renderWaypoint(
            context,
            this.formattedText,
            this.pos,
            floatArrayOf(this.r, this.g, this.b),
            this.hexCode,
            this.alpha.toFloat(),
            true,
            this.line,
            Diana.dianaLineWidth.toFloat(),
            this.beam,
            lineColor = if (WaypointManager.getBestGuess() == this) extractColorComponents(Color(Customization.closestColor)) else null
        )
    }
}
