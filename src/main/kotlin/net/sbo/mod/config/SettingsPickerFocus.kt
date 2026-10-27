package net.sbo.mod.config

import net.sbo.guilib.core.dsl.ComponentScope

/** Keep a still-open multiselect keyboard-operable after choosing an option with the mouse. */
fun ComponentScope.useSettingsPickerFocus() {
    val document = useDocument()
    useDocumentEvent("click") { event ->
        val menu = document.body.querySelector(".guilib-select-menu")
        if (menu?.contains(event.target) == true) {
            document.body.querySelector(".cfg-select[open]")?.focus()
        }
    }
}
