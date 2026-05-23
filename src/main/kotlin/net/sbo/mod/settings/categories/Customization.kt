package net.sbo.mod.settings.categories

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.SboDataObject
import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import gg.essential.universal.UDesktop
import net.fabricmc.loader.api.FabricLoader
import java.awt.Color
import java.io.File

object Customization : CategoryKt("Customization") {
    val ALL_SOUNDS_FILENAMES: List<String> = try {
        val directory = File(File(FabricLoader.getInstance().configDir.toFile(), SboDataObject.dataDir), "sounds")
        directory.mkdirs()

        if (directory.isDirectory) {
            directory.listFiles { file -> file.extension == "ogg" }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                ?: emptyList()
        } else {
            SBOKotlin.logger.warn("Sounds directory was a file, expected directory at: $directory")
            SBOKotlin.logger.warn("You should delete and re-create it as a directory and put the sounds under that directory.")
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    init {
        separator {
            this.title = "Waypoint Customization"
        }
    }

    var guessColor by color(
        Color(0.0f, 0.964f, 1.0f).rgb) {
        this.name = Literal("Guess Color")
        this.description = Literal("Pick a color for your guess")
        this.allowAlpha = true
    }

    var StartColor by color(
        Color(0.333f, 1.0f, 0.333f).rgb) {
        this.name = Literal("Start Burrow Color")
        this.description = Literal("Pick a color for start burrows")
        this.allowAlpha = true
    }
    
    var closestColor by color(
        Color(0.6f, 0.2f, 0.8f).rgb) {
        this.name = Literal("Closest Color")
        this.description = Literal("Pick a color for your closest burrow")
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

    var burrowSound by strings("") {
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

    var sprSound by strings("") {
        this.name = Literal("Shelmet/Plushie/Remedies Drop Sound")
        this.description = Literal("Set the sound that plays when you drop a Shelmet/Plushie/Remedies. (enter filename)")
    }
    var sprVolume by float(1.0f) {
        this.name = Literal("Shelmet/Plushie/Remedies Drop Volume")
        this.description = Literal("Set the volume of the Shelmet/Plushie/Remedies drop sound")
        this.range = 0.0f..1.0f
        this.slider = true
    }

    fun resetSoundCustomizationToDefaults() {
        Customization.rareMobSound = arrayOf("exporb")
        Customization.rareMobVolume = 1.0f

        Customization.inqSound = arrayOf("")
        Customization.inqVolume = 1.0f

        Customization.sphinxSound = arrayOf("")
        Customization.sphinxVolume = 1.0f

        Customization.kingSound = arrayOf("")
        Customization.kingVolume = 1.0f

        Customization.mantiSound = arrayOf("")
        Customization.mantiVolume = 1.0f

        Customization.cocoonSound = arrayOf("")
        Customization.cocoonVolume = 1.0f

        Customization.burrowSound = arrayOf("")
        Customization.burrowVolume = 1.0f

        Customization.chimSound = arrayOf("")
        Customization.chimVolume = 1.0f

        Customization.bfSound = arrayOf("")
        Customization.bfVolume = 1.0f

        Customization.coreSound = arrayOf("")
        Customization.coreVolume = 1.0f

        Customization.stingerSound = arrayOf("")
        Customization.stingerVolume = 1.0f

        Customization.woolSound = arrayOf("")
        Customization.woolVolume = 1.0f

        Customization.relicSound = arrayOf("")
        Customization.relicVolume = 1.0f

        Customization.stickSound = arrayOf("")
        Customization.stickVolume = 1.0f

        Customization.sprSound = arrayOf("")
        Customization.sprVolume = 1.0f
    }
}
