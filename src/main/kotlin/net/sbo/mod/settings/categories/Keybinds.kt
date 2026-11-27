package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category

object Keybinds : Category("Keybinds") {
    override val description = "Every SBO key. Click a key and press the new one (or a mouse button), Escape unbinds it. They used to be in Minecraft's Controls, your keys from there were taken over."

    init {
        sub {
            this.title = "Diana"
        }
    }

    var guessWarpKey by keybind {
        this.name = literal("Guess Warp")
        this.description = literal("Warps to the closest warp to your guess or burrow, see the Warps category for which warps.")
    }

    var rareMobWarpKey by keybind {
        this.name = literal("Rare Mob Warp")
        this.description = literal("Warps to the closest warp to the last shared rare mob.")
    }

    var generalWarpKey by keybind {
        this.name = literal("General Warp")
        this.description = literal("Warps to the rare mob if there is one, otherwise to the guess.")
    }

    var sendCoordsKey by keybind {
        this.name = literal("Send Coordinates")
        this.description = literal("Sends your coordinates in the current chat.")
    }

    var sphinxSolverKey by keybind {
        this.name = literal("Sphinx Answer ⚠")
        this.description = literal("Clicks the correct sphinx answer. §c⚠ USE AT YOUR OWN RISK ⚠")
        this.warning = "The key clicks the sphinx answer for you. Hypixel could see that as a macro."
    }

    var clearRareMobWorldWaypointsKey by keybind {
        this.name = literal("Clear Rare Mob and World Waypoints")
        this.description = literal("Removes every rare mob and world waypoint.")
    }

    var dianaGuideKey by keybind {
        this.name = literal("Diana Guide")
        this.since = "0.8.1-beta1"
        this.description = literal("Opens the Diana guide (/sboguide) with your gear next to the guide, pet rules and tips.")
    }

    init {
        sub {
            this.title = "Loadouts"
        }
    }

    var loadoutKeybinds by boolean(true) {
        this.name = literal("Loadout Keybinds")
        this.since = "0.8.0"
        this.description = literal("In the /loadouts menu, the keys below equip the loadouts on the current page and change the page. Locked and not yet customized loadouts are skipped. Only work in that menu, so they can share keys with the hotbar.")
    }

    var loadoutNextPageKey by keybind("key.keyboard.right") {
        this.name = literal("Next Page")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadoutPreviousPageKey by keybind("key.keyboard.left") {
        this.name = literal("Previous Page")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout1 by keybind("key.keyboard.1") {
        this.name = literal("Loadout 1")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout2 by keybind("key.keyboard.2") {
        this.name = literal("Loadout 2")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout3 by keybind("key.keyboard.3") {
        this.name = literal("Loadout 3")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout4 by keybind("key.keyboard.4") {
        this.name = literal("Loadout 4")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout5 by keybind("key.keyboard.5") {
        this.name = literal("Loadout 5")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout6 by keybind("key.keyboard.6") {
        this.name = literal("Loadout 6")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout7 by keybind("key.keyboard.7") {
        this.name = literal("Loadout 7")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout8 by keybind("key.keyboard.8") {
        this.name = literal("Loadout 8")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout9 by keybind("key.keyboard.9") {
        this.name = literal("Loadout 9")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout10 by keybind("key.keyboard.0") {
        this.name = literal("Loadout 10")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout11 by keybind("key.keyboard.minus") {
        this.name = literal("Loadout 11")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    var loadout12 by keybind("key.keyboard.equal") {
        this.name = literal("Loadout 12")
        depends(::loadoutKeybinds)
        this.menuOnly = true
    }

    /** The loadout keys of a page, top left to bottom right. */
    val loadoutKeys: List<String>
        get() = listOf(loadout1, loadout2, loadout3, loadout4, loadout5, loadout6, loadout7, loadout8, loadout9, loadout10, loadout11, loadout12)
}
