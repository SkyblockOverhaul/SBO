package net.sbo.mod.guis.partyfinder.pages

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
import net.sbo.mod.guis.partyfinder.Theme
import net.sbo.mod.partyfinder.PartyFinderManager.hasSboKey
import net.sbo.mod.partyfinder.PartyPlayer.getPartyPlayerStats
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.Reqs
import net.sbo.mod.utils.data.SboDataObject.pfConfigState

class CustomPage(private val parent: PartyFinderGUI) {
    internal fun getPartyInfo(info: PartyPlayerStats): String {
        var formattedInfoString = ""
        val formattedInfo = listOf(
            "&9Name: &b" to info.name,
            "&9Skyblock Level: " to Helper.matchLvlToColor(info.sbLvl),
            "&9Uuid: &7" to info.uuid,
            "&9Eman9: " to Helper.getNumberColor(info.emanLvl, 9),
            "&9Clover: " to if (info.clover) "&a✔" else "&c✘",
            "&9Magical Power: &b" to info.magicalPower,
            "&9Enrichments: &b" to info.enrichments,
            "&9Missing Enrichments: &b" to info.missingEnrichments,
            "&9Warnings: &7" to info.warnings.joinToString(", ")
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
        }.setColor(Theme.TRANSPARENT) childOf parent.cpWindow
        val lvlbox = UIBlock().constrain {
            x = 0.percent()
            y = 5.pixels()
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("SbLvL").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf lvlbox
        val lvlinput = GuiHandler.TextInput(
            list = "custom",
            key = "lvl",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        lvlinput.create().setChildOf(lvlbox)
        lvlinput.onlyNumbers = true
        lvlinput.maxChars = 3
        lvlinput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.custom.lvl != 0) {
            lvlinput.textInputText.setText(pfConfigState.inputs.custom.lvl.toString())
        }
        val mpbox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("Mp").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf mpbox
        val mpinput = GuiHandler.TextInput(
            list = "custom",
            key = "mp",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        mpinput.create().setChildOf(mpbox)
        mpinput.onlyNumbers = true
        mpinput.maxChars = 4
        mpinput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.custom.mp != 0) {
            mpinput.textInputText.setText(pfConfigState.inputs.custom.mp.toString())
        }
        val partySizeBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("Party Size").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf partySizeBox
        val partySizeInput = GuiHandler.TextInput(
            list = "custom",
            key = "partySize",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        partySizeInput.create().setChildOf(partySizeBox)
        partySizeInput.onlyNumbers = true
        partySizeInput.maxChars = 2
        partySizeInput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.custom.partySize != 0) {
            partySizeInput.textInputText.setText(pfConfigState.inputs.custom.partySize.toString())
        }
        val noteBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("Note").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf noteBox
        val noteInput = GuiHandler.TextInput(
            list = "custom",
            key = "note",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        noteInput.create().setChildOf(noteBox)
        noteInput.maxChars = 30
        noteInput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.custom.note.isNotEmpty()) {
            noteInput.textInputText.setText(pfConfigState.inputs.custom.note)
        }
        val eman9Box = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        val eman9Checkbox = GuiHandler.Checkbox(
            list = "custom",
            key = "eman9",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Theme.CHECKBOX_BG,
            checkedColor = Theme.CHECKBOX_CHECKED,
            text = "Eman9",
            rounded = true,
            roundness = 5f
        )
        eman9Checkbox.create().setChildOf(eman9Box)
        eman9Checkbox.setBgBoxColor(Theme.INPUT_BG)
        eman9Checkbox.textObject.setTextScale(parent.getTextScaleOfScaleText())
        parent.createBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint()
            width = 100.percent()
            height = 18.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.cpWindow
        val createButton = GuiHandler.Button(
            text = "Create Party",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 70.percent(),
            height = 60.percent(),
            color = Theme.BUTTON_DEFAULT,
            textColor = Theme.TEXT_PRIMARY,
            parent = parent.createBox,
            rounded = true
        )
        createButton.hoverEffect(Theme.BUTTON_DEFAULT, Theme.BUTTON_HOVER)
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
            if (!hasSboKey()) {
                parent.closeCpWindow()
                return@setOnClick
            }
            parent.partyCreate(reqs = reqString, note = note, type = partyType, size = partySize)
            parent.closeCpWindow()
        }
        createButton.textObject.setTextScale(parent.getTextScaleOfScaleText())
    }

    internal fun addCustomFilter(x1: PositionConstraint, y1: PositionConstraint) {
        parent.filterWindow.constrain {
            x = x1
            y = y1
            width = 15.percent()
            height = 15.percent()
        }.setColor(Theme.TRANSPARENT)
        parent.filterWindow.setX((parent.filterWindow.getLeft() - parent.filterWindow.getWidth()).pixels())

        parent.filterBox = UIRoundedRectangle(10f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.FILTER_BOX_BG) childOf parent.filterWindow
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
        }.setColor(Theme.TRANSPARENT) childOf parent.filterBox
        val row2 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = 50.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.filterBox
        val eman9Filter = GuiHandler.Checkbox(
            list = "custom",
            key = "eman9Filter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Theme.FILTER_TINT,
            checkedColor = Theme.CHECKBOX_CHECKED,
            text = "Eman9",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        eman9Filter.create().setChildOf(row1)
        eman9Filter.setBgBoxColor(Theme.CHECKBOX_FILTER_BG)
        eman9Filter.textObject.setTextScale(parent.getTextScaleOfScaleText())
        eman9Filter.setOnClick { setFilter() }

        val canIjoinFilter = GuiHandler.Checkbox(
            list = "custom",
            key = "canIjoinFilter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Theme.FILTER_TINT,
            checkedColor = Theme.CHECKBOX_CHECKED,
            text = "Can I Join?",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        canIjoinFilter.create().setChildOf(row2)
        canIjoinFilter.setBgBoxColor(Theme.CHECKBOX_FILTER_BG)
        canIjoinFilter.textObject.setTextScale(parent.getTextScaleOfScaleText())
        canIjoinFilter.setOnClick { setFilter() }
    }
}
