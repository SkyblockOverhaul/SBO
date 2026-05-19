package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.concurrent.CopyOnWriteArrayList

object TickScheduler {
    val tasks = CopyOnWriteArrayList<ScheduledTask>()

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            val iterator = tasks.iterator()
            while (iterator.hasNext()) {
                val task = iterator.next()
                task.counter++
                if (task.counter >= task.tick) {
                    task.action { iterator.remove() }
                    task.counter = 0
                }
            }
        }
    }

    data class ScheduledTask(
        val tick: Int,
        val action: (() -> Unit) -> Unit,
        var counter: Int = 0
    )
}
