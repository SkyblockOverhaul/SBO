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
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.tabs
import net.sbo.guilib.core.dsl.useEscapeBack
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
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeMotion
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.BadgeRank
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.TeamTagPlace
import net.sbo.mod.utils.http.SboApi

object BadgeGui {
    private val STYLES = listOf(SboLook.BASE_STYLE, "sbo:ui/badge/badge.css", SboLook.STYLE)
    private const val LOGO = "sbo:textures/font/badge.png"

    private const val DEFAULT_GRADIENT_END = "#FFD966"
    // Gray of the team tag brackets, like in game
    private const val TAG_BRACKETS = "#AAAAAA"
    // Shown in the preview where the rank is colored, the own rank is not known
    private const val SAMPLE_RANK = "MVP+"
    private const val RAINBOW = "linear-gradient(90deg, #FF5555, #FFFF55, #55FF55, #55FFFF, #5555FF, #FF55FF)"
    // Frame time of the preview while a part moves
    private const val PREVIEW_FRAME_MS = 50L
    // Chars a glitching char of the preview shows, like §k in game
    private const val SCRAMBLE = "ABCDEFGHKMNPRSTUVWXYZ0123456789"
    private const val SCRAMBLE_MS = 60L

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
        // The part the Colors and Effects tabs change, picked there, in the matrix or in the preview
        var picked by useState(BadgePart.NAME)
        var tab by useState(TAB_PLACES)
        // The dialog asking before leaving unsaved changes
        var leaving by useState(false)
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

        fun save(settings: BadgeSettings, close: Boolean = false) {
            saving = true
            SboApi.saveBadge(settings)
                .toJson<OwnBadgeResponse>(ignoreUnknownKeys = true) { response ->
                    mc.execute {
                        saving = false
                        if (response.success) {
                            loaded(response)
                            SboBadges.fetch()
                            if (close) GuiLib.close()
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

        val ready = load as? Load.Ready
        val unsaved = ready != null && draft != null && draft != ready.badge.settings
        fun close() = if (unsaved) leaving = true else GuiLib.close()
        // Escape asks first too; while the dialog is open its own Escape closes it
        useEscapeBack(unsaved && !saving) { leaving = true }

        val fit = useFitScale()

        div(className = "bd-window", ref = fit.ref, style = fit.style) {
            header(className = "bd-header") {
                img(LOGO, className = "bd-header-logo")
                span(className = "bd-title") { +"SBO Badge" }
                div(className = "bd-spacer")
                button(className = "sbo-close", title = "Close", onClick = { close() }) { +"×" }
            }
            div(className = "bd-body") {
                when (val state = load) {
                    Load.Loading -> {
                        div(className = "bd-center") { +"Loading your badge..." }
                        if (SboBadges.VIEWER_TOGGLES) ViewSettings()
                    }
                    is Load.Failed -> div(className = "bd-columns") {
                        div(className = "bd-col-left") { failed(state) }
                        if (SboBadges.VIEWER_TOGGLES) div(className = "bd-col-right") { ViewSettings() }
                    }
                    is Load.Ready -> {
                        val settings = draft ?: state.badge.settings!!
                        editor(
                            state.badge, settings, picked, tab, saving,
                            onPick = { picked = it }, onTab = { tab = it }, onChange = { draft = it }, onSave = { save(settings) },
                        )
                    }
                }
            }
        }

        modal(open = leaving, onClose = { leaving = false }, className = "bd-modal") {
            div(className = "bd-modal-title") { +"Leave without saving?" }
            div(className = "bd-modal-text") { +"Your changes to the badge are not saved yet." }
            div(className = "bd-modal-buttons") {
                button(className = "sbo-button", onClick = { leaving = false }) { +"Keep editing" }
                button(className = "sbo-button bd-danger", onClick = {
                    leaving = false
                    GuiLib.close()
                }) { +"Discard" }
                button(className = "sbo-button bd-primary", disabled = saving, onClick = {
                    leaving = false
                    draft?.let { save(it, close = true) }
                }) { +"Save" }
            }
        }
    }

    private val ViewSettings = component("BadgeView") {
        var inTab by useState(DataManager.sboData.badgesInTab)
        var inChat by useState(DataManager.sboData.badgesInChat)
        var animations by useState(DataManager.sboData.badgeAnimationsOfOthers)
        var motion by useState(DataManager.sboData.badgeMotionOfOthers)
        var tabAnimations by useState(DataManager.sboData.badgeAnimationsInTab)
        div(className = "bd-section-title") { +"Badges of Others" }
        setting("In the tab list", "The SBO logo behind their name and the name in their color. The SBO team always shows.") {
            switch(checked = inTab, onChange = {
                inTab = it.checked
                DataManager.sboData.badgesInTab = it.checked
                DataManager.sboData.save()
            })
        }
        setting("In chat", "The SBO logo behind their name and the name in their color, for new messages. The SBO team always shows.") {
            switch(checked = inChat, onChange = {
                inChat = it.checked
                DataManager.sboData.badgesInChat = it.checked
                DataManager.sboData.save()
            })
        }
        setting("Animations", "Moving colors (wave, pulse, chroma) of other players' badges. Off shows them standing still, your own badge always moves.") {
            switch(checked = animations, onChange = {
                animations = it.checked
                DataManager.sboData.badgeAnimationsOfOthers = it.checked
                DataManager.sboData.save()
                SboBadges.relook()
            })
        }
        setting("Motion", "Moving letters (wavy, bounce, shake, glitch) of other players' badges above their heads. Off keeps the letters still, your own badge always moves.") {
            switch(checked = motion, onChange = {
                motion = it.checked
                DataManager.sboData.badgeMotionOfOthers = it.checked
                DataManager.sboData.save()
                SboBadges.relook()
            })
        }
        setting("Animations in the tab list", "Moving colors of every badge in the tab list, your own too. Off shows them standing still.") {
            switch(checked = tabAnimations, onChange = {
                tabAnimations = it.checked
                DataManager.sboData.badgeAnimationsInTab = it.checked
                DataManager.sboData.save()
                SboBadges.relook()
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
        tab: String,
        saving: Boolean,
        onPick: (String) -> Unit,
        onTab: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
        onSave: () -> Unit,
    ) {
        val stat = badge.stats.firstOrNull { it.id == settings.stat }
        val changed = settings != badge.settings
        val rank = badge.rankInfo
        // The tag only has a row for team ranks, a picked tag falls back to the name
        val part = if (picked == BadgePart.TAG && !hasTag(badge)) BadgePart.NAME else picked

        div(className = "bd-rank-row") {
            span(className = "bd-rank", style = "background: ${background(PartColor(rank?.color ?: "#FFAA00", rank?.to), BadgePart.TAG)}") {
                +(rank?.label ?: badge.rank.orEmpty())
            }
            span(className = "bd-hint") { +if (badge.team) "Thank you for working on SBO!" else "Thank you for supporting SBO!" }
            div(className = "bd-spacer")
            // Always there, so the row does not jump while editing
            button(className = "sbo-button", disabled = saving || !changed, title = "Back to your saved badge", onClick = { onChange(badge.settings!!) }) { +"Undo" }
            button(
                className = "sbo-button bd-primary",
                disabled = saving || !changed,
                title = if (changed) "Saves your badge" else "Nothing changed",
                onClick = { onSave() },
            ) { +if (saving) "Saving..." else "Save" }
        }

        // A click on a part of the preview opens its colors
        Preview(PreviewProps(badge, settings, stat, targetOf(settings, part)) { clicked ->
            onPick(clicked)
            if (tab != TAB_COLORS && tab != TAB_EFFECTS) onTab(TAB_COLORS)
        })

        tabs(value = tab, onChange = onTab, className = "bd-tabs") {
            tab(TAB_PLACES, "Places")
            tab(TAB_COLORS, "Colors")
            tab(TAB_EFFECTS, "Effects")
            tab(TAB_STAT, "Stat")
            tab(TAB_VIEW, "View")
        }
        // Fixed height, so switching tabs or parts never changes the window's size
        scroll(className = "bd-panel guilib-autohide", key = "panel:$tab") {
            when (tab) {
                TAB_PLACES -> placesTab(badge, settings, part, onPick, onChange)
                TAB_COLORS -> colorsTab(badge, settings, part, onPick, onChange)
                TAB_EFFECTS -> effectsTab(badge, settings, part, onPick, onChange)
                TAB_STAT -> statTab(badge, settings, onChange)
                else -> {
                    OwnView()
                    if (SboBadges.VIEWER_TOGGLES) ViewSettings()
                }
            }
        }
    }

    private const val TAB_PLACES = "places"
    private const val TAB_COLORS = "colors"
    private const val TAB_EFFECTS = "effects"
    private const val TAB_STAT = "stat"
    private const val TAB_VIEW = "view"

    private fun hasTag(badge: OwnBadgeResponse): Boolean = badge.team && badge.rankInfo?.tag != null

    /** A row of the matrix: the part that shows per place and the part whose color it uses. */
    private class PartRow(val show: String, val color: String, val label: String)

    private val PLACES = listOf(TeamTagPlace.HEAD to "Head", TeamTagPlace.TAB to "Tab", TeamTagPlace.CHAT to "Chat")

    private val PART_LABELS = mapOf(
        BadgePart.STAT to "Logo + stat",
        BadgePart.NAME to "Name",
        BadgePart.LEVEL to "Level",
        BadgePart.TAG to "Team tag",
        BadgePart.RANK to "Rank",
    )

    // Short names for the part picker, in the order they stand in game
    private val PICKER_LABELS = listOf(
        BadgePart.STAT to "Stat",
        BadgePart.TAG to "Tag",
        BadgePart.LEVEL to "Level",
        BadgePart.RANK to "Rank",
        BadgePart.NAME to "Name",
    )

    private val COLOR_HINTS = mapOf(
        BadgePart.STAT to "The stat above your head.",
        BadgePart.NAME to "Your name where it is colored.",
        BadgePart.LEVEL to "Your SkyBlock level in front of your name.",
        BadgePart.TAG to "Your team tag; others see the rank colors while your badge is hidden.",
        BadgePart.RANK to "Your Hypixel rank, like [MVP+], where Hypixel shows it.",
    )

    /** Not a part of its own: tag, level, rank and name together while they flow as one line. */
    private const val LINE = "line"

    /** Tag, level, rank and name flow as one line while all of them are chained. */
    private fun flowsAsLine(settings: BadgeSettings): Boolean = BadgePart.CHAINS.all { settings.colorOf(it).chain == true }

    /** The part the Colors and Effects tabs change: the whole line while it flows as one. */
    private fun targetOf(settings: BadgeSettings, picked: String): String {
        val flows = flowsAsLine(settings)
        return when {
            picked == LINE -> if (flows) LINE else BadgePart.NAME
            picked in BadgePart.CHAINS && flows -> LINE
            else -> picked
        }
    }

    private fun partsOf(target: String): List<String> = if (target == LINE) BadgePart.CHAINS.toList() else listOf(target)

    /** The part whose look the tabs show for [target]; the line shows its name's, all parts of it look the same. */
    private fun shownPart(target: String): String = if (target == LINE) BadgePart.NAME else target

    private fun labelOf(target: String): String = if (target == LINE) "Name Line" else PART_LABELS.getValue(target)

    /** Applies [update] to every part of [target]; each part keeps what the update does not change. */
    private fun BadgeSettings.edit(target: String, update: (PartColor) -> PartColor): BadgeSettings =
        partsOf(target).fold(this) { next, part -> next.withColor(part, update(next.colorOf(part))) }

    /** The part picker of the Colors and Effects tabs with the switch that makes the name line one part. */
    private fun NodeBuilder.partPicker(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        target: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        val flows = flowsAsLine(settings)
        div(className = "bd-picker-row") {
            segmented(value = target, onChange = onPick, className = "bd-parts") {
                PICKER_LABELS.forEach { (part, label) ->
                    when {
                        part == BadgePart.STAT -> option(part, label)
                        flows -> if (part == BadgePart.NAME) option(LINE, "Name Line")
                        part != BadgePart.TAG || hasTag(badge) -> option(part, label)
                    }
                }
            }
            div(className = "bd-spacer")
            span(className = "bd-hint", title = "Tag, level, rank and name as one: you change them together and effects run over the whole line.") {
                +"Flow line"
            }
            switch(checked = flows, onChange = {
                if (it.checked) {
                    // The line takes the look of the part picked now, else of the name
                    val source = settings.colorOf(if (target in BadgePart.CHAINS) target else BadgePart.NAME)
                    onChange(settings.edit(LINE) { c -> c.copy(color = source.color, to = source.to, effect = source.effect, motion = source.motion, chain = true) })
                    onPick(LINE)
                } else {
                    onChange(settings.edit(LINE) { c -> c.copy(chain = null) })
                    onPick(BadgePart.NAME)
                }
            })
        }
    }

    private fun NodeBuilder.placesTab(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        val rows = buildList {
            add(PartRow(BadgePart.LOGO, BadgePart.STAT, PART_LABELS.getValue(BadgePart.STAT)))
            if (hasTag(badge)) add(PartRow(BadgePart.TAG, BadgePart.TAG, "Team tag"))
            add(PartRow(BadgePart.LEVEL, BadgePart.LEVEL, "Level color"))
            add(PartRow(BadgePart.RANK, BadgePart.RANK, "Rank color"))
            add(PartRow(BadgePart.NAME, BadgePart.NAME, "Name color"))
        }
        div(className = "bd-matrix") {
            div(className = "bd-mrow bd-mhead") {
                span(className = "bd-mlabel") { +"Where every part shows" }
                PLACES.forEach { (place, label) -> span(className = "bd-mcell", key = place) { +label } }
            }
            rows.forEach { row ->
                val places = if (row.show == BadgePart.TAG) settings.teamTagPlaces else settings.placesOf(row.show)
                val color = settings.colorOf(row.color)
                div(
                    className = classNames("bd-mrow", "bd-mrow-picked" to (picked == row.color)),
                    key = row.show,
                    onClick = { onPick(row.color) },
                ) {
                    div(className = "bd-mlabel") {
                        span(className = "bd-mswatch", style = "background: ${background(color, row.color)}")
                        span { +row.label }
                    }
                    PLACES.forEach { (place, _) ->
                        div(className = "bd-mcell", key = place) {
                            checkbox(checked = place in places, onChange = {
                                val next = if (it.checked) places + place else places - place
                                onChange(if (row.show == BadgePart.TAG) settings.withTeamTagPlaces(next) else settings.withPlaces(row.show, next))
                            })
                        }
                    }
                }
            }
        }
    }

    private fun NodeBuilder.colorsTab(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        val target = targetOf(settings, picked)
        partPicker(badge, settings, target, onPick, onChange)
        val shown = shownPart(target)
        val color = settings.colorOf(shown)
        val label = labelOf(target)
        val effect = color.effectOf(shown)

        setting("Color", if (target == LINE) "Tag, level, rank and name together." else COLOR_HINTS.getValue(target)) {
            div(className = "bd-colors") {
                colorInput(value = argb(color.color), onChange = { c -> onChange(settings.edit(target) { it.copy(color = hex(c)) }) }, key = "from-$target")
                color.to?.let { to ->
                    span(className = "bd-arrow") { +"→" }
                    colorInput(value = argb(to), onChange = { c -> onChange(settings.edit(target) { it.copy(to = hex(c)) }) }, key = "to-$target")
                }
            }
        }
        if (badge.gradient) {
            setting("Gradient", "A second color that the first one flows into.") {
                switch(checked = color.to != null, onChange = { e ->
                    // A wave needs both colors, without the second one it stands still
                    val keepsEffect = e.checked || effect != BadgeEffect.WAVE
                    onChange(settings.edit(target) {
                        it.copy(to = if (e.checked) DEFAULT_GRADIENT_END else null, effect = if (keepsEffect) it.effect else BadgeEffect.STATIC)
                    })
                })
            }
        }
        // The line's brackets are those of level, rank and tag together
        val bracketParts = partsOf(target).filter { it in BadgePart.BRACKETS }
        if (bracketParts.isNotEmpty()) {
            val bracketPart = bracketParts.first()
            val bracketColor = settings.colorOf(bracketPart)
            setting("Brackets", "The [ and ] around the ${label.lowercase()}.") {
                div(className = "bd-colors") {
                    span(className = "bd-arrow") { +"[" }
                    colorInput(
                        value = argb(bracketOf(bracketPart, bracketColor, close = false)),
                        onChange = { c -> onChange(bracketParts.fold(settings) { next, part -> next.withColor(part, next.colorOf(part).copy(open = hex(c))) }) },
                        key = "open-$target",
                    )
                    span(className = "bd-arrow") { +"]" }
                    colorInput(
                        value = argb(bracketOf(bracketPart, bracketColor, close = true)),
                        onChange = { c -> onChange(bracketParts.fold(settings) { next, part -> next.withColor(part, next.colorOf(part).copy(close = hex(c))) }) },
                        key = "close-$target",
                    )
                }
            }
        }
        if (BadgePart.RANK in partsOf(target)) {
            val rank = settings.colorOf(BadgePart.RANK)
            setting("Plus", "The + of VIP+, MVP+ and MVP++.") {
                colorInput(value = argb(rank.plus ?: rank.to ?: rank.color), onChange = { c -> onChange(settings.edit(BadgePart.RANK) { it.copy(plus = hex(c)) }) }, key = "plus")
            }
        }
        setting("All parts", "Gives every part these colors and this effect; brackets and plus stay.") {
            button(className = "sbo-button", onClick = {
                onChange(PART_LABELS.keys.fold(settings) { next, part -> next.withColor(part, next.colorOf(part).copy(color = color.color, to = color.to, effect = effect)) })
            }) { +"Use for all" }
        }
    }

    private fun NodeBuilder.effectsTab(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        val target = targetOf(settings, picked)
        partPicker(badge, settings, target, onPick, onChange)
        val shown = shownPart(target)
        val color = settings.colorOf(shown)
        val effect = color.effectOf(shown)
        setting("Effect", EFFECT_HINTS.getValue(effect)) {
            select(
                value = effect,
                onChange = { e ->
                    // The wave flows between two colors, so it brings the gradient with it
                    onChange(settings.edit(target) {
                        it.copy(effect = e.value, to = if (e.value == BadgeEffect.WAVE && badge.gradient) it.to ?: DEFAULT_GRADIENT_END else it.to)
                    })
                },
                className = "bd-select",
                key = "effect-$target",
            ) {
                BadgeEffect.ALL.forEach { option(it, EFFECT_LABELS.getValue(it)) }
            }
        }
        val motion = color.motion ?: BadgeMotion.NONE
        setting("Motion", MOTION_HINTS.getValue(motion)) {
            select(
                value = motion,
                onChange = { e -> onChange(settings.edit(target) { it.copy(motion = e.value.takeUnless { m -> m == BadgeMotion.NONE }) }) },
                className = "bd-select",
                key = "motion-$target",
            ) {
                BadgeMotion.ALL.forEach { option(it, MOTION_LABELS.getValue(it)) }
            }
        }
        div(className = "bd-hint bd-note") {
            +"Motion only shows above heads. Chat lines stay as they came in, so chat shows colors standing still. Others can turn animations off for themselves."
        }
    }

    private val MOTION_LABELS = mapOf(
        BadgeMotion.NONE to "None",
        BadgeMotion.WAVY to "Wavy",
        BadgeMotion.BOUNCE to "Bounce",
        BadgeMotion.SHAKE to "Shake",
        BadgeMotion.GLITCH to "Glitch",
    )

    private val MOTION_HINTS = mapOf(
        BadgeMotion.NONE to "The chars stay in place.",
        BadgeMotion.WAVY to "The chars ride a wave along the text.",
        BadgeMotion.BOUNCE to "One char after the other hops.",
        BadgeMotion.SHAKE to "The chars jitter.",
        BadgeMotion.GLITCH to "Single chars flicker scrambled, like §k.",
    )

    private val EFFECT_LABELS = mapOf(
        BadgeEffect.STATIC to "None",
        BadgeEffect.WAVE to "Wave",
        BadgeEffect.PULSE to "Pulse",
        BadgeEffect.CHROMA to "Chroma",
        BadgeEffect.SHINE to "Shine",
        BadgeEffect.FLASH to "Flash",
        BadgeEffect.SPARKLE to "Sparkle",
    )

    private val EFFECT_HINTS = mapOf(
        BadgeEffect.SHINE to "A white gleam sweeps over the text now and then.",
        BadgeEffect.FLASH to "Jumps between both colors, or to a light version of one color.",
        BadgeEffect.SPARKLE to "Single chars twinkle white.",
        BadgeEffect.STATIC to "The colors stand still.",
        BadgeEffect.WAVE to "The gradient flows along the text.",
        BadgeEffect.PULSE to "Fades between both colors, or towards white with one color.",
        BadgeEffect.CHROMA to "A rainbow runs along the text, your colors are not used.",
    )

    private fun NodeBuilder.statTab(badge: OwnBadgeResponse, settings: BadgeSettings, onChange: (BadgeSettings) -> Unit) {
        setting("Stat", "Shown above your head next to the logo, read from your SkyBlock profile.") {
            select(
                value = settings.stat,
                onChange = { onChange(settings.copy(stat = it.value)) },
                className = "bd-select",
                searchable = true,
                searchPlaceholder = "Search..."
            ) {
                // "Logo only" first, the stats by name
                badge.stats.sortedWith(compareBy({ it.id != "none" }, { it.label.lowercase() })).forEach { option(it.id, it.label) }
            }
        }
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

    private data class PreviewProps(
        val badge: OwnBadgeResponse,
        val settings: BadgeSettings,
        val stat: BadgeChoice?,
        val picked: String,
        val onPick: (String) -> Unit,
    )

    // Its own component, so moving colors only draw the preview again
    private val Preview = component<PreviewProps>("BadgePreview") { (badge, settings, stat, picked, onPick) ->
        var now by useState(System.currentTimeMillis())
        val moving = PART_LABELS.keys.any { SboBadges.moves(settings.colorOf(it), it) }
        useInterval(PREVIEW_FRAME_MS) { if (moving) now = System.currentTimeMillis() }
        preview(badge, settings, stat, picked, onPick, now)
    }

    private fun NodeBuilder.preview(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        stat: BadgeChoice?,
        picked: String,
        onPick: (String) -> Unit,
        now: Long,
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
        val level = badge.level?.toString().orEmpty()
        // Others only get the badge while it shows somewhere and the server does not hide it
        val listed = settings.enabled && !badge.hiddenByServer
        fun shows(part: String, place: String) = listed && place in settings.placesOf(part)
        val tagAt = { place: String -> hasTag(badge) && place in settings.teamTagPlaces }
        val atHead = shows(BadgePart.LOGO, TeamTagPlace.HEAD) || shows(BadgePart.NAME, TeamTagPlace.HEAD) ||
            shows(BadgePart.LEVEL, TeamTagPlace.HEAD) || shows(BadgePart.RANK, TeamTagPlace.HEAD) || tagAt(TeamTagPlace.HEAD)
        val rankInfo = badge.rankInfo
        val tagText = rankInfo?.tag
        // Others see the rank colors while the badge is hidden
        val tagColor = if (listed) settings.colorOf(BadgePart.TAG) else rankInfo?.let { PartColor(it.color, it.to) }
        // The parts of a place laid out like the line in game, so chained parts share one wave
        fun paintsAt(place: String): Map<String, Painted> {
            val segments = ArrayList<SboBadges.Segment>()
            var pos = 0
            fun add(part: String, length: Int, colored: Boolean, color: PartColor? = settings.colorOf(part), space: Int = 0) {
                if (colored && color != null) segments += SboBadges.Segment(part, color, pos until pos + length)
                pos += length + space
            }
            // "[SBO Dev]" without its space, like the tag in game
            fun tag() = tagText?.let { add(BadgePart.TAG, it.length + 2, tagAt(place), tagColor, space = 1) }
            if (place == TeamTagPlace.HEAD) tag()
            // "[465] " and "[MVP+] " with their space, like the ranges in game
            if (level.isNotEmpty()) add(BadgePart.LEVEL, level.length + 3, shows(BadgePart.LEVEL, place))
            if (shows(BadgePart.RANK, place)) add(BadgePart.RANK, SAMPLE_RANK.length + 3, true)
            if (place != TeamTagPlace.HEAD) tag()
            add(BadgePart.NAME, name.length, shows(BadgePart.NAME, place))
            // Only a space may stand between chained parts
            val paints = SboBadges.paints(segments) { before, next -> next.range.first - before.range.last <= 2 }
            // Like in game: chars only move above heads, chat lines keep the colors they came in with
            return segments.indices.associate {
                segments[it].part to Painted(segments[it].range.first, paints[it], moves = place == TeamTagPlace.HEAD, animate = place != TeamTagPlace.CHAT)
            }
        }
        val head = paintsAt(TeamTagPlace.HEAD)
        val tab = paintsAt(TeamTagPlace.TAB)
        val chat = paintsAt(TeamTagPlace.CHAT)
        val parts = PreviewParts(badge, settings, picked, onPick, listed, now)

        div(className = "bd-preview-row") {
            div(className = classNames("bd-preview", "bd-hidden" to !atHead)) {
                if (shows(BadgePart.LOGO, TeamTagPlace.HEAD)) {
                    div(className = "bd-tag") {
                        img(LOGO, className = "bd-tag-logo")
                        if (text != null) parts.colored(this, BadgePart.STAT, text)
                    }
                }
                div(className = "bd-tag bd-tag-name") {
                    if (tagAt(TeamTagPlace.HEAD)) parts.tag(this, head[BadgePart.TAG])
                    parts.bracketed(this, BadgePart.LEVEL, level, head[BadgePart.LEVEL])
                    if (shows(BadgePart.RANK, TeamTagPlace.HEAD)) parts.bracketed(this, BadgePart.RANK, SAMPLE_RANK, head[BadgePart.RANK])
                    parts.plainOr(this, BadgePart.NAME, name, head[BadgePart.NAME])
                }
            }
            div(className = "bd-lines") {
                div(className = "bd-line") {
                    span(className = "bd-line-label") { +"Tab" }
                    div(className = "bd-line-box bd-line-tab") {
                        parts.bracketed(this, BadgePart.LEVEL, level, tab[BadgePart.LEVEL])
                        if (shows(BadgePart.RANK, TeamTagPlace.TAB)) parts.bracketed(this, BadgePart.RANK, SAMPLE_RANK, tab[BadgePart.RANK])
                        if (tagAt(TeamTagPlace.TAB)) parts.tag(this, tab[BadgePart.TAG])
                        parts.plainOr(this, BadgePart.NAME, name, tab[BadgePart.NAME])
                        if (shows(BadgePart.LOGO, TeamTagPlace.TAB)) img(LOGO, className = "bd-tag-logo bd-line-logo")
                    }
                }
                div(className = "bd-line") {
                    span(className = "bd-line-label") { +"Chat" }
                    div(className = "bd-line-box bd-line-chat") {
                        parts.bracketed(this, BadgePart.LEVEL, level, chat[BadgePart.LEVEL])
                        if (shows(BadgePart.RANK, TeamTagPlace.CHAT)) parts.bracketed(this, BadgePart.RANK, SAMPLE_RANK, chat[BadgePart.RANK])
                        if (tagAt(TeamTagPlace.CHAT)) parts.tag(this, chat[BadgePart.TAG])
                        parts.plainOr(this, BadgePart.NAME, name, chat[BadgePart.NAME])
                        if (shows(BadgePart.LOGO, TeamTagPlace.CHAT)) img(LOGO, className = "bd-tag-logo bd-line-logo")
                        span(className = "bd-tag-plain") { +": Hello!" }
                    }
                }
            }
        }
        // Always there with a fixed height, so a message coming or going does not move the tabs
        div(className = "bd-hint bd-status") {
            when {
                badge.hiddenByServer -> +"Hidden by the SBO server, nobody sees your badge right now."
                !settings.enabled -> +"Nothing is ticked, nobody sees your badge."
                stat != null && settings.stat != "none" && value == null && profileRead -> +"Your profile has no ${stat.label} yet, only the logo shows."
                text != null && value == null -> +"The value is read from your profile after saving."
                else -> +"Click a part to change its colors."
            }
        }
    }

    /** A part of a preview line: where it starts in the line and the colors of its chain. */
    private class Painted(val start: Int, val paint: SboBadges.Paint, val moves: Boolean, val animate: Boolean) {
        fun at(i: Int, now: Long): Int = paint.at(start + i, animate = animate, now = now)
        fun drop(i: Int, now: Long): Int = if (moves) paint.dropAt(start + i, now) else 0
        fun glitch(i: Int, now: Long): Boolean = moves && paint.glitchAt(start + i, now)
    }

    /** Clickable parts of the preview: a click picks the part for the color picker. */
    private class PreviewParts(
        val badge: OwnBadgeResponse,
        val settings: BadgeSettings,
        val picked: String,
        val onPick: (String) -> Unit,
        val listed: Boolean,
        val now: Long,
    ) {
        /** The whole name line shows as picked while it flows as one. */
        fun isPicked(part: String): Boolean = picked == part || (picked == LINE && part in BadgePart.CHAINS)

        fun colored(builder: NodeBuilder, part: String, text: String) = with(builder) {
            val color = settings.colorOf(part)
            span(
                className = classNames("bd-pick", "bd-picked" to isPicked(part)),
                title = "Click to change the ${PART_LABELS.getValue(part).lowercase()} color",
                onClick = { onPick(part) },
            ) { coloredText(text, color, part, now) }
        }

        /** "[465] " or "[MVP+] ": in its chain's colors with its own brackets and plus where it is colored, gray otherwise. */
        fun bracketed(builder: NodeBuilder, part: String, text: String, painted: Painted?) = with(builder) {
            if (text.isEmpty()) return@with
            if (painted == null) {
                span(className = "bd-tag-level") { +"[$text] " }
                return@with
            }
            val color = settings.colorOf(part)
            span(
                className = classNames("bd-pick", "bd-picked" to isPicked(part)),
                title = "Click to change the ${PART_LABELS.getValue(part).lowercase()} color",
                onClick = { onPick(part) },
            ) {
                span(className = "bd-tag-text") {
                    "[$text]".forEachIndexed { i, char ->
                        // Like in game: brackets and plus without their own color take the chain's color at their place
                        val own = when {
                            char == '[' -> color.open
                            char == ']' -> color.close
                            char == '+' && part == BadgePart.RANK -> color.plus
                            else -> null
                        }
                        glyph(i, char, own?.let(SboBadges::parseHex) ?: painted.at(i, now), painted, now)
                    }
                }
            }
            span(className = "bd-tag-plain") { +" " }
        }

        /** The part in its chain's colors where it is colored, plain otherwise. */
        fun plainOr(builder: NodeBuilder, part: String, text: String, painted: Painted?) = with(builder) {
            if (text.isEmpty()) return@with
            if (painted == null) {
                span(className = if (part == BadgePart.LEVEL) "bd-tag-level" else "bd-tag-plain") { +text }
                return@with
            }
            span(
                className = classNames("bd-pick", "bd-picked" to isPicked(part)),
                title = "Click to change the ${PART_LABELS.getValue(part).lowercase()} color",
                onClick = { onPick(part) },
            ) {
                span(className = "bd-tag-text") {
                    text.forEachIndexed { i, char -> glyph(i, char, painted.at(i, now), painted, now) }
                }
            }
        }

        /** "[SBO Owner] " in its chain's colors; others see the rank colors and gray brackets while the badge is hidden. */
        fun tag(builder: NodeBuilder, painted: Painted?) = with(builder) {
            val text = badge.rankInfo?.tag ?: return@with
            if (painted == null) return@with
            val color = settings.colorOf(BadgePart.TAG)
            span(
                className = classNames("bd-pick", "bd-picked" to isPicked(BadgePart.TAG)),
                title = "Click to change the team tag color",
                onClick = { onPick(BadgePart.TAG) },
            ) {
                span(className = "bd-tag-text") {
                    "[$text]".forEachIndexed { i, char ->
                        val own = when {
                            !listed -> null
                            i == 0 -> color.open
                            i == text.length + 1 -> color.close
                            else -> null
                        }
                        // Gray without an own color, unless the tag flows in the name line
                        val bracket = (i == 0 || i == text.length + 1) && !(listed && color.chain == true)
                        val rgb = own?.let(SboBadges::parseHex) ?: if (bracket) SboBadges.parseHex(TAG_BRACKETS) else painted.at(i, now)
                        glyph(i, char, rgb, painted, now)
                    }
                }
            }
            span(className = "bd-tag-plain") { +" " }
        }
    }

    /** The text as in game; your own badge always moves, so the preview does too. */
    private fun NodeBuilder.coloredText(text: String, color: PartColor, part: String, now: Long) {
        if (color.to == null && color.effectOf(part) == BadgeEffect.STATIC && color.motion == null) {
            span(className = "bd-tag-text", style = "color: ${color.color}") { +text }
            return
        }
        span(className = "bd-tag-text") {
            text.forEachIndexed { i, char ->
                val rgb = SboBadges.colorAt(color, part, i, text.length, animate = true, now = now)
                glyph(i, char, rgb, SboBadges.dropAt(color.motion, i, text.length, now), SboBadges.glitchAt(color.motion, i, text.length, now), now)
            }
        }
    }

    private fun NodeBuilder.glyph(i: Int, char: Char, rgb: Int, painted: Painted, now: Long) =
        glyph(i, char, rgb, painted.drop(i, now), painted.glitch(i, now), now)

    /** One char of the preview: lower by [drop] pixels or scrambled, like the shifted fonts and §k in game. */
    private fun NodeBuilder.glyph(i: Int, char: Char, rgb: Int, drop: Int, glitch: Boolean, now: Long) {
        // Only plain chars move in game, the shifted fonts only have those
        val plain = char in '!'..'~'
        val shown = if (glitch && plain) SCRAMBLE[((now / SCRAMBLE_MS + i * 7) % SCRAMBLE.length).toInt()] else char
        // A lower baseline, which keeps the line's height
        val lower = if (plain && drop > 0) "; vertical-align: -${drop}px" else ""
        span(key = "c$i", style = "color: #%06X$lower".format(rgb)) { +shown.toString() }
    }

    /** A bracket as it looks in game: its own color, else gray for the team tag and the part's color (gradient start or end) otherwise. */
    private fun bracketOf(part: String, color: PartColor, close: Boolean): String = (if (close) color.close else color.open)
        ?: if (part == BadgePart.TAG && color.chain != true) TAG_BRACKETS else if (close) color.to ?: color.color else color.color

    private fun hex(color: Int): String = "#%06X".format(color and 0xFFFFFF)

    // The picker works with ARGB, badge colors have no alpha
    private fun argb(hex: String): Int = SboBadges.parseHex(hex) or 0xFF000000.toInt()

    private fun background(color: PartColor, part: String): String = when {
        color.effectOf(part) == BadgeEffect.CHROMA -> RAINBOW
        color.to != null -> "linear-gradient(90deg, ${color.color}, ${color.to})"
        else -> color.color
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
