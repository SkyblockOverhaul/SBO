package net.sbo.mod.config

import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.span

private val descriptionColors = Regex("§([0-9a-fr])", RegexOption.IGNORE_CASE)

/** Preserve source formatting, with readable equivalents of Minecraft colors on the Light theme. */
fun NodeBuilder.settingsDescription(text: String, className: String, light: Boolean) {
    div(className = className) {
        text.lines().forEach { line ->
            div(className = "cfg-line") {
                if (!light || !descriptionColors.containsMatchIn(line)) {
                    +line.ifEmpty { " " }
                } else {
                    var start = 0
                    var color: Char? = null
                    fun run(end: Int) {
                        if (end > start) span(className = color?.let { "cfg-format-$it" }) { +line.substring(start, end) }
                    }
                    descriptionColors.findAll(line).forEach { code ->
                        run(code.range.first)
                        color = code.groupValues[1].lowercase().first().takeUnless { it == 'r' }
                        start = code.range.last + 1
                    }
                    run(line.length)
                }
            }
        }
    }
}
