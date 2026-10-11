package net.sbo.mod.guis.look

import net.sbo.guilib.core.dom.Element
import net.sbo.guilib.core.dom.Ref
import net.sbo.guilib.core.dsl.ComponentScope
import kotlin.math.abs

class Fit(val ref: Ref<Element?>, val style: String?)

// Never scrolls: a screen too small for the window scales it down as a whole. Polled, there is no resize event.
fun ComponentScope.useFitScale(): Fit {
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
