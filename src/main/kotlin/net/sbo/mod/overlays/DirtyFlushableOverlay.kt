package net.sbo.mod.overlays

import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.events.Register

abstract class DirtyFlushableOverlay {
    private var dirty = false
    abstract val overlay: Overlay

    init {
        Register.onTick(1) { flushUpdateLines() }
    }

    fun updateLines() { dirty = true }
    fun flushUpdateLines() {
        if (!dirty) return
        overlay.setLines(generateLines())
        dirty = false
    }
    abstract fun generateLines(): List<OverlayTextLine>
}
