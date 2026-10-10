package net.sbo.mod.test

import net.sbo.mod.utils.InputCompatibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
//#if MC > 26.2
//$$ import org.lwjgl.sdl.SDLMouse
//#endif

class InputCompatibilityTest {
    @Test
    fun `native buttons retain the SBO event contract`() {
        //#if MC > 26.2
        //$$ assertEquals(0, InputCompatibility.mouseButton(SDLMouse.SDL_BUTTON_LEFT))
        //$$ assertEquals(1, InputCompatibility.mouseButton(SDLMouse.SDL_BUTTON_RIGHT))
        //$$ assertEquals(2, InputCompatibility.mouseButton(SDLMouse.SDL_BUTTON_MIDDLE))
        //$$ assertEquals(3, InputCompatibility.mouseButton(SDLMouse.SDL_BUTTON_X1))
        //$$ assertEquals(4, InputCompatibility.mouseButton(SDLMouse.SDL_BUTTON_X2))
        //#else
        for (button in 0..4) assertEquals(button, InputCompatibility.mouseButton(button))
        //#endif
    }
}
