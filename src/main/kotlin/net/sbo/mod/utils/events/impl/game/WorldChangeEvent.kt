package net.sbo.mod.utils.events.impl.game

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel

/**
 * Called when the world is changed (e.g., when joining a new server or switching dimensions).
 * @param mc The Minecraft client instance.
 * @param world The new ClientWorld instance.
 */
class WorldChangeEvent(val mc: Minecraft, val world: ClientLevel)