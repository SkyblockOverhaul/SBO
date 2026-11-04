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
        this.name = literal("Guess & Burrow Line")
        this.description = literal("Draws line to the closest guess and/or burrow, Disable View Bobbing in controls if its buggy.")
    }

    var inqLine by boolean(true) {
        this.name = literal("Rare Mob Line")
        this.description = literal("Draws line to rare mob waypoints, Disable View Bobbing in controls if its buggy.")
    }

    var dianaLineWidth by int(5) {
        this.range = 1..20
        this.slider = true
        this.name = literal("Line Width")
        dependsAny(::guessAndBurrowLine, ::inqLine)
        this.description = literal("The width of the lines drawn for Diana waypoints.")
    }

    var drawOptimalOrderLines by boolean(false) {
        this.name = literal("Draw Optimal Order Lines")
        this.description = literal("Draws lines between burrows from closest to your current location to the farthest from current location (e.g., optimal order to do them in).")
    }

    var OptimalOrderLineColor by color(
        Color(1.0f, 1.0f, 1.0f).rgb) {
        this.name = literal("Optimal Order Line Color")
        depends(::drawOptimalOrderLines)
        this.description = literal("Color of the line drawn between guesses in the optimal order to them.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Beacon Beam"
        }
    }

    var showBeaconBeam by boolean(true) {
        this.name = literal("Show Beacon Beam")
        this.description = literal("Shows a beacon beam for waypoints going to the sky if enabled.")
    }

    var beamDistance by int(8) {
        this.name = literal("Beacon Beam Distance")
        depends(::showBeaconBeam)
        this.description = literal("Hide the beam when this many blocks or more close to it.")
    }

    init {
        sub {
            this.title = "Guess Colors"
        }
    }

    var ClosestGuessColor by color(
        Color(0.6f, 0.2f, 0.8f).rgb) {
        this.name = literal("Closest Guess Color")
        this.description = literal("Pick a color for closest guess.")
        this.allowAlpha = false
    }

    var OtherGuessColor by color(
        Color(0.0f, 0.964f, 1.0f).rgb) {
        this.name = literal("Other Guess Color")
        this.description = literal("Pick a color for other guesses.")
        this.allowAlpha = false
    }

    var SubGuessColor by color(
        Color(0.55f, 0.55f, 0.55f).rgb
    ) {
        this.name = literal("Sub Guess Color")
        depends(Burrows::showArrowSubGuesses)
        this.description = literal("Color of inactive arrow guess locations.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Burrow Colors"
        }
    }

    var StartColor by color(
        Color(0.333f, 1.0f, 0.333f).rgb) {
        this.name = literal("Start Burrow Color")
        this.description = literal("Pick a color for start burrows.")
        this.allowAlpha = false
    }

    var MobColor by color(
        Color(1.0f, 0.333f, 0.333f).rgb) {
        this.name = literal("Mob Burrow Color")
        this.description = literal("Pick a color for mob burrows.")
        this.allowAlpha = false
    }

    var TreasureColor by color(
        Color(1f, 0.666f, 0.0f).rgb) {
        this.name = literal("Treasure Burrow Color")
        this.description = literal("Pick a color for treasure burrows.")
        this.allowAlpha = false
    }

    init {
        sub {
            this.title = "Waypoint Look"
        }
    }

    var lookAlike by boolean(false) {
        this.name = literal("Look Alike")
        this.description = literal("Mimics visuals of SkyHanni on the block outline opacity and guess/distance text. This can help if you are migrating between the mods and can't get used to the look of another.")
    }

    var dynamicWaypointOpacity by boolean(true) {
        this.name = literal("Dynamic Waypoint Opacity")
        this.description = literal("Uses a dynamic waypoint opacity that changes based on how far or close you are to the waypoints. If you enable this, the Waypoint Opacity setting below will not take effect.")
    }

    var waypointOpacity by int(50) {
        this.range = 0..100
        this.name = literal("Waypoint Opacity")
        dependsOff(::dynamicWaypointOpacity)
        this.description = literal("The opacity of the rendered waypoints. 50 will make it 50% transparent, 100% fully opaque, 0% fully invisible, etc. (default 50)")
    }

    var waypointTextOpacity by int(100) {
        this.range = 0..100
        this.name = literal("Waypoint Text Opacity")
        this.description = literal("The opacity of the text on the rendered waypoints. 50 will make it 50% transparent, 100% fully opaque, 0% fully invisible, etc. (default 100)")
    }

    var waypointTextShadow by boolean(true) {
        this.name = literal("Waypoint Text Shadow")
        this.description = literal("Enables shadow for waypoint text.")
    }

    var waypointTextScale by float(0.7f) {
        this.name = literal("Waypoint Text Scale")
        this.description = literal("Scale of the waypoint text.")
        this.range = 0.3f..2.0f
        this.slider = true
    }

    var showTimesDug by boolean(true) {
        this.name = literal("Show Times Dug")
        this.description = literal("Shows times dug on the waypoint text for known burrows.")
    }

    var showDistanceCutoff by int(50) {
        this.range = 0..150
        this.name = literal("Show Distance & Times Dug Cutoff")
        depends(::showTimesDug)
        this.description = literal("The distance cutoff at which the distance text disappears, and the times dug text appears. For example if set to 50 will not display the distance text if 50m or closer, and will only display the times dug when 50m or closer. (0 to always show distance and times dug, if times dug is enabled)")
    }

    init {
        sub {
            this.title = "Other Waypoints"
        }
    }

    var patcherWaypoints by boolean(true) {
        this.name = literal("Waypoints From Chat")
        this.description = literal("Creates waypoints from chat messages. (format: x: 20, y: 60, z: 80)")
    }

    var RareMobColor by color(
        Color(1.0f, 0.84f, 0.0f).rgb) {
        this.name = literal("Rare Mob Waypoint Color")
        this.description = literal("Pick a color for rare mob waypoints.")
        this.allowAlpha = false
    }

    var OtherWaypointColor by color(
        Color(0.0f, 0.2f, 1.0f).rgb) {
        this.name = literal("Other Waypoint Color")
        this.description = literal("Pick a color for other waypoints, e.g., waypoints created from non-SBO player-sent coordinates. (Separate from rare mob color)")
        this.allowAlpha = false
    }

    var hideOwnCoordinateWaypoints by boolean(false) {
        this.name = literal("Hide Own Coordinate Waypoints")
        this.since = "0.7.2-beta.2"
        this.description = literal("Hides the waypoints from coordinates you sent in chat yourself.")
    }

    var hideOwnWaypoints by options<RareMob> {
        this.name = literal("Hide Own Rare Mob Waypoints")
        this.description = literal("Hides the waypoints of these rare mobs when you spawned them.")
    }
}
