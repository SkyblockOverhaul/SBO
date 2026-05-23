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
    internal val activeTimers = mutableSetOf<SBOTimer>()
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
            activeTimers.forEach { it.tick() }
        }
    }

    private fun addTimer(timer: SBOTimer) {
        activeTimers.add(timer)
    }

    private fun removeTimer(timer: SBOTimer) {
        activeTimers.remove(timer)
    }

    internal fun getTimer(name: String): SBOTimer? {
        return SBOTimer.timerList.find { it.name.lowercase() == name.lowercase() }
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        activeTimers.forEach { it.pause() }
    }

    fun updateAllActivity() {
        SBOTimer.timerList.forEach { it.updateActivity() }
    }

    sealed class TimerState {
        object Idle : TimerState()
        object Running : TimerState()
        object Paused : TimerState()
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
        private var state: TimerState = TimerState.Idle
        private var lastActivityTime: Long = System.currentTimeMillis()
        private val INACTIVITY_LIMIT_MS: Long = (inactiveTimeLimit * 60 * 1000).toLong()
        private var inactivityFlag: Boolean = false

        init {
            timerList.add(this)
        }

        fun isRunning(): Boolean = state == TimerState.Running

        fun start() {
            if (state == TimerState.Running || elapsedTime > 0) return

            startTime = System.currentTimeMillis()

            val storedTime = tracker.items.TIME
            if (storedTime > 0) {
                elapsedTime = storedTime
            }

            state = TimerState.Running
            updateElapsedTime()
            startInactivityCheck()
        }

        fun tick() {
            if (state != TimerState.Running) return
            val now = System.currentTimeMillis()
            updateElapsedTime(now)
            if (now - lastActivityTime > INACTIVITY_LIMIT_MS) {
                pause()
                if (!inactivityFlag) {
                    tracker.items.TIME = tracker.items.TIME - INACTIVITY_LIMIT_MS
                    inactivityFlag = true
                }
            }
        }

        private fun updateElapsedTime(now: Long = System.currentTimeMillis()) {
            if (state != TimerState.Running) return
            elapsedTime += now - startTime
            startTime = now
            tracker.items.TIME = elapsedTime
        }

        fun pause() {
            if (state != TimerState.Running) return
            updateElapsedTime()
            state = TimerState.Paused
            stopInactivityCheck()
        }

        fun continueTimer() {
            if (state == TimerState.Running) return
            if (inactivityFlag) {
                elapsedTime -= INACTIVITY_LIMIT_MS
            }
            startTime = System.currentTimeMillis()
            state = TimerState.Running
            startInactivityCheck()
        }

        fun reset() {
            state = TimerState.Idle
            elapsedTime = 0
            startTime = 0
            tracker.items.TIME = 0
            stopInactivityCheck()
        }

        fun getHourTime(): Double = elapsedTime / 3600000.0

        fun updateActivity() {
            this.start()
            this.continueTimer()
            lastActivityTime = System.currentTimeMillis()
        }

        private fun startInactivityCheck() {
            addTimer(this)
            inactivityFlag = false
        }

        private fun stopInactivityCheck() {
            removeTimer(this)
            inactivityFlag = false
        }
    }
}
