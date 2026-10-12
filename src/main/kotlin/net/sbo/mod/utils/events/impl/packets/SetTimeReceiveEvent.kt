package net.sbo.mod.utils.events.impl.packets

import net.minecraft.network.protocol.game.ClientboundSetTimePacket

/** Emitted after the client has handled a time update on the game thread. */
class SetTimeReceiveEvent(val packet: ClientboundSetTimePacket)
