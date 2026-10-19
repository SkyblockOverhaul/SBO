package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category

object Debug : Category("Debug") {
    var alwaysInSkyblock by boolean(false) {
        this.name = Literal("Always on Skyblock")
        this.description = Literal("Always assumes you are on Hypixel and in Skyblock. §cDo not enable unless the detection does not work. §rDefault disabled.")
    }

    var debugOnlyMessages by boolean(false) {
        this.name = Literal("Debug Only Messages")
        this.description = Literal("Enable debug only messages for development purposes. §cDo not enable unless you are instructed to do so. §rDefault disabled.")
    }

    var showInternalStateWaypoints by boolean(false) {
        this.name = Literal("Show Internal State Waypoints")
        this.description = Literal("Show debug waypoints for burrows and arrow guesses that do not have a regular waypoint. §cDo not enable unless you are fine with debug waypoints. §rDefault disabled.")
    }

    var disableAutoTweaks by boolean(false) {
        this.name = Literal("Disable Auto Tweaks")
        this.description = Literal("Disables automatic tweaks the mod makes for you to have a better experience. §cDo not enable this unless you know what you are doing. §rThis for example will disable automatic pq extreme switch for Diana solver quality. Default disabled meaning automatic tweaks will be done.")
    }

    var repeatableAchie by boolean(true) {
        this.name = Literal("Enable Repeatable Achievements")
        this.description = Literal("Allows you to unlock repeatable achievements for each new event. Default enabled.")
    }

    var lootshareDisplayTest by boolean(false) {
        this.name = Literal("Lootshare Display Test")
        this.description = Literal("Shows the lootshare display without a party and on rare mobs you spawned yourself, for testing.")
    }
}
