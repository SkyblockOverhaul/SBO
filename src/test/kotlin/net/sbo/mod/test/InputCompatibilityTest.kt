package net.sbo.mod.test

import net.sbo.mod.utils.InputCompatibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InputCompatibilityTest {
    @Test
    fun mouseButtonsKeepExistingEventIds() {
        //#if MC > 26.2
        //$$ assertEquals(0, InputCompatibility.mouseButton(1))
        //$$ assertEquals(1, InputCompatibility.mouseButton(3))
        //$$ assertEquals(2, InputCompatibility.mouseButton(2))
        //$$ assertEquals(3, InputCompatibility.mouseButton(4))
        //$$ assertEquals(4, InputCompatibility.mouseButton(5))
        //#else
        for (button in 0..4) assertEquals(button, InputCompatibility.mouseButton(button))
        //#endif
    }
}
