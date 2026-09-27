package net.sbo.mod.guis.partyfinder

import java.awt.Color

object Theme {
    val TRANSPARENT = Color(0, 0, 0, 0)
    val WHITE = Color(255, 255, 255, 255)
    val DARK_GRAY = Color(50, 50, 50, 255)
    private val LIGHT_GRAY = Color(100, 100, 100, 200)
    val ROYAL_BLUE = Color(50, 50, 255, 200)
    val SBO_BLUE = Color(0, 110, 250, 255)
    private val AQUA = Color(85, 255, 255, 255)
    val BLACK_SEMI_TRANS = Color(0, 0, 0, 200)
    val BLACK_HALF_TRANS = Color(0, 0, 0, 150)
    private val GREEN = Color(0, 255, 0, 255)
    private val ORANGE = Color(255,165,0,255)
    val RED = Color(255, 0, 0, 255)


    val TEXT_PRIMARY = WHITE

    val INPUT_BG = DARK_GRAY
    val INPUT_TEXT = WHITE

    val CHECKBOX_BG = BLACK_SEMI_TRANS
    val CHECKBOX_CHECKED = SBO_BLUE
    val CHECKBOX_FILTER_BG = Color(25, 25, 25, 200)

    val BUTTON_DEFAULT = DARK_GRAY
    val BUTTON_HOVER = LIGHT_GRAY

    val FILTER_BOX_BG = DARK_GRAY
    val FILTER_TINT = BLACK_HALF_TRANS

    val BASE = Color(30, 30, 30, 240)

    // --- BUTTONS ---
    val BUTTON_JOIN_BG = DARK_GRAY
    val BUTTON_JOIN_TEXT = GREEN
    val BUTTON_JOIN_HOVER_IN = Color(70, 70, 70, 200)
    val BUTTON_JOIN_HOVER_OUT = Color(30, 30, 30, 255)
    val BUTTON_TITLE_DISC_GIT_PAT_UNDERLINE = SBO_BLUE
    val BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN = SBO_BLUE
    val BUTTON_TITLE_DISC_GIT_PAT_HOVER_OUT = TEXT_PRIMARY

    // --- PAGES ---
    val PAGE_BLOCK = TRANSPARENT
    val PAGE_TITLE = WHITE
    val PAGE_HOVER_IN = DARK_GRAY
    val PAGE_HOVER_OUT = TRANSPARENT
    val PAGE_CATEGORY_LINE = SBO_BLUE

    // --- PARTY LIST ---
    val PARTY_LIST_BG = BLACK_HALF_TRANS
    val PARTY_LIST_OUTLINE = SBO_BLUE
    val PARTY_LIST_LEADER = AQUA
    val PARTY_LIST_INFO_SEPARATOR = SBO_BLUE
    val PARTY_LIST_SEPARATOR = SBO_BLUE
    val PARTY_LIST_FILTER = SBO_BLUE
    val PARTY_LIST_FILTER_HOVER_IN = Color(50, 50, 255, 200)
    val PARTY_LIST_FILTER_HOVER_OUT = SBO_BLUE
    val PARTY_LIST_REFRESH = SBO_BLUE
    val PARTY_LIST_REFRESH_HOVER_IN = Color(50, 50, 255, 200)
    val PARTY_LIST_REFRESH_HOVER_OUT = SBO_BLUE
    val PARTY_LIST_UNQUEUE = RED
    val PARTY_LIST_UNQUEUE_HOVER_IN = Color(50, 50, 255, 200)
    val PARTY_LIST_UNQUEUE_HOVER_OUT = RED
    val PARTY_LIST_CREATE = GREEN
    val PARTY_LIST_CREATE_HOVER_IN = Color(50, 50, 255, 200)
    val PARTY_LIST_CREATE_HOVER_OUT = GREEN

    // --- PARTY INFO ---
    val INFO_BG = Color(30, 30, 30, 240)
    val INFO_PLAYER_HOVER_IN = DARK_GRAY
    val INFO_PLAYER_HOVER_OUT = BLACK_SEMI_TRANS

    // --- CREATE PARTY ---
    val CREATE_BG = Color(30, 30, 30, 240)
    val CREATE_FILTER_BG = Color(0, 0, 0, 100)


    internal fun getMemberColor(members: Int, partySize: Int): Color =
        if (members * 2 < partySize) GREEN else ORANGE
}