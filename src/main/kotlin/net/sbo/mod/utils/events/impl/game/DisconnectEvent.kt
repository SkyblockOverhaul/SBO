package net.sbo.mod.utils.events.impl.game

import net.minecraft.client.Minecraft

/**
 * Called when the client disconnects from a server.
 * @param mc The MinecraftClient instance.
 */
class DisconnectEvent(val mc: Minecraft)
