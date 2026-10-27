package net.sbo.mod.guis.look

import net.sbo.guilib.core.dsl.ComponentScope
import net.sbo.guilib.core.event.KeyboardEvent

/** GuiLib's modal handles Escape/backdrop clicks, but does not contain or restore keyboard focus. */
fun ComponentScope.useCommandsFocus(open: Boolean) {
    val document = useDocument()
    useEffect(open) {
        if (open) {
            val previous = document.focusedElement
            // The portal is attached after this component renders.
            setTimeout(0) { document.body.querySelector(".hub-close-small")?.focus() }
            onCleanup { previous?.takeIf { document.body.contains(it) }?.focus() }
        }
    }
    useDocumentEvent("keydown") { event ->
        val key = event as KeyboardEvent
        if (open && key.key == "Tab") {
            val modal = document.body.querySelector(".hub-commands")
            val targets = modal?.querySelectorAll("button, scroll")?.filterNot { it.disabled }.orEmpty()
            if (targets.isNotEmpty()) {
                key.preventDefault()
                val current = targets.indexOf(document.focusedElement)
                val next = if (key.shiftKey) {
                    if (current <= 0) targets.lastIndex else current - 1
                } else (current + 1) % targets.size
                targets[next].focus()
            }
        }
    }
}
