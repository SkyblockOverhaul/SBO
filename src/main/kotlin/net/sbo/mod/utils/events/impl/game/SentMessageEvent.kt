package net.sbo.mod.utils.events.impl.game

/**
 * Event fired when a chat message is sent by the player to the server.
 * Includes messages sent automatically by any mod in behalf of the player.
 * Triggers in all channels (all, guild, party, officer, etc.)
 *
 * For commands, use {@link SentCommandEvent}.
 */
class SentMessageEvent
