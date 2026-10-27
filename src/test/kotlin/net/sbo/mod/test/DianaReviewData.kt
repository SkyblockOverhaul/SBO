package net.sbo.mod.test

import com.google.gson.JsonParser
import java.nio.file.Files

/** Reads declarations, never initializes Diana, settings persistence, Minecraft or action callbacks. */
internal object DianaReviewData {
    data class Condition(val id: String, val state: String = "ON")
    data class Requirement(val any: Boolean, val conditions: List<Condition>)
    data class Entry(val id: String, val name: String, val description: String, val kind: String,
        val section: String, val initial: String = "", val options: List<String> = emptyList(),
        val minimum: Double? = null, val maximum: Double? = null,
        val requirements: List<Requirement> = emptyList(), val buttonText: String = "",
        val optionLabels: Map<String, String> = emptyMap(), val optionDescriptions: Map<String, String> = emptyMap())
    data class Group(val name: String, val description: String)

    private val source = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/settings/categories/Diana.kt"))
    private val quoted = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
    private fun decode(value: String) = JsonParser.parseString(value).asString
    private fun literal(body: String, field: String): String {
        val start = Regex("this\\." + field + "\\s*=\\s*Literal\\(").find(body)?.range?.last?.plus(1) ?: return ""
        val content = balanced(body, start - 1, '(', ')')
        return quoted.findAll(content).joinToString("") { decode(it.value) }
    }
    private fun balanced(text: String, start: Int, open: Char, close: Char): String {
        var level = 0
        var string = false
        var escape = false
        for (i in start until text.length) {
            val ch = text[i]
            if (string) {
                if (escape) escape = false else if (ch == '\\') escape = true else if (ch == '"') string = false
            } else when (ch) {
                '"' -> string = true
                open -> level++
                close -> if (--level == 0) return text.substring(start + 1, i)
            }
        }
        error("Unbalanced source fixture declaration at $start")
    }
    private val groupMatches = Regex("sub \\{").findAll(source).map { match ->
        val body = balanced(source, match.range.last, '{', '}')
        fun text(field: String) = Regex("this\\." + field + "\\s*=\\s*(\"(?:\\\\.|[^\"\\\\])*\")")
            .find(body)?.groupValues?.get(1)?.let(::decode) ?: ""
        match.range.first to Group(text("title"), text("description"))
    }.toList()
    val groups = groupMatches.map { it.second }
    private fun section(at: Int) = groupMatches.last { it.first < at }.second.name
    private val enums: Map<String, List<String>> = buildMap {
        for (content in listOf(source, Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/utils/waypoint/WarpPoint.kt")))) {
            Regex("enum class (\\w+)(?:\\([^\\n]*\\))? \\{").findAll(content).forEach { match ->
                val body = balanced(content, match.range.last, '{', '}').substringBefore(';')
                put(match.groupValues[1], Regex("[A-Z][A-Z_]+").findAll(body).map { it.value }.toList())
            }
        }
    }
    val entries: List<Entry> = buildList {
        val vars = Regex("(?m)^    var (\\w+) by ").findAll(source).toList()
        vars.forEach { match ->
            val end = Regex("(?m)^    (?:var |init \\{|}$)").find(source, match.range.last + 1)?.range?.first ?: source.length
            val body = source.substring(match.range.last + 1, end)
            val declaration = Regex("(boolean|int|double|enum|select|strings)(?:<([^>]+)>)?\\s*([({])").find(body) ?: error(match.value)
            val kind = declaration.groupValues[1]
            val args = if (declaration.groupValues[3] == "(") balanced(body, declaration.range.last, '(', ')') else ""
            val enumName = declaration.groupValues[2].ifBlank { args.trim().removePrefix("*").substringBefore('.') }
            val range = Regex("this.range = (-?[0-9]+)\\.\\.(-?[0-9]+)").find(body)
            val requirements = Regex("depends(Any)?\\(").findAll(body).map { dependency ->
                val content = balanced(body, dependency.range.last, '(', ')')
                val states = Regex("state\\(::(\\w+),\\s*\"([^\"]+)\"").findAll(content)
                    .associate { it.groupValues[1] to it.groupValues[2].uppercase() }
                Requirement(dependency.groupValues[1] == "Any", Regex("::(\\w+)").findAll(content)
                    .map { Condition(it.groupValues[1], states[it.groupValues[1]] ?: "ON") }.toList())
            }.toList()
            val options = if (kind == "enum" || kind == "select") enums.getValue(enumName) else emptyList()
            val initial = when (kind) {
                "enum" -> args.substringAfter('.').trim()
                "select" -> when { args.isBlank() -> ""; args.startsWith("*") -> options.joinToString(","); else -> args.split(',').joinToString(",") { it.substringAfter('.').trim() } }
                "strings" -> if (args.trim().startsWith('"')) decode(args.trim()) else {
                    val constantSource = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/diana/LootshareDisplay.kt"))
                    decode(quoted.find(constantSource.substringAfter("const val DEFAULT_TEXT = "))!!.value)
                }
                else -> args.trim()
            }
            add(match.range.first to Entry(match.groupValues[1], literal(body, "name"), literal(body, "description"),
                if (kind == "int" && "this.slider = true" in body) "slider" else kind, section(match.range.first), initial, options,
                range?.groupValues?.get(1)?.toDouble(), range?.groupValues?.get(2)?.toDouble(), requirements))
        }
        Regex("button \\{").findAll(source).forEachIndexed { i, match ->
            val body = balanced(source, match.range.last, '{', '}')
            fun text(field: String) = Regex("(?m)^\\s*" + field + " = (\"(?:\\\\.|[^\"\\\\])*\")")
                .find(body)?.groupValues?.get(1)?.let(::decode) ?: ""
            add(match.range.first to Entry("action-$i", text("title"), text("description"), "button", section(match.range.first), buttonText = text("text")))
        }
    }.sortedBy { it.first }.map { it.second }

    val pickerEntries = listOf(
        Entry("rgb", "RGB picker fixture", "Shared color control, without alpha. Diana has no color entries; its colors live in Customization.", "rgb", "Shared picker fixtures", "-6736897"),
        Entry("rgba", "RGBA picker fixture", "Shared color control with alpha enabled, using synthetic values. No setting is saved.", "rgba", "Shared picker fixtures", "-2140793345"),
        Entry("choice", "Choice picker fixture", "Shared dynamic-choice control. Diana itself uses enum choices and multiselects.", "enum", "Shared picker fixtures", "Default", listOf("Default", "Long descriptive choice label", "Compact")),
    )
}
