package net.sbo.mod.guis

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h1
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.section
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.GuiLibScreen
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.config.ConfigGui
import net.sbo.mod.general.SboBadges
import net.sbo.mod.utils.data.cloud.CloudSync
import net.sbo.mod.general.HelpCommand
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useCommandsFocus
import net.sbo.mod.guis.look.useFitScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.settings.Settings
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.overlay.OverlayEditScreen
import org.lwjgl.glfw.GLFW
import java.util.Collections
import java.util.WeakHashMap

object HubGui {
    private val STYLES = listOf("sbo:ui/hub/hub.css", SboLook.STYLE)

    fun open() {
        GuiLib.open(App, STYLES, title = "Skyblock Overhaul")
    }

    fun openSettings() {
        ConfigGui.open(Settings, "SBO Settings", onBack = ::open)
    }

    fun settingsScreen(): GuiLibScreen = ConfigGui.screen(Settings, "SBO Settings", onBack = ::open)

    private class Tile(
        val title: String,
        val command: String?,
        val icon: Item,
        val escBackToHub: Boolean = true,
        val supporter: Supporter? = null,
        val open: () -> Unit,
    )

    private class Supporter(val who: String, val unlocked: () -> Boolean?)

    private val escBackToHub: MutableSet<Screen> = Collections.newSetFromMap(WeakHashMap())

    fun register() {
        // Fabric drops a screen's listeners when it initializes again (e.g. on resize)
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen in escBackToHub) listenForEsc(screen)
        }
    }

    private fun openFromHub(tile: Tile) {
        val hub = GuiLib.currentScreen()
        tile.open()
        if (!tile.escBackToHub) return
        // Some tiles open their screen one task later
        mc.schedule {
            val screen = GuiLib.currentScreen()
            if (screen != null && screen !== hub && escBackToHub.add(screen)) listenForEsc(screen)
        }
    }

    // Esc that only closes a modal or leaves an input keeps the screen open, so only a closed screen goes back
    private fun listenForEsc(screen: Screen) {
        ScreenKeyboardEvents.allowKeyPress(screen).register { _, key ->
            if (key.key == GLFW.GLFW_KEY_ESCAPE) mc.schedule { if (GuiLib.currentScreen() == null) open() }
            true
        }
    }

    private class Link(val title: String, val description: String, val url: String)

    private val TILES = listOf(
        Tile("Settings", "/sbosettings", Items.COMPARATOR) { openSettings() },
        Tile("Party Finder", "/sbopf", Items.PLAYER_HEAD) { PartyFinderGui.open() },
        Tile("Events", "/sboevents", Items.CLOCK) { EventsGui.open() },
        Tile("Achievements", "/sboachievements", Items.NETHER_STAR) { AchievementsGui.open() },
        Tile("Sounds", "/sbosounds", Items.JUKEBOX) { SoundsGui.open() },
        Tile("Move Overlays", "/sboguis", Items.ITEM_FRAME) { mc.setScreen(OverlayEditScreen()) },
        Tile(
            "Cloud Sync", "/sbocloud", Items.ENDER_CHEST,
            supporter = Supporter("Patreon and Ko-fi supporters and server boosters") {
                if (CloudSync.premium() == true || SboBadges.hasAccess() == true) true else CloudSync.premium()
            },
        ) { CloudSyncGui.open() },
        Tile(
            "SBO Badge", "/sbobadge", Items.NAME_TAG,
            supporter = Supporter("Patreon and Ko-fi supporters, maintainers and moderators") { SboBadges.hasAccess() },
        ) { BadgeGui.open() },
    )

    private val COMMANDS = Tile("Commands", "/sbohelp", Items.COMMAND_BLOCK) {}

    private val SUPPORT = listOf(
        Link("Patreon", "Support our development and keep the server running", "https://www.patreon.com/Skyblock_Overhaul"),
        Link("Ko-fi", "Buy us a coffee", "https://ko-fi.com/skyblock_overhaul"),
    )

    private val COMMUNITY = listOf(
        Link("Discord", "Support, updates and parties", "https://discord.gg/QvM6b9jsJD"),
        Link("GitHub", "Releases and source code", "https://github.com/SkyblockOverhaul/SBO/releases"),
        Link("Website", "Read about us and our Projects", "https://skyblockoverhaul.com/"),
    )

    private val CREDITS = listOf(
        Link("SkyHanni", "Spade Guess (bloxigus) and Arrow Guess (SidOfThe7Cs)", "https://github.com/hannibal002/SkyHanni"),
        Link("GuiLib", "UI library for these screens (SkyblockOverhaul)", "https://skyblockoverhaul.github.io/maven/guilib/"),
        Link("hm-api", "Hypixel Mod API wrapper (AzureAaron)", "https://github.com/AzureAaron/hm-api"),
        Link("RenderChest", "Glow API (AzureAaron)", "https://github.com/AzureAaron/RenderChest"),
        Link("Fabric API", "Modding API (FabricMC)", "https://github.com/FabricMC/fabric-api"),
        Link("Stat Icons", "From the Hypixel SkyBlock Resource Pack by Hypixel Inc., via the Hypixel SkyBlock Wiki", "https://github.com/skyblock-wiki/wiki-assets"),
        Link("NEU Repo", "Pet heads, pet lore and level tables (NotEnoughUpdates, MIT)", "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO"),
    )

    private var openCommandsOnNextOpen = false

    private val App = component("Hub") {
        var showCommands by useState(openCommandsOnNextOpen)
        openCommandsOnNextOpen = false
        useCommandsFocus(showCommands)
        useSboScale()
        useSboTheme()
        val fit = useFitScale(preferredScale = 1.2f)

        div(className = "hub-window", ref = fit.ref, style = fit.style) {
            div(className = "hub-hero") {
                button(className = "hub-close", title = "Close", onClick = { GuiLib.close() }) { +"×" }
                div(className = "hub-hero-content") {
                    div(className = "hub-emblem") {
                        img("sbo:ui/hub/emblem.png", className = "hub-emblem-image", alt = "SBO")
                    }
                    div(className = "hub-hero-text") {
                        h1(className = "hub-title") { +"Skyblock Overhaul" }
                        div(className = "hub-chips") {
                            span(className = "hub-chip hub-chip-version") { +"SBO ${SBOKotlin.version}" }
                            span(className = "hub-chip") { +"Minecraft ${SBOKotlin.mcVersion}" }
                        }
                    }
                }
            }

            div(className = "hub-body") {
                div(className = "hub-tiles") {
                    fun NodeBuilder.tileButton(tile: Tile, className: String, onClick: () -> Unit) {
                        button(className = className, key = tile.title, onClick = { onClick() }) {
                            div(className = "hub-tile-icon") {
                                // Item icons need a world, Mod Menu can open the screens from the title screen
                                if (mc.level != null) item(ItemStack(tile.icon), decorations = false)
                            }
                            div(className = "hub-tile-text") {
                                div(className = "hub-tile-title") { +tile.title }
                                if (tile.command != null) div(className = "hub-tile-command") { +tile.command }
                            }
                            tile.supporter?.let { supporterChip(it) }
                        }
                    }

                    val (supporterTiles, tiles) = TILES.partition { it.supporter != null }
                    tiles.forEach { tile ->
                        // Screens are switched after the click is handled, not in the middle of it
                        tileButton(tile, "hub-tile") { mc.schedule { openFromHub(tile) } }
                    }
                    tileButton(COMMANDS, "hub-tile") { showCommands = true }
                    supporterTiles.forEach { tile ->
                        tileButton(tile, "hub-tile hub-supporter") { mc.schedule { openFromHub(tile) } }
                    }
                }

                div(className = "hub-columns") {
                    section(className = "hub-card") {
                        h2(className = "hub-card-title") { +"Support SBO" }
                        div(className = "hub-buttons") {
                            SUPPORT.forEach { linkButton(it, "hub-donate") }
                        }
                    }
                    section(className = "hub-card") {
                        h2(className = "hub-card-title") { +"Community" }
                        div(className = "hub-buttons") {
                            COMMUNITY.forEach { linkButton(it, "hub-link") }
                        }
                    }
                    section(className = "hub-card hub-credits") {
                        h2(className = "hub-card-title") { +"Credits" }
                        div(className = "hub-credit-list") {
                            CREDITS.forEach { credit ->
                                button(
                                    className = "hub-credit",
                                    key = credit.title,
                                    title = credit.description,
                                    onClick = { SBOKotlin.openInBrowser(credit.url) },
                                ) { +credit.title }
                            }
                        }
                    }
                }
                div(className = "hub-thanks") {
                    +"Special thanks to all our supporters and contributors, the people who helped testing and gave feedback, "
                    +"and all open source Skyblock mods and libraries we used."
                }

                div(className = "hub-footer") { +"Made by D4rkSwift, RolexDE and contributors" }
            }

            modal(open = showCommands, onClose = { showCommands = false }, className = "hub-commands") {
                div(className = "hub-commands-header") {
                    h2(className = "hub-card-title") { +"Commands" }
                    button(className = "hub-close-small", title = "Close", onClick = { showCommands = false }) { +"×" }
                }
                scroll(className = "hub-commands-list guilib-autohide") {
                    HelpCommand.commands.forEach { command ->
                        div(className = "hub-command", key = command["cmd"]) {
                            div(className = "hub-command-name") { +"/${command["cmd"]}" }
                            div(className = "hub-command-description") { +command["desc"].orEmpty() }
                        }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.supporterChip(supporter: Supporter) {
        val unlocked = supporter.unlocked() == true
        span(
            className = if (unlocked) "hub-premium hub-premium-on" else "hub-premium",
            title = if (unlocked) "You have it, thank you for supporting SBO!" else "Supporter feature: only for ${supporter.who}.",
        ) {
            span(className = "hub-premium-label") { +"Supporter" }
        }
    }

    private fun NodeBuilder.linkButton(link: Link, className: String) {
        button(className = className, key = link.title, title = link.description, onClick = { SBOKotlin.openInBrowser(link.url) }) {
            +link.title
        }
    }

    fun openCommands() {
        openCommandsOnNextOpen = true
        open()
    }
}
