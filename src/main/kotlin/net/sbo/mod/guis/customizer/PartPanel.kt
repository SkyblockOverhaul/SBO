package net.sbo.mod.guis.customizer

import net.minecraft.network.chat.Style
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.chips
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.colorInput
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.switch
import net.sbo.mod.utils.data.BadgeEffect
import net.sbo.mod.utils.data.BadgeMotion
import net.sbo.mod.utils.data.BadgePart
import net.sbo.mod.utils.data.BadgeSettings
import net.sbo.mod.utils.data.OwnBadgeResponse
import net.sbo.mod.utils.data.PartColor
import net.sbo.mod.utils.data.TeamTagPlace

private const val DEFAULT_GRADIENT_END = "#FFD966"

private val PLACES = listOf(TeamTagPlace.HEAD to "Head", TeamTagPlace.TAB to "Tab", TeamTagPlace.CHAT to "Chat")

private const val BOLD = "bold"
private const val ITALIC = "italic"
private const val UNDERLINE = "underline"
private const val STRIKE = "strike"

internal fun NodeBuilder.partPanel(badge: OwnBadgeResponse, settings: BadgeSettings, part: String, onChange: (BadgeSettings) -> Unit) {
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

private val SHOWS_HINTS = mapOf(
    BadgePart.TAG to "Your tag in front of your name.",
    BadgePart.LEVEL to "Your SkyBlock level in your colors.",
    BadgePart.RANK to "Your Hypixel rank, like [MVP+], in your colors where Hypixel shows it.",
    BadgePart.NAME to "Your name in your colors, with its symbols and style.",
)

private val COLOR_HINTS = mapOf(
    BadgePart.STAT to "The stat above your head.",
    BadgePart.NAME to "Your name where it is colored.",
    BadgePart.LEVEL to "Your SkyBlock level in front of your name.",
    BadgePart.TAG to "Your tag; others see the rank colors while your badge is hidden.",
    BadgePart.RANK to "Your Hypixel rank, like [MVP+].",
)

private const val NO_SYMBOL = "none"

/** A symbol in front of and behind the part picked in the middle; every part keeps its own. */
internal fun NodeBuilder.symbolsRow(
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
