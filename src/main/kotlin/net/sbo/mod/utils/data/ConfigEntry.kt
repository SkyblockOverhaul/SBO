package net.sbo.mod.utils.data

/**
 * Metadata for a single persistent config.
 * Registered in the DataRegistry.
 */
data class ConfigEntry<T : Any>(
    val name: String,           // "SboData" - for dirty tracking
    val fileName: String,       // "SboData.json"
    val dataClass: Class<T>,    // For Gson deserialization
    val getter: () -> T          // Lambda that retrieves the data
)
