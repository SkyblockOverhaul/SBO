package net.sbo.mod.guis

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
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
import net.sbo.mod.utils.data.BadgeRank
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
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
                        editor(state.badge, settings, saving, onChange = { draft = it }, onSave = { save(settings) })
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
        setting("In chat", "The SBO logo behind their name and the name in their color, for new messages.") {
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
        saving: Boolean,
        onChange: (BadgeSettings) -> Unit,
        onSave: () -> Unit,
    ) {
        val stat = badge.stats.firstOrNull { it.id == settings.stat }
        val changed = settings != badge.settings
        val rank = badge.rankInfo

        div(className = "bd-col-left") {
            div(className = "bd-rank-row") {
                span(className = "bd-rank", style = "background: ${background(rank?.color ?: "#FFAA00", rank?.to)}") {
                    +(rank?.label ?: badge.rank.orEmpty())
                }
                span(className = "bd-hint") { +if (badge.team) "Thank you for working on SBO!" else "Thank you for supporting SBO!" }
            }

            preview(badge, settings, stat)

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
            settings(badge, settings, rank, onChange)
        }
    }

    private fun NodeBuilder.settings(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        rank: BadgeRank?,
        onChange: (BadgeSettings) -> Unit,
    ) {
        div(className = "bd-section-title") { +"Your Badge" }

        setting("Show my badge", "Others with SBO see it above your head and in the tab list.") {
            switch(checked = settings.enabled, onChange = { onChange(settings.copy(enabled = it.checked)) })
        }

        if (badge.team && rank?.tag != null) {
            setting("Show team tag", "Puts [${rank.tag}] in front of your name above your head, so players know they can ask you about SBO.") {
                switch(checked = settings.teamTag, onChange = { onChange(settings.copy(teamTag = it.checked)) })
            }
        }

        setting("Color my name", "Your name above your head and in the tab list takes the color.") {
            switch(checked = settings.colorName, onChange = { onChange(settings.copy(colorName = it.checked)) })
        }

        setting("Color my level", "Your SkyBlock level in front of your name takes the color too.") {
            switch(checked = settings.colorLevel, onChange = { onChange(settings.copy(colorLevel = it.checked)) })
        }

        setting("Stat", "Read from your SkyBlock profile, like in the party finder.") {
            select(value = settings.stat, onChange = { onChange(settings.copy(stat = it.value)) }, className = "bd-select") {
                badge.stats.forEach { option(it.id, it.label) }
            }
        }

        if (badge.gradient) {
            setting("Gradient", "Two colors that flow into each other.") {
                switch(checked = settings.colorTo != null, onChange = {
                    onChange(settings.copy(colorTo = if (it.checked) badge.settings?.colorTo ?: DEFAULT_GRADIENT_END else null))
                })
            }
        }

        div(className = "bd-setting bd-colors-setting") {
            div(className = "bd-setting-text") {
                div(className = "bd-setting-title") { +if (settings.colorTo != null) "Colors" else "Color" }
                div(className = "bd-hint") { +"For your stat, and your name and level when they are colored." }
            }
            div(className = "bd-colors") {
                colorInput(value = argb(settings.color), onChange = { onChange(settings.copy(color = hex(it))) }, key = "from")
                settings.colorTo?.let { to ->
                    span(className = "bd-arrow") { +"\u2192" }
                    colorInput(value = argb(to), onChange = { onChange(settings.copy(colorTo = hex(it))) }, key = "to")
                    div(className = "bd-gradient", style = "background: ${background(settings.color, to)}")
                }
            }
        }

        OwnView()
    }

    // Only on this PC, so it changes right away without saving
    private val OwnView = component("BadgeOwnView") {
        var inF5 by useState(DataManager.sboData.badgeOwnInF5)
        setting("Show my badge in F5", "See your own name tag, badge and team tag in third person. Only on this PC.") {
            switch(checked = inF5, onChange = {
                inF5 = it.checked
                DataManager.sboData.badgeOwnInF5 = it.checked
                DataManager.sboData.save()
            })
        }
    }

    private fun NodeBuilder.preview(badge: OwnBadgeResponse, settings: BadgeSettings, stat: BadgeChoice?) {
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
        val color = settings.color
        val to = settings.colorTo
        val tag = badge.rankInfo?.takeIf { badge.team && settings.teamTag }
        div(className = classNames("bd-preview", "bd-hidden" to (!settings.enabled || badge.hiddenByServer))) {
            div(className = "bd-tag") {
                img(LOGO, className = "bd-tag-logo")
                if (text != null) coloredText(text, color, to)
            }
            div(className = "bd-tag bd-tag-name") {
                tag?.tag?.let { tagText ->
                    span(className = "bd-tag-level") { +"[" }
                    coloredText(tagText, tag.color, tag.to)
                    span(className = "bd-tag-level") { +"] " }
                }
                when {
                    settings.colorName && (settings.colorLevel || level.isEmpty()) -> coloredText(level + name, color, to)
                    settings.colorName -> {
                        span(className = "bd-tag-level") { +level }
                        coloredText(name, color, to)
                    }
                    settings.colorLevel && level.isNotEmpty() -> {
                        coloredText(level, color, to)
                        span(className = "bd-tag-plain") { +name }
                    }
                    else -> {
                        span(className = "bd-tag-level") { +level }
                        span(className = "bd-tag-plain") { +name }
                    }
                }
            }
        }
        when {
            badge.hiddenByServer -> div(className = "bd-hint bd-center-text") { +"Hidden by the SBO server, nobody sees your badge right now." }
            !settings.enabled -> div(className = "bd-hint bd-center-text") { +"Hidden, nobody sees your badge." }
            stat != null && settings.stat != "none" && value == null && profileRead ->
                div(className = "bd-hint bd-center-text") { +"Your profile has no ${stat.label} yet, only the logo shows." }
            text != null && value == null -> div(className = "bd-hint bd-center-text") { +"The value is read from your profile after saving." }
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
