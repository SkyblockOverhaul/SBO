package net.sbo.mod.utils.data

/**
 * Central registry for all configs.
 * Entries are registered via @DataField annotation on DataManager fields.
 */
object DataRegistry {
    private val _entries: LinkedHashMap<String, ConfigEntry<*>> = linkedMapOf()
    val entries: Collection<ConfigEntry<*>> get() = _entries.values

    fun register(entry: ConfigEntry<*>) { _entries[entry.name] = entry }
    fun get(name: String): ConfigEntry<*>? = _entries[name]
    fun contains(name: String): Boolean = _entries.containsKey(name)
}
