package net.sbo.mod.utils.data

/**
 * Marks a field as a persistent data file.
 * Used by reflection-based auto-loading in DataManager.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class DataField(val fileName: String)
