package net.sbo.mod.guis.customizer

import net.minecraft.network.chat.Style
import net.sbo.guilib.core.dom.component
import net.sbo.guilib.core.dsl.NodeBuilder
import net.sbo.guilib.core.dsl.button
import net.sbo.guilib.core.dsl.div
import net.sbo.guilib.core.dsl.select
import net.sbo.guilib.core.dsl.slider
import net.sbo.guilib.core.dsl.switch
import net.sbo.mod.general.SboBadges
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.DataManager
import java.util.Locale
import kotlin.math.roundToInt

internal data class TextSizes(val tag: Float, val name: Float)

internal data class SupportersProps(val textSizes: TextSizes, val onTextSizes: (TextSizes) -> Unit, val onChanged: () -> Unit)

internal val Supporters = component<SupportersProps>("CustomizerSupporters") { (textSizes, onTextSizes, onChanged) ->
    val data = DataManager.sboData
    var inTab by useState(data.badgesInTab)
    var inChat by useState(data.badgesInChat)
    var namesInChat by useState(data.badgeNamesInChat)
    var namesTag by useState(data.badgeNamesTag)
    var everywhere by useState(data.badgeReplaceEverywhere)
    var colors by useState(data.badgeAnimationsOfOthers)
    var motion by useState(data.badgeMotionOfOthers)
    var tabAnimations by useState(data.badgeAnimationsInTab)
    var chatAnimations by useState(data.badgeAnimationsInChat)
    var looks by useState(data.badgeLooksOfOthers)
    var sizes by useState(data.badgeSizesOfOthers)

    fun saved(relook: Boolean = false) {
        data.save()
        if (relook) SboBadges.relook()
        onChanged()
    }

    div(className = "cz-hint cz-note") { +"Changes how other supporters look on your screen. The preview shows one." }
    section("Where they show", "Their SBO logo and name colors.")
    row("Tab list", "Their logo and name colors in the tab list.") {
        switch(checked = inTab, onChange = { inTab = it.checked; data.badgesInTab = it.checked; saved() })
    }
    row("Chat", "Their logo and name colors when they write in chat.") {
        switch(checked = inChat, onChange = { inChat = it.checked; data.badgesInChat = it.checked; saved() })
    }
    row("Names in messages", "Also color names someone writes in a message, not only the sender.") {
        select(value = namesInChat, onChange = { namesInChat = it.value; data.badgeNamesInChat = it.value; saved() }, className = "cz-select") {
            option(SboBadges.NAMES_OFF, "Off")
            option(SboBadges.NAMES_OWN, "Only mine")
            option(SboBadges.NAMES_ALL, "Everyone")
        }
    }
    if (namesInChat != SboBadges.NAMES_OFF) {
        row("Team tags on them", "Show the team tag, like [SBO Dev], in front of those names.") {
            switch(checked = namesTag, onChange = { namesTag = it.checked; data.badgeNamesTag = it.checked; saved() })
        }
        row("Outside of chat", "Also color those names in menus, the scoreboard, titles and signs.") {
            switch(checked = everywhere, onChange = { everywhere = it.checked; data.badgeReplaceEverywhere = it.checked; saved() })
        }
    }
    section("Animations", "Off makes them stand still. Your own badge always moves.")
    row("Moving colors", "Wave, pulse, chroma and the other color effects.") {
        switch(checked = colors, onChange = { colors = it.checked; data.badgeAnimationsOfOthers = it.checked; saved(relook = true) })
    }
    row("Moving letters", "Wavy, bounce, shake and glitch letters.") {
        switch(checked = motion, onChange = { motion = it.checked; data.badgeMotionOfOthers = it.checked; saved(relook = true) })
    }
    row("Animate tab list", "Moving colors in the tab list. Off also stops your own there.") {
        switch(checked = tabAnimations, onChange = { tabAnimations = it.checked; data.badgeAnimationsInTab = it.checked; saved(relook = true) })
    }
    row("Animate chat", "Whose badges move in chat. Everyone still follows the two switches above.") {
        select(value = chatAnimations, onChange = { chatAnimations = it.value; data.badgeAnimationsInChat = it.value; saved() }, className = "cz-select") {
            option(SboBadges.NAMES_OFF, "Nobody")
            option(SboBadges.NAMES_OWN, "Only mine")
            option(SboBadges.NAMES_ALL, "Everyone")
        }
    }
    section("Style and size", "Off shows them plain and in normal size.")
    row("Symbols and styles", "Symbols next to their names and bold, italic, underlined or struck text.") {
        switch(checked = looks, onChange = { looks = it.checked; data.badgeLooksOfOthers = it.checked; saved(relook = true) })
    }
    row("Player sizes", "The model size they picked.") {
        switch(checked = sizes, onChange = { sizes = it.checked; data.badgeSizesOfOthers = it.checked; saved() })
    }
    section("Text above heads", "For every supporter and the team, yours too.")
    row("Logo line", "The SBO logo and stat line on top.") {
        textSizeSlider(textSizes.tag) { onTextSizes(textSizes.copy(tag = it)); data.badgeLineSize = it; saved() }
    }
    row("Name line", "The name with rank and level below it.") {
        textSizeSlider(textSizes.name) { onTextSizes(textSizes.copy(name = it)); data.badgeNameSize = it; saved() }
    }
}

private fun NodeBuilder.textSizeSlider(value: Float, onChange: (Float) -> Unit) {
    div(className = "cz-inline") {
        slider(
            value = value,
            onChange = { v ->
                // On the steps, so the saved value stays a clean number
                val size = ((v / SboBadges.TEXT_SIZE_STEP).roundToInt() * SboBadges.TEXT_SIZE_STEP * 100f).roundToInt() / 100f
                onChange(size.coerceIn(SboBadges.MIN_TEXT_SIZE, SboBadges.MAX_TEXT_SIZE))
            },
            min = SboBadges.MIN_TEXT_SIZE,
            max = SboBadges.MAX_TEXT_SIZE,
            step = SboBadges.TEXT_SIZE_STEP,
            showValue = true,
            format = { "%.2fx".format(Locale.ROOT, it) },
            className = "cz-slider",
        )
        button(disabled = value == 1f, title = "Normal size", onClick = { onChange(1f) }) { +"Reset" }
    }
}
