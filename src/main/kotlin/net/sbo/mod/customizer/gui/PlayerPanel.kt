package net.sbo.mod.customizer.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.switch
import net.sbo.mod.customizer.CustomizerLook
import net.sbo.mod.customizer.CustomizerSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.customizer.OwnCustomizerResponse
import java.util.Locale
import kotlin.math.roundToInt

/** Your player. The size snaps to the backend's steps; 1 is the normal size and is not saved. */
internal fun NodeBuilder.playerPanel(
    customizer: OwnCustomizerResponse,
    settings: CustomizerSettings,
    symbolPart: String,
    onSymbolPart: (String) -> Unit,
    onChange: (CustomizerSettings) -> Unit,
) {
    val sizes = customizer.sizes
    val look = settings.look ?: CustomizerLook()
    symbolsRow(customizer, settings, symbolPart, onSymbolPart, onChange)
    if (sizes != null) {
        row("Size", "How big others with SBO see you. Only your model changes, never your hitbox.") {
            div(className = "cz-inline") {
                slider(
                    value = look.scale,
                    onChange = { v ->
                        val size = ((v / sizes.step).roundToInt() * sizes.step * 100f).roundToInt() / 100f
                        onChange(settings.copy(look = look.copy(size = size.coerceIn(sizes.min, sizes.max).takeUnless { it == 1f })))
                    },
                    min = sizes.min,
                    max = sizes.max,
                    step = sizes.step,
                    showValue = true,
                    format = { "%.2fx".format(Locale.ROOT, it) },
                    className = "cz-slider",
                )
                button(disabled = look.size == null, title = "Normal size", onClick = { onChange(settings.copy(look = look.copy(size = null))) }) { +"Reset" }
            }
        }
    } else {
        div(className = "cz-hint cz-note") { +"The SBO server does not support player sizes yet." }
    }
    OwnView()
}

/** Only on this PC, so it changes right away without saving. */
private val OwnView = component("CustomizerOwnView") {
    var inF5 by useState(DataManager.sboData.customizerOwnInF5)
    row("Show me in F5", "See your own name tag and customizer in third person.") {
        switch(checked = inF5, onChange = {
            inF5 = it.checked
            DataManager.sboData.customizerOwnInF5 = it.checked
            DataManager.sboData.save()
        })
    }
}
