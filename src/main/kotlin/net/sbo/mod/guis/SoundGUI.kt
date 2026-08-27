package net.sbo.mod.guis

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UKeyboard
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.GuiHandler
import net.sbo.mod.utils.SoundHandler
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.guis.SoundsOpenEvent
import java.awt.Color

class SoundGUI : WindowScreen(ElementaVersion.V10) {
    private lateinit var contentPanel: UIComponent
    private lateinit var scrollComponent: ScrollComponent
    private var guiScale: Int? = null

    private val soundSettings = listOf(
        SoundSetting("Rare Mob Spawn", { SboDataObject.soundSettingsData.rareMobSound }, { SboDataObject.soundSettingsData.rareMobVolume }, { v -> SboDataObject.soundSettingsData.rareMobSound = v }, { v -> SboDataObject.soundSettingsData.rareMobVolume = v }),
        SoundSetting("Rare Mob Low HP", { SboDataObject.soundSettingsData.lowHpSound }, { SboDataObject.soundSettingsData.lowHpVolume }, { v -> SboDataObject.soundSettingsData.lowHpSound = v }, { v -> SboDataObject.soundSettingsData.lowHpVolume = v }),
        SoundSetting("Inquisitor Spawn", { SboDataObject.soundSettingsData.inqSound }, { SboDataObject.soundSettingsData.inqVolume }, { v -> SboDataObject.soundSettingsData.inqSound = v }, { v -> SboDataObject.soundSettingsData.inqVolume = v }),
        SoundSetting("Sphinx Spawn", { SboDataObject.soundSettingsData.sphinxSound }, { SboDataObject.soundSettingsData.sphinxVolume }, { v -> SboDataObject.soundSettingsData.sphinxSound = v }, { v -> SboDataObject.soundSettingsData.sphinxVolume = v }),
        SoundSetting("King Minos Spawn", { SboDataObject.soundSettingsData.kingSound }, { SboDataObject.soundSettingsData.kingVolume }, { v -> SboDataObject.soundSettingsData.kingSound = v }, { v -> SboDataObject.soundSettingsData.kingVolume = v }),
        SoundSetting("Manticore Spawn", { SboDataObject.soundSettingsData.mantiSound }, { SboDataObject.soundSettingsData.mantiVolume }, { v -> SboDataObject.soundSettingsData.mantiSound = v }, { v -> SboDataObject.soundSettingsData.mantiVolume = v }),
        SoundSetting("Cocoon", { SboDataObject.soundSettingsData.cocoonSound }, { SboDataObject.soundSettingsData.cocoonVolume }, { v -> SboDataObject.soundSettingsData.cocoonSound = v }, { v -> SboDataObject.soundSettingsData.cocoonVolume = v }),
        SoundSetting("Burrow Found", { SboDataObject.soundSettingsData.burrowFoundSound }, { SboDataObject.soundSettingsData.burrowVolume }, { v -> SboDataObject.soundSettingsData.burrowFoundSound = v }, { v -> SboDataObject.soundSettingsData.burrowVolume = v }),
        SoundSetting("Chimera Drop", { SboDataObject.soundSettingsData.chimSound }, { SboDataObject.soundSettingsData.chimVolume }, { v -> SboDataObject.soundSettingsData.chimSound = v }, { v -> SboDataObject.soundSettingsData.chimVolume = v }),
        SoundSetting("Brain Food Drop", { SboDataObject.soundSettingsData.bfSound }, { SboDataObject.soundSettingsData.bfVolume }, { v -> SboDataObject.soundSettingsData.bfSound = v }, { v -> SboDataObject.soundSettingsData.bfVolume = v }),
        SoundSetting("Manti-Core Drop", { SboDataObject.soundSettingsData.coreSound }, { SboDataObject.soundSettingsData.coreVolume }, { v -> SboDataObject.soundSettingsData.coreSound = v }, { v -> SboDataObject.soundSettingsData.coreVolume = v }),
        SoundSetting("Fateful Stinger Drop", { SboDataObject.soundSettingsData.stingerSound }, { SboDataObject.soundSettingsData.stingerVolume }, { v -> SboDataObject.soundSettingsData.stingerSound = v }, { v -> SboDataObject.soundSettingsData.stingerVolume = v }),
        SoundSetting("Shimmering Wool Drop", { SboDataObject.soundSettingsData.woolSound }, { SboDataObject.soundSettingsData.woolVolume }, { v -> SboDataObject.soundSettingsData.woolSound = v }, { v -> SboDataObject.soundSettingsData.woolVolume = v }),
        SoundSetting("Minos Relic Drop", { SboDataObject.soundSettingsData.relicSound }, { SboDataObject.soundSettingsData.relicVolume }, { v -> SboDataObject.soundSettingsData.relicSound = v }, { v -> SboDataObject.soundSettingsData.relicVolume = v }),
        SoundSetting("Daedalus Stick Drop", { SboDataObject.soundSettingsData.stickSound }, { SboDataObject.soundSettingsData.stickVolume }, { v -> SboDataObject.soundSettingsData.stickSound = v }, { v -> SboDataObject.soundSettingsData.stickVolume = v }),
        SoundSetting("Misc Drop", { SboDataObject.soundSettingsData.miscDropSound }, { SboDataObject.soundSettingsData.miscDropVolume }, { v -> SboDataObject.soundSettingsData.miscDropSound = v }, { v -> SboDataObject.soundSettingsData.miscDropVolume = v })
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
        SboDataObject.soundSettingsData.save()
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

        soundSettings.forEachIndexed { index, setting ->
            val rowY = index * 70f

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
            GuiHandler.UILine(
                x = 0.pixels,
                y = (rowY + 65).pixels,
                width = 100.percent(),
                height = 1f.pixels,
                color = Color(80, 80, 80, 100)
            ).get().setChildOf(contentPanel)
        }

        contentPanel.constrain {
            height = (soundSettings.size * 75 + 20).pixels
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