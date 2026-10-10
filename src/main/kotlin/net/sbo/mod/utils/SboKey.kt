package net.sbo.mod.utils

import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.data.LocalStore
import net.sbo.mod.utils.data.cloud.CloudSync

// Saved in <game dir>/.sbo/sbo-auth.json
object SboKey {
    private val store = LocalStore("sbo-auth.json")

    fun get(): String {
        migrateLegacy()
        return store[Player.accountUuid()] ?: ""
    }

    fun set(key: String) {
        store[Player.accountUuid()] = key
        CloudSync.onSboKeySet()
    }

    fun clear() {
        store[Player.accountUuid()] = null
    }

    private fun migrateLegacy() {
        if (sboData.sboKey.isBlank()) return
        if (store[Player.accountUuid()] == null) store[Player.accountUuid()] = sboData.sboKey
        sboData.sboKey = ""
        sboData.save()
    }
}
