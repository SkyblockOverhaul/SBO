package net.sbo.mod.test

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import net.sbo.mod.guis.look.useFitScale
import java.nio.file.Files
import kotlin.math.sin

/** Source-derived copy and real GuiLib controls/fit scaling; all account, API and save effects inert.
 * Preview glyph colors/motion are deterministic specimens, not the live SboBadges renderer. */
internal class BadgeReviewFixture(css:String=StandaloneReviewFrame.css("badge"),width:Float=640f,height:Float=480f,theme:String="sbo-dark",initialTab:String="places",state:String="ready",initialPart:String="name",phase:Long=0L) {
    companion object {
        val out get()=ReviewResources.project.resolve("build/ui-review/badge")
        private val source=Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/guis/BadgeGui.kt"))
        val tabNames=Regex("tab\\(TAB_([A-Z]+), \"([^\"]+)\"").findAll(source).map{it.groupValues[1].lowercase() to it.groupValues[2]}.toList()
        private fun map(name:String,next:String,prefix:String)=Regex("$prefix\\.([A-Z_]+) to \"([^\"]+)\"").findAll(source.substringAfter("private val $name =").substringBefore(next)).associate{it.groupValues[1].lowercase() to it.groupValues[2]}
        val effects=map("EFFECT_LABELS","private val EFFECT_HINTS","BadgeEffect")
        val effectHints=map("EFFECT_HINTS","private fun NodeBuilder.statTab","BadgeEffect")
        val motions=map("MOTION_LABELS","private val MOTION_HINTS","BadgeMotion")
        val motionHints=map("MOTION_HINTS","private val EFFECT_LABELS","BadgeMotion")
        val viewRows=Regex("setting\\(\"([^\"]+)\", \"([^\"]+)\"").findAll(source.substringAfter("private val ViewSettings").substringBefore("private fun NodeBuilder.failed")).map{it.groupValues[1] to it.groupValues[2]}.toList()
        val ownView=Regex("setting\\(\"([^\"]+)\", \"([^\"]+)\"").find(source.substringAfter("private val OwnView"))!!.let{it.groupValues[1] to it.groupValues[2]}
        val parts=listOf("stat" to "Stat","tag" to "Tag","level" to "Level","rank" to "Rank","name" to "Name")
        val stats=listOf("none" to "Logo only","chimera" to "Chimeras","long" to "Lifetime Diana Mythological Creature Kills")
    }
    var actionCount=0;private set
    var tabValue=initialTab;private set
    var changedValue=false;private set
    private val app=component("Offline Badge") {
        var tab by useState(initialTab)
        var picked by useState(initialPart)
        var changed by useState(state=="unsaved")
        var leaving by useState(state=="unsaved")
        var gradient by useState(true)
        var flow by useState(false)
        var color by useState(0xFF0AB4C7.toInt())
        var end by useState(0xFFFFD966.toInt())
        var effect by useState("wave")
        var motion by useState("wavy")
        var stat by useState("chimera")
        var checks by useState((0..14).filter{it%3!=2}.toSet())
        var toggles by useState(setOf(0,1,2,4,5))
        tabValue=tab;changedValue=changed
        val saving=state=="saving"
        useEscapeBack(changed&&!saving){leaving=true}
        fun NodeBuilder.setting(title:String,text:String,control:NodeBuilder.()->Unit) {
            div(className="bd-setting") {div(className="bd-setting-text"){div(className="bd-setting-title"){+title};div(className="bd-hint"){+text}};control()}
        }
        fun NodeBuilder.viewer(own:Boolean) {
            if(own)setting(ownView.first,ownView.second){switch(checked=0 in toggles,onChange={toggles=if(it.checked)toggles+0 else toggles-0})}
            div(className="bd-section-title"){+"Badges of Others"}
            viewRows.forEachIndexed{i,(label,hint)->setting(label,hint){switch(checked=i+1 in toggles,onChange={toggles=if(it.checked)toggles+(i+1)else toggles-(i+1)})}}
        }
        fun NodeBuilder.picker() {
            div(className="bd-picker-row") {
                segmented(value=if(flow)"line"else picked,onChange={picked=it},className="bd-parts") {
                    if(flow){option("stat","Stat");option("line","Name Line")}else parts.forEach{option(it.first,it.second)}
                }
                div(className="bd-spacer");span(className="bd-hint"){+"Flow line"};switch(checked=flow,onChange={flow=it.checked;changed=true})
            }
        }
        fun NodeBuilder.paint(text:String,moving:Boolean=false) {
            text.forEachIndexed{i,c->
                val t=((i.toFloat()/text.length+phase/3000f)%1f)
                val a=color and 0xFFFFFF;val b=end and 0xFFFFFF
                fun channel(shift:Int)=(((a shr shift and 255)*(1-t)+(b shr shift and 255)*t).toInt())
                val rgb=(channel(16)shl 16)or(channel(8)shl 8)or channel(0)
                span(style="color:#%06X;".format(rgb)+(if(moving)"transform:translateY(${(sin(i*.7+phase/300.0)+1).toInt()}px);"else"")){+c.toString()}
            }
        }
        fun NodeBuilder.preview() {
            div(className="bd-preview-row") {
                div(className="bd-preview"+if(state=="hidden")" bd-hidden"else"") {
                    div(className="bd-tag"){img("sbo:textures/font/badge.png",className="bd-tag-logo");span(className="bd-pick",onClick={picked="stat";tab="colors"}){paint("Chimeras: 1,234")}}
                    div(className="bd-tag bd-tag-name"){span(className="bd-pick bd-picked",onClick={tab="colors"}){paint("[SBO Dev] [465] [MVP+] SyntheticPlayer",moving=true)}}
                }
                div(className="bd-lines") {for(label in listOf("Tab","Chat"))div(className="bd-line"){
                    span(className="bd-line-label"){+label};div(className="bd-line-box "+if(label=="Tab")"bd-line-tab"else"bd-line-chat"){
                        span(className="bd-pick",onClick={tab="colors"}){paint("[465] [MVP+] [SBO Dev] SyntheticPlayer")};img("sbo:textures/font/badge.png",className="bd-tag-logo bd-line-logo");if(label=="Chat")span(className="bd-tag-plain"){+": Hello!"}
                    }
                }}
            }
            div(className="bd-hint bd-status"){+if(state=="hidden")"Hidden by the SBO server, nobody sees your badge right now."else"Click a part to change its colors."}
        }
        val fit=useFitScale()
        div(className="bd-window",ref=fit.ref,style=fit.style) {
            header(className="bd-header"){img("sbo:textures/font/badge.png",className="bd-header-logo");span(className="bd-title"){+"SBO Badge"};div(className="bd-spacer");button(className="sbo-close",onClick={if(changed)leaving=true}){+"×"}}
            div(className="bd-body") {
                when(state) {
                    "loading"->{div(className="bd-center"){+"Loading your badge..."};viewer(false)}
                    "error","ineligible"->div(className="bd-columns") {
                        div(className="bd-col-left") {
                            if(state=="error")div(className="bd-error"){+"Could not reach the SBO server: synthetic offline error"}
                            else {div(className="bd-hint bd-intro"){+"Supporters (Patreon or Ko-fi), maintainers and moderators get their own badge: the SBO logo, a stat of their choice and their name in their color."};div(className="bd-steps"){div(className="bd-steps-title"){+"Already a supporter? Check these:"};listOf("1. Your Minecraft account is linked to your Discord account: type /link followed by your Minecraft name on the SBO Discord.","2. Use the Discord account that has the Patreon or Ko-fi supporter role, or the maintainer or moderator role.","3. Open this window again with /sbobadge. New roles can take a few minutes.").forEach{div(className="bd-info-line"){+it}}}}
                        };div(className="bd-col-right"){viewer(false)}
                    }
                    else->{
                        div(className="bd-rank-row") {
                            span(className="bd-rank",style="background:linear-gradient(90deg,#0AB4C7,#FFD966)"){+"Developer"};span(className="bd-hint"){+"Thank you for working on SBO!"};div(className="bd-spacer")
                            button(className="sbo-button",id="badge-undo",disabled=saving||!changed,onClick={changed=false;color=0xFF0AB4C7.toInt();end=0xFFFFD966.toInt();gradient=true;effect="wave";motion="wavy";stat="chimera";checks=(0..14).filter{it%3!=2}.toSet();flow=false}){+"Undo"}
                            button(className="sbo-button bd-primary",id="badge-save",disabled=saving||!changed,onClick={actionCount++;changed=false}){+if(saving)"Saving..."else"Save"}
                        }
                        preview()
                        tabs(value=tab,onChange={tab=it},className="bd-tabs"){tabNames.forEach{tab(it.first,it.second)}}
                        scroll(className="bd-panel guilib-autohide",key="panel:$tab") {
                            when(tab) {
                                "places"->div(className="bd-matrix") {
                                    div(className="bd-mrow bd-mhead"){span(className="bd-mlabel"){+"Where every part shows"};listOf("Head","Tab","Chat").forEach{span(className="bd-mcell"){+it}}}
                                    listOf("Logo + stat","Team tag","Level color","Rank color","Name color").forEachIndexed{i,label->div(className="bd-mrow"+if(picked==parts[i].first)" bd-mrow-picked"else"",onClick={picked=parts[i].first}){
                                        div(className="bd-mlabel"){span(className="bd-mswatch",style="background:linear-gradient(90deg,#0AB4C7,#FFD966)");span{+label}}
                                        repeat(3){j->val id=i*3+j;div(className="bd-mcell"){checkbox(checked=id in checks,onChange={checks=if(it.checked)checks+id else checks-id;changed=true})}}
                                    }}
                                }
                                "colors"->{
                                    picker()
                                    setting("Color",if(picked=="rank")"Your Hypixel rank, like [MVP+], where Hypixel shows it."else"Your name where it is colored.") {div(className="bd-colors"){colorInput(value=color,onChange={color=it;changed=true});if(gradient){span(className="bd-arrow"){+"→"};colorInput(value=end,onChange={end=it;changed=true})}}}
                                    setting("Gradient","A second color that the first one flows into."){switch(checked=gradient,onChange={gradient=it.checked;if(!gradient&&effect=="wave")effect="static";changed=true})}
                                    if(picked in listOf("rank","level","tag"))setting("Brackets","The [ and ] around the ${picked.lowercase()}."){div(className="bd-colors"){span(className="bd-arrow"){+"["};colorInput(value=color,onChange={color=it;changed=true});span(className="bd-arrow"){+"]"};colorInput(value=end,onChange={end=it;changed=true})}}
                                    if(picked=="rank")setting("Plus","The + of VIP+, MVP+ and MVP++."){colorInput(value=end,onChange={end=it;changed=true})}
                                    setting("All parts","Gives every part these colors and this effect; brackets and plus stay."){button(className="sbo-button",onClick={changed=true}){+"Use for all"}}
                                }
                                "effects"->{
                                    picker()
                                    setting("Effect",effectHints.getValue(effect)){select(value=effect,onChange={effect=it.value;if(effect=="wave")gradient=true;changed=true},className="bd-select"){effects.forEach{option(it.key,it.value)}}}
                                    setting("Motion",motionHints.getValue(motion)){select(value=motion,onChange={motion=it.value;changed=true},className="bd-select"){motions.forEach{option(it.key,it.value)}}}
                                    div(className="bd-hint bd-note"){+"Motion only shows above heads. Chat lines stay as they came in, so chat shows colors standing still. Others can turn animations off for themselves."}
                                }
                                "stat"->setting("Stat","Shown above your head next to the logo, read from your SkyBlock profile."){select(value=stat,onChange={stat=it.value;changed=true},className="bd-select",searchable=true,searchPlaceholder="Search..."){stats.forEach{option(it.first,it.second)}}}
                                else->viewer(true)
                            }
                        }
                    }
                }
            }
        }
        modal(open=leaving,onClose={leaving=false},className="bd-modal") {
            div(className="bd-modal-title"){+"Leave without saving?"};div(className="bd-modal-text"){+"Your changes to the badge are not saved yet."}
            div(className="bd-modal-buttons"){button(className="sbo-button",id="badge-keep",onClick={leaving=false}){+"Keep editing"};button(className="sbo-button bd-danger",onClick={leaving=false}){+"Discard"};button(className="sbo-button bd-primary",disabled=saving,onClick={actionCount++;leaving=false;changed=false}){+"Save"}}
        }
    }
    val frame=StandaloneReviewFrame("badge",css,width,height,theme){app(Unit)}
}
