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


class DianaPage(private val parent: PartyFinderGUI) {
    internal fun getPartyInfo(info: PartyPlayerStats): String {
        var formattedInfoString = ""
        val formattedInfo = listOf(
            "&9Name: &b" to info.name,
            "&9Skyblock Level: " to Helper.matchLvlToColor(info.sbLvl),
            "&9Uuid: &7" to info.uuid,
            "&9Eman9: " to Helper.getNumberColor(info.emanLvl, 9),
            "&9Clover: " to if (info.clover) "&a✔" else "&c✘",
            "&9Looting 5: " to Helper.getNumberColor(info.daxeLootingLvl, 5),
            "&9Chimera: " to Helper.getNumberColor(info.daxeChimLvl, 5),
            "&9Griffin Item: " to Helper.getGriffinItemColor(info.griffinItem?.content),
            "&9Griffin Rarity: " to Helper.getRarity(info.griffinRarity),
            "&9Diana Kills: " to Helper.matchDianaKillsToColor(info.mythosKills),
            "&9Leaderboard: &b#" to info.killLeaderboard,
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
        }.setColor(Theme.TRANSPARENT) childOf parent.cpWindow
        val lvlbox = UIBlock().constrain {
            x = 0.percent()
            y = 5.percent()
            width = 100.percent()
            height = 23.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("SbLvL").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf lvlbox
        val lvlInput = GuiHandler.TextInput(
            list = "diana",
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
        lvlInput.create().setChildOf(lvlbox)
        lvlInput.onlyNumbers = true
        lvlInput.maxChars = 3
        lvlInput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.diana.lvl > 0) {
            lvlInput.textInputText.setText(pfConfigState.inputs.diana.lvl.toString())
        }

        val killsBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 23.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("Kills").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf killsBox
        val killsInput = GuiHandler.TextInput(
            list = "diana",
            key = "kills",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 60.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        killsInput.create().setChildOf(killsBox)
        killsInput.onlyNumbers = true
        killsInput.maxChars = 6
        killsInput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.diana.kills > 0) {
            killsInput.textInputText.setText(pfConfigState.inputs.diana.kills.toString())
        }

        val noteBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 23.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        UIText("Note ").constrain {
            x = 5.percent()
            y = SiblingConstraint(5f)
            textScale = parent.getTextScaleOfScaleText()
        }.setColor(Theme.TEXT_PRIMARY) childOf noteBox
        val noteInput = GuiHandler.TextInput(
            list = "diana",
            key = "note",
            x = CenterConstraint(),
            y = SiblingConstraint(5f),
            width = 90.percent(),
            height = 50.percent(),
            inputWidth = 90.percent(),
            color = Theme.INPUT_BG,
            textColor = Theme.INPUT_TEXT,
            rounded = true
        )
        noteInput.create().setChildOf(noteBox)
        noteInput.maxChars = 30
        noteInput.textInputText.setTextScale(parent.getTextScaleOfScaleText())
        if (pfConfigState.inputs.diana.note.isNotEmpty()) {
            noteInput.textInputText.setText(pfConfigState.inputs.diana.note)
        }

        val l5e9box = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 20.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.reqsBox
        val eman9box = UIBlock().constrain {
            x = 0.percent()
            y = 0.percent()
            width = 50.percent()
            height = 100.percent()
        }.setColor(Theme.TRANSPARENT) childOf l5e9box
        val eman9checkbox = GuiHandler.Checkbox(
            list = "diana",
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
        eman9checkbox.create().setChildOf(eman9box)
        eman9checkbox.setBgBoxColor(Theme.INPUT_BG)
        eman9checkbox.textObject.setTextScale(parent.getTextScaleOfScaleText())
        val looting5box = UIBlock().constrain {
            x = SiblingConstraint()
            y = 0.percent()
            width = 50.percent()
            height = 100.percent()
        }.setColor(Theme.TRANSPARENT) childOf l5e9box
        val looting5checkbox = GuiHandler.Checkbox(
            list = "diana",
            key = "looting5",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Theme.CHECKBOX_BG,
            checkedColor = Theme.CHECKBOX_CHECKED,
            text = "Looting5",
            rounded = true,
            roundness = 5f
        )
        looting5checkbox.create().setChildOf(looting5box)
        looting5checkbox.setBgBoxColor(Theme.INPUT_BG)
        looting5checkbox.textObject.setTextScale(parent.getTextScaleOfScaleText())

        parent.createBox = UIBlock().constrain {
            x = 0.percent()
            y = SiblingConstraint(5f)
            width = 100.percent()
            height = 20.percent()
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
            val reqs = Reqs(
                lvl = pfConfigState.inputs.diana.lvl,
                kills = pfConfigState.inputs.diana.kills,
                eman9 = pfConfigState.checkboxes.diana.eman9,
                looting5 = pfConfigState.checkboxes.diana.looting5
            )
            val note = pfConfigState.inputs.diana.note
            val partyType = "Diana"
            hasSboKey()
            parent.partyCreate(reqs = reqs, note = note, type = partyType)
            parent.closeCpWindow()
        }
        createButton.textObject.setTextScale(parent.getTextScaleOfScaleText())
    }

    internal fun addDianaFilter(x1: PositionConstraint, y1: PositionConstraint) {
        parent.filterWindow.constrain {
            x = x1
            y = y1
            width = 15.percent()
            height = 20.percent()
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
            this@DianaPage.parent.closeFilterWindow()
        }

        val row1 = UIBlock().constrain {
            x = CenterConstraint()
            y = 0.percent()
            width = 100.percent()
            height = 33.33f.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.filterBox
        val row2 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = 33.33f.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.filterBox
        val row3 = UIBlock().constrain {
            x = CenterConstraint()
            y = SiblingConstraint()
            width = 100.percent()
            height = 33.33f.percent()
        }.setColor(Theme.TRANSPARENT) childOf parent.filterBox
        val eman9Filter = GuiHandler.Checkbox(
            list = "diana",
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

        val looting5Filter = GuiHandler.Checkbox(
            list = "diana",
            key = "looting5Filter",
            x = CenterConstraint(),
            y = CenterConstraint(),
            width = 80.percent(),
            height = 80.percent(),
            color = Theme.FILTER_TINT,
            checkedColor = Theme.CHECKBOX_CHECKED,
            text = "Looting 5",
            rounded = true,
            roundness = 5f,
            filter = true
        )
        looting5Filter.create().setChildOf(row2)
        looting5Filter.setBgBoxColor(Theme.CHECKBOX_FILTER_BG)
        looting5Filter.textObject.setTextScale(parent.getTextScaleOfScaleText())
        looting5Filter.setOnClick { setFilter() }

        val canIjoinFilter = GuiHandler.Checkbox(
            list = "diana",
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
        canIjoinFilter.create().setChildOf(row3)
        canIjoinFilter.setBgBoxColor(Theme.CHECKBOX_FILTER_BG)
        canIjoinFilter.textObject.setTextScale(parent.getTextScaleOfScaleText())
        canIjoinFilter.setOnClick { setFilter() }
    }
}
