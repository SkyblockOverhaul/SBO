package net.sbo.mod.diana.burrows

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket

internal data class ParticleCheck(val typeCheck: (packet: ClientboundLevelParticlesPacket) -> Boolean)