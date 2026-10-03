package net.sbo.mod.partyfinder

import gg.essential.universal.utils.toFormattedString
import kotlinx.serialization.json.Json
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.PartyBody
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.Problem
import net.sbo.mod.partyfinder.api.RolesBody
import net.sbo.mod.settings.categories.PartyFinder
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderRefreshListEvent
import net.sbo.mod.utils.http.Http.getInt
import net.sbo.mod.utils.http.SboApi
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object PartyFinderManager {
    const val NOTE_MAX_LENGTH = 100
    // The party list shows two lines of a note
    const val NOTE_MAX_LINES = 2
    private val JOIN_REQUEST_COOLDOWN = TimeUnit.MINUTES.toNanos(1)

    var creatingParty = false
    var inQueue = false
    private var updateBool = false
    private var requeue = false
    private var ghostParty = false
    var usedPf = false

    /** The party the player queued last, also used for requeueing. */
    var draft: PartyDraft? = null
        private set

    /** The queued party as the backend answered it. */
    var queuedParty: PartyView? = null
        private set

    private val partySize: Int get() = draft?.partySize ?: 0
    private var partyMemberCount = 0
    private var partyMember: List<String> = emptyList()
    private var isLeader = false
    var isInParty = false

    // Roles players picked when they asked to join, by uuid without dashes
    private val memberRoles = mutableMapOf<String, String>()
    private val playersSentRequest = mutableMapOf<String, Long>()

    private val draftJson = Json { ignoreUnknownKeys = true }

    private val partyDisbandRegexes = listOf(
        Regex("^.+ §r§ehas disbanded the party!$"),
        Regex("^§r§cThe party was disbanded because (.+)$"),
        Regex("^§r§eYou left the party.$"),
        Regex("^§r§cYou are not currently in a party.$"),
        Regex("^§r§eYou have been kicked from the party by .+$"),
    )

    private val leaderChangeRegexes = listOf(
        Regex("^§r§eYou have joined §r(.+)'s* §r§eparty!$"),
        Regex("^§r§eThe party was transferred to §r(.+) §r§eby §r.+$"),
        Regex("^(.+)§r§e has promoted §r(.+) §r§eto Party Leader$")
    )

    private val ownJoinRegex = Regex("^§r§eYou have joined §r(.+)'s? §r§eparty!$")

    /** Goes up when the player joined a party, the open party list reloads then. */
    @Volatile
    var joinedParties = 0
        private set

    private val partyJoinRegexes = listOf(
        Regex("^(.+) §r§ejoined the party.$"),
        Regex("^§r§eYou have joined §r(.+)'s? §r§eparty!$")
    )

    private val partyLeaveRegexes = listOf(
        Regex("^(.+) §r§ehas been removed from the party.$"),
        Regex("^(.+) §r§ehas left the party.$"),
        Regex("^(.+) §r§ewas removed from your party because they disconnected.$"),
        Regex("^§r§eKicked (.+) because they were offline.$")
    )

    /** Set while the party finder GUI is open: results go there as toasts instead of the chat hidden behind it. */
    @Volatile
    var listener: ((success: Boolean, text: String) -> Unit)? = null

    // Toast while the GUI is open, chat otherwise
    private fun tell(text: String, success: Boolean) {
        val gui = listener
        if (gui != null) gui(success, text.replace(Regex("§."), "").removePrefix("[SBO] ")) else Chat.chat(text)
    }

    fun hasSboKey(): Boolean {
        val sboKey = SboKey.get()
        if (sboKey.isBlank() || !sboKey.startsWith("sbo")) {
            tell("§cPlease set your SBO key with /sboKey <key>, if you don't have one, get it in our discord.", false)
            return false
        }
        return true
    }

    private fun myUuid(): String = OwnStats.uuid()

    fun init() {
        Register.command("sborequeue") {
            val last = draft
            if (inQueue) {
                Chat.chat("§6[SBO] §eYour party is already in the queue.")
            } else if (last == null) {
                Chat.chat("§6[SBO] §4There is no party to requeue yet.")
            } else {
                Chat.chat("§6[SBO] §eRequeuing party with last used requirements...")
                createParty(last)
            }
        }

        Register.command("sbodequeue") {
            if (inQueue) {
                usedPf = false
                removePartyFromQueue()
            } else {
                Chat.chat("§6[SBO] §4You are not in a party queue.")
            }
        }

        Register.command("sboKey", "sbokey") { args ->
            if (args.isEmpty()) {
                Chat.chat("§6[SBO] §cPlease provide a key")
            } else if (args[0].startsWith("sbo").not()) {
                Chat.chat("§6[SBO] §cInvalid key format! get one in our Discord")
            } else {
                SboKey.set(args[0])
                Chat.chat("§6[SBO] §aKey has been set")
            }
        }

        Register.command("sboClearKey") {
            SboKey.clear()
            Chat.chat("§6[SBO] §aKey has been cleared")
        }

        Register.onChatMessageCancelable(
            Pattern.compile("§d(.*?) (.*?)§7: (.*?) join party request - id:(.*)", Pattern.DOTALL)
        ) { _, matchResult ->
            if ("From" in matchResult.group(1) && partyMemberCount < partySize) {
                val playerName = Helper.getPlayerName(matchResult.group(2) ?: "no name")
                val request = JoinRequest.parse(matchResult.group(4) ?: "")
                if (PartyFinder.autoInvite) {
                    invitePlayerIfMeetsReqs(playerName, request)
                } else {
                    showJoinRequest(playerName, request.role)
                }
            }
            false
        }

        Register.onChatMessageCancelable(
            Pattern.compile("^§9§m(.*?) §ehas invited you to join their party!(.*?)$", Pattern.DOTALL)
        ) { _, matchResult ->
            val playername = Helper.getPlayerName(matchResult.group(1) ?: "")
            if (playersSentRequest.containsKey(playername)) {
                Chat.chat("§6[SBO] §eJoining party of §b$playername§e...")
                Chat.command("p accept $playername")
                playersSentRequest.remove(playername)
            }
            true
        }

        Register.onTick(20 * 60 * 4) { // every 4 minutes
            if (!inQueue || !hasSboKey()) return@onTick
            PartyFinderApi.refreshParty(onError = { error ->
                if (error.code == PfError.PARTY_NOT_FOUND || error.code == PfError.PARTY_TOO_OLD || error.code == PfError.INVALID_KEY) {
                    inQueue = false
                    queuedParty = null
                    Chat.chat("§6[SBO] §4Your party left the queue: ${ProblemText.error(error)}")
                }
            }) {}
        }

        HypixelModApi.onPartyInfo { isInParty, isLeader, members ->
            this.isInParty = isInParty
            this.isLeader = isLeader
            this.partyMember = members
            partyMemberCount = members.size
            queueParty()
            updateParty()
        }

        HypixelModApi.onError { packet ->
            if (packet.type() == PartyInfoS2CPacket.ID) {
                creatingParty = false
                updateBool = false
            }
        }
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        if (inQueue) {
            removePartyFromQueue()
        }
    }

    /** Queues [newDraft]. Size and note are fitted to the category, the definitions are loaded when needed. */
    fun createParty(newDraft: PartyDraft) {
        if (creatingParty) return
        if (!hasSboKey()) return
        PartyCategories.get { data ->
            val target = data?.let { PartyCategories.target(newDraft.partyType, newDraft.subType) }
            if (target == null) {
                tell("§6[SBO] §4Could not load the party types from the SBO server. Please try again later.", false)
                return@get
            }
            if (!target.createOpen) {
                val text = if (target.opensAt == null) "can only be created while the event is running" else "can be listed from one hour before the event starts"
                tell("§6[SBO] §4${target.label} parties $text.", false)
                return@get
            }
            draft = newDraft.copy(
                partyType = target.partyType,
                subType = target.subType,
                partySize = target.clampSize(newDraft.partySize),
                note = checkPartyNote(newDraft.note)
            )
            usedPf = true
            mc.execute { HypixelModApi.sendPartyInfoPacket(createParty = true) }
        }
    }

    private fun partyBody(current: PartyDraft): PartyBody {
        val uuids = partyMember.map { it.replace("-", "") }
        return PartyBody(
            partyType = current.partyType,
            subType = current.subType,
            version = PartyCategories.version ?: "",
            uuids = uuids,
            partySize = current.partySize,
            note = current.note,
            reqs = current.reqs.mapNotNull { (stat, value) ->
                runCatching { stat to draftJson.parseToJsonElement(value) }.getOrNull()
            }.toMap(),
            options = current.options,
            roles = RolesBody(current.wantedRoles, memberRoles.filterKeys { it in uuids })
        )
    }

    /** Creates or updates the party; outdated definitions are reloaded and the call is tried once more. */
    private fun sendParty(update: Boolean, retried: Boolean = false, onError: (PfError) -> Unit, onSuccess: (PartyView) -> Unit) {
        val current = draft ?: return
        val body = partyBody(current)
        val retry: (PfError) -> Unit = { error ->
            if (error.code == PfError.DEFINITIONS_OUTDATED && !retried) {
                PartyCategories.get(force = true) { sendParty(update, true, onError, onSuccess) }
            } else {
                onError(error)
            }
        }
        if (update) PartyFinderApi.updateParty(body, retry, onSuccess)
        else PartyFinderApi.createParty(body, retry, onSuccess)
    }

    private fun queueParty() {
        if (!this.creatingParty) return
        creatingParty = false
        if (partyMember.size > partySize) {
            tell("§6[SBO] §4Party is over the limit. ${partyMember.size}/$partySize", false)
            return
        }
        if (inQueue) {
            tell("§6[SBO] §4Party is already in the queue.", false)
            return
        }
        if (!isLeader) {
            tell("§6[SBO] §4You must be the party leader to queue the party.", false)
            return
        }

        val startTime = System.nanoTime()
        sendParty(update = false, onError = { error ->
            reportFailure("create party", error)
        }) { party ->
            val timeTaken = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            inQueue = true
            queuedParty = party
            SBOEvent.emit(PartyFinderRefreshListEvent())

            if (ghostParty) {
                removePartyFromQueue()
                ghostParty = false
            }

            if (requeue) {
                requeue = false
                Chat.clickableChat("§6[SBO] §eClick to dequeue party", "Dequeue Party", "/sbodequeue")
            }

            tell("§6[SBO] §aParty created successfully! Time taken: ${timeTaken}ms", true)
            Chat.chat("§6[SBO] §ePlease note that for people to be able to join your party, you MUST set direct message privacy to \"Anyone\" in /settings -> Social Settings in the Hypixel Lobby. If you have already done so, you can click to hide this message.", true)

            if (isInParty) Chat.pc("[SBO] Party now in queue.")
        }
    }

    private fun updateParty() {
        if (!this.updateBool) return
        updateBool = false
        if (!inQueue || !isInParty || !isLeader) return
        if (partyMember.size !in 2..<partySize) return
        val startTime = System.nanoTime()
        sendParty(update = true, onError = { error ->
            inQueue = false
            queuedParty = null
            reportFailure("update party", error)
        }) { party ->
            queuedParty = party
            val timeTaken = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            tell("§6[SBO] §eParty updated successfully! Time taken: ${timeTaken}ms", true)
        }
    }

    private fun reportFailure(action: String, error: PfError) {
        val gui = listener
        if (gui != null) {
            val target = draft?.let { PartyCategories.target(it.partyType, it.subType) }
            val details = error.problems.joinToString("") { "\n${it.name}: ${ProblemText.describe(it, target)}" }
            val tip = if (error.code == PfError.REQS_NOT_MET) "\n${ProblemText.MEMBERS_RELOAD_HINT}" else ""
            gui(false, "Failed to $action: ${ProblemText.error(error)}$details$tip")
            return
        }
        Chat.chat("§6[SBO] §4Failed to $action: ${ProblemText.error(error)}")
        printProblems(error.problems)
    }

    private fun printProblems(problems: List<Problem>) {
        val target = draft?.let { PartyCategories.target(it.partyType, it.subType) }
        problems.forEach { Chat.chat("§7• §b${it.name}§7: §c${ProblemText.describe(it, target)}") }
    }

    fun listParties(
        partyType: String,
        subType: String = "",
        onComplete: ((List<PartyView>) -> Unit)? = null,
        onError: ((PfError) -> Unit)? = null
    ) {
        PartyFinderApi.parties(partyType, subType, onError = { error ->
            // The open GUI shows the error in the list
            if (listener == null) Chat.chat("§6[SBO] §4Failed to get parties: ${ProblemText.error(error)}")
            onError?.invoke(error)
        }) { parties -> onComplete?.invoke(parties) }
    }

    /** Number of SBO users that sent a request in the last 5 minutes. */
    fun getActiveUsers(
        onError: ((Exception) -> Unit)? = null,
        onComplete: (Int) -> Unit
    ) {
        SboApi.activeUsers().toJsonObject { response ->
            val count = response.getInt("activeUsers")
            if (count != null) onComplete(count) else onError?.invoke(Exception("No activeUsers in the response"))
        }.error { error -> onError?.invoke(error) }
    }

    private fun showJoinRequest(playerName: String, role: String?) {
        val roleText = role?.let { id ->
            val label = draft?.let { PartyCategories.target(it.partyType, it.subType) }?.roles?.firstOrNull { it.id == id }?.label ?: id
            " as §b$label"
        } ?: ""
        Chat.chat(Chat.getChatBreak())
        Chat.chat(
            Chat.textComponent("§6[SBO] §b$playerName §ewants to join your party$roleText§e.\n"),
            Chat.textComponent("§7[§aInvite§7]", "/p $playerName", "/p invite $playerName"),
            Chat.textComponent(" §7[§eCheck Stats§7]", "/sboc $playerName", "/sbocheck $playerName"),
        )
        Chat.chat(Chat.getChatBreak())
    }

    // todo: add a way to prevent inviting more player then party has space (maybe every user has 10 seconds to accept else next player gets invited)
    private fun invitePlayerIfMeetsReqs(playerName: String, request: JoinRequest) {
        val current = draft ?: return showJoinRequest(playerName, request.role)
        // The sender's name comes from Hypixel, so it can't be faked like a uuid in the text
        PartyFinderApi.checkMembers(
            CheckBody(current.partyType, current.subType, names = listOf(playerName), partyId = myUuid(), role = request.role),
            onError = { error ->
                if (error.code == PfError.PARTY_FULL) return@checkMembers
                Chat.chat("§6[SBO] §eCould not check §b$playerName§e: ${ProblemText.error(error)}")
                showJoinRequest(playerName, request.role)
            }
        ) { data ->
            val member = data.members.firstOrNull()
            if (member == null || !member.name.equals(playerName, ignoreCase = true)) {
                showJoinRequest(playerName, request.role)
                return@checkMembers
            }
            if (data.problems.isNotEmpty()) {
                Chat.chat("§6[SBO] §b$playerName §ewants to join, but does not meet the requirements:")
                printProblems(data.problems)
                return@checkMembers
            }
            if (partyMemberCount < partySize) {
                request.role?.let { memberRoles[member.uuid] = it }
                Chat.command("p invite $playerName")
                Chat.chat("§6[SBO] §eInvited $playerName to the party.")
            }
        }
    }

    /** Asks the leader of [party] to invite the player. [role] is needed when the party asks for roles. */
    fun sendJoinRequest(party: PartyView, role: String? = null) {
        val leaderName = party.leader?.name?.takeIf { it.isNotBlank() } ?: return
        val target = PartyCategories.target(party.partyType, party.subType)
        if (target == null) {
            tell("§6[SBO] §4This party type is unknown. Please reopen the party finder.", false)
            return
        }
        if (ReqMatcher.isFull(party)) {
            tell("§6[SBO] §cThis party is already full.", false)
            return
        }
        if (party.roles.wanted.isNotEmpty() && role == null) {
            tell("§6[SBO] §cThis party asks for roles. Please pick the role you want to play.", false)
            return
        }
        val lastSent = playersSentRequest[leaderName]
        if (lastSent != null && System.nanoTime() - lastSent < JOIN_REQUEST_COOLDOWN) {
            tell("§6[SBO] §cYou have already sent a request to this player recently.", false)
            return
        }
        OwnStats.get(target, party.options, onError = { error ->
            tell("§6[SBO] §4Could not load your stats: ${ProblemText.error(error)}", false)
        }) { me ->
            val problems = ReqMatcher.checkJoin(party, target, me, role)
            if (problems.isNotEmpty()) {
                val gui = listener
                if (gui != null) {
                    gui(false, "You don't meet the requirements: " + problems.joinToString("; ") { ProblemText.describe(it, target) } +
                        "\n${ProblemText.OWN_RELOAD_HINT}")
                    return@get
                }
                Chat.chat("§6[SBO] §cYou don't meet the requirements to join this party:")
                problems.forEach { Chat.chat("§7• §c${ProblemText.describe(it, target)}") }
                return@get
            }
            tell("§6[SBO] §eSending join request to $leaderName...", true)
            Chat.command("msg $leaderName ${JoinRequest.message(role)}")
            playersSentRequest[leaderName] = System.nanoTime()
        }
    }

    fun removePartyFromQueue(onComplete: ((Boolean) -> Unit)? = null) {
        if (inQueue) {
            inQueue = false
            queuedParty = null
            if (!hasSboKey()) {
                onComplete?.invoke(false)
                return
            }
            PartyFinderApi.removeParty(onError = { error ->
                onComplete?.invoke(false)
                tell("§6[SBO] §4Failed to remove party from queue: ${ProblemText.error(error)}", false)
            }) {
                onComplete?.invoke(true)
                tell("§6[SBO] §eParty removed from queue.", true)
            }
        } else if (creatingParty) {
            ghostParty = true
        }
    }

    @SboEvent
    fun trackMemberRegister(event: ChatMessageEvent) {
        val text = event.message.toFormattedString()
        var match = false
        leaderChangeRegexes.forEach {
            if (it.matches(text)) {
                match = true
                isInParty = true
                isLeader = false
                removePartyFromQueue()
            }
        }
        partyDisbandRegexes.forEach {
            if (it.matches(text)) {
                creatingParty = false
                partyMemberCount = 1
                match = true
                isInParty = false
                memberRoles.clear()
                removePartyFromQueue()
            }
        }
        ownJoinRegex.matchEntire(text)?.let { joined ->
            // Hypixel already says it in chat, so only a toast while the GUI is open
            listener?.invoke(true, "You joined ${Helper.getPlayerName(joined.groupValues[1])}'s party.")
            // The leader's mod updates the listed party first
            sleep(3000) { joinedParties++ }
        }
        partyJoinRegexes.forEach {
            if (it.matches(text)) {
                updateBool = true
                partyMemberCount += 1
                match = true
                isInParty = true
            }
        }
        partyLeaveRegexes.forEach {
            if (it.matches(text)) {
                updateBool = true
                partyMemberCount -= 1
                match = true
                isInParty = partyMemberCount > 1
            }
        }
        if (match) trackMemberCount()
    }

    private fun trackMemberCount() {
        if (inQueue) {
            if (partyMemberCount >= partySize) {
                sleep(100) {
                    Chat.chat("§6[SBO] §4Party is full, removing from queue.")
                    removePartyFromQueue()
                }
            } else {
                updateBool = true
                sleep(200) {
                    if (updateBool) HypixelModApi.sendPartyInfoPacket()
                }
            }
        } else {
            if (!isInParty) return
            if (!isLeader) return
            val last = draft ?: return
            if (partyMemberCount < partySize && !creatingParty && !requeue && usedPf) {
                requeue = true
                sleep(200) {
                    if (PartyFinder.autoRequeue) {
                        Chat.chat("§6[SBO] §eRequeuing party with last used requirements...")
                        createParty(last)
                    } else {
                        Chat.clickableChat("§6[SBO] §eClick to requeue party with last used requirements.", "/sborequeue", "/sborequeue")
                    }
                }
            }
        }
    }

    /** Same filter as the backend: letters, digits, spaces, line breaks and ,.!?-_ */
    fun checkPartyNote(note: String): String {
        return limitNoteLines(note.replace(Regex("[^\\p{L}\\p{N}\\s,.!?\\-_]"), ""))
            .take(NOTE_MAX_LENGTH)
            .trim()
    }

    /** Line breaks after the second line become spaces, like pasting into the note field. */
    fun limitNoteLines(note: String): String {
        val lines = note.replace("\r\n", "\n").replace('\r', '\n').split('\n', limit = NOTE_MAX_LINES)
        return (lines.dropLast(1) + lines.last().replace('\n', ' ')).joinToString("\n")
    }
}

/**
 * The text of a join request whisper. `id:` is a random uuid like old mods send, so Hypixel never
 * sees the same message twice; who asks comes from the whisper's sender name.
 */
data class JoinRequest(val role: String?) {
    companion object {
        private val ROLE = Regex("\\brole:([a-z_]{1,32})")

        /** [text] is everything after `id:`. */
        fun parse(text: String): JoinRequest = JoinRequest(ROLE.find(text)?.groupValues?.get(1))

        fun message(role: String?, id: String = java.util.UUID.randomUUID().toString()): String =
            "[SBO] join party request - id:$id" + (role?.let { " role:$it" } ?: "")
    }
}
