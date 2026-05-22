package net.sbo.mod.diana.burrows

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.core.particles.ParticleTypes as MCParticleTypes

internal object ParticleTypes {
    internal val PARTICLE_CHECKS = mutableMapOf(
        "ENCHANT" to ParticleCheck { packet ->
            packet.particle.type == MCParticleTypes.ENCHANT &&
                    packet.count == 5 &&
                    packet.maxSpeed == 0.05f &&
                    packet.xDist == 0.5f &&
                    packet.yDist == 0.4f &&
                    packet.zDist == 0.5f
        },
        "EMPTY" to ParticleCheck { packet ->
            packet.particle.type == MCParticleTypes.ENCHANTED_HIT &&
                    packet.count == 4 &&
                    packet.maxSpeed == 0.01f &&
                    packet.xDist == 0.5f &&
                    packet.yDist == 0.1f &&
                    packet.zDist == 0.5f
        },
        "MOB" to ParticleCheck { packet ->
            packet.particle.type == MCParticleTypes.CRIT &&
                    packet.count == 3 &&
                    packet.maxSpeed == 0.01f &&
                    packet.xDist == 0.5f &&
                    packet.yDist == 0.1f &&
                    packet.zDist == 0.5f
        },
        "TREASURE" to ParticleCheck { packet ->
            packet.particle.type == MCParticleTypes.DRIPPING_LAVA &&
                    packet.count == 2 &&
                    packet.maxSpeed == 0.01f &&
                    packet.xDist == 0.35f &&
                    packet.yDist == 0.1f &&
                    packet.zDist == 0.35f
        },
        "FOOTSTEP" to ParticleCheck { packet ->
            packet.particle.type == MCParticleTypes.CRIT &&
                    packet.count == 1 &&
                    packet.maxSpeed == 0.0f &&
                    packet.xDist == 0.05f &&
                    packet.yDist == 0.0f &&
                    packet.zDist == 0.05f
        }
    )

    fun getParticleType(packet: ClientboundLevelParticlesPacket): String? {
        PARTICLE_CHECKS.forEach { (type, check) ->
            if (check.typeCheck(packet)) {
                return type
            }
        }
        return null
    }
}