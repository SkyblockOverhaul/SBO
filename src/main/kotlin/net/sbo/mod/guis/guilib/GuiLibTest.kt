package net.sbo.mod.guis.guilib

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.checkbox
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World

/** Small test screen for GuiLib inside SBO. Open with `/sboguitest`. Demo data only. */
object GuiLibTest {
    private data class Mob(val name: String, val color: String)

    private val MOBS = listOf(
        Mob("Minos Inquisitor", "§d"),
        Mob("Minos Champion", "§5"),
        Mob("Minotaur", "§6"),
        Mob("Gaia Construct", "§a"),
        Mob("Siamese Lynxes", "§b"),
        Mob("Minos Hunter", "§e"),
    )

    private data class MobRowProps(val mob: Mob, val count: Int, val onChange: (Int) -> Unit)

    private val MobRow = component<MobRowProps>("MobRow") { p ->
        div(className = "mob-row") {
            span(className = "mob-name") { +"${p.mob.color}${p.mob.name}" }
            div(className = "counter") {
                button(className = "small", disabled = p.count == 0, onClick = { p.onChange(p.count - 1) }) { +"−" }
                span(className = "count") { +"${p.count}" }
                button(className = "small", onClick = { p.onChange(p.count + 1) }) { +"+" }
            }
        }
    }

    private val App = component("SboGuiLibTest") {
        var counts by useState(MOBS.associate { it.name to 0 })
        var note by useState("")
        var filter by useState("all")
        var announce by useState(true)
        var confirmReset by useState(false)
        var clock by useState(0)
        useInterval(1000) { clock++ }

        val total = counts.values.sum()
        val shown = MOBS.filter { filter == "all" || (filter == "rare" && it.name in setOf("Minos Inquisitor", "Minos Champion")) }

        div(className = "window") {
            div(className = "titlebar") {
                span(className = "title") { +"§6SBO §f× GuiLib" }
                button(className = "close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }
            div(className = "content") {
                div(className = "info") {
                    span { +"Player: §b${mc.player?.name?.string ?: "unknown"}" }
                    span { +"Area: §e${World.getWorld()}" }
                    span(className = "muted") { +"Open for ${clock}s" }
                }

                div(className = "toolbar") {
                    select(value = filter, onChange = { filter = it.value }) {
                        option("all", "All mobs")
                        option("rare", "Rare only")
                    }
                    span(className = "total") { +"Total: §a$total" }
                }

                scroll(className = "mob-list") {
                    for (mob in shown) {
                        MobRow(MobRowProps(mob, counts.getValue(mob.name)) { n -> counts = counts + (mob.name to n) }, key = mob.name)
                    }
                }

                div(className = "form") {
                    input(value = note, onChange = { note = it.value }, placeholder = "Party note…", maxLength = 32)
                    checkbox(checked = announce, onChange = { announce = it.checked }, label = "Announce in chat")
                }

                div(className = "actions") {
                    tooltip("Resets all counters (asks first)") {
                        button(className = "danger", disabled = total == 0, onClick = { confirmReset = true }) { +"Reset" }
                    }
                    button(className = "primary", onClick = {
                        val text = "§6[SBO] §7GuiLib test: §a$total §7mobs" + (if (note.isNotBlank()) " §8– §f$note" else "")
                        if (announce) Chat.chat(text)
                    }) { +"Send to chat" }
                }
            }
        }

        modal(open = confirmReset, onClose = { confirmReset = false }, className = "confirm") {
            h3 { +"Reset counters?" }
            p(className = "muted") { +"This sets all $total counted mobs back to 0." }
            div(className = classNames("actions", "end")) {
                button(onClick = { confirmReset = false }) { +"Cancel" }
                button(className = "danger", onClick = {
                    counts = MOBS.associate { it.name to 0 }
                    confirmReset = false
                }) { +"Reset" }
            }
        }
    }

    fun open() = GuiLib.open(App, stylesheets = listOf("sbo:ui/guilib-test.css"), title = "SBO GuiLib Test")

    fun register() {
        Register.command("sboguitest") {
            // Open after the chat screen has closed.
            mc.schedule { open() }
        }
    }
}
