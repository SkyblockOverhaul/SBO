package net.sbo.mod.utils.game

import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
import kotlin.math.max
import java.util.concurrent.TimeUnit

object ServerStats {
    private var prevTime = 0L
    private var averageTps = 20f

    fun getTps(): Float {
        return averageTps
    }

    @SboEvent
    fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packet) {
            is ClientboundSetTimePacket -> {
                val currentTime = System.nanoTime()
                if (prevTime != 0L) {
                    val deltaTime = currentTime - prevTime
                    averageTps = (TimeUnit.MILLISECONDS.toNanos(20000L).toFloat() / max(1, deltaTime)).coerceIn(0f, 20f)
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
