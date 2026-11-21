package net.sbo.mod.customizer

import com.google.common.collect.MapMaker
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.ComponentRenderUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.customizer.CustomizerEffects.changesAt
import net.sbo.mod.customizer.CustomizerRecolor.colorsMove
import net.sbo.mod.customizer.CustomizerRecolor.isNameChar
import net.sbo.mod.customizer.CustomizerRecolor.recolor
import net.sbo.mod.customizer.SboCustomizer.LOGO
import net.sbo.mod.customizer.SboCustomizer.NAMES_ALL
import net.sbo.mod.customizer.SboCustomizer.NAMES_OFF
import net.sbo.mod.customizer.SboCustomizer.NAMES_OWN
import net.sbo.mod.customizer.SboCustomizer.animates
import net.sbo.mod.customizer.SboCustomizer.byName
import net.sbo.mod.customizer.SboCustomizer.chatTags
import net.sbo.mod.customizer.SboCustomizer.logo
import net.sbo.mod.customizer.SboCustomizer.movingEverywhere
import net.sbo.mod.customizer.SboCustomizer.own
import net.sbo.mod.customizer.SboCustomizer.safe
import net.sbo.mod.customizer.SboCustomizer.showInChat
import net.sbo.mod.customizer.SboCustomizer.shown
import net.sbo.mod.customizer.SboCustomizer.team
import net.sbo.mod.customizer.SboCustomizer.uuidOf
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.DataManager
import java.util.WeakHashMap

/** Customizers in chat (sender and names inside messages, moving ones built again every frame) and in any other drawn text. */
internal object CustomizerChat {
    /** The sender stands before the first ": " of a chat line: "[296] [MVP+] Name: hi", "Party > Name: hi", "From Name: hi". */
    private const val MAX_SENDER_LENGTH = 80
    private val WORD = Regex("[A-Za-z0-9_]{1,16}")
    /** A whole word, so the end of a longer word never reads as a name. */
    private val MENTION = Regex("[A-Za-z0-9_]+")

    fun clearCache() {
        synchronized(scans) { scans.clear() }
    }

    /**
     * The sender of a chat line: team tag in front, customizer color and the SBO logo behind the name.
     * With [NAMES_OWN] or [NAMES_ALL] names inside the message get tag and color too.
     * Lines with moving customizers are built again while they are drawn, see [animatedChatLine].
     */
    fun decorateChat(line: Component): Component = safe(line) {
        val decorated = decorateChatOf(line, animate = false)
        if (decorated !== line && movesInChat(line)) animatedMessages[decorated] = AnimatedMessage(line)
        decorated
    }

    /** Whether a name the chat line decorates has moving colors or chars that the viewer lets move in chat. */
    private fun movesInChat(line: Component): Boolean {
        if (DataManager.sboData.customizerAnimationsInChat == NAMES_OFF) return false
        val plain = ChatFormatting.stripFormatting(line.string) ?: return false
        return MENTION.findAll(plain).any { animatesInChat(it.value) }
    }

    /**
     * Whose customizer moves in chat: nobody's, only your own or everyone's whose animations the viewer allows.
     * Chars move in chat too, so it checks like above heads: colors only while the viewer allows them, chars always
     * (Motion off already took them out).
     */
    private fun animatesInChat(name: String): Boolean {
        val lower = name.lowercase()
        val customizer = byName[lower] ?: return false
        val uuid = uuidOf(customizer.uuid) ?: return false
        val allowed = when (DataManager.sboData.customizerAnimationsInChat) {
            NAMES_ALL -> true
            NAMES_OWN -> uuid == uuidOf(Player.accountUuid())
            else -> false
        }
        if (!allowed) return false
        val parts = listOf(CustomizerPart.NAME, CustomizerPart.LEVEL, CustomizerPart.RANK).filter { customizer.shows(it, TeamTagPlace.CHAT) } +
            listOfNotNull(CustomizerPart.TAG.takeIf { lower in chatTags })
        return parts.any { changesAt(customizer.colorOf(it), it, TeamTagPlace.HEAD, animates(uuid)) }
    }

    private class AnimatedMessage(val original: Component) {
        var frame = -1L
        var width = -1
        var lines: List<FormattedCharSequence> = emptyList()
    }
    private class AnimatedLine(val message: AnimatedMessage, val width: Int, val index: Int, val count: Int)
    /** By the objects themselves (weak, by identity), gone once the chat drops the message; [animatedLines] the same. */
    private val animatedMessages: MutableMap<Component, AnimatedMessage> = MapMaker().weakKeys().makeMap()
    private val animatedLines: MutableMap<FormattedCharSequence, AnimatedLine> = MapMaker().weakKeys().makeMap()
    private const val CHAT_FRAME_MS = 25L

    /** The chat split a message into lines; lines of a message with moving customizers are remembered. */
    fun splitChatLines(content: Component, width: Int, lines: List<FormattedCharSequence>) {
        if (animatedMessages.isEmpty()) return
        val message = animatedMessages[content] ?: return
        lines.forEachIndexed { i, line -> animatedLines[line] = AnimatedLine(message, width, i, lines.size) }
    }

    /**
     * A chat line with moving customizers as it looks right now, null for every other text. Built once per frame for all
     * lines of the message; colors and motion never change the width, so it splits the same.
     */
    fun animatedChatLine(text: FormattedCharSequence?): FormattedCharSequence? {
        if (text == null || animatedLines.isEmpty()) return null
        val line = animatedLines[text] ?: return null
        return safe(null) {
            val message = line.message
            val frame = System.currentTimeMillis() / CHAT_FRAME_MS
            synchronized(message) {
                if (message.frame != frame || message.width != line.width) {
                    val decorated = decorateChatOf(message.original, animate = true)
                    message.lines = ComponentRenderUtils.wrapComponents(decorated, line.width, mc.font)
                    message.frame = frame
                    message.width = line.width
                }
                message.lines.takeIf { it.size == line.count }?.get(line.index)
            }
        }
    }

    /** Names inside the message go right to left, so a tag put in never moves the names still to come; the sender stands before all of them. */
    private fun decorateChatOf(line: Component, animate: Boolean): Component {
        val mentions = DataManager.sboData.customizerNamesInChat
        if (mentions != NAMES_OWN && !showInChat && team.isEmpty() && own() == null || byName.isEmpty() && chatTags.isEmpty()) return line
        val plain = ChatFormatting.stripFormatting(line.string) ?: return line
        if (LOGO in plain) return line
        val colon = plain.indexOf(": ").takeIf { it in 1..MAX_SENDER_LENGTH }
        val sender = colon?.let { end -> WORD.findAll(plain.take(end)).map { it.value }.firstOrNull(::shownInChat) }
        val wanted = wantedNames()
        var out = line
        if (wanted != null) {
            MENTION.findAll(plain, colon?.plus(2) ?: 0).filter { wanted(it.value) }.toList().asReversed()
                .forEach { out = decorateChatName(out, it.value, from = it.range.first, nameOnly = true, animate = animate) }
        }
        if (sender != null) out = decorateChatName(out, sender, animate = animate)
        return out
    }

    /** Which names inside a message get their look, null while names in messages are off. */
    private fun wantedNames(): ((String) -> Boolean)? = when (DataManager.sboData.customizerNamesInChat) {
        NAMES_ALL -> ::shownInChat
        NAMES_OWN -> ownName()?.let { own -> { name: String -> name.equals(own, ignoreCase = true) } }
        else -> null
    }

    private fun shownInChat(name: String): Boolean =
        name.length <= 16 && (name.lowercase() in byName || name.lowercase() in chatTags) && shown(showInChat, name)

    /** Your own name while you have a customizer or a team tag in chat. */
    private fun ownName(): String? = own()?.name ?: uuidOf(Player.accountUuid())?.let { team[it] }?.name?.takeIf { it.lowercase() in chatTags }

    /** [name] in [line]; with [nameOnly] (a name inside the message) level and rank stay as they are, no logo, the tag only while the viewer wants it. */
    private fun decorateChatName(line: Component, name: String, from: Int = 0, nameOnly: Boolean = false, animate: Boolean = false): Component {
        val customizer = byName[name.lowercase()]
        val moving = animate && animatesInChat(name)
        return recolor(
            line, name, customizer, TeamTagPlace.CHAT,
            tag = chatTags[name.lowercase()]?.takeIf { !nameOnly || DataManager.sboData.customizerNamesTag },
            afterName = customizer?.takeIf { !nameOnly && it.shows(CustomizerPart.LOGO, TeamTagPlace.CHAT) }?.let { Component.literal(" ").append(logo()) },
            from = from,
            nameOnly = nameOnly,
            animate = moving && colorsMove(name),
            moveChars = moving,
        )
    }

    /**
     * Recolors the customizer holders' names that names in messages picks (and the level/rank in front of them) in any text that is drawn:
     * GUIs, titles, signs, scoreboards, books... Returns null when nothing changes.
     */
    fun replaceEverywhere(text: FormattedCharSequence?): FormattedCharSequence? {
        if (text == null || !DataManager.sboData.customizerReplaceEverywhere || byName.isEmpty()) return null
        return safe(null) { replaceEverywhereOf(text) }
    }

    /**
     * Names in messages decides whose names are recolored, off recolors none. Every drawn text passes here (drt and SkyOcean
     * scanning all of it cost players whole frames). Chat lines and most component text come back as the same object every
     * frame, so what was found in it is kept: text without a name then costs one lookup, text whose colors stand still is
     * built once. Names go right to left like inside chat messages, without tag or logo, so the text keeps its width and
     * recoloring it again changes nothing.
     */
    private fun replaceEverywhereOf(text: FormattedCharSequence): FormattedCharSequence? {
        val mode = DataManager.sboData.customizerNamesInChat
        if (mode == NAMES_OFF) return null
        val list = byName
        val inChat = showInChat
        val known = synchronized(scans) { scans[text] }?.takeIf { it.list === list && it.mode == mode && it.inChat == inChat }
        if (known != null) {
            if (known.found.isEmpty()) return null
            known.still?.let { return it }
        }
        val found = known?.found ?: namesIn(text, list, wantedNames() ?: return null)
        if (found.isEmpty()) {
            synchronized(scans) { scans[text] = Scan(list, mode, inChat, found, null) }
            return null
        }

        val runs = ArrayList<Pair<String, Style>>()
        val run = StringBuilder()
        var runStyle: Style? = null
        text.accept { _, style, codePoint ->
            if (style != runStyle) {
                runStyle?.let { runs += run.toString() to it }
                run.setLength(0)
                runStyle = style
            }
            run.appendCodePoint(codePoint)
            true
        }
        runStyle?.let { runs += run.toString() to it }

        var out: Component = Component.empty().also { line -> runs.forEach { (s, style) -> line.append(Component.literal(s).withStyle(style)) } }
        for (name in found.asReversed()) {
            out = recolor(
                out, name.name, list[name.name.lowercase()], TeamTagPlace.CHAT,
                animate = name.name.lowercase() in movingEverywhere, from = name.start, forceRank = true, keepWidth = true,
            )
        }
        val result = out.visualOrderText
        val still = found.none { it.name.lowercase() in movingEverywhere }
        synchronized(scans) { scans[text] = Scan(list, mode, inChat, found, result.takeIf { still }) }
        return result
    }

    /**
     * Customizer holders' names in the text, written exactly like theirs; empty when it holds none. Whole words only, so the
     * end of a longer word never reads as a name; no regex, this runs for every new text.
     */
    private fun namesIn(text: FormattedCharSequence, list: Map<String, PlayerCustomizer>, wanted: (String) -> Boolean): List<FoundName> {
        val plain = StringBuilder()
        text.accept { _, _, codePoint -> plain.appendCodePoint(codePoint); true }
        if (plain.indexOf(LOGO) >= 0) return emptyList()
        var found: MutableList<FoundName>? = null
        var start = -1
        for (i in 0..plain.length) {
            val inWord = i < plain.length && isNameChar(plain[i])
            if (inWord) {
                if (start < 0) start = i
                continue
            }
            if (start >= 0 && i - start <= 16) {
                val word = plain.substring(start, i)
                if (list[word.lowercase()]?.name == word && wanted(word)) {
                    (found ?: ArrayList<FoundName>().also { found = it }).add(FoundName(word, start))
                }
            }
            start = -1
        }
        return found.orEmpty()
    }

    private class FoundName(val name: String, val start: Int)
    /** What a drawn text held, valid for the list and settings it was found with; [still] is the finished text when its colors stand still. */
    private class Scan(val list: Map<String, PlayerCustomizer>, val mode: String, val inChat: Boolean, val found: List<FoundName>, val still: FormattedCharSequence?)
    /** By the text object itself, gone once the game drops the text. */
    private val scans = WeakHashMap<FormattedCharSequence, Scan>()
}
