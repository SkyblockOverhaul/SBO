package net.sbo.mod.utils

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.configs.diana.DianaTracker
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import java.util.concurrent.TimeUnit

internal fun getInactivityLimitNanos(): Long {
    return TimeUnit.SECONDS.toNanos(
        Diana.afkTimeout.toLong().coerceAtLeast(15L)
    )
}

object SboTimerManager {
    internal val activeTimers = mutableSetOf<SBOTimer>()
    val timerMayor = SBOTimer(
        tracker = DataManager.dianaTrackerMayorData
    )
    val timerTotal = SBOTimer(
        tracker = DataManager.dianaTrackerTotalData
    )
    val timerSession = SBOTimer(
        tracker = DataManager.dianaTrackerSessionData
    )

    fun init() {
        Register.onTick(1) {
            val iterator = activeTimers.iterator()
            while (iterator.hasNext()) {
                if (!iterator.next().tick()) iterator.remove()
            }
        }
    }

    private fun addTimer(timer: SBOTimer) {
        activeTimers.add(timer)
    }

    private fun removeTimer(timer: SBOTimer) {
        activeTimers.remove(timer)
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        pauseAllActiveTimers()
    }

    fun pauseAllActiveTimers() {
        val iterator = activeTimers.iterator()
        while (iterator.hasNext()) {
            iterator.next().pauseWithoutRemoving()
            iterator.remove()
        }
    }

    fun updateAllActivity() {
        SBOTimer.timerList.forEach { it.updateActivity() }
    }

    private sealed class TimerState {
        object Idle : TimerState()
        object Running : TimerState()
        object Paused : TimerState()
    }

    class SBOTimer(
        private val tracker: DianaTracker
    ) {
        companion object {
            val timerList = mutableListOf<SBOTimer>()
        }

        private var startNanoTime: Long = 0L
        private var elapsedNanoTime: Long = 0L
        private var state: TimerState = TimerState.Idle
        private var lastActivityNanoTime: Long = System.nanoTime()

        init {
            val storedTimeMs = tracker.items.TIME

            if (storedTimeMs > 0L) {
                elapsedNanoTime = TimeUnit.MILLISECONDS.toNanos(storedTimeMs)
            }

            timerList.add(this)
        }

        fun isRunning(): Boolean = state == TimerState.Running

        private fun start() {
            if (state == TimerState.Running || elapsedNanoTime > 0L) return

            val storedTimeMs = tracker.items.TIME

            if (storedTimeMs > 0L) {
                elapsedNanoTime = TimeUnit.MILLISECONDS.toNanos(storedTimeMs)
            }

            startNanoTime = System.nanoTime()
            lastActivityNanoTime = startNanoTime

            state = TimerState.Running

            startInactivityCheck()
        }

        fun tick(): Boolean {
            if (state != TimerState.Running) return false

            val now = System.nanoTime()
            val inactivityLimit = getInactivityLimitNanos()

            updateElapsedTime(now)

            if (now - lastActivityNanoTime > inactivityLimit) {
                elapsedNanoTime -= inactivityLimit

                if (elapsedNanoTime < 0L) {
                    elapsedNanoTime = 0L
                }

                tracker.items.TIME =
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanoTime)

                if (this == timerTotal) { // message is sent three times otherwise for each timer
                    Chat.chat("§6[SBO] §ePausing playtime timer due to inactivity threshold of ${TimeUnit.NANOSECONDS.toSeconds(inactivityLimit)} seconds being reached.", true)
                }

                pauseWithoutRemoving()
                return false
            }
            return true
        }

        private fun updateElapsedTime(now: Long = System.nanoTime()) {
            if (state != TimerState.Running) return

            elapsedNanoTime += now - startNanoTime
            startNanoTime = now

            tracker.items.TIME = TimeUnit.NANOSECONDS.toMillis(elapsedNanoTime)
        }

        fun pause() {
            pauseWithoutRemoving()
            removeTimer(this)
        }

        internal fun pauseWithoutRemoving() {
            if (state != TimerState.Running) return

            updateElapsedTime()

            state = TimerState.Paused

            // The manager removes this timer after the current iterator step.
        }

        private fun continueTimer() {
            if (state == TimerState.Running) return

            startNanoTime = System.nanoTime()
            lastActivityNanoTime = startNanoTime

            state = TimerState.Running

            startInactivityCheck()
        }

        fun reset() {
            state = TimerState.Idle

            elapsedNanoTime = 0L
            startNanoTime = 0L
            lastActivityNanoTime = System.nanoTime()

            tracker.items.TIME = 0L

            stopInactivityCheck()
        }

        private fun getElapsedNanos(): Long {
            return if (state == TimerState.Running) {
                elapsedNanoTime + (System.nanoTime() - startNanoTime)
            } else {
                elapsedNanoTime
            }
        }

        internal fun getHourTime(): Double = getElapsedNanos() / TimeUnit.HOURS.toNanos(1L).toDouble()

        fun updateActivity() {
            start()
            continueTimer()

            lastActivityNanoTime = System.nanoTime()
        }

        private fun startInactivityCheck() {
            addTimer(this)
        }

        private fun stopInactivityCheck() {
            removeTimer(this)
        }
    }
}
