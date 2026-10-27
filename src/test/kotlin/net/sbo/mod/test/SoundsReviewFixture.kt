package net.sbo.mod.test

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import java.nio.file.Files

/** Real GuiLib controls with source-derived sound labels and disposable values.
 * Never constructs SoundsGui, SoundHandler, settings, files or audio callbacks. */
internal class SoundsReviewFixture(css:String=StandaloneReviewFrame.css("sounds"),width:Float=640f,height:Float=480f,theme:String="sbo-dark") {
    companion object {
        val out get()=ReviewResources.project.resolve("build/ui-review/sounds")
        private val source=Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/guis/SoundsGui.kt"))
        val groups=Regex("\"([^\"]+)\" to listOf\\(([\\s\\S]*?)(?=\\n        \\),)").findAll(source.substringAfter("private val GROUPS").substringBefore("private val App")).map { m ->
            m.groupValues[1] to Regex("SoundSetting\\(\"([^\"]+)\"").findAll(m.groupValues[2]).map{it.groupValues[1]}.toList()
        }.toList()
        val filenames=listOf("inquisitor.ogg","custom_alert_for_rare_drops_with_a_very_long_filename.wav","bell.mp3")
        const val missing="removed_custom_sound_with_a_long_name.aiff"
    }
    var testCount=0;private set
    var otherActions=0;private set
    private val row=component<Pair<String,Int>>("Offline sound row") { (label,index) ->
        var sound by useState(when(index%4){0->filenames[1];1->"";2->missing;else->filenames[0]})
        var volume by useState(if(index%3==0)100 else if(index%3==1)0 else 50)
        div(className="snd-row") {
            span(className="snd-label",title=label){+label}
            select(value=sound,onChange={sound=it.value},searchable=true,searchPlaceholder="Search sounds",className="snd-sound") {
                option("","(None)")
                if(sound==missing)option(missing,missing,title="Not in the sound folder anymore")
                filenames.forEach{option(it,it)}
            }
            slider(value=volume,onChange={volume=it},showValue=true,format={"$it%"},className="snd-volume")
            button(className="snd-test sbo-button",disabled=sound.isEmpty(),title=if(sound.isEmpty())"Pick a sound first"else"Plays the sound at this volume",onClick={testCount++}){+"Test"}
        }
    }
    private val app=component("Offline Sounds") {
        var master by useState(80)
        var size by useState("global")
        fun NodeBuilder.setting(title:String,text:String,control:NodeBuilder.()->Unit) {
            div(className="snd-setting") {
                div(className="snd-setting-text") {div(className="snd-setting-title"){+title};div(className="snd-hint"){+text}}
                control()
            }
        }
        div(className="snd-window") {
            header(className="snd-header") {span(className="snd-title"){+"Sounds"};div(className="snd-spacer");button(className="snd-close sbo-close",title="Close"){+"×"}}
            scroll(className="snd-body guilib-autohide") {
                h2(className="snd-section"){+"General"}
                setting("Master Volume","Applies to every sound below.") {slider(value=master,onChange={master=it},showValue=true,format={"$it%"},className="snd-volume")}
                setting("Size","How big the sound window is. Auto uses your Minecraft GUI scale.") {select(value=size,onChange={size=it.value},className="snd-scale-select"){option("global","Global");option("auto","Auto");option("2","2")}}
                setting("Sound Folder","Put your own sounds in here (.ogg, .mp3, .wav, .au, .aif, .aiff), then reload the list.") {
                    div(className="snd-buttons") {button(className="sbo-button",onClick={otherActions++}){+"Open Folder"};button(className="sbo-button",title="Looks for new files in the sound folder",onClick={otherActions++}){+"Reload"}}
                }
                var index=0
                groups.forEach{(title,labels)->h2(className="snd-section",id="sounds-section-"+title.replace(" ","-")){+title};labels.forEach{row(it to index++,key=it)}}
            }
        }
    }
    val frame=StandaloneReviewFrame("sounds",css,width,height,theme){app(Unit)}
}
