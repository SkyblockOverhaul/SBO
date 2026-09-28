package net.sbo.mod.utils.data.configs.partyfinder

data class Inputs(
    var custom: CustomInputs = CustomInputs(),
    var diana: DianaInputs = DianaInputs()
)

data class CustomInputs(
    var lvl: Int = 0,
    var mp: Int = 0,
    var partySize: Int = 0,
    var note: String = "..."
)

data class DianaInputs(
    var kills: Int = 0,
    var lvl: Int = 0,
    var note: String = "..."
)
