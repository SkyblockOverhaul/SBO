package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.gui.PartyFinderGui
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.World
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object PartyFinderReminder {
    const val DEFAULT_MINUTES = 60
    // Without new parties the next look is this much later, not every minute
    private const val RETRY_MS = 10 * 60 * 1000L

    // Fixed text: suppressed messages are matched by their text
    private const val TEXT = "§6[SBO] §eNew parties were listed in the Party Finder! "

    @Volatile
    private var checking = false
    private var nextCheck = 0L

    fun init() {
        Register.onTick(20 * 60) { check() }
    }

    fun markVisited() {
        DataManager.sboData.pfLastVisit = System.currentTimeMillis()
        DataManager.sboData.save()
    }

    private val message
        get() = arrayOf(
            Chat.textComponent(TEXT),
            Chat.textComponent("§a[Open]", "Open the Party Finder", "/sbopf"),
            Chat.textComponent(" §7[Dismiss]")
        )

    private val dismissed get() = DataManager.sboData.suppressedMessages.contains(message.joinToString("") { it.string })

    private fun blocked() = !World.isInSkyblock() || PartyFinderGui.isOpen || PartyFinderGui.onboardingActive || dismissed

    private fun check() {
        val minutes = DataManager.partyFinderConfigState.reminderMinutes
        val now = System.currentTimeMillis()
        if (minutes <= 0 || checking || now < nextCheck || blocked()) return
        val data = DataManager.sboData
        if (now - maxOf(data.pfLastVisit, data.pfLastReminder) < minutes * 60_000L) return

        checking = true
        countNewParties(data.pfLastVisit) { count ->
            mc.execute {
                checking = false
                if (count == 0) {
                    nextCheck = System.currentTimeMillis() + RETRY_MS
                } else if (!blocked()) {
                    Chat.chat(*message, dontShowAgain = true)
                    data.pfLastReminder = System.currentTimeMillis()
                    data.save()
                }
            }
        }
    }

    private fun countNewParties(since: Long, callback: (Int) -> Unit) {
        val favorites = DataManager.partyFinderConfigState.favorites.toList()
        if (favorites.isNotEmpty()) {
            count(favorites.map { it.substringBefore('/') to it.substringAfter('/', "") }, since, callback)
            return
        }
        PartyCategories.get { loaded ->
            count(loaded?.categories?.map { it.id to "" }.orEmpty(), since, callback)
        }
    }

    private fun count(targets: List<Pair<String, String>>, since: Long, callback: (Int) -> Unit) {
        if (targets.isEmpty()) return callback(0)
        val me = Player.accountUuid()
        // "kuudra" and "kuudra/infernal" can both be favorites, a party counts once
        val found = ConcurrentHashMap.newKeySet<String>()
        val left = AtomicInteger(targets.size)
        fun done() {
            if (left.decrementAndGet() == 0) callback(found.size)
        }
        targets.forEach { (type, sub) ->
            PartyFinderApi.parties(type, sub, onError = { done() }) { parties ->
                parties.filter { it.createdAt > since && it.id != me }.forEach { found += it.id }
                done()
            }
        }
    }
}
