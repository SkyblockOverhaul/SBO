package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.overlay.OverlayEditScreen

object General : CategoryKt("General") {
    enum class HideOwnWaypoints {
        NORMAL, INQ, MANTICORE, KING, SPHINX
    }

    init {
        separator {
            this.title = "Overlays"
        }
    }

    var bobberOverlay by boolean(false) {
        this.name = Literal("Bobber Overlay")
        this.description = Literal("Tracks the number of bobbers near you /sboguis to move the overlay")
    }

    var legionOverlay by boolean(false) {
        this.name = Literal("Legion Overlay")
        this.description = Literal("Tracks the players near you for legion buff /sboguis to move the overlay")
    }

    init {
        button {
            title = "Move GUI's"
            text = "Move GUI's"
            description = "Opens Gui Move Menu you can use /sboguis too"
            onClick {
                mc.schedule {
                    mc.setScreen(OverlayEditScreen())
                }
            }
        }

        separator {
            this.title = "Waypoints"
        }
    }

    var hideOwnWaypoints by select<HideOwnWaypoints> {
        this.name = Literal("Hide Own Waypoints")
        this.description = Literal("Hides waypoints you created")
    }

    var patcherWaypoints by boolean(true) {
        this.name = Literal("Waypoints From Chat")
        this.description = Literal("Creates waypoints from chat messages (format: x: 20, y: 60, z: 80)")
    }

    init {
        separator {
            this.title = "Accessibility"
        }
    }

    var assumeMuted by boolean(false) {
        this.name = Literal("Assume Muted")
        this.description = Literal("Assumes you are muted and instead of running /pc to send mod messages always prefers to send them client-side to yourself only.")
    }

    init {
        separator {
            this.title = "Achievements"
        }
    }

    var achievementsButton by boolean(true) {
        this.name = Literal("Achievements Button")
        this.description = Literal("Shows the SBO Achievements button in the pause menu")
    }

    init {
        separator {
            this.title = "Chunk Cache"
        }
    }

    var hubChunkCache by boolean(true) {
        this.name = Literal("Hub Chunk Cache")
        this.description = Literal("Caches unloaded chunks in Hub for accurate arrow guesses. Disable if using Bobby or other chunk caching mods.")
    }
}
