package net.sbo.mod.guis.partyfinder.pages

import gg.essential.elementa.constraints.PositionConstraint
import net.sbo.mod.utils.data.Party
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.Reqs

interface PartyPage {
    val pageName: String
    val partyType: String
    val listDisplayName: String

    fun render()
    fun createParty() {}
    fun addFilter(x: PositionConstraint, y: PositionConstraint) {}
    fun getPartyInfo(info: PartyPlayerStats): String = ""
    fun getReqsString(reqs: Reqs?, callback: (String) -> Unit) { callback("") }
    fun setFilter() {}
    fun createFilterConfig(stats: PartyPlayerStats): ((Party) -> Boolean)? = null
}
