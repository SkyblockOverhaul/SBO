package net.sbo.mod.customizer.gui

import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.switch
import net.sbo.mod.customizer.SboCustomizer
import net.sbo.mod.utils.data.DataManager
import java.util.Locale
import kotlin.math.roundToInt

internal data class TextSizes(val tag: Float, val name: Float)

internal data class SupportersProps(val textSizes: TextSizes, val onTextSizes: (TextSizes) -> Unit, val onChanged: () -> Unit)

internal val Supporters = component<SupportersProps>("CustomizerSupporters") { (textSizes, onTextSizes, onChanged) ->
    val data = DataManager.sboData
    var inTab by useState(data.customizerInTab)
    var inChat by useState(data.customizerInChat)
    var namesInChat by useState(data.customizerNamesInChat)
    var namesTag by useState(data.customizerNamesTag)
    var everywhere by useState(data.customizerReplaceEverywhere)
    var colors by useState(data.customizerAnimationsOfOthers)
    var motion by useState(data.customizerMotionOfOthers)
    var tabAnimations by useState(data.customizerAnimationsInTab)
    var chatAnimations by useState(data.customizerAnimationsInChat)
    var looks by useState(data.customizerLooksOfOthers)
    var sizes by useState(data.customizerSizesOfOthers)

    fun saved(relook: Boolean = false) {
        data.save()
        if (relook) SboCustomizer.relook()
        onChanged()
    }

    div(className = "cz-hint cz-note") { +"Changes how other supporters look on your screen. The preview shows one." }
    section("Where they show", "Their SBO logo and name colors.")
    row("Tab list", "Their logo and name colors in the tab list.") {
        switch(checked = inTab, onChange = { inTab = it.checked; data.customizerInTab = it.checked; saved() })
    }
    row("Chat", "Their logo and name colors when they write in chat.") {
        switch(checked = inChat, onChange = { inChat = it.checked; data.customizerInChat = it.checked; saved() })
    }
    row("Names in messages", "Also color names someone writes in a message, not only the sender.") {
        select(value = namesInChat, onChange = { namesInChat = it.value; data.customizerNamesInChat = it.value; saved() }, className = "cz-select") {
            option(SboCustomizer.NAMES_OFF, "Off")
            option(SboCustomizer.NAMES_OWN, "Only mine")
            option(SboCustomizer.NAMES_ALL, "Everyone")
        }
    }
    if (namesInChat != SboCustomizer.NAMES_OFF) {
        row("Tags on them", "Show their tag in front of those names.") {
            switch(checked = namesTag, onChange = { namesTag = it.checked; data.customizerNamesTag = it.checked; saved() })
        }
        row("Outside of chat", "Also color those names in menus, the scoreboard, titles and signs.") {
            switch(checked = everywhere, onChange = { everywhere = it.checked; data.customizerReplaceEverywhere = it.checked; saved() })
        }
    }
    section("Animations", "Off makes them stand still. Your own customizer always moves.")
    row("Moving colors", "Wave, pulse, chroma and the other color effects.") {
        switch(checked = colors, onChange = { colors = it.checked; data.customizerAnimationsOfOthers = it.checked; saved(relook = true) })
    }
    row("Moving letters", "Wavy, bounce, shake and glitch letters.") {
        switch(checked = motion, onChange = { motion = it.checked; data.customizerMotionOfOthers = it.checked; saved(relook = true) })
    }
    row("Animate tab list", "Moving colors in the tab list. Off also stops your own there.") {
        switch(checked = tabAnimations, onChange = { tabAnimations = it.checked; data.customizerAnimationsInTab = it.checked; saved(relook = true) })
    }
    row("Animate chat", "Whose customizers move in chat. Everyone still follows the two switches above.") {
        select(value = chatAnimations, onChange = { chatAnimations = it.value; data.customizerAnimationsInChat = it.value; saved() }, className = "cz-select") {
            option(SboCustomizer.NAMES_OFF, "Nobody")
            option(SboCustomizer.NAMES_OWN, "Only mine")
            option(SboCustomizer.NAMES_ALL, "Everyone")
        }
    }
    section("Style and size", "Off shows them plain and in normal size.")
    row("Symbols and styles", "Symbols next to their names and bold, italic, underlined or struck text.") {
        switch(checked = looks, onChange = { looks = it.checked; data.customizerLooksOfOthers = it.checked; saved(relook = true) })
    }
    row("Player sizes", "The model size they picked.") {
        switch(checked = sizes, onChange = { sizes = it.checked; data.customizerSizesOfOthers = it.checked; saved() })
    }
    section("Text above heads", "For every supporter, yours too.")
    row("Logo line", "The SBO logo and stat line on top.") {
        textSizeSlider(textSizes.tag) { onTextSizes(textSizes.copy(tag = it)); data.customizerLineSize = it; saved() }
    }
    row("Name line", "The name with rank and level below it.") {
        textSizeSlider(textSizes.name) { onTextSizes(textSizes.copy(name = it)); data.customizerNameSize = it; saved() }
    }
}

/** Snaps to the steps, so the saved value stays a clean number. */
private fun NodeBuilder.textSizeSlider(value: Float, onChange: (Float) -> Unit) {
    div(className = "cz-inline") {
        slider(
            value = value,
            onChange = { v ->
                val size = ((v / SboCustomizer.TEXT_SIZE_STEP).roundToInt() * SboCustomizer.TEXT_SIZE_STEP * 100f).roundToInt() / 100f
                onChange(size.coerceIn(SboCustomizer.MIN_TEXT_SIZE, SboCustomizer.MAX_TEXT_SIZE))
            },
            min = SboCustomizer.MIN_TEXT_SIZE,
            max = SboCustomizer.MAX_TEXT_SIZE,
            step = SboCustomizer.TEXT_SIZE_STEP,
            showValue = true,
            format = { "%.2fx".format(Locale.ROOT, it) },
            className = "cz-slider",
        )
        button(disabled = value == 1f, title = "Normal size", onClick = { onChange(1f) }) { +"Reset" }
    }
}
