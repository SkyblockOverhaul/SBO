package net.sbo.mod.utils

/** Keeps the existing mouse event IDs: left 0, right 1, middle 2. */
object InputCompatibility {
    fun mouseButton(button: Int): Int {
        //#if MC > 26.2
        //$$ return when (button) {
        //$$     org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT -> 0
        //$$     org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT -> 1
        //$$     org.lwjgl.sdl.SDLMouse.SDL_BUTTON_MIDDLE -> 2
        //$$     else -> button - 1
        //$$ }
        //#else
        return button
        //#endif
    }
}
