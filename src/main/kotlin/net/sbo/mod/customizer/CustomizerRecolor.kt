package net.sbo.mod.customizer

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.sbo.mod.customizer.CustomizerEffects.Segment
import net.sbo.mod.customizer.CustomizerEffects.colorAt
import net.sbo.mod.customizer.CustomizerEffects.isStill
import net.sbo.mod.customizer.CustomizerEffects.moved
import net.sbo.mod.customizer.CustomizerEffects.paints
import net.sbo.mod.customizer.CustomizerEffects.parseHex
import net.sbo.mod.customizer.SboCustomizer.animates
import net.sbo.mod.customizer.SboCustomizer.byName
import net.sbo.mod.customizer.SboCustomizer.isOwn
import net.sbo.mod.customizer.SboCustomizer.movingAtHead
import net.sbo.mod.customizer.SboCustomizer.movingInTab
import net.sbo.mod.customizer.SboCustomizer.ranks
import net.sbo.mod.customizer.SboCustomizer.uuidOf
import net.sbo.mod.utils.data.DataManager
import java.util.Optional

/** A team tag: the rank gives its text, the member's customizer its colors (the rank's while their customizer is off). */
internal class TagLook(val rank: String, val color: PartColor?, val animated: Boolean)

/** Puts a customizer's colors, symbols, styles and team tag into a line that holds the player's name. */
internal object CustomizerRecolor {
    /** In chat the emblem stands between level and rank: "[466] ♙ [MVP+] Name". */
    private val LEVEL = Regex("""(\[\d+])(?:\s*[^\[\]\sA-Za-z0-9_]+)?\s*$""")
    /** Hypixel's rank right before the name: "[MVP++] Name", "[YOUTUBE] Name". */
    private val RANK = Regex("""\[[A-Z]+\+*]\s*$""")
    private const val TAG_BRACKETS = 0xAAAAAA
    /** Mods rebuild Hypixel's § colors as rgb colors, so they are compared by value. */
    private val CHAT_COLORS: Set<Int> = ChatFormatting.entries.mapNotNull { TextColor.fromLegacyFormat(it)?.value }.toSet()

    /**
     * Symbols around a part as the viewer sees them, with the space between them and the part: other players' only while
     * allowed, your own always; none where text keeps its width. Backends before symbols per part only had the name's.
     */
    fun symbolsOf(customizer: PlayerCustomizer?, part: String, keepWidth: Boolean = false): Pair<String, String> {
        if (customizer == null || keepWidth || !stylesShown(customizer)) return NO_SYMBOLS
        val color = customizer.colors?.get(part)
        val look = customizer.look.takeIf { part == CustomizerPart.NAME }
        val before = color?.before ?: look?.before
        val after = color?.after ?: look?.after
        return before?.let { "$it " }.orEmpty() to after?.let { " $it" }.orEmpty()
    }

    private val NO_SYMBOLS = "" to ""

    /** Whether the viewer sees the customizer's text styles: other players' only while allowed, your own always. */
    fun stylesShown(customizer: PlayerCustomizer?): Boolean = customizer != null && (DataManager.sboData.customizerLooksOfOthers || isOwn(customizer))

    /** "[SBO Dev] " in the given color or the rank's, null for ranks without a tag. */
    fun tagOf(rank: String, partColor: PartColor? = null): Component? {
        val info = ranks[rank] ?: return null
        val text = info.tag ?: return null
        fun bracket(hex: String?) = Style.EMPTY.withColor(hex?.let(::parseHex) ?: TAG_BRACKETS)
        return Component.empty()
            .append(Component.literal("[").withStyle(bracket(partColor?.open)))
            .append(colored(text, partColor ?: PartColor(info.color, info.to), CustomizerPart.TAG, animate = false))
            .append(Component.literal("] ").withStyle(bracket(partColor?.close)))
    }

    /** Whether the colors of [name]'s customizer move for the viewer; their chars can move without them. */
    fun colorsMove(name: String): Boolean = byName[name.lowercase()]?.let { uuidOf(it.uuid) }?.let(::animates) != false

    private const val RECOLOR_CACHE_SIZE = 256
    /** Customizer and tag follow from the name and the list, which clears the cache. */
    private val recolorCache = object : LinkedHashMap<Triple<Component, String, String>, Component>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Triple<Component, String, String>, Component>) = size > RECOLOR_CACHE_SIZE
    }

    fun clearCache() {
        synchronized(recolorCache) { recolorCache.clear() }
    }

    /**
     * [recolor] through a cache. Lines with moving colors change every frame, so they skip it; moving chars with their
     * colors turned off still move, the colors stand still.
     */
    fun recolored(line: Component, player: String, place: String, tag: TagLook?): Component {
        val lower = player.lowercase()
        if (lower in (if (place == TeamTagPlace.TAB) movingInTab else movingAtHead)) {
            val customizer = byName[lower]
            return recolor(line, player, customizer, place, tag, animate = colorsMove(player), moveChars = place == TeamTagPlace.HEAD)
        }
        val key = Triple(line, lower, place)
        synchronized(recolorCache) { recolorCache[key]?.let { return it } }
        val result = recolor(line, player, byName[lower], place, tag)
        synchronized(recolorCache) { recolorCache[key] = result }
        return result
    }

    private class StyledChar(val char: Char, val style: Style)

    private fun findPlayerNameRange(
        text: String,
        playerName: String,
        from: Int = 0,
    ): IntRange? {
        if (playerName.isEmpty()) return null

        val nameLength = playerName.length
        val startFrom = from.coerceIn(0, text.length)

        for (start in startFrom..text.length - nameLength) {
            val end = start + nameLength

            if (start > 0 && isNameChar(text[start - 1])) continue

            if (end < text.length && isNameChar(text[end])) continue

            if (text.regionMatches(
                    ignoreCase = true,
                    thisOffset = start,
                    other = playerName,
                    otherOffset = 0,
                    length = nameLength,
                )
            ) {
                return start..<end
            }
        }

        return null
    }

    fun isNameChar(char: Char): Boolean =
        char in 'A'..'Z' ||
            char in 'a'..'z' ||
            char in '0'..'9' ||
            char == '_'

    /**
     * [line] with [playerName] in the customizer's look at [place]. Hypixel mixes styles and § codes, so the line is read
     * char by char and built again. Name, level and rank each take their own color where the customizer shows them;
     * without a customizer (team tag only) nothing is colored. A name another mod already colored (not one of the 16 §
     * colors Hypixel uses) keeps its color, only the tag and logo go in.
     *
     * The rank is found in the line as it came, so a tag never reads as a rank; in chat and for some ranks in the tab list
     * Hypixel's rank stands between level and name. The level only colors its brackets and digits, an emblem behind them
     * keeps its own color. Symbols go in as chars of their part, so they take its colors, effect and style; left to right,
     * each moves the parts behind it.
     *
     * The team tag ("[SBO Dev] ") goes in as chars too, in front of the whole name tag above heads and in front of the name
     * elsewhere, so it can share a chain with the parts next to it. Its range leaves out the space behind it so the gap
     * check sees it: only spaces may stand between chained parts. Brackets and the rank's plus can have their own color,
     * picked by the char since symbols can stand outside the brackets; the tag's brackets are gray without one, unless the
     * tag flows in a line with the parts next to it.
     *
     * @param from the name is looked for from this char on
     * @param nameOnly leaves level and rank in front of the name alone (names inside a message)
     * @param forceRank the rank takes its color even where the customizer doesn't show it (names recolored everywhere)
     * @param keepWidth names recolored everywhere keep their width: no symbols, no bold
     * @param moveChars chars move above heads, and in chat while chat animations are on
     */
    fun recolor(
        line: Component,
        playerName: String,
        customizer: PlayerCustomizer?,
        place: String,
        tag: TagLook? = null,
        afterName: Component? = null,
        animate: Boolean = false,
        from: Int = 0,
        nameOnly: Boolean = false,
        forceRank: Boolean = false,
        keepWidth: Boolean = false,
        moveChars: Boolean = animate && place == TeamTagPlace.HEAD,
    ): Component {
        val read = ArrayList<StyledChar>()
        val readPlainBuilder = StringBuilder()
        line.visit({ style, text ->
            var current = style
            var i = 0
            while (i < text.length) {
                val format = if (text[i] == '§' && i + 1 < text.length) ChatFormatting.getByCode(text[i + 1]) else null
                if (format != null) {
                    current = if (format == ChatFormatting.RESET) style else current.applyLegacyFormat(format)
                    i += 2
                    continue
                }
                read += StyledChar(text[i], current)
                readPlainBuilder.append(text[i])
                i++
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)

        val readPlain = readPlainBuilder.toString()
        var foundName = findPlayerNameRange(readPlain, playerName, from) ?: return line
        val nameColor = customizer?.takeIf { it.shows(CustomizerPart.NAME, place) }?.colorOf(CustomizerPart.NAME)
        val levelColor = customizer?.takeIf { !nameOnly && it.shows(CustomizerPart.LEVEL, place) }?.colorOf(CustomizerPart.LEVEL)
        val rankColor = customizer?.takeIf { !nameOnly && (forceRank || it.shows(CustomizerPart.RANK, place)) }?.colorOf(CustomizerPart.RANK)
        val tagInfo = tag?.let { ranks[it.rank] }
        val tagText = tagInfo?.tag
        if (nameColor == null && levelColor == null && rankColor == null && tagText == null && afterName == null) return line

        val foreign = foundName.any { i ->
            read[i].style.color?.let { it.value !in CHAT_COLORS } == true
        }
        if (foreign && tagText == null && afterName == null) return line

        val styles = stylesShown(customizer)

        val rankMatch = RANK.find(readPlain.take(foundName.first))
        val levelEnd = rankMatch?.range?.first ?: foundName.first
        var levelRange = levelColor?.takeUnless { foreign }?.let { LEVEL.find(readPlain.take(levelEnd))?.groups?.get(1)?.range }
        var rankRange = rankColor?.takeUnless { foreign }?.let { rankMatch }?.let { it.range.first..<it.range.first + it.value.trimEnd().length }

        var shift = 0
        fun withSymbols(range: IntRange, part: String): IntRange {
            val at = range.first + shift..range.last + shift
            val (before, after) = symbolsOf(customizer, part, keepWidth)
            if (before.isEmpty() && after.isEmpty()) return at
            val first = read[at.first].style
            val last = read[at.last].style
            read.addAll(at.last + 1, after.map { StyledChar(it, last) })
            read.addAll(at.first, before.map { StyledChar(it, first) })
            shift += before.length + after.length
            return at.first..at.last + before.length + after.length
        }
        levelRange = levelRange?.let { withSymbols(it, CustomizerPart.LEVEL) }
        rankRange = rankRange?.let { withSymbols(it, CustomizerPart.RANK) }
        foundName = if (nameColor != null && !foreign) withSymbols(foundName, CustomizerPart.NAME) else foundName.first + shift..foundName.last + shift

        val insertAt = if (place == TeamTagPlace.HEAD) 0 else foundName.first
        val (tagBefore, tagAfter) = if (tagText != null) symbolsOf(customizer, CustomizerPart.TAG, keepWidth) else NO_SYMBOLS
        val tagChars = tagText?.let { "$tagBefore[$it]$tagAfter " }.orEmpty()
        fun moved(range: IntRange) = if (range.first >= insertAt) range.first + tagChars.length..range.last + tagChars.length else range
        val name = moved(foundName)
        levelRange = levelRange?.let(::moved)
        rankRange = rankRange?.let(::moved)
        val tagRange = tagText?.let { insertAt..<insertAt + tagChars.length - 1 }
        val tagColor = tag?.color ?: tagInfo?.let { PartColor(it.color, it.to) }

        val segments = listOfNotNull(
            levelColor?.let { color -> levelRange?.let { Segment(CustomizerPart.LEVEL, color, it) } },
            rankColor?.let { color -> rankRange?.let { Segment(CustomizerPart.RANK, color, it) } },
            nameColor?.takeUnless { foreign }?.let { Segment(CustomizerPart.NAME, it, name) },
            tagColor?.let { color -> tagRange?.let { Segment(CustomizerPart.TAG, color, it) } },
        ).sortedBy { it.range.first }
        if (afterName == null && segments.isEmpty()) return line
        val paints = paints(segments) { before, next ->
            (before.range.last + 1..<next.range.first).all { i ->
                if (i in insertAt..<insertAt + tagChars.length) tagChars[i - insertAt] == ' '
                else read[if (i < insertAt) i else i - tagChars.length].char == ' '
            }
        }

        val now = System.currentTimeMillis()

        fun colorOf(k: Int, i: Int, char: Char): Int {
            val segment = segments[k]
            val color = segment.color
            val own = color.chain != true
            val tagBracket = segment.part == CustomizerPart.TAG && own && (char == '[' || char == ']')
            return when {
                own && char == '[' && color.open != null -> paints[k].at(i, color.open, animate, now)
                own && char == ']' && color.close != null -> paints[k].at(i, color.close, animate, now)
                tagBracket -> TAG_BRACKETS
                own && char == '+' && color.plus != null -> paints[k].at(i, color.plus, animate, now)
                else -> paints[k].at(i, animate, now)
            }
        }
        val out = Component.empty()
        var run = StringBuilder()
        var runStyle: Style? = null
        fun flush() {
            runStyle?.let { out.append(Component.literal(run.toString()).withStyle(it)) }
            run = StringBuilder()
        }
        var segmentIndex = 0
        val outputLength = read.size + tagChars.length
        for (i in 0..<outputLength) {
            val inTag = i in insertAt..<insertAt + tagChars.length
            val sourceIndex = if (inTag) -1 else if (i < insertAt) i else i - tagChars.length
            val char = if (inTag) tagChars[i - insertAt] else read[sourceIndex].char
            val baseStyle = if (inTag) Style.EMPTY else read[sourceIndex].style
            while (segmentIndex < segments.size && i > segments[segmentIndex].range.last) segmentIndex++
            val k = segmentIndex.takeIf { it < segments.size && i in segments[it].range } ?: -1
            val painted = if (k < 0) baseStyle else baseStyle.withColor(colorOf(k, i, char))
            val colored = if (k >= 0 && styles) segments[k].color.styled(painted, keepWidth) else painted
            val style = if (k < 0 || !moveChars) colored else paints[k].moved(colored, i, char, true, now)
            if (style != runStyle) {
                flush()
                runStyle = style
            }
            run.append(char)
            if (afterName != null && i == name.last) {
                flush()
                runStyle = null
                out.append(afterName)
            }
        }
        flush()
        return out
    }

    /** [text] in the part's colors; [animate] moves the colors, [moveChars] the chars: the viewer can turn off one without the other. */
    fun colored(text: String, color: PartColor, part: String, animate: Boolean, styled: Boolean = false, moveChars: Boolean = animate): MutableComponent {
        fun look(style: Style) = if (styled) color.styled(style) else style
        if (color.to == null && color.effectOf(part) == CustomizerEffect.STATIC && (!moveChars || color.motion.isStill())) {
            return Component.literal(text).withStyle { look(it.withColor(parseHex(color.color))) }
        }
        val now = System.currentTimeMillis()
        val out = Component.empty()
        text.forEachIndexed { i, char ->
            out.append(Component.literal(char.toString()).withStyle {
                moved(look(it.withColor(colorAt(color, part, i, text.length, animate, now))), color.motion, i, text.length, char, moveChars, now)
            })
        }
        return out
    }
}
