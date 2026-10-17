package net.sbo.mod.utils.events.impl.game

import net.minecraft.network.chat.Component

/**
 * Event fired when a chat message is received, action bar updates excluded.
 * @param message The chat message text.
 */
class ChatMessageEvent(val message: Component)