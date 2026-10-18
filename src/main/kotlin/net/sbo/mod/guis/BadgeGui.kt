package net.sbo.mod.guis

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.checkbox
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.colorInput
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.general.SboBadges
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useFitScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeChoice
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.BadgeRank
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.TeamTagPlace
import net.sbo.mod.utils.http.SboApi

object BadgeGui {
    private val STYLES = listOf("sbo:ui/badge/badge.css", SboLook.STYLE)
    private const val LOGO = "sbo:textures/font/badge.png"

    private const val DEFAULT_GRADIENT_END = "#FFD966"

    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Badge")
    }

    private sealed interface Load {
        data object Loading : Load
        data class Failed(val message: String, val notEligible: Boolean) : Load
        data class Ready(val badge: OwnBadgeResponse) : Load
    }

    private val App = component("Badge") {
        val toast = useToast()
        var load by useState<Load>(Load.Loading)
        var draft by useState<BadgeSettings?>(null)
        var saving by useState(false)
        // The part whose color the picker changes, picked in the matrix or the preview
        var picked by useState(BadgePart.NAME)
        useSboScale()
        useSboTheme()

        fun loaded(response: OwnBadgeResponse) {
            if (response.success && response.settings != null) {
                SboBadges.rememberAccess(true)
                load = Load.Ready(response)
                draft = response.settings
            } else {
                if (response.code == "NOT_ELIGIBLE") SboBadges.rememberAccess(false)
                load = Load.Failed(response.error ?: "Could not load your badge.", response.code == "NOT_ELIGIBLE")
            }
        }

        useEffect {
            SboApi.ownBadge()
                .toJson<OwnBadgeResponse>(ignoreUnknownKeys = true) { mc.execute { loaded(it) } }
                .error { mc.execute { load = Load.Failed("Could not reach the SBO server: ${it.message}", false) } }
        }

        fun save(settings: BadgeSettings) {
            saving = true
            SboApi.saveBadge(settings)
                .toJson<OwnBadgeResponse>(ignoreUnknownKeys = true) { response ->
                    mc.execute {
                        saving = false
                        if (response.success) {
                            loaded(response)
                            SboBadges.fetch()
                            toast.success("Others see your new badge within a few minutes.", title = "Badge saved")
                        } else {
                            toast.error(response.error ?: "Could not save your badge.", title = "SBO Badge", durationMs = 6000)
                        }
                    }
                }
                .error {
                    mc.execute {
                        saving = false
                        toast.error("Could not reach the SBO server: ${it.message}", title = "SBO Badge", durationMs = 6000)
                    }
                }
        }

        val fit = useFitScale()

        div(className = "bd-window", ref = fit.ref, style = fit.style) {
            header(className = "bd-header") {
                img(LOGO, className = "bd-header-logo")
                span(className = "bd-title") { +"SBO Badge" }
                div(className = "bd-spacer")
                button(className = "bd-icon bd-close", title = "Close", onClick = { GuiLib.close() }) { +"x" }
            }
            // Wide instead of tall: preview and switches for other badges left, the own badge's settings right
            div(className = "bd-body bd-columns") {
                when (val state = load) {
                    Load.Loading -> {
                        div(className = "bd-col-left") {
                            div(className = "bd-center") { +"Loading your badge..." }
                            if (SboBadges.VIEWER_TOGGLES) ViewSettings()
                        }
                        div(className = "bd-col-right")
                    }
                    is Load.Failed -> {
                        div(className = "bd-col-left") { failed(state) }
                        div(className = "bd-col-right") { if (SboBadges.VIEWER_TOGGLES) ViewSettings() }
                    }
                    is Load.Ready -> {
                        val settings = draft ?: state.badge.settings!!
                        editor(
                            state.badge, settings, picked, saving,
                            onPick = { picked = it }, onChange = { draft = it }, onSave = { save(settings) },
                        )
                    }
                }
            }
        }
    }

    private val ViewSettings = component("BadgeView") {
        var aboveHeads by useState(DataManager.sboData.badgesAboveHeads)
        var inTab by useState(DataManager.sboData.badgesInTab)
        var inChat by useState(DataManager.sboData.badgesInChat)
        div(className = "bd-section-title") { +"Badges of Others" }
        setting("Above heads", "The SBO logo, the chosen stat and the colored name of supporters, maintainers and moderators.") {
            switch(checked = aboveHeads, onChange = {
                aboveHeads = it.checked
                DataManager.sboData.badgesAboveHeads = it.checked
                DataManager.sboData.save()
            })
        }
        setting("In the tab list", "The SBO logo behind their name and the name in their color.") {
            switch(checked = inTab, onChange = {
                inTab = it.checked
                DataManager.sboData.badgesInTab = it.checked
                DataManager.sboData.save()
            })
        }
        setting("In chat", "The SBO logo behind their name, the name in their color and team tags, for new messages.") {
            switch(checked = inChat, onChange = {
                inChat = it.checked
                DataManager.sboData.badgesInChat = it.checked
                DataManager.sboData.save()
            })
        }
    }

    private fun NodeBuilder.failed(state: Load.Failed) {
        if (!state.notEligible) {
            div(className = "bd-error") { +state.message }
            return
        }
        div(className = "bd-hint bd-intro") {
            +"Supporters (Patreon or Ko-fi), maintainers and moderators get their own badge: the SBO logo, a stat of their choice and their name in their color."
        }
        div(className = "bd-steps") {
            div(className = "bd-steps-title") { +"Already a supporter? Check these:" }
            line("1. Your Minecraft account is linked to your Discord account: type /link followed by your Minecraft name on the SBO Discord.")
            line("2. Use the Discord account that has the Patreon or Ko-fi supporter role, or the maintainer or moderator role.")
            line("3. Open this window again with /${SboBadges.COMMAND}. New roles can take a few minutes.")
        }
    }

    private fun NodeBuilder.editor(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        saving: Boolean,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
        onSave: () -> Unit,
    ) {
        val stat = badge.stats.firstOrNull { it.id == settings.stat }
        val changed = settings != badge.settings
        val rank = badge.rankInfo
        // The tag only has a row for team ranks, a picked tag falls back to the name
        val part = if (picked == BadgePart.TAG && !hasTag(badge)) BadgePart.NAME else picked

        div(className = "bd-col-left") {
            div(className = "bd-rank-row") {
                span(className = "bd-rank", style = "background: ${background(rank?.color ?: "#FFAA00", rank?.to)}") {
                    +(rank?.label ?: badge.rank.orEmpty())
                }
                span(className = "bd-hint") { +if (badge.team) "Thank you for working on SBO!" else "Thank you for supporting SBO!" }
            }

            preview(badge, settings, stat, part, onPick)

            div(className = "bd-actions") {
                button(
                    className = "bd-primary",
                    disabled = saving || !changed,
                    title = if (changed) "Saves your badge" else "Nothing changed",
                    onClick = { onSave() },
                ) { +if (saving) "Saving..." else "Save" }
                if (changed) button(disabled = saving, title = "Back to your saved badge", onClick = { onChange(badge.settings!!) }) { +"Undo" }
            }

            if (SboBadges.VIEWER_TOGGLES) ViewSettings()
        }

        div(className = "bd-col-right") {
            settings(badge, settings, part, onPick, onChange)
        }
    }

    private fun hasTag(badge: OwnBadgeResponse): Boolean = badge.team && badge.rankInfo?.tag != null

    /** A row of the matrix: the part that shows per place and the part whose color it uses. */
    private class PartRow(val show: String, val color: String, val label: String, val allowed: Set<String>)

    private val PLACES = listOf(TeamTagPlace.HEAD to "Head", TeamTagPlace.TAB to "Tab", TeamTagPlace.CHAT to "Chat")

    private val PART_LABELS = mapOf(
        BadgePart.STAT to "Logo + stat",
        BadgePart.NAME to "Name",
        BadgePart.LEVEL to "Level",
        BadgePart.TAG to "Team tag",
    )

    private val COLOR_HINTS = mapOf(
        BadgePart.STAT to "The stat above your head.",
        BadgePart.NAME to "Your name where it is colored.",
        BadgePart.LEVEL to "Your SkyBlock level in front of your name where it is colored.",
        BadgePart.TAG to "Your team tag; others see it in the rank colors while your badge is hidden.",
    )

    private fun NodeBuilder.settings(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        div(className = "bd-section-title") { +"Your Badge" }
        div(className = "bd-hint") { +"Pick where every part shows. Click a part, here or in the preview, to change its color." }

        val rows = buildList {
            add(PartRow(BadgePart.LOGO, BadgePart.STAT, PART_LABELS.getValue(BadgePart.STAT), TeamTagPlace.ALL))
            add(PartRow(BadgePart.NAME, BadgePart.NAME, "Name color", TeamTagPlace.ALL))
            add(PartRow(BadgePart.LEVEL, BadgePart.LEVEL, "Level color", TeamTagPlace.LEVEL))
            if (hasTag(badge)) add(PartRow(BadgePart.TAG, BadgePart.TAG, "Team tag", TeamTagPlace.ALL))
        }
        div(className = "bd-matrix") {
            div(className = "bd-mrow bd-mhead") {
                span(className = "bd-mlabel")
                PLACES.forEach { (place, label) -> span(className = "bd-mcell", key = place) { +label } }
            }
            rows.forEach { row ->
                val places = if (row.show == BadgePart.TAG) settings.teamTagPlaces else settings.placesOf(row.show)
                val color = settings.colorOf(row.color)
                div(
                    className = classNames("bd-mrow", "bd-mrow-picked" to (picked == row.color)),
                    key = row.show,
                    title = "Click to change the ${row.label.lowercase()} color",
                    onClick = { onPick(row.color) },
                ) {
                    div(className = "bd-mlabel") {
                        span(className = "bd-mswatch", style = "background: ${background(color.color, color.to)}")
                        span { +row.label }
                    }
                    PLACES.forEach { (place, _) ->
                        div(className = "bd-mcell", key = place) {
                            if (place in row.allowed) {
                                checkbox(checked = place in places, onChange = {
                                    val next = if (it.checked) places + place else places - place
                                    onChange(if (row.show == BadgePart.TAG) settings.withTeamTagPlaces(next) else settings.withPlaces(row.show, next))
                                })
                            } else {
                                span(className = "bd-muted", title = "Hypixel's rank stands between level and name in chat") { +"-" }
                            }
                        }
                    }
                }
            }
        }

        val color = settings.colorOf(picked)
        val label = PART_LABELS.getValue(picked)
        div(className = "bd-setting bd-colors-setting") {
            div(className = "bd-setting-text") {
                div(className = "bd-setting-title") { +"$label ${if (color.to != null) "colors" else "color"}" }
                div(className = "bd-hint") { +COLOR_HINTS.getValue(picked) }
            }
            div(className = "bd-colors") {
                colorInput(value = argb(color.color), onChange = { onChange(settings.withColor(picked, color.copy(color = hex(it)))) }, key = "from-$picked")
                color.to?.let { to ->
                    span(className = "bd-arrow") { +"→" }
                    colorInput(value = argb(to), onChange = { onChange(settings.withColor(picked, color.copy(to = hex(it)))) }, key = "to-$picked")
                    div(className = "bd-gradient", style = "background: ${background(color.color, to)}")
                }
            }
            div(className = "bd-color-actions") {
                button(title = "Gives every part this color", onClick = {
                    onChange(PART_LABELS.keys.fold(settings) { next, part -> next.withColor(part, color) })
                }) { +"Use for all parts" }
            }
        }

        if (badge.gradient) {
            setting("Gradient", "Two colors that flow into each other, for the ${label.lowercase()}.") {
                switch(checked = color.to != null, onChange = {
                    onChange(settings.withColor(picked, color.copy(to = if (it.checked) DEFAULT_GRADIENT_END else null)))
                })
            }
        }

        setting("Stat", "Shown above your head next to the logo, read from your SkyBlock profile.") {
            select(
                value = settings.stat,
                onChange = { onChange(settings.copy(stat = it.value)) },
                className = "bd-select",
                searchable = true,
                searchPlaceholder = "Search..."
            ) {
                badge.stats.forEach { option(it.id, it.label) }
            }
        }

        OwnView()
    }

    // Only on this PC, so it changes right away without saving
    private val OwnView = component("BadgeOwnView") {
        var inF5 by useState(DataManager.sboData.badgeOwnInF5)
        setting("Show my badge in F5", "See your own name tag, badge and team tag in third person.") {
            switch(checked = inF5, onChange = {
                inF5 = it.checked
                DataManager.sboData.badgeOwnInF5 = it.checked
                DataManager.sboData.save()
            })
        }
    }

    private fun NodeBuilder.preview(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        stat: BadgeChoice?,
        picked: String,
        onPick: (String) -> Unit,
    ) {
        // No values at all means the profile could not be read, a single missing one that the stat has none
        val profileRead = badge.values.isNotEmpty()
        val value = badge.values[settings.stat]
            ?: badge.preview?.value?.takeIf { settings.stat == badge.settings?.stat }
        val text = when {
            stat == null || settings.stat == "none" -> null
            value != null -> "${stat.label}: $value"
            profileRead -> null
            else -> "${stat.label}: ..."
        }
        val name = Player.accountName()
        val level = badge.level?.let { "[$it] " }.orEmpty()
        // Others only get the badge while it shows somewhere and the server does not hide it
        val listed = settings.enabled && !badge.hiddenByServer
        fun shows(part: String, place: String) = listed && place in settings.placesOf(part)
        val tagAt = { place: String -> hasTag(badge) && place in settings.teamTagPlaces }
        val atHead = shows(BadgePart.LOGO, TeamTagPlace.HEAD) || shows(BadgePart.NAME, TeamTagPlace.HEAD) ||
            shows(BadgePart.LEVEL, TeamTagPlace.HEAD) || tagAt(TeamTagPlace.HEAD)
        val parts = PreviewParts(badge, settings, picked, onPick, listed)

        div(className = classNames("bd-preview", "bd-hidden" to !atHead)) {
            if (shows(BadgePart.LOGO, TeamTagPlace.HEAD)) {
                div(className = "bd-tag") {
                    img(LOGO, className = "bd-tag-logo")
                    if (text != null) parts.colored(this, BadgePart.STAT, text)
                }
            }
            div(className = "bd-tag bd-tag-name") {
                if (tagAt(TeamTagPlace.HEAD)) parts.tag(this)
                parts.plainOr(this, BadgePart.LEVEL, level, shows(BadgePart.LEVEL, TeamTagPlace.HEAD))
                parts.plainOr(this, BadgePart.NAME, name, shows(BadgePart.NAME, TeamTagPlace.HEAD))
            }
        }
        div(className = "bd-lines") {
            div(className = "bd-line") {
                span(className = "bd-line-label") { +"Tab" }
                div(className = "bd-line-box bd-line-tab") {
                    parts.plainOr(this, BadgePart.LEVEL, level, shows(BadgePart.LEVEL, TeamTagPlace.TAB))
                    if (tagAt(TeamTagPlace.TAB)) parts.tag(this)
                    parts.plainOr(this, BadgePart.NAME, name, shows(BadgePart.NAME, TeamTagPlace.TAB))
                    if (shows(BadgePart.LOGO, TeamTagPlace.TAB)) img(LOGO, className = "bd-tag-logo bd-line-logo")
                }
            }
            div(className = "bd-line") {
                span(className = "bd-line-label") { +"Chat" }
                div(className = "bd-line-box bd-line-chat") {
                    // Hypixel's rank stands between level and name in chat, so the level keeps its color
                    span(className = "bd-tag-level") { +level }
                    if (tagAt(TeamTagPlace.CHAT)) parts.tag(this)
                    parts.plainOr(this, BadgePart.NAME, name, shows(BadgePart.NAME, TeamTagPlace.CHAT))
                    if (shows(BadgePart.LOGO, TeamTagPlace.CHAT)) img(LOGO, className = "bd-tag-logo bd-line-logo")
                    span(className = "bd-tag-plain") { +": Hello!" }
                }
            }
        }
        when {
            badge.hiddenByServer -> div(className = "bd-hint bd-center-text") { +"Hidden by the SBO server, nobody sees your badge right now." }
            !settings.enabled -> div(className = "bd-hint bd-center-text") { +"Nothing is ticked, nobody sees your badge." }
            stat != null && settings.stat != "none" && value == null && profileRead ->
                div(className = "bd-hint bd-center-text") { +"Your profile has no ${stat.label} yet, only the logo shows." }
            text != null && value == null -> div(className = "bd-hint bd-center-text") { +"The value is read from your profile after saving." }
        }
    }

    /** Clickable parts of the preview: a click picks the part for the color picker. */
    private class PreviewParts(
        val badge: OwnBadgeResponse,
        val settings: BadgeSettings,
        val picked: String,
        val onPick: (String) -> Unit,
        val listed: Boolean,
    ) {
        fun colored(builder: NodeBuilder, part: String, text: String) = with(builder) {
            val color = settings.colorOf(part)
            span(
                className = classNames("bd-pick", "bd-picked" to (picked == part)),
                title = "Click to change the ${PART_LABELS.getValue(part).lowercase()} color",
                onClick = { onPick(part) },
            ) { coloredText(text, color.color, color.to) }
        }

        /** The part in its color where it is colored, plain otherwise. */
        fun plainOr(builder: NodeBuilder, part: String, text: String, colored: Boolean) = with(builder) {
            if (text.isEmpty()) return@with
            if (colored) colored(this, part, text)
            else span(className = if (part == BadgePart.LEVEL) "bd-tag-level" else "bd-tag-plain") { +text }
        }

        /** "[SBO Owner] " in the tag color; others see it in the rank colors while the badge is hidden. */
        fun tag(builder: NodeBuilder) = with(builder) {
            val rank = badge.rankInfo ?: return@with
            val text = rank.tag ?: return@with
            span(className = "bd-tag-level") { +"[" }
            if (listed) colored(this, BadgePart.TAG, text)
            else span(className = "bd-pick", onClick = { onPick(BadgePart.TAG) }) { coloredText(text, rank.color, rank.to) }
            span(className = "bd-tag-level") { +"] " }
        }
    }

    private fun NodeBuilder.coloredText(text: String, color: String, to: String?) {
        if (to == null) {
            span(className = "bd-tag-text", style = "color: $color") { +text }
            return
        }
        val a = SboBadges.parseHex(color)
        val b = SboBadges.parseHex(to)
        span(className = "bd-tag-text") {
            text.forEachIndexed { i, char ->
                val t = if (text.length > 1) i / (text.length - 1f) else 0f
                span(key = "c$i", style = "color: #%06X".format(mix(a, b, t))) { +char.toString() }
            }
        }
    }

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt()
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun hex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

    // The picker works with ARGB, badge colors have no alpha
    private fun argb(hex: String): Int = SboBadges.parseHex(hex) or 0xFF000000.toInt()

    private fun background(color: String, to: String?): String = to?.let { "linear-gradient(90deg, $color, $it)" } ?: color

    private fun NodeBuilder.line(text: String) {
        div(className = "bd-info-line") { +text }
    }

    private fun NodeBuilder.setting(title: String, text: String, control: NodeBuilder.() -> Unit) {
        div(className = "bd-setting") {
            div(className = "bd-setting-text") {
                div(className = "bd-setting-title") { +title }
                div(className = "bd-hint") { +text }
            }
            control()
        }
    }
}
