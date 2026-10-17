package net.sbo.mod.utils.data.cloud

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

enum class CloudSide { PC, CLOUD }

// pc/cloud are display texts, null = not there
data class CloudField(val id: String, val label: String, val pc: String?, val cloud: String?)

// perField: each field can be picked, otherwise only the whole area
data class CloudArea(val file: String, val label: String, val perField: Boolean, val fields: List<CloudField>, val hidden: Int)

// Compares and merges the files of a cloud save, no game code so it can be tested
object CloudDiff {
    const val CONFIG = "config"
    private val PER_FIELD = setOf(CONFIG, "SboData.json", "soundSettingsData.json")
    private val IGNORED = setOf("rconfig:version")
    private const val MAX_SHOWN = 40
    private const val MAX_TEXT = 40

    private val LABELS = linkedMapOf(
        CONFIG to "Settings",
        "SboData.json" to "SBO Data",
        "soundSettingsData.json" to "Sounds",
        "dianaTrackerTotal.json" to "Diana Tracker (Total)",
        "dianaTrackerMayor.json" to "Diana Tracker (Mayor)",
        "dianaTrackerSession.json" to "Diana Tracker (Session)",
        "sbo_achievements.json" to "Achievements",
        "pastDianaEvents.json" to "Past Diana Events",
        "partyFinderConfigState.json" to "Party Finder",
        "overlayData.json" to "Overlay Positions",
    )

    fun label(file: String): String = LABELS[file] ?: file.removeSuffix(".json")

    fun compare(pc: Map<String, String>, cloud: Map<String, String>): List<CloudArea> {
        val files = (LABELS.keys + pc.keys + cloud.keys).distinct().filter { it in pc || it in cloud }
        return files.mapNotNull { file ->
            val pcJson = pc[file]?.let(::parse)
            val cloudJson = cloud[file]?.let(::parse)
            if (pcJson == cloudJson && pcJson != null) return@mapNotNull null
            if (pcJson == null || cloudJson == null) {
                val field = CloudField(file, label(file), pcJson?.let { "Saved" }, cloudJson?.let { "Saved" })
                return@mapNotNull CloudArea(file, label(file), false, listOf(field), 0)
            }
            val perField = file in PER_FIELD
            val depth = if (!perField) Int.MAX_VALUE else if (file == CONFIG) 2 else 1
            val fields = diff(file, pcJson, cloudJson, depth)
            if (fields.isEmpty()) return@mapNotNull null
            val shown = if (perField) fields else fields.take(MAX_SHOWN)
            CloudArea(file, label(file), perField, shown, fields.size - shown.size)
        }
    }

    // choices: area file or field id -> side, missing = PC
    fun merge(pc: Map<String, String>, cloud: Map<String, String>, areas: List<CloudArea>, choices: Map<String, CloudSide>): Map<String, String> {
        val merged = pc.toMutableMap()
        for (area in areas) {
            val cloudText = cloud[area.file] ?: continue
            if (!area.perField) {
                if (choices[area.file] == CloudSide.CLOUD) merged[area.file] = cloudText
                continue
            }
            val picked = area.fields.filter { choices[it.id] == CloudSide.CLOUD }
            if (picked.isEmpty()) continue
            val result = parse(pc.getValue(area.file)).deepCopy()
            val cloudJson = parse(cloudText)
            for (field in picked) {
                val path = pathOf(area.file, field.id)
                val value = get(cloudJson, path)
                if (value == null) remove(result, path) else set(result, path, value.deepCopy())
            }
            merged[area.file] = result.toString()
        }
        return merged
    }

    private fun parse(text: String): JsonObject =
        runCatching { JsonParser.parseString(text).asJsonObject }.getOrElse { JsonObject() }

    private fun diff(file: String, pc: JsonObject, cloud: JsonObject, depth: Int, prefix: List<String> = emptyList()): List<CloudField> {
        val keys = (pc.keySet() + cloud.keySet()).filter { it !in IGNORED }
        return keys.flatMap { key ->
            val a = pc.get(key)
            val b = cloud.get(key)
            if (a == b) return@flatMap emptyList()
            val path = prefix + key
            if (depth > 1 && a is JsonObject && b is JsonObject) return@flatMap diff(file, a, b, depth - 1, path)
            listOf(CloudField(idOf(file, path), path.joinToString(" › ") { humanize(it) }, a?.let(::display), b?.let(::display)))
        }
    }

    private fun idOf(file: String, path: List<String>): String = file + "\u0000" + path.joinToString("\u0000")

    private fun pathOf(file: String, id: String): List<String> = id.removePrefix(file + "\u0000").split("\u0000")

    private fun get(root: JsonObject, path: List<String>): JsonElement? {
        var current: JsonElement = root
        for (key in path) current = (current as? JsonObject)?.get(key) ?: return null
        return current
    }

    private fun set(root: JsonObject, path: List<String>, value: JsonElement) {
        var current = root
        for (key in path.dropLast(1)) {
            current = current.get(key) as? JsonObject ?: JsonObject().also { current.add(key, it) }
        }
        current.add(path.last(), value)
    }

    private fun remove(root: JsonObject, path: List<String>) {
        (get(root, path.dropLast(1)) as? JsonObject)?.remove(path.last())
    }

    fun display(value: JsonElement): String {
        val text = when {
            value is JsonPrimitive && value.isBoolean -> if (value.asBoolean) "On" else "Off"
            value is JsonPrimitive && value.isString -> value.asString.ifEmpty { "(empty)" }
            value is JsonPrimitive -> value.asString
            value is JsonArray && value.isEmpty -> "(empty)"
            value is JsonArray && value.size() <= 3 && value.all { it is JsonPrimitive } -> value.joinToString(", ") { display(it) }
            value is JsonArray -> if (value.size() == 1) "1 entry" else "${value.size()} entries"
            value is JsonObject -> if (value.size() == 1) "1 value" else "${value.size()} values"
            else -> "(empty)"
        }
        return if (text.length > MAX_TEXT) text.take(MAX_TEXT - 1) + "…" else text
    }

    // "mobsSinceInq" -> "Mobs Since Inq"
    fun humanize(key: String): String {
        if (key.contains(' ')) return key
        return key.replace('_', ' ')
            .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }
}
