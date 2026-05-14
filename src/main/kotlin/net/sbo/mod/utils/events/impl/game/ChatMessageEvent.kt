package net.sbo.mod.utils.events.impl.game

import net.minecraft.network.chat.Component

/**
 * Event fired when a chat message is sent or received.
 * @param message The chat message text.
 * @param signed Whether the message is signed.
 */
class ChatMessageEvent(val message: Component, val signed: Boolean)