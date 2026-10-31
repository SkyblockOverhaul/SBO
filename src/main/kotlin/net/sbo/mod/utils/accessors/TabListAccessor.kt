package net.sbo.mod.utils.accessors

import net.minecraft.client.gui.components.PlayerTabOverlay

/**
 * Interface to inject via Mixin
 */
internal interface TabListAccessor {
    fun `sbo$isVisible`(): Boolean
}

// Whether the game drew the tab list last frame, also when a mod keeps it open without the key (VanillaHUD's toggle mode)
val PlayerTabOverlay.isSboVisible: Boolean
    get() = (this as? TabListAccessor)?.`sbo$isVisible`() ?: false
