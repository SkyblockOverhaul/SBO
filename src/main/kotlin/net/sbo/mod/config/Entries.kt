package net.sbo.mod.config

import kotlin.reflect.KProperty

open class EntriesBuilder {

    private val ids = mutableSetOf<String>()
    internal val elements = mutableListOf<ConfigElement>()

    internal fun add(element: ConfigElement) {
        if (element is ConfigEntry<*>) {
            require(element.id.isNotEmpty()) { "Entry id cannot be empty" }
            require('.' !in element.id) { "Entry id ${element.id} cannot contain '.'" }
            require(ids.add(element.id)) { "Entry with id ${element.id} already exists" }
        }
        elements += element
    }

    fun boolean(value: Boolean, builder: TypeBuilder.() -> Unit = {}) =
        EntryProvider(null, ::TypeBuilder, builder) { id, b -> BooleanEntry(id, value, b.name, b.description) }

    fun int(value: Int, builder: NumberBuilder<Int>.() -> Unit = {}) = number(value, Number::toInt, builder)
    fun long(value: Long, builder: NumberBuilder<Long>.() -> Unit = {}) = number(value, Number::toLong, builder)
    fun float(value: Float, builder: NumberBuilder<Float>.() -> Unit = {}) = number(value, Number::toFloat, builder)
    fun double(value: Double, builder: NumberBuilder<Double>.() -> Unit = {}) = number(value, Number::toDouble, builder)

    private fun <T> number(value: T, convert: (Number) -> T, builder: NumberBuilder<T>.() -> Unit)
        where T : Number, T : Comparable<T> =
        EntryProvider(null, { NumberBuilder<T>(it) }, builder) { id, b ->
            NumberEntry(id, value, b.name, b.description, b.range, b.slider, convert)
        }

    fun color(value: Int, builder: ColorBuilder.() -> Unit = {}) =
        EntryProvider(null, ::ColorBuilder, builder) { id, b -> ColorEntry(id, value, b.name, b.description, b.allowAlpha, b.presets) }

    fun string(value: String, builder: TypeBuilder.() -> Unit = {}) =
        EntryProvider(null, ::TypeBuilder, builder) { id, b -> StringEntry(id, value, b.name, b.description) }

    fun choice(value: String, builder: ChoiceBuilder.() -> Unit = {}) =
        EntryProvider(null, ::ChoiceBuilder, builder) { id, b -> ChoiceEntry(id, value, b.name, b.description, b.options) }

    fun strings(vararg value: String, builder: TypeBuilder.() -> Unit = {}) =
        EntryProvider(null, ::TypeBuilder, builder) { id, b -> StringsEntry(id, arrayOf(*value), b.name, b.description) }

    inline fun <reified E : Enum<E>> enum(value: E, noinline builder: TypeBuilder.() -> Unit = {}) =
        EntryProvider(null, ::TypeBuilder, builder) { id, b -> EnumEntry(id, value, b.name, b.description, enumValues<E>().toList()) }

    inline fun <reified E : Enum<E>> select(vararg value: E, noinline builder: TypeBuilder.() -> Unit = {}) =
        EntryProvider(null, ::TypeBuilder, builder) { id, b -> SelectEntry(id, arrayOf(*value), b.name, b.description, E::class.java) }

    fun button(builder: ButtonBuilder.() -> Unit) {
        val button = ButtonBuilder().apply(builder)
        add(Button(button.title, button.description, button.text, button.condition, button.callback))
    }

    fun separator(builder: SeparatorBuilder.() -> Unit) {
        val separator = SeparatorBuilder().apply(builder)
        add(Separator(separator.title, separator.description, separator.condition))
    }

    fun <T> observable(entry: ConfigDelegateProvider<T>, onChange: (T, T) -> Unit) = ObservableEntry(entry, onChange)

    companion object {
        // Kept from ResourcefulConfigKt so the existing categories compile unchanged
        fun Literal(value: String): String = value
    }
}

interface ConfigDelegateProvider<T> {
    operator fun provideDelegate(entries: EntriesBuilder, prop: KProperty<*>): ConfigEntry<T>
}

class EntryProvider<T, B : TypeBuilder> @PublishedApi internal constructor(
    private val id: String?,
    private val builderFactory: (String) -> B,
    private val builder: B.() -> Unit,
    private val create: (String, B) -> ConfigEntry<T>,
) : ConfigDelegateProvider<T> {

    override operator fun provideDelegate(entries: EntriesBuilder, prop: KProperty<*>): ConfigEntry<T> {
        val id = id ?: prop.name
        val entry = create(id, builderFactory(id).apply(builder))
        entries.add(entry)
        return entry
    }
}

// Also called when the file is loaded
class ObservableEntry<T>(
    private val entry: ConfigDelegateProvider<T>,
    private val onChange: (T, T) -> Unit,
) : ConfigDelegateProvider<T> {

    override operator fun provideDelegate(entries: EntriesBuilder, prop: KProperty<*>): ConfigEntry<T> =
        entry.provideDelegate(entries, prop).also { it.onChange = onChange }
}

open class TypeBuilder(val id: String) {
    var name: String = id
    var description: String = ""
}

class NumberBuilder<T>(id: String) : TypeBuilder(id) where T : Number, T : Comparable<T> {
    var range: ClosedRange<T>? = null
    var slider: Boolean = false
}

class ColorBuilder(id: String) : TypeBuilder(id) {
    var allowAlpha: Boolean = false
    var presets: IntArray = intArrayOf()
}

class ChoiceBuilder(id: String) : TypeBuilder(id) {
    var options: () -> List<Choice> = { emptyList() }
}

class ButtonBuilder {
    var title: String = ""
    var description: String = ""
    var text: String? = null
    var condition: () -> Boolean = { true }
    internal var callback: () -> Unit = {}

    fun onClick(callback: () -> Unit) {
        this.callback = callback
    }
}

class SeparatorBuilder {
    var title: String = ""
    var description: String = ""
    var condition: () -> Boolean = { true }
}
