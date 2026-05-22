package net.sbo.mod.qol

import net.sbo.mod.settings.categories.QOL
import net.sbo.mod.utils.events.Register
import java.util.regex.Pattern

object MessageHider {
    private data class HideRule(val pattern: Pattern, val isEnabled: () -> Boolean)

    private val generalRules = listOf(
        HideRule(Pattern.compile("^§cAutopet §eequipped your (.*?) §a§lVIEW RULE$")) { QOL.hideAutoPetMSG },
        HideRule(Pattern.compile("^§7Your Implosion hit (.*?) §7damage\\.$")) { QOL.hideImplosionMSG },
    )

    private val dianaRules = listOf(
        HideRule(Pattern.compile("^§eFollow the arrows to find the §6treasure§e!$")) { QOL.dianaMessageHider },
        HideRule(Pattern.compile("^§cThis ability is on cooldown for (.*?)$")) { QOL.dianaMessageHider },
        HideRule(Pattern.compile("^§7Warping\\.\\.\\.$")) { QOL.dianaMessageHider },
        HideRule(Pattern.compile("^§cThere are blocks in the way!$")) { QOL.dianaMessageHider },
    )

    fun init() {
        (generalRules + dianaRules).forEach { rule ->
            Register.onChatMessageCancable(rule.pattern) { _, _ -> !rule.isEnabled() }
        }
    }
}