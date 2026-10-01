package net.sbo.mod.guis

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.PositionConstraint
import gg.essential.elementa.constraints.SizeConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import java.awt.Color

class UILine(
    private val x: PositionConstraint,
    private val y: PositionConstraint,
    private val width: SizeConstraint,
    private val height: SizeConstraint,
    color: Color,
    parent: UIComponent? = null,
    rounded: Boolean = false,
    roundness: Float = 5f
) {
    private val uiObject: UIComponent = if (rounded) UIRoundedRectangle(roundness) else UIBlock()

    fun get(): UIComponent = uiObject

    init {
        uiObject.constrain {
            this.x = this@UILine.x
            this.y = this@UILine.y
            this.width = this@UILine.width
            this.height = this@UILine.height
        }.setColor(color)

        parent?.let { uiObject.childOf(it) }
    }
}
