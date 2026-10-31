package net.sbo.mod.qol

import net.sbo.mod.settings.categories.General
import net.sbo.mod.utils.events.Register
import java.util.regex.Pattern

object MessageHider {
    private data class HideRule(val pattern: Pattern, val isEnabled: () -> Boolean)

    private val generalRules = listOf(
        HideRule(Pattern.compile("^§cAutopet §eequipped your (.*?) §a§lVIEW RULE$")) { General.HiddenMessage.AUTOPET in General.hideMessages },
        HideRule(Pattern.compile("^§7Your Implosion hit (.*?) §7damage\\.$")) { General.HiddenMessage.IMPLOSION in General.hideMessages },
    )

    private val dianaRules = listOf(
        HideRule(Pattern.compile("^§eFollow the arrows to find the §6treasure§e!$")) { General.HiddenMessage.DIANA in General.hideMessages },
        HideRule(Pattern.compile("^§cThis ability is on cooldown for (.*?)$")) { General.HiddenMessage.DIANA in General.hideMessages },
        HideRule(Pattern.compile("^§7Warping\\.\\.\\.$")) { General.HiddenMessage.DIANA in General.hideMessages },
        HideRule(Pattern.compile("^§cThere are blocks in the way!$")) { General.HiddenMessage.DIANA in General.hideMessages },
    )

    fun init() {
        (generalRules + dianaRules).forEach { rule ->
            Register.onChatMessageCancelable(rule.pattern) { _, _ -> !rule.isEnabled() }
        }
    }
}