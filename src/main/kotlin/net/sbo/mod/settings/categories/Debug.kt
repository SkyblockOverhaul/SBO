package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object Debug : CategoryKt("Debug") {
    var alwaysInSkyblock by boolean(false) {
        this.name = Literal("Always on Skyblock")
        this.description = Literal("Always assume you are on hypixel skyblock.")
    }

    var debugOnlyMessages by boolean(false) {
        this.name = Literal("Debug Only Messages")
        this.description = Literal("Enable debug only messages for development purposes. Do not enable unless you are instructed to do so.")
    }

    var repeatableAchie by boolean(true) {
        this.name = Literal("Enable Repeatable Achievements")
        this.description = Literal("Allows you to unlock repeatable achievements for each new event.")
    }
}
