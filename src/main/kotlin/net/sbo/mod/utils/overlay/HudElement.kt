package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.data.DataManager.overlayData
import net.sbo.mod.utils.data.configs.overlay.OverlayValues

/**
 * Something on the HUD that /sboguis can move and scale: an [Overlay] of text lines or a [GuiLibOverlay].
 * Keeps the saved position, keeps it on the screen and runs the render conditions.
 * @property allowedScreens The screens it is also drawn in (and clickable in), besides no screen.
 */
abstract class HudElement(
    var name: String,
    var x: Float,
    var y: Float,
    var scale: Float,
    val allowedScreens: List<(screen: Screen?) -> Boolean>
) {
    var selected: Boolean = false

    // The position it was made with, for /sboresethudpositions
    private val defaultX = x
    private val defaultY = y

    protected var condition: () -> Boolean = { true }
    private var cachedCondition = true

    protected var extraCondition: () -> Boolean = { true }
    private var cachedExtraCondition = true

    // Where it is drawn: its position, moved back onto the screen if it would be partly off it (like a modpack
    // config made for another resolution). The saved position is left as it is.
    protected var renderX = x
        private set
    protected var renderY = y
        private set

    /** Width in GUI px at scale 1. */
    protected abstract fun contentWidth(): Int

    /** Height in GUI px at scale 1. */
    protected abstract fun contentHeight(): Int

    abstract fun render(drawContext: GuiGraphicsExtractor, mouseX: Double, mouseY: Double)

    open fun overlayClicked(mouseX: Double, mouseY: Double) {}

    fun init() {
        loadPosition()
        OverlayManager.overlays.add(this)
    }

    // Reads the stored position, or stores the current one if missing
    fun loadPosition() {
        val data = overlayData.overlays[name]
        if (data != null) {
            x = data.x
            y = data.y
            scale = data.scale
        } else {
            overlayData.overlays[name] = OverlayValues(x, y, scale)
        }
    }

    fun resetPosition() {
        x = defaultX
        y = defaultY
        val values = overlayData.overlays[name]
        if (values != null) {
            values.x = defaultX
            values.y = defaultY
        } else {
            overlayData.overlays[name] = OverlayValues(defaultX, defaultY, scale)
        }
    }

    open fun updateConditions() {
        cachedCondition = condition()

        // When on the editing screen, show overlays even if extra condition is not met. Some overlays can e.g. only render whilst in The Hub, but the user needs to be able to edit it's position outside of The Hub as well. The main condition() checks for Config toggle (we don't show these on overlay edit screen), while extraCondition() checks for these dynamic toggles like being in The Hub.
        cachedExtraCondition = extraCondition() || inEditingScreen()
    }

    fun checkCondition(): Boolean = cachedCondition && cachedExtraCondition

    protected fun inEditingScreen(): Boolean = mc.screen is OverlayEditScreen

    fun isOverOverlay(mouseX: Double, mouseY: Double): Boolean = isOverOverlay(mouseX, mouseY, contentWidth(), contentHeight())

    protected fun isOverOverlay(mouseX: Double, mouseY: Double, width: Int, height: Int): Boolean {
        if (!checkCondition()) return false
        val totalWidth = width * scale
        val totalHeight = height * scale

        return mouseX >= renderX && mouseX <= renderX + totalWidth && mouseY >= renderY && mouseY <= renderY + totalHeight
    }

    protected fun updateRenderPosition(width: Int, height: Int) {
        val maxX = (mc.window.guiScaledWidth - width * scale).coerceAtLeast(0f)
        val maxY = (mc.window.guiScaledHeight - height * scale).coerceAtLeast(0f)
        renderX = x.coerceIn(0f, maxX)
        renderY = y.coerceIn(0f, maxY)
    }

    /** Moves the saved position onto the screen, so it can't be moved off it while editing. */
    fun clampPosition() {
        updateRenderPosition(contentWidth(), contentHeight())
        x = renderX
        y = renderY
        overlayData.overlays[name]?.let {
            it.x = x
            it.y = y
            it.scale = scale
        }
    }
}
