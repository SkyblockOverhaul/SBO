package net.sbo.mod.test

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.*
import java.nio.file.Files

/** Source-matched Cloud UI specimens. No CloudSync, keys, backups or DataManager is initialized. */
internal class CloudReviewFixture(css:String=StandaloneReviewFrame.css("cloud"),width:Float=640f,height:Float=480f,theme:String="sbo-dark",view:String="main",status:String="conflict") {
    companion object {
        val out get()=ReviewResources.project.resolve("build/ui-review/cloud")
        private val source=Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/guis/CloudSyncGui.kt"))
        val info=Regex("infoLine\\(\"([^\"]+)\"\\)").findAll(source.substringAfter("details(\"How it works\"").substringBefore("modal(open = confirmDelete")).map{it.groupValues[1]}.toList()
        val statuses=Regex("SyncState\\.([A-Z_]+) -> \"([^\"]+)\" to \"([^\"]+)\"").findAll(source.substringAfter("private fun NodeBuilder.statusCard")).associate{it.groupValues[1] to(it.groupValues[2] to it.groupValues[3])}
        data class Field(val label:String,val pc:String?,val cloud:String?)
        val fields=listOf(Field("General › Bobber Overlay","On","Off"),Field("Diana › Hide Own Waypoints","Normal, Inquisitor, Sphinx, Manticore","Inquisitor"),Field("Customization › Inquisitor Waypoint Color","#55FF55CC","#FFAA00FF"),Field("Mobs Since Inq","1,234","57"),Field("New Field",null,"A new value"))
    }
    var actionCount=0;private set
    var picks=emptyMap<String,String>();private set
    private val app=component("Offline Cloud") {
        var page by useState(if(view=="restore")"backups"else if(view in listOf("compare","backups","empty-backups","empty-compare","loading","failure"))view else "main")
        var dialog by useState(view.takeIf{it in listOf("delete","restore")})
        var choices by useState(emptyMap<String,String>())
        var auto by useState(true)
        var key by useState("synthetic-demo-key")
        var showKey by useState(false)
        var size by useState("global")
        val busy=status=="busy"
        picks=choices
        useEscapeBack(page!="main"){page="main"}
        fun NodeBuilder.header(title:String,back:Boolean) {
            header(className="cs-header") {
                if(back)button(className="sbo-button cs-icon",title="Back",onClick={page="main"}){+"←"}
                span(className="cs-title"){+title};div(className="cs-spacer")
                if(!back)button(className="sbo-button cs-header-button",onClick={page="backups"}){+"Backups"}
                span(className="cs-size-label"){+"Size"}
                select(value=size,onChange={size=it.value},className="cs-size"){option("global","Global");option("auto","Auto");option("2","2")}
                button(className="sbo-close",title="Close"){+"×"}
            }
        }
        fun NodeBuilder.pick(id:String,a:String,b:String) {
            segmented(value=choices[id]?:"pc",onChange={choices=choices+(id to it)},className="cs-pick") {option("pc",a,title="This PC: $a");option("cloud",b,title="Cloud: $b")}
        }
        div(className="cs-window"+if(page!="main")" cs-wide"else"") {
            header(when(page){"compare","empty-compare","loading","failure"->"Compare";"backups","empty-backups"->"Backups";else->"Cloud Sync"},page!="main")
            when(page) {
                "compare" -> {
                    scroll(className="cs-body guilib-autohide") {
                        div(className="cs-compare-head") {span(className="cs-count"){+"10 differences"};div(className="cs-spacer");span(className="cs-hint"){+"Left is this PC, right is the cloud. Click what to keep."}}
                        div(className="cs-warning"){+"This cloud save has no sign key, so SBO cannot check that it really comes from you."}
                        for((index,title)in listOf("Settings","SBO Data","Diana Tracker").withIndex()) {
                            details(summary={span(className="cs-area-title"){+title};span(className="cs-area-count"){+(if(index==2)"5"else if(index==0)"3"else"2")};div(className="cs-spacer");if(choices.values.any{it=="cloud"})span(className="cs-area-cloud"){+"from cloud"}},defaultOpen=true,className="cs-area"+if(index<2)" cs-perfield"else"") {
                                if(index==2) {
                                    div(className="cs-field cs-whole"){span(className="cs-field-label"){+"Keep"};pick("tracker","This PC","Cloud")}
                                    div(className="cs-field cs-columns"){span{+"Differences"};span{+"This PC"};span{+"Cloud"}}
                                    for((label,a,b)in listOf(Triple("Items › CHIMERA","1,234","57"),Triple("Mobs › MINOS_INQUISITOR","123,456","1,234")))div(className="cs-field"){span(className="cs-field-label",title=label){+label};span(className="cs-value",title="This PC: $a"){+a};span(className="cs-value",title="Cloud: $b"){+b}}
                                    div(className="cs-hint cs-more"){+"and 3 more"}
                                } else for(field in if(index==0)fields.take(3)else fields.takeLast(2))div(className="cs-field") {
                                    span(className="cs-field-label",title=field.label){+field.label};pick(field.label,field.pc?:"(not set)",field.cloud?:"(not set)")
                                }
                            }
                        }
                    }
                    div(className="cs-footer") {
                        button(className="sbo-button",id="all-pc",onClick={choices=emptyMap()}){+"All from PC"}
                        button(className="sbo-button",id="all-cloud",onClick={choices=(fields.map{it.label}+"tracker").associateWith{"cloud"}}){+"All from Cloud"}
                        div(className="cs-spacer");button(className="sbo-button cs-primary",disabled=busy,onClick={actionCount++}){+"Save choice"}
                    }
                }
                "backups","empty-backups" -> scroll(className="cs-body guilib-autohide") {
                    div(className="cs-hint cs-backups-hint"){+"SBO makes a backup every time you close the game. The newest 10 are kept."}
                    if(page=="empty-backups")div(className="cs-center cs-empty"){+"No backups yet. SBO makes one when you close the game."}
                    else repeat(4){i->div(className="cs-backup"){
                        div(className="cs-backup-text"){div(className="cs-backup-date"){+"2026-10-${27-i} 14:30"};div(className="cs-hint"){+"${i+1} h ago · ${1234+i*500} KB"}}
                        button(className="sbo-button",onClick={dialog="restore"}){+"Load"}
                    }}
                }
                "empty-compare","loading","failure" -> div(className="cs-body cs-center") {div(className=if(page=="failure")"cs-error"else null){+when(page){"loading"->"Loading your cloud save...";"failure"->"Could not load the cloud save. Please try again.";else->"No differences. This PC and your cloud save are the same."}}}
                else -> scroll(className="cs-body guilib-autohide") {
                    val (tone,text)=when(status){"busy"->"idle" to "Uploading...";"no-save"->statuses.getValue("NO_SAVE");"supporter"->"warn" to "Only for SBO supporters";"error"->"bad" to "Could not check your cloud save. Please try again.";else->statuses.getValue("BOTH_CHANGED")}
                    div(className="cs-status $tone") {div(className="cs-dot");div(className="cs-status-text"){div(className="cs-status-title"){+text};if(status!="no-save")div(className="cs-hint"){+"Last upload: 2026-10-27 14:30"}};button(className="sbo-button cs-icon cs-refresh",disabled=busy,title="Checks your cloud save again"){span(className="cs-refresh-icon"){+"↻"}}}
                    if(status=="supporter") {
                        div(className="cs-steps"){div(className="cs-steps-title"){+"Cloud Sync is for supporters"};div(className="cs-info-line"){+"Cloud Sync keeps a copy of your SBO settings and trackers on our server, so you can use them on every PC. Storing and syncing this for every player needs a lot of server power. Donations from our supporters pay for the server and help us make it better, so Cloud Sync is only for supporters."};div(className="cs-info-line"){+"To become a supporter, support SBO on Patreon or Ko-fi, or boost the SBO Discord."};div(className="cs-buttons cs-support"){button(className="sbo-button cs-primary"){+"Patreon"};button(className="sbo-button"){+"Ko-fi"}}}
                        div(className="cs-steps"){div(className="cs-steps-title"){+"Already a supporter? Check these:"};listOf("1. Your Minecraft account is linked to your Discord account: type /link followed by your Minecraft name on the SBO Discord.","2. Use the Discord account that has the supporter role or boosts the server.","3. Open this window again with /sbocloud. New supporter roles can take a few minutes.").forEach{div(className="cs-info-line"){+it}}}
                    } else {
                        if(status=="conflict")div(className="cs-question") {
                            div(className="cs-question-top"){div(className="cs-question-text"){+"This PC and your cloud save both changed. Which data do you want to keep?"};button(className="sbo-close"){+"×"}}
                            div(className="cs-buttons"){button(className="sbo-button cs-danger",onClick={actionCount++}){+"Keep this PC"};button(className="sbo-button",onClick={actionCount++}){+"Use cloud save"};button(className="sbo-button",onClick={page="compare"}){+"Compare"}}
                        }
                        div(className="cs-actions") {
                            button(className="sbo-button cs-primary",disabled=busy,onClick={actionCount++}){+"Upload"};button(className="sbo-button",disabled=busy||status=="no-save",onClick={actionCount++}){+"Download"};button(className="sbo-button",disabled=busy||status=="no-save",onClick={page="compare"}){+"Compare"};div(className="cs-spacer");button(className="sbo-button cs-danger",disabled=busy||status=="no-save",onClick={dialog="delete"}){+"Delete"}
                        }
                        div(className="cs-setting"){div(className="cs-setting-text"){div(className="cs-setting-title"){+"Auto Sync"};div(className="cs-hint"){+"Saves and loads for you."}};switch(checked=auto,onChange={auto=it.checked})}
                        div(className="cs-setting cs-key") {
                            tooltip(content={div{+"Signs your cloud save. If it gets changed or damaged, SBO notices and does not load it."};div(className="cs-tip-line"){+"Optional: only set one if you really want your cloud save signed. Without a key, Cloud Sync works just the same."};div(className="cs-tip-line"){+"Only PCs or instances with the same key can load it."}}){span(className="cs-setting-title"){+"Sign Key"}}
                            div(className="cs-key-row"){input(className="cs-key-input",type=if(showKey)"text"else"password",value=key,placeholder="Optional, pick one you can remember",maxLength=64,onInput={key=it.value});button(className="sbo-button",onClick={showKey=!showKey}){+if(showKey)"Hide"else"Show"};button(className="sbo-button",disabled=busy,onClick={actionCount++}){+"Save"}}
                            div(className="cs-hint"){+"Use the same key on every PC or instance and remember it."}
                        }
                        details("How it works",className="cs-info"){info.forEach{div(className="cs-info-line"){+it}}}
                    }
                }
            }
        }
        modal(open=dialog!=null,onClose={dialog=null},className="cs-modal") {
            val restore=dialog=="restore"
            div(className="cs-modal-title"){+if(restore)"Load this backup?"else"Delete your cloud save?"}
            div(className="cs-modal-text"){+if(restore)"Your SBO data goes back to 2026-10-27 14:30. Your current data is backed up first."else"Your data on this PC stays."}
            div(className="cs-buttons cs-modal-buttons"){button(className="sbo-button",id="cloud-cancel",onClick={dialog=null}){+"Cancel"};button(className="sbo-button "+if(restore)"cs-primary"else"cs-danger-filled",id="cloud-confirm",onClick={actionCount++;dialog=null}){+if(restore)"Load backup"else"Delete"}}
        }
    }
    val frame=StandaloneReviewFrame("cloud",css,width,height,theme){app(Unit)}
}
