package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.sbo.mod.utils.data.DataManager
import org.lwjgl.glfw.GLFW

class OverlayEditScreen : Screen(Component.literal("Move Overlays")) {
    private var selectedOverlay: HudElement? = null
    private var isDragging = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        context.fill(0, 0, width, height, OverlayEditorStyle.BACKGROUND)
        OverlayManager.render(context, this)
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
                    it.selected = it == selectedOverlay
                }
            } else {
                OverlayManager.overlays.forEach { it.selected = false }
            }
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val overlay = selectedOverlay
        if (isDragging && overlay != null) {
            overlay.x += deltaX.toFloat()
            overlay.y += deltaY.toFloat()
            overlay.clampPosition()
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
            it.clampPosition()
            return@mouseScrolled true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyInput: KeyEvent): Boolean {
        val keyCode = keyInput.key
        selectedOverlay?.let {
            val step = when {
                keyInput.hasControlDown() && keyInput.hasShiftDown() -> 0.05f
                keyInput.hasControlDown() -> 0.5f
                keyInput.hasShiftDown() -> 0.01f
                else -> 0.1f
            }

            when (keyCode) {
                GLFW.GLFW_KEY_UP -> it.y -= 1f
                GLFW.GLFW_KEY_DOWN -> it.y += 1f
                GLFW.GLFW_KEY_LEFT -> it.x -= 1f
                GLFW.GLFW_KEY_RIGHT -> it.x += 1f
                GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> it.scale = (it.scale + step).coerceIn(0.5f, 5.0f)
                GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> it.scale = (it.scale - step).coerceIn(0.5f, 5.0f)
                else -> return@keyPressed super.keyPressed(keyInput)
            }
            // Keeps it on the screen and saves the position and scale
            it.clampPosition()
            return@keyPressed true
        }
        return super.keyPressed(keyInput)
    }

    override fun removed() {
        super.removed()
        OverlayManager.overlays.forEach { it.selected = false }
        DataManager.save(DataManager::overlayData)
    }
}
