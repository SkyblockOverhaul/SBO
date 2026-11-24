package net.sbo.mod.utils.data.cloud

import com.google.gson.Gson
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.LocalStore

// The files of the last upload or download on this PC, so Compare knows which side changed since then. Never uploaded.
object CloudBase {
    private class Saved(val version: Int = 0, val files: Map<String, String> = emptyMap())

    // uuid -> Saved as json
    private val store = LocalStore("cloud-base.json")
    private val gson = Gson()

    fun save(uuid: String, version: Int, files: Map<String, String>) {
        runCatching { store[uuid] = gson.toJson(Saved(version, files)) }
            .onFailure { SBOKotlin.logger.warn("[CloudSync] could not remember the synced data: ${it.message}") }
    }

    // Only valid if this PC still builds on the same cloud version
    fun load(uuid: String, version: Int): Map<String, String>? {
        val saved = runCatching { gson.fromJson(store[uuid] ?: return null, Saved::class.java) }.getOrNull() ?: return null
        return saved.files.takeIf { saved.version == version && version != 0 && it.isNotEmpty() }
    }

    fun clear(uuid: String) {
        runCatching { store[uuid] = null }
            .onFailure { SBOKotlin.logger.warn("[CloudSync] could not forget the synced data: ${it.message}") }
    }
}
