package net.sbo.mod.partyfinder

import net.sbo.mod.SBOKotlin.API_URL
import net.sbo.mod.SBOKotlin.logger
import net.sbo.mod.diana.achievements.AchievementManager.trackWithCheckPlayer
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.HypixelModApi
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.data.PartyInfo
import net.sbo.mod.utils.data.PartyPlayerStats
import net.sbo.mod.utils.http.Http

object PartyCheck {
    private var checkPartyBool = false
    private var partyCheckLimit = 6
    private var checkCooldown: Long = 0

    fun init() {
        HypixelModApi.onPartyInfo { isInParty, isLeader, members ->
            val partyMember = members.filter { it != Player.getUUIDString() }
            checkParty(partyMember)
        }

        Register.command("sbocheckparty", "sbocheckp", "sbocp") {
            if (System.currentTimeMillis() - checkCooldown > 30000) { // 30 seconds cooldown
                checkCooldown = System.currentTimeMillis()
                checkPartyBool = true
                Chat.chat("§6[SBO] §eChecking party members...")
                HypixelModApi.sendPartyInfoPacket()
            } else {
                Chat.chat("§6[SBO] §ePlease wait 30 seconds before checking party members again.")
            }
        }

        Register.command("sbocheck", "sboc") { args ->
            if (args.isEmpty()) {
                Chat.chat("§6[SBO] §ePlease provide a player name to check.")
                return@command
            }
            val playerName = args[0].trim()
            checkPlayer(playerName)
        }
    }

    fun checkPlayer(
        playerName: String,
        noMessage: Boolean = false,
        readCache: Boolean = true,
        onComplete: ((PartyPlayerStats)-> Unit)? = null
    ) {
        if (!noMessage) Chat.chat("§6[SBO] §eChecking player: §b$playerName")
        Http.sendGetRequest("$API_URL/partyInfo?party=$playerName&readCache=$readCache")
            .toJson<PartyInfo> { response ->
                if (response.success) {
                    val partyInfo = response.partyInfo
                    if (partyInfo.firstOrNull() != null) {
                        if (!noMessage && partyInfo[0].uuid == Player.getUUIDString().replace("-", "")) {
                            printPartyInfo(partyInfo)
                            trackWithCheckPlayer(partyInfo[0])
                        } else if (!noMessage) {
                            printPartyInfo(partyInfo, true)
                        }
                        onComplete?.invoke(partyInfo[0])
                    }
                }
            }
            .error { error ->
                Chat.chat("§6[SBO] §eError checking player: ${error.message ?: "Unknown error"}")
            }
    }

    fun checkParty(partyMember: List<String>) {
        if (!checkPartyBool) return
        checkPartyBool = false
        if (partyMember.size > partyCheckLimit) {
            Chat.chat("§6[SBO] §eParty members limit reached. Only checking first $partyCheckLimit members.")
        }
        if (partyMember.isEmpty()) {
            Chat.chat("§6[SBO] §eNo party members found.")
            return
        }
        Http.sendGetRequest("$API_URL/partyInfoByUuids?uuids=${partyMember.joinToString(",").replace("-", "")}")
            .toJson<PartyInfo> { response ->
                if (response.success) {
                    printPartyInfo(response.partyInfo)
                }
            }
            .error { error ->
                logger.error("Error whilst checking party members", error) // print full error with stacktrace to logs

                Chat.chat("§6[SBO] §eError checking party members: ${error.message ?: "Unknown error"}. More information might be available in your logs!")
            }
    }

    fun printPartyInfo(partyInfo: List<PartyPlayerStats>, inviteButton: Boolean = false) {
        partyInfo.forEach { player ->
            Chat.chat(
                "§6[SBO] §eName: §b${player.name} §9│ §eLvL: §6${player.sbLvl} " +
                "§9│ §eEman 9: §f${if (player.eman9) "§a✓" else "§4✗"} §9│ §eL5 Daxe: ${if (player.looting5daxe) "§a✓" else "§4✗"} " +
                "§9│ §eKills: §6${Helper.formatNumber(player.mythosKills)}"
            )
            if (inviteButton) Chat.clickableChat("§7[§eClick to invite§7]", "/p ${player.name}", "/p invite ${player.name}")
        }
    }
}