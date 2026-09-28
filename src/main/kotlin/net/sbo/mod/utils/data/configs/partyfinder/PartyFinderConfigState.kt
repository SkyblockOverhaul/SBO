package net.sbo.mod.utils.data.configs.partyfinder

import net.sbo.mod.utils.data.DataManager

data class PartyFinderConfigState(
    var checkboxes: Checkboxes = Checkboxes(),
    var inputs: Inputs = Inputs(),
    var filters: Filters = Filters()
) {
    fun save() = DataManager.save("PartyFinderConfigState")
}
