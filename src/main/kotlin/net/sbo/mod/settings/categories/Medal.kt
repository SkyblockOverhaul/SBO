package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object Medal : CategoryKt("Medal") {
    init {
        separator {
            this.title = "Medal Clips"
            this.description = "Saves Medal clips on Diana drops. Requires Medal to be running locally."
        }
    }

    var medalEnabled by boolean(false) {
        this.name = Literal("Enable Medal Clips")
        this.description = Literal("Saves Medal clips on selected Diana drops")
    }

    var medalChimeraEnabled by boolean(true) {
        this.name = Literal("Clip Chimera")
        this.description = Literal("Saves a Medal clip when Chimera drops")
    }

    var medalWoolEnabled by boolean(true) {
        this.name = Literal("Clip Shimmering Wool")
        this.description = Literal("Saves a Medal clip when Shimmering Wool drops")
    }

    var medalBrainFoodEnabled by boolean(true) {
        this.name = Literal("Clip Brain Food")
        this.description = Literal("Saves a Medal clip when Brain Food drops")
    }

    var medalMinosRelicEnabled by boolean(true) {
        this.name = Literal("Clip Minos Relic")
        this.description = Literal("Saves a Medal clip when Minos Relic drops")
    }

    var medalMantiCoreEnabled by boolean(true) {
        this.name = Literal("Clip Manti-core")
        this.description = Literal("Saves a Medal clip when Manti-core drops")
    }

    var medalFatefulStingerEnabled by boolean(true) {
        this.name = Literal("Clip Fateful Stinger")
        this.description = Literal("Saves a Medal clip when Fateful Stinger drops")
    }

    var medalDaedalusStickEnabled by boolean(true) {
        this.name = Literal("Clip Daedalus Stick")
        this.description = Literal("Saves a Medal clip when Daedalus Stick drops")
    }

    var medalBraidedGriffinFeatherEnabled by boolean(true) {
        this.name = Literal("Clip Braided Griffin Feather")
        this.description = Literal("Saves a Medal clip when Braided Griffin Feather drops")
    }

    var medalMythologicalDyeEnabled by boolean(true) {
        this.name = Literal("Clip Mythological Dye")
        this.description = Literal("Saves a Medal clip when Mythological Dye drops")
    }

    var medalMythTheFishEnabled by boolean(true) {
        this.name = Literal("Clip Myth the Fish")
        this.description = Literal("Saves a Medal clip when Myth the Fish drops")
    }

    var medalCrownOfGreedEnabled by boolean(true) {
        this.name = Literal("Clip Crown of Greed")
        this.description = Literal("Saves a Medal clip when Crown of Greed drops")
    }

    var medalHiltOfRevelationsEnabled by boolean(true) {
        this.name = Literal("Clip Hilt of Revelations")
        this.description = Literal("Saves a Medal clip when Hilt of Revelations drops")
    }

    var medalWashedUpSouvenirEnabled by boolean(true) {
        this.name = Literal("Clip Washed-up Souvenir")
        this.description = Literal("Saves a Medal clip when Washed-up Souvenir drops")
    }

    var medalDwarfTurtleShelmetEnabled by boolean(true) {
        this.name = Literal("Clip Dwarf Turtle Shelmet")
        this.description = Literal("Saves a Medal clip when Dwarf Turtle Shelmet drops")
    }

    var medalCrochetTigerPlushieEnabled by boolean(true) {
        this.name = Literal("Clip Crochet Tiger Plushie")
        this.description = Literal("Saves a Medal clip when Crochet Tiger Plushie drops")
    }

    var medalAntiqueRemediesEnabled by boolean(true) {
        this.name = Literal("Clip Antique Remedies")
        this.description = Literal("Saves a Medal clip when Antique Remedies drops")
    }

    var medalCretanUrnEnabled by boolean(true) {
        this.name = Literal("Clip Cretan Urn")
        this.description = Literal("Saves a Medal clip when Cretan Urn drops")
    }

    var medalClipDurationSeconds by int(30) {
        this.range = 1..120
        this.slider = true
        this.name = Literal("Clip Duration (seconds)")
        this.description = Literal("How long Medal clips should be")
    }

    var medalCaptureDelayMs by int(10000) {
        this.range = 0..60000
        this.slider = true
        this.name = Literal("Capture Delay (ms)")
        this.description = Literal("Delay before Medal captures clips")
    }
}
