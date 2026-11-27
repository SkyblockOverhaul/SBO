package net.sbo.mod.customizer.gui

import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.colorInput
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.mod.customizer.CustomizerEffect
import net.sbo.mod.customizer.CustomizerEffects
import net.sbo.mod.customizer.CustomizerMotion
import net.sbo.mod.customizer.CustomizerPart
import net.sbo.mod.customizer.CustomizerSettings
import net.sbo.mod.customizer.OwnCustomizerResponse
import net.sbo.mod.customizer.PartColor
import net.sbo.mod.customizer.TeamTagPlace

private const val DEFAULT_GRADIENT_END = "#FFD966"

/** Gradients turned off this game session per part (or the whole line), so turning one on again brings it back. */
private val lastGradients = mutableMapOf<String, PartColor>()

/** The color halfway between two hex colors, where a new color of a gradient starts. */
private fun between(a: String, b: String): String {
    val x = CustomizerEffects.parseHex(a)
    val y = CustomizerEffects.parseHex(b)
    fun channel(shift: Int) = ((x shr shift and 0xFF) + (y shr shift and 0xFF)) / 2 shl shift
    return hex(channel(16) or channel(8) or channel(0))
}

/** Where the first and the last swatch of the gradient bar sit, in % of its width; the swatches between are spread evenly. */
private const val STRIP_EDGE = 8

/** The gradient bar's background: every color sits right under its swatch, so the swatches at the ends hide none of it. */
private fun stripGradient(stops: List<String>): String {
    val last = stops.size - 1
    return "linear-gradient(90deg, " + stops.mapIndexed { k, stop ->
        "$stop ${STRIP_EDGE + (100 - 2 * STRIP_EDGE) * k / last}%"
    }.joinToString() + ")"
}

/** The gradient with color [k] of its [PartColor.stops] set to [hex]. */
private fun PartColor.withStop(k: Int, hex: String): PartColor {
    val last = stops.size - 1
    return when (k) {
        0 -> copy(color = hex)
        last -> copy(to = hex)
        else -> copy(mid = mid?.mapIndexed { j, m -> if (j == k - 1) hex else m })
    }
}

/** The color with its gradient on: the one [target] had before it was turned off, else the default second color. */
private fun PartColor.withGradient(target: String): PartColor {
    if (to != null) return this
    val last = lastGradients[target] ?: return copy(to = DEFAULT_GRADIENT_END)
    return copy(to = last.to, mid = last.mid)
}

private val PLACES = listOf(TeamTagPlace.HEAD to "Head", TeamTagPlace.TAB to "Tab", TeamTagPlace.CHAT to "Chat")

private const val BOLD = "bold"
private const val ITALIC = "italic"
private const val UNDERLINE = "underline"
private const val STRIKE = "strike"

/**
 * The editor of one [part]. Where it shows: the stat has its rows above, the tag its own places. A wave needs both
 * colors, without the second one it stands still. A gradient shows as a bar of its colors with a swatch per color at its
 * place, the hex shows in the picker. Without their own colors the brackets flow with the part's gradient and effect again;
 * while the parts flow as one line they always do, so brackets and plus have no own colors there.
 */
internal fun NodeBuilder.partPanel(customizer: OwnCustomizerResponse, settings: CustomizerSettings, part: String, onChange: (CustomizerSettings) -> Unit) {
    if (part == CustomizerPart.STAT) {
        row("Logo", "The SBO logo: above your head, behind your name in the tab list and in chat.") {
            val logo = settings.placesOf(CustomizerPart.LOGO)
            chips(
                values = PLACES.map { it.first }.filter { it in logo },
                onChange = { picked -> onChange(settings.withPlaces(CustomizerPart.LOGO, picked.toSet())) },
                className = "cz-chips",
                key = "places-logo",
            ) { PLACES.forEach { (place, label) -> option(place, label) } }
        }
        row("Above head", "Your stat above your head, next to the logo if it shows there.") {
            val statAtHead = TeamTagPlace.HEAD in settings.placesOf(CustomizerPart.STAT)
            switch(checked = statAtHead, onChange = {
                onChange(settings.withPlaces(CustomizerPart.STAT, if (it.checked) setOf(TeamTagPlace.HEAD) else emptySet()))
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
                customizer.stats.sortedWith(compareBy({ it.id != "none" }, { it.label.lowercase() })).forEach { option(it.id, it.label) }
            }
        }
    }

    if (part != CustomizerPart.STAT) {
        val places = if (part == CustomizerPart.TAG) settings.teamTagPlaces else settings.placesOf(part)
        row("Shows in", SHOWS_HINTS.getValue(part)) {
            chips(
                values = PLACES.map { it.first }.filter { it in places },
                onChange = { picked ->
                    val next = picked.toSet()
                    onChange(if (part == CustomizerPart.TAG) settings.withTeamTagPlaces(next) else settings.withPlaces(part, next))
                },
                className = "cz-chips",
                key = "places-$part",
            ) { PLACES.forEach { (place, label) -> option(place, label) } }
        }
    }

    val flows = flowsAsLine(settings)
    val target = if (flows && part in CustomizerPart.CHAINS) LINE else part
    if (part in CustomizerPart.CHAINS) {
        row("One line", "Tag, level, rank and name share colors, effect and style, and effects run over all of them.") {
            switch(checked = flows, onChange = {
                if (it.checked) {
                    val source = settings.colorOf(part)
                    onChange(settings.edit(LINE) { c ->
                        c.copy(
                            color = source.color, to = source.to, mid = source.mid, effect = source.effect, motion = source.motion, chain = true,
                            open = null, close = null, plus = null,
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
            if (color.to == null) {
                colorInput(value = argb(color.color), onChange = { c -> onChange(settings.edit(target) { it.copy(color = hex(c)) }) }, key = "from-$target")
            } else {
                div(className = "cz-strip", style = "background: ${stripGradient(color.stops)}; background-clip: padding-box") {
                    color.stops.forEachIndexed { k, stop ->
                        colorInput(
                            value = argb(stop),
                            onChange = { c -> onChange(settings.edit(target) { it.withStop(k, hex(c)) }) },
                            className = "cz-stop",
                            key = "stop$k-$target",
                        )
                    }
                }
            }
            if (customizer.gradient && color.to != null) {
                if (color.stops.size < customizer.gradientColors) {
                    button(
                        className = "cz-toggle",
                        title = "Another color in the gradient, in front of the last one",
                        onClick = {
                            onChange(settings.edit(target) {
                                val stops = it.stops
                                it.copy(mid = it.mid.orEmpty() + between(stops[stops.size - 2], stops.last()))
                            })
                        },
                        key = "add-color-$target",
                    ) { +"+" }
                }
                if (!color.mid.isNullOrEmpty()) {
                    button(
                        className = "cz-toggle",
                        title = "Removes the color in front of the last one",
                        onClick = { onChange(settings.edit(target) { it.copy(mid = it.mid?.dropLast(1)?.takeIf { m -> m.isNotEmpty() }) }) },
                        key = "remove-color-$target",
                    ) { +"-" }
                }
            }
            if (customizer.gradient) {
                button(
                    className = classNames("cz-toggle", "cz-on" to (color.to != null)),
                    title = "Colors the first one fades into; turned on again it comes back as it was",
                    onClick = {
                        val on = color.to == null
                        if (!on) lastGradients[target] = color
                        onChange(settings.edit(target) {
                            if (on) it.withGradient(target)
                            else it.copy(
                                to = null,
                                mid = null,
                                effect = if (effect == CustomizerEffect.WAVE) CustomizerEffect.STATIC else it.effect,
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
                    val wave = e.value == CustomizerEffect.WAVE && customizer.gradient
                    (if (wave) it.withGradient(target) else it).copy(effect = e.value)
                })
            },
            className = "cz-select",
            key = "effect-$target",
        ) { CustomizerEffect.ALL.forEach { option(it, EFFECT_LABELS.getValue(it)) } }
    }
    val motion = color.motion ?: CustomizerMotion.NONE
    row("Motion", MOTION_HINTS[motion].orEmpty()) {
        select(
            value = motion,
            onChange = { e -> onChange(settings.edit(target) { it.copy(motion = e.value.takeUnless { m -> m == CustomizerMotion.NONE }) }) },
            className = "cz-select",
            key = "motion-$target",
        ) { CustomizerMotion.ALL.forEach { option(it, MOTION_LABELS.getValue(it)) } }
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

    if (part in CustomizerPart.BRACKETS && target != LINE) {
        row("Brackets", "The [ and ] around it.") {
            div(className = "cz-inline") {
                if (color.open != null || color.close != null) {
                    button(
                        title = if (part == CustomizerPart.TAG) "Gray brackets again, or the tag's colors while it flows with the parts next to it"
                        else "The brackets take the part's colors and effect again",
                        onClick = { onChange(settings.withColor(part, color.copy(open = null, close = null))) },
                        key = "reset-brackets-$part",
                    ) { +"Reset" }
                }
                span(className = "cz-muted") { +"[" }
                colorInput(value = argb(bracketOf(part, color, close = false)), onChange = { c -> onChange(settings.withColor(part, color.copy(open = hex(c)))) }, key = "open-$part")
                span(className = "cz-muted") { +"]" }
                colorInput(value = argb(bracketOf(part, color, close = true)), onChange = { c -> onChange(settings.withColor(part, color.copy(close = hex(c)))) }, key = "close-$part")
            }
        }
    }
    if (part == CustomizerPart.RANK && target != LINE) {
        row("Plus", "The + of VIP+, MVP+ and MVP++.") {
            div(className = "cz-inline") {
                if (color.plus != null) {
                    button(
                        title = "The plus takes the rank's colors and effect again",
                        onClick = { onChange(settings.withColor(part, color.copy(plus = null))) },
                        key = "reset-plus",
                    ) { +"Reset" }
                }
                colorInput(value = argb(color.plus ?: color.to ?: color.color), onChange = { c -> onChange(settings.withColor(part, color.copy(plus = hex(c)))) }, key = "plus")
            }
        }
    }
    div(className = "cz-footer") {
        button(title = "Every part gets these colors, effect and style; brackets and plus stay", onClick = {
            onChange(ALL_PARTS.fold(settings) { next, p ->
                next.withColor(p, next.colorOf(p).copy(
                    color = color.color, to = color.to, mid = color.mid, effect = effect, motion = color.motion,
                    bold = color.bold, italic = color.italic, underline = color.underline, strike = color.strike,
                ))
            })
        }) { +"Use for all parts" }
    }
}

private val SHOWS_HINTS = mapOf(
    CustomizerPart.TAG to "Your tag in front of your name.",
    CustomizerPart.LEVEL to "Your SkyBlock level in your colors.",
    CustomizerPart.RANK to "Your Hypixel rank, like [MVP+], in your colors where Hypixel shows it.",
    CustomizerPart.NAME to "Your name in your colors, with its symbols and style.",
)

private val COLOR_HINTS = mapOf(
    CustomizerPart.STAT to "The stat above your head.",
    CustomizerPart.NAME to "Your name where it is colored.",
    CustomizerPart.LEVEL to "Your SkyBlock level in front of your name.",
    CustomizerPart.TAG to "Your tag; others see the rank colors while your customizer is hidden.",
    CustomizerPart.RANK to "Your Hypixel rank, like [MVP+].",
)

private const val NO_SYMBOL = "none"

/** A symbol in front of and behind the part picked in the middle; every part keeps its own. */
internal fun NodeBuilder.symbolsRow(
    customizer: OwnCustomizerResponse,
    settings: CustomizerSettings,
    picked: String,
    onPick: (String) -> Unit,
    onChange: (CustomizerSettings) -> Unit,
) {
    row("Symbols", "Next to the part wherever it is colored, in its colors and style. Every part has its own.") {
        if (customizer.symbols.isEmpty()) {
            span(className = "cz-hint") { +"Not on the SBO server yet" }
            return@row
        }
        val part = picked.takeUnless { it == CustomizerPart.TAG && !hasTag(customizer) } ?: CustomizerPart.NAME
        val color = settings.colorOf(part)
        div(className = "cz-inline") {
            symbolSelect(customizer, color.before, "before-$part") { onChange(settings.withColor(part, color.copy(before = it))) }
            select(value = part, onChange = { onPick(it.value) }, className = "cz-select cz-symbol-part", key = "symbol-part") {
                option(CustomizerPart.STAT, "Stat")
                if (hasTag(customizer)) option(CustomizerPart.TAG, "Tag")
                option(CustomizerPart.LEVEL, "Level")
                option(CustomizerPart.RANK, "Rank")
                option(CustomizerPart.NAME, "Name")
            }
            symbolSelect(customizer, color.after, "after-$part") { onChange(settings.withColor(part, color.copy(after = it))) }
        }
    }
}

private fun NodeBuilder.symbolSelect(customizer: OwnCustomizerResponse, value: String?, key: String, onChange: (String?) -> Unit) {
    select(
        value = value ?: NO_SYMBOL,
        onChange = { onChange(it.value.takeUnless { v -> v == NO_SYMBOL }) },
        className = "cz-select cz-symbol",
        key = "symbol-$key",
    ) {
        option(NO_SYMBOL, "None")
        customizer.symbols.forEach { option(it, it) }
    }
}

/** Tag, level, rank and name share one look while all of them are chained. */
private fun flowsAsLine(settings: CustomizerSettings): Boolean = CustomizerPart.CHAINS.all { settings.colorOf(it).chain == true }

private fun partsOf(target: String): List<String> = if (target == LINE) CustomizerPart.CHAINS.toList() else listOf(target)

/** Applies [update] to every part of [target]; each part keeps what the update does not change. */
private fun CustomizerSettings.edit(target: String, update: (PartColor) -> PartColor): CustomizerSettings =
    partsOf(target).fold(this) { next, part -> next.withColor(part, update(next.colorOf(part))) }
