package net.sbo.mod.test

import net.sbo.guilib.core.UiRoot
import net.sbo.guilib.core.css.Origin
import net.sbo.guilib.core.css.Stylesheet
import net.sbo.guilib.core.dom.VComponent
import net.sbo.guilib.core.dsl.ComponentScope
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.paint.PaintCommand
import java.nio.file.Files

/** Actual GuiLib layout/paint without a client, app initialization, data store or callbacks. */
internal class StandaloneReviewFrame(screen: String, css: String, override val width: Float, override val height: Float,
    theme: String = "sbo-dark", content: ComponentScope.() -> Unit) : ReviewFrame {
    companion object {
        fun css(screen: String) = Files.readString(ReviewResources.project.resolve("src/main/resources/assets/sbo/ui/$screen/$screen.css"))
        fun before(screen: String) = Files.readString(ReviewResources.project.resolve("tools/ui-review/baseline/$screen-before.css"))
        private fun sheet(path: String, origin: Origin = Origin.AUTHOR) = ReviewResources.resource(path).bufferedReader().use { Stylesheet.parse(it.readText(), path, origin) }
    }
    override val fonts = ReviewFonts()
    private var now = 0L
    val root = UiRoot(fonts, listOf(sheet("/assets/guilib/css/ua.css", Origin.USER_AGENT), sheet("/assets/sbo/ui/common.css"),
        Stylesheet.parse(css, "$screen.css"), sheet("/assets/sbo/ui/themes/themes.css")), clock = { now })
    init { root.render(VComponent(component("Offline $screen") { content() }, Unit, null)); root.document.body.classList.add("sbo-theme-$theme"); settle() }
    override fun settle(): List<PaintCommand> { var commands = emptyList<PaintCommand>(); repeat(5) { now += 250; commands = root.frame(width, height) }; return commands }
    fun all(selector: String) = root.document.body.querySelectorAll(selector)
    fun element(selector: String) = requireNotNull(root.document.body.querySelector(selector)) { selector }
    fun click(selector: String) { val b = element(selector).getBoundingClientRect(); root.input.mouseDown(b.x+b.width/2,b.y+b.height/2,0); root.input.mouseUp(b.x+b.width/2,b.y+b.height/2,0); settle() }
    fun key(key: String) { root.input.keyDown(key,0); root.input.keyUp(key,0); settle() }
    fun scrollTo(selector: String, container: String) { val c = element(container); c.scrollTop += element(selector).getBoundingClientRect().y - c.getBoundingClientRect().y - 4; settle() }
}
