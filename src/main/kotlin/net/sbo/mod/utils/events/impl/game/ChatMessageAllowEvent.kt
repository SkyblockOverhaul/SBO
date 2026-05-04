package net.sbo.mod.utils.events.impl.game

import net.minecraft.network.chat.Component

class ChatMessageAllowEvent(val message: Component, val signed: Boolean, var isAllowed: Boolean)