package net.sbo.mod.utils.events.impl.packets

import net.minecraft.network.protocol.game.ClientboundLoginPacket

/** Emitted after the client has handled a login packet on the game thread. */
class LoginReceiveEvent(val packet: ClientboundLoginPacket)
