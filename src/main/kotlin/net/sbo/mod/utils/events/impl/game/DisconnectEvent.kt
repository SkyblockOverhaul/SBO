package net.sbo.mod.utils.events.impl.game

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener

/**
 * Called when the client disconnects from a server.
 * @param handler The ClientPlayNetworkHandler instance.
 * @param mc The MinecraftClient instance.
 */
class DisconnectEvent(val handler: ClientPacketListener, val mc: Minecraft)