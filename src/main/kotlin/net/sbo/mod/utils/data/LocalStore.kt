package net.sbo.mod.utils.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime

// Stored in <game dir>/.sbo so shared configs don't include it
class LocalStore(fileName: String) {
    private val file: Path = FabricLoader.getInstance().gameDir.resolve(".sbo").resolve(fileName)
    private val gson = Gson()
    private val mapType = object : TypeToken<MutableMap<String, String>>() {}.type

    private var cache: Map<String, String> = emptyMap()
    private var cachedAt: FileTime? = null

    // Reload if another instance changed it
    @Synchronized
    private fun readAll(): Map<String, String> {
        val modified = runCatching { Files.getLastModifiedTime(file) }.getOrNull()
        if (modified == cachedAt) return cache
        cache = try {
            if (modified != null) gson.fromJson(Files.readString(file), mapType) ?: emptyMap() else emptyMap()
        } catch (e: Exception) {
            SBOKotlin.logger.error("Could not read $file", e)
            emptyMap()
        }
        cachedAt = modified
        return cache
    }

    operator fun get(uuid: String): String? = readAll()[uuid]

    // null removes
    @Synchronized
    operator fun set(uuid: String, value: String?) {
        val values = readAll().toMutableMap()
        if (value == null) values.remove(uuid) else values[uuid] = value
        Files.createDirectories(file.parent)
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(tmp, gson.toJson(values))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        cache = values
        cachedAt = runCatching { Files.getLastModifiedTime(file) }.getOrNull()
    }
}
