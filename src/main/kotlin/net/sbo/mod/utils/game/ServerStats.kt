 package net.sbo.mod.utils.game
 
 import net.minecraft.network.protocol.game.ClientboundLoginPacket
 import net.minecraft.network.protocol.game.ClientboundSetTimePacket
 import net.sbo.mod.utils.events.annotations.SboEvent
 import net.sbo.mod.utils.events.impl.packets.PacketReceiveEvent
 import java.util.concurrent.TimeUnit
 import kotlin.math.max
 
 object ServerStats {
     private var prevTime = 0L
     private var averageTps = 20f
     private const val TPS_HISTORY = 20
     private val tpsLog = FloatArray(TPS_HISTORY)
     private var tpsIndex = 0
     private var tpsCount = 0

     var lastPacket = 0L

     private fun addTpsSample(tps: Float) {
         tpsLog[tpsIndex] = tps
         tpsIndex = (tpsIndex + 1) % TPS_HISTORY
         if (tpsCount < TPS_HISTORY) tpsCount++
     }
 
     fun getTps(): Float {
         return averageTps
     }

     /**
      * Output format and sample size matches Odin's. https://github.com/odtheking/Odin/pull/124
      */
     fun getTpsString(): String {
         if (tpsCount == 0) return "Current: ${"%.1f".format(averageTps)}"

         var max = tpsLog[0]
         var min = tpsLog[0]
         var total = 0f
         for (i in 0 until tpsCount) {
             val tps = tpsLog[i]
             max = maxOf(max, tps)
             min = minOf(min, tps)
             total += tps
         }

         return "Current: ${"%.1f".format(averageTps)} (max/min/avg) ${"%.1f".format(max)}/${"%.1f".format(min)}/${"%.1f".format(total / tpsCount)}"
     }

     @SboEvent
     fun onPacketReceive(event: PacketReceiveEvent) {
         when (event.packet) {
             is ClientboundSetTimePacket -> {
                 val currentTime = System.nanoTime()
                 lastPacket = currentTime
                 if (prevTime != 0L) {
                     val deltaTime = currentTime - prevTime
                     averageTps = (TimeUnit.MILLISECONDS.toNanos(20000L).toFloat() / max(1, deltaTime)).coerceIn(0f, 20f)
                     addTpsSample(averageTps)
                 }
                 prevTime = currentTime
             }
 
             is ClientboundLoginPacket -> {
                 averageTps = 20f
                 prevTime = 0L
                 tpsIndex = 0
                 tpsCount = 0
             }
         }
     }
 }
