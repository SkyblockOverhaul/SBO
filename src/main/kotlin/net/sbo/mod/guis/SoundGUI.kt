package net.sbo.mod.guis

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UKeyboard
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.utils.SoundHandler
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.SoundsOpenEvent
import java.awt.Color

class SoundGUI : WindowScreen(ElementaVersion.V10) {
    private lateinit var contentPanel: UIComponent
    private lateinit var scrollComponent: ScrollComponent
    private var guiScale: Int? = null

    private val soundSettings = listOf(
        listOf(
            SoundSetting("Rare Mob Spawn", { DataManager.soundSettingsData.rareMobSound }, { DataManager.soundSettingsData.rareMobVolume }, { v -> DataManager.soundSettingsData.rareMobSound = v }, { v -> DataManager.soundSettingsData.rareMobVolume = v }),
            SoundSetting("Inquisitor Spawn", { DataManager.soundSettingsData.inqSound }, { DataManager.soundSettingsData.inqVolume }, { v -> DataManager.soundSettingsData.inqSound = v }, { v -> DataManager.soundSettingsData.inqVolume = v }),
            SoundSetting("Sphinx Spawn", { DataManager.soundSettingsData.sphinxSound }, { DataManager.soundSettingsData.sphinxVolume }, { v -> DataManager.soundSettingsData.sphinxSound = v }, { v -> DataManager.soundSettingsData.sphinxVolume = v }),
            SoundSetting("King Minos Spawn", { DataManager.soundSettingsData.kingSound }, { DataManager.soundSettingsData.kingVolume }, { v -> DataManager.soundSettingsData.kingSound = v }, { v -> DataManager.soundSettingsData.kingVolume = v }),
            SoundSetting("Manticore Spawn", { DataManager.soundSettingsData.mantiSound }, { DataManager.soundSettingsData.mantiVolume }, { v -> DataManager.soundSettingsData.mantiSound = v }, { v -> DataManager.soundSettingsData.mantiVolume = v }),
            SoundSetting("Cocoon", { DataManager.soundSettingsData.cocoonSound }, { DataManager.soundSettingsData.cocoonVolume }, { v -> DataManager.soundSettingsData.cocoonSound = v }, { v -> DataManager.soundSettingsData.cocoonVolume = v }),
            SoundSetting("Burrow Found", { DataManager.soundSettingsData.burrowFoundSound }, { DataManager.soundSettingsData.burrowVolume }, { v -> DataManager.soundSettingsData.burrowFoundSound = v }, { v -> DataManager.soundSettingsData.burrowVolume = v }),
        ) to "Spawns",

        listOf(
            SoundSetting("Inquisitor Low HP", { DataManager.soundSettingsData.lowInqHpSound }, { DataManager.soundSettingsData.lowInqHpVoume }, { v -> DataManager.soundSettingsData.lowInqHpSound = v }, { v -> DataManager.soundSettingsData.lowInqHpVoume = v }),
            SoundSetting("Sphinx Low HP", { DataManager.soundSettingsData.lowSphinxHpSound }, { DataManager.soundSettingsData.lowSphinxHpVoume }, { v -> DataManager.soundSettingsData.lowSphinxHpSound = v }, { v -> DataManager.soundSettingsData.lowSphinxHpVoume = v }),
            SoundSetting("King Minos Low HP", { DataManager.soundSettingsData.lowKingHpSound }, { DataManager.soundSettingsData.lowKingHpVoume }, { v -> DataManager.soundSettingsData.lowKingHpSound = v }, { v -> DataManager.soundSettingsData.lowKingHpVoume = v }),
            SoundSetting("Manticore Low HP", { DataManager.soundSettingsData.lowMantiHpSound }, { DataManager.soundSettingsData.lowMantiHpVoume }, { v -> DataManager.soundSettingsData.lowMantiHpSound = v }, { v -> DataManager.soundSettingsData.lowMantiHpVoume = v }),
        ) to "Low HPs",

        listOf(
            SoundSetting("Chimera Drop", { DataManager.soundSettingsData.chimSound }, { DataManager.soundSettingsData.chimVolume }, { v -> DataManager.soundSettingsData.chimSound = v }, { v -> DataManager.soundSettingsData.chimVolume = v }),
            SoundSetting("Brain Food Drop", { DataManager.soundSettingsData.bfSound }, { DataManager.soundSettingsData.bfVolume }, { v -> DataManager.soundSettingsData.bfSound = v }, { v -> DataManager.soundSettingsData.bfVolume = v }),
            SoundSetting("Manti-Core Drop", { DataManager.soundSettingsData.coreSound }, { DataManager.soundSettingsData.coreVolume }, { v -> DataManager.soundSettingsData.coreSound = v }, { v -> DataManager.soundSettingsData.coreVolume = v }),
            SoundSetting("Fateful Stinger Drop", { DataManager.soundSettingsData.stingerSound }, { DataManager.soundSettingsData.stingerVolume }, { v -> DataManager.soundSettingsData.stingerSound = v }, { v -> DataManager.soundSettingsData.stingerVolume = v }),
            SoundSetting("Shimmering Wool Drop", { DataManager.soundSettingsData.woolSound }, { DataManager.soundSettingsData.woolVolume }, { v -> DataManager.soundSettingsData.woolSound = v }, { v -> DataManager.soundSettingsData.woolVolume = v }),
            SoundSetting("Minos Relic Drop", { DataManager.soundSettingsData.relicSound }, { DataManager.soundSettingsData.relicVolume }, { v -> DataManager.soundSettingsData.relicSound = v }, { v -> DataManager.soundSettingsData.relicVolume = v }),
            SoundSetting("Daedalus Stick Drop", { DataManager.soundSettingsData.stickSound }, { DataManager.soundSettingsData.stickVolume }, { v -> DataManager.soundSettingsData.stickSound = v }, { v -> DataManager.soundSettingsData.stickVolume = v }),
            SoundSetting("Misc Drop", { DataManager.soundSettingsData.miscDropSound }, { DataManager.soundSettingsData.miscDropVolume }, { v -> DataManager.soundSettingsData.miscDropSound = v }, { v -> DataManager.soundSettingsData.miscDropVolume = v })
        ) to "Drops"
    )

    private data class SoundSetting(
        val label: String,
        val soundGetter: () -> String,
        val volumeGetter: () -> Float,
        val soundSetter: (String) -> Unit,
        val volumeSetter: (Float) -> Unit
    )

    init {
        create()
    }

    companion object {
        var instance: SoundGUI? = null

        @SboEvent
        fun onSoundOpenEvent(event: SoundsOpenEvent) {
            instance?.onScreenOpen()
        }
    }

    private fun create() {
        instance = this
        renderGui()
        renderSettings()
        window.onKeyType { typedChar, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.schedule {
                    displayScreen(null)
                }
            }
        }
    }

    private fun onScreenOpen() {
        if (mc.options.guiScale().get() == 2) return
        guiScale = mc.options.guiScale().get()
        mc.options.guiScale().set(2) // this is a workaround for text scaling
    }

    override fun onScreenClose() {
        super.onScreenClose()
        DataManager.soundSettingsData.save()
        if (mc.options.guiScale().get() != 2 || guiScale == null) return
        mc.options.guiScale().set(guiScale!!) // restore original gui scale
        guiScale = null
    }


    private fun renderGui() {
        UIBlock().constrain {
            width = 100.percent
            height = 100.percent
        }.setColor(Color(0, 0, 0, 200)) childOf window

        val container = UIBlock().constrain {
            x = CenterConstraint()
            y = 10.percent
            width = 70.percent
            height = 80.percent
        } childOf window
        container.setColor(Color(0, 0, 0, 0))

        UIText("SBO Sound Settings").constrain {
            x = CenterConstraint()
            y = (container.getTop() - 30).pixels
            textScale = 1.5.pixels
        }.setColor(Color.WHITE) childOf window

        UIText("Master Volume & Open Sound Folder is found in /sbo -> Customization -> Sounds").constrain {
            x = CenterConstraint()
            y = (container.getTop() - 10).pixels
            textScale = 0.8.pixels
        }.setColor(Color.GRAY) childOf window

        scrollComponent = ScrollComponent().constrain {
            x = 0.pixels
            y = 0.pixels
            width = FillConstraint()
            height = FillConstraint()
        } childOf container
        scrollComponent.setColor(Color(0, 0, 0, 0))

        contentPanel = UIBlock().constrain {
            x = 0.pixels
            y = 0.pixels
            width = FillConstraint()
            height = ChildBasedSizeConstraint()
        }.setColor(Color(0, 0, 0, 0)) childOf scrollComponent
    }

    private fun renderSettings() {
        contentPanel.clearChildren()

        val availableSounds = SoundHandler.getAvailableSoundsWithExt()
        val options = listOf("(None)") + availableSounds

        var rowY = 0

        soundSettings.forEachIndexed { ind, (group, groupName) ->
            val addedY = if (ind == 0) 10 else 5

            val groupTitle = UIBlock().constrain {
                x = 10.pixel
                y = (rowY + addedY).pixels
                width = 100.percent()
                height = 25f.pixels
                color = contentPanel.getColor().toConstraint()
            } childOf contentPanel

            UIText(groupName).constrain {
                x = CenterConstraint()
                y = 10.pixels
                textScale = 1.1.pixels
            }.setColor(Color.WHITE) childOf groupTitle

            UILine(
                x = 0.pixels,
                y = 25.pixels,
                width = 100.percent(),
                height = 1f.pixels,
                color = Color.WHITE
            ).get().setChildOf(groupTitle)

            rowY += 45 + addedY

            group.forEachIndexed { index, setting ->
                // Current values
                val currentSound = setting.soundGetter()
                val currentVolume = setting.volumeGetter()

                // Label row (top of this setting)
                val controlY = rowY + 35f  // Center of the control area

                // Label
                UIText(setting.label).constrain {
                    x = 10.pixels
                    y = rowY.pixels
                    textScale = 1.1.pixels
                }.setColor(Color.WHITE) childOf contentPanel

                // Current value text
                UIText("Sound: $currentSound | Volume: ${(currentVolume * 100).toInt()}%").constrain {
                    x = 10.pixels
                    y = (rowY + 18).pixels
                    textScale = 0.8.pixels
                }.setColor(Color.GRAY) childOf contentPanel

                // Sound dropdown button
                val dropdownOutline = UIRoundedRectangle(5f).constrain {
                    x = 10.pixels
                    y = controlY.pixels
                    width = 160.pixels
                    height = 28.pixels
                }.setColor(Color.WHITE)

                val dropdownBg = UIBlock().constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    width = 156.pixels
                    height = 24.pixels
                }.setColor(Color(30, 30, 30))

                UIText(currentSound.ifEmpty { "(None)" }).constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 0.9.pixels
                }.setColor(Color.WHITE) childOf dropdownBg

                dropdownOutline childOf contentPanel
                dropdownBg childOf dropdownOutline

                dropdownOutline.onMouseClick {
                    showSoundPicker(setting.label, options) { selected ->
                        val value = if (selected == "(None)") "" else selected
                        setting.soundSetter(value)
                        renderSettings()
                    }
                }

                // Volume slider - next to dropdown
                val sliderOutline = UIRoundedRectangle(5f).constrain {
                    x = 180.pixels
                    y = controlY.pixels
                    width = 160.pixels
                    height = 28.pixels
                }.setColor(Color.WHITE)

                val sliderBg = UIBlock().constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    width = 156.pixels
                    height = 24.pixels
                }.setColor(Color(30, 30, 30))

                UIBlock().constrain {
                    x = 2.pixels
                    y = CenterConstraint()
                    width = (currentVolume * 152).pixels
                    height = 20.pixels
                }.setColor(Color(100, 149, 237)) childOf sliderBg

                UIText("${(currentVolume * 100).toInt()}%").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 0.9.pixels
                }.setColor(Color.WHITE) childOf sliderBg

                sliderOutline childOf contentPanel
                sliderBg childOf sliderOutline

                // Click to set volume
                sliderOutline.onMouseClick { clickEvent ->
                    val relativeX = clickEvent.relativeX
                    if (relativeX in 0f..160f) {
                        val newVolume = (relativeX / 160f).coerceIn(0f, 1f)
                        setting.volumeSetter(newVolume)
                        renderSettings()
                    }
                }

                // +/- buttons - next to slider
                val minusBtn = UIRoundedRectangle(5f).constrain {
                    x = 350.pixels
                    y = controlY.pixels
                    width = 24.pixels
                    height = 28.pixels
                }.setColor(Color(80, 80, 80))

                UIText("-").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 1.2.pixels
                }.setColor(Color.WHITE) childOf minusBtn
                minusBtn childOf contentPanel

                val plusBtn = UIRoundedRectangle(5f).constrain {
                    x = 378.pixels
                    y = controlY.pixels
                    width = 24.pixels
                    height = 28.pixels
                }.setColor(Color(80, 80, 80))

                UIText("+").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 1.2.pixels
                }.setColor(Color.WHITE) childOf plusBtn
                plusBtn childOf contentPanel

                minusBtn.onMouseClick {
                    val newVol = (setting.volumeGetter() - 0.05f).coerceIn(0f, 1f)
                    setting.volumeSetter(newVol)
                    renderSettings()
                }

                plusBtn.onMouseClick {
                    val newVol = (setting.volumeGetter() + 0.05f).coerceIn(0f, 1f)
                    setting.volumeSetter(newVol)
                    renderSettings()
                }

                // Test button
                val testOutline = UIRoundedRectangle(5f).constrain {
                    x = 415.pixels
                    y = controlY.pixels
                    width = 60.pixels
                    height = 28.pixels
                }.setColor(Color(0, 200, 0))

                val testBg = UIBlock().constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    width = 56.pixels
                    height = 24.pixels
                }.setColor(Color(0, 100, 0))

                UIText("Test").constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                    textScale = 0.9.pixels
                }.setColor(Color.WHITE) childOf testBg

                testOutline childOf contentPanel
                testBg childOf testOutline

                testOutline.onMouseClick {
                    val sound = setting.soundGetter()
                    if (sound.isNotEmpty()) {
                        SoundHandler.playCustomSound(sound, volume = setting.volumeGetter())
                    }
                }

                testOutline.onMouseEnter { testOutline.setColor(Color(0, 255, 0)) }
                testOutline.onMouseLeave { testOutline.setColor(Color(0, 200, 0)) }
                dropdownOutline.onMouseEnter { dropdownOutline.setColor(Color.CYAN) }
                dropdownOutline.onMouseLeave { dropdownOutline.setColor(Color.WHITE) }
                sliderOutline.onMouseEnter { sliderOutline.setColor(Color.CYAN) }
                sliderOutline.onMouseLeave { sliderOutline.setColor(Color.WHITE) }

                // Separator line between settings
                if (index != group.lastIndex) UILine(
                    x = 0.pixels,
                    y = (rowY + 66).pixels,
                    width = 100.percent(),
                    height = 1f.pixels,
                    color = Color(80, 80, 80, 100)
                ).get().setChildOf(contentPanel)

                rowY += if (index != group.lastIndex) 70 else 85
            }
        }

        contentPanel.constrain {
            height = (rowY + 25).pixels
        }
    }


    private fun showSoundPicker(title: String, options: List<String>, onSelect: (String) -> Unit) {
        // Backdrop to block clicks on background
        val backdrop = UIBlock().constrain {
            x = 0.pixels
            y = 0.pixels
            width = 100.percent
            height = 100.percent
        }.setColor(Color(0, 0, 0, 150))
        backdrop.childOf(window)

        val picker = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 300.pixels
            height = 350.pixels
        }.setColor(Color(40, 40, 40))
        picker.childOf(window)

        // Title
        UIText("Select $title").constrain {
            x = CenterConstraint()
            y = 15.pixels
            textScale = 1.1.pixels
        }.setColor(Color.WHITE) childOf picker

        // Close button
        val closeBtn = UIRoundedRectangle(5f).constrain {
            x = (picker.getRight() - 35).pixels
            y = 15.pixels
            width = 25.pixels
            height = 25.pixels
        }.setColor(Color.RED)

        UIText("X").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        }.setColor(Color.WHITE) childOf closeBtn

        closeBtn.onMouseClick {
            picker.hide()
            backdrop.hide()
        }

        // Scroll component - use fixed sizes to avoid circular constraints
        val scroll = ScrollComponent().constrain {
            x = 10.pixels
            y = 50.pixels
            width = 280.pixels
            height = 290.pixels
        } childOf picker
        scroll.setColor(Color(0, 0, 0, 0))

        // Option buttons
        options.forEachIndexed { index, option ->
            val btn = UIRoundedRectangle(5f).constrain {
                x = 0.pixels
                y = (index * 30).pixels
                width = 260.pixels
                height = 28.pixels
            }.setColor(Color(60, 60, 60))

            UIText(option).constrain {
                x = 10.pixels
                y = CenterConstraint()
                textScale = 0.9.pixels
            }.setColor(Color.WHITE) childOf btn

            btn childOf scroll
            btn.onMouseClick {
                onSelect(option)
                picker.hide()
                backdrop.hide()
            }
            btn.onMouseEnter { btn.setColor(Color(80, 80, 80)) }
            btn.onMouseLeave { btn.setColor(Color(60, 60, 60)) }
        }
    }
}