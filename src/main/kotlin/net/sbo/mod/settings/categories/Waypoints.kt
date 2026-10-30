package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.diana.RareMob
import java.awt.Color

object Waypoints : Category("Waypoints") {
    override val description = "How guesses, burrows and other waypoints look: lines, beams, colors and text."

    init {
        sub {
            this.title = "Lines"
            this.description = "§bDisable View Bobbing in controls if lines are buggy."
        }
    }

    var guessAndBurrowLine by boolean(true) {
        this.name = Literal("Guess & Burrow Line")
        this.description = Literal("Draws line to the closest guess and/or burrow, Disable View Bobbing in controls if its buggy.")
    }

    var inqLine by boolean(true) {
        this.name = Literal("Rare Mob Line")
        this.description = Literal("Draws line to rare mob waypoints, Disable View Bobbing in controls if its buggy.")
    }

    var dianaLineWidth by int(5) {
        this.range = 1..20
        this.slider = true
        this.name = Literal("Line Width")
        dependsAny(::guessAndBurrowLine, ::inqLine)
        this.description = Literal("The width of the lines drawn for Diana waypoints.")
    }

    var drawOptimalOrderLines by boolean(false) {
        this.name = Literal("Draw Optimal Order Lines")
        this.description = Literal("Draws lines between burrows from closest to your current location to the farthest from current location (e.g., optimal order to do them in).")
    }

    var OptimalOrderLineColor by color(
        Color(1.0f, 1.0f, 1.0f).rgb) {
        this.name = Literal("Optimal Order Line Color")
        depends(::drawOptimalOrderLines)
        this.description = Literal("Color of the line drawn between guesses in the optimal order to them.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Beacon Beam"
        }
    }

    var showBeaconBeam by boolean(true) {
        this.name = Literal("Show Beacon Beam")
        this.description = Literal("Shows a beacon beam for waypoints going to the sky if enabled.")
    }

    var beamDistance by int(8) {
        this.name = Literal("Beacon Beam Distance")
        depends(::showBeaconBeam)
        this.description = Literal("Hide the beam when this many blocks or more close to it.")
    }

    init {
        sub {
            this.title = "Guess Colors"
        }
    }

    var ClosestGuessColor by color(
        Color(0.6f, 0.2f, 0.8f).rgb) {
        this.name = Literal("Closest Guess Color")
        this.description = Literal("Pick a color for closest guess.")
        this.allowAlpha = false
    }

    var OtherGuessColor by color(
        Color(0.0f, 0.964f, 1.0f).rgb) {
        this.name = Literal("Other Guess Color")
        this.description = Literal("Pick a color for other guesses.")
        this.allowAlpha = false
    }

    var SubGuessColor by color(
        Color(0.55f, 0.55f, 0.55f).rgb
    ) {
        this.name = Literal("Sub Guess Color")
        depends(Burrows::showArrowSubGuesses)
        this.description = Literal("Color of inactive arrow guess locations.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Burrow Colors"
        }
    }

    var StartColor by color(
        Color(0.333f, 1.0f, 0.333f).rgb) {
        this.name = Literal("Start Burrow Color")
        this.description = Literal("Pick a color for start burrows.")
        this.allowAlpha = false
    }

    var MobColor by color(
        Color(1.0f, 0.333f, 0.333f).rgb) {
        this.name = Literal("Mob Burrow Color")
        this.description = Literal("Pick a color for mob burrows.")
        this.allowAlpha = false
    }

    var TreasureColor by color(
        Color(1f, 0.666f, 0.0f).rgb) {
        this.name = Literal("Treasure Burrow Color")
        this.description = Literal("Pick a color for treasure burrows.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Waypoint Look"
        }
    }

    var lookAlike by boolean(false) {
        this.name = Literal("Look Alike")
        this.description = Literal("Mimics visuals of SkyHanni on the block outline opacity and guess/distance text. This can help if you are migrating between the mods and can't get used to the look of another.")
    }

    var dynamicWaypointOpacity by boolean(true) {
        this.name = Literal("Dynamic Waypoint Opacity")
        this.description = Literal("Uses a dynamic waypoint opacity that changes based on how far or close you are to the waypoints. If you enable this, the Waypoint Opacity setting below will not take effect.")
    }

    var waypointOpacity by int(50) {
        this.range = 0..100
        this.name = Literal("Waypoint Opacity")
        dependsOff(::dynamicWaypointOpacity)
        this.description = Literal("The opacity of the rendered waypoints. 50 will make it 50% transparent, 100% fully opaque, 0% fully invisible, etc. (default 50)")
    }

    var waypointTextOpacity by int(100) {
        this.range = 0..100
        this.name = Literal("Waypoint Text Opacity")
        this.description = Literal("The opacity of the text on the rendered waypoints. 50 will make it 50% transparent, 100% fully opaque, 0% fully invisible, etc. (default 100)")
    }

    var waypointTextShadow by boolean(true) {
        this.name = Literal("Waypoint Text Shadow")
        this.description = Literal("Enables shadow for waypoint text.")
    }

    var waypointTextScale by float(0.7f) {
        this.name = Literal("Waypoint Text Scale")
        this.description = Literal("Scale of the waypoint text.")
        this.range = 0.3f..2.0f
        this.slider = true
    }

    var showTimesDug by boolean(true) {
        this.name = Literal("Show Times Dug")
        this.description = Literal("Shows times dug on the waypoint text for known burrows.")
    }

    var showDistanceCutoff by int(50) {
        this.range = 0..150
        this.name = Literal("Show Distance & Times Dug Cutoff")
        depends(::showTimesDug)
        this.description = Literal("The distance cutoff at which the distance text disappears, and the times dug text appears. For example if set to 50 will not display the distance text if 50m or closer, and will only display the times dug when 50m or closer. (0 to always show distance and times dug, if times dug is enabled)")
    }

    init {
        sub {
            this.title = "Other Waypoints"
        }
    }

    var patcherWaypoints by boolean(true) {
        this.name = Literal("Waypoints From Chat")
        this.description = Literal("Creates waypoints from chat messages. (format: x: 20, y: 60, z: 80)")
    }

    var RareMobColor by color(
        Color(1.0f, 0.84f, 0.0f).rgb) {
        this.name = Literal("Rare Mob Waypoint Color")
        this.description = Literal("Pick a color for rare mob waypoints.")
        this.allowAlpha = false
    }

    var OtherWaypointColor by color(
        Color(0.0f, 0.2f, 1.0f).rgb) {
        this.name = Literal("Other Waypoint Color")
        this.description = Literal("Pick a color for other waypoints, e.g., waypoints created from non-SBO player-sent coordinates. (Separate from rare mob color)")
        this.allowAlpha = false
    }

    var hideOwnCoordinateWaypoints by boolean(false) {
        this.name = Literal("Hide Own Coordinate Waypoints")
        this.description = Literal("Hides the waypoints from coordinates you sent in chat yourself.")
    }

    var hideOwnWaypoints by options<RareMob> {
        this.name = Literal("Hide Own Rare Mob Waypoints")
        this.description = Literal("Hides the waypoints of these rare mobs when you spawned them.")
    }
}
