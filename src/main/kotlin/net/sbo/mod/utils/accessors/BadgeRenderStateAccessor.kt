package net.sbo.mod.utils.accessors

import net.minecraft.network.chat.Component

/**
 * Interface to inject via Mixin
 */
internal interface BadgeRenderStateAccessor {
    fun `sbo$getBadgeLine`(): Component?
    fun `sbo$setBadgeLine`(line: Component?)
}
