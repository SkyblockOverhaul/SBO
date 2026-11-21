package net.sbo.mod.customizer.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.customizer.SboCustomizer
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useFitScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.customizer.CustomizerPart
import net.sbo.mod.customizer.CustomizerSettings
import net.sbo.mod.customizer.OwnCustomizerResponse
import net.sbo.mod.customizer.PartColor
import net.sbo.mod.utils.http.SboApi

/**
 * SBO Customizer (/sbocustomizer): your customizer, the look of your name and your player size.
 * Left the real player model with the name tag, tab line and chat line as others see them, built by the same code
 * as in game; right everything of one part in one place.
 */
object CustomizerGui {
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Customizer")
    }
}

private val STYLES = listOf("sbo:ui/customizer/customizer.css", SboLook.STYLE)
internal const val LOGO = "sbo:textures/font/customizer.png"

/** Sections of the editor after the parts: your player, then how other supporters look to you ([SUPPORTERS]). */
private const val PLAYER = "player"
private const val SUPPORTERS = "supporters"
/** Not a part of its own: tag, level, rank and name while they share one look. */
internal const val LINE = "line"

private sealed interface Load {
    data object Loading : Load
    data class Failed(val message: String, val notEligible: Boolean) : Load
    data class Ready(val customizer: OwnCustomizerResponse) : Load
}

private val App = component("Customizer") {
    val toast = useToast()
    var load by useState<Load>(Load.Loading)
    var draft by useState<CustomizerSettings?>(null)
    var saving by useState(false)
    var section by useState(CustomizerPart.NAME)
    /** The part the symbols row in Player changes. */
    var symbolPart by useState(CustomizerPart.NAME)
    var leaving by useState(false)
    /** Here and not in Supporters, so the stage shows the text sizes while the sliders move. */
    var textSizes by useState(TextSizes(SboCustomizer.lineSize(), SboCustomizer.nameSize()))
    /** Counts the changes on Supporters, so the sample supporter draws again with them. */
    var supportersChanges by useState(0)
    useSboScale()
    useSboTheme("customizer")

    fun loaded(response: OwnCustomizerResponse) {
        if (response.success && response.settings != null) {
            SboCustomizer.rememberAccess(true)
            load = Load.Ready(response)
            draft = response.settings
        } else {
            if (response.code == "NOT_ELIGIBLE") SboCustomizer.rememberAccess(false)
            load = Load.Failed(response.error ?: "Could not load your customizer.", response.code == "NOT_ELIGIBLE")
        }
    }

    useEffect {
        SboApi.ownCustomizer()
            .toJson<OwnCustomizerResponse>(ignoreUnknownKeys = true) { mc.execute { loaded(it) } }
            .error { mc.execute { load = Load.Failed("Could not reach the SBO server: ${it.message}", false) } }
    }

    fun save(settings: CustomizerSettings, close: Boolean = false) {
        saving = true
        SboApi.saveCustomizer(settings)
            .toJson<OwnCustomizerResponse>(ignoreUnknownKeys = true) { response ->
                mc.execute {
                    saving = false
                    if (response.success) {
                        loaded(response)
                        SboCustomizer.fetch()
                        if (close) GuiLib.close()
                        toast.success("Others see your new look within a few minutes.", title = "Saved")
                    } else {
                        toast.error(response.error ?: "Could not save.", title = "SBO Customizer", durationMs = 6000)
                    }
                }
            }
            .error {
                mc.execute {
                    saving = false
                    toast.error("Could not reach the SBO server: ${it.message}", title = "SBO Customizer", durationMs = 6000)
                }
            }
    }

    val ready = load as? Load.Ready
    val settings = ready?.let { draft ?: it.customizer.settings }
    val unsaved = ready != null && settings != null && settings != ready.customizer.settings
    fun close() = if (unsaved) leaving = true else GuiLib.close()
    useEscapeBack(unsaved && !saving) { leaving = true }
    val fit = useFitScale()

    div(className = "cz-window", ref = fit.ref, style = fit.style) {
        header(className = "cz-header") {
            img(LOGO, className = "cz-logo")
            span(className = "cz-title") { +"SBO Customizer" }
            ready?.customizer?.rankInfo?.let { rank ->
                span(className = "cz-rank", style = "background: ${background(PartColor(rank.color, rank.to), CustomizerPart.TAG)}") { +rank.label }
            }
            div(className = "cz-spacer")
            if (ready != null && settings != null) {
                button(disabled = saving || !unsaved, title = "Back to what is saved", onClick = { draft = ready.customizer.settings }) { +"Undo" }
                button(className = "cz-primary", disabled = saving || !unsaved, onClick = { save(settings) }) {
                    +if (saving) "Saving..." else "Save"
                }
            }
            button(className = "cz-icon cz-close", title = "Close", onClick = { close() }) { +"x" }
        }
        div(className = "cz-body") {
            val supportersProps = SupportersProps(textSizes, { textSizes = it }) { supportersChanges++ }
            if (load is Load.Failed || ready != null && section == SUPPORTERS) {
                SupportersStage(SupportersStageProps(textSizes, supportersChanges))
            } else {
                Stage(StageProps(ready?.customizer, settings, textSizes))
            }
            div(className = "cz-editor") {
                when (val state = load) {
                    Load.Loading -> {
                        div(className = "cz-center") { +"Loading..." }
                    }
                    is Load.Failed -> scroll(className = "cz-panel cz-panel-tall guilib-autohide") {
                        failed(state)
                        heading("How supporters look")
                        Supporters(supportersProps)
                    }
                    is Load.Ready -> if (settings != null) {
                        val customizer = state.customizer
                        nav(customizer, section) { section = it }
                        scroll(className = "cz-panel guilib-autohide", key = "panel:$section") {
                            when (section) {
                                PLAYER -> playerPanel(customizer, settings, symbolPart, { symbolPart = it }) { draft = it }
                                SUPPORTERS -> Supporters(supportersProps)
                                else -> partPanel(customizer, settings, section) { draft = it }
                            }
                        }
                    }
                }
            }
        }
    }

    modal(open = leaving, onClose = { leaving = false }, className = "cz-modal") {
        div(className = "cz-modal-title") { +"Leave without saving?" }
        div(className = "cz-modal-text") { +"Your changes are not saved yet." }
        div(className = "cz-modal-buttons") {
            button(onClick = { leaving = false }) { +"Keep editing" }
            button(className = "cz-danger", onClick = {
                leaving = false
                GuiLib.close()
            }) { +"Discard" }
            button(className = "cz-primary", disabled = saving, onClick = {
                leaving = false
                draft?.let { save(it, close = true) }
            }) { +"Save" }
        }
    }
}

private fun NodeBuilder.nav(customizer: OwnCustomizerResponse, section: String, onSection: (String) -> Unit) {
    segmented(value = section, onChange = onSection, className = "cz-nav") {
        option(CustomizerPart.STAT, "Logo & Stat")
        if (hasTag(customizer)) option(CustomizerPart.TAG, "Tag")
        option(CustomizerPart.LEVEL, "Level")
        option(CustomizerPart.RANK, "Rank")
        option(CustomizerPart.NAME, "Name")
        option(PLAYER, "Player")
        option(SUPPORTERS, "Supporters")
    }
}

internal fun hasTag(customizer: OwnCustomizerResponse): Boolean = customizer.team && customizer.rankInfo?.tag != null

private fun NodeBuilder.failed(state: Load.Failed) {
    if (!state.notEligible) {
        div(className = "cz-error") { +state.message }
        return
    }
    div(className = "cz-steps") {
        div(className = "cz-steps-title") { +"For SBO supporters (Patreon or Ko-fi)." }
        div(className = "cz-step") { +"1. Link your Minecraft account: /link followed by your Minecraft name on the SBO Discord." }
        div(className = "cz-step") { +"2. Use the Discord account with the supporter role." }
        div(className = "cz-step") { +"3. Open this again with /${SboCustomizer.COMMAND}. New roles can take a few minutes." }
    }
}
