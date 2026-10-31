package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.general.ModConflicts
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged

/**
 * Finds other mods that hook into the Diana tracker, so the report can name them. Checked once per game.
 */
internal object BphGuard {
    private const val MAX_MODS = 10

    // The classes behind the tracker and its report
    private val WATCHED = listOf(
        "net.sbo.mod.utils.SboTimerManagerKt",
        "net.sbo.mod.utils.SboTimerManager",
        "net.sbo.mod.utils.SboTimerManager\$SBOTimer",
        "net.sbo.mod.diana.DianaTracker",
        "net.sbo.mod.utils.data.DataManager",
        "net.sbo.mod.utils.data.TrackerSeal",
        "net.sbo.mod.utils.data.configs.diana.DianaItemsData",
        "net.sbo.mod.utils.data.configs.diana.DianaTrackerMayorData",
        "net.sbo.mod.utils.game.Mayor",
        "net.sbo.mod.partyfinder.StatReporter",
        "net.sbo.mod.partyfinder.BphGuard",
    )

    @Volatile
    private var found: List<String>? = null

    /** Ids of the mods found, empty when nothing hooks in. */
    internal fun foreignMods(): List<String> = found ?: runCatching(::check)
        .onFailure { logger.warn("[SBO] Could not check for mods changing the burrows per hour", it) }
        .getOrDefault(emptyList())
        .also { found = it }

    private fun check(): List<String> {
        val owners = ModConflicts.mixinPackages()
        fun modOf(mixinClass: String) = owners.entries.filter { mixinClass.startsWith(it.key + ".") }
            .maxByOrNull { it.key.length }?.value?.metadata?.id ?: mixinClass.substringBeforeLast('.')
        // Other mods' mixins leave @MixinMerged on every method they add or change. Class.forName loads the class, so
        // this also sees mixins of classes nothing has touched yet.
        val mods = WATCHED.flatMap { name ->
            val type = runCatching { Class.forName(name) }.getOrNull() ?: return@flatMap emptyList()
            type.declaredMethods.mapNotNull { it.getAnnotation(MixinMerged::class.java)?.mixin }
        }.filterNot { it.startsWith("net.sbo.mod.") }.map(::modOf).distinct().take(MAX_MODS)
        if (mods.isNotEmpty()) logger.warn("[SBO] Mods hooking into the burrows per hour tracking: ${mods.joinToString()}")
        return mods
    }
}
