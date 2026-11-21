package net.sbo.mod.utils.overlay

import net.sbo.mod.utils.events.Register

abstract class DirtyFlushableOverlay {
    private var dirty = false

    abstract val overlay: Overlay

    init {
        Register.onTick(1) {
            flushUpdateLines()
        }
    }

    fun updateLines() {
        dirty = true
    }

    private fun flushUpdateLines() {
        if (!dirty || !isShown()) {
            return
        }

        flush()
        dirty = false
    }

    /** Whether it is drawn, the lines are only made while it is. */
    protected open fun isShown(): Boolean = overlay.checkCondition()

    /** Shows the current data, by default as the [generateLines] of [overlay]. */
    protected open fun flush() {
        overlay.setLines(generateLines())
    }

    abstract fun generateLines(): List<OverlayTextLine>
}
