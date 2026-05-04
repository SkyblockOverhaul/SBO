package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.overlayData
import org.lwjgl.glfw.GLFW
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent

class OverlayEditScreen : Screen(Component.literal("SBO_Overlay_Editor")) {
    private var selectedOverlay: Overlay? = null
    private var isDragging = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        this.renderMenuBackground(context)
        OverlayManager.render(context)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x
        val mouseY = click.y
        val button = click.buttonInfo().button
        if (button == 0) {
            selectedOverlay = OverlayManager.overlays.firstOrNull { it.isOverOverlay(mouseX, mouseY) }

            if (selectedOverlay != null) {
                isDragging = true
                lastMouseX = mouseX
                lastMouseY = mouseY

                OverlayManager.overlays.forEach {
                    it.selected = (it == selectedOverlay)
                }
            } else {
                OverlayManager.overlays.forEach { it.selected = false }
            }
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging && selectedOverlay != null) {
            selectedOverlay?.x = (selectedOverlay?.x ?: 0f) + deltaX.toFloat()
            overlayData.overlays[selectedOverlay!!.name]?.x = selectedOverlay?.x ?: 0f
            selectedOverlay?.y = (selectedOverlay?.y ?: 0f) + deltaY.toFloat()
            overlayData.overlays[selectedOverlay!!.name]?.y = selectedOverlay?.y ?: 0f
            return true
        }
        return super.mouseDragged(click, deltaX, deltaY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        val button = click.buttonInfo().button
        if (button == 0) {
            isDragging = false
            return true
        }
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        selectedOverlay?.let {
            it.scale = (it.scale + verticalAmount * 0.1f).coerceIn(0.5, 5.0).toFloat()
            overlayData.overlays[it.name]?.scale = it.scale
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyInput: KeyEvent): Boolean {
        val keyCode = keyInput.key
        selectedOverlay?.let {
            val step = 1f
            when (keyCode) {
                GLFW.GLFW_KEY_UP -> {
                    it.y -= step
                    overlayData.overlays[it.name]?.y = it.y
                }
                GLFW.GLFW_KEY_DOWN -> {
                    it.y += step
                    overlayData.overlays[it.name]?.y = it.y
                }
                GLFW.GLFW_KEY_LEFT -> {
                    it.x -= step
                    overlayData.overlays[it.name]?.x = it.x
                }
                GLFW.GLFW_KEY_RIGHT -> {
                    it.x += step
                    overlayData.overlays[it.name]?.x = it.x
                }
                else -> return super.keyPressed(keyInput)
            }
            return true
        }
        return super.keyPressed(keyInput)
    }

    override fun removed() {
        super.removed()
        OverlayManager.overlays.forEach { it.selected = false }
        SboDataObject.save("OverlayData")
    }
}