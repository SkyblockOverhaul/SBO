package net.sbo.mod.test

import com.google.gson.JsonParser
import java.nio.file.Files
import kotlin.math.roundToInt

/** Source-only inventory. Never initializes category objects, theme folders or action callbacks. */
internal object SettingsSweepData {
    data class Category(val id: String, val name: String, val description: String,
        val entries: List<DianaReviewData.Entry>, val groups: List<DianaReviewData.Group>)
    private val quoted = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
    private fun decode(value: String) = JsonParser.parseString(value).asString
    private fun source(name: String) = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/settings/categories/$name.kt")).replace("\r", "")
    private fun balanced(text: String, start: Int, open: Char = '(', close: Char = ')'): String {
        var depth = 0; var string = false; var escape = false
        for (i in start until text.length) {
            val c = text[i]
            if (string) { if (escape) escape = false else if (c == '\\') escape = true else if (c == '"') string = false }
            else when (c) { '"' -> string = true; open -> depth++; close -> if (--depth == 0) return text.substring(start + 1, i) }
        }
        error("Unbalanced source declaration")
    }
    private fun literal(body: String, field: String, prefix: String = "this\\."): String {
        val start = Regex(prefix + field + "\\s*=\\s*(Literal\\()?\\s*").find(body) ?: return ""
        val tail = if (start.groupValues[1].isNotEmpty()) balanced(body, body.indexOf('(', start.range.first)) else body.substring(start.range.last + 1)
        return if (start.groupValues[1].isNotEmpty()) quoted.findAll(tail).joinToString("") { decode(it.value) }
            else quoted.find(tail)?.value?.let(::decode) ?: ""
    }
    private val themeSource = Files.readString(ReviewResources.project.resolve("src/main/kotlin/net/sbo/mod/guis/look/SboThemes.kt"))
    private val themeOptions = Regex("Theme\\(\\s*(DEFAULT|\"[^\"]+\")\\s*,\\s*(\"[^\"]+\")\\s*,\\s*(\"(?:\\\\.|[^\"\\\\])*\")")
        .findAll(themeSource.substringAfter("val BUILT_IN").substringBefore("val BASES")).map {
            Triple(if (it.groupValues[1] == "DEFAULT") "sbo-dark" else decode(it.groupValues[1]), decode(it.groupValues[2]), decode(it.groupValues[3]))
        }.toList()
    private val scaleOptions = listOf("auto" to "Auto", "1" to "1", "1.5" to "1.5", "2" to "2", "2.5" to "2.5", "3" to "3", "4" to "4")

    fun read(id: String): Category {
        val text = source(id)
        val name = Regex("Category\\(\"([^\"]+)\"\\)").find(text)!!.groupValues[1]
        val groupMatches = Regex("sub \\{").findAll(text).map { match ->
            val body = balanced(text, match.range.last, '{', '}')
            match.range.first to DianaReviewData.Group(literal(body, "title"), literal(body, "description"))
        }.toList()
        fun section(at: Int) = groupMatches.lastOrNull { it.first < at }?.second?.name ?: ""
        val entries = buildList {
            Regex("(?m)^    var (\\w+) by ").findAll(text).forEach { match ->
                val end = Regex("(?m)^    (?:var |init \\{|}$)").find(text, match.range.last + 1)?.range?.first ?: text.length
                val body = text.substring(match.range.last + 1, end)
                val declaration = Regex("(boolean|int|float|double|color|choice)\\(").find(body) ?: error(match.value)
                val kind = declaration.groupValues[1]
                val args = balanced(body, declaration.range.last)
                val range = Regex("this.range = ([0-9.]+)f?\\.\\.([0-9.]+)f?").find(body)
                val requirements = Regex("depends(Off|Any)?\\(").findAll(body).map { dependency ->
                    val content = balanced(body, dependency.range.last)
                    DianaReviewData.Requirement(dependency.groupValues[1] == "Any", Regex("(?:(Diana)::|::)(\\w+)").findAll(content).map {
                        DianaReviewData.Condition(it.groupValues[2], if (dependency.groupValues[1] == "Off") "OFF" else "ON")
                    }.toList())
                }.toList()
                val entryId = match.groupValues[1]
                val choices = if (kind == "choice") if (entryId == "theme") themeOptions.map { it.first } else scaleOptions.map { it.first } else emptyList()
                val labels = if (kind == "choice") if (entryId == "theme") themeOptions.associate { it.first to it.second } else scaleOptions.toMap() else emptyMap()
                val descriptions = if (entryId == "theme") themeOptions.associate { it.first to it.third } else emptyMap()
                val initial = when (kind) {
                    "color" -> {
                        val components = Regex("Color\\((.*?)\\)").find(args)!!.groupValues[1].split(',').map { it.trim().removeSuffix("f").toFloat() }
                        val channels = components.map { (it * 255f).roundToInt() }
                        ((0xff shl 24) or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]).toString()
                    }
                    "choice" -> if (entryId == "theme") "sbo-dark" else "auto"
                    else -> args.trim().removeSuffix("f")
                }
                val widget = when {
                    kind == "color" -> if ("this.allowAlpha = true" in body) "rgba" else "rgb"
                    kind == "float" && "this.slider = true" in body -> "float-slider"
                    kind == "int" && "this.slider = true" in body -> "slider"
                    kind == "float" -> "double"
                    else -> kind
                }
                add(match.range.first to DianaReviewData.Entry(entryId, literal(body, "name"), literal(body, "description"), widget,
                    section(match.range.first), initial, choices, range?.groupValues?.get(1)?.toDouble(), range?.groupValues?.get(2)?.toDouble(), requirements,
                    optionLabels = labels, optionDescriptions = descriptions))
            }
            Regex("button \\{").findAll(text).forEachIndexed { i, match ->
                val body = balanced(text, match.range.last, '{', '}')
                add(match.range.first to DianaReviewData.Entry("action-$i", literal(body, "title", ""), literal(body, "description", ""), "button",
                    section(match.range.first), buttonText = literal(body, "text", "")))
            }
        }.sortedBy { it.first }.map { it.second }
        val description = Regex("override val description = (\"[^\"]+\")").find(text)?.groupValues?.get(1)?.let(::decode) ?: ""
        return Category(id, name, description, entries, groupMatches.map { it.second })
    }
    fun fixture(category: Category, width: Float = 640f, height: Float = 480f, theme: String = "sbo-dark",
        values: Map<String, String> = emptyMap(), collapsed: Set<String> = emptySet(), off: Set<String> = emptySet()) = DianaReviewFixture(
        DianaReviewFixture.currentCss, width, height, theme, categoryName = category.name, categoryEntries = category.entries,
        categoryGroups = category.groups, categoryDescription = category.description, valueOverrides = values,
        externalEntries = if (category.id == "Customization") DianaReviewData.entries else emptyList(), collapsedGroups = collapsed, offGroups = off)
}
