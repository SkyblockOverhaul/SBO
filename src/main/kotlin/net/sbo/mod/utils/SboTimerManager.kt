package net.sbo.mod.utils

import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.settings.categories.Diana
import java.util.concurrent.CopyOnWriteArraySet

object SboTimerManager {
    internal val activeTimers = CopyOnWriteArraySet<SBOTimer>()
    val timerMayor = SBOTimer(
        name = "Mayor",
        tracker = SboDataObject.dianaTrackerMayor
    )
    val timerTotal = SBOTimer(
        name = "Total",
        tracker = SboDataObject.dianaTrackerTotal
    )
    val timerSession = SBOTimer(
        name = "Session",
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
        return SBOTimer.timerList.find { it.name.equals(name, ignoreCase = true) }
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
        private val tracker: DianaTracker
    ) {
        companion object {
            val timerList = mutableListOf<SBOTimer>()
        }

        private var startTime: Long = 0
        var elapsedTime: Long = 0
        private var state: TimerState = TimerState.Idle
        private var lastActivityTime: Long = System.currentTimeMillis()
        private var inactivityFlag: Boolean = false

        private fun getInactivityLimitMs(): Long {
            return Diana.afkTimeout.toLong().coerceAtLeast(15L) * 1000L
        }

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
            if (now - lastActivityTime > getInactivityLimitMs()) {
                pause()
                if (!inactivityFlag) {
                    tracker.items.TIME -= getInactivityLimitMs()
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
                elapsedTime -= getInactivityLimitMs()
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
