package net.sbo.mod.partyfinder

import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.SBOKotlin.LIVE_API_URL
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.MemberView
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.ChatMessageQueue
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Dev only: plays the Hypixel side (party packets, chat lines, whispers) so the party finder logic
 * can be tested against a local backend without joining a server. Remove with the old GUI cleanup (4f).
 *
 *   ./gradlew :26.1.2-fabric:runClient -PsboApiUrl=http://localhost:3000 -PsboPfSimulate=<sbo key of D4rkswift>
 *
 * Every step logs "PF SIM ok" or "PF SIM FAIL", the client closes when done.
 */
object PartyFinderSimulation {
    private const val D4RK = "33cd429790564c91a7579f56a3739431"
    private const val EMXA = "6099000dfd1e4153acc1d206f1a93433"
    private const val ROLEX = "88ab893d058c4f7ead8274a8385a7043"
    private const val HOT = "504b14df86ea44c1b04f54f747f47982"
    private val NAMES = mapOf(D4RK to "D4rkswift", EMXA to "Emxa", ROLEX to "RolexDE", HOT to "HotMenFeet")

    private val sent = CopyOnWriteArrayList<String>()
    private val local = CopyOnWriteArrayList<String>()
    private val members = CopyOnWriteArrayList<String>()
    private var failures = 0

    fun init() {
        val key = System.getProperty("sbo.pfSimulate")?.takeIf { it.startsWith("sbo") } ?: return
        if (API_URL == LIVE_API_URL) return
        Thread({ runCatching { run(key) }.onFailure { logger.error("[SBO] PF SIM crashed", it) }; finish() }, "SBO PF Simulation")
            .apply { isDaemon = true }
            .start()
    }

    private fun run(key: String) {
        waitFor("game started", 180_000) { mc.screen != null && mc.overlay == null }
        onMain { SboKey.set(key) }
        ChatMessageQueue.outgoingHook = { message -> sent += message; true }
        Chat.localMessageHook = { message -> local += message.replace(Regex("§."), "") }
        HypixelModApi.partyInfoOverride = { deliverParty() }
        OwnStats.uuidOverride = D4RK

        val categories = future { done -> PartyCategories.get(force = true) { done(it) } }
        check("categories loaded", categories != null) ?: return

        dianaFlows()
        outdatedDefinitions()
        closedEvent()
        roleFlows()
    }

    private fun dianaFlows() {
        // A requirement that D4rkswift, Emxa and HotMenFeet meet but RolexDE does not
        val stats = stats(D4RK, EMXA, ROLEX, HOT, type = "diana")
        val (stat, need) = listOf("dianaKills", "magicalPower", "sbLevel").firstNotNullOfOrNull { id ->
            val values = stats.associate { it.uuid to (ProblemText.value(id, it.stats[id]).replace(",", "").toDoubleOrNull() ?: 0.0) }
            val floor = listOf(D4RK, EMXA, HOT).minOf { values[it] ?: 0.0 }
            if (floor > 0 && (values[ROLEX] ?: 0.0) < floor) id to floor else null
        } ?: run { check("found a requirement RolexDE fails", false); return }
        logger.info("[SBO] PF SIM diana requirement $stat >= $need")

        setParty(D4RK, EMXA)
        val draft = PartyDraft(partyType = "diana", partySize = 4, note = "sim test", reqs = mutableMapOf(stat to need.toString()))
        onMain { PartyFinderManager.createParty(draft) }
        check("create: party queued", waitFor { PartyFinderManager.inQueue })
        check("create: backend has 2 members", waitFor { party("diana")?.memberCount == 2 })
        check("create: party chat told", waitFor { sent.any { it == "/pc [SBO] Party now in queue." } })

        whisper("HotMenFeet", "[SBO] join party request - id:$HOT")
        check("auto invite: HotMenFeet invited", waitFor { "/p invite HotMenFeet" in sent })
        joined(HOT)
        check("join: backend has 3 members", waitFor { party("diana")?.memberCount == 3 })

        val mark = local.size
        whisper("RolexDE", "[SBO] join party request - id:$ROLEX")
        check("auto invite: RolexDE not invited, leader told why", waitFor { since(mark).any { "RolexDE wants to join, but does not meet" in it } } && "/p invite RolexDE" !in sent)

        val oldMark = local.size
        whisper("RolexDE", "[SBO] join party request - id:${UUID.randomUUID()}")
        check("old mod request: invite buttons shown", waitFor { since(oldMark).any { "RolexDE wants to join your party" in it } } && "/p invite RolexDE" !in sent)

        val fakeMark = local.size
        whisper("Faker", "[SBO] join party request - id:$EMXA")
        check("wrong name: invite buttons, no invite", waitFor { since(fakeMark).any { "Faker wants to join your party" in it } } && "/p invite Faker" !in sent)

        left(HOT)
        check("leave: backend has 2 members", waitFor { party("diana")?.memberCount == 2 })

        joined(HOT)
        check("join again: backend has 3 members", waitFor { party("diana")?.memberCount == 3 })
        joined(ROLEX)
        check("full: removed from queue", waitFor { !PartyFinderManager.inQueue && party("diana") == null })

        left(ROLEX)
        check("requeue after a member left", waitFor(timeoutMs = 10_000) { PartyFinderManager.inQueue && party("diana")?.memberCount == 3 })

        incoming("§b[MVP§c+§b] D4rkswift §r§ehas disbanded the party!")
        setParty(D4RK)
        check("disband: removed from queue", waitFor { !PartyFinderManager.inQueue && party("diana") == null })
    }

    private fun outdatedDefinitions() {
        val real = PartyCategories.data ?: return
        PartyCategories.use(real.copy(version = "outdated"))
        setParty(D4RK, EMXA)
        onMain { PartyFinderManager.createParty(PartyDraft(partyType = "custom", partySize = 3)) }
        check("outdated definitions: reloaded and queued", waitFor { PartyFinderManager.inQueue && PartyCategories.version == real.version })
        onMain { PartyFinderManager.removePartyFromQueue() }
        check("dequeue: backend removed party", waitFor { party("custom") == null })
    }

    private fun closedEvent() {
        val closed = PartyCategories.categories.flatMap { c -> c.subcategories.filter { !it.open }.map { c.id to it.id } }.firstOrNull()
        if (closed == null) {
            logger.info("[SBO] PF SIM skipped closed event, every event is open right now")
            return
        }
        val mark = local.size
        onMain { PartyFinderManager.createParty(PartyDraft(partyType = closed.first, subType = closed.second, partySize = 4)) }
        check("closed event: refused in the mod", waitFor { since(mark).any { "can only be created while the event is running" in it } } && !PartyFinderManager.inQueue)
    }

    private fun roleFlows() {
        setParty(D4RK)
        onMain { PartyFinderManager.createParty(PartyDraft(partyType = "kuudra", subType = "basic", partySize = 4, wantedRoles = mutableListOf("dps"))) }
        check("kuudra: queued with wanted roles", waitFor { PartyFinderManager.inQueue })
        val kuudra = waitForValue { party("kuudra", "basic") } ?: run { check("kuudra: listed", false); return }

        // Emxa's side: asks to join
        OwnStats.uuidOverride = EMXA
        OwnStats.clear()
        val mark = local.size
        onMain { PartyFinderManager.sendJoinRequest(kuudra, null) }
        check("join without role: asked to pick one", waitFor { since(mark).any { "asks for roles" in it } })
        onMain { PartyFinderManager.sendJoinRequest(kuudra, "dps") }
        val leaderName = kuudra.leader?.name ?: "D4rkswift"
        val request = "/msg $leaderName [SBO] join party request - id:$EMXA role:dps"
        check("join with role: whisper sent", waitFor { request in sent })

        // Back to the leader: gets that whisper
        OwnStats.uuidOverride = D4RK
        OwnStats.clear()
        whisper("Emxa", request.removePrefix("/msg $leaderName "))
        check("role request: Emxa invited", waitFor { "/p invite Emxa" in sent })
        joined(EMXA)
        check("role request: backend stores Emxa as dps", waitFor { party("kuudra", "basic")?.members?.any { it.uuid == EMXA && it.role == "dps" } == true })

        val roleMark = local.size
        val sentMark = sent.size
        whisper("HotMenFeet", "[SBO] join party request - id:$HOT role:support")
        check("wrong role: not invited", waitFor { since(roleMark).any { "pick one of these roles" in it } } && "/p invite HotMenFeet" !in sent.drop(sentMark))

        onMain { PartyFinderManager.removePartyFromQueue() }
        check("kuudra: removed", waitFor { party("kuudra", "basic") == null })
    }

    // Fake Hypixel

    private fun setParty(vararg uuids: String) {
        members.clear()
        members.addAll(uuids)
    }

    private fun deliverParty() {
        val list = members.map { dashed(it) }
        mc.execute { HypixelModApi.deliverPartyInfo(list.size > 1, members.firstOrNull() == OwnStats.uuid(), list) }
    }

    private fun joined(uuid: String) {
        members += uuid
        incoming("§b[MVP§c+§b] ${NAMES[uuid]} §r§ejoined the party.")
    }

    private fun left(uuid: String) {
        members -= uuid
        incoming("§b[MVP§c+§b] ${NAMES[uuid]} §r§ehas left the party.")
    }

    private fun whisper(from: String, text: String) = incoming("§dFrom §b[MVP§c+§b] $from§7: §7$text")

    private fun incoming(text: String) = onMain { mc.chatListener.handleSystemMessage(Component.literal(text), false) }

    private fun dashed(uuid: String) = "${uuid.substring(0, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20)}"

    // Backend lookups

    private fun party(type: String, sub: String = ""): PartyView? = future<List<PartyView>?> { done ->
        PartyFinderApi.parties(type, sub, onError = { done(null) }) { done(it) }
    }?.firstOrNull { it.id == D4RK }

    private fun stats(vararg uuids: String, type: String): List<MemberView> = future<List<MemberView>?> { done ->
        PartyFinderApi.checkMembers(CheckBody(type, uuids = uuids.toList()), onError = { done(null) }) { done(it.members) }
    }.orEmpty()

    // Helpers

    private fun since(mark: Int) = local.drop(mark)

    private fun <T> future(start: ((T) -> Unit) -> Unit): T? {
        val result = CompletableFuture<T>()
        start { result.complete(it) }
        return runCatching { result.get(15, TimeUnit.SECONDS) }.getOrNull()
    }

    private fun onMain(block: () -> Unit) {
        val latch = CountDownLatch(1)
        mc.execute { try { block() } finally { latch.countDown() } }
        latch.await(5, TimeUnit.SECONDS)
    }

    private fun waitFor(label: String? = null, timeoutMs: Long = 8_000, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (runCatching(condition).getOrDefault(false)) return true
            Thread.sleep(500)
        }
        if (label != null) logger.warn("[SBO] PF SIM timed out waiting for $label")
        return false
    }

    private fun <T> waitForValue(timeoutMs: Long = 8_000, value: () -> T?): T? {
        var found: T? = null
        waitFor(null, timeoutMs) { value().also { found = it } != null }
        return found
    }

    private fun check(label: String, ok: Boolean): Unit? {
        if (!ok) failures++
        logger.info("[SBO] PF SIM ${if (ok) "ok  " else "FAIL"} $label")
        return if (ok) Unit else null
    }

    private fun finish() {
        logger.info("[SBO] PF SIM done, $failures failed")
        ChatMessageQueue.outgoingHook = null
        Chat.localMessageHook = null
        HypixelModApi.partyInfoOverride = null
        OwnStats.uuidOverride = null
        logger.info("[SBO] PF SIM sent: ${sent.joinToString(" | ")}")
        mc.execute { mc.stop() }
    }
}
