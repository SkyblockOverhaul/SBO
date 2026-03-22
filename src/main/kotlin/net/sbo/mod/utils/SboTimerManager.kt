package net.sbo.mod.utils

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import java.lang.reflect.Field
import java.util.Locale

object SboTimerManager {
    internal val activeTimers = mutableListOf<SBOTimer>()
    val timerMayor = SBOTimer(
        name = "Mayor",
        inactiveTimeLimit = 1.5f,
        tracker = SboDataObject.dianaTrackerMayor
    )
    val timerTotal = SBOTimer(
        name = "Total",
        inactiveTimeLimit = 1.5f,
        tracker = SboDataObject.dianaTrackerTotal
    )
    val timerSession = SBOTimer(
        name = "Session",
        inactiveTimeLimit = 1.5f,
        tracker = SboDataObject.dianaTrackerSession
    )

    fun init() {
        Register.onTick(1) {
            activeTimers.toList().forEach { it.tick() }
        }
    }

    private fun addTimer(timer: SBOTimer) {
        if (!activeTimers.contains(timer)) {
            activeTimers.add(timer)
        }
    }

    private fun removeTimer(timer: SBOTimer) {
        activeTimers.remove(timer)
    }

    internal fun getTimer(name: String): SBOTimer? {
        return SBOTimer.timerList.find { it.name.lowercase() == name.lowercase() }
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        activeTimers.toList().forEach { it.pause() }
    }

    fun updateAllActivity() {
        SBOTimer.timerList.forEach { it.updateActivity() }
    }

    class SBOTimer(
        val name: String,
        inactiveTimeLimit: Float,
        private val tracker: DianaTracker
    ) {
        companion object {
            val timerList = mutableListOf<SBOTimer>()
        }

        private var startTime: Long = 0
        var elapsedTime: Long = 0
        var running: Boolean = false
        private var startedOnce: Boolean = false
        private var lastActivityTime: Long = System.currentTimeMillis()
        private val INACTIVITY_LIMIT: Long = (inactiveTimeLimit * 60 * 1000).toLong()
        private var inactivityFlag: Boolean = false

        init {
            timerList.add(this)
        }

        /**
         * Starts the timer.
         */
        fun start() {
            if (running || startedOnce) return

            startTime = System.currentTimeMillis()

            val storedTime = tracker.items.TIME
            if (storedTime > 0) {
                elapsedTime = storedTime
            }

            running = true
            startedOnce = true
            updateElapsedTime()
            startInactivityCheck()
        }

        /**
         * The method called by the TimerManager on every tick.
         */
        fun tick() {
            if (!running) return
            updateElapsedTime()
            if (System.currentTimeMillis() - lastActivityTime > INACTIVITY_LIMIT) {
                pause()
                if (!inactivityFlag) {
                    tracker.items.TIME = tracker.items.TIME - INACTIVITY_LIMIT
                    inactivityFlag = true
                }
            }
        }

        /**
         * Updates the elapsed time based on the time since the last update.
         */
        private fun updateElapsedTime() {
            if (!running) return
            val now = System.currentTimeMillis()
            elapsedTime += now - startTime
            startTime = now
            tracker.items.TIME = elapsedTime
        }

        /**
         * Pauses the timer.
         */
        fun pause() {
            if (!running) return
            updateElapsedTime()
            running = false
            stopInactivityCheck()
        }

        /**
         * Continues the timer from where it was paused.
         */
        fun continueTimer() {
            if (running) return
            if (inactivityFlag) {
                elapsedTime -= INACTIVITY_LIMIT
            }
            startTime = System.currentTimeMillis()
            running = true
            startInactivityCheck()
        }

        /**
         * Resets the timer to 0.
         */
        fun reset() {
            running = false
            startedOnce = false
            elapsedTime = 0
            startTime = 0
            tracker.items.TIME = 0
            stopInactivityCheck()
        }

        /**
         * Returns the elapsed time in hours.
         */
        fun getHourTime(): Double {
            val millisecondTime = tracker.items.TIME
            val formattedString = String.format(Locale.US, "%.6f", millisecondTime / 3600000.0)
            return formattedString.toDouble()
        }

        /**
         * Updates the last activity time to the current time.
         */
        fun updateActivity() {
            this.start()
            this.continueTimer()
            lastActivityTime = System.currentTimeMillis()
        }

        /**
         * Starts the inactivity check by adding this timer to the central manager.
         */
        private fun startInactivityCheck() {
            addTimer(this)
            inactivityFlag = false
        }

        /**
         * Stops the inactivity check by removing this timer from the central manager.
         */
        private fun stopInactivityCheck() {
            removeTimer(this)
            inactivityFlag = false
        }
    }
}
