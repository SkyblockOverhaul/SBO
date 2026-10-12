package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.ArrayDeque

object TickScheduler {
    private val tasks = ArrayDeque<ScheduledTask>()

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            var remaining = tasks.size

            while (remaining > 0) {
                remaining--

                val task = tasks.pollFirst()
                    ?: break

                if (!task.tick()) {
                    tasks.addLast(task)
                }
            }
        }
    }

    fun schedule(task: ScheduledTask) {
        tasks.addLast(task)
    }

    data class ScheduledTask(
        private val interval: Int,
        private var counter: Int = 0,
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
