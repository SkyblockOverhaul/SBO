package net.sbo.mod.diana.guide

import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.details
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.item
import net.sbo.guilib.core.dsl.li
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.ul
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.utils.chat.Chat

/**
 * The Pet Rules tab. Each rule shows the trigger and then the pet with its held item like the Hypixel Autopet menu.
 * After the Autopet menu was opened once each rule also shows whether the player has it set up.
 */
internal fun NodeBuilder.petRulesTab(guide: DianaGuide, now: Long) {
    val readAt = PetRulesReader.readAt
    div(className = "dg-toolbar") {
        span(className = "dg-hint dg-grow") {
            +if (readAt == null) "Open your Autopet rules once (/pets and then Autopet) to check which ones you have."
            else "Checked against your Autopet rules ${ago(now - readAt)}. Open them again after a change."
        }
        button(
            disabled = !GearScan.onSkyblock(),
            title = if (GearScan.onSkyblock()) "Opens /pets. Click Autopet there." else "Join SkyBlock to open your pets",
            onClick = {
                GuiLib.close()
                Chat.command("pets")
            }
        ) { +"Open /pets" }
    }
    guide.petRules.forEach { group ->
        div(className = "dg-section", key = "title:${group.title}") {
            +group.title
            group.hint?.let { span(className = "dg-hint") { +it } }
        }
        group.rules.forEachIndexed { i, rule ->
            val state = rule.match?.let { PetRulesReader.state(it) } ?: RuleState.UNKNOWN
            div(className = "dg-rule ${state.name.lowercase()}", key = "${group.title}:$i") {
                div(className = "dg-rule-when") {
                    div { +rule.`when` }
                    rule.unless?.let { div(className = "dg-hint") { +it } }
                    rule.why?.let { div(className = "dg-rule-why") { +it } }
                }
                span(className = "dg-rule-arrow") { +"→" }
                div(className = "dg-rule-pet") {
                    div(className = "dg-rule-icon") {
                        rule.icon?.let { key -> GuideData.icon(key)?.let { item(it, className = "dg-rule-item", decorations = false) } }
                        rule.badge?.let { key -> GuideData.icon(key)?.let { item(it, className = "dg-slot-badge", decorations = false) } }
                    }
                    span(className = "dg-r-${rule.rarity}") { +rule.pet }
                }
                ruleChip(state)
            }
        }
    }
}

private fun NodeBuilder.ruleChip(state: RuleState) {
    when (state) {
        RuleState.SET_UP -> span(className = "dg-chip have") { +"✓ Set up" }
        RuleState.DISABLED -> span(className = "dg-chip disabled") { +"Disabled" }
        RuleState.MISSING -> span(className = "dg-chip missing") { +"Missing" }
        RuleState.UNKNOWN -> span(className = "dg-chip unknown") { +"Not checked" }
    }
}

/** The Tracking tab. The guide totals per scenario and then every source. */
internal fun NodeBuilder.trackingTab(guide: DianaGuide) {
    div(className = "dg-toolbar") {
        guide.tracking.scenarios.forEach { scenario ->
            span(className = "dg-stat", key = scenario.name) {
                span(className = "dg-stat-name") { +"${scenario.name} " }
                +scenario.value
            }
        }
    }
    div(className = "dg-section") { +"Sources" }
    guide.tracking.sources.forEach { source ->
        div(className = "dg-track-row", key = source.name) {
            span(className = "dg-name") { +source.name }
            span(className = "dg-value") { +"+${source.value}" }
            span(className = "dg-hint") { +if (source.sometimes) "Only sometimes" else "" }
        }
    }
}

/** The Technique tab. The written guide chapter with one collapsible section per topic and the credit at both ends. */
internal fun NodeBuilder.techniqueTab(guide: DianaGuide) {
    val credit = guide.credit
    div(className = "dg-credit") {
        +"From the "
        b { +credit.guide }
        +" by "
        b { +credit.author }
        +", with support from "
        b { +credit.support }
        +". "
        link("Full guide", credit.url)
    }
    guide.technique.forEachIndexed { i, section ->
        details(section.title, defaultOpen = i == 0, className = "dg-article", key = section.title) {
            section.parts.forEachIndexed { j, part ->
                div(className = "dg-part", key = "$i:$j") {
                    part.heading?.let { div(className = "dg-part-heading") { +it } }
                    part.text?.let { p(className = "dg-part-text") { +it } }
                    if (part.bullets.isNotEmpty()) ul(className = "dg-part-list") {
                        part.bullets.forEach { li(key = it) { +it } }
                    }
                    part.link?.let { link(it.label, it.url) }
                }
            }
        }
    }
    div(className = "dg-credit dg-credit-end") {
        +"${credit.questions} "
        link("SBO Discord", credit.discord)
    }
}
