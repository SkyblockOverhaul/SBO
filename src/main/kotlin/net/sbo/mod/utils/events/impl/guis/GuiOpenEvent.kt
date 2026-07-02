package net.sbo.mod.utils.events.impl.guis

import net.minecraft.client.gui.screens.Screen
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Event fired when a GUI screen is opened.
 * @param screen The screen that is being opened.
 * @param ci The callback info to control the event flow.
 */
class GuiOpenEvent(val screen: Screen, private val ci: CallbackInfo)