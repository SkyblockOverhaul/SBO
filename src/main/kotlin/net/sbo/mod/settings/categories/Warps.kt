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
        this.name = Literal("Add Warps")
        this.description = Literal("Select the warps you want to be able to warp to with the guess and inquisitor warp keys.")
    }

    var dontWarpIfBurrowClose by boolean(true) {
        this.name = Literal("Don't Warp If Close")
        this.description = Literal("If enabled, the warp key will not warp you if you are already within 60 blocks of a warp target (burrow, guess or rare mob waypoint).")
    }

    var warpDiff by int(22) {
        this.range = 0..60
        this.slider = true
        this.name = Literal("Warp Block Difference")
        this.description = Literal("The additional block difference to consider when warping to a waypoint. (0 to disable)")
    }

    var ignoreYLevel by boolean(false) {
        this.name = Literal("Ignore Y Level for Castle & Wizard")
        this.description = Literal("If enabled, ignores Y-Level when comparing distance between warp and the burrow when determining the closest warp for the Wizard and Castle warps, which have high Y-Level.")
    }

    var badWarpDistance by int(0) {
        this.range = 0..150
        this.name = Literal("Bad Warp Distance")
        this.description = Literal("Blocks at which good warps are prioritized over bad ones (Currently only Castle and Crypt). For example, if you set it to 100, when the Crypt warp is the closest warp to a burrow, and the Castle is second closest, with Crypt warp being only 100 blocks or less closer than Castle, Castle will be preferred over Crypt warp for accessibility, as it can be faster to AOTV out of castle to reach the burrow whereas Crypt is longer to get out. (0 to disable)")
    }

    init {
        sub {
            this.title = "Warp Title"
        }
    }

    var showTitleWhenWarpAvailable by boolean(false) {
        this.name = Literal("Show Title When Warp Is Available")
        this.description = Literal("If enabled, will show a title when warp is available. Wait a few seconds to see if any close burrows appear, and if not, proceed to warp with the key, as the title updates live with new burrows discovered, it might sometimes only show for a split second when a new closer burrow is found making the warp unnecessary after title was shown.")
    }

    var warpTitleAsSubtitle by boolean(false) {
        this.name = Literal("Warp Title As Subtitle")
        depends(::showTitleWhenWarpAvailable)
        this.description = Literal("Shows the warp title as a subtitle instead of title, reducing its size on screen. This will also cause it to move down slightly.")
    }
}
