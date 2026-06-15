package net.sbo.mod.utils.accessors

import net.minecraft.world.entity.Entity
import java.awt.Color

/**
 * Interface to inject via Mixin
 */
internal interface EntityAccessor {
    fun `sbo$setGlowing`(glowing: Boolean)
    fun `sbo$setGlowingColor`(color: Int)
    fun `sbo$glowTime`(time: Long)
    fun `sbo$setGlowingThisFrame`(glowing: Boolean)
}

// Extension property to easily toggle custom glowing
var Entity.isSboGlowing: Boolean
    get() = this.isCurrentlyGlowing
    set(value) {
        (this as? EntityAccessor)?.`sbo$setGlowing`(value)
    }

// Extension property to set the color
var Entity.sboGlowingColor: Int
    get() = this.teamColor
    set(value) {
        (this as? EntityAccessor)?.`sbo$setGlowingColor`(value)
    }

// Helper to set color using java.awt.Color
fun Entity.setSboGlowColor(color: Color) {
    this.sboGlowingColor = color.rgb
}