package net.sbo.mod.utils.game

import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.util.Util
import kotlin.math.max

object ServerStats {
    private var prevTime = 0L
    var averageTps = 20f
        private set

    fun getTps(): Float {
        return averageTps
    }

    @SboEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundSetTimePacket -> {
                val currentTime = Util.getMillis()
                if (prevTime != 0L) {
                    val deltaTime = currentTime - prevTime
                    averageTps = (20000f / max(1, deltaTime)).coerceIn(0f, 20f)
                }
                prevTime = currentTime
            }

            is ClientboundLoginPacket -> {
                averageTps = 20f
                prevTime = 0L
            }
        }
    }
}
