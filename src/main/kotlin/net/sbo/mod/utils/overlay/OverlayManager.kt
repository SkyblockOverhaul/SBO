package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.render.RenderEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickAfter
import net.sbo.mod.utils.events.impl.guis.GuiPostRenderEvent

object OverlayManager {
    val overlays = mutableListOf<Overlay>()

    fun init() {
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
            mc.send {
                mc.setScreen(OverlayEditScreen())
            }
        }
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
        render(event.context,  mc.currentScreen?.title?.string ?: "")
    }

    fun render(drawContext: DrawContext, renderScreen: String = "") {
        if (!World.isInSkyblock()) return
        val scaleFactor = mc.window.scaleFactor
        val mouseX = mc.mouse.x / scaleFactor
        val mouseY = mc.mouse.y / scaleFactor
        for (overlay in overlays.toList()) {
            if (renderScreen == "" && !mc.options.playerListKey.isPressed && !mc.options.hudHidden)
                overlay.render(drawContext, mouseX, mouseY)
        }
    }

    fun postRender(drawContext: DrawContext, renderScreen: Screen) {
        if (!World.isInSkyblock()) return
        val scaleFactor = mc.window.scaleFactor
        val mouseX = mc.mouse.x / scaleFactor
        val mouseY = mc.mouse.y / scaleFactor
        for (overlay in overlays.toList()) {
            if (renderScreen.title.string in overlay.allowedGuis && !mc.options.hudHidden)
                overlay.render(drawContext, mouseX, mouseY)
        }
    }
}
