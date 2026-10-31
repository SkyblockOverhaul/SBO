package net.sbo.mod.config

/** "1.2.3" or "1.2.3-beta.4", build metadata after "+" is ignored. A beta comes before its release. */
class ModVersion private constructor(private val numbers: List<Int>, val beta: Int?) : Comparable<ModVersion> {

    val isBeta: Boolean get() = beta != null

    /**
     * Settings after this one are new in the release line of this version: for a beta the ones of that beta,
     * for a release the ones of all its betas and the release itself.
     */
    val lineStart: ModVersion get() = ModVersion(numbers, (beta ?: 1) - 1)

    override fun compareTo(other: ModVersion): Int {
        for (i in 0 until maxOf(numbers.size, other.numbers.size)) {
            val diff = numbers.getOrElse(i) { 0 } - other.numbers.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return (beta ?: Int.MAX_VALUE).compareTo(other.beta ?: Int.MAX_VALUE)
    }

    override fun equals(other: Any?) = other is ModVersion && compareTo(other) == 0
    override fun hashCode() = numbers.dropLastWhile { it == 0 }.hashCode() * 31 + (beta ?: -1)
    override fun toString() = numbers.joinToString(".") + (beta?.let { "-beta.$it" } ?: "")

    companion object {
        private val FORMAT = Regex("""(\d+(?:\.\d+)*)(?:-beta\.?(\d+))?""")

        fun parse(text: String?): ModVersion? {
            val match = FORMAT.matchEntire(text?.substringBefore('+')?.trim() ?: return null) ?: return null
            return ModVersion(match.groupValues[1].split('.').map { it.toInt() }, match.groupValues[2].toIntOrNull())
        }
    }
}

/** The settings tagged with a `since` after [after] and up to [upTo], newest first. */
fun newSettings(config: Config, after: ModVersion, upTo: ModVersion): List<SearchHit> {
    val hits = mutableListOf<SearchHit>()
    fun visit(path: List<Category>) {
        var sub: Sub? = null
        for (element in path.last().elements) {
            if (element is Sub) sub = element
            val since = (element as? ConfigEntry<*>)?.since ?: continue
            if (since > after && since <= upTo) hits += SearchHit(path, sub, element)
        }
        path.last().children.forEach { visit(path + it) }
    }
    visit(listOf(config))
    return hits.sortedByDescending { (it.element as ConfigEntry<*>).since }
}

/** Whether [entry] can be switched on and off: on/off settings, and options with an [OffOption]. */
fun switchable(entry: ConfigEntry<*>): Boolean = when (entry) {
    is BooleanEntry -> true
    is OptionEntry<*> -> entry.off != null
    else -> false
}

/** True when [entry] is not off, for [switchable] entries. */
fun isOn(entry: ConfigEntry<*>): Boolean = when (entry) {
    is BooleanEntry -> entry.value
    is OptionEntry<*> -> !entry.value.isOff
    else -> false
}

/** Turns a [switchable] entry on (its default, or the first option that isn't off) or off. */
fun switchTo(entry: ConfigEntry<*>, on: Boolean) {
    when (entry) {
        is BooleanEntry -> entry.set(on)
        is OptionEntry<*> -> entry.switchTo(on)
        else -> {}
    }
}

/** What the settings screen shows under What's New. */
class WhatsNew(
    val title: String,
    val text: String,
    val hits: List<SearchHit>,
    /** Opens the screen on What's New instead of All Settings. */
    val openFirst: Boolean,
    /** Opened once already, the sidebar then shows it without the count. */
    val seen: Boolean,
    /** A setting shown above the new ones, e.g. the one that switches new settings off on their own. */
    val extra: ConfigElement?,
    val onOpened: () -> Unit,
)
