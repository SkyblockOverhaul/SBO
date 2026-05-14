package net.sbo.mod.utils.events.impl.packets

import net.minecraft.network.protocol.Packet

/**
 * Called when the client receives any packet from the server.
 * @param packet The packet that was received.
 */
class PacketSendEvent(val packet: Packet<*>)
