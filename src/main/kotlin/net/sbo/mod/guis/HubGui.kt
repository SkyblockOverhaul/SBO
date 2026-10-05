package net.sbo.mod.guis

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.Ref
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.ComponentScope
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h1
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import kotlin.math.abs
import net.sbo.guilib.core.dsl.section
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.GuiLibScreen
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.cloud.gui.CloudSyncGui
import net.sbo.mod.config.Category
import net.sbo.mod.config.ConfigGui
import net.sbo.mod.general.HelpCommand
import net.sbo.mod.settings.Settings
import net.sbo.mod.utils.overlay.OverlayEditScreen
import org.lwjgl.glfw.GLFW
import java.util.Collections
import java.util.WeakHashMap

object HubGui {
    private val STYLES = listOf("sbo:ui/hub/hub.css")

    fun open() {
        GuiLib.open(App, STYLES, title = "Skyblock Overhaul")
    }

    fun openSettings() {
        ConfigGui.open(Settings, "SBO Settings", onBack = ::open)
    }

    fun settingsScreen(): GuiLibScreen = ConfigGui.screen(Settings, "SBO Settings", onBack = ::open)

    private class Tile(
        val title: String,
        val description: String,
        val command: String?,
        val icon: Item,
        val escBackToHub: Boolean = true,
        val open: () -> Unit,
    )

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
        Tile("Settings", "Every option, with a search over all of them", "/sbosettings", Items.COMPARATOR) { openSettings() },
        Tile("Party Finder", "Join a party in seconds or create your own", "/sbopf", Items.PLAYER_HEAD, escBackToHub = false) { Guis.openSboPf(calledFromGUI = true) },
        Tile("Events", "Your Diana events, trackers and comparisons", "/sboevents", Items.CLOCK) { EventsGui.open() },
        Tile("Achievements", "Everything you unlocked and what is left", "/sboachievements", Items.NETHER_STAR) { AchievementsGui.open() },
        Tile("Sounds", "Your own sounds for spawns and drops", "/sbosounds", Items.JUKEBOX) { Guis.openSoundGui(calledFromGUI = true) },
        Tile("Cloud Sync", "Keep your settings and trackers on every PC", "/sbocloud", Items.ENDER_CHEST) { CloudSyncGui.open() },
        Tile("Move Overlays", "Place and resize the on-screen overlays", "/sboguis", Items.ITEM_FRAME) { mc.setScreen(OverlayEditScreen()) },
    )

    private val SUPPORT = listOf(
        Link("Patreon", "Support our development and keep the server running", "https://www.patreon.com/Skyblock_Overhaul"),
        Link("Ko-fi", "Buy us a coffee", "https://ko-fi.com/skyblock_overhaul"),
    )

    private val COMMUNITY = listOf(
        Link("Discord", "Support, updates and parties", "https://discord.gg/QvM6b9jsJD"),
        Link("GitHub", "Releases and source code", "https://github.com/SkyblockOverhaul/SBO/releases"),
        Link("Website", "Track your Magic Find upgrades", "https://skyblockoverhaul.com/"),
    )

    private val CREDITS = listOf(
        Link("SkyHanni", "Spade Guess (bloxigus) and Arrow Guess (SidOfThe7Cs)", "https://github.com/hannibal002/SkyHanni"),
        Link("GuiLib", "UI library for these screens (SkyblockOverhaul)", "https://skyblockoverhaul.github.io/maven/guilib/"),
        Link("Elementa", "UI library (SparkUniverse)", "https://github.com/SparkUniverse/Elementa"),
        Link("UniversalCraft", "Compatibility layer (SparkUniverse)", "https://github.com/SparkUniverse/UniversalCraft"),
        Link("hm-api", "Hypixel Mod API wrapper (AzureAaron)", "https://github.com/AzureAaron/hm-api"),
        Link("RenderChest", "Glow API (AzureAaron)", "https://github.com/AzureAaron/RenderChest"),
        Link("Fabric API", "Modding API (FabricMC)", "https://github.com/FabricMC/fabric-api"),
    )

    private val App = component("Hub") {
        val settingsCount = useMemo { countEntries(Settings) }
        var showCommands by useState(false)
        val fit = useFitScale()

        div(className = "hub-window", ref = fit.ref, style = fit.style) {
            div(className = "hub-hero") {
                div(className = "hub-hero-sweep")
                button(className = "hub-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
                div(className = "hub-hero-content") {
                    div(className = "hub-emblem") {
                        img("sbo:ui/hub/emblem.png", className = "hub-emblem-image", alt = "SBO")
                    }
                    div(className = "hub-hero-text") {
                        div(className = "hub-eyebrow") { +"Hypixel Skyblock · Diana · Party Finder" }
                        h1(className = "hub-title") { +"Skyblock Overhaul" }
                        div(className = "hub-tagline") { +"Find a Diana party in seconds with the SBO Party Finder, plus burrow guesses, trackers and QOL features." }
                        div(className = "hub-chips") {
                            span(className = "hub-chip hub-chip-version") { +"SBO ${SBOKotlin.version}" }
                            span(className = "hub-chip") { +"Minecraft ${SBOKotlin.mcVersion}" }
                            span(className = "hub-chip") { +"$settingsCount settings" }
                        }
                    }
                }
            }

            div(className = "hub-body") {
                div(className = "hub-tiles") {
                    TILES.forEachIndexed { i, tile ->
                        val featured = i == 0
                        val highlighted = tile.title == "Party Finder"
                        // Screens are switched after the click is handled, not in the middle of it
                        button(
                            className = when {
                                featured -> "hub-tile hub-featured"
                                highlighted -> "hub-tile hub-highlight"
                                else -> "hub-tile"
                            },
                            key = tile.title,
                            onClick = { mc.schedule { openFromHub(tile) } },
                        ) {
                            div(className = "hub-tile-icon") {
                                // Item icons need a world, Mod Menu can open the screens from the title screen
                                if (mc.level != null) item(ItemStack(tile.icon), decorations = false)
                            }
                            div(className = "hub-tile-text") {
                                div(className = "hub-tile-title") { +tile.title }
                                div(className = "hub-tile-description") {
                                    +(if (featured) "Search all $settingsCount settings at once" else tile.description)
                                }
                                if (tile.command != null) div(className = "hub-tile-command") { +tile.command }
                            }
                            if (featured) span(className = "hub-featured-arrow") { +"→" }
                        }
                    }
                    button(className = "hub-tile", key = "Commands", onClick = { showCommands = true }) {
                        div(className = "hub-tile-icon") {
                            if (mc.level != null) item(ItemStack(Items.COMMAND_BLOCK), decorations = false)
                        }
                        div(className = "hub-tile-text") {
                            div(className = "hub-tile-title") { +"Commands" }
                            div(className = "hub-tile-description") { +"Every SBO command and what it does" }
                            div(className = "hub-tile-command") { +"/sbohelp" }
                        }
                    }
                }

                div(className = "hub-columns") {
                    section(className = "hub-card hub-support") {
                        h2(className = "hub-card-title") { +"Support SBO" }
                        div(className = "hub-card-text") { +"SBO is free. Donations keep the Party Finder and the server running." }
                        div(className = "hub-buttons") {
                            SUPPORT.forEach { linkButton(it, "hub-donate") }
                        }
                    }
                    section(className = "hub-card") {
                        h2(className = "hub-card-title") { +"Community" }
                        div(className = "hub-card-text") { +"Questions, ideas and updates." }
                        div(className = "hub-buttons") {
                            COMMUNITY.forEach { linkButton(it, "hub-link") }
                        }
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
                    div(className = "hub-thanks") {
                        +"Special thanks to all our supporters and contributors, the people who helped testing and gave feedback, "
                        +"and all open source Skyblock mods and libraries we used."
                    }
                }

                div(className = "hub-footer") { +"Made by D4rkSwift, RolexDE and contributors" }
            }

            modal(open = showCommands, onClose = { showCommands = false }, className = "hub-commands") {
                div(className = "hub-commands-header") {
                    h2(className = "hub-card-title") { +"Commands" }
                    button(className = "hub-close-small", title = "Close", onClick = { showCommands = false }) { +"✕" }
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

    private class Fit(val ref: Ref<Element?>, val style: String?)

    // Never scrolls: a screen too small for the window scales it down as a whole. Polled, there is no resize event.
    private fun ComponentScope.useFitScale(): Fit {
        val ref = useElementRef()
        val doc = useDocument()
        var scale by useState(1f)
        useInterval(100) {
            val box = ref.current?.box ?: return@useInterval
            if (box.width <= 0f || box.height <= 0f) return@useInterval
            val next = minOf(1f, doc.viewportWidth * 0.96f / box.width, doc.viewportHeight * 0.96f / box.height)
            if (abs(next - scale) > 0.005f) scale = next
        }
        return Fit(ref, if (scale < 1f) "transform: scale($scale)" else null)
    }

    private fun countEntries(category: Category): Int = category.entries.size + category.subcategories.sumOf(::countEntries)

    private fun NodeBuilder.linkButton(link: Link, className: String) {
        button(className = className, key = link.title, title = link.description, onClick = { SBOKotlin.openInBrowser(link.url) }) {
            +link.title
        }
    }
}
