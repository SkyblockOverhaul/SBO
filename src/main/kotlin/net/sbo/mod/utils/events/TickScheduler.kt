package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.concurrent.ConcurrentLinkedQueue

object TickScheduler {
    private val tasks = ConcurrentLinkedQueue<ScheduledTask>()

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            var remaining = tasks.size

            while (remaining > 0) {
                remaining--

                val task = tasks.poll()
                    ?: break

                if (!task.tick()) {
                    tasks.offer(task)
                }
            }
        }
    }

    fun schedule(task: ScheduledTask) {
        tasks.offer(task)
    }

    data class ScheduledTask(
        val interval: Int,
        var counter: Int = 0,
        val action: () -> Boolean
    ) {
        fun tick(): Boolean {
            counter++

            if (counter < interval) {
                return false
            }

            counter = 0
            return action()
        }
    }
}
