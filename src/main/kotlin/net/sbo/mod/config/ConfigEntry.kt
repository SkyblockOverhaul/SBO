package net.sbo.mod.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.reflect.KProperty

sealed interface ConfigElement

class Separator(
    val title: String,
    val description: String,
    val condition: () -> Boolean,
) : ConfigElement

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

    var value: T = default
        private set
    var onChange: (T, T) -> Unit = { _, _ -> }

    fun get(): T = value

    fun set(newValue: T) {
        val old = value
        value = newValue
        if (!same(old, newValue)) onChange(old, newValue)
    }

    fun reset() = set(default)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) = set(newValue)

    internal open fun same(a: T, b: T): Boolean = a == b

    internal abstract fun toJson(): JsonElement

    internal abstract fun parse(json: JsonElement): T?

    internal fun comments(): List<String> = listOfNotNull(description.takeIf { it.isNotEmpty() }) + typeComments()

    internal open fun typeComments(): List<String> = emptyList()
}

class BooleanEntry internal constructor(id: String, default: Boolean, name: String, description: String) :
    ConfigEntry<Boolean>(id, default, name, description) {

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
