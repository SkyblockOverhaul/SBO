package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickAfter
import net.sbo.mod.utils.events.impl.guis.GuiPostRenderEvent
import net.sbo.mod.utils.events.impl.render.RenderEvent
import net.sbo.mod.utils.game.World

object OverlayManager {
    val overlays = mutableListOf<Overlay>()

    fun init() {
        Register.onTick(1) {
            overlays.forEach { it.updateConditions() }
        }

//        example:
//        val testOverlay = Overlay("test1",50.0f, 10.0f, 2.0f, "both")
//        val textline = OverlayTextLine("Enjoy using SBO-Kotlin!")
//        textline.onHover { drawContext, textRenderer ->
//            val scaleFactor = mc.window.scaleFactor
//            val mouseX = mc.mouse.x / scaleFactor
//            val mouseY = mc.mouse.y / scaleFactor
//            RenderUtils2D.drawHoveringString(drawContext, "this is hovered text", mouseX, mouseY, textRenderer, testOverlay.scale)
//        }

        Register.command("sboguis", "sbomoveguis", "sbomove") {
            mc.schedule {
                mc.setScreen(OverlayEditScreen())
            }
        }
    }

    fun reloadPositions() {
        overlays.forEach { it.loadPosition() }
    }

    @SboEvent
    fun onMouseClickAfter(event: GuiMouseClickAfter) {
        if (event.screen !is OverlayEditScreen && event.button == 0) {
            overlays.forEach { it.overlayClicked(event.mouseX, event.mouseY) }
        }
    }

    @SboEvent
    fun onPostRender(event: GuiPostRenderEvent) {
        if (event.screen !is OverlayEditScreen) {
            postRender(event.context, event.screen)
        }
    }

    @SboEvent
    fun onRender(event: RenderEvent) {
        render(event.context, mc.screen)
    }

    fun render(drawContext: GuiGraphicsExtractor, renderScreen: Screen?) {
        val inEditScreen = renderScreen is OverlayEditScreen

        if (!World.isInSkyblock() && !inEditScreen) return
        if (mc.options.hideGui || mc.options.keyPlayerList.isDown) return
        if (!inEditScreen && renderScreen != null) return

        val scaleFactor = mc.window.guiScale
        val mouseX = mc.mouseHandler.xpos() / scaleFactor
        val mouseY = mc.mouseHandler.ypos() / scaleFactor

        for (overlay in overlays) {
            overlay.render(drawContext, mouseX, mouseY)
        }
    }

    private fun postRender(drawContext: GuiGraphicsExtractor, renderScreen: Screen) {
        if (!World.isInSkyblock() && renderScreen !is OverlayEditScreen) return
        if (mc.options.hideGui) return

        val scaleFactor = mc.window.guiScale
        val mouseX = mc.mouseHandler.xpos() / scaleFactor
        val mouseY = mc.mouseHandler.ypos() / scaleFactor

        for (overlay in overlays) {
            if (overlay.allowedScreens.any { it(renderScreen) })
                overlay.render(drawContext, mouseX, mouseY)
        }
    }
}
