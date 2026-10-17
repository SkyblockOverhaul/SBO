package net.sbo.mod.config

class SearchHit(val path: List<Category>, val sub: Sub?, val element: ConfigElement) {
    val category: Category get() = path.last()
}

private val FORMATTING = Regex("§.")

internal fun String.stripFormatting() = replace(FORMATTING, "")

// Category and sub names count too, so "diana color" finds the colors in the Diana category
fun search(config: Config, query: String): List<SearchHit> {
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val hits = mutableListOf<SearchHit>()

    fun visit(path: List<Category>) {
        val category = path.last()
        val names = path.drop(1).joinToString(" ") { it.name }
        var sub: Sub? = null
        for (element in category.elements) {
            val own = when (element) {
                is Sub -> {
                    sub = element
                    continue
                }
                is Button -> listOfNotNull(element.title, element.description, element.text)
                is ConfigEntry<*> -> listOf(element.name, element.description, element.id)
            }
            val text = (own + names + listOfNotNull(sub?.title)).joinToString(" ").stripFormatting().lowercase()
            if (words.all { it in text }) hits += SearchHit(path, sub, element)
        }
        category.children.forEach { visit(path + it) }
    }

    visit(listOf(config))
    return hits
}
