package net.sbo.mod.partyfinder

import gg.essential.universal.utils.toFormattedString
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.sbo.mod.settings.categories.PartyFinder
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.http.Http
import net.sbo.mod.utils.http.Http.getBoolean
import net.sbo.mod.utils.http.Http.getString
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.partyfinder.PartyPlayer.getPartyPlayerStats
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.data.GetAllParties
import net.sbo.mod.utils.data.Party
import net.sbo.mod.utils.data.PartyAddResponse
import net.sbo.mod.utils.data.PartyUpdateResponse
import net.sbo.mod.utils.data.Reqs
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderRefreshListEvent
import net.sbo.mod.utils.events.impl.game.ChatMessageEvent
import net.sbo.mod.utils.http.Http.getInt
import java.util.UUID
import java.util.regex.Pattern
import kotlin.collections.mutableMapOf

object PartyFinderManager {
    var creatingParty = false
    var inQueue = false
    private var updateBool = false
    private var requeue = false
    private var ghostParty = false
    var usedPf = false

    private var partySize = 0
    private var partyMemberCount = 0
    private var partyMember: List<String> = emptyList()
    private var isLeader = false
    private var partyNote = ""
    private var partyType = ""
    private var partyReqs = ""
    private var partyReqsMap = Reqs()
    var isInParty = false

    private val playersSentRequest = mutableMapOf<String, Long>()

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

    fun init() {
        Register.command("sborequeue") {
            if (!inQueue) {
                Chat.chat("§6[SBO] §eRequeuing party with last used requirements...")
                createParty(partyReqs, partyNote, partyType, partySize)
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

        Register.command("sboKey") { args ->
            if (args.isEmpty()) {
                Chat.chat("§6[SBO] §cPlease provide a key")
            } else if (args[0].startsWith("sbo").not()) {
                Chat.chat("§6[SBO] §cInvalid key format! get one in our Discord")
            } else {
                sboData.sboKey = args[0]
                SboDataObject.save("SboData")
                Chat.chat("§6[SBO] §aKey has been set")
            }
        }

        Register.command("sboClearKey") {
            sboData.sboKey = ""
            SboDataObject.save("SboData")
            Chat.chat("§6[SBO] §aKey has been cleared")
        }

        Register.onChatMessageCancable(
            Pattern.compile("§d(.*?) (.*?)§7: (.*?) join party request - id:(.*)", Pattern.DOTALL)
        ) { message, matchResult ->
            if (matchResult.group(1).contains("From")) {
                if (partyMemberCount < partySize) {
                    val playerName = Helper.getPlayerName(matchResult.group(2) ?: "no name")

                    if (PartyFinder.autoInvite) {
                        invitePlayerIfMeetsReqs(playerName)
                    } else {
                        Chat.chat(Chat.getChatBreak())
                        Chat.chat(
                            Chat.textComponent("§6[SBO] §b$playerName §ewants to join your party.\n"),
                            Chat.textComponent("§7[§aInvite§7]", "/p $playerName", "/p invite $playerName"),
                            Chat.textComponent(" §7[§eCheck Stats§7]", "/sboc $playerName", "/sbocheck $playerName"),
                        )
                        Chat.chat(Chat.getChatBreak())
                    }
                }
            }
            false
        }

        Register.onChatMessageCancable(
            Pattern.compile("^§9§m(.*?) §ehas invited you to join their party!(.*?)$", Pattern.DOTALL)
        ) { message, matchResult ->
            val playername = Helper.getPlayerName(matchResult.group(1) ?: "")
            if (playersSentRequest.containsKey(playername)) {
                Chat.chat("§6[SBO] §eJoining party of §b$playername§e...")
                Chat.command("p accept $playername")
                playersSentRequest.remove(playername)
            }
            true
        }

        Register.onTick(20 * 60 * 4) { // every 4 minutes
            Http.sendGetRequest("$API_URL/countActiveUsers")

            if (inQueue) {
                Http.sendGetRequest("$API_URL/queueUpdate?leaderId=${Player.getUUIDString().replace("-", "")}")
                    .toJsonObject { response ->
                        if (!response.getBoolean("Success")) {
                            inQueue = false
                            Chat.chat("§6[SBO] §4${response.getString("Error") ?: "Unknown error"}")
                        }
                    }
                    .error { error ->
                        inQueue = false
                        Chat.chat("§6[SBO] §4Unexpected error")
                    }
            }
        }

        HypixelModApi.onPartyInfo{ isInParty, isLeader, members ->
            this.isInParty = isInParty
            this.isLeader = isLeader
            this.partyMember = members
            partyMemberCount = members.size
            queueParty()
            updateParty()
        }

        HypixelModApi.onError { packet ->
            if (packet.id == PartyInfoS2CPacket.ID) {
                creatingParty
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

    fun createParty(
        reqs: String,
        note: String,
        type: String,
        size: Int,
    ) {
        if (this.creatingParty) return
        this.partyReqs = reqs
        this.partyNote = note
        this.partyType = type
        this.partySize = size
        this.usedPf = true

        HypixelModApi.sendPartyInfoPacket(true)
    }

    fun queueParty() {
        if (!this.creatingParty) return
        creatingParty = false
        if (partyMember.size > partySize) {
            Chat.chat("§6[SBO] §4Party is over the limit. ${partyMember.size}/$partySize")
            return
        }
        if (inQueue) {
            Chat.chat("§6[SBO] §4Party is already in the queue.")
            return
        }
        if (!isLeader) {
            Chat.chat("§6[SBO] §4You must be the party leader to queue the party.")
            return
        }

        try {
            val currentTime = System.currentTimeMillis()
            Http.sendGetRequest(
                "$API_URL/createParty?uuids=${partyMember.joinToString(",").replace("-", "")}" +
                        "&reqs=$partyReqs" +
                        "&note=${checkPartyNote(partyNote)}" +
                        "&partytype=$partyType" +
                        "&partysize=$partySize" +
                        "&key=${sboData.sboKey}"
            ).toJson<PartyAddResponse> { response ->
                if (response.success) {
                    val timeTaken = System.currentTimeMillis() - currentTime
                    inQueue = true
                    creatingParty = false
                    partyReqsMap = response.partyReqs!!
                    SBOEvent.emit(PartyFinderRefreshListEvent())

                    if (ghostParty) {
                        removePartyFromQueue()
                        ghostParty = false
                    }

                    if (requeue) {
                        requeue = false
                        Chat.clickableChat("§6[SBO] §eClick to dequeue party", "Dequeue Party", "/sbodequeue")
                    }

                    Chat.chat("§6[SBO] §eParty created successfully! Time taken: ${timeTaken}ms")

                    if (isInParty) Chat.command("pc [SBO] Party now in queue.")
                } else {
                    val errorMessage = response.error ?: "Unknown error"
                    Chat.chat("§6[SBO] §4Failed to create party: ${errorMessage.replace("&", "§")}")

                }

            }.error { error ->
                Chat.chat("§6[SBO] §4Unexpected error while creating party: ${error.message}")
            }

        } catch (_: Exception) {
            return
        }
    }

    fun updateParty() {
        if (!this.updateBool) return
        updateBool = false
        if (inQueue && isInParty && isLeader) {
            if (partyMember.size >= partySize || partyMember.size < 2) return
            val currentTime = System.currentTimeMillis()
            Http.sendGetRequest(
                "$API_URL/queuePartyUpdate?uuids=${partyMember.joinToString(",").replace("-", "")}" +
                        "&reqs=$partyReqs" +
                        "&note=${checkPartyNote(partyNote)}" +
                        "&partytype=$partyType" +
                        "&partysize=$partySize" +
                        "&key=${sboData.sboKey}"
            ).toJson<PartyUpdateResponse> { response ->
                if (response.success) {
                    val timeTaken = System.currentTimeMillis() - currentTime
                    partyReqsMap = response.partyReqs!!
                    Chat.chat("§6[SBO] §eParty updated successfully! Time taken: ${timeTaken}ms")
                } else {
                    inQueue = false
                    val errorMessage = response.error ?: "Unknown error"
                    Chat.chat("§6[SBO] §4Failed to update party: ${errorMessage.replace("&", "§")}")
                }
            }.error { error ->
                inQueue = false
                Chat.chat("§6[SBO] §4Unexpected error while updating party: ${error.message}")
            }
        }
    }

    fun getAllParties(
        partyType: String,
        onComplete: ((List<Party>) -> Unit)? = null
    ) {
        Http.sendGetRequest("$API_URL/getAllParties?partytype=$partyType").toJson<GetAllParties>(true) { response ->
            if (response.success) {
                val partyList = response.parties
                onComplete?.invoke(partyList)
            } else {
                Chat.chat("§6[SBO] §4Failed to get parties")
            }
        }.error { error ->
            Chat.chat("§6[SBO] §4Unexpected error while getting parties: ${error.message}")
        }
    }

    fun getActiveUsers(
        onComplete: ((Int) -> Unit)? = null
    ) {
        Http.sendGetRequest("$API_URL/activeUsers").toJsonObject { response ->
            onComplete?.invoke(response.getInt("activeUsers") ?: 0)
        }.error { error ->
            Chat.chat("§6[SBO] §4Unexpected error while getting active users: ${error.message}")
        }
    }

    // todo: add a way to prevent inviting more player then party has space (maybe every user has 10 seconds to accept else next player gets invited)
    fun invitePlayerIfMeetsReqs(playerName: String) {
        PartyCheck.checkPlayer(playerName, true) { stats ->
            if (checkIfPlayerMeetsReqs(stats, partyReqsMap)) {
                if (partyMemberCount < partySize) {
                    Chat.command("p invite $playerName")
                    Chat.chat("§6[SBO] §eInvited $playerName to the party.")
                }
            }
        }
    }

    fun checkIfPlayerMeetsReqs(
        stats: PartyPlayerStats,
        reqs: Reqs
    ): Boolean {
        if (stats.sbLvl < reqs.lvl) {
            return false
        }
        if (stats.mythosKills < reqs.kills) {
            return false
        }
        if (reqs.eman9 && !stats.eman9) {
            return false
        }
        if (reqs.looting5 && !stats.looting5daxe) {
            return false
        }
        if (stats.magicalPower < reqs.mp) {
            return false
        }
        return true
    }

    fun sendJoinRequest(
        partyLeader: String,
        partyReqs: Reqs
    ) {
        getPartyPlayerStats { playerStats ->
            if (checkIfPlayerMeetsReqs(playerStats, partyReqs)) {
                if (playersSentRequest.containsKey(partyLeader) && (System.currentTimeMillis() - playersSentRequest[partyLeader]!! < 60000)) { // 1 minute cooldown
                    Chat.chat("§6[SBO] §cYou have already sent a request to this player recently.")
                } else {
                    Chat.chat("§6[SBO] §eSending join request to $partyLeader...")
                    Chat.command("msg $partyLeader [SBO] join party request - id:${UUID.randomUUID()}")
                    playersSentRequest[partyLeader] = System.currentTimeMillis()
                }
            } else {
                Chat.chat("§6[SBO] §cYou don't meet the requirements to join this party.")
            }
        }
    }

    fun removePartyFromQueue(onComplete: ((Boolean) -> Unit)? = null) {
        if (inQueue) {
            inQueue = false
            Http.sendGetRequest("$API_URL/unqueueParty?leaderId=${Player.getUUIDString().replace("-", "")}")
                .result { response ->
                    onComplete?.invoke(true)
                    Chat.chat("§6[SBO] §eParty removed from queue.")
                }.error { error ->
                    Chat.chat("§6[SBO] §4Unexpected error while removing party from queue")
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
                removePartyFromQueue()
            }
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

    fun trackMemberCount() {
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
            if (partyMemberCount < partySize && !creatingParty && !requeue && usedPf) {
                requeue = true
                sleep(200) {
                    if (PartyFinder.autoRequeue) {
                        Chat.chat("§6[SBO] §eRequeuing party with last used requirements...")
                        createParty(partyReqs, partyNote, partyType, partySize)
                    } else {
                        Chat.clickableChat("§6[SBO] §eClick to requeue party with last used requirements.", "/sborequeue", "/sborequeue")
                    }
                }
            }
        }
    }

    fun checkPartyNote(note: String): String {
        // allowed characters a-z, A-Z, 0-9, comma, dot, exclamation mark, hyphen, underscore, question mark
        return note.replace(Regex("[^a-zA-Z0-9 ,.!?\\-_]"), "")
            .take(30)
            .trim().replace(" ", "%20")
    }
}