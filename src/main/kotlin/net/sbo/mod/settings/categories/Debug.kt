package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object Debug : CategoryKt("Debug") {
    var itsAlwaysDiana by boolean(false) {
        this.name = Literal("Always Diana Mayor")
        this.description = Literal("Its always Diana, no need to check for mayor, perks, spade or world")
    }

    var alwaysInSkyblock by boolean(false) {
        this.name = Literal("Always on Skyblock")
        this.description = Literal("Always assume you are on hypixel skyblock")
    }

    var debugMessages by boolean(false) {
        this.name = Literal("Debug Messages")
        this.description = Literal("Enable debug messages for development purposes")
    }

    var repeatableAchie by boolean(true) {
        this.name = Literal("[WIP] Enable Repetable Achievements")
        this.description = Literal("[WIP] allows you to unlock repeatable achievements for each new event")
    }
}