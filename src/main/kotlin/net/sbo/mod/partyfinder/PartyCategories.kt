package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.partyfinder.api.CategoriesData
import net.sbo.mod.partyfinder.api.CategoryDef
import net.sbo.mod.partyfinder.api.ItemChoice
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyOption
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.ReqDef
import net.sbo.mod.partyfinder.api.RoleDef
import net.sbo.mod.partyfinder.api.StatDef
import net.sbo.mod.partyfinder.api.SubcategoryDef

/** Party categories, stats and requirements as the backend defines them (`GET /pf/categories`). */
object PartyCategories {
    // Event subcategories open and close, so the definitions are not kept forever
    private const val MAX_AGE_MS = 10 * 60 * 1000L

    @Volatile
    var data: CategoriesData? = null
        private set

    private var loadedAt = 0L
    private var loading = false
    private val waiting = mutableListOf<(CategoriesData?) -> Unit>()

    val version: String? get() = data?.version

    /** Calls [callback] with the definitions, loading them first when missing, old or [force]d. Null on failure. */
    fun get(force: Boolean = false, callback: (CategoriesData?) -> Unit) {
        synchronized(this) {
            val cached = data
            if (!force && cached != null && System.currentTimeMillis() - loadedAt < MAX_AGE_MS) {
                callback(cached)
                return
            }
            waiting += callback
            if (loading) return
            loading = true
        }
        PartyFinderApi.categories(
            onError = { error ->
                logger.warn("[SBO] Party categories not loaded: ${error.code} ${error.message}")
                finish(data)
            },
            onSuccess = { loaded ->
                use(loaded)
                finish(loaded)
            }
        )
    }

    private fun finish(result: CategoriesData?) {
        val callbacks = synchronized(this) {
            loading = false
            waiting.toList().also { waiting.clear() }
        }
        callbacks.forEach { it(result) }
    }

    fun use(loaded: CategoriesData) {
        synchronized(this) {
            data = loaded
            loadedAt = System.currentTimeMillis()
        }
    }

    val categories: List<CategoryDef> get() = data?.categories.orEmpty()

    fun stat(id: String): StatDef? = data?.stats?.firstOrNull { it.id == id }

    /** An item choice of a stat, looked up in every category that asks for it. */
    fun choice(statId: String, id: String): ItemChoice? =
        categories.asSequence()
            .flatMap { c -> c.reqs.asSequence() + c.subcategories.asSequence().flatMap { it.reqs.asSequence() } }
            .filter { it.stat == statId }
            .flatMap { it.choices.asSequence() }
            .firstOrNull { it.id == id }

    fun category(id: String): CategoryDef? = data?.categories?.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

    /** Category with its subcategory applied; an unknown [subType] falls back to the first subcategory. */
    fun target(partyType: String, subType: String = ""): PartyTarget? {
        val category = category(partyType) ?: return null
        if (subType.trim().equals(PartyTarget.ALL, ignoreCase = true) && category.subcategories.size > 1) return PartyTarget(category, null, all = true)
        val sub = category.subcategories.firstOrNull { it.id.equals(subType.trim(), ignoreCase = true) }
            ?: category.subcategories.firstOrNull()
        return PartyTarget(category, sub)
    }
}

// Lead time for listing event parties, same as EARLY_LISTING_MS in the backend calendar
private const val EARLY_LISTING_MS = 60 * 60 * 1000L

/**
 * What one party is built and checked against, like `resolveTarget` in the backend.
 * With [all] it stands for the parties of every subcategory; each of them has its own target, see [forParty].
 */
data class PartyTarget(val category: CategoryDef, val sub: SubcategoryDef?, val all: Boolean = false) {
    val partyType: String get() = category.id
    val subType: String get() = if (all) ALL else sub?.id ?: ""
    // All shares the key of the category, so its star is the one of the party type
    val key: String get() = if (sub == null) partyType else "$partyType/$subType"
    val label: String get() = if (sub == null) category.label else "${category.label}: ${sub.label}"

    val minSize: Int get() = category.minSize
    val maxSize: Int get() = when {
        all -> category.subcategories.maxOf { it.maxSize ?: category.maxSize }
        else -> sub?.maxSize ?: category.maxSize
    }
    val open: Boolean get() = sub?.open ?: true
    val opensAt: Long? get() = sub?.opensAt
    val createOpen: Boolean get() = sub?.createOpen ?: open

    val createOpensAt: Long? get() = opensAt?.let { it - EARLY_LISTING_MS }

    val reqs: List<ReqDef> = mergeBy({ it.stat }, category.reqs, sub?.reqs.orEmpty())
    val options: List<PartyOption> = mergeBy({ it.id }, category.options, sub?.options.orEmpty())
    val display: List<String> = (category.display + sub?.display.orEmpty()).distinct()
    val roles: List<RoleDef> get() = sub?.roles ?: category.roles

    fun req(stat: String): ReqDef? = reqs.firstOrNull { it.stat == stat }

    fun option(id: String): PartyOption? = options.firstOrNull { it.id == id }

    fun clampSize(size: Int): Int = size.coerceIn(minSize, maxSize)

    /** The subcategory target a listed party belongs to; itself unless this is [all]. */
    fun forParty(party: PartyView): PartyTarget = if (all) PartyCategories.target(partyType, party.subType) ?: this else this

    fun subTargets(): List<PartyTarget> =
        if (all) category.subcategories.mapNotNull { PartyCategories.target(partyType, it.id) } else listOf(this)

    companion object {
        const val ALL = "all"
        // Later entries replace earlier ones with the same key
        private fun <T> mergeBy(key: (T) -> String, vararg lists: List<T>): List<T> {
            val byKey = LinkedHashMap<String, T>()
            lists.forEach { list -> list.forEach { byKey[key(it)] = it } }
            return byKey.values.toList()
        }
    }
}
