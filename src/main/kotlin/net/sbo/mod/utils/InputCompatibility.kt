package net.sbo.mod.utils

/** Preserve SBO's zero-based mouse event IDs across the SDL input transition. */
object InputCompatibility {
    fun mouseButton(button: Int): Int {
        //#if MC > 26.2
        //$$ return when (button) {
        //$$     1 -> 0 // SDL left -> SBO left
        //$$     3 -> 1 // SDL right -> SBO right
        //$$     2 -> 2 // SDL middle -> SBO middle
        //$$     else -> button - 1
        //$$ }
        //#else
        return button
        //#endif
    }
}
