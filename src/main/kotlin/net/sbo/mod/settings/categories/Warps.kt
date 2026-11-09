package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.utils.waypoint.AdditionalHubWarps

object Warps : Category("Warps") {
    override val description = "The guess and rare mob warp keys. Set the keys in (ESC) -> Options -> Controls -> Key Binds..., under SBO - Keybinds."

    init {
        sub {
            this.title = "Warp Targets"
        }
    }

    var allowedWarps by options(AdditionalHubWarps.WIZARD, AdditionalHubWarps.DA, AdditionalHubWarps.CASTLE) {
        this.name = literal("Add Warps")
        this.description = literal("Select the warps you want to be able to warp to with the guess and inquisitor warp keys.")
    }

    var dontWarpIfBurrowClose by boolean(true) {
        this.name = literal("Don't Warp If Close")
        this.description = literal("If enabled, the warp key will not warp you if you are already within 60 blocks of a warp target (burrow, guess or rare mob waypoint).")
    }

    var warpDiff by int(22) {
        this.range = 0..60
        this.slider = true
        this.name = literal("Warp Block Difference")
        this.description = literal("The additional block difference to consider when warping to a waypoint. (0 to disable)")
    }

    var ignoreYLevel by boolean(false) {
        this.name = literal("Ignore Y Level for Castle & Wizard")
        this.description = literal("If enabled, ignores Y-Level when comparing distance between warp and the burrow when determining the closest warp for the Wizard and Castle warps, which have high Y-Level.")
    }

    var badWarpDistance by int(0) {
        this.range = 0..150
        this.name = literal("Bad Warp Distance")
        this.description = literal("Blocks at which good warps are prioritized over bad ones (0 to disable). For example if you set it to 100 and Crypt is the closest burrow while Castle is the second closest, and Crypt is less than 100 blocks closer than Castle, then Castle will be chosen instead. This is because it's faster to AOTV out of Castle to reach that burrow due to the time needed to exit the Crypt area.")
    }

    init {
        sub {
            this.title = "Warp Title"
        }
    }

    var showTitleWhenWarpAvailable by boolean(false) {
        this.name = literal("Show Title When Warp Is Available")
        this.description = literal("If enabled, will show a title when warp is available. Wait a few seconds to see if any close burrows appear, and if not, proceed to warp with the key, as the title updates live with new burrows discovered, it might sometimes only show for a split second when a new closer burrow is found making the warp unnecessary after title was shown.")
    }

    var warpTitleAsSubtitle by boolean(false) {
        this.name = literal("Warp Title As Subtitle")
        depends(::showTitleWhenWarpAvailable)
        this.description = literal("Shows the warp title as a subtitle instead of title, reducing its size on screen. This will also cause it to move down slightly.")
    }
}
