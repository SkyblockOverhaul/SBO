package net.sbo.mod.utils.overlay

import net.sbo.mod.utils.events.Register

abstract class DirtyFlushableOverlay {
    private var dirty = false
    abstract val overlay: Overlay

    init {
        Register.onTick(1) { flushUpdateLines() }
    }

    fun updateLines() { dirty = true }
    private fun flushUpdateLines() {
        if (!dirty) return
        overlay.setLines(generateLines())
        dirty = false
    }
    abstract fun generateLines(): List<OverlayTextLine>
}