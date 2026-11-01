package net.sbo.mod.diana.tracker

import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.overlays.DianaStats
import net.sbo.mod.settings.Settings
import net.sbo.mod.settings.categories.Trackers
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.medal.MedalIntegration

internal object Commands {
    fun register() {
        Register.command("sboresetsession") {
            DianaLoot.resetSession()
            Chat.chat("§6[SBO] §aDiana session tracker has been reset.")
        }

        Register.command("sbolegacylook") { args ->
            val legacy = when (args.firstOrNull()?.lowercase()) {
                "yes" -> true
                "no" -> false
                else -> {
                    Chat.chat("§6[SBO] §7Use §e/sbolegacylook yes §7for the old tracker look or §e/sbolegacylook no §7to keep the new one.")
                    return@command
                }
            }
            sboData.trackerLookAnswered = true
            DataManager.save(DataManager::sboData)
            if (legacy) {
                Trackers.legacyTrackers = Trackers.TrackerLook.entries.toSet()
                Settings.save()
                Chat.chat("§6[SBO] §aAll trackers use the legacy look now. §7Change it per tracker in Trackers > Tracker Legacy Look.")
            } else {
                Chat.chat("§6[SBO] §aKeeping the new tracker look. §7You can still switch in Trackers > Tracker Legacy Look.")
            }
            // The notice goes away on the next refresh of the trackers
            Trackers.onTrackerLookChanged()
        }

        Register.command("sboresetmayortracker") {
            MayorReset.reset(true)
        }

        Register.command("sboresetstatstracker") {
            sboData.mobsSinceInq = 0
            sboData.inqsSinceChim = 0
            sboData.minotaursSinceStick = 0
            sboData.champsSinceRelic = 0
            sboData.inqsSinceLsChim = 0
            DataManager.save(DataManager::sboData)
            SboTimerManager.timerSession.reset()
            DianaStats.updateLines()
        }

        Register.command("sbotestmedalclip", "sbomedaltest") { args ->
            MedalIntegration.testClip(args.firstOrNull())
        }
    }
}
