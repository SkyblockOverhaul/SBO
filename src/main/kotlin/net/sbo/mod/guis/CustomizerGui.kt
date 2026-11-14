package net.sbo.mod.guis

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.colorInput
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.entity
import net.sbo.guilib.core.dsl.header
import net.sbo.guilib.core.dsl.img
import net.sbo.guilib.core.dsl.modal
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.segmented
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.guilib.core.dsl.useEscapeBack
import net.sbo.guilib.core.dsl.useToast
import net.sbo.guilib.fabric.GuiLib
import net.sbo.guilib.fabric.entity.FakePlayer
import net.sbo.guilib.fabric.text
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.general.SboBadges
import net.sbo.mod.guis.look.SboLook
import net.sbo.mod.guis.look.useFitScale
import net.sbo.mod.guis.look.useSboScale
import net.sbo.mod.guis.look.useSboTheme
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.BadgeColors
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeLook
import net.sbo.mod.utils.data.BadgeMotion
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.BadgePlaces
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.SboBadge
import net.sbo.mod.utils.data.TeamTagPlace
import net.sbo.mod.utils.http.SboApi
import java.util.Locale
import java.util.Optional
import kotlin.math.roundToInt

/**
 * SBO Customizer (/sbocustomizer): your badge, the look of your name and your player size.
 * Left the real player model with the name tag, tab line and chat line as others see them, built by the same code
 * as in game; right everything of one part in one place.
 */
object CustomizerGui {
    private val STYLES = listOf("sbo:ui/customizer/customizer.css", SboLook.STYLE)
    private const val LOGO = "sbo:textures/font/badge.png"

    private const val DEFAULT_GRADIENT_END = "#FFD966"
    // The preview's rank, the own Hypixel rank is not known
    private const val SAMPLE_RANK = "§b[MVP§c+§b] "
    private const val FRAME_MS = 20L
    // Height of the model at the largest size; smaller sizes shrink from there, so the size shows as it is
    private const val MODEL_HEIGHT = 150f
    private const val MODEL_MAX_SIZE = 1.75f

    fun open() {
        GuiLib.open(App, STYLES, title = "SBO Customizer")
    }

    // Sections of the editor: the parts, then your player and how others' badges show for you
    private const val PLAYER = "player"
    private const val OTHERS = "others"
    /** Not a part of its own: tag, level, rank and name while they share one look. */
    private const val LINE = "line"

    private sealed interface Load {
        data object Loading : Load
        data class Failed(val message: String, val notEligible: Boolean) : Load
        data class Ready(val badge: OwnBadgeResponse) : Load
    }

    private val App = component("Customizer") {
        val toast = useToast()
        var load by useState<Load>(Load.Loading)
        var draft by useState<BadgeSettings?>(null)
        var saving by useState(false)
        var section by useState(BadgePart.NAME)
        // The part the symbols row in Player changes
        var symbolPart by useState(BadgePart.NAME)
        var leaving by useState(false)
        // Here and not in Others, so the stage shows the text sizes while the sliders move
        var textSizes by useState(TextSizes(SboBadges.lineSize(), SboBadges.nameSize()))
        useSboScale()
        useSboTheme("customizer")

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
                            toast.success("Others see your new look within a few minutes.", title = "Saved")
                        } else {
                            toast.error(response.error ?: "Could not save.", title = "SBO Customizer", durationMs = 6000)
                        }
                    }
                }
                .error {
                    mc.execute {
                        saving = false
                        toast.error("Could not reach the SBO server: ${it.message}", title = "SBO Customizer", durationMs = 6000)
                    }
                }
        }

        val ready = load as? Load.Ready
        val settings = ready?.let { draft ?: it.badge.settings }
        val unsaved = ready != null && settings != null && settings != ready.badge.settings
        fun close() = if (unsaved) leaving = true else GuiLib.close()
        useEscapeBack(unsaved && !saving) { leaving = true }
        val fit = useFitScale()

        div(className = "cz-window", ref = fit.ref, style = fit.style) {
            header(className = "cz-header") {
                img(LOGO, className = "cz-logo")
                span(className = "cz-title") { +"SBO Customizer" }
                ready?.badge?.rankInfo?.let { rank ->
                    span(className = "cz-rank", style = "background: ${background(PartColor(rank.color, rank.to), BadgePart.TAG)}") { +rank.label }
                }
                div(className = "cz-spacer")
                if (ready != null && settings != null) {
                    button(disabled = saving || !unsaved, title = "Back to what is saved", onClick = { draft = ready.badge.settings }) { +"Undo" }
                    button(className = "cz-primary", disabled = saving || !unsaved, onClick = { save(settings) }) {
                        +if (saving) "Saving..." else "Save"
                    }
                }
                button(className = "cz-icon cz-close", title = "Close", onClick = { close() }) { +"x" }
            }
            div(className = "cz-body") {
                Stage(StageProps(ready?.badge, settings, textSizes))
                div(className = "cz-editor") {
                    when (val state = load) {
                        Load.Loading -> {
                            div(className = "cz-center") { +"Loading..." }
                        }
                        is Load.Failed -> scroll(className = "cz-panel cz-panel-tall guilib-autohide") {
                            failed(state)
                            heading("How others' badges show for you")
                            Others(OthersProps(textSizes) { textSizes = it })
                        }
                        is Load.Ready -> {
                            val badge = state.badge
                            val current = settings!!
                            nav(badge, section) { section = it }
                            scroll(className = "cz-panel guilib-autohide", key = "panel:$section") {
                                when (section) {
                                    PLAYER -> playerPanel(badge, current, symbolPart, { symbolPart = it }) { draft = it }
                                    OTHERS -> Others(OthersProps(textSizes) { textSizes = it })
                                    else -> partPanel(badge, current, section) { draft = it }
                                }
                            }
                        }
                    }
                }
            }
        }

        modal(open = leaving, onClose = { leaving = false }, className = "cz-modal") {
            div(className = "cz-modal-title") { +"Leave without saving?" }
            div(className = "cz-modal-text") { +"Your changes are not saved yet." }
            div(className = "cz-modal-buttons") {
                button(onClick = { leaving = false }) { +"Keep editing" }
                button(className = "cz-danger", onClick = {
                    leaving = false
                    GuiLib.close()
                }) { +"Discard" }
                button(className = "cz-primary", disabled = saving, onClick = {
                    leaving = false
                    draft?.let { save(it, close = true) }
                }) { +"Save" }
            }
        }
    }

    // ---- Navigation -------------------------------------------------------------------------------------------

    private fun NodeBuilder.nav(badge: OwnBadgeResponse, section: String, onSection: (String) -> Unit) {
        segmented(value = section, onChange = onSection, className = "cz-nav") {
            option(BadgePart.STAT, "Logo & Stat")
            if (hasTag(badge)) option(BadgePart.TAG, "Tag")
            option(BadgePart.LEVEL, "Level")
            option(BadgePart.RANK, "Rank")
            option(BadgePart.NAME, "Name")
            option(PLAYER, "Player")
            option(OTHERS, "Others")
        }
    }

    private fun hasTag(badge: OwnBadgeResponse): Boolean = badge.team && badge.rankInfo?.tag != null

    // ---- One part: where it shows, colors, effect, style -------------------------------------------------------

    private val PLACES = listOf(TeamTagPlace.HEAD to "Head", TeamTagPlace.TAB to "Tab", TeamTagPlace.CHAT to "Chat")

    private const val BOLD = "bold"
    private const val ITALIC = "italic"
    private const val UNDERLINE = "underline"
    private const val STRIKE = "strike"

    private fun NodeBuilder.partPanel(badge: OwnBadgeResponse, settings: BadgeSettings, part: String, onChange: (BadgeSettings) -> Unit) {
        if (part == BadgePart.STAT) {
            row("Logo", "The SBO logo: above your head, behind your name in the tab list and in chat.") {
                val logo = settings.placesOf(BadgePart.LOGO)
                chips(
                    values = PLACES.map { it.first }.filter { it in logo },
                    onChange = { picked -> onChange(settings.withPlaces(BadgePart.LOGO, picked.toSet())) },
                    className = "cz-chips",
                    key = "places-logo",
                ) { PLACES.forEach { (place, label) -> option(place, label) } }
            }
            row("Above head", "Your stat above your head, next to the logo if it shows there.") {
                val statAtHead = TeamTagPlace.HEAD in settings.placesOf(BadgePart.STAT)
                switch(checked = statAtHead, onChange = {
                    onChange(settings.withPlaces(BadgePart.STAT, if (it.checked) setOf(TeamTagPlace.HEAD) else emptySet()))
                })
            }
            row("Stat", "Read from your SkyBlock profile.") {
                select(
                    value = settings.stat,
                    onChange = { onChange(settings.copy(stat = it.value)) },
                    className = "cz-select",
                    searchable = true,
                    searchPlaceholder = "Search...",
                ) {
                    badge.stats.sortedWith(compareBy({ it.id != "none" }, { it.label.lowercase() })).forEach { option(it.id, it.label) }
                }
            }
        }

        // Where it shows; the stat has its rows above, the tag its own places
        if (part != BadgePart.STAT) {
            val places = if (part == BadgePart.TAG) settings.teamTagPlaces else settings.placesOf(part)
            row("Shows in", SHOWS_HINTS.getValue(part)) {
                chips(
                    values = PLACES.map { it.first }.filter { it in places },
                    onChange = { picked ->
                        val next = picked.toSet()
                        onChange(if (part == BadgePart.TAG) settings.withTeamTagPlaces(next) else settings.withPlaces(part, next))
                    },
                    className = "cz-chips",
                    key = "places-$part",
                ) { PLACES.forEach { (place, label) -> option(place, label) } }
            }
        }

        // Tag, level, rank and name can share one look
        val flows = flowsAsLine(settings)
        val target = if (flows && part in BadgePart.CHAINS) LINE else part
        if (part in BadgePart.CHAINS) {
            row("One line", "Tag, level, rank and name share colors, effect and style, and effects run over all of them.") {
                switch(checked = flows, onChange = {
                    if (it.checked) {
                        val source = settings.colorOf(part)
                        onChange(settings.edit(LINE) { c ->
                            c.copy(
                                color = source.color, to = source.to, effect = source.effect, motion = source.motion, chain = true,
                                bold = source.bold, italic = source.italic, underline = source.underline, strike = source.strike,
                            )
                        })
                    } else {
                        onChange(settings.edit(LINE) { c -> c.copy(chain = null) })
                    }
                })
            }
        }

        val color = settings.colorOf(part)
        val effect = color.effectOf(part)
        row("Color", if (target == LINE) "Tag, level, rank and name together." else COLOR_HINTS.getValue(part)) {
            div(className = "cz-inline") {
                colorInput(value = argb(color.color), onChange = { c -> onChange(settings.edit(target) { it.copy(color = hex(c)) }) }, key = "from-$target")
                color.to?.let { to ->
                    span(className = "cz-muted") { +"→" }
                    colorInput(value = argb(to), onChange = { c -> onChange(settings.edit(target) { it.copy(to = hex(c)) }) }, key = "to-$target")
                }
                if (badge.gradient) {
                    button(
                        className = classNames("cz-toggle", "cz-on" to (color.to != null)),
                        title = "A second color the first one fades into",
                        onClick = {
                            val on = color.to == null
                            // A wave needs both colors, without the second one it stands still
                            onChange(settings.edit(target) {
                                it.copy(
                                    to = if (on) DEFAULT_GRADIENT_END else null,
                                    effect = if (!on && effect == BadgeEffect.WAVE) BadgeEffect.STATIC else it.effect,
                                )
                            })
                        },
                    ) { +"Gradient" }
                }
            }
        }
        row("Effect", EFFECT_HINTS[effect].orEmpty()) {
            select(
                value = effect,
                onChange = { e ->
                    onChange(settings.edit(target) {
                        it.copy(effect = e.value, to = if (e.value == BadgeEffect.WAVE && badge.gradient) it.to ?: DEFAULT_GRADIENT_END else it.to)
                    })
                },
                className = "cz-select",
                key = "effect-$target",
            ) { BadgeEffect.ALL.forEach { option(it, EFFECT_LABELS.getValue(it)) } }
        }
        val motion = color.motion ?: BadgeMotion.NONE
        row("Motion", MOTION_HINTS[motion].orEmpty()) {
            select(
                value = motion,
                onChange = { e -> onChange(settings.edit(target) { it.copy(motion = e.value.takeUnless { m -> m == BadgeMotion.NONE }) }) },
                className = "cz-select",
                key = "motion-$target",
            ) { BadgeMotion.ALL.forEach { option(it, MOTION_LABELS.getValue(it)) } }
        }
        row("Style", "Bold draws wider; names in menus and the scoreboard stay thin so nothing gets cut off.") {
            chips(
                values = listOfNotNull(
                    BOLD.takeIf { color.bold == true },
                    ITALIC.takeIf { color.italic == true },
                    UNDERLINE.takeIf { color.underline == true },
                    STRIKE.takeIf { color.strike == true },
                ),
                onChange = { on ->
                    fun flag(style: String) = true.takeIf { style in on }
                    onChange(settings.edit(target) { it.copy(bold = flag(BOLD), italic = flag(ITALIC), underline = flag(UNDERLINE), strike = flag(STRIKE)) })
                },
                className = "cz-chips",
                key = "style-$target",
            ) {
                option(BOLD, "Bold")
                option(ITALIC, "Italic")
                option(UNDERLINE, "Underline")
                option(STRIKE, "Strike")
            }
        }

        // The [ ] around level, rank and tag, and the rank's plus
        if (part in BadgePart.BRACKETS) {
            row("Brackets", "The [ and ] around it.") {
                div(className = "cz-inline") {
                    span(className = "cz-muted") { +"[" }
                    colorInput(value = argb(bracketOf(part, color, close = false)), onChange = { c -> onChange(settings.withColor(part, color.copy(open = hex(c)))) }, key = "open-$part")
                    span(className = "cz-muted") { +"]" }
                    colorInput(value = argb(bracketOf(part, color, close = true)), onChange = { c -> onChange(settings.withColor(part, color.copy(close = hex(c)))) }, key = "close-$part")
                }
            }
        }
        if (part == BadgePart.RANK) {
            row("Plus", "The + of VIP+, MVP+ and MVP++.") {
                colorInput(value = argb(color.plus ?: color.to ?: color.color), onChange = { c -> onChange(settings.withColor(part, color.copy(plus = hex(c)))) }, key = "plus")
            }
        }
        div(className = "cz-footer") {
            button(title = "Every part gets these colors, effect and style; brackets and plus stay", onClick = {
                onChange(ALL_PARTS.fold(settings) { next, p ->
                    next.withColor(p, next.colorOf(p).copy(
                        color = color.color, to = color.to, effect = effect, motion = color.motion,
                        bold = color.bold, italic = color.italic, underline = color.underline, strike = color.strike,
                    ))
                })
            }) { +"Use for all parts" }
        }
    }

    private val ALL_PARTS = listOf(BadgePart.STAT, BadgePart.TAG, BadgePart.LEVEL, BadgePart.RANK, BadgePart.NAME)

    private val SHOWS_HINTS = mapOf(
        BadgePart.TAG to "Your team tag in front of your name.",
        BadgePart.LEVEL to "Your SkyBlock level in your colors.",
        BadgePart.RANK to "Your Hypixel rank, like [MVP+], in your colors where Hypixel shows it.",
        BadgePart.NAME to "Your name in your colors, with its symbols and style.",
    )

    private val COLOR_HINTS = mapOf(
        BadgePart.STAT to "The stat above your head.",
        BadgePart.NAME to "Your name where it is colored.",
        BadgePart.LEVEL to "Your SkyBlock level in front of your name.",
        BadgePart.TAG to "Your team tag; others see the rank colors while your badge is hidden.",
        BadgePart.RANK to "Your Hypixel rank, like [MVP+].",
    )

    private const val NO_SYMBOL = "none"

    /** A symbol in front of and behind the part picked in the middle; every part keeps its own. */
    private fun NodeBuilder.symbolsRow(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        picked: String,
        onPick: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        row("Symbols", "Next to the part wherever it is colored, in its colors and style. Every part has its own.") {
            if (badge.symbols.isEmpty()) {
                span(className = "cz-hint") { +"Not on the SBO server yet" }
                return@row
            }
            val part = picked.takeUnless { it == BadgePart.TAG && !hasTag(badge) } ?: BadgePart.NAME
            val color = settings.colorOf(part)
            div(className = "cz-inline") {
                symbolSelect(badge, color.before, "before-$part") { onChange(settings.withColor(part, color.copy(before = it))) }
                select(value = part, onChange = { onPick(it.value) }, className = "cz-select cz-symbol-part", key = "symbol-part") {
                    option(BadgePart.STAT, "Stat")
                    if (hasTag(badge)) option(BadgePart.TAG, "Tag")
                    option(BadgePart.LEVEL, "Level")
                    option(BadgePart.RANK, "Rank")
                    option(BadgePart.NAME, "Name")
                }
                symbolSelect(badge, color.after, "after-$part") { onChange(settings.withColor(part, color.copy(after = it))) }
            }
        }
    }

    private fun NodeBuilder.symbolSelect(badge: OwnBadgeResponse, value: String?, key: String, onChange: (String?) -> Unit) {
        select(
            value = value ?: NO_SYMBOL,
            onChange = { onChange(it.value.takeUnless { v -> v == NO_SYMBOL }) },
            className = "cz-select cz-symbol",
            key = "symbol-$key",
        ) {
            option(NO_SYMBOL, "None")
            badge.symbols.forEach { option(it, it) }
        }
    }

    /** Tag, level, rank and name share one look while all of them are chained. */
    private fun flowsAsLine(settings: BadgeSettings): Boolean = BadgePart.CHAINS.all { settings.colorOf(it).chain == true }

    private fun partsOf(target: String): List<String> = if (target == LINE) BadgePart.CHAINS.toList() else listOf(target)

    /** Applies [update] to every part of [target]; each part keeps what the update does not change. */
    private fun BadgeSettings.edit(target: String, update: (PartColor) -> PartColor): BadgeSettings =
        partsOf(target).fold(this) { next, part -> next.withColor(part, update(next.colorOf(part))) }

    // ---- Player: size and own view ------------------------------------------------------------------------------

    private fun NodeBuilder.playerPanel(
        badge: OwnBadgeResponse,
        settings: BadgeSettings,
        symbolPart: String,
        onSymbolPart: (String) -> Unit,
        onChange: (BadgeSettings) -> Unit,
    ) {
        val sizes = badge.sizes
        val look = settings.look ?: BadgeLook()
        symbolsRow(badge, settings, symbolPart, onSymbolPart, onChange)
        if (sizes != null) {
            row("Size", "How big others with SBO see you. Only your model changes, never your hitbox.") {
                div(className = "cz-inline") {
                    slider(
                        value = look.scale,
                        onChange = { v ->
                            // On the steps; 1 is the normal size and is not saved
                            val size = ((v / sizes.step).roundToInt() * sizes.step * 100f).roundToInt() / 100f
                            onChange(settings.copy(look = look.copy(size = size.coerceIn(sizes.min, sizes.max).takeUnless { it == 1f })))
                        },
                        min = sizes.min,
                        max = sizes.max,
                        step = sizes.step,
                        showValue = true,
                        format = { "%.2fx".format(Locale.ROOT, it) },
                        className = "cz-slider",
                    )
                    button(disabled = look.size == null, title = "Normal size", onClick = { onChange(settings.copy(look = look.copy(size = null))) }) { +"Reset" }
                }
            }
        } else {
            div(className = "cz-hint cz-note") { +"The SBO server does not support player sizes yet." }
        }
        OwnView()
    }

    // Only on this PC, so it changes right away without saving
    private val OwnView = component("CustomizerOwnView") {
        var inF5 by useState(DataManager.sboData.badgeOwnInF5)
        row("Show me in F5", "See your own name tag and badge in third person.") {
            switch(checked = inF5, onChange = {
                inF5 = it.checked
                DataManager.sboData.badgeOwnInF5 = it.checked
                DataManager.sboData.save()
            })
        }
    }

    // ---- Others: how other players' badges show for you, saved right away ---------------------------------------

    private data class TextSizes(val tag: Float, val name: Float)

    private data class OthersProps(val textSizes: TextSizes, val onTextSizes: (TextSizes) -> Unit)

    private val Others = component<OthersProps>("CustomizerOthers") { (textSizes, onTextSizes) ->
        val data = DataManager.sboData
        var inTab by useState(data.badgesInTab)
        var inChat by useState(data.badgesInChat)
        var namesInChat by useState(data.badgeNamesInChat)
        var namesTag by useState(data.badgeNamesTag)
        var everywhere by useState(data.badgeReplaceEverywhere)
        var colors by useState(data.badgeAnimationsOfOthers)
        var motion by useState(data.badgeMotionOfOthers)
        var tabAnimations by useState(data.badgeAnimationsInTab)
        var chatAnimations by useState(data.badgeAnimationsInChat)
        var looks by useState(data.badgeLooksOfOthers)
        var sizes by useState(data.badgeSizesOfOthers)

        fun saved(relook: Boolean = false) {
            data.save()
            if (relook) SboBadges.relook()
        }

        div(className = "cz-hint cz-note") { +"Only on your screen, changes right away. The SBO team and your own badge always show." }
        heading("Show")
        row("Tab list", "The SBO logo behind their name and their name colors.") {
            switch(checked = inTab, onChange = { inTab = it.checked; data.badgesInTab = it.checked; saved() })
        }
        row("Chat", "The SBO logo behind their name and their name colors, for new messages.") {
            switch(checked = inChat, onChange = { inChat = it.checked; data.badgesInChat = it.checked; saved() })
        }
        row("Names in messages", "Names written inside a message, not only the sender's, in their look. For new messages.") {
            select(value = namesInChat, onChange = { namesInChat = it.value; data.badgeNamesInChat = it.value; saved() }, className = "cz-select") {
                option(SboBadges.NAMES_OFF, "Off")
                option(SboBadges.NAMES_OWN, "Only mine")
                option(SboBadges.NAMES_ALL, "Everyone")
            }
        }
        if (namesInChat != SboBadges.NAMES_OFF) {
            row("Team tag in messages", "The team tag in front of names inside a message. Off only colors the name.") {
                switch(checked = namesTag, onChange = { namesTag = it.checked; data.badgeNamesTag = it.checked; saved() })
            }
            row("Names everywhere", "Those names in their colors in all text: menus, titles, scoreboard, signs.") {
                switch(checked = everywhere, onChange = { everywhere = it.checked; data.badgeReplaceEverywhere = it.checked; saved() })
            }
        }
        heading("Animations")
        row("Colors", "Moving colors (wave, pulse, chroma...) of other players. Off shows them standing still.") {
            switch(checked = colors, onChange = { colors = it.checked; data.badgeAnimationsOfOthers = it.checked; saved(relook = true) })
        }
        row("Motion", "Moving letters (wavy, bounce, shake, glitch) of other players.") {
            switch(checked = motion, onChange = { motion = it.checked; data.badgeMotionOfOthers = it.checked; saved(relook = true) })
        }
        row("In the tab list", "Moving colors of every badge in the tab list, your own too.") {
            switch(checked = tabAnimations, onChange = { tabAnimations = it.checked; data.badgeAnimationsInTab = it.checked; saved(relook = true) })
        }
        row("In chat", "Moving colors and letters in chat. \"Everyone\" follows Colors and Motion above. For new messages.") {
            select(value = chatAnimations, onChange = { chatAnimations = it.value; data.badgeAnimationsInChat = it.value; saved() }, className = "cz-select") {
                option(SboBadges.NAMES_OFF, "Off")
                option(SboBadges.NAMES_OWN, "Only mine")
                option(SboBadges.NAMES_ALL, "Everyone")
            }
        }
        heading("Looks")
        row("Symbols and styles", "Symbols next to other players' names and their bold, italic, underlined or struck text.") {
            switch(checked = looks, onChange = { looks = it.checked; data.badgeLooksOfOthers = it.checked; saved(relook = true) })
        }
        row("Player sizes", "Other players in the size they picked. Off shows everyone in their normal size.") {
            switch(checked = sizes, onChange = { sizes = it.checked; data.badgeSizesOfOthers = it.checked; saved() })
        }
        heading("Text above heads")
        row("Tag size", "The badge line above every player's head, yours too.") {
            textSizeSlider(textSizes.tag) { onTextSizes(textSizes.copy(tag = it)); data.badgeLineSize = it; saved() }
        }
        row("Name size", "The name line of players with an SBO badge or team tag, yours too. The tag stays on top of it.") {
            textSizeSlider(textSizes.name) { onTextSizes(textSizes.copy(name = it)); data.badgeNameSize = it; saved() }
        }
    }

    private fun NodeBuilder.textSizeSlider(value: Float, onChange: (Float) -> Unit) {
        div(className = "cz-inline") {
            slider(
                value = value,
                onChange = { v ->
                    // On the steps, so the saved value stays a clean number
                    val size = ((v / SboBadges.TEXT_SIZE_STEP).roundToInt() * SboBadges.TEXT_SIZE_STEP * 100f).roundToInt() / 100f
                    onChange(size.coerceIn(SboBadges.MIN_TEXT_SIZE, SboBadges.MAX_TEXT_SIZE))
                },
                min = SboBadges.MIN_TEXT_SIZE,
                max = SboBadges.MAX_TEXT_SIZE,
                step = SboBadges.TEXT_SIZE_STEP,
                showValue = true,
                format = { "%.2fx".format(Locale.ROOT, it) },
                className = "cz-slider",
            )
            button(disabled = value == 1f, title = "Normal size", onClick = { onChange(1f) }) { +"Reset" }
        }
    }

    private fun NodeBuilder.failed(state: Load.Failed) {
        if (!state.notEligible) {
            div(className = "cz-error") { +state.message }
            return
        }
        div(className = "cz-steps") {
            div(className = "cz-steps-title") { +"For supporters (Patreon or Ko-fi), contributors, maintainers and moderators." }
            div(className = "cz-step") { +"1. Link your Minecraft account: /link followed by your Minecraft name on the SBO Discord." }
            div(className = "cz-step") { +"2. Use the Discord account with the supporter or team role." }
            div(className = "cz-step") { +"3. Open this again with /${SboBadges.COMMAND}. New roles can take a few minutes." }
        }
    }

    // ---- Stage: the player as others see them --------------------------------------------------------------------

    private data class StageProps(val badge: OwnBadgeResponse?, val settings: BadgeSettings?, val textSizes: TextSizes)

    // Its own component, so moving colors only draw the stage again
    private val Stage = component<StageProps>("CustomizerStage") { (badge, settings, textSizes) ->
        val model = useMemo { FakePlayer.ofLocalPlayer() }
        // A new frame number draws the stage again; the colors read the time themselves
        val frame = useState(0L)
        val moving = settings != null && ALL_PARTS.any { SboBadges.moves(settings.colorOf(it), it) }
        useInterval(FRAME_MS) { if (moving) frame.value = System.currentTimeMillis() / FRAME_MS }

        val preview = if (badge != null && settings != null) previewOf(badge, settings) else null
        val name = Player.accountName()
        val size = settings?.look?.scale ?: 1f
        val prefix = (badge?.level?.let { "§8[§b$it§8] " } ?: "") + SAMPLE_RANK
        fun base(after: String) = Component.literal("$prefix§b$name$after")
        fun shown(place: String, after: String = ""): Component =
            preview?.let { SboBadges.previewLine(it.badge, base(after), place, it.tagAt(place), it.animates(place)) } ?: base(after)

        div(className = "cz-stage") {
            div(className = "cz-scene") {
                div(className = "cz-figure") {
                    // The text sizes from Others, the name size only for players with a badge as in game
                    if (preview != null && SboBadges.hasHeadLine(preview.badge)) {
                        div(className = "cz-nametag", style = textSize(textSizes.tag)) { game(SboBadges.line(preview.badge, animate = true)) }
                    }
                    div(className = "cz-nametag", style = textSize(if (preview != null) textSizes.name else 1f)) { game(shown(TeamTagPlace.HEAD)) }
                    if (model != null) {
                        entity(
                            model,
                            className = "cz-model",
                            style = "height: ${(MODEL_HEIGHT * size / MODEL_MAX_SIZE).roundToInt().coerceAtLeast(4)}px",
                            followMouse = true,
                        )
                    }
                }
                div(className = "cz-ground")
            }
            div(className = "cz-lines") {
                div(className = "cz-line cz-line-tab") { game(shown(TeamTagPlace.TAB)) }
                div(className = "cz-line cz-line-chat") { game(shown(TeamTagPlace.CHAT, "§f: Hello!")) }
            }
            div(className = "cz-hint cz-status") {
                +when {
                    badge == null -> ""
                    badge.hiddenByServer -> "Hidden by the SBO server, nobody sees it right now."
                    settings?.enabled == false -> "Nothing shows anywhere, nobody sees your badge."
                    size != 1f -> "Size %.2fx".format(Locale.ROOT, size)
                    else -> "This is how others with SBO see you."
                }
            }
        }
    }

    /** The draft as a badge from the list, so the stage is built like in game. */
    private class Preview(val badge: SboBadge, private val response: OwnBadgeResponse, private val settings: BadgeSettings) {
        fun tagAt(place: String): String? = response.rank?.takeIf { hasTag(response) && place in settings.teamTagPlaces }

        // Your own badge always moves above heads and in the tab list; in chat while chat animations are on
        fun animates(place: String): Boolean = place != TeamTagPlace.CHAT || DataManager.sboData.badgeAnimationsInChat != SboBadges.NAMES_OFF
    }

    private fun previewOf(response: OwnBadgeResponse, settings: BadgeSettings): Preview? {
        // Others only get the badge while it shows somewhere and the server does not hide it
        if (!settings.enabled || response.hiddenByServer) return null
        val stat = response.stats.firstOrNull { it.id == settings.stat }?.takeIf { it.id != "none" }
        val value = response.values[settings.stat] ?: response.preview?.value?.takeIf { settings.stat == response.settings?.stat }
        fun places(part: String) = TeamTagPlace.ALL.filter { it in settings.placesOf(part) }
        val badge = SboBadge(
            uuid = Player.accountUuid(),
            name = Player.accountName(),
            rank = response.rank ?: "supporter",
            color = settings.color,
            label = stat?.label,
            value = if (stat != null) value ?: "..." else null,
            places = BadgePlaces(
                places(BadgePart.LOGO), places(BadgePart.NAME), places(BadgePart.LEVEL), places(BadgePart.RANK), stat = places(BadgePart.STAT),
            ),
            colors = BadgeColors(
                settings.colorOf(BadgePart.STAT), settings.colorOf(BadgePart.NAME), settings.colorOf(BadgePart.LEVEL),
                settings.colorOf(BadgePart.TAG), settings.colorOf(BadgePart.RANK),
            ),
            look = settings.look,
        )
        return Preview(badge, response, settings)
    }

    // ---- Labels and small helpers ---------------------------------------------------------------------------------

    private val MOTION_LABELS = mapOf(
        BadgeMotion.NONE to "None",
        BadgeMotion.WAVY to "Wavy",
        BadgeMotion.BOUNCE to "Bounce",
        BadgeMotion.SHAKE to "Shake",
        BadgeMotion.GLITCH to "Glitch",
    )

    private val MOTION_HINTS = mapOf(
        BadgeMotion.NONE to "The letters stay in place.",
        BadgeMotion.WAVY to "The letters ride a wave. Above heads, and in chat for those who turned chat animations on.",
        BadgeMotion.BOUNCE to "One letter after the other hops.",
        BadgeMotion.SHAKE to "The letters jitter.",
        BadgeMotion.GLITCH to "Single letters flicker scrambled, like §k.",
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
        BadgeEffect.STATIC to "The colors stand still.",
        BadgeEffect.WAVE to "The gradient flows along the text.",
        BadgeEffect.PULSE to "Fades between both colors, or towards white with one color.",
        BadgeEffect.CHROMA to "A rainbow runs along the text, your colors are not used.",
        BadgeEffect.SHINE to "A white gleam sweeps over the text now and then.",
        BadgeEffect.FLASH to "Jumps between both colors, or to a light version of one color.",
        BadgeEffect.SPARKLE to "Single letters twinkle white.",
    )

    private const val TAG_BRACKETS = "#AAAAAA"
    private const val RAINBOW = "linear-gradient(90deg, #FF5555, #FFFF55, #55FF55, #55FFFF, #5555FF, #FF55FF)"

    /** A bracket as it looks in game: its own color, else gray for the team tag and the part's color otherwise. */
    private fun bracketOf(part: String, color: PartColor, close: Boolean): String = (if (close) color.close else color.open)
        ?: if (part == BadgePart.TAG && color.chain != true) TAG_BRACKETS else if (close) color.to ?: color.color else color.color

    private fun hex(color: Int): String = "#%06X".format(Locale.ROOT, color and 0xFFFFFF)

    // The picker works with ARGB, badge colors have no alpha
    private fun argb(hex: String): Int = SboBadges.parseHex(hex) or 0xFF000000.toInt()

    private fun background(color: PartColor, part: String): String = when {
        color.effectOf(part) == BadgeEffect.CHROMA -> RAINBOW
        color.to != null -> "linear-gradient(90deg, ${color.color}, ${color.to})"
        else -> color.color
    }

    /**
     * A line from the game; the SBO logo is a char of its own font, which the window's text can't draw, so it shows as the image.
     * Every char is a piece of its own with a fixed key: moving colors would cut the line into other pieces every frame,
     * and the window's font places pieces a little differently, so chars would twitch. The motion fonts that set chars
     * lower in game are left out by the window's text too, so those chars are drawn the same pixels lower here.
     */
    private fun textSize(size: Float): String? = if (size == 1f) null else "font-size: ${"%.2f".format(Locale.ROOT, size)}em"

    private fun NodeBuilder.game(line: Component) {
        div(className = "cz-game") {
            var index = 0
            line.visit(FormattedText.StyledContentConsumer<Unit> { style, s ->
                for (char in s) {
                    if (char.toString() == SboBadges.LOGO) img(LOGO, className = "cz-inline-logo", key = "c${index++}")
                    // Spaces never move, and an inline block holding only a space would collapse
                    else if (char == ' ') text(Component.literal(" ").withStyle(style), key = "c${index++}")
                    else {
                        val drop = SboBadges.dropOf(style)
                        // A transform only moves where it is drawn, the line and its box stay. It needs a box of its own,
                        // so every char is an inline block, also while it doesn't move, and the layout is the same in every frame
                        span(className = "cz-ch", style = if (drop > 0) "transform: translateY(${drop}px)" else null, key = "c${index++}") {
                            text(Component.literal(char.toString()).withStyle(style))
                        }
                    }
                }
                Optional.empty()
            }, Style.EMPTY)
        }
    }

    private fun NodeBuilder.heading(text: String) {
        div(className = "cz-heading") { +text }
    }

    /** One setting: its name left (the hint shows on hover), the control right. */
    private fun NodeBuilder.row(label: String, hint: String, control: NodeBuilder.() -> Unit) {
        div(className = "cz-row") {
            span(className = "cz-label", title = hint) { +label }
            div(className = "cz-control") { control() }
        }
    }
}
