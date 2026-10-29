package net.sbo.mod.utils.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderPipelines
import net.sbo.guilib.fabric.image.Images
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.overlayData
import net.sbo.mod.utils.data.configs.overlay.OverlayValues
import java.awt.Color

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

    // Moved by this while a screen is open, where overlays grow (hidden lines, buttons) and could cover each
    // other, see OverlayManager.pushApart. The saved position is left as it is.
    internal var pushX = 0f
    internal var pushY = 0f

    /** The full name (hover of the eye) and the 1-3 letters on the collapsed bar. */
    var label: String = defaultLabel(name)
        private set
    var shortLabel: String = defaultShortLabel(label)
        private set

    // Where the eye was drawn this frame (GUI px), null while it isn't
    private var eyeBounds: FloatArray? = null

    /** Width and height on screen, at its scale (collapsed: the bar). */
    val drawnWidth: Float get() = if (isCollapsed() && !isEmpty()) collapsedWidth() else contentWidth() * scale
    val drawnHeight: Float get() = if (isCollapsed() && !isEmpty()) COLLAPSED_HEIGHT else contentHeight() * scale

    /** Where it is drawn without [pushX] and [pushY]. */
    val baseX: Float get() = x.coerceIn(0f, (mc.window.guiScaledWidth - drawnWidth).coerceAtLeast(0f))
    val baseY: Float get() = y.coerceIn(0f, (mc.window.guiScaledHeight - drawnHeight).coerceAtLeast(0f))

    // Shown but with nothing in it (like the pickup log without pickups): no eye or collapsed bar either
    private fun isEmpty(): Boolean = contentWidth() <= 0 || contentHeight() <= 0

    fun setLabel(label: String, shortLabel: String = defaultShortLabel(label)) {
        this.label = label
        this.shortLabel = shortLabel
    }

    /** Collapsed with its eye: in the inventory only a tiny bar with [shortLabel] is drawn, so it doesn't cover the items. */
    fun isCollapsed(): Boolean = isCraftingScreenOpen() && name in DataManager.sboData.collapsedOverlays

    private fun collapsedWidth(): Float = EYE_SIZE + 5f + mc.font.width(shortLabel)

    /** Draws it in the inventory: collapsed as the bar, else as usual with the eye next to it. */
    fun renderInInventory(drawContext: GuiGraphicsExtractor, mouseX: Double, mouseY: Double) {
        if (!checkCondition()) return
        if (!isCollapsed()) {
            render(drawContext, mouseX, mouseY)
            if (isEmpty()) return
            // Right of its top right corner, left of the top left one when that is off the screen
            val right = renderX + contentWidth() * scale + 1f
            val left = if (right + EYE_SIZE + 2f <= mc.window.guiScaledWidth) right else renderX - EYE_SIZE - 3f
            drawEye(drawContext, left, renderY, EYE_SIZE + 2f, false)
            return
        }
        if (isEmpty()) return
        val width = collapsedWidth()
        placeAt(width, COLLAPSED_HEIGHT)
        drawEye(drawContext, renderX, renderY, width, true)
        drawContext.text(mc.font, shortLabel, (renderX + EYE_SIZE + 3f).toInt(), (renderY + 1f).toInt(), Color(181, 183, 189).rgb, true)
    }

    // A dark box from [left] [width] wide with the eye icon at its start, open or [closed] (crossed out)
    private fun drawEye(drawContext: GuiGraphicsExtractor, left: Float, top: Float, width: Float, closed: Boolean) {
        val height = if (closed) COLLAPSED_HEIGHT else EYE_SIZE + 2f
        drawContext.fill(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt(), Color(14, 14, 16, 200).rgb)
        val color = if (closed) Color(107, 109, 116).rgb else Color(232, 233, 236).rgb
        drawIcon(drawContext, if (closed) EYE_CLOSED else EYE_OPEN, left + 1f, top + (height - EYE_SIZE) / 2f, EYE_SIZE, color)
        eyeBounds = floatArrayOf(left, top, left + width, top + height)
    }

    // The SVG at [src] [size] GUI px big, rasterized by GuiLib at the screen's real pixel size; [color] is its currentColor
    private fun drawIcon(drawContext: GuiGraphicsExtractor, src: String, x: Float, y: Float, size: Float, color: Int) {
        val entry = Images.entry(src) ?: return
        val pixels = Images.physical(size)
        val texture = Images.texture(entry, pixels, pixels, color) ?: return
        val pose = drawContext.pose()
        pose.pushMatrix()
        pose.translate(x, y)
        pose.scale(size / texture.width, size / texture.height)
        drawContext.blit(RenderPipelines.GUI_TEXTURED, texture.id, 0, 0, 0f, 0f, texture.width, texture.height, texture.width, texture.height, texture.width, texture.height, -1)
        pose.popMatrix()
    }

    /** Whether the eye (or the collapsed bar) drawn this frame is under the mouse. */
    fun isOverEye(mouseX: Double, mouseY: Double): Boolean {
        val bounds = eyeBounds ?: return false
        return mouseX >= bounds[0] && mouseX < bounds[2] && mouseY >= bounds[1] && mouseY < bounds[3]
    }

    internal fun hideEye() {
        eyeBounds = null
    }

    fun toggleCollapsed() {
        val collapsed = DataManager.sboData.collapsedOverlays
        if (!collapsed.remove(name)) collapsed.add(name)
    }

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

    protected fun updateRenderPosition(width: Int, height: Int) = placeAt(width * scale, height * scale)

    // [width] and [height] on screen
    private fun placeAt(width: Float, height: Float) {
        val maxX = (mc.window.guiScaledWidth - width).coerceAtLeast(0f)
        val maxY = (mc.window.guiScaledHeight - height).coerceAtLeast(0f)
        renderX = (x.coerceIn(0f, maxX) + pushX).coerceIn(0f, maxX)
        renderY = (y.coerceIn(0f, maxY) + pushY).coerceIn(0f, maxY)
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

    companion object {
        private const val EYE_SIZE = 8f
        private const val COLLAPSED_HEIGHT = 10f
        private const val EYE_OPEN = "sbo:ui/overlay/eye.svg"
        private const val EYE_CLOSED = "sbo:ui/overlay/eye-off.svg"

        // "bobberOverlay" -> "Bobber"
        private fun defaultLabel(name: String): String =
            name.removeSuffix("Overlay").replace(Regex("([a-z])([A-Z])"), "$1 $2").split(' ')
                .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

        // "Diana Loot" -> "DL", a single word -> its first letters
        private fun defaultShortLabel(label: String): String {
            val words = label.split(' ').filter { it.isNotEmpty() }
            return if (words.size == 1) words[0].take(3) else words.take(3).joinToString("") { it.take(1).uppercase() }
        }
    }
}
