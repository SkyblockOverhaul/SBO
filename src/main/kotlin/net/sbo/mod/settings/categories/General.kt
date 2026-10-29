package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.config.Choice
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.SoundsGui
import net.sbo.mod.utils.overlay.OverlayEditScreen

object General : Category("General") {
    override val description = "Everything that isn't only for Diana: overlays, chat, achievements, sounds and the chunk cache."

    init {
        sub {
            this.title = "Overlays"
        }
    }

    var bobberOverlay by boolean(false) {
        this.name = Literal("Bobber Overlay")
        this.description = Literal("Tracks the number of bobbers near you /sboguis to move the overlay.")
    }

    var legionOverlay by boolean(false) {
        this.name = Literal("Legion Overlay")
        this.description = Literal("Tracks the players near you for legion buff /sboguis to move the overlay.")
    }

    var pickuplogOverlay by boolean(false) {
        this.name = Literal("Pickup Log Overlay")
        this.description = Literal("Displays a pickup log in an overlay like sba. /sboguis to move the overlay.")
    }

    init {
        button {
            title = "Move GUI's"
            text = "Move GUI's"
            description = "Opens Move GUI's Menu, you can also use /sboguis to open it."
            onClick {
                mc.schedule {
                    mc.setScreen(OverlayEditScreen())
                }
            }
        }

        sub {
            this.title = "Chat"
        }
    }

    // Was one Hide ... Messages switch each
    var hideMessages by choices {
        this.name = Literal("Hide Messages")
        this.description = Literal("Chat messages to hide, hover an option for which ones.")
        options(listOf(
            Choice("diana", "Diana spam", "The treasure arrow hint, ability cooldown, Warping... and blocks in the way messages."),
            Choice("autopet", "Autopet", "All autopet messages."),
            Choice("implosion", "Implosion", "All implosion messages."),
            Choice("sacks", "Sacks", "All sack messages."),
        ))
    }

    var assumeMuted by boolean(false) {
        this.name = Literal("Assume Muted")
        this.description = Literal("Assumes you are muted and instead of running /pc to send mod messages always prefers to send them client-side to yourself only.")
    }

    var phoenixAnnouncer by boolean(true) {
        this.name = Literal("Phoenix Announcer")
        this.description = Literal("Announces on screen when you drop a phoenix pet.")
    }

    init {
        sub {
            this.title = "Achievements"
        }
    }

    var disableAchievements by boolean(false) {
        this.name = Literal("Disable Achievements")
        this.description = Literal("Disables the SBO Achievements. Note that, internally your achievements will still be tracked and unlocked, just that you won't be notified in chat, via a title, and a sound, when one unlocks, if this option is enabled.")
    }

    var achievementsButton by boolean(true) {
        this.name = Literal("Achievements Button")
        this.description = Literal("Shows the SBO Achievements button in the pause menu.")
    }

    init {
        sub {
            this.title = "Sounds"
        }

        button {
            title = "Sound Settings"
            text = "Configure"
            description = "Open the sound GUI: sounds, volumes, master volume and the sound folder."
            onClick {
                SoundsGui.open()
            }
        }

        sub {
            this.title = "Chunk Cache"
        }
    }

    var hubChunkCache by boolean(true) {
        this.name = Literal("Chunk Cache")
        this.description = Literal("Keeps chunks after the server unloads them and keeps rendering them up to your render distance, so each chunk only loads once per session and arrow guesses see far away. Only in the areas below. Paused while Bobby is enabled, turn Bobby off to use this instead (starts from your next lobby).")
    }

    var chunkCacheAreas by strings("Hub") {
        this.name = Literal("Chunk Cache Areas")
        depends(::hubChunkCache)
        this.description = Literal("Areas to cache, one per line (or separated by commas). Write the name exactly as it shows after \"Area:\" in the tab list, e.g. Hub, The Park or Dwarven Mines; upper/lower case doesn't matter. Each area needs memory for every chunk you saw, until you disconnect.")
    }

    var chunkCacheWorkEverywhere by boolean(false) {
        this.name = Literal("Chunk Cache Work Everywhere")
        depends(::hubChunkCache)
        this.description = Literal("Caches chunks in every SkyBlock area without listing each area above. Chunk Cache Areas is ineffective while this is enabled, since the cache works everywhere.")
    }

    var chunkCacheHubIslandOnly by boolean(true) {
        this.name = Literal("Cache Hub Island Only")
        depends(::hubChunkCache)
        this.description = Literal("Only limits caching while in the Hub: caches chunks of the Hub island itself, not the islands visible from it (The Park, Spider's Den, The Barn...). Other areas are unaffected and cache all chunks. Less memory and better performance in the Hub, but other islands stop rendering beyond the server's view distance. Within server view distance, they still render.")
    }

    var chunkCacheRenderHubIslandOnly by boolean(false) {
        this.name = Literal("Render Hub Island Only")
        depends(::chunkCacheHubIslandOnly)
        this.description = Literal("Only affects rendering while in the Hub: only renders chunks of the Hub island itself or overlapping chunks with parts connected to Hub. Other areas are unaffected. Will make large parts of other islands such as The Park, Spider's Den, The Barn etc. not render, improving performance. This is an addition on top of Cache Hub Island Only (needs it enabled), making it not render these chunks at all, even if the server sent them and you are in server view distance.")
    }

    var chunkCacheAboveServerOnly by boolean(false) {
        this.name = Literal("Only Above Server Render Distance")
        depends(::hubChunkCache)
        this.description = Literal("Only runs the Chunk Cache while your render distance is higher than the server's (Hypixel: 8). Below that it only helps right after switching lobbies, so this saves a bit of performance.")
    }
}
