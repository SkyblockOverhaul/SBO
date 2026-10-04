package net.sbo.mod.partyfinder.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.h3
import net.sbo.guilib.core.dsl.p
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.span
import net.sbo.mod.partyfinder.ProblemText
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.RulesData
import net.sbo.mod.partyfinder.gui.PartyFinderGui.message

/** The party finder rules; the text comes from the backend, so it can change without a mod update. */
internal val RulesPage = component<Unit>("RulesPage") {
    var rules by useState<RulesData?>(null)
    var error by useState<String?>(null)
    var attempt by useState(0)

    useEffect(attempt) {
        error = null
        PartyFinderApi.rules(onError = { e -> error = ProblemText.error(e) }) { rules = it }
    }

    val loaded = rules
    when {
        loaded == null && error != null -> message("Could not load the rules. $error") {
            button(onClick = { attempt++ }) { +"Try again" }
        }
        loaded == null -> message("Loading the rules...")
        else -> scroll(className = "pf-form pf-rules guilib-autohide") {
            h3(className = "pf-section") { +"Party Finder Rules" }
            p(className = "pf-hint") { +loaded.intro }
            loaded.rules.forEachIndexed { i, rule ->
                div(className = "pf-rule", key = i) {
                    b(className = "pf-rule-title") { +"${i + 1}. ${rule.title}." }
                    span { +" ${rule.text}" }
                }
            }
            p(className = "pf-hint pf-rules-outro") { +loaded.outro }
        }
    }
}
