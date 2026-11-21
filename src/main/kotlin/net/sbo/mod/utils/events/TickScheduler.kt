package net.sbo.mod.utils.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import net.sbo.mod.SBOKotlin.mc

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

    class ScheduledTask(
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

    private val SBO_CALLBACK_THREAD: ExecutorService = Executors.newThreadPerTaskExecutor(Thread
            .ofVirtual()
            .name("sbo-callback-thread-", 1) // sbo-callback-thread-1, sbo-callback-thread-2 etc. starting from 1 (second parameter)
            .factory() // virtual threads are daemon by default
    )

    /**
     * Sleeps for the specified number of milliseconds and then executes the callback on the client thread (for thread-safety).
     * The sleep itself is done in a separate thread to avoid blocking the client thread while the callback still runs on the client thread.
     *
     * @param milliseconds The number of milliseconds to sleep.
     * @param callback The function to execute after sleeping.
     */
    fun sleep(milliseconds: Long, callback: () -> Unit) {
        SBO_CALLBACK_THREAD.execute {
            Thread.sleep(milliseconds)
            mc.schedule {
                callback()
            }
        }
    }
}
