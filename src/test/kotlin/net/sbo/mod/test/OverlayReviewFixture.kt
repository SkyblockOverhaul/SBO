package net.sbo.mod.test

import net.sbo.guilib.core.dsl.*
import net.sbo.mod.utils.overlay.overlayReadoutPosition
import java.nio.file.Files

/** Diagnostic canvas. Native Minecraft glyphs are approximated with monospace;
 * no HudElement/OverlayManager instances or user overlay data are constructed. */
internal class OverlayReviewFixture(width:Float=640f,height:Float=360f,edge:Boolean=false) {
    companion object {
        val out get()=ReviewResources.project.resolve("build/ui-review/overlay-editor")
        private val source=Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/utils/overlay/OverlayEditorStyle.kt"))
        private fun color(name:String):String {
            val value=Regex("const val $name = 0x([0-9A-Fa-f]+)").find(source)!!.groupValues[1].toLong(16)
            return "rgba(${value shr 16 and 255},${value shr 8 and 255},${value and 255},${(value shr 24 and 255)/255.0})"
        }
        private val css="""
            body { display:block; }
            .ov-canvas { position:relative; width:100vw; height:100vh; background-color:${color("BACKGROUND")}; font-family:minecraft; font-size:9px; color:#DBDEE1; }
            .ov-item { position:absolute; }
            .ov-hover { background-color:${color("HOVER")}; }
            .ov-selected { outline:1px solid ${color("SELECTED")}; }
            .ov-readout { position:absolute; padding:3px 4px; height:15px; background-color:${color("PANEL")}; color:${color("TEXT")}; white-space:nowrap; }
            .ov-line { height:10px; }
            .ov-guilib { display:flex; gap:6px; background-color:#25262A; padding:4px; }
        """.trimIndent()
    }
    data class Item(val left:Int,val top:Int,val width:Int,val height:Int,val scale:Float,val selected:Boolean,val hovered:Boolean)
    val selected=if(edge)Item((width-145).toInt(),0,145,32,1.5f,true,true)else Item(28,45,150,32,1f,true,false)
    private val label="X: ${selected.left} Y: ${selected.top} Scale: ${String.format(java.util.Locale.ROOT,"%.1f",selected.scale)}"
    // Native uses font.width + 8 and lineHeight + 6. 5.4px monospace approximates its 9px glyphs.
    val labelWidth=kotlin.math.ceil(label.length*5.4).toInt()+8
    val labelHeight=15
    val readout=overlayReadoutPosition(selected.left,selected.top,selected.height,labelWidth,labelHeight,width.toInt(),height.toInt())
    val frame=StandaloneReviewFrame("overlay-review",css,width,height) {
        div(className="ov-canvas") {
            div(className="ov-item ov-selected"+if(selected.hovered)" ov-hover"else"",style="left:${selected.left}px;top:${selected.top}px;width:${selected.width}px;height:${selected.height}px"){
                div(className="ov-line",style="color:#FFAA00"){+"Diana Tracker"};div(className="ov-line"){+"Chimeras: 1,234"};div(className="ov-line"){+"Inquisitors: 57"}
            }
            div(className="ov-item ov-guilib ov-hover",style="left:24px;top:${height-60}px;width:150px;height:30px") {span(style="color:#55FFFF"){+"Party"};span{+"4 / 5 players"}}
            div(className="ov-item",style="left:${width-155}px;top:${height-42}px;width:145px;height:22px") {div(className="ov-line"){+"Burrows: 1,234"};div(className="ov-line"){+"Profit: 123.4M"}}
            div(className="ov-readout",style="left:${readout.first}px;top:${readout.second}px;width:${labelWidth}px"){+label}
        }
    }
}
