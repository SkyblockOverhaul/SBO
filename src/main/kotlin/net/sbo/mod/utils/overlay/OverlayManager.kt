package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.GuiMouseClickAfter
import net.sbo.mod.utils.events.impl.guis.GuiPostRenderEvent
import net.sbo.mod.utils.events.impl.render.RenderEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.accessors.isSboVisible
import net.sbo.mod.utils.render.RenderUtils2D

object OverlayManager {
    val overlays = mutableListOf<HudElement>()

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

        Register.command("sboresethudpositions") {
            overlays.forEach { it.resetPosition() }
            DataManager.overlayData.save()
        }
    }

    fun reloadPositions() {
        overlays.forEach { it.loadPosition() }
    }

    @SboEvent
    fun onMouseClickAfter(event: GuiMouseClickAfter) {
        if (event.screen !is OverlayEditScreen && event.button == 0) {
            // The eye collapses or opens its overlay; a collapsed one takes no other clicks
            overlays.firstOrNull { it.checkCondition() && it.isOverEye(event.mouseX, event.mouseY) }?.let {
                it.toggleCollapsed()
                return
            }
            overlays.forEach { if (!it.isCollapsed()) it.overlayClicked(event.mouseX, event.mouseY) }
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
        if (mc.options.hideGui || isTabListShown()) return
        if (!inEditScreen && renderScreen != null) return

        // Without a screen (and while moving them) everything is at its saved position
        overlays.forEach {
            it.pushX = 0f
            it.pushY = 0f
            it.hideEye()
        }
        pushedLayout = null

        val scaleFactor = mc.window.guiScale
        val mouseX = mc.mouseHandler.xpos() / scaleFactor
        val mouseY = mc.mouseHandler.ypos() / scaleFactor

        for (overlay in overlays) {
            overlay.render(drawContext, mouseX, mouseY)
        }
    }

    /**
     * The key alone misses a tab list another mod keeps open (VanillaHUD's toggle mode), the visible flag alone misses
     * one a mod draws itself in place of the vanilla one (SkyHanni's compact tab list).
     */
    private fun isTabListShown(): Boolean {
        if (mc.options.keyPlayerList.isDown) return true
        //#if MC > 26.1
        //$$ val tabList = mc.gui.hud.tabList
        //#else
        val tabList = mc.gui.tabList
        //#endif
        return tabList.isSboVisible
    }

    private fun postRender(drawContext: GuiGraphicsExtractor, renderScreen: Screen) {
        if (!World.isInSkyblock() && renderScreen !is OverlayEditScreen) return
        if (mc.options.hideGui) return

        val scaleFactor = mc.window.guiScale
        val mouseX = mc.mouseHandler.xpos() / scaleFactor
        val mouseY = mc.mouseHandler.ypos() / scaleFactor

        overlays.forEach { it.hideEye() }
        val shown = overlays.filter { overlay -> overlay.checkCondition() && overlay.allowedScreens.any { it(renderScreen) } }
        pushApart(shown)
        // In the inventory every overlay gets an eye to collapse it, so it doesn't cover the items
        if (renderScreen !is InventoryScreen) {
            for (overlay in shown) overlay.render(drawContext, mouseX, mouseY)
            return
        }
        for (overlay in shown) overlay.renderInInventory(drawContext, mouseX, mouseY)
        // After all overlays, so it is drawn above them
        val hovered = shown.lastOrNull { it.isOverEye(mouseX, mouseY) } ?: return
        RenderUtils2D.drawHoveringString(drawContext, if (hovered.isCollapsed()) hovered.label else "Collapse ${hovered.label}", mouseX, mouseY, mc.font)
    }

    private const val PUSH_GAP = 2f

    // The shown overlays and the screen and overlay sizes the pushes were worked out for, null when they were reset
    private var pushedOverlays: List<HudElement> = emptyList()
    private var pushedLayout: FloatArray? = null

    /**
     * In a screen overlays grow (hidden lines, buttons), so one that would cover an overlay placed before it (top to
     * bottom) is moved to the closest free spot on the screen, in any direction. It stays where it is when there is none.
     * The sizes are the ones of the last drawn frame. Only worked out again when an overlay or the screen changed size
     * or place, or a different set is shown.
     */
    private fun pushApart(shown: List<HudElement>) {
        val layout = FloatArray(shown.size * 4 + 2)
        shown.forEachIndexed { i, overlay ->
            layout[i * 4] = overlay.baseX
            layout[i * 4 + 1] = overlay.baseY
            layout[i * 4 + 2] = overlay.drawnWidth
            layout[i * 4 + 3] = overlay.drawnHeight
        }
        layout[shown.size * 4] = mc.window.guiScaledWidth.toFloat()
        layout[shown.size * 4 + 1] = mc.window.guiScaledHeight.toFloat()
        if (shown == pushedOverlays && layout.contentEquals(pushedLayout)) return
        pushedOverlays = shown
        pushedLayout = layout
        // Overlays no longer shown here aren't drawn, so they keep no old push
        overlays.forEach {
            it.pushX = 0f
            it.pushY = 0f
        }

        // left, top, right, bottom of the overlays already placed
        val placed = mutableListOf<FloatArray>()
        val maxRight = mc.window.guiScaledWidth.toFloat()
        val maxBottom = mc.window.guiScaledHeight.toFloat()
        for (overlay in shown.sortedWith(compareBy({ it.baseY }, { it.baseX }))) {
            val width = overlay.drawnWidth
            val height = overlay.drawnHeight
            val baseX = overlay.baseX
            val baseY = overlay.baseY
            fun free(x: Float, y: Float) = placed.none { it[0] < x + width && x < it[2] && it[1] < y + height && y < it[3] }

            var bestX = baseX
            var bestY = baseY
            if (!free(baseX, baseY)) {
                // The closest spot touches an edge of a placed overlay or the screen on each axis it was moved on
                val xs = listOf(baseX, 0f, maxRight - width) + placed.flatMap { listOf(it[0] - width - PUSH_GAP, it[2] + PUSH_GAP) }
                val ys = listOf(baseY, 0f, maxBottom - height) + placed.flatMap { listOf(it[1] - height - PUSH_GAP, it[3] + PUSH_GAP) }
                var bestDistance = Float.MAX_VALUE
                for (x in xs) {
                    if (x < 0f || x + width > maxRight) continue
                    for (y in ys) {
                        if (y < 0f || y + height > maxBottom) continue
                        val distance = (x - baseX) * (x - baseX) + (y - baseY) * (y - baseY)
                        if (distance < bestDistance && free(x, y)) {
                            bestDistance = distance
                            bestX = x
                            bestY = y
                        }
                    }
                }
            }
            overlay.pushX = bestX - baseX
            overlay.pushY = bestY - baseY
            placed += floatArrayOf(bestX, bestY, bestX + width, bestY + height)
        }
    }
}
