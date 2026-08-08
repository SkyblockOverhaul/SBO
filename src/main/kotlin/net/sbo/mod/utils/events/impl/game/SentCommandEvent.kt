package net.sbo.mod.utils.events.impl.game

/**
 * Event fired when a chat command is sent by the player to the server.
 * Includes commands sent automatically by any mod in behalf of the player.
 *
 * @param content The content of the command. Always begins with a slash ('/').
 * For messages, use {@link SentMessageEvent}.
 */
class SentCommandEvent(private val content: String)
