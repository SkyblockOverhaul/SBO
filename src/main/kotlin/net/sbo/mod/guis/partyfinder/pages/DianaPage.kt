package net.sbo.mod.guis.partyfinder.pages
//todo: remake this with Vexel https://github.com/meowing-xyz/vexel

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.PositionConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import net.sbo.mod.guis.partyfinder.GuiHandler
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import net.sbo.mod.partyfinder.PartyPlayer.getPartyPlayerStats
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.Reqs
import net.sbo.mod.utils.data.SboDataObject.pfConfigState
import net.sbo.mod.utils.data.SboDataObject.sboData
import java.awt.Color


class DianaPage(private val parent: PartyFinderGUI) {
    internal fun getPartyInfo(info: PartyPlayerStats): String {
        var formattedInfoString = ""
        val formattedInfo = listOf(
            Pair("&9Name: &b", info.name),
            Pair("&9Skyblock Level: ", Helper.matchLvlToColor(info.sbLvl)),
            Pair("&9Uuid: &7", info.uuid),
            Pair("&9Eman9: ", Helper.getNumberColor(info.emanLvl, 9)),
            Pair("&9Clover: ", if (info.clover) "&a✔" else "&c✘"),
            Pair("&9Looting 5: ", Helper.getNumberColor(info.daxeLootingLvl, 5)),
            Pair("&9Chimera: ", Helper.getNumberColor(info.daxeChimLvl, 5)),
            Pair("&9Griffin Item: ", Helper.getGriffinItemColor(info.griffinItem?.content)),
            Pair("&9Griffin Rarity: ", Helper.getRarity(info.griffinRarity)),
            Pair("&9Diana Kills: ", Helper.matchDianaKillsToColor(info.mythosKills)),
            Pair("&9Leaderboard: &b#", info.killLeaderboard),
            Pair("&9Magical Power: &b", info.magicalPower),
            Pair("&9Enrichments: &b", info.enrichments),
            Pair("&9Missing Enrichments: &b", info.missingEnrichments),
            Pair("&9Warnings: &7", info.warnings.joinToString(", "))
        )
        formattedInfo.forEach { (key, value) ->
            formattedInfoString += "$key$value\n\n"
        }
        return formattedInfoString
    }

    private fun setFilter() {
        parent.getFilter(parent.selectedPage) { filter ->
            Window.enqueueRenderOperation {
                parent.filterPartyList(filter)
            }
        }
    }

    internal fun getReqsString(reqs: Reqs?, callback: (String) -> Unit) {
        if (reqs == null) {
            callback("")
            return
        }

        getPartyPlayerStats { stats ->
            val sb = StringBuilder()

            if (reqs.lvl > 0) {
                sb.append("§bLvl: ")
                    .append(if (stats.sbLvl >= reqs.lvl) "§a" else "§c")
                    .append(reqs.lvl)
                    .append("§r, ")
            }

            if (reqs.kills > 0) {
                sb.append("§bKills: ")
                    .append(if (stats.mythosKills >= reqs.kills) "§a" else "§c")
                    .append(Helper.formatNumber(reqs.kills))
                    .append("§r, ")
            }

            if (reqs.eman9) {
                sb.append(if (stats.eman9) "§aEman9" else "§cEman9")
                    .append("§r, ")
            }

            if (reqs.looting5) {
                sb.append(if (stats.looting5daxe) "§aLooting5" else "§cLooting5")
                    .append("§r")
            }
            callback(sb.toString())
        }
    }

    internal fun render() {
        Window.enqueueRenderOperation {
            parent.addPartyListFunctions("Diana Party List", ::createParty)
            parent.updateCurrentPartyList(true)
        }
    }

    private fun createParty() {
        parent.openCpWindow()
        parent.cpWindow.setWidth(20.percent())
        parent.cpWindow.setHeight(40.percent())
        parent.reqsBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint()
            width = 100.percent()
            height = 68.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.cpWindow
        val lvlbox = UIBlock().constrain {
            x = 0.percent()
            y = 5.percent()
            width = 100.percent()
            height = 23.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("SbLvL").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf lvlbox
        val lvlInput = GuiHandler.TextInput(
            list = "diana",
            key = "lvl",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        lvlInput.create().setChildOf(lvlbox)
        lvlInput.onlyNumbers = true
        lvlInput.maxChars = 3
        lvlInput.textInputText.setTextScale(parent.getTextScale(1f))
        if (pfConfigState.inputs.diana.lvl > 0) {
            lvlInput.textInputText.setText(pfConfigState.inputs.diana.lvl.toString())
        }

        val killsBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 23.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("Kills").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale(1f)
        }.setColor(Color(255, 255, 255, 255)) childOf killsBox
        val killsInput = GuiHandler.TextInput(
            list = "diana",
            key = "kills",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        killsInput.create().setChildOf(killsBox)
        killsInput.onlyNumbers = true
        killsInput.maxChars = 6
        killsInput.textInputText.setTextScale(parent.getTextScale(1f))
        if (pfConfigState.inputs.diana.kills > 0) {
            killsInput.textInputText.setText(pfConfigState.inputs.diana.kills.toString())
        }

        val noteBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 23.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("Note ").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf noteBox
        val noteInput = GuiHandler.TextInput(
            list = "diana",
            key = "note",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 50.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        noteInput.create().setChildOf(noteBox)
        noteInput.maxChars = 30
        noteInput.textInputText.setTextScale(parent.getTextScale())
        if (pfConfigState.inputs.diana.note.isNotEmpty()) {
            noteInput.textInputText.setText(pfConfigState.inputs.diana.note)
        }

        val l5e9box = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 20.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        val eman9box = UIBlock().constrain {
            x = 0.percent()
            y = 0.percent()
            width = 50.percent()
            height = 100.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf l5e9box
        val eman9checkbox = GuiHandler.Checkbox(
            list = "diana",
            key = "eman9",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Color(0, 0, 0, 200),
            checkedColor = Color(0, 110, 250, 255),
            text = "Eman9",
            rounded = true,
            roundness = 5f
        )
        eman9checkbox.create().setChildOf(eman9box)
        eman9checkbox.setBgBoxColor(Color(50, 50, 50, 200))
        eman9checkbox.textObject.setTextScale(parent.getTextScale(1f))
        val looting5box = UIBlock().constrain {
            x = SiblingConstraint()
            y = 0.percent()
            width = 50.percent()
            height = 100.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf l5e9box
        val looting5checkbox = GuiHandler.Checkbox(
            list = "diana",
            key = "looting5",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Color(0, 0, 0, 200),
            checkedColor = Color(0, 110, 250, 255),
            text = "Looting5",
            rounded = true,
            roundness = 5f
        )
        looting5checkbox.create().setChildOf(looting5box)
        looting5checkbox.setBgBoxColor(Color(50, 50, 50, 200))
        looting5checkbox.textObject.setTextScale(parent.getTextScale(1f))

        parent.createBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 20.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.cpWindow

        val createButton = GuiHandler.Button(
            text = "Create Party",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 70.percent(),
            height = 60.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            parent = parent.createBox,
            rounded = true
        )
        createButton.hoverEffect(Color(50, 50, 50, 200), Color(100, 100, 100, 200))
        createButton.setOnClick {
            val reqsMap = mapOf(
                "lvl" to pfConfigState.inputs.diana.lvl,
                "kills" to pfConfigState.inputs.diana.kills
            )
            var reqString = ""
            reqsMap.forEach { (key, value) ->
                if (value != 0) {
                    reqString += "$key$value,"
                }
            }
            if (pfConfigState.checkboxes.diana.eman9) reqString += "eman9,"
            if (pfConfigState.checkboxes.diana.looting5) reqString += "looting5,"
            val note = pfConfigState.inputs.diana.note
            val partyType = "Diana"
            val sboKey = sboData.sboKey
            if (sboKey.isEmpty() && !sboKey.startsWith("sbo")) Chat.chat("§cPlease set your SBO key with /sboKey <key>, if you don't have one, get it in our discord.")
            parent.partyCreate(reqs = reqString, note = note, type = partyType)
            parent.closeCpWindow()
        }
        createButton.textObject.setTextScale(parent.getTextScale())
    }

    internal fun addDianaFilter(x1: PositionConstraint, y1: PositionConstraint) {
        parent.filterWindow.constrain {
            x = x1
            y = y1
            width = 15.percent()
            height = 20.percent()
        }.setColor(Color(0, 0, 0, 0))
        parent.filterWindow.setX((parent.filterWindow.getLeft() - parent.filterWindow.getWidth()).pixels())

        parent.filterBox = UIRoundedRectangle(10f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Color(50,50,50,255)) childOf parent.filterWindow
        parent.filterBox.grabWindowFocus()
        parent.filterBox.onMouseClick {
            this.grabWindowFocus()
        }
        parent.filterBox.onFocusLost {
            this@DianaPage.parent.closeFilterWindow()
        }

        val row1 = UIBlock().constrain {
            x = CenterConstraint()
            y = 0.percent()
            width = 100.percent()
            height = (33.33f).percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.filterBox
        val row2 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = (33.33f).percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.filterBox
        val row3 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = (33.33f).percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.filterBox
        val eman9Filter = GuiHandler.Checkbox(
            list = "diana",
            key = "eman9Filter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Color(0, 0, 0, 150),
            checkedColor = Color(0, 110, 250, 255),
            text = "Eman9",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        eman9Filter.create().setChildOf(row1)
        eman9Filter.setBgBoxColor(Color(25, 25, 25, 200))
        eman9Filter.textObject.setTextScale(parent.getTextScale())
        eman9Filter.setOnClick { setFilter() }

        val looting5Filter = GuiHandler.Checkbox(
            list = "diana",
            key = "looting5Filter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Color(0, 0, 0, 150),
            checkedColor = Color(0, 110, 250, 255),
            text = "Looting 5",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        looting5Filter.create().setChildOf(row2)
        looting5Filter.setBgBoxColor(Color(25, 25, 25, 200))
        looting5Filter.textObject.setTextScale(parent.getTextScale())
        looting5Filter.setOnClick { setFilter() }

        val canIjoinFilter = GuiHandler.Checkbox(
            list = "diana",
            key = "canIjoinFilter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Color(0, 0, 0, 150),
            checkedColor = Color(0, 110, 250, 255),
            text = "Can I Join?",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        canIjoinFilter.create().setChildOf(row3)
        canIjoinFilter.setBgBoxColor(Color(25, 25, 25, 200))
        canIjoinFilter.textObject.setTextScale(parent.getTextScale())
        canIjoinFilter.setOnClick { setFilter() }
    }
}