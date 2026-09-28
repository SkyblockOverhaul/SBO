package net.sbo.mod.utils.data.configs.partyfinder

data class Checkboxes(
    var custom: CustomCheckboxes = CustomCheckboxes(),
    var diana: DianaCheckboxes = DianaCheckboxes()
)

data class CustomCheckboxes(
    var eman9: Boolean = false
)

data class DianaCheckboxes(
    var eman9: Boolean = false,
    var looting5: Boolean = false
)
