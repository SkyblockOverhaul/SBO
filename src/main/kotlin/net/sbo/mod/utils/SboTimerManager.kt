package net.sbo.mod.utils

import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

object SboTimerManager {
    internal val activeTimers = CopyOnWriteArraySet<SBOTimer>()
    val timerMayor = SBOTimer(
        tracker = SboDataObject.dianaTrackerMayor
    )
    val timerTotal = SBOTimer(
        tracker = SboDataObject.dianaTrackerTotal
    )
    val timerSession = SBOTimer(
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

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        activeTimers.forEach { it.pause() }
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

        private fun getInactivityLimitNanos(): Long {
            return TimeUnit.SECONDS.toNanos(
                Diana.afkTimeout.toLong().coerceAtLeast(15L)
            )
        }

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

        fun tick() {
            if (state != TimerState.Running) return

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

                Chat.chat("§6[SBO] §ePausing playtime timer due to inactivity threshold of ${TimeUnit.NANOSECONDS.toSeconds(inactivityLimit)} seconds being reached.")
                pause()
            }
        }

        private fun updateElapsedTime(now: Long = System.nanoTime()) {
            if (state != TimerState.Running) return

            elapsedNanoTime += now - startNanoTime
            startNanoTime = now

            tracker.items.TIME = TimeUnit.NANOSECONDS.toMillis(elapsedNanoTime)
        }

        fun pause() {
            if (state != TimerState.Running) return

            updateElapsedTime()

            state = TimerState.Paused

            stopInactivityCheck()
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

        fun getHourTime(): Double {
            return getElapsedNanos() / TimeUnit.HOURS.toNanos(1L).toDouble()
        }

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
