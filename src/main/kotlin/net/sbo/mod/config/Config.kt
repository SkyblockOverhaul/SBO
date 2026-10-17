package net.sbo.mod.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

open class Category(val id: String) : EntriesBuilder() {

    open val name: String get() = id
    open val description: String get() = ""

    internal val categories = LinkedHashMap<String, Category>()

    fun <T : Category> category(category: T): T {
        require(category.id !in categories) { "Category with id ${category.id} already exists" }
        categories[category.id] = category
        return category
    }

    val entries: List<ConfigEntry<*>> get() = elements.filterIsInstance<ConfigEntry<*>>()
    val subcategories: Collection<Category> get() = categories.values

    // Missing, unknown or invalid values leave the entry as it is
    open fun load(json: JsonObject) {
        for (entry in entries) {
            val data = json.get(entry.id) ?: continue
            if (!entry.load(data)) logger.warn("Invalid value for config entry {}: {}", entry.id, data)
        }
        for ((id, category) in categories) {
            (json.get(id) as? JsonObject)?.let(category::load)
        }
    }

    fun resetAll() {
        entries.forEach { it.reset() }
        categories.values.forEach { it.resetAll() }
    }

    internal fun write(out: StringBuilder, indentation: Int, extra: List<Pair<String, Pair<String, JsonElement>>> = emptyList()) {
        val members = extra.map { (key, value) -> Triple(key, listOf(value.first), value.second.toString()) } +
            entries.map { Triple(it.id, it.comments(), it.toJson().toString(indentation + 1)) } +
            categories.map { (id, category) -> Triple(id, emptyList(), StringBuilder().also { category.write(it, indentation + 1) }.toString()) }
        writeObject(out, indentation, members)
    }

    companion object {
        internal val logger = LoggerFactory.getLogger("sbo-config")
    }
}

open class Config(val file: String) : Category(file) {

    open val version: Int get() = 0

    private var configDir: Path? = null
    private val path: Path get() = checkNotNull(configDir) { "Config $file was never loaded" }.resolve("$file.jsonc")

    private val backup: Path get() = path.resolveSibling("${path.fileName}.bak")

    fun load(configDir: Path) {
        this.configDir = configDir
        if (Files.exists(path)) {
            val current = read(path)
            val json = current ?: read(backup)
            if (current == null) {
                // Moved away so the save below doesn't copy it over the good backup; kept for a manual rescue
                Files.move(path, path.resolveSibling("${path.fileName}.broken"), StandardCopyOption.REPLACE_EXISTING)
                if (json != null) logger.warn("{} is broken, restored the settings from {}", path, backup.fileName)
                else logger.error("{} is broken and there is no usable backup, using defaults", path)
            }
            json?.let(::load)
        }
        save()
    }

    private fun read(file: Path): JsonObject? =
        if (!Files.exists(file)) null
        else runCatching { parse(Files.readString(file)) }.onFailure { logger.error("Could not read {}", file, it) }.getOrNull()

    fun reload() {
        load(checkNotNull(configDir) { "Config $file was never loaded" })
    }

    fun save() {
        Files.createDirectories(path.parent)
        val temp = path.resolveSibling("${path.fileName}.tmp")
        // On disk before the rename, otherwise a power cut can leave an empty config.jsonc behind
        FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use {
            it.write(ByteBuffer.wrap(toJsonc().toByteArray(Charsets.UTF_8)))
            it.force(true)
        }
        // The last good file, loaded when config.jsonc can't be read
        if (read(path) != null) Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING)
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun toJsonc(): String = StringBuilder().also {
        write(it, 0, listOf(VERSION_KEY to (VERSION_COMMENT to JsonPrimitive(version))))
    }.toString()

    companion object {
        const val VERSION_KEY = "rconfig:version"
        private const val VERSION_COMMENT = "The version of the config file. Do not change this unless you know what you are doing."

        fun parse(text: String): JsonObject = JsonParser.parseString(text).asJsonObject
    }
}

private const val INDENT = "    "

internal fun ConfigEntry<*>.load(json: JsonElement): Boolean {
    @Suppress("UNCHECKED_CAST")
    val entry = this as ConfigEntry<Any?>
    val value = entry.parse(json) ?: return false
    entry.set(value)
    return true
}

private fun JsonElement.toString(indentation: Int): String {
    if (!isJsonArray) return toString()
    val array = asJsonArray
    if (array.isEmpty) return "[]"
    return array.joinToString(",\n", "[\n", "\n" + INDENT.repeat(indentation) + "]") {
        INDENT.repeat(indentation + 1) + it.toString(indentation + 1)
    }
}

private fun writeObject(out: StringBuilder, indentation: Int, members: List<Triple<String, List<String>, String>>) {
    if (members.isEmpty()) {
        out.append("{}")
        return
    }
    val pad = INDENT.repeat(indentation + 1)
    out.append("{\n")
    members.forEachIndexed { i, (key, comments, value) ->
        val lines = comments.flatMap { it.lines() }
        if (lines.size == 1) {
            out.append(pad).append("// ").append(lines[0]).append('\n')
        } else if (lines.size > 1) {
            out.append(pad).append("/*\n")
            lines.forEach { out.append(pad).append(" * ").append(it).append('\n') }
            out.append(pad).append(" */\n")
        }
        out.append(pad).append('"').append(key).append("\": ").append(value)
        out.append(if (i < members.lastIndex) ",\n" else "\n")
    }
    out.append(INDENT.repeat(indentation)).append('}')
}
