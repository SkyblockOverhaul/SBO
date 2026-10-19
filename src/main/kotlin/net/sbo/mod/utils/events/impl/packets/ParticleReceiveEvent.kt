package net.sbo.mod.utils.events.impl.packets

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

/** Emitted after the client has handled a particle packet on the game thread. */
class ParticleReceiveEvent(val packet: ClientboundLevelParticlesPacket)
