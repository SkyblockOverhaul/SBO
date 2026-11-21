package net.sbo.mod.utils.accessors

import net.minecraft.network.chat.Component

/**
 * Interface to inject via Mixin
 */
internal interface CustomizerRenderStateAccessor {
    fun `sbo$getCustomizerLine`(): Component?
    fun `sbo$setCustomizerLine`(line: Component?)
    fun `sbo$getNameSize`(): Float
    fun `sbo$setNameSize`(size: Float)
}
