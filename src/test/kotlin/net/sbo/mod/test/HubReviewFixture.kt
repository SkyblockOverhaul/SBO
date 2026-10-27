package net.sbo.mod.test

import net.sbo.guilib.core.UiRoot
import net.sbo.guilib.core.css.Origin
import net.sbo.guilib.core.css.Stylesheet
import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.VComponent
import net.sbo.guilib.core.dom.Rect
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.guilib.core.paint.PaintCommand
import net.sbo.mod.guis.look.useFitScale
import net.sbo.mod.guis.look.useCommandsFocus
import net.sbo.guilib.core.event.Modifiers
import java.nio.file.Files
import java.nio.file.Path

/** Offline presentation fixture. Matches HubGui's hierarchy, without Minecraft/network hooks.
 * Labels and all command descriptions are read from production source to prevent stale samples.
 * Item slots are deliberately empty: headless output cannot verify native item rendering.
 */
internal class HubReviewFixture(
    css: String,
    override val width: Float = 640f,
    override val height: Float = 360f,
    val theme: String = "sbo-dark",
    unlocked: Boolean = false,
    manageCommandsFocus: Boolean = true,
) : ReviewFrame {
    companion object {
        val project: Path get() = ReviewResources.project
        private fun source(path: String) = Files.readString(project.resolve("src/main/kotlin/net/sbo/mod/$path"))
        private val hub = source("guis/HubGui.kt")
        private val tilePattern = Regex("Tile\\(\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"")
        val tiles = tilePattern.findAll(hub).map { it.groupValues[1] to it.groupValues[2] }.toList()
            .let { all -> all.filter { it.first !in setOf("Cloud Sync", "SBO Badge") } + all.filter { it.first in setOf("Cloud Sync", "SBO Badge") } }
        val commands = Regex("mapOf\\(\"cmd\" to \"([^\"]+)\", \"desc\" to \"([^\"]+)\"\\)")
            .findAll(source("general/HelpCommand.kt")).map { it.groupValues[1] to it.groupValues[2] }.toList()
        val currentCss: String get() = Files.readString(project.resolve("src/main/resources/assets/sbo/ui/hub/hub.css"))
        val baselineCss: String get() = Files.readString(project.resolve("tools/ui-review/baseline/hub.css"))
        fun resource(path: String) = ReviewResources.resource(path)
        private fun sheet(path: String, origin: Origin = Origin.AUTHOR) = resource(path).bufferedReader().use {
            Stylesheet.parse(it.readText(), path, origin)
        }
        private fun links(name: String) = Regex("Link\\(\"([^\"]+)\", \"([^\"]+)\", \"([^\"]+)\"\\)")
            .findAll(hub.substringAfter("private val $name =").substringBefore("\n    )"))
            .map { it.groupValues[1] to it.groupValues[2] }.toList()
    }

    private var now = 0L
    override val fonts = ReviewFonts()
    override val emblem: Rect get() = element(".hub-emblem-image").getBoundingClientRect()
    val root = UiRoot(fonts, listOf(
        sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT),
        Stylesheet.parse(css, "hub.css"),
        sheet("/assets/sbo/ui/themes/themes.css"),
    ), clock = { now })
    var commandsOpen = false
        private set
    private val app = component("Offline Hub review") {
        var showCommands by useState(false)
        commandsOpen = showCommands
        useCommandsFocus(showCommands && manageCommandsFocus)
        val fit = useFitScale(preferredScale = 1.2f)
        div(className = "hub-window", ref = fit.ref, style = fit.style) {
            div(className = "hub-hero") {
                button(className = "hub-close", title = "Close", onClick = {}) { +"×" }
                div(className = "hub-hero-content") {
                    div(className = "hub-emblem") { img("sbo:ui/hub/emblem.png", className = "hub-emblem-image", alt = "SBO") }
                    div(className = "hub-hero-text") {
                        h1(className = "hub-title") { +"Skyblock Overhaul" }
                        div(className = "hub-chips") {
                            span(className = "hub-chip hub-chip-version") { +"SBO 0.7.2" }
                            span(className = "hub-chip") { +"Minecraft 26.2" }
                        }
                    }
                }
            }
            div(className = "hub-body") {
                div(className = "hub-tiles") {
                    tiles.forEach { (name, command) ->
                        val supporter = name in setOf("Cloud Sync", "SBO Badge")
                        button(className = "hub-tile" + if (supporter) " hub-supporter" else "", key = name,
                            onClick = { if (name == "Commands") showCommands = true }) {
                            div(className = "hub-tile-icon")
                            div(className = "hub-tile-text") {
                                div(className = "hub-tile-title") { +name }
                                div(className = "hub-tile-command") { +command }
                            }
                            if (supporter) span(className = "hub-premium" + if (unlocked) " hub-premium-on" else "") {
                                span(className = "hub-premium-label") { +"Supporter" }
                            }
                        }
                    }
                }
                div(className = "hub-columns") {
                    listOf("SUPPORT" to "Support SBO", "COMMUNITY" to "Community", "CREDITS" to "Credits").forEach { (key, label) ->
                        section(className = "hub-card" + if (key == "CREDITS") " hub-credits" else "") {
                            h2(className = "hub-card-title") { +label }
                            div(className = if (key == "CREDITS") "hub-credit-list" else "hub-buttons") {
                                links(key).forEach { (title, description) ->
                                    button(className = when (key) { "SUPPORT" -> "hub-donate"; "CREDITS" -> "hub-credit"; else -> "hub-link" },
                                        title = description, onClick = {}) { +title }
                                }
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
                    commands.forEach { (command, description) ->
                        div(className = "hub-command", key = command) {
                            div(className = "hub-command-name") { +"/$command" }
                            div(className = "hub-command-description") { +description }
                        }
                    }
                }
            }
        }
    }
    init {
        root.render(VComponent(app, Unit, null))
        root.document.body.classList.add("sbo-theme-$theme")
        settle()
    }
    override fun settle(): List<PaintCommand> {
        var frame = emptyList<PaintCommand>()
        repeat(5) { now += 250; frame = root.frame(width, height) }
        return frame
    }
    fun element(selector: String): Element = requireNotNull(root.document.body.querySelector(selector)) { selector }
    fun all(selector: String) = root.document.body.querySelectorAll(selector)
    fun hover(element: Element, bottom: Boolean = false) {
        val r = element.getBoundingClientRect()
        root.input.mouseMove(r.x + r.width / 2, if (bottom) r.bottom - 0.25f else r.y + r.height / 2)
        settle()
    }
    fun click(element: Element) {
        val r = element.getBoundingClientRect()
        root.input.mouseDown(r.x + r.width / 2, r.y + r.height / 2, 0)
        root.input.mouseUp(r.x + r.width / 2, r.y + r.height / 2, 0)
        settle()
    }
    fun key(key: String, shift: Boolean = false) {
        val modifiers = Modifiers(shift = shift)
        root.input.keyDown(key, 0, modifiers)
        root.input.keyUp(key, 0, modifiers)
        settle()
    }
    fun openCommands() = click(all(".hub-tile")[6])
}

