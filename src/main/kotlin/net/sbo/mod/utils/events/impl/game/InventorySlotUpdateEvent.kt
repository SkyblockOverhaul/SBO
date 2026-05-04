package net.sbo.mod.utils.events.impl.game

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket

/**
 * Called when the client receives a ScreenHandlerSlotUpdateS2CPacket from the server.
 * @param packet The packet that was received, can be null.
 */
class InventorySlotUpdateEvent(val packet: ClientboundContainerSetSlotPacket?)