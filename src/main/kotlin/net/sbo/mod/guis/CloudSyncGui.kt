package net.sbo.mod.guis

import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.UiScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.core.dsl.details
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.input
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.utils.data.Backups
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.cloud.CloudArea
import net.sbo.mod.utils.data.cloud.CloudSide
import net.sbo.mod.utils.data.cloud.CloudSync
import net.sbo.mod.utils.data.cloud.CloudSync.CompareResult
import net.sbo.mod.utils.data.cloud.CloudSync.SyncState
import net.sbo.mod.utils.data.cloud.CloudSyncKeys
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CloudSyncGui {
    private val STYLES = listOf("sbo:ui/cloud/cloud.css", SboLook.STYLE)
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    /** Opens the window. Must run on the client thread. */
    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Cloud Sync")
    }

    private val App = component("CloudSync") {
        val toast = useToast()
        var page by useState(Page.MAIN)
        useEscapeBack(page != Page.MAIN) { page = Page.MAIN }
        var uiScale by useState(UiScale.own(DataManager.sboData.cloudSyncUiScale))
        useSboScale(uiScale)
        useSboTheme()
        val size = WindowSize(uiScale) { scale ->
            uiScale = scale
            DataManager.sboData.cloudSyncUiScale = scale
            DataManager.sboData.save()
        }

        // Toasts of CloudSync land here while the window is open
        useEffect {
            CloudSync.toaster = { kind, title, message -> toast.show(message, kind, title, durationMs = if (kind == "error") 6000 else 3500) }
            onCleanup { CloudSync.toaster = null }
        }

        when (page) {
            Page.MAIN -> MainPage(MainProps(size, onCompare = { page = Page.COMPARE }, onBackups = { page = Page.BACKUPS }))
            Page.COMPARE -> ComparePage(CompareProps(size, onBack = { page = Page.MAIN }))
            Page.BACKUPS -> BackupsPage(BackupsProps(size, onBack = { page = Page.MAIN }))
        }
    }

    private enum class Page { MAIN, COMPARE, BACKUPS }

    // Window size picker in the header
    private data class WindowSize(val scale: Float?, val onChange: (Float?) -> Unit)

    private data class MainProps(val size: WindowSize, val onCompare: () -> Unit, val onBackups: () -> Unit)

    private val MainPage = component<MainProps>("CloudSyncMain") { (size, onCompare, onBackups) ->
        var status by useState<CloudSync.Status?>(null)
        var revision by useState(0)
        var signKey by useState(CloudSyncKeys.text() ?: "")
        var confirmDelete by useState(false)
        var showKey by useState(false)
        var refreshing by useState(false)
        var refreshWait by useState(0L)
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
        // Keeps the refresh button's wait time current
        useInterval(1000) { refreshWait = CloudSync.refreshWaitMs() }

        fun refresh() {
            refreshing = true
            CloudSync.fetchStatus(refresh = true) {
                status = it
                refreshing = false
                refreshWait = CloudSync.refreshWaitMs()
            }
        }

        val notSupporter = status?.state == SyncState.ERROR && status?.error?.contains("supporter", ignoreCase = true) == true
        val locked = busy != null || status == null
        val hasSave = status?.state !in setOf(SyncState.NO_SAVE, SyncState.ERROR, null)

        div(className = "cs-window") {
            windowHeader("Cloud Sync", size, onBackups = onBackups)
            scroll(className = "cs-body guilib-autohide") {
                statusCard(status, busy, refreshWait, refreshing || busy != null) { refresh() }

                // Without supporter status nothing below works, only the steps to fix it
                if (notSupporter) {
                    setupSteps()
                    return@scroll
                }

                CloudSync.question?.let { question ->
                    div(className = "cs-question") {
                        div(className = "cs-question-top") {
                            div(className = "cs-question-text") { +question.text }
                            button(className = "cs-icon", title = "Hide this question, decide later", onClick = { CloudSync.dismissQuestion() }) { +"x" }
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

                div(className = "cs-actions") {
                    button(className = "cs-primary", disabled = locked, title = "Saves this PC's data in the cloud", onClick = { CloudSync.upload() }) { +"Upload" }
                    button(disabled = locked || !hasSave, title = "Loads your cloud save on this PC. A backup is made first.", onClick = { CloudSync.download() }) { +"Download" }
                    button(disabled = locked || !hasSave, title = "Shows what is different", onClick = { onCompare() }) { +"Compare" }
                    div(className = "cs-spacer")
                    button(className = "cs-danger", disabled = locked || !hasSave, title = "Deletes your cloud save", onClick = { confirmDelete = true }) { +"Delete" }
                }

                setting("Auto Sync", "Saves and loads for you.") {
                    switch(checked = CloudSync.autoSync, onChange = { CloudSync.autoSync = it.checked })
                }

                div(className = "cs-setting cs-key") {
                    tooltip(content = {
                        div { +"Signs your cloud save. If it gets changed or damaged, SBO notices and does not load it." }
                        div(className = "cs-tip-line") { +"Optional: only set one if you really want your cloud save signed. Without a key, Cloud Sync works just the same." }
                        div(className = "cs-tip-line") { +"Only PCs or instances with the same key can load it." }
                        div(className = "cs-tip-line") { +"If you forget it, your cloud save cannot be loaded anymore and SBO cannot reset the key. You can only set a new key and upload again, which overwrites your old cloud save with the current data from this PC." }
                    }) {
                        span(className = "cs-setting-title") {
                            +"Sign Key"
                            img("sbo:ui/partyfinder/info.svg", className = "cs-info-icon")
                        }
                    }
                    div(className = "cs-key-row") {
                        input(
                            className = "cs-key-input",
                            type = if (showKey) "text" else "password",
                            value = signKey,
                            placeholder = if (CloudSyncKeys.hasKey()) "Set, type it again to see it" else "Optional, pick one you can remember",
                            maxLength = 64,
                            onInput = { signKey = it.value }
                        )
                        button(title = if (showKey) "Hides the sign key" else "Shows the sign key", onClick = { showKey = !showKey }) { +if (showKey) "Hide" else "Show" }
                        button(
                            disabled = busy != null,
                            title = if (signKey.isBlank()) "Removes the sign key from this PC" else "Saves the sign key on this PC",
                            onClick = { CloudSync.saveSignKey(signKey) }
                        ) { +"Save" }
                    }
                    div(className = "cs-hint") { +"Use the same key on every PC or instance and remember it." }
                }

                details("How it works", className = "cs-info") {
                    infoLine("• On join: loads newer data or uploads your changes")
                    infoLine("• Every 30 min: uploads, only if something changed")
                    infoLine("• Leaving a server or closing the game: uploads")
                    infoLine("• Both sides changed: nothing is overwritten, you pick here")
                    infoLine("• Auto Sync off: only Upload and Download")
                    infoLine("• Saved: settings, trackers, achievements, past Diana events, party finder, overlays, sounds")
                    infoLine("• Not saved: SBO key and sign key")
                }
            }
        }

        modal(open = confirmDelete, onClose = { confirmDelete = false }, className = "cs-modal") {
            div(className = "cs-modal-title") { +"Delete your cloud save?" }
            div(className = "cs-modal-text") { +"Your data on this PC stays." }
            div(className = "cs-buttons cs-modal-buttons") {
                button(onClick = { confirmDelete = false }) { +"Cancel" }
                button(className = "cs-danger-filled", onClick = {
                    confirmDelete = false
                    CloudSync.delete()
                }) { +"Delete" }
            }
        }
    }

    private data class CompareProps(val size: WindowSize, val onBack: () -> Unit)

    private val ComparePage = component<CompareProps>("CloudSyncCompare") { (size, onBack) ->
        var result by useState<CompareResult?>(null)
        var choices by useState(emptyMap<String, CloudSide>())

        useEffect { CloudSync.compare { result = it } }

        fun side(id: String): CloudSide = choices[id] ?: CloudSide.PC
        fun pick(id: String, side: CloudSide) { choices = choices + (id to side) }

        div(className = "cs-window cs-wide") {
            windowHeader("Compare", size, onBack)
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

    private data class BackupsProps(val size: WindowSize, val onBack: () -> Unit)

    private val BackupsPage = component<BackupsProps>("CloudSyncBackups") { (size, onBack) ->
        val toast = useToast()
        var backups by useState(Backups.list())
        var confirm by useState<Backups.Backup?>(null)

        // Loading one adds a new backup in the background
        useInterval(2000) { backups = Backups.list() }

        div(className = "cs-window cs-wide") {
            windowHeader("Backups", size, onBack)
            scroll(className = "cs-body guilib-autohide") {
                div(className = "cs-hint cs-backups-hint") {
                    +"SBO makes a backup every time you close the game. The newest 10 are kept."
                }
                if (backups.isEmpty()) div(className = "cs-center cs-empty") { +"No backups yet. SBO makes one when you close the game." }
                backups.forEach { backup ->
                    div(className = "cs-backup", key = backup.file.name) {
                        div(className = "cs-backup-text") {
                            div(className = "cs-backup-date") { +DATE_FORMAT.format(Instant.ofEpochMilli(backup.createdAt)) }
                            div(className = "cs-hint") { +"${ago(backup.createdAt)} · ${(backup.size + 1023) / 1024} KB" }
                        }
                        button(title = "Puts your SBO data back to this backup", onClick = { confirm = backup }) { +"Load" }
                    }
                }
            }
        }

        modal(open = confirm != null, onClose = { confirm = null }, className = "cs-modal") {
            val backup = confirm ?: return@modal
            div(className = "cs-modal-title") { +"Load this backup?" }
            div(className = "cs-modal-text") {
                +"Your SBO data goes back to ${DATE_FORMAT.format(Instant.ofEpochMilli(backup.createdAt))}. Your current data is backed up first."
            }
            div(className = "cs-buttons cs-modal-buttons") {
                button(onClick = { confirm = null }) { +"Cancel" }
                button(className = "cs-primary", onClick = {
                    confirm = null
                    runCatching { Backups.load(backup) }
                        .onSuccess { toast.success("Backup from ${DATE_FORMAT.format(Instant.ofEpochMilli(backup.createdAt))} loaded.", title = "Backups") }
                        .onFailure { toast.error("Could not load the backup: ${it.message}", title = "Backups", durationMs = 6000) }
                }) { +"Load backup" }
            }
        }
    }

    // "5 min ago", "3 h ago", "2 days ago"
    private fun ago(time: Long): String {
        val minutes = Duration.ofMillis(System.currentTimeMillis() - time).toMinutes()
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 48 * 60 -> "${minutes / 60} h ago"
            else -> "${minutes / (24 * 60)} days ago"
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

    private fun NodeBuilder.windowHeader(title: String, size: WindowSize, onBack: (() -> Unit)? = null, onBackups: (() -> Unit)? = null) {
        header(className = "cs-header") {
            if (onBack != null) button(className = "cs-icon", title = "Back", onClick = { onBack() }) { +"←" }
            span(className = "cs-title") { +title }
            div(className = "cs-spacer")
            if (onBackups != null) button(className = "cs-header-button", title = "Load one of the backups SBO made on this PC", onClick = { onBackups() }) { +"Backups" }
            span(className = "cs-size-label") { +"Size" }
            select(value = UiScale.id(size.scale), onChange = { e -> size.onChange(UiScale.parse(e.value)) }, className = "cs-size") {
                UiScale.OWN_CHOICES.forEach { scale ->
                    val title = when (scale) {
                        null -> "Uses the size from the SBO settings"
                        UiScale.AUTO -> "Uses your Minecraft GUI scale"
                        else -> null
                    }
                    option(UiScale.id(scale), UiScale.label(scale), title = title)
                }
            }
            button(className = "cs-icon cs-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
        }
    }

    private fun NodeBuilder.statusCard(status: CloudSync.Status?, busy: String?, refreshWait: Long, refreshing: Boolean, onRefresh: () -> Unit) {
        val (tone, text) = when (status?.state) {
            null -> "idle" to "Checking..."
            SyncState.NO_SAVE -> "warn" to "No cloud save yet. Click Upload."
            SyncState.NOT_USED_HERE -> "warn" to "Not loaded on this PC yet. Click Download."
            SyncState.BOTH_CHANGED -> "bad" to "This PC and the cloud both changed. Click Compare."
            SyncState.CLOUD_NEWER -> "warn" to "The cloud is newer. Click Download."
            SyncState.PC_CHANGED -> "warn" to "This PC has new changes. Click Upload."
            SyncState.SAME -> "ok" to "Everything is up to date."
            SyncState.ERROR -> "bad" to (status.error?.replaceFirstChar(Char::uppercaseChar) ?: "Something went wrong.")
        }
        div(className = classNames("cs-status", tone)) {
            div(className = "cs-dot")
            div(className = "cs-status-text") {
                div(className = "cs-status-title") { +(busy ?: text) }
                if (status != null && status.updatedAt > 0) {
                    div(className = "cs-hint") {
                        +"Last upload: ${DATE_FORMAT.format(Instant.ofEpochMilli(status.updatedAt))}"
                    }
                }
            }
            button(
                className = "cs-icon cs-refresh",
                disabled = refreshing || refreshWait > 0,
                title = if (refreshWait > 0) "Check again in ${(refreshWait + 999) / 1000} s" else "Checks your cloud save again, e.g. after you uploaded on another PC",
                onClick = { onRefresh() }
            ) { img("sbo:ui/cloud/refresh.svg", className = "cs-refresh-icon") }
        }
    }

    private fun NodeBuilder.setupSteps() {
        div(className = "cs-steps") {
            div(className = "cs-steps-title") { +"Already a supporter? Check these:" }
            infoLine("1. Your Minecraft account is linked to your Discord account: type /link followed by your Minecraft name on the SBO Discord.")
            infoLine("2. Use the Discord account that has the supporter role or boosts the server.")
            infoLine("3. Open this window again with /sbocloud. New supporter roles can take a few minutes.")
        }
    }


    private fun NodeBuilder.infoLine(text: String) {
        div(className = "cs-info-line") { +text }
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
