package net.sbo.mod.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

sealed interface ConfigElement

class Sub(
    val title: String,
    val description: String,
    val condition: () -> Boolean,
) : ConfigElement {
    /** Only subs with a title can be switched off, the others just separate. */
    val toggleable: Boolean get() = title.isNotEmpty()

    /** Off: the on/off settings in it read as off and the settings screen hides them. */
    var enabled: Boolean = true
        private set

    internal val entries = mutableListOf<ConfigEntry<*>>()

    fun setEnabled(value: Boolean) {
        if (enabled == value || !toggleable) return
        // Features that react to an on/off setting see it switch with the sub, and so do settings depending on it
        notifyChanges(entries, includeRoots = true) { enabled = value }
    }
}

/**
 * Runs [change], then calls onChange on every on/off setting depending on [roots] (also further down the chain)
 * whose value flipped because of it, and with [includeRoots] on the on/off [roots] themselves too.
 */
internal fun notifyChanges(roots: Collection<ConfigEntry<*>>, includeRoots: Boolean, change: () -> Unit) {
    val watched = LinkedHashSet<BooleanEntry>()
    fun collect(entry: ConfigEntry<*>) {
        for (dependent in entry.dependents) if (watched.add(dependent)) collect(dependent)
    }
    for (root in roots) {
        if (includeRoots && root is BooleanEntry) watched += root
        collect(root)
    }
    // A root's own onChange is the caller's job
    if (!includeRoots) watched.removeAll(roots.toSet())

    val before = watched.associateWith { it.get() }
    change()
    for (entry in watched) entry.invalidate()
    for ((entry, old) in before) {
        val new = entry.get()
        if (old != new) entry.onChange(old, new)
    }
}

/** One setting that has to be in a certain state for another one to be usable. */
class Condition internal constructor(
    private val property: KProperty0<*>,
    /** Shown after the setting's name, e.g. "off", empty for plain "on". */
    val state: String,
    private val check: (Any?) -> Boolean,
) {
    private var resolved: ConfigEntry<*>? = null

    /** Looked up on first use since it can be declared further down, null while its category is still initializing. */
    val entry: ConfigEntry<*>?
        get() = resolved ?: (property.apply { isAccessible = true }.getDelegate() as? ConfigEntry<*>)?.also { resolved = it }

    // Counts as met until the setting is found, so a read during init doesn't lock anything
    val met: Boolean get() = entry?.let { check(it.get()) } ?: true
}

/** A group of [conditions] that all, or with [any] at least one, have to be met. */
class Requirement internal constructor(val conditions: List<Condition>, val any: Boolean) {
    val met: Boolean
        get() {
            var index = 0
            while (index < conditions.size) {
                val conditionMet = conditions[index].met
                if (conditionMet == any) return any
                index++
            }
            return !any
        }
}

class Button(
    val title: String,
    val description: String,
    val text: String?,
    val condition: () -> Boolean,
    val onClick: () -> Unit,
) : ConfigElement

// The JSON matches what ResourcefulConfig wrote, so existing config.jsonc files and cloud saves load unchanged
sealed class ConfigEntry<T>(
    val id: String,
    val default: T,
    val name: String,
    val description: String,
) : ConfigElement {

    /** The saved value, also while its [sub] is off. Features read [get] / the delegate instead. */
    var value: T = default
        private set
    var onChange: (T, T) -> Unit = { _, _ -> }

    internal var sub: Sub? = null

    /** From `depends(...)`, `dependsOff(...)` and `dependsAny(...)` in the entry's builder. */
    var requirements: List<Requirement> = emptyList()
        internal set

    /** On/off settings with a requirement on this one, linked when the config loads. */
    internal val dependents = LinkedHashSet<BooleanEntry>()

    internal fun linkRequirements() {
        if (this !is BooleanEntry) return
        var all = true
        for (requirement in requirements) for (condition in requirement.conditions) {
            val target = condition.entry
            if (target == null) all = false else target.dependents.add(this)
        }
        linked = all
        invalidate()
    }

    /** False while a requirement isn't met, the settings screen then locks this one. */
    val requirementsMet: Boolean
        get() {
            var index = 0
            while (index < requirements.size) {
                if (!requirements[index].met) return false
                index++
            }
            return true
        }

    internal open val effectiveValue: T get() = value

    /** Drops a kept [effectiveValue], called whenever something it follows from may have changed. */
    internal open fun invalidate() {}

    fun get(): T = effectiveValue

    fun set(newValue: T) {
        notifyChanges(listOf(this), includeRoots = false) {
            val old = effectiveValue
            value = newValue
            invalidate()
            val new = effectiveValue
            if (!same(old, new)) onChange(old, new)
        }
    }

    fun reset() = set(default)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = effectiveValue
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) = set(newValue)

    internal open fun same(a: T, b: T): Boolean = a == b

    internal abstract fun toJson(): JsonElement

    internal abstract fun parse(json: JsonElement): T?

    internal fun comments(): List<String> = listOfNotNull(description.takeIf { it.isNotEmpty() }) + typeComments()

    internal open fun typeComments(): List<String> = emptyList()
}

class BooleanEntry internal constructor(id: String, default: Boolean, name: String, description: String) :
    ConfigEntry<Boolean>(id, default, name, description) {

    // Some features read their setting for every frame or chunk, and a requirement chain makes that a walk over
    // several settings, so the result is kept until its own set, its sub switching or a setting it depends on
    // changing (all of these go through notifyChanges)
    @Volatile private var cache = UNKNOWN

    /** Every setting its requirements point at knows about this one, so a change there drops [cache]. */
    internal var linked = false

    // A requirement switching does not call onChange here, only the sub does that
    override val effectiveValue: Boolean
        get() {
            val cached = cache
            if (cached != UNKNOWN) return cached == ON
            val result = value && sub?.enabled != false && requirementsMet
            if (linked) cache = if (result) ON else OFF
            return result
        }

    override fun invalidate() {
        cache = UNKNOWN
    }

    override fun toJson() = JsonPrimitive(value)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isBoolean }?.asBoolean

    private companion object {
        const val UNKNOWN = 0
        const val OFF = 1
        const val ON = 2
    }
}

class NumberEntry<T> internal constructor(
    id: String,
    default: T,
    name: String,
    description: String,
    val range: ClosedRange<T>?,
    val slider: Boolean,
    private val convert: (Number) -> T,
) : ConfigEntry<T>(id, default, name, description) where T : Number, T : Comparable<T> {

    override fun toJson() = JsonPrimitive(value)

    // Out-of-range values are rejected (the old value stays), like ResourcefulConfig did.
    override fun parse(json: JsonElement): T? {
        val number = (json as? JsonPrimitive)?.takeIf { it.isNumber }?.asNumber ?: return null
        return convert(number).takeIf { range == null || it in range }
    }

    override fun typeComments(): List<String> {
        val range = range ?: return listOf("Type: " + typeName(default))
        // ResourcefulConfig used the system locale here ("0,5" in German); comments only, so a fixed one is fine.
        val format = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT)).apply {
            isGroupingUsed = false
            maximumFractionDigits = 340
        }
        return listOf("Range: " + format.format(range.start.toDouble()) + " - " + format.format(range.endInclusive.toDouble()))
    }

    private fun typeName(value: Number) = when (value) {
        is Int -> "Integer"
        is Float -> "Float"
        is Double -> "Double"
        is Long -> "Long"
        else -> value.javaClass.simpleName
    }
}

// ARGB
class ColorEntry internal constructor(
    id: String,
    default: Int,
    name: String,
    description: String,
    val allowAlpha: Boolean,
    val presets: IntArray,
) : ConfigEntry<Int>(id, default, name, description) {

    override fun toJson() = JsonPrimitive(value)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isNumber }?.asNumber?.toInt()
    override fun typeComments() = listOf(if (allowAlpha) "[Color Format: RGBA]" else "[Color Format: RGB]")
}

class StringEntry internal constructor(id: String, default: String, name: String, description: String) :
    ConfigEntry<String>(id, default, name, description) {

    override fun toJson() = JsonPrimitive(value)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isString }?.asString
}

class Choice(val id: String, val label: String, val description: String = "")

/** An enum constant in a dropdown: the settings screen shows [label], [description] when hovered. */
interface Option {
    val label: String
    val description: String get() = ""
}

/** Marks the constant that turns the feature off, see [isOff] and [nextOption]. */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class OffOption

private val offConstants = object : ClassValue<Set<String>>() {
    override fun computeValue(type: Class<*>): Set<String> =
        type.fields.filter { it.isEnumConstant && it.isAnnotationPresent(OffOption::class.java) }.mapTo(HashSet()) { it.name }
}

// A constant with a body is a subclass of its enum
private val Enum<*>.enumClass: Class<*> get() = javaClass.let { if (it.isEnum) it else it.superclass }

/** True for the constant marked with [OffOption]. */
val Enum<*>.isOff: Boolean get() = name in offConstants.get(enumClass)

val Enum<*>.optionLabel: String get() = (this as? Option)?.label ?: name.lowercase().replaceFirstChar { it.uppercase() }

val Enum<*>.optionDescription: String get() = (this as? Option)?.description ?: ""

/** The next constant, from the last one back to the first. The [OffOption] one is skipped, so cycling never turns a feature off. */
fun <E : Enum<E>> E.nextOption(): E {
    @Suppress("UNCHECKED_CAST")
    val cycle = (enumClass.enumConstants as Array<E>).filterNot { it.isOff }
    return cycle.getOrNull((cycle.indexOf(this) + 1) % cycle.size) ?: this
}

// A string out of options that can change while the game runs (e.g. files in a folder), shown as a dropdown.
// Ids that aren't an option (anymore) are kept, the code reading the entry picks the fallback.
class ChoiceEntry internal constructor(
    id: String,
    default: String,
    name: String,
    description: String,
    val options: () -> List<Choice>,
) : ConfigEntry<String>(id, default, name, description) {

    override fun toJson() = JsonPrimitive(value)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isString }?.asString
}

class StringsEntry internal constructor(id: String, default: Array<String>, name: String, description: String) :
    ConfigEntry<Array<String>>(id, default, name, description) {

    override fun same(a: Array<String>, b: Array<String>) = a.contentEquals(b)
    override fun toJson() = JsonArray().apply { value.forEach(::add) }

    override fun parse(json: JsonElement): Array<String>? {
        val array = json as? JsonArray ?: return null
        return array.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.asString }.toTypedArray()
    }
}

// Saved as the constant's name in lower case. Read ignoring case, so files from before (upper case names) load too.
private fun <E : Enum<E>> List<E>.byId(id: String) = firstOrNull { it.name.equals(id, ignoreCase = true) }

private fun Enum<*>.id() = name.lowercase()

// One constant of an enum, shown as a dropdown
class OptionEntry<E : Enum<E>> @PublishedApi internal constructor(
    id: String,
    default: E,
    name: String,
    description: String,
    val constants: List<E>,
) : ConfigEntry<E>(id, default, name, description) {

    override fun toJson() = JsonPrimitive(value.id())
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isString }?.let { constants.byId(it.asString) }

    fun setName(name: String) {
        constants.byId(name)?.let(::set)
    }

    override fun typeComments() = listOf("Valid Values: " + constants.joinToString { it.id() })
}

// Any number of constants of an enum, shown as a multi select, kept in the order of the enum
class OptionsEntry<E : Enum<E>> @PublishedApi internal constructor(
    id: String,
    default: Set<E>,
    name: String,
    description: String,
    val constants: List<E>,
) : ConfigEntry<Set<E>>(id, default, name, description) {

    override fun toJson() = JsonArray().apply { value.forEach { add(it.id()) } }

    // Unknown names (e.g. a removed warp) are dropped instead of rejecting the whole list
    override fun parse(json: JsonElement): Set<E>? {
        val array = json as? JsonArray ?: return null
        val ids = array.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.asString }
        return constants.filterTo(LinkedHashSet()) { constant -> ids.any { constant.name.equals(it, ignoreCase = true) } }
    }

    fun setNames(names: Collection<String>) = set(constants.filterTo(LinkedHashSet()) { it.name in names })

    override fun typeComments() = listOf("Valid Values: " + constants.joinToString { it.id() })
}
