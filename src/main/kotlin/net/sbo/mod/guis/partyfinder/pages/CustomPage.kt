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

class CustomPage(private val parent: PartyFinderGUI) {
    internal fun getPartyInfo(info: PartyPlayerStats): String {
        var formattedInfoString = ""
        val formattedInfo = listOf(
            Pair("&9Name: &b", info.name),
            Pair("&9Skyblock Level: ", Helper.matchLvlToColor(info.sbLvl)),
            Pair("&9Uuid: &7", info.uuid),
            Pair("&9Eman9: ", Helper.getNumberColor(info.emanLvl, 9)),
            Pair("&9Clover: ", if (info.clover) "&a✔" else "&c✘"),
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
            if (reqs.mp > 0) {
                sb.append("§bMp: ")
                    .append(if (stats.magicalPower >= reqs.mp) "§a" else "§c")
                    .append(Helper.formatNumber(reqs.mp))
                    .append("§r, ")
            }
            if (reqs.eman9) {
                sb.append(if (stats.eman9) "§aEman9" else "§cEman9")
                    .append("§r, ")
            }

            callback(sb.toString())
        }
    }

    internal fun render() {
        Window.enqueueRenderOperation {
            parent.addPartyListFunctions("Custom Party List", ::createParty)
            parent.updateCurrentPartyList(true)
        }
    }

    private fun createParty() {
        parent.openCpWindow()
        parent.cpWindow.setWidth(20.percent())
        parent.cpWindow.setHeight(54.percent())
        parent.reqsBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint()
            width = 100.percent()
            height = 70.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.cpWindow
        val lvlbox = UIBlock().constrain {
            x = 0.percent()
            y = 5.pixels()
            width = 100.percent()
            height = 18.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("SbLvL").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf lvlbox
        val lvlinput = GuiHandler.TextInput(
            list = "custom",
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
        lvlinput.create().setChildOf(lvlbox)
        lvlinput.onlyNumbers = true
        lvlinput.maxChars = 3
        lvlinput.textInputText.setTextScale(parent.getTextScale())
        if (pfConfigState.inputs.custom.lvl != 0) {
            lvlinput.textInputText.setText(pfConfigState.inputs.custom.lvl.toString())
        }
        val mpbox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("Mp").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf mpbox
        val mpinput = GuiHandler.TextInput(
            list = "custom",
            key = "mp",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        mpinput.create().setChildOf(mpbox)
        mpinput.onlyNumbers = true
        mpinput.maxChars = 4
        mpinput.textInputText.setTextScale(parent.getTextScale())
        if (pfConfigState.inputs.custom.mp != 0) {
            mpinput.textInputText.setText(pfConfigState.inputs.custom.mp.toString())
        }
        val partySizeBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("Party Size").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf partySizeBox
        val partySizeInput = GuiHandler.TextInput(
            list = "custom",
            key = "partySize",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        partySizeInput.create().setChildOf(partySizeBox)
        partySizeInput.onlyNumbers = true
        partySizeInput.maxChars = 2
        partySizeInput.textInputText.setTextScale(parent.getTextScale())
        if (pfConfigState.inputs.custom.partySize != 0) {
            partySizeInput.textInputText.setText(pfConfigState.inputs.custom.partySize.toString())
        }
        val noteBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        UIText("Note").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScale()
        }.setColor(Color(255, 255, 255, 255)) childOf noteBox
        val noteInput = GuiHandler.TextInput(
            list = "custom",
            key = "note",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Color(50, 50, 50, 200),
            textColor = Color(255, 255, 255, 255),
            rounded = true
        )
        noteInput.create().setChildOf(noteBox)
        noteInput.maxChars = 30
        noteInput.textInputText.setTextScale(parent.getTextScale())
        if (pfConfigState.inputs.custom.note.isNotEmpty()) {
            noteInput.textInputText.setText(pfConfigState.inputs.custom.note)
        }
        val eman9Box = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.reqsBox
        val eman9Checkbox = GuiHandler.Checkbox(
            list = "custom",
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
        eman9Checkbox.create().setChildOf(eman9Box)
        eman9Checkbox.setBgBoxColor(Color(50, 50, 50, 200))
        eman9Checkbox.textObject.setTextScale(parent.getTextScale())
        parent.createBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint()
            width = 100.percent()
            height = 18.percent()
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
                "lvl" to pfConfigState.inputs.custom.lvl,
                "mp" to pfConfigState.inputs.custom.mp
            )
            var reqString = ""
            reqsMap.forEach { (key, value) ->
                if (value != 0) {
                    reqString += "$key$value,"
                }
            }
            if (pfConfigState.checkboxes.custom.eman9) reqString += "eman9,"
            val note = pfConfigState.inputs.custom.note
            val partyType = "Custom"
            val partySize = pfConfigState.inputs.custom.partySize
            val sboKey = sboData.sboKey
            if (sboKey.isEmpty() && !sboKey.startsWith("sbo")) {
                parent.closeCpWindow()
                Chat.chat("§cPlease set your SBO key with /sboKey <key>, if you don't have one, get it in our discord.")
                return@setOnClick
            }
            parent.partyCreate(reqs = reqString, note = note, type = partyType, size = partySize)
            parent.closeCpWindow()
        }
        createButton.textObject.setTextScale(parent.getTextScale())
    }

    internal fun addCustomFilter(x1: PositionConstraint, y1: PositionConstraint) {
        parent.filterWindow.constrain {
            x = x1
            y = y1
            width = 15.percent()
            height = 15.percent()
        }.setColor(Color(0, 0, 0, 0))
        parent.filterWindow.setX((parent.filterWindow.getLeft() - parent.filterWindow.getWidth()).pixels())

        parent.filterBox = UIRoundedRectangle(10f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Color(50, 50, 50, 255)) childOf parent.filterWindow
        parent.filterBox.grabWindowFocus()
        parent.filterBox.onMouseClick {
            this.grabWindowFocus()
        }
        parent.filterBox.onFocusLost {
            this@CustomPage.parent.closeFilterWindow()
        }

        val row1 = UIBlock().constrain {
            x = CenterConstraint()
            y = 0.percent()
            width = 100.percent()
            height = 50.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.filterBox
        val row2 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = 50.percent()
        }.setColor(Color(0, 0, 0, 0)) childOf parent.filterBox
        val eman9Filter = GuiHandler.Checkbox(
            list = "custom",
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

        val canIjoinFilter = GuiHandler.Checkbox(
            list = "custom",
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
        canIjoinFilter.create().setChildOf(row2)
        canIjoinFilter.setBgBoxColor(Color(25, 25, 25, 200))
        canIjoinFilter.textObject.setTextScale(parent.getTextScale())
        canIjoinFilter.setOnClick { setFilter() }
    }
}