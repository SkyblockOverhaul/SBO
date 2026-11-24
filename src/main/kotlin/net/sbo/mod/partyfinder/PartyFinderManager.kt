package net.sbo.mod.partyfinder

import net.sbo.mod.utils.chat.toFormattedString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.profileviewer.ProfileViewerGui
import net.sbo.mod.partyfinder.api.CheckBody
import net.sbo.mod.partyfinder.api.CheckData
import net.sbo.mod.partyfinder.api.PartyBody
import net.sbo.mod.partyfinder.api.PartyFinderApi
import net.sbo.mod.partyfinder.api.PartyView
import net.sbo.mod.partyfinder.api.PfError
import net.sbo.mod.partyfinder.api.Problem
import net.sbo.mod.partyfinder.api.RolesBody
import net.sbo.mod.settings.Settings
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.TickScheduler.sleep
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboKey
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.ClickActionManager
import net.sbo.mod.utils.data.configs.partyfinder.PartyDraft
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderRefreshListEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http.getInt
import net.sbo.mod.utils.http.SboApi
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import net.sbo.mod.utils.TextUtil

object PartyFinderManager {
    const val NOTE_MAX_LENGTH = 100
    // The party list shows two lines of a note
    const val NOTE_MAX_LINES = 2
    private val JOIN_REQUEST_COOLDOWN = TimeUnit.SECONDS.toNanos(30)
    // Invited players join right away through SBO's auto accept, so after 10 seconds they won't join anymore
    private val INVITE_HOLD_MS = TimeUnit.SECONDS.toMillis(10)
    private val partyChatRegex = Regex("^§9[^§]+ §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")
    private val QUIET_QUEUE_HINT_AFTER = TimeUnit.MINUTES.toMillis(10)
    // The party message needs a moment to reach the player before the kick
    private const val HYPIXEL_KICK_DELAY_MS = 800L
    private const val HYPIXEL_KICK_SETTLE_MS = 1000L
    // Between two auto kicks, so Hypixel never gets a burst of party messages and kicks
    private const val AUTO_KICK_GAP_MS = 1500L
    // How long after an auto kick the end of the party still counts as caused by it
    private const val AUTO_KICK_DISBAND_WINDOW_MS = 5000L
    private const val UPDATE_RETRY_MS = 5000L
    private const val JOIN_PARTY_INFO_DELAY_MS = 1000L
    // Hypixel says "removed" and "disbanded" in either order, listing again waits for both
    private const val RELIST_DELAY_MS = 2000L
    // Switching servers leaves SkyBlock for a few seconds
    private const val PAUSE_OUTSIDE_SKYBLOCK_SECONDS = 30
    private const val PARTY_EMPTY = "the party was empty"
    // Errors that say nothing about the party itself
    private val TEMPORARY_ERRORS = setOf(
        PfError.NETWORK, PfError.BAD_RESPONSE, PfError.INTERNAL_ERROR, PfError.HYPIXEL_UNAVAILABLE, PfError.RATE_LIMITED, PfError.MOJANG_BUSY
    )
    private val LISTING_GONE_ERRORS = setOf(PfError.PARTY_NOT_FOUND, PfError.PARTY_TOO_OLD)

    /** The start of the chat message for each failure, the error follows after it. */
    private enum class Failure(val start: String) {
        NOT_QUEUED("Could not queue party:"),
        NOT_UPDATED("Could not update party, it stays queued."),
        REMOVED("Party removed from queue:")
    }

    var creatingParty = false
    var inQueue = false
    private var updateBool = false
    private var requeue = false
    // Set while a paused party is queued again by itself, not by the player
    private var relisting = false
    private var ghostParty = false
    internal var usedPf = false

    // Also used for requeueing
    var draft: PartyDraft? = null
        private set

    var queuedParty: PartyView? = null
        private set
    // The last party the server accepted, so an update that changes nothing is not sent
    private var lastSent: PartyBody? = null

    // New settings for a listed party, they replace draft once the server accepted them
    private var editDraft: PartyDraft? = null
    private var updateRetried = false

    private var paused: PausedParty? = null
    internal val isPaused: Boolean get() = paused != null
    private var secondsOutsideSkyblock = 0
    // Players whose requirements are being checked and who are still in the party
    private val joinsBeingChecked = mutableSetOf<String>()

    /** Party key and the requirements players missed when that party was last created or updated, for the create page. */
    @Volatile
    var createProblems: Pair<String, List<Problem>>? = null
        private set

    // When the party was queued and nobody joined since, 0 once someone joined or the hint was shown
    private var quietSince = 0L

    // Join requests come over the socket, private messages are only the fallback, so once per game start is enough
    private var dmHintShown = false

    // An update with a player who doesn't meet the requirements would end the listing, so updates wait for these checks
    private var hypixelJoinChecks = 0
    private var hypixelJoinChecked = false
    // Kicking the only other player ends the party on Hypixel, but the SBO listing goes on with the leader alone
    private var autoKicks = 0
    private val kickQueue = ArrayDeque<() -> Unit>()
    private var kicking = false
    // Lowercase names waiting for their kick, a player who leaves first is taken out
    private val pendingKicks = mutableSetOf<String>()
    private var kickSession = 0

    private val partySize: Int get() = draft?.partySize ?: 0
    private var partyMemberCount = 0
    val memberCount: Int get() = partyMemberCount
    private var partyMember: List<String> = emptyList()
    var isLeader = false
        private set
    var isInParty = false

    // Roles players picked when they asked to join, by uuid without dashes
    private val memberRoles = mutableMapOf<String, String>()
    // When a player was invited, by lowercase name. The place stays theirs until they join or INVITE_HOLD_MS is over
    private val invitedAt = mutableMapOf<String, Long>()
    // When a player was invited with a role, by uuid without dashes
    private val roleInvites = mutableMapOf<String, Long>()
    private val playersSentRequest = mutableMapOf<String, Long>()

    private val draftJson = Json { ignoreUnknownKeys = true }

    private val partyDisbandRegexes = listOf(
        Regex("^.+ §r§ehas disbanded the party!$"),
        Regex("^§r§cThe party was disbanded because (.+)$"),
        Regex("^§r§eYou left the party.$"),
        Regex("^§r§cYou are not currently in a party.$"),
        Regex("^§r§eYou have been kicked from the party by .+$"),
    )

    private val partyDisbandMatchers =
        partyDisbandRegexes.map { CachedPattern(it.toPattern()) }

    private val leaderChangeRegexes = listOf(
        Regex("^§r§eYou have joined §r(.+)'s* §r§eparty!$"),
        Regex("^§r§eThe party was transferred to §r(.+) §r§eby §r.+$"),
        Regex("^(.+)§r§e has promoted §r(.+) §r§eto Party Leader$")
    )

    private val leaderChangeMatchers =
        leaderChangeRegexes.map { CachedPattern(it.toPattern()) }

    private val ownJoinRegex = Regex("^§r§eYou have joined §r(.+)'s? §r§eparty!$")

    private val ownJoinMatcher =
        CachedPattern(ownJoinRegex.toPattern())

    /** Goes up when the player joined a party, the open party list reloads then. */
    @Volatile
    var joinedParties = 0
        private set

    private val partyJoinRegexes = listOf(
        Regex("^(.+) §r§ejoined the party.$"),
        Regex("^§r§eYou have joined §r(.+)'s? §r§eparty!$")
    )

    private val partyJoinMatchers =
        partyJoinRegexes.map { CachedPattern(it.toPattern()) }

    private val partyLeaveRegexes = listOf(
        Regex("^(.+) §r§ehas been removed from the party.$"),
        Regex("^(.+) §r§ehas left the party.$"),
        Regex("^(.+) §r§ewas removed from your party because they disconnected.$"),
        Regex("^§r§eKicked (.+) because they were offline.$")
    )

    private val partyLeaveMatchers =
        partyLeaveRegexes.map { CachedPattern(it.toPattern()) }

    /** Set while the party finder GUI is open: results go there as toasts instead of the chat hidden behind it. */
    @Volatile
    var listener: ((success: Boolean, text: String) -> Unit)? = null

    private class CachedPattern(pattern: Pattern) {
        private val matcher = pattern.matcher("")

        fun matches(input: String): Boolean =
            matcher.reset(input).matches()

        fun matchGroup(input: String, group: Int): String? {
            matcher.reset(input)
            return if (matcher.matches()) matcher.group(group) else null
        }
    }

    internal fun tell(text: String, success: Boolean) {
        val gui = listener
        if (gui != null) gui(success, text.replace(Regex("§."), "").removePrefix("[SBO] ")) else Chat.chat(text)
    }

    private fun myUuid(): String = OwnStats.uuid()

    private fun takeOverLegacySettings() {
        val old = Settings.legacyPartyFinder ?: return
        Settings.legacyPartyFinder = null
        val config = DataManager.partyFinderConfigState
        old.get("autoInvite")?.takeIf { it.isJsonPrimitive }?.let { config.autoInvite = it.asBoolean }
        old.get("autoRequeue")?.takeIf { it.isJsonPrimitive }?.let { config.autoRequeue = it.asBoolean }
        config.save()
    }

    fun init() {
        takeOverLegacySettings()
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
            } else if (paused != null) {
                usedPf = false
                paused = null
                Chat.chat("§6[SBO] §eYour party won't be requeued.")
            } else {
                Chat.chat("§6[SBO] §4You are not in a party queue.")
            }
        }

        Register.command("sboKey", "sbokey", argName = "key") { args ->
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
            Pattern.compile("^§dFrom (.*?)§7: .*? join party request - id:(.*)$")
        ) { _, matchResult ->
            if (freePlaces() > 0) {
                val playerName = TextUtil.getPlayerName(matchResult.group(1) ?: "no name")
                val request = JoinRequest.parse(matchResult.group(2) ?: "")
                if (!TextUtil.isPlayerName(playerName) || BlockedPlayers.isBlockedName(playerName)) return@onChatMessageCancelable false
                if (DataManager.partyFinderConfigState.autoInvite) {
                    invitePlayerIfMeetsReqs(playerName, request)
                } else {
                    val role = request.role
                    showJoinRequest(playerName, role)
                }
            }
            false
        }

        Register.onChatMessage(partyChatRegex) { _, match ->
            val parts = match.groupValues[2].trim().split(Regex("\\s+"))
            if (parts.firstOrNull()?.lowercase() != "!role" || parts.size != 2) return@onChatMessage
            onRoleCommand(TextUtil.getPlayerName(match.groupValues[1]), parts[1])
        }

        Register.onChatMessage(HypixelJoin.MESSAGE, noFormatting = true) { _, match ->
            val join = HypixelJoin.of(match)
            onHypixelGroupJoin(join.copy(player = TextUtil.getPlayerName(join.player)))
        }

        Register.onChatMessageCancelable(
            Pattern.compile("^§9§m(.*?) §ehas invited you to join their party!(.*?)$")
        ) { _, matchResult ->
            val playername = TextUtil.getPlayerName(matchResult.group(1) ?: "")
            if (TextUtil.isPlayerName(playername) && playersSentRequest.containsKey(playername)) {
                Chat.chat("§6[SBO] §eJoining party of §b$playername§e...")
                Chat.command("p accept $playername")
                playersSentRequest.remove(playername)
            }
            true
        }

        Register.onTick(20 * 60 * 4) { // every 4 minutes
            // The socket's pings keep the party listed while it is connected
            if (!inQueue || PartyFinderSocket.isConnected()) return@onTick
            PartyFinderApi.refreshParty(onError = { error ->
                if (error.code == PfError.PARTY_NOT_FOUND || error.code == PfError.PARTY_TOO_OLD || error.code == PfError.INVALID_KEY) {
                    onPartyRemoved(error)
                }
            }) {}
        }

        Register.onTick(20 * 310) { checkQuietQueue() }
        Register.onTick(20) { checkPause() }

        HypixelModApi.onPartyInfo { isInParty, isLeader, members ->
            this.isInParty = isInParty
            this.isLeader = isLeader
            this.partyMember = members
            partyMemberCount = members.size
            if (hypixelJoinChecked) {
                hypixelJoinChecked = false
                if (inQueue) trackMemberCount()
            }
            queueParty()
            updateParty()
        }

        HypixelModApi.onError { packet ->
            if (packet.type() == PartyInfoS2CPacket.ID) {
                creatingParty = false
                updateBool = false
                requeue = false
                relisting = false
                editDraft = null
            }
        }
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        paused = null
        joinsBeingChecked.clear()
        invitedAt.clear()
        // Kicks still waiting belong to the old server
        kickSession++
        kickQueue.clear()
        pendingKicks.clear()
        kicking = false
        hypixelJoinChecks = 0
        autoKicks = 0
        requeue = false
        relisting = false
        editDraft = null
        if (inQueue) {
            removePartyFromQueue()
        }
    }

    /** Queues newDraft, or updates the listed party with its settings. Size and note are fitted to the category, the definitions are loaded when needed. */
    fun createParty(newDraft: PartyDraft) {
        if (creatingParty) return
        PartyCategories.get { data ->
            val target = data?.let { PartyCategories.target(newDraft.partyType, newDraft.subType) }
            if (target == null) {
                relisting = false
                tell("§6[SBO] §4Could not load the party types from the SBO server. Please try again later.", false)
                return@get
            }
            if (!target.createOpen) {
                relisting = false
                val text = if (target.opensAt == null) "can only be created while the event is running" else "can be queued from one hour before the event starts"
                tell("§6[SBO] §4${target.label} parties $text.", false)
                return@get
            }
            val wanted = newDraft.deepCopy().apply {
                partyType = target.partyType
                subType = target.subType
                partySize = target.clampSize(newDraft.partySize)
                note = checkPartyNote(newDraft.note)
                wantedRoles = target.fittingRoles(ownRole, wantedRoles).toMutableList()
            }
            if (inQueue) {
                editDraft = wanted
            } else {
                editDraft = null
                draft = wanted
            }
            createProblems = null
            usedPf = true
            mc.execute { HypixelModApi.sendPartyInfoPacket(createParty = true) }
        }
    }

    internal fun reqsOf(current: PartyDraft): Map<String, JsonElement> =
        current.reqs.mapNotNull { (stat, value) ->
            runCatching { stat to draftJson.parseToJsonElement(value) }.getOrNull()
        }.toMap()

    private fun partyBody(current: PartyDraft, keepOnFail: Boolean): PartyBody {
        val uuids = partyMember.map { it.replace("-", "") }
        val own = current.ownRole?.takeIf { role -> PartyCategories.target(current.partyType, current.subType)?.roles?.any { it.id == role } == true }
        // The leader's place comes first; without places for others their role is only shown
        val wanted = if (own != null && current.wantedRoles.isNotEmpty()) listOf(own) + current.wantedRoles else current.wantedRoles
        val members = memberRoles.filterKeys { it in uuids } + listOfNotNull(own?.let { myUuid() to it })
        return PartyBody(
            partyType = current.partyType,
            subType = current.subType,
            version = PartyCategories.version.orEmpty(),
            uuids = uuids,
            partySize = current.partySize,
            note = current.note,
            reqs = reqsOf(current),
            options = current.options,
            keepOnFail = if (keepOnFail) true else null,
            roles = RolesBody(wanted, members)
        )
    }

    // A copy, so later changes to the draft don't change it too
    private fun comparable(body: PartyBody) = body.copy(keepOnFail = null, reqs = body.reqs.toMap(), options = body.options.toMap())

    /** Creates or updates the party. Outdated definitions are reloaded and the call is tried once more. Callbacks run on the client thread. */
    private fun sendParty(
        current: PartyDraft,
        update: Boolean,
        keepOnFail: Boolean,
        retried: Boolean = false,
        onError: (PfError) -> Unit,
        onSuccess: (PartyView) -> Unit
    ) {
        val body = partyBody(current, keepOnFail)
        val failed: (PfError) -> Unit = { error ->
            if (error.code == PfError.DEFINITIONS_OUTDATED && !retried) {
                PartyCategories.get(force = true) { mc.execute { sendParty(current, update, keepOnFail, true, onError, onSuccess) } }
            } else {
                mc.execute {
                    // The server may have the change or not, so the next update always goes out
                    lastSent = null
                    onError(error)
                }
            }
        }
        val done: (PartyView) -> Unit = { party ->
            mc.execute {
                lastSent = comparable(body)
                onSuccess(party)
            }
        }
        if (update) PartyFinderApi.updateParty(body, failed, done)
        else PartyFinderApi.createParty(body, failed, done)
    }

    private fun queueParty() {
        if (!this.creatingParty) return
        creatingParty = false
        val edited = editDraft
        editDraft = null
        val relisted = relisting
        relisting = false
        val wanted = edited ?: draft ?: return
        val editing = edited != null && inQueue
        if (!editing) draft = wanted
        if (partyMember.size > wanted.partySize) {
            tell("§6[SBO] §4Party is over the limit. ${partyMember.size}/${wanted.partySize}", false)
            return
        }
        if (inQueue && !editing) {
            tell("§6[SBO] §4Party is already in the queue.", false)
            return
        }
        if (!isLeader) {
            tell("§6[SBO] §4You must be the party leader to queue the party.", false)
            return
        }

        val startTime = System.nanoTime()
        // Creating a party the server already lists updates it, after the same checks as a new one
        sendParty(wanted, update = false, keepOnFail = editing, onError = { error ->
            onQueueFailed(wanted, editing, relisted, error)
        }) { party ->
            // Removed while the edit was on its way
            if (editing && !inQueue) {
                PartyFinderApi.removeParty(onError = {}) {}
                return@sendParty
            }
            val timeTaken = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            draft = wanted
            paused = null
            updateRetried = false
            inQueue = true
            queuedParty = party
            quietSince = System.currentTimeMillis()
            SBOEvent.emit(PartyFinderRefreshListEvent())
            if (party.partyType == "kuudra") HypixelPartyFinder.onListedOnSbo()

            if (ghostParty) {
                removePartyFromQueue()
                ghostParty = false
            }

            if (editing) {
                tell("§6[SBO] §aParty updated successfully! Time taken: ${timeTaken}ms", true)
                if (isInParty) Chat.pc("[SBO] Party updated.")
                return@sendParty
            }
            if (relisted) {
                Chat.clickableChat("§6[SBO] §aParty requeued. §eClick to remove it from queue.", "/sbodequeue", "/sbodequeue")
            } else {
                tell("§6[SBO] §aParty created successfully! Time taken: ${timeTaken}ms", true)
                showDmHint()
            }
            if (isInParty) Chat.pc("[SBO] Party now in queue.")
        }
    }

    private fun onQueueFailed(wanted: PartyDraft, editing: Boolean, relisted: Boolean, error: PfError) {
        if (error.problems.isNotEmpty()) createProblems = wanted.key to error.problems
        if (editing) {
            if (error.code in LISTING_GONE_ERRORS) {
                dropListing()
            } else if (inQueue && (error.code == PfError.NETWORK || error.code == PfError.BAD_RESPONSE)) {
                // The change may have reached the server, so it gets the settings from before again
                updateBool = true
                HypixelModApi.sendPartyInfoPacket()
            }
            reportFailure(if (inQueue) Failure.NOT_UPDATED else Failure.REMOVED, error)
            return
        }
        if (relisted && error.code == PfError.REQS_NOT_MET) {
            val others = error.problems.groupBy { it.name }.filterKeys { !it.equals(Player.getName(), ignoreCase = true) }
            if (others.isNotEmpty()) {
                val pause = PausedParty(wanted.deepCopy(), reqsOf(wanted), wanted.options.toMap())
                paused = pause
                others.forEach { (name, problems) ->
                    pause.add(name)
                    warnHypixelJoin(name, problems, relistFailed = true)
                }
                return
            }
        }
        reportFailure(Failure.NOT_QUEUED, error)
    }

    private fun dropListing() {
        inQueue = false
        queuedParty = null
        PartyFinderApi.removeParty(onError = {}) {}
    }

    private fun updateParty() {
        if (!this.updateBool || hypixelJoinChecks > 0) return
        updateBool = false
        // Without a party the leader is alone, which happens when auto kick removed the only other player
        if (!inQueue || !isLeader) return
        if (partyMember.size !in 1..<partySize) return
        val current = draft ?: return
        // Nothing changed, e.g. auto kick removed the player who just joined, so the listing is still right
        if (comparable(partyBody(current, keepOnFail = true)) == lastSent) return
        val startTime = System.nanoTime()
        sendParty(current, update = true, keepOnFail = true, onError = ::onUpdateFailed) { party ->
            // Removed while the update was on its way
            if (!inQueue) {
                if (!creatingParty) PartyFinderApi.removeParty(onError = {}) {}
                return@sendParty
            }
            updateRetried = false
            queuedParty = party
            val timeTaken = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            tell("§6[SBO] §eParty updated successfully! Time taken: ${timeTaken}ms", true)
        }
    }

    private fun onUpdateFailed(error: PfError) {
        if (!inQueue) return
        if (error.code in TEMPORARY_ERRORS) {
            if (updateRetried) {
                updateRetried = false
                reportFailure(Failure.NOT_UPDATED, error)
                return
            }
            updateRetried = true
            sleep(UPDATE_RETRY_MS) {
                if (!inQueue) return@sleep
                updateBool = true
                HypixelModApi.sendPartyInfoPacket()
            }
            return
        }
        updateRetried = false
        dropListing()
        reportFailure(Failure.REMOVED, error)
    }

    private fun showDmHint() {
        if (dmHintShown) return
        dmHintShown = true
        // Hidden messages are saved by their text, and older versions had a longer text for this one
        if (DataManager.sboData.suppressedMessages.any { "direct message privacy" in it }) return
        Chat.chat("§6[SBO] §eTo get all join requests, set direct messages to \"Anyone\" in /settings -> Social Settings in the Hypixel Lobby. §7Click to hide this message.", true)
    }

    private fun currentTarget(): PartyTarget? = draft?.let { PartyCategories.target(it.partyType, it.subType) }

    /** Places that are neither taken by party members nor held for a player invited in the last 10 seconds. */
    private fun freePlaces(): Int {
        val now = System.currentTimeMillis()
        invitedAt.values.removeIf { now - it >= INVITE_HOLD_MS }
        return partySize - partyMemberCount - invitedAt.size
    }

    private fun holdPlace(playerName: String) {
        invitedAt[playerName.lowercase()] = System.currentTimeMillis()
    }

    /** The role has a free place, counting players in the party and the ones invited in the last 10 seconds. */
    private fun roleOpen(role: String, except: String? = null): Boolean {
        val wanted = draft?.wantedRoles.orEmpty()
        if (wanted.isEmpty()) return true
        val now = System.currentTimeMillis()
        val inParty = partyMember.asSequence().map { it.replace("-", "") }.toSet()
        val holders = memberRoles.count { (uuid, r) ->
            r == role && uuid != except && (uuid in inParty || now - (roleInvites[uuid] ?: 0L) < INVITE_HOLD_MS)
        }
        return holders < wanted.count { it == role }
    }

    private fun holdRole(uuid: String, role: String) {
        memberRoles[uuid] = role
        roleInvites[uuid] = System.currentTimeMillis()
    }

    private fun openRoleLabels(target: PartyTarget): String {
        val open = draft?.wantedRoles.orEmpty().asSequence().distinct().filter { roleOpen(it) }.toList()
        return if (open.isEmpty()) "none" else open.joinToString(", ") { target.roleLabel(it) }
    }

    /** Sets or removes the role of a member of the listed party and sends it to the server. */
    fun setMemberRole(uuid: String, role: String?) {
        if (role == null) memberRoles.remove(uuid) else memberRoles[uuid] = role
        updateBool = true
        HypixelModApi.sendPartyInfoPacket()
    }

    /** Sets or removes the leader's own role of the listed party, keeps it for the next party and sends it to the server. */
    fun setOwnRole(role: String?) {
        val current = draft ?: return
        current.ownRole = role
        currentTarget()?.let { current.wantedRoles = it.fittingRoles(role, current.wantedRoles).toMutableList() }
        val config = DataManager.partyFinderConfigState
        config.drafts[current.key]?.let { saved ->
            saved.ownRole = role
            config.save()
        }
        updateBool = true
        HypixelModApi.sendPartyInfoPacket()
    }

    // "!role <role>" in party chat; only the mod of the leader of a listed party answers
    private fun onRoleCommand(playerName: String, wanted: String) {
        if (!inQueue || !isLeader || !TextUtil.isPlayerName(playerName)) return
        val target = currentTarget() ?: return
        if (target.roles.isEmpty()) return
        val member = queuedParty?.members?.firstOrNull { it.name.equals(playerName, ignoreCase = true) } ?: return
        if (member.uuid == myUuid()) return
        val key = wanted.lowercase().filter { it.isLetterOrDigit() }
        val role = target.roles.firstOrNull { r -> r.id.filter { it.isLetterOrDigit() } == key || r.label.lowercase().filter { it.isLetterOrDigit() } == key }
        val wantedRoles = draft?.wantedRoles.orEmpty()
        when {
            role == null || wantedRoles.isNotEmpty() && role.id !in wantedRoles -> Chat.pc("[SBO] Unknown role. Open roles: ${openRoleLabels(target)}.")
            memberRoles[member.uuid] == role.id -> return
            !roleOpen(role.id, except = member.uuid) -> Chat.pc("[SBO] ${role.label} is already taken. Open roles: ${openRoleLabels(target)}.")
            else -> {
                setMemberRole(member.uuid, role.id)
                Chat.pc("[SBO] $playerName now plays ${role.label}.")
            }
        }
    }

    private fun reportFailure(failure: Failure, error: PfError) {
        val gui = listener
        if (gui != null) {
            // The create page lists the details, the toast only who misses what
            val details = ProblemText.labelsByPlayer(error.problems).joinToString("") { "\n$it" }
            gui(false, "${failure.start} ${ProblemText.error(error)}$details")
            return
        }
        val first = error.problems.firstOrNull()
        if (first == null) {
            Chat.chat("§6[SBO] §4${failure.start} ${ProblemText.error(error)}")
            return
        }
        val target = currentTarget()
        val who = if (first.name.equals(Player.getName(), ignoreCase = true)) "you don't" else "§b${first.name} §edoesn't"
        val missed = "§c${ProblemText.compact(error.problems.filter { it.name == first.name }, target)}"
        val text = when (failure) {
            Failure.NOT_QUEUED -> "Party not queued because $who meet the requirements: $missed"
            // The create page lists what is missing, here it matters that the party is still queued
            Failure.NOT_UPDATED -> "Party not updated because $who meet the requirements. It stays queued."
            Failure.REMOVED -> "Party removed from queue because $who meet the requirements: $missed"
        }
        Chat.chat(Chat.textComponent("§6[SBO] §e$text", ProblemText.details(error.problems, target)))
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

    private fun showJoinRequest(
        playerName: String, role: String?, onInvite: (() -> Unit)? = null, onBlock: () -> Unit = { BlockedPlayers.block(playerName) }
    ) {
        val roleText = role?.let { id ->
            val label = draft?.let { PartyCategories.target(it.partyType, it.subType) }?.roles?.firstOrNull { it.id == id }?.label ?: id
            " as §b$label"
        }.orEmpty()
        val invite = "/__sbo_run_clickable_action ${ClickActionManager.registerAction(onInvite ?: { inviteByName(playerName, role) })}"
        Chat.chat(Chat.getChatBreak())
        Chat.chat(
            Chat.textComponent("§6[SBO] §b$playerName §ewants to join your party$roleText§e.\n"),
            Chat.textComponent("§7[§aInvite§7]", "/p $playerName", invite),
            Chat.textComponent(" §7[§eOpen PV§7]", "Open $playerName's profile", "/__sbo_run_clickable_action ${ClickActionManager.registerAction { mc.schedule { ProfileViewerGui.open(playerName) } }}"),
            Chat.textComponent(" §7[§cBlock§7]", "Block player", "/__sbo_run_clickable_action ${ClickActionManager.registerAction(onBlock)}"),
        )
        Chat.chat(Chat.getChatBreak())
    }

    private fun invitePlayerIfMeetsReqs(playerName: String, request: JoinRequest) {
        val role = request.role
        val current = draft ?: return showJoinRequest(playerName, role)
        // The sender's name comes from Hypixel, so it can't be faked like an uuid in the text
        PartyFinderApi.checkMembers(
            CheckBody(current.partyType, current.subType, names = listOf(playerName), partyId = myUuid(), role = role),
            onError = { error ->
                if (error.code == PfError.PARTY_FULL) return@checkMembers
                Chat.chat("§6[SBO] §eCould not check §b$playerName§e: ${ProblemText.error(error)}")
                showJoinRequest(playerName, role)
            }
        ) { data ->
            val member = data.members.firstOrNull()
            if (member == null || !member.name.equals(playerName, ignoreCase = true)) {
                showJoinRequest(playerName, role)
                return@checkMembers
            }
            if (data.problems.isNotEmpty()) {
                val target = currentTarget()
                val text = "§6[SBO] §b$playerName §ewants to join but doesn't meet the requirements: §c${ProblemText.compact(data.problems, target)}"
                Chat.chat(Chat.textComponent(text, ProblemText.details(data.problems, target)))
                return@checkMembers
            }
            if (role != null && !roleOpen(role)) {
                Chat.chat("§6[SBO] §b$playerName §ewants to join as ${currentTarget()?.roleLabel(role) ?: role}, but that role is already taken.")
                return@checkMembers
            }
            if (freePlaces() > 0) {
                if (role != null) holdRole(member.uuid, role)
                holdPlace(playerName)
                Chat.command("p invite $playerName")
                Chat.chat("§6[SBO] §eInvited $playerName to the party.")
            }
        }
    }

    // Invite button of a /msg request: the uuid for the role comes from the server
    private fun inviteByName(playerName: String, role: String?) {
        Chat.command("p invite $playerName")
        holdPlace(playerName)
        if (role == null) return
        val current = draft ?: return
        PartyFinderApi.checkMembers(CheckBody(current.partyType, current.subType, names = listOf(playerName)), onError = {}) { data ->
            data.members.firstOrNull()?.takeIf { it.name.equals(playerName, ignoreCase = true) && roleOpen(role) }?.let { holdRole(it.uuid, role) }
        }
    }

    // Players who come through Hypixel's party finder only get this message, no "joined the party"
    private fun onHypixelGroupJoin(join: HypixelJoin) {
        isInParty = true
        val ownJoin = join.player.equals(Player.getName(), ignoreCase = true)
        if (ownJoin) isLeader = false
        onHypixelJoin(join)
        // For a listed party, the party info is requested after the player's check
        if (!ownJoin && inQueue) return
        sleep(JOIN_PARTY_INFO_DELAY_MS) {
            if (HypixelModApi.isOnHypixel) HypixelModApi.sendPartyInfoPacket()
        }
    }

    // Hypixel's party finder lets anyone in, so the requirements of the SBO listing are checked after the join
    private fun onHypixelJoin(join: HypixelJoin) {
        val playerName = join.player
        val pause = paused.takeUnless { inQueue }
        if (!inQueue && pause == null) return
        if (!isLeader || !TextUtil.isPlayerName(playerName) || playerName.equals(Player.getName(), ignoreCase = true)) return
        val current = pause?.draft ?: draft ?: return
        quietSince = 0L
        hypixelJoinChecks++
        val checkedName = playerName.lowercase()
        joinsBeingChecked += checkedName
        // A paused party is not on the server, so its requirements are checked here
        val body = if (pause == null) CheckBody(current.partyType, current.subType, names = listOf(playerName), partyId = myUuid())
            else CheckBody(current.partyType, current.subType, names = listOf(playerName), options = pause.options)
        PartyFinderApi.checkMembers(body, onError = { error ->
            mc.execute {
                val stillThere = joinsBeingChecked.remove(checkedName)
                finishHypixelJoinCheck()
                if (!stillThere) return@execute
                // Someone else paused the listing during the check
                if (pause == null && paused != null && error.code == PfError.PARTY_NOT_FOUND) onHypixelJoin(join)
                else Chat.chat("§6[SBO] §eCould not check §b$playerName§e: ${ProblemText.error(error)}")
            }
        }) { data ->
            mc.execute { onHypixelJoinChecked(join, current, pause, data) }
        }
    }

    private fun onHypixelJoinChecked(join: HypixelJoin, current: PartyDraft, pause: PausedParty?, data: CheckData) {
        val playerName = join.player
        val stillThere = joinsBeingChecked.remove(playerName.lowercase())
        val problems = if (pause == null) {
            // Players pick a role when they ask to join on SBO, Hypixel's party finder has none
            data.problems.filter { it.stat != "role" }
        } else {
            val target = PartyCategories.target(current.partyType, current.subType)
            val member = data.members.firstOrNull()
            if (target == null || member == null) emptyList()
            else ReqMatcher.check(playerName, member.stats, pause.reqs, target, pause.options)
        }
        if (!stillThere || problems.isEmpty()) return finishHypixelJoinCheck()
        if (join.canAutoKick(current.partyType) && DataManager.partyFinderConfigState.autoKick) return kickHypixelJoin(playerName, problems)
        pauseListing(playerName)
        warnHypixelJoin(playerName, problems)
    }

    private fun pauseListing(playerName: String) {
        val current = draft
        if (paused == null && inQueue && current != null) {
            val listed = queuedParty
            paused = PausedParty(current.deepCopy(), listed?.reqs ?: reqsOf(current), listed?.options ?: current.options.toMap())
            secondsOutsideSkyblock = 0
        }
        paused?.add(playerName)
    }

    private fun checkPause() {
        if (paused == null) return
        secondsOutsideSkyblock = if (World.isInSkyblock()) 0 else secondsOutsideSkyblock + 1
        if (HypixelModApi.inInstance || secondsOutsideSkyblock >= PAUSE_OUTSIDE_SKYBLOCK_SECONDS) paused = null
    }

    private fun relistWhenEveryoneLeft() {
        val pause = paused ?: return
        if (!pause.everyoneLeft) return
        sleep(RELIST_DELAY_MS) {
            if (paused !== pause || !pause.everyoneLeft) return@sleep
            paused = null
            if (inQueue || !isLeader) return@sleep
            draft = pause.draft
            if (partyMemberCount < pause.draft.partySize) requeue(pause.draft)
        }
    }

    private fun finishHypixelJoinCheck() {
        hypixelJoinChecks = (hypixelJoinChecks - 1).coerceAtLeast(0)
        if (hypixelJoinChecks > 0 || !inQueue) return
        hypixelJoinChecked = true
        mc.execute { HypixelModApi.sendPartyInfoPacket() }
    }

    private fun kickHypixelJoin(playerName: String, problems: List<Problem>) {
        val target = currentTarget()
        val stays = if (inQueue) " Your party stays queued." else ""
        Chat.chat(
            Chat.textComponent("§6[SBO] §eKicking §b$playerName§e, they don't meet the requirements.$stays\n", ProblemText.details(problems, target)),
            toggleAutoKickButton(),
        )
        autoKicks++
        pendingKicks += playerName.lowercase()
        kickQueue.addLast { sendKick(playerName, ProblemText.kickReason(playerName, problems, target)) }
        if (!kicking) nextKick()
    }

    // Several players can join at once, their party messages and kicks go to Hypixel one after another
    private fun nextKick() {
        val kick = kickQueue.removeFirstOrNull()
        kicking = kick != null
        kick?.invoke()
    }

    private fun sendKick(playerName: String, reason: String) {
        val done = {
            sleep(AUTO_KICK_DISBAND_WINDOW_MS) { autoKicks = (autoKicks - 1).coerceAtLeast(0) }
        }
        // The player already left, so Hypixel gets nothing for them
        if (!pendingKicks.remove(playerName.lowercase())) {
            finishHypixelJoinCheck()
            done()
            return nextKick()
        }
        // Sent before the kick, so the kicked player still reads why
        Chat.pc(reason)
        val session = kickSession
        sleep(HYPIXEL_KICK_DELAY_MS) {
            if (session != kickSession) return@sleep
            Chat.command("p kick $playerName")
            sleep(HYPIXEL_KICK_SETTLE_MS) { finishHypixelJoinCheck() }
            done()
            sleep(AUTO_KICK_GAP_MS) { if (session == kickSession) nextKick() }
        }
    }

    private fun warnHypixelJoin(playerName: String, problems: List<Problem>, relistFailed: Boolean = false) {
        val target = currentTarget()
        val details = ProblemText.details(problems, target)
        val player = "§b$playerName §edoesn't meet the requirements"
        val happened = when {
            relistFailed -> "Party not queued because $player"
            // The first player who didn't meet the requirements already removed it
            !inQueue && paused != null -> player
            else -> "Party removed from queue because $player"
        }
        // The warning below says it already
        removePartyFromQueue(announce = false)
        finishHypixelJoinCheck()
        val warning = Chat.textComponent(
            "§6[SBO] §e$happened: §c${ProblemText.compact(problems, target)}\n", details
        )
        // Minecraft may ask before it runs a clicked server command, the mod sends the kick without asking
        val kickAction = ClickActionManager.registerRepeatableAction("kick:${playerName.lowercase()}") { Chat.command("p kick $playerName") }
        val kick = Chat.textComponent("§7[§cKick§7]", "Kicks $playerName from your party.\n$details", "/__sbo_run_clickable_action $kickAction")
        Chat.chat(warning, kick, Chat.textComponent(" "), toggleAutoKickButton())
    }

    private fun toggleAutoKickButton(): Component {
        val toggle = ClickActionManager.registerRepeatableAction("toggleAutoKick") {
            val config = DataManager.partyFinderConfigState
            config.autoKick = !config.autoKick
            config.save()
            Chat.chat(
                if (config.autoKick) "§6[SBO] §aAuto Kick is now on for Kuudra parties."
                else "§6[SBO] §cAuto Kick is now off for Kuudra parties."
            )
        }
        return Chat.textComponent(
            "§7[§eToggle Auto Kick§7]",
            "Turns Auto Kick on or off. It is only for Kuudra parties, other party types are never kicked automatically.\n" +
                "When it is on, SBO kicks players from Hypixel's party finder who don't meet the requirements.",
            "/__sbo_run_clickable_action $toggle"
        )
    }

    fun onPartyRemoved(error: PfError) {
        if (!inQueue) return
        inQueue = false
        queuedParty = null
        Chat.chat("§6[SBO] §4Party removed from queue: ${ProblemText.error(error)}")
    }

    // The backend already checked the player against the party
    fun onSocketJoinRequest(requestId: String, uuid: String, playerName: String, role: String?) {
        if (!inQueue || freePlaces() <= 0 || BlockedPlayers.isBlocked(uuid)) return PartyFinderSocket.answer(requestId, invited = false)
        // Another player got this place a moment ago
        if (role != null && !roleOpen(role)) return PartyFinderSocket.answer(requestId, invited = false)
        val invite = {
            if (role != null) holdRole(uuid, role)
            holdPlace(playerName)
            Chat.command("p invite $playerName")
            PartyFinderSocket.answer(requestId, invited = true)
        }
        if (DataManager.partyFinderConfigState.autoInvite) {
            invite()
            Chat.chat("§6[SBO] §eInvited $playerName to the party.")
        } else {
            showJoinRequest(playerName, role, invite) {
                BlockedPlayers.block(playerName, uuid)
                PartyFinderSocket.answer(requestId, invited = false)
            }
        }
    }

    /** [role] is needed when the party asks for roles. */
    fun sendJoinRequest(party: PartyView, role: String? = null) {
        // The name ends up in /msg
        val leaderName = party.leader?.name?.takeIf { TextUtil.isPlayerName(it) } ?: return
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
            if (problems.isNotEmpty()) return@get showOwnProblems(problems, target)
            playersSentRequest[leaderName] = System.nanoTime()
            // Over the socket first, by /msg when the leader is not connected or the socket fails
            PartyFinderSocket.requestJoin(party.id, role) { status, answer ->
                when (status) {
                    "sent" -> tell("§6[SBO] §eJoin request sent.", true)
                    "invited" -> tell("§6[SBO] §aYou were invited.", true)
                    "declined" -> tell("§6[SBO] §cThe leader declined.", false)
                    "failed" -> joinFailed(answer, target, leaderName)
                    else -> {
                        tell("§6[SBO] §eSending join request to $leaderName...", true)
                        Chat.command("msg $leaderName ${JoinRequest.message(role)}")
                    }
                }
            }
        }
    }

    private fun joinFailed(answer: JsonObject, target: PartyTarget, leaderName: String) {
        val problems = (answer["problems"] as? JsonArray)?.let { runCatching { PartyFinderApi.json.decodeFromJsonElement<List<Problem>>(it) }.getOrNull() }
        if (!problems.isNullOrEmpty()) {
            playersSentRequest.remove(leaderName)
            return showOwnProblems(problems, target)
        }
        val error = (answer["error"] as? JsonObject)?.let { runCatching { PartyFinderApi.json.decodeFromJsonElement<PfError>(it) }.getOrNull() }
            ?: PfError(PfError.BAD_RESPONSE)
        // The backend's wait between requests to the same leader stays
        if (error.code != PfError.RATE_LIMITED) playersSentRequest.remove(leaderName)
        tell("§6[SBO] §4Failed to join party: ${ProblemText.error(error)}", false)
    }

    private fun showOwnProblems(problems: List<Problem>, target: PartyTarget) {
        val gui = listener
        if (gui != null) {
            gui(false, "You don't meet the requirements: " + problems.joinToString("; ") { ProblemText.describe(it, target) } +
                "\n${ProblemText.OWN_RELOAD_HINT}")
            return
        }
        val text = "§6[SBO] §cYou don't meet the requirements: ${ProblemText.compact(problems, target)}"
        Chat.chat(Chat.textComponent(text, "${ProblemText.details(problems, target)}\n${ProblemText.OWN_RELOAD_HINT}"))
    }

    /** Without announce no chat message is sent, for callers that already sent one. */
    fun removePartyFromQueue(announce: Boolean = true, onComplete: (Boolean) -> Unit = {}) {
        if (inQueue) {
            inQueue = false
            queuedParty = null
            PartyFinderApi.removeParty(onError = { error ->
                onComplete(false)
                tell("§6[SBO] §4Failed to remove party from queue: ${ProblemText.error(error)}", false)
            }) {
                onComplete(true)
                if (announce) tell("§6[SBO] §eParty removed from queue.", true)
                if (isInParty) Chat.pc("[SBO] Party removed from queue.")
            }
        } else if (creatingParty) {
            ghostParty = true
        }
    }

    @SboEvent
    fun trackMemberRegister(event: ChatMessageEvent) {
        val text = event.message.toFormattedString()
        var match = false
        var someoneLeft = false

        if (leaderChangeMatchers.any { it.matches(text) }) {
            match = true
            paused = null
            isInParty = true
            isLeader = false
            removePartyFromQueue()
        }

        if (partyDisbandMatchers.any { it.matches(text) }) {
            creatingParty = false
            partyMemberCount = 1
            match = true
            isInParty = false
            memberRoles.clear()
            roleInvites.clear()
            invitedAt.clear()
            if (PARTY_EMPTY in text) {
                paused?.allLeft()
                relistWhenEveryoneLeft()
            } else {
                paused = null
            }
            if (autoKicks == 0) removePartyFromQueue()
        }

        ownJoinMatcher.matchGroup(text, 1)?.let { playerName ->
            // The leader's mod updates the listed party first
            listener?.invoke(
                true,
                "You joined ${TextUtil.getPlayerName(playerName)}'s party."
            )
            sleep(3000) { joinedParties++ }
        }

        val joinedPlayer = partyJoinMatchers.firstNotNullOfOrNull { it.matchGroup(text, 1) }
        if (joinedPlayer != null) {
            invitedAt.remove(TextUtil.getPlayerName(joinedPlayer).lowercase())
            quietSince = 0L
            updateBool = true
            partyMemberCount += 1
            match = true
            isInParty = true
        }

        val leftPlayer = partyLeaveMatchers.firstNotNullOfOrNull { it.matchGroup(text, 1) }
        if (leftPlayer != null) {
            someoneLeft = true
            val name = TextUtil.getPlayerName(leftPlayer)
            joinsBeingChecked.remove(name.lowercase())
            pendingKicks.remove(name.lowercase())
            invitedAt.remove(name.lowercase())
            paused?.left(name)
            relistWhenEveryoneLeft()
            updateBool = true
            partyMemberCount -= 1
            match = true
            // The count is a guess after joining someone else's party, ask the server instead of leaving the party here.
            // A party that really ends sends one of the disband messages.
            if (partyMemberCount <= 1 && !inQueue) sleep(200) { HypixelModApi.sendPartyInfoPacket() }
        }

        if (match) trackMemberCount(someoneLeft)
    }

    private fun trackMemberCount(someoneLeft: Boolean = false) {
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
            // Only a leave frees a place, and a paused party waits until the players who miss requirements are gone
            if (!someoneLeft || paused != null) return
            if (!isInParty) return
            if (!isLeader) return
            val last = draft ?: return
            if (partyMemberCount < partySize) requeue(last)
        }
    }

    private fun requeue(last: PartyDraft) {
        if (creatingParty || requeue || !usedPf) return
        requeue = true
        sleep(200) {
            requeue = false
            if (DataManager.partyFinderConfigState.autoRequeue) {
                relisting = true
                createParty(last)
            } else {
                Chat.clickableChat("§6[SBO] §eClick to requeue party with last used requirements.", "/sborequeue", "/sborequeue")
            }
        }
    }

    private fun checkQuietQueue() {
        if (!inQueue || quietSince == 0L || System.currentTimeMillis() - quietSince < QUIET_QUEUE_HINT_AFTER) return
        quietSince = 0L
        SBOKotlin.toast(
            Component.literal("SBO Party Finder").withStyle(ChatFormatting.GOLD),
            Component.literal("Nobody joined in 10 minutes. Try lowering your requirements.").withStyle(ChatFormatting.YELLOW)
        )
        Chat.chat(
            Chat.textComponent("§6[SBO] §eNobody joined your party in the last 10 minutes. Try lowering or adjusting your requirements.\n"),
            Chat.textComponent("§7[§aOpen Party Finder§7]", "Open the Party Finder", "/sbopf")
        )
    }

    private val INVALID_NOTE_CHARACTERS = Regex("""[^\p{L}\p{N} \n,.!?_\-+/:'()]""")
    private val NOTE_LINE_START_SLASHES = Regex("""(?m)^/+""")

    /** Same filter as the backend: letters, digits, spaces, line breaks and ,.!?-_+/:'(), no / at a line start */
    internal fun checkPartyNote(note: String): String {
        return limitNoteLines(note.replace('\t', ' '))
            .replace(INVALID_NOTE_CHARACTERS, "")
            .replace(NOTE_LINE_START_SLASHES, "")
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
            "[SBO] join party request - id:$id" + role?.let { " role:$it" }.orEmpty()
    }
}
