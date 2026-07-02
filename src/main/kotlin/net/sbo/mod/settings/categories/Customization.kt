package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import gg.essential.universal.UDesktop
import net.fabricmc.loader.api.FabricLoader
import java.awt.Color
import java.io.File

object Customization : CategoryKt("Customization") {
    init {
        separator {
            this.title = "Waypoint Customization"
        }
    }

    var ClosestGuessColor by color(
        Color(0.6f, 0.2f, 0.8f).rgb) {
        this.name = Literal("Closest Guess Color")
        this.description = Literal("Pick a color for closest guess")
        this.allowAlpha = true
    }

    var OtherGuessColor by color(
        Color(0.0f, 0.964f, 1.0f).rgb) {
        this.name = Literal("Other Guess Color")
        this.description = Literal("Pick a color for other guesses")
        this.allowAlpha = true
    }

    var StartColor by color(
        Color(0.333f, 1.0f, 0.333f).rgb) {
        this.name = Literal("Start Burrow Color")
        this.description = Literal("Pick a color for start burrows")
        this.allowAlpha = true
    }

    var MobColor by color(
        Color(1.0f, 0.333f, 0.333f).rgb) {
        this.name = Literal("Mob Burrow Color")
        this.description = Literal("Pick a color for mob burrows")
        this.allowAlpha = true
    }

    var TreasureColor by color(
        Color(1f, 0.666f, 0.0f).rgb) {
        this.name = Literal("Treasure Burrow Color")
        this.description = Literal("Pick a color for treasure burrows")
        this.allowAlpha = true
    }

    var RareMobColor by color(
        Color(1.0f, 0.84f, 0.0f).rgb) {
        this.name = Literal("Rare Mob Waypoint Color")
        this.description = Literal("Pick a color for rare mob waypoints")
        this.allowAlpha = true
    }

    var OtherWaypointColor by color(
        Color(0.0f, 0.2f, 1.0f).rgb) {
        this.name = Literal("Other Waypoint Color")
        this.description = Literal("Pick a color for other waypoints, e.g., waypoints created from non-SBO player-sent coordinates (Separate from rare mob color).")
        this.allowAlpha = true
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

    init {
        separator {
            this.title = "Waypoint Text Customization"
        }
    }

    var waypointTextShadow by boolean(true) {
        this.name = Literal("Waypoint Text Shadow")
        this.description = Literal("Enables shadow for waypoint text")
    }

    var waypointTextScale by float(0.7f) {
        this.name = Literal("Waypoint Text Scale")
        this.description = Literal("Scale of the waypoint text")
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
            text = "open"
            description = "Custom sounds go in here (sound must be a .ogg). You need to restart minecraft after adding a sound"
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
    }

    var rareMobSound by strings("exporb") {
        this.name = Literal("Rare Spawn Sound")
        this.description = Literal("Set the sound that plays when an rare mob spawns. (enter filename)")
    }
    var rareMobVolume by float(1.0f) {
        this.name = Literal("Rare Spawn Volume")
        this.description = Literal("Set the volume of the rare mob spawn sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var inqSound by strings("") {
        this.name = Literal("Inq Sound")
        this.description = Literal("Set the sound that plays when an Inquisitor Spawn. (enter filename)")
    }
    var inqVolume by float(1.0f) {
        this.name = Literal("Inq volume Volume")
        this.description = Literal("Set the volume of the Inquisitor spawn sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var sphinxSound by strings("") {
        this.name = Literal("Sphinx Sound")
        this.description = Literal("Set the sound that plays when a Sphinx spawns. (enter filename)")
    }
    var sphinxVolume by float(1.0f) {
        this.name = Literal("Sphinx Volume")
        this.description = Literal("Set the volume of the Sphinx spawn sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var kingSound by strings("") {
        this.name = Literal("King Minos Sound")
        this.description = Literal("Set the sound that plays when King Minos spawns. (enter filename)")
    }
    var kingVolume by float(1.0f) {
        this.name = Literal("King Minos Volume")
        this.description = Literal("Set the volume of the King Minos spawn sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var mantiSound by strings("") {
        this.name = Literal("Manticores Sound")
        this.description = Literal("Set the sound that plays when a Manticore spawns. (enter filename)")
    }
    var mantiVolume by float(1.0f) {
        this.name = Literal("Manticores Volume")
        this.description = Literal("Set the volume of the Manticore spawn sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var cocoonSound by strings("") {
        this.name = Literal("Cocoon Sound")
        this.description = Literal("Set the sound that plays when a rare mob gets cocooned (enter filename)")
    }
    var cocoonVolume by float(1.0f) {
        this.name = Literal("Cocoon Volume")
        this.description = Literal("Set the volume of the cocooning sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var burrowFoundSound by strings("notification") {
        this.name = Literal("Burrow Found Sound")
        this.description = Literal("Set the sound that plays when you find a burrow. (enter filename)")
    }
    var burrowVolume by float(1.0f) {
        this.name = Literal("Burrow Found Volume")
        this.description = Literal("Set the volume of the burrow found sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var chimSound by strings("") {
        this.name = Literal("Chimera Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a chimera book. (enter filename)")
    }
    var chimVolume by float(1.0f) {
        this.name = Literal("Chimera Drop Volume")
        this.description = Literal("Set the volume of the chimera drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var bfSound by strings("") {
        this.name = Literal("Brain Food Drop Sound")
        this.description = Literal("Set the sound that plays when you drop Brain Food. (enter filename)")
    }

    var bfVolume by float(1.0f) {
        this.name = Literal("Brain Food Drop Volume")
        this.description = Literal("Set the volume of the Brain Food drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var coreSound by strings("") {
        this.name = Literal("Manti-Core Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a Manti-Core. (enter filename)")
    }

    var coreVolume by float(1.0f) {
        this.name = Literal("Manti-Core Drop Volume")
        this.description = Literal("Set the volume of the Manti-Core drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var stingerSound by strings("") {
        this.name = Literal("Fateful Stinger Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a Fateful Stinger. (enter filename)")
    }

    var stingerVolume by float(1.0f) {
        this.name = Literal("Fateful Stinger Drop Volume")
        this.description = Literal("Set the volume of the Fateful Stinger drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var woolSound by strings("") {
        this.name = Literal("Shimmering Wool Drop Sound")
        this.description = Literal("Set the sound that plays when you drop Shimmering Wool. (enter filename)")
    }

    var woolVolume by float(1.0f) {
        this.name = Literal("Shimmering Wool Drop Volume")
        this.description = Literal("Set the volume of the Shimmering Wool drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var relicSound by strings("") {
        this.name = Literal("Relic Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a minos relic. (enter filename)")
    }
    var relicVolume by float(1.0f) {
        this.name = Literal("Relic Drop Volume")
        this.description = Literal("Set the volume of the relic drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var stickSound by strings("") {
        this.name = Literal("Daedalus Stick Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a daedalus stick.")
    }
    var stickVolume by float(1.0f) {
        this.name = Literal("Daedalus Stick Drop Volume")
        this.description = Literal("Set the volume of the daedalus stick drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    var miscDropSound by strings("") {
        this.name = Literal("Misc Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a crown of greed, washed-up souvenir, dwarf turtle shelmet, crochet tiger plushie, antique remedies, cretan urn or hilt of revelations. (enter filename)")
    }
    var miscDropVolume by float(1.0f) {
        this.name = Literal("Misc Drop Volume")
        this.description = Literal("Set the volume of the misc drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }
}
