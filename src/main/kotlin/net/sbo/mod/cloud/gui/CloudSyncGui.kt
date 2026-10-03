package net.sbo.mod.cloud.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.details
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.utils.data.cloud.CloudArea
import net.sbo.mod.utils.data.cloud.CloudSide
import net.sbo.mod.utils.data.cloud.CloudSync
import net.sbo.mod.utils.data.cloud.CloudSync.CompareResult
import net.sbo.mod.utils.data.cloud.CloudSync.SyncState
import net.sbo.mod.utils.data.cloud.CloudSyncKeys
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CloudSyncGui {
    private val STYLES = listOf("sbo:ui/cloud/cloud.css")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Cloud Sync")
    }

    private val App = component("CloudSync") {
        val toast = useToast()
        var comparing by useState(false)

        // Toasts of CloudSync land here while the window is open
        useEffect {
            CloudSync.toaster = { kind, title, message -> toast.show(message, kind, title, durationMs = if (kind == "error") 6000 else 3500) }
            onCleanup { CloudSync.toaster = null }
        }

        if (comparing) ComparePage(CompareProps(onBack = { comparing = false }))
        else MainPage(MainProps(onCompare = { comparing = true }))
    }

    private data class MainProps(val onCompare: () -> Unit)

    private val MainPage = component<MainProps>("CloudSyncMain") { (onCompare) ->
        var status by useState<CloudSync.Status?>(null)
        var revision by useState(0)
        var signKey by useState(CloudSyncKeys.text() ?: "")
        var confirmDelete by useState(false)
        val forceUpdate = useForceUpdate()
        val busy = CloudSync.busy

        // Status again after every upload, download or delete
        useEffect {
            val remove = CloudSync.addListener {
                forceUpdate()
                if (CloudSync.busy == null) revision++
            }
            onCleanup { remove() }
        }
        useEffect(revision) {
            CloudSync.fetchStatus { status = it }
        }

        val noSboKey = status?.state == SyncState.NO_SBO_KEY
        val locked = busy != null || noSboKey || status == null
        val hasSave = status?.state !in setOf(SyncState.NO_SAVE, SyncState.NO_SBO_KEY, SyncState.ERROR, null)

        div(className = "cs-window") {
            windowHeader("Cloud Sync")
            scroll(className = "cs-body guilib-autohide") {
                statusCard(status, busy)

                CloudSync.question?.let { question ->
                    div(className = "cs-question") {
                        div(className = "cs-question-top") {
                            div(className = "cs-question-text") { +question.text }
                            button(className = "cs-icon", title = "Hide this question, decide later", onClick = { CloudSync.dismissQuestion() }) { +"✕" }
                        }
                        div(className = "cs-buttons") {
                            question.actions.forEach { action ->
                                button(
                                    className = classNames("cs-danger" to action.danger),
                                    title = action.hint,
                                    disabled = busy != null,
                                    onClick = { action.run() }
                                ) { +action.label }
                            }
                            if (hasSave) button(title = "Shows what is different before you decide", disabled = busy != null, onClick = { onCompare() }) { +"Compare" }
                        }
                    }
                }

                setting("Auto Sync", "Loads newer data from your other PC when you join and saves your changes while you play. Only for this PC.") {
                    switch(checked = CloudSync.autoSync, disabled = noSboKey, onChange = { CloudSync.autoSync = it.checked })
                }

                div(className = "cs-setting cs-key") {
                    div(className = "cs-setting-title") { +"Sign Key" }
                    div(className = "cs-key-row") {
                        input(
                            className = "cs-key-input",
                            value = signKey,
                            placeholder = if (CloudSyncKeys.hasKey()) "Set, enter it again to show it" else "Optional",
                            maxLength = 64,
                            onInput = { signKey = it.value }
                        )
                        button(
                            disabled = busy != null,
                            title = if (signKey.isBlank()) "Removes the sign key from this PC" else "Saves the sign key on this PC",
                            onClick = { CloudSync.saveSignKey(signKey) }
                        ) { +"Save" }
                        button(title = "Fills in a new random sign key, click Save to use it", onClick = { signKey = CloudSyncKeys.generate() }) { +"New" }
                    }
                    div(className = "cs-hint") {
                        +"Protects your cloud save: only PCs with the same sign key can load it. Write it down, it cannot be recovered. Save it empty to remove it."
                    }
                }

                div(className = "cs-actions") {
                    button(className = "cs-primary", disabled = locked, title = "Saves the settings and data from this PC as your cloud save", onClick = { CloudSync.upload() }) { +"Upload" }
                    button(disabled = locked || !hasSave, title = "Replaces the settings and data on this PC with your cloud save. A backup is made first.", onClick = { CloudSync.download() }) { +"Download" }
                    button(disabled = locked || !hasSave, title = "Shows what is different between this PC and your cloud save", onClick = { onCompare() }) { +"Compare" }
                    div(className = "cs-spacer")
                    button(className = "cs-danger", disabled = locked || !hasSave, title = "Deletes your cloud save. The data on this PC stays.", onClick = { confirmDelete = true }) { +"Delete" }
                }
            }
        }

        modal(open = confirmDelete, onClose = { confirmDelete = false }, className = "cs-modal") {
            div(className = "cs-modal-title") { +"Delete your cloud save?" }
            div(className = "cs-modal-text") { +"It is deleted for good. The settings and data on this PC stay." }
            div(className = "cs-buttons cs-modal-buttons") {
                button(onClick = { confirmDelete = false }) { +"Cancel" }
                button(className = "cs-danger-filled", onClick = {
                    confirmDelete = false
                    CloudSync.delete()
                }) { +"Delete" }
            }
        }
    }

    private data class CompareProps(val onBack: () -> Unit)

    private val ComparePage = component<CompareProps>("CloudSyncCompare") { (onBack) ->
        var result by useState<CompareResult?>(null)
        var choices by useState(emptyMap<String, CloudSide>())

        useEffect { CloudSync.compare { result = it } }

        fun side(id: String): CloudSide = choices[id] ?: CloudSide.PC
        fun pick(id: String, side: CloudSide) { choices = choices + (id to side) }

        div(className = "cs-window cs-wide") {
            windowHeader("Compare", onBack)
            when (val r = result) {
                null -> div(className = "cs-body cs-center") { +"Loading your cloud save..." }
                is CompareResult.Failed -> div(className = "cs-body cs-center") {
                    div(className = "cs-error") { +r.message }
                }
                is CompareResult.Ready -> {
                    val count = r.areas.sumOf { it.fields.size + it.hidden }
                    scroll(className = "cs-body guilib-autohide") {
                        if (r.areas.isEmpty()) {
                            div(className = "cs-center") { +"No differences. This PC and your cloud save are the same." }
                        } else {
                            div(className = "cs-compare-head") {
                                span(className = "cs-count") { +"$count ${if (count == 1) "difference" else "differences"}" }
                                div(className = "cs-spacer")
                                span(className = "cs-hint") { +"Left is this PC, right is the cloud. Click what to keep." }
                            }
                            if (r.unsigned) div(className = "cs-warning") {
                                +"This cloud save has no sign key, so SBO cannot check that it really comes from you."
                            }
                            r.areas.forEach { area -> areaBlock(area, ::side, ::pick) }
                        }
                    }
                    if (r.areas.isNotEmpty()) {
                        div(className = "cs-footer") {
                            button(title = "Keep everything from this PC", onClick = { choices = emptyMap() }) { +"All from PC" }
                            button(title = "Take everything from the cloud save", onClick = { choices = allIds(r.areas).associateWith { CloudSide.CLOUD } }) { +"All from Cloud" }
                            div(className = "cs-spacer")
                            button(
                                className = "cs-primary",
                                disabled = CloudSync.busy != null,
                                title = "Uses your picks on this PC and uploads them as your cloud save. A backup is made first.",
                                onClick = {
                                    CloudSync.merge(r, choices)
                                    onBack()
                                }
                            ) { +"Save choice" }
                        }
                    }
                }
            }
        }
    }

    private fun allIds(areas: List<CloudArea>): List<String> =
        areas.flatMap { area -> if (area.perField) area.fields.map { it.id } else listOf(area.file) }

    private fun NodeBuilder.areaBlock(area: CloudArea, side: (String) -> CloudSide, pick: (String, CloudSide) -> Unit) {
        val total = area.fields.size + area.hidden
        val fromCloud = if (area.perField) area.fields.count { side(it.id) == CloudSide.CLOUD } else if (side(area.file) == CloudSide.CLOUD) total else 0
        details(
            summary = {
                span(className = "cs-area-title") { +area.label }
                span(className = "cs-area-count") { +"$total" }
                div(className = "cs-spacer")
                if (fromCloud > 0) span(className = "cs-area-cloud") { +if (area.perField) "$fromCloud from cloud" else "from cloud" }
            },
            className = classNames("cs-area", "cs-perfield" to area.perField),
            key = area.file
        ) {
            if (!area.perField) {
                div(className = "cs-field cs-whole") {
                    span(className = "cs-field-label") { +"Keep" }
                    segmented(value = side(area.file).name, onChange = { pick(area.file, CloudSide.valueOf(it)) }, className = "cs-pick") {
                        option(CloudSide.PC.name, "This PC")
                        option(CloudSide.CLOUD.name, "Cloud")
                    }
                }
                div(className = "cs-field cs-columns") {
                    span { +"Differences" }
                    span { +"This PC" }
                    span { +"Cloud" }
                }
            }
            area.fields.forEach { field ->
                div(className = "cs-field", key = field.id) {
                    span(className = "cs-field-label", title = field.label) { +field.label }
                    if (area.perField) {
                        segmented(value = side(field.id).name, onChange = { pick(field.id, CloudSide.valueOf(it)) }, className = "cs-pick") {
                            option(CloudSide.PC.name, field.pc ?: "(not set)", title = "This PC")
                            option(CloudSide.CLOUD.name, field.cloud ?: "(not set)", title = "Cloud")
                        }
                    } else {
                        span(className = "cs-value", title = "This PC") { +(field.pc ?: "(not set)") }
                        span(className = "cs-value", title = "Cloud") { +(field.cloud ?: "(not set)") }
                    }
                }
            }
            if (area.hidden > 0) div(className = "cs-hint cs-more") { +"and ${area.hidden} more" }
        }
    }

    private fun NodeBuilder.windowHeader(title: String, onBack: (() -> Unit)? = null) {
        header(className = "cs-header") {
            if (onBack != null) button(className = "cs-icon", title = "Back", onClick = { onBack() }) { +"←" }
            span(className = "cs-title") { +title }
            div(className = "cs-spacer")
            button(className = "cs-icon cs-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
        }
    }

    private fun NodeBuilder.statusCard(status: CloudSync.Status?, busy: String?) {
        val (tone, text) = when (status?.state) {
            null -> "idle" to "Checking your cloud save..."
            SyncState.NO_SBO_KEY -> "bad" to "Set your SBO key first: get it on the SBO Discord with /generatesbokey, then type /sbokey <key>."
            SyncState.NO_SAVE -> "warn" to "You have no cloud save yet. Click Upload to create one."
            SyncState.NOT_USED_HERE -> "warn" to "This PC has not used your cloud save yet. Click Download to load it here, or Compare first."
            SyncState.BOTH_CHANGED -> "bad" to "Your cloud save and this PC both have changes. Click Compare to see them and pick what to keep."
            SyncState.CLOUD_NEWER -> "warn" to "Your cloud save is newer than this PC. Click Download to get it."
            SyncState.PC_CHANGED -> "warn" to "This PC has changes that are not in your cloud save yet. Click Upload to save them."
            SyncState.SAME -> "ok" to "This PC and your cloud save are the same."
            SyncState.ERROR -> "bad" to (status.error ?: "Something went wrong.")
        }
        div(className = classNames("cs-status", tone)) {
            div(className = "cs-dot")
            div(className = "cs-status-text") {
                div(className = "cs-status-title") { +(busy ?: text) }
                if (status != null && status.updatedAt > 0) {
                    div(className = "cs-hint") {
                        +"Last upload: ${DATE_FORMAT.format(Instant.ofEpochMilli(status.updatedAt))} · ${(status.size + 1023) / 1024} KB"
                    }
                }
            }
        }
    }

    private fun NodeBuilder.setting(title: String, text: String, control: NodeBuilder.() -> Unit) {
        div(className = "cs-setting") {
            div(className = "cs-setting-text") {
                div(className = "cs-setting-title") { +title }
                div(className = "cs-hint") { +text }
            }
            control()
        }
    }
}
