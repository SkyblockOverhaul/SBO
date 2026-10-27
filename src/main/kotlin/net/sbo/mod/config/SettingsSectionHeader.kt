package net.sbo.mod.config

import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h2
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch

/** Shared by production and offline presentation fixtures; callbacks retain existing persistence. */
fun NodeBuilder.settingsSectionHeader(title: String, enabled: Boolean, onCollapse: () -> Unit, onEnabled: (Boolean) -> Unit) {
    div(className = "cfg-sub-head") {
        h2(className = "cfg-sub-title", title = "Collapse or expand $title", tabIndex = if (enabled) 0 else null,
            onClick = if (enabled) { _ -> onCollapse() } else null,
            onKeyDown = { event ->
                if (enabled && (event.key == "Enter" || event.key == " ")) { event.preventDefault(); onCollapse() }
            }) {
            span(className = "cfg-chevron")
            +title
        }
        div(title = "Enable or disable the on/off settings in $title") {
            switch(checked = enabled, label = "$title section enabled", className = "cfg-section-switch", onChange = { onEnabled(it.checked) })
        }
    }
}
