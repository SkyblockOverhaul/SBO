package net.sbo.mod.utils.data.configs.partyfinder

data class Filters(
    var custom: CustomFilters = CustomFilters(),
    var diana: DianaFilters = DianaFilters()
)

data class CustomFilters(
    var eman9Filter: Boolean = false,
    var noteFilter: String = ".",
    var canIjoinFilter: Boolean = false
)

data class DianaFilters(
    var eman9Filter: Boolean = false,
    var looting5Filter: Boolean = false,
    var canIjoinFilter: Boolean = false
)
