package net.sbo.mod.guis

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.general.SboBadges
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeChoice
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.http.SboApi

object BadgeGui {
    private val STYLES = listOf("sbo:ui/badge/badge.css", SboLook.STYLE)
    private const val LOGO = "sbo:textures/font/badge.png"

    private val RANK_LABELS = mapOf("owner" to "Owner", "dev" to "SBO Dev", "maintainer" to "Maintainer", "supporter" to "Supporter")

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

        div(className = "bd-window") {
            header(className = "bd-header") {
                img(LOGO, className = "bd-header-logo")
                span(className = "bd-title") { +"SBO Badge" }
                div(className = "bd-spacer")
                button(className = "bd-icon bd-close", title = "Close", onClick = { GuiLib.close() }) { +"✕" }
            }
            scroll(className = "bd-body guilib-autohide") {
                if (SboBadges.VIEWER_TOGGLES) {
                    ViewSettings()
                    div(className = "bd-section-title") { +"Your Badge" }
                }
                when (val state = load) {
                    Load.Loading -> div(className = "bd-center") { +"Loading your badge..." }
                    is Load.Failed -> failed(state)
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
        div(className = "bd-section-title") { +"Badges of Others" }
        setting("Above heads", "The SBO logo, the chosen stat and the colored name of supporters, maintainers and devs.") {
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
    }

    private fun NodeBuilder.failed(state: Load.Failed) {
        if (!state.notEligible) {
            div(className = "bd-error") { +state.message }
            return
        }
        div(className = "bd-hint bd-intro") {
            +"Supporters (Patreon or Ko-fi), maintainers and devs get their own badge: the SBO logo, a stat of their choice and their name in their color."
        }
        div(className = "bd-steps") {
            div(className = "bd-steps-title") { +"Already a supporter? Check these:" }
            line("1. Your Minecraft account is linked to your Discord account: type /link followed by your Minecraft name on the SBO Discord.")
            line("2. Use the Discord account that has the Patreon or Ko-fi supporter role, or the maintainer or dev role.")
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
        val color = badge.colors.firstOrNull { it.id == settings.color } ?: badge.colors.first()
        val stat = badge.stats.firstOrNull { it.id == settings.stat }
        val changed = settings != badge.settings

        div(className = "bd-rank-row") {
            span(className = classNames("bd-rank", "bd-rank-${badge.rank}")) { +(RANK_LABELS[badge.rank] ?: badge.rank.orEmpty()) }
            span(className = "bd-hint") { +if (badge.rank == "supporter") "Thank you for supporting SBO!" else "Thank you for working on SBO!" }
        }

        preview(badge, settings, color, stat)

        setting("Show my badge", "Others with SBO see it above your head and in the tab list.") {
            switch(checked = settings.enabled, onChange = { onChange(settings.copy(enabled = it.checked)) })
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

        div(className = "bd-setting bd-colors-setting") {
            div(className = "bd-setting-text") {
                div(className = "bd-setting-title") { +"Color" }
                div(className = "bd-hint") { +"Red is for the SBO devs." }
            }
            div(className = "bd-colors") {
                badge.colors.forEach { choice ->
                    button(
                        className = classNames("bd-swatch", "bd-selected" to (choice.id == settings.color)),
                        key = choice.id,
                        title = choice.label,
                        style = "background: ${swatchBackground(choice)}",
                        onClick = { onChange(settings.copy(color = choice.id)) },
                    )
                }
            }
        }

        div(className = "bd-actions") {
            button(
                className = "bd-primary",
                disabled = saving || !changed,
                title = if (changed) "Saves your badge" else "Nothing changed",
                onClick = { onSave() },
            ) { +if (saving) "Saving..." else "Save" }
            if (changed) button(disabled = saving, title = "Back to your saved badge", onClick = { onChange(badge.settings!!) }) { +"Undo" }
        }
    }

    private fun NodeBuilder.preview(badge: OwnBadgeResponse, settings: BadgeSettings, color: BadgeChoice, stat: BadgeChoice?) {
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
        val level = badge.level?.let { "[$it] " }.orEmpty()
        div(className = classNames("bd-preview", "bd-hidden" to (!settings.enabled || badge.hiddenByServer))) {
            div(className = "bd-tag") {
                img(LOGO, className = "bd-tag-logo")
                if (text != null) coloredText(text, color)
            }
            div(className = "bd-tag bd-tag-name") {
                when {
                    settings.colorName && (settings.colorLevel || level.isEmpty()) -> coloredText(level + name, color)
                    settings.colorName -> {
                        span(className = "bd-tag-level") { +level }
                        coloredText(name, color)
                    }
                    settings.colorLevel && level.isNotEmpty() -> {
                        coloredText(level, color)
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

    private fun NodeBuilder.coloredText(text: String, color: BadgeChoice) {
        val from = color.hex ?: "#FFFFFF"
        val to = color.to
        if (to == null) {
            span(className = "bd-tag-text", style = "color: $from") { +text }
            return
        }
        val a = SboBadges.parseHex(from)
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

    private fun swatchBackground(choice: BadgeChoice): String {
        val hex = choice.hex ?: "#FFFFFF"
        return choice.to?.let { "linear-gradient(135deg, $hex, $it)" } ?: hex
    }

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
