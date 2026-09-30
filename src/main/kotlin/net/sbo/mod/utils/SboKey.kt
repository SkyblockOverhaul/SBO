package net.sbo.mod.utils

import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.HomeStore

// Saved in ~/.sbo/sbo-keys.json
object SboKey {
    private val store = HomeStore("sbo-keys.json")

    fun get(): String {
        migrateLegacy()
        return store[Player.accountUuid()] ?: ""
    }

    fun set(key: String) {
        store[Player.accountUuid()] = key
    }

    fun clear() {
        store[Player.accountUuid()] = null
    }

    private fun migrateLegacy() {
        if (sboData.sboKey.isBlank()) return
        if (store[Player.accountUuid()] == null) set(sboData.sboKey)
        sboData.sboKey = ""
        sboData.save()
    }
}
