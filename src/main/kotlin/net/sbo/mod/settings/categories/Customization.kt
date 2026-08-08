package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import gg.essential.universal.UDesktop
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.guis.Guis
import java.awt.Color
import java.io.File

object Customization : CategoryKt("Customization") {
    init {
        separator {
            this.title = "Guess Color Customization"
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
        this.description = Literal("Color of inactive arrow guess locations. (only used if \"Show Arrow Sub Guesses\" enabled in Diana category)")
        this.allowAlpha = false
    }

    var OptimalOrderLineColor by color(
        Color(1.0f, 1.0f, 1.0f).rgb) {
        this.name = Literal("Optimal Order Line Color")
        this.description = Literal("Pick a color for optimal order line color. (line drawn between guesses in the optimal order to them, only used if \"Draw Optimal Order Lines\" enabled in Diana category)")
        this.allowAlpha = false
    }

    init {
        separator {
            this.title = "Burrow Color Customization"
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
        separator {
            this.title = "Waypoint Color Customization"
        }
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

    init {
        separator {
            this.title = "Glow Color Customization"
        }
    }

    var KingMinosGlowColor by color(
        Color(1.0f, 0.55f, 0.0f).rgb
    ) {
        this.name = Literal("King Minos Glow Color")
        this.description = Literal("Glow color for King Minos.")
        this.allowAlpha = true
    }

    var MinosInquisitorGlowColor by color(
        Color(1.0f, 0.35f, 0.75f).rgb
    ) {
        this.name = Literal("Minos Inquisitor Glow Color")
        this.description = Literal("Glow color for Minos Inquisitor.")
        this.allowAlpha = true
    }

    var ManticoreGlowColor by color(
        Color(0.0f, 0.45f, 0.0f).rgb
    ) {
        this.name = Literal("Manticore Glow Color")
        this.description = Literal("Glow color for Manticore.")
        this.allowAlpha = true
    }

    var SphinxGlowColor by color(
        Color(0.35f, 0.8f, 1.0f).rgb
    ) {
        this.name = Literal("Sphinx Glow Color")
        this.description = Literal("Glow color for Sphinx.")
        this.allowAlpha = true
    }

    init {
        separator {
            this.title = "Waypoint Customization"
        }
    }

    var dynamicWaypointOpacity by boolean(true) {
        this.name = Literal("Dynamic Waypoint Opacity")
        this.description = Literal("Uses a dynamic waypoint opacity that changes based on how far or close you are to the waypoints. If you enable this, the Waypoint Opacity setting below will not take effect.")
    }

    var waypointOpacity by int(50) {
        this.range = 0..100
        this.name = Literal("Waypoint Opacity")
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

    var showDistanceCutoff by int(50) {
        this.range = 0..150
        this.name = Literal("Show Distance & Times Dug Cutoff")
        this.description = Literal("The distance cutoff at which the distance text disappears, and the times dug text appears. For example if set to 50 will not display the distance text if 50m or closer, and will only display the times dug when 50m or closer. (0 to always show distance and times dug, if times dug is enabled)")
    }

    var showTimesDug by boolean(true) {
        this.name = Literal("Show Times Dug")
        this.description = Literal("Shows times dug on the waypoint text for known burrows.")
    }

    init {
        separator {
            this.title = "Warp Title Customization"
        }
    }

    var warpTitleAsSubtitle by boolean(false) {
        this.name = Literal("Warp Title As Subtitle")
        this.description = Literal("Shows the warp title as a subtitle instead of title, reducing its size on screen. This will also cause it to move down slightly.")
    }

    init {
        separator {
            this.title = "Sounds"
        }

        button {
            title = "Open Sound Folder"
            text = "Open"
            description = "Custom sounds go in here. (Must be one of those extensions: .ogg, .mp3, .wav, .au, .aif, .aiff)"
            onClick {
                val path = "${FabricLoader.getInstance().configDir}/sbo/sounds"
                val directory = File(path)
                if (directory.exists()) {
                    try {
                        UDesktop.open(directory)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    println("Directory not found: $path")
                }
            }
        }

        button {
            title = "Sound Settings"
            text = "Configure"
            description = "Open GUI to configure all sound settings."
            onClick {
                Guis.openSoundGui(calledFromGUI = true)
            }
        }
    }

    var masterVolume by float(1.0f) {
        this.name = Literal("Master Volume")
        this.description = Literal("Set the volume for all sounds.")
        this.range = 0.0f..1.0f
        this.slider = true
    }
}
