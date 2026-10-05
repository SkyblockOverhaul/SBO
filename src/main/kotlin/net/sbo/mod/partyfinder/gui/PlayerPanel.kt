package net.sbo.mod.partyfinder.gui

import kotlinx.serialization.json.JsonNull
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.aside
import net.sbo.guilib.core.dsl.b
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.classNames
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.playerHead
import net.sbo.guilib.core.dsl.scroll
import net.sbo.guilib.core.dsl.span
import net.sbo.guilib.core.dsl.tooltip
import net.sbo.mod.partyfinder.PartyTarget
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyView

internal data class InspectedPlayer(val member: MemberView, val party: PartyView)

/** Panel right of the window: every stat of the party type for one player, not only what the party asks for. */
internal fun NodeBuilder.playerPanel(inspected: InspectedPlayer, target: PartyTarget, own: MemberView?, onClose: () -> Unit) {
    val member = inspected.member
    val party = inspected.party
    val name = member.name.ifBlank { "Unknown" }
    aside(className = "pf-panel") {
        div(className = "pf-panel-head") {
            playerHead(uuidOf(member.uuid), className = "pf-head")
            b(className = "pf-panel-name") { +name }
            button(className = "pf-close", title = "Close", onClick = { onClose() }) { +"✕" }
        }
        div(className = "pf-panel-sub") {
            +(if (member.uuid == party.id) "Leader, ${target.label}" else target.label)
        }
        scroll(className = "pf-panel-stats guilib-autohide") {
            (target.reqs.map { it.stat } + target.display).distinct().forEach { id ->
                val value = member.stats[id]
                val def = target.req(id)
                val need = party.reqs[id]?.takeIf { it !is JsonNull }
                div(className = "pf-panel-row", key = id) {
                    statLabel(id, own)
                    tooltip(content = { statInfo(id, own) }, className = "pf-tip") {
                        // Members already meet the party's requirements, so values keep the theme colors
                        span(className = classNames(
                            "pf-panel-value",
                            "estimated" to StatView.estimated(id),
                            "missing" to (value == null || value is JsonNull)
                        )) {
                            pieces(StatView.valuePieces(id, value))
                        }
                    }
                    if (def != null && need != null) div(className = "pf-panel-need") {
                        // Stays gray, Hypixel colors are only for the player's values
                        +"Party needs: ${StatView.need(def, need)}"
                    }
                }
            }
        }
    }
}
