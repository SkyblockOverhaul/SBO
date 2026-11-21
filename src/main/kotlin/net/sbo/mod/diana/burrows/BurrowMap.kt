package net.sbo.mod.diana.burrows

import net.fabricmc.loader.api.FabricLoader
import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.GameCloseEvent
import net.sbo.mod.utils.math.SboVec
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** How many burrows this player dug from each block. Only the mod writes it, there is no command or menu to change it. */
object BurrowMap {
    private const val SAVE_TICKS = 200

    private val store = BurrowMapFile(FabricLoader.getInstance().gameDir.resolve(".sbo").resolve("burrow_map.bin"))
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "SBO-BurrowMap").apply { isDaemon = true } }
    private var pending = HashMap<Long, Int>()

    fun init() {
        writer.execute { runCatching { store.compact() }.onFailure { SBOKotlin.logger.error("Could not read the burrow map", it) } }
        Register.onTick(SAVE_TICKS) { flush() }
    }

    fun record(pos: SboVec) {
        val y = pos.y.toInt()
        if (y !in 0..255) return
        pending.merge(BurrowMapFile.key(pos.x.toInt(), y, pos.z.toInt()), 1, Int::plus)
    }

    /** This player's whole map in the file format, for sending to the backend. */
    fun export(): CompletableFuture<ByteArray> = flush().thenApplyAsync({ store.export() }, writer)

    @SboEvent
    fun onGameClose(event: GameCloseEvent) {
        runCatching { flush().get(5, TimeUnit.SECONDS) }
    }

    private fun flush(): CompletableFuture<Void> {
        if (pending.isEmpty()) return CompletableFuture.completedFuture(null)
        val digs = pending
        pending = HashMap()
        return CompletableFuture.runAsync({
            try {
                store.add(digs)
            } catch (e: Exception) {
                SBOKotlin.logger.error("Could not save the burrow map", e)
                // Tried again on the next save
                SBOKotlin.mc.execute { digs.forEach { (block, n) -> pending.merge(block, n, Int::plus) } }
            }
        }, writer)
    }
}
