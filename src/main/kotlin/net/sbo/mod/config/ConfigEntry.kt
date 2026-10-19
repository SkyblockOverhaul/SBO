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
    val met: Boolean get() = if (any) conditions.any { it.met } else conditions.all { it.met }
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
        for (requirement in requirements) for (condition in requirement.conditions) condition.entry?.dependents?.add(this)
    }

    /** False while a requirement isn't met, the settings screen then locks this one. */
    val requirementsMet: Boolean get() = requirements.all { it.met }

    internal open val effectiveValue: T get() = value

    fun get(): T = effectiveValue

    fun set(newValue: T) {
        notifyChanges(listOf(this), includeRoots = false) {
            val old = effectiveValue
            value = newValue
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

    // A requirement switching does not call onChange here, only the sub does that
    override val effectiveValue: Boolean get() = value && sub?.enabled != false && requirementsMet

    override fun toJson() = JsonPrimitive(value)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isBoolean }?.asBoolean
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

class EnumEntry<E : Enum<E>> @PublishedApi internal constructor(
    id: String,
    default: E,
    name: String,
    description: String,
    val constants: List<E>,
) : ConfigEntry<E>(id, default, name, description) {

    override fun toJson() = JsonPrimitive(value.name)
    override fun parse(json: JsonElement) = (json as? JsonPrimitive)?.takeIf { it.isString }?.let { p -> constants.find { it.name == p.asString } }

    fun setName(name: String) {
        constants.find { it.name == name }?.let(::set)
    }
    override fun typeComments() = listOf("Valid Values: " + constants.joinToString { it.name })
}

class SelectEntry<E : Enum<E>> @PublishedApi internal constructor(
    id: String,
    default: Array<E>,
    name: String,
    description: String,
    private val enumClass: Class<E>,
) : ConfigEntry<Array<E>>(id, default, name, description) {

    val constants: List<E> = enumClass.enumConstants.toList()

    override fun same(a: Array<E>, b: Array<E>) = a.contentEquals(b)
    override fun toJson() = JsonArray().apply { value.forEach { add(it.name) } }

    // Unknown names (e.g. a removed warp) are dropped instead of rejecting the whole list.
    override fun parse(json: JsonElement): Array<E>? {
        val array = json as? JsonArray ?: return null
        val names = array.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.asString }
        return arrayOf(names.mapNotNull { name -> constants.find { it.name == name } })
    }

    fun setNames(names: Collection<String>) = set(arrayOf(constants.filter { it.name in names }))

    // A real E[], code reading the entry casts it to its enum's array type
    private fun arrayOf(constants: List<E>): Array<E> {
        @Suppress("UNCHECKED_CAST")
        val result = java.lang.reflect.Array.newInstance(enumClass, constants.size) as Array<E>
        constants.forEachIndexed { i, constant -> result[i] = constant }
        return result
    }

    override fun typeComments() = listOf("Valid Values: " + constants.joinToString { it.name })
}
