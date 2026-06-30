package net.sbo.mod.guis.partyfinder

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.events.UIClickEvent
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.pfConfigState
import org.lwjgl.glfw.GLFW
import java.awt.Color

object GuiHandler {

    fun addHoverEffect(
        uiObject: UIComponent,
        baseColor: Color,
        hoverColor: Color = Color(50, 50, 50, 200)
    ) {
        uiObject.onMouseEnter {
            uiObject.setColor(hoverColor)
        }.onMouseLeave {
            uiObject.setColor(baseColor)
        }
    }

    class UILine(
        private val x: PositionConstraint,
        private val y: PositionConstraint,
        private val width: SizeConstraint,
        private val height: SizeConstraint,
        color: Color,
        parent: UIComponent? = null,
        rounded: Boolean = false,
        roundness: Float = 5f
    ) {
        private val uiObject: UIComponent = if (rounded) UIRoundedRectangle(roundness) else UIBlock()

        fun get(): UIComponent = uiObject

        init {
            uiObject.constrain {
                this.x = this@UILine.x
                this.y = this@UILine.y
                this.width = this@UILine.width
                this.height = this@UILine.height
            }.setColor(color)

            parent?.let { uiObject.childOf(it) }
        }
    }

    class Button(
        text: String,
        private val x: PositionConstraint,
        private val y: PositionConstraint,
        private val width: SizeConstraint,
        private val height: SizeConstraint,
        private val color: Color,
        private val textColor: Color? = null,
        outline: Effect? = null,
        parent: UIComponent? = null,
        rounded: Boolean = false,
        wrapped: Boolean = false,
    ) {
        val uiObject: UIComponent = if (rounded) UIRoundedRectangle(10f) else UIBlock()
        val textObject: UIComponent = if (wrapped) UIWrappedText(text) else UIText(text)

        fun get(): UIComponent = uiObject

        fun hoverEffect(baseColor: Color = color, hoverColor: Color = Color(50, 50, 50, 200)): Button {
            addHoverEffect(uiObject, baseColor, hoverColor)
            return this
        }

        fun textHoverEffect(
            baseColor: Color = textColor ?: Color.WHITE,
            hoverColor: Color = Color(50, 50, 50, 200),
            comp: UIComponent = textObject
        ): Button {
            comp.onMouseEnter { textObject.setColor(hoverColor) }
                .onMouseLeave { textObject.setColor(baseColor) }
            return this
        }

        fun setOnClick(action: () -> Unit): Button {
            uiObject.onMouseClick { event ->
                event.stopPropagation()
                action()
            }
            return this
        }

        fun setTextOnClick(action: () -> Unit): Button {
            textObject.onMouseClick { event ->
                event.stopPropagation()
                action()
            }
            return this
        }

        init {
            uiObject.constrain {
                this.x = this@Button.x
                this.y = this@Button.y
                this.width = this@Button.width
                this.height = this@Button.height
            }.setColor(color)

            outline?.let { uiObject.enableEffect(it) }
            parent?.let { uiObject.childOf(it) }
            textObject.constrain {
                this.x = CenterConstraint()
                this.y = CenterConstraint()
            }.childOf(uiObject)

            textColor?.let { textObject.setColor(it) }
        }
    }

    class Checkbox(
        private val list: String,
        private val key: String,
        private val x: PositionConstraint,
        private val y: PositionConstraint,
        private val width: SizeConstraint,
        private val height: SizeConstraint,
        private val color: Color,
        private val checkedColor: Color,
        private val text: String = "",
        rounded: Boolean = false,
        roundness: Float = 10f,
        private val filter: Boolean = false
    ) {
        private lateinit var onClick: () -> Unit

        private var checked: Boolean = if (filter) {
            when (list) {
                "diana" -> when (key) {
                    "eman9Filter" -> pfConfigState.filters.diana.eman9Filter
                    "looting5Filter" -> pfConfigState.filters.diana.looting5Filter
                    "canIjoinFilter" -> pfConfigState.filters.diana.canIjoinFilter
                    else -> false // Default case if key is not found
                }

                "custom" -> when (key) {
                    "eman9Filter" -> pfConfigState.filters.custom.eman9Filter
                    "canIjoinFilter" -> pfConfigState.filters.custom.canIjoinFilter
                    else -> false // Default case if key is not found
                }

                else -> false // Default case if list is not found
            }
        } else {
            when (list) {
                "diana" -> when (key) {
                    "eman9" -> pfConfigState.checkboxes.diana.eman9
                    "looting5" -> pfConfigState.checkboxes.diana.looting5
                    else -> false
                }

                "custom" -> when (key) {
                    "eman9" -> pfConfigState.checkboxes.custom.eman9
                    else -> false
                }

                else -> false
            }
        }

        private val bgbox = if (rounded) UIRoundedRectangle(roundness) else UIBlock()
        private val checkbox = if (rounded) UIRoundedRectangle(roundness) else UIBlock()
        internal lateinit var textObject: UIText

        fun setBgBoxColor(color: Color): Checkbox {
            bgbox.setColor(color)
            return this
        }

        fun setOnClick(callback: () -> Unit): Checkbox {
            onClick = callback
            return this
        }

        fun create(): UIComponent {
            bgbox.constrain {
                this.x = this@Checkbox.x
                this.y = this@Checkbox.y
                this.width = this@Checkbox.width
                this.height = this@Checkbox.height
            }.setColor(Color(0, 0, 0, 0))

            val groupContainer = UIBlock().constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = ChildBasedSizeConstraint()
                height = ChildBasedSizeConstraint()
            }.setColor(Color(0, 0, 0, 0)) childOf bgbox

            textObject = UIText(text).constrain {
                x = 0.pixels
                y = CenterConstraint()
            } childOf groupContainer
            textObject.setColor(Color(255, 255, 255, 255))

            checkbox.constrain {
                x = SiblingConstraint(5f)
                y = CenterConstraint()
                width = 16.pixels()
                height = 16.pixels()
            }.setColor(if (checked) checkedColor else color) childOf groupContainer

            checkbox.onMouseClick {
                checked = !checked
                checkbox.setColor(if (checked) checkedColor else color)
                SboDataObject.updatePfConfigState(if (filter) "filters" else "checkboxes", list, key, checked)
                if (this@Checkbox::onClick.isInitialized) {
                    this@Checkbox.onClick()
                }
            }

            return bgbox
        }
    }

    class TextInput(
        private val list: String,
        private val key: String,
        private val x: PositionConstraint,
        private val y: PositionConstraint,
        private val width: SizeConstraint,
        private val height: SizeConstraint,
        private val inputWidth: SizeConstraint,
        private val color: Color,
        private val textColor: Color,
        rounded: Boolean = false,
        roundness: Float = 5f
    ) {
        internal var onlyNumbers = false
        private var onlyText = false
        private var lastValidText = getValue()
        internal var maxChars = 0

        internal var text = ""
        private var textSet = false
        private val textInput = if (rounded) UIRoundedRectangle(roundness) else UIBlock()
        internal val textInputText = UITextInput("", true)

        fun getValue(): String {
            return when (list) {
                "custom" -> {
                    val custom = pfConfigState.inputs.custom
                    when (key) {
                        "lvl" -> custom.lvl.toString()
                        "mp" -> custom.mp.toString()
                        "partySize" -> custom.partySize.toString()
                        "note" -> custom.note
                        else -> ""
                    }
                }
                "diana" -> {
                    val diana = pfConfigState.inputs.diana
                    when (key) {
                        "kills" -> diana.kills.toString()
                        "lvl" -> diana.lvl.toString()
                        "note" -> diana.note
                        else -> ""
                    }
                }
                else -> ""
            }
        }

        fun create(): UIComponent {
            textInput.constrain {
                x = this@TextInput.x
                y = this@TextInput.y
                width = this@TextInput.width
                height = this@TextInput.height
            }.setColor(color)

            textInputText.constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = inputWidth
                height = 10.pixels
            }.setColor(textColor) childOf textInput

            textInputText.onFocusLost {
                if (text.isNotEmpty()) return@onFocusLost
            }

            val onClick: UIComponent.(UIClickEvent) -> Unit = {
                if (!textSet) {
                    text = getValue().ifEmpty {
                        textInputText.getText()
                    }
                    textInputText.setText(text)
                    textSet = true
                }
                textInputText.grabWindowFocus()
                textInputText.focus()
            }

            textInput.onMouseClick(onClick)
            textInputText.onMouseClick(onClick)

            textInputText.onKeyType { typedChar, keyCode ->
                if (maxChars > 0 && textInputText.getText().length > maxChars && keyCode != GLFW.GLFW_KEY_BACKSPACE && keyCode != GLFW.GLFW_KEY_DELETE) {
                    textInputText.setText(lastValidText)
                    return@onKeyType
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                    text = textInputText.getText()
                    lastValidText = text
                    SboDataObject.updatePfConfigState("textInputTexts", list, key, text)
                    return@onKeyType
                }
                if (onlyNumbers && !typedChar.isDigit()) {
                    textInputText.setText(lastValidText)
                    return@onKeyType
                }
                if (onlyText && !typedChar.isLetterOrDigit() && typedChar != ' ') {
                    textInputText.setText(lastValidText)
                    return@onKeyType
                }
                text = textInputText.getText()
                lastValidText = text
                SboDataObject.updatePfConfigState("inputs", list, key, text)
            }
            return textInput
        }
    }
}