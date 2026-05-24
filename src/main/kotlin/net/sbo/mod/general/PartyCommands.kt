package net.sbo.mod.general

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.events.Register
import net.sbo.mod.settings.categories.PartyCommands
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.SboDataObject.sboData
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerMayor
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Helper.getPlayerName
import net.sbo.mod.utils.Helper.calcPercentOne
import net.sbo.mod.utils.Helper.formatNumber
import net.sbo.mod.utils.Helper.formatTime
import net.sbo.mod.diana.DianaStats
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.game.ServerStats
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date

object PartyCommands {
    // How to add a new party command:
    //   PartyCommand(listOf("!alias"), { settings.dianaPartyCommands }) { "Response" }
    //   PartyCommand(listOf("!alias1", "!alias2"), { settings.someSetting }) {
    //       val count = dianaTrackerMayor.items.SOME_ITEM
    //       "Your message: $count"
    //   }
    // For count+percent: use fmt("Label", count, "ITEM_KEY", "MOB_KEY")

    val commandRegex = Regex("^§9[^§]+ §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")

    val settings = PartyCommands

    val carrot = listOf(
        "As I see it, Carrot",
        "It is Carrot",
        "It is decidedly Carrot",
        "Most likely Carrot",
        "Outlook Carrot",
        "Signs point to Carrot",
        "Without a Carrot",
        "Yes - Carrot",
        "Carrot - definitely",
        "You may rely on Carrot",
        "Ask Carrot later",
        "Carrot predict now",
        "Concentrate and ask Carrot ",
        "Don't count on it - Carrot 2024",
        "My reply is Carrot",
        "My sources say Carrot",
        "Outlook not so Carrot",
        "Very Carrot"
    )

    fun init() {
        registerPartyChatListeners()
        partyCommands()
    }

    fun partyCommands() {
        Register.command("sbopartycommands", "sbopcom") {
            help()
        }
    }

    val helpCommands = listOf(
        "!chim", "!chimls", "!stick", "!relic", "!feathers",
        "!profit", "!playtime", "!mobs", "!burrows", "!mf",
        "!stats <playername>",
        "!since (chim, chimls, relic, stick, inq, king, manti, core, corels, wool, woolls)"
    )

    fun help() {
        Chat.chat("§6[SBO] §eDiana party commands:")
        helpCommands.forEach {
            Chat.chat("§7> §a$it")
        }
    }

    private data class PartyCommand(
        val aliases: List<String>,
        val isEnabled: () -> Boolean,
        val execute: () -> String
    )

    private val dianaCommands = listOf(
        PartyCommand(listOf("!chim", "!chimera", "!chims", "!chimeras", "!book", "!books"), { settings.dianaPartyCommands }) {
            fmt("Chimera", dianaTrackerMayor.items.CHIMERA, "CHIMERA", "MINOS_INQUISITOR") + " +${dianaTrackerMayor.items.CHIMERA_LS} LS"
        },
        PartyCommand(listOf("!inqsls", "!inquisitorls", "!inquisls", "!lsinq", "!lsinqs", "!lsinquisitor", "!lsinquis"), { settings.dianaPartyCommands }) {
            "Inquisitor LS: ${dianaTrackerMayor.mobs.MINOS_INQUISITOR_LS}"
        },
        PartyCommand(listOf("!inq", "!inqs", "!inquisitor", "!inquis"), { settings.dianaPartyCommands }) {
            fmt("Inquisitor", dianaTrackerMayor.mobs.MINOS_INQUISITOR, "MINOS_INQUISITOR")
        },
        PartyCommand(listOf("!burrows", "!burrow"), { settings.dianaPartyCommands }) {
            val burrows = dianaTrackerMayor.items.TOTAL_BURROWS
            val perHr = Helper.getBurrowsPerHr(dianaTrackerMayor, SboTimerManager.timerMayor)
            "Burrows: $burrows ($perHr/h)"
        },
        PartyCommand(listOf("!relic", "!relics"), { settings.dianaPartyCommands }) {
            fmt("Relics", dianaTrackerMayor.items.MINOS_RELIC, "MINOS_RELIC", "MINOS_CHAMPION")
        },
        PartyCommand(listOf("!chimls", "!chimerals", "!bookls", "!lschim", "!lsbook", "!lootsharechim", "!lschimera"), { settings.dianaPartyCommands }) {
            fmt("Chimera LS", dianaTrackerMayor.items.CHIMERA_LS, "CHIMERALS", "MINOS_INQUISITOR_LS")
        },
        PartyCommand(listOf("!core", "!manticore"), { settings.dianaPartyCommands }) {
            fmt("Cores", dianaTrackerMayor.items.MANTI_CORE, "MANTI_CORE", "MANTICORE")
        },
        PartyCommand(listOf("!corels", "!manticorels", "!lscore", "!lsmanticore"), { settings.dianaPartyCommands }) {
            fmt("Core LS", dianaTrackerMayor.items.MANTI_CORE_LS, "MANTI_CORE_LS", "MANTICORE_LS")
        },
        PartyCommand(listOf("!stinger", "!fatefulstinger"), { settings.dianaPartyCommands }) {
            fmt("Stingers", dianaTrackerMayor.items.FATEFUL_STINGER, "FATEFUL_STINGER", "MANTICORE")
        },
        PartyCommand(listOf("!stingerls", "!fatefulstingerls", "!lsstinger", "!lsfatefulstinger"), { settings.dianaPartyCommands }) {
            fmt("Stinger LS", dianaTrackerMayor.items.FATEFUL_STINGER_LS, "FATEFUL_STINGER_LS", "MANTICORE_LS")
        },
        PartyCommand(listOf("!wool", "!shimmering", "!shimmeringwool"), { settings.dianaPartyCommands }) {
            fmt("Wool", dianaTrackerMayor.items.SHIMMERING_WOOL, "SHIMMERING_WOOL", "KING_MINOS")
        },
        PartyCommand(listOf("!woolls", "!shimmeringwoolls", "!lsshimmering", "!lsshimmeringwool"), { settings.dianaPartyCommands }) {
            fmt("Wool LS", dianaTrackerMayor.items.SHIMMERING_WOOL_LS, "SHIMMERING_WOOL_LS", "KING_MINOS_LS")
        },
        PartyCommand(listOf("!food", "!brainfood", "!brain"), { settings.dianaPartyCommands }) {
            fmt("Brain Food", dianaTrackerMayor.items.BRAIN_FOOD, "BRAIN_FOOD", "SPHINX")
        },
        PartyCommand(listOf("!foodls", "!brainfoodls", "!lsbrainfood", "!lsbrain"), { settings.dianaPartyCommands }) {
            fmt("Brain Food LS", dianaTrackerMayor.items.BRAIN_FOOD_LS, "BRAIN_FOOD_LS", "SPHINX_LS")
        },
        PartyCommand(listOf("!kingshard", "!kingshards"), { settings.dianaPartyCommands }) {
            fmt("King Shards", dianaTrackerMayor.items.KING_MINOS_SHARD, "KING_MINOS_SHARD", "KING_MINOS")
        },
        PartyCommand(listOf("!sphinxshard", "!sphinxshards"), { settings.dianaPartyCommands }) {
            fmt("Sphinx Shards", dianaTrackerMayor.items.SPHINX_SHARD, "SPHINX_SHARD", "SPHINX")
        },
        PartyCommand(listOf("!minotaurshard", "!minotaurshards"), { settings.dianaPartyCommands }) {
            fmt("Minotaur Shards", dianaTrackerMayor.items.MINOTAUR_SHARD, "MINOTAUR_SHARD", "MINOTAUR")
        },
        PartyCommand(listOf("!certanshard", "!certanshards"), { settings.dianaPartyCommands }) {
            fmt("Certan Shards", dianaTrackerMayor.items.CRETAN_BULL_SHARD, "CRETAN_BULL_SHARD", "CRETAN_BULL")
        },
        PartyCommand(listOf("!mythofrag", "!frags"), { settings.dianaPartyCommands }) {
            "Mytho Frags: ${dianaTrackerMayor.items.MYTHOS_FRAGMENT}"
        },
        PartyCommand(listOf("!urns", "!urn", "!cretanurn"), { settings.dianaPartyCommands }) {
            fmt("Urns", dianaTrackerMayor.items.CRETAN_URN, "CRETAN_URN", "CRETAN_BULL")
        },
        PartyCommand(listOf("!hilt", "!hiltofrevelations"), { settings.dianaPartyCommands }) {
            fmt("Hilts", dianaTrackerMayor.items.HILT_OF_REVELATIONS, "HILT_OF_REVELATIONS", "MINOS_HUNTER")
        },
        PartyCommand(listOf("!sticks", "!stick"), { settings.dianaPartyCommands }) {
            fmt("Sticks", dianaTrackerMayor.items.DAEDALUS_STICK, "DAEDALUS_STICK", "MINOTAUR")
        },
        PartyCommand(listOf("!feathers", "!feather"), { settings.dianaPartyCommands }) {
            "Feathers: ${dianaTrackerMayor.items.GRIFFIN_FEATHER}"
        },
        PartyCommand(listOf("!coins", "!coin"), { settings.dianaPartyCommands }) {
            "Coins: ${formatNumber(dianaTrackerMayor.items.COINS, withCommas = true)}"
        },
        PartyCommand(listOf("!mobs", "!mob"), { settings.dianaPartyCommands }) {
            val totalMobs = dianaTrackerMayor.mobs.TOTAL_MOBS
            val perHr = Helper.getMobsPerHr(dianaTrackerMayor, SboTimerManager.timerMayor)
            "Mobs: $totalMobs ($perHr/h)"
        },
        PartyCommand(listOf("!mf", "!magicfind"), { settings.dianaPartyCommands }) {
            "Chims (${sboData.highestChimMagicFind}% ✯) Sticks (${sboData.highestStickMagicFind}% ✯)"
        },
        PartyCommand(listOf("!playtime"), { settings.dianaPartyCommands }) {
            "Playtime: ${formatTime(dianaTrackerMayor.items.TIME)}"
        },
        PartyCommand(listOf("!profits", "!profit"), { settings.dianaPartyCommands }) {
            val playtime = dianaTrackerMayor.items.TIME
            val playTimeHrs = playtime.toDouble() / TimeUnit.HOURS.toMillis(1)
            val profit = DianaLoot.totalProfit(dianaTrackerMayor)
            val offerType = Diana.bazaarSettingDiana.toString()
            val profitHour = profit / playTimeHrs
            "Profit: ${formatNumber(profit)} (${Helper.toTitleCase(offerType)}) ${formatNumber(profitHour)}/h"
        },
    )

    private val commandMap: Map<String, PartyCommand> by lazy {
        dianaCommands.flatMap { cmd -> cmd.aliases.map { it to cmd } }.toMap()
    }

    private fun fmt(label: String, count: Int, itemKey: String, mobKey: String? = null): String {
        val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, itemKey, mobKey)
        return "$label: $count ($percent%)"
    }

    fun registerPartyChatListeners() {
        DianaStats.registerReplaceStatsMessage()
        Register.onChatMessage(commandRegex) { message, matchResult ->
            val unformattedPlayerName = matchResult.groupValues[1]
            val fullMessage = matchResult.groupValues[2]
            val messageParts = fullMessage.trim().split(Regex("\\s+"))
            val command = messageParts.getOrNull(0)?.lowercase()?.removeFormatting() ?: return@onChatMessage
            val secondArg = messageParts.getOrNull(1)
            val playerName = getPlayerName(unformattedPlayerName)
            val user = Player.getName() ?: return@onChatMessage

            val commandsWithArgs = setOf("!since", "!demote", "!promote", "!ptme", "!transfer", "!stats", "!totalstats", "!sessionstats", "!sessionstat")
            if (messageParts.size > 1 && command !in commandsWithArgs) return@onChatMessage

            when (command) {
                "!w", "!warp" -> if (settings.warpCommand) sendCommand("p warp")
                "!allinv", "!allinvite" -> if (settings.allinviteCommand) sendCommand("p setting allinvite")
                "!ptme", "!transfer" -> if (settings.transferCommand) sendCommand("p transfer $playerName")
                "!demote" -> if (settings.moteCommand) sendCommand("p demote ${secondArg ?: playerName}")
                "!promote" -> if (settings.moteCommand) sendCommand("p promote ${secondArg ?: playerName}")
                "!c", "!carrot" -> if (settings.carrotCommand) sendResponse(carrot.random())
                "!time" -> if (settings.timeCommand) sendResponse(SimpleDateFormat("HH:mm:ss").format(Date()))
                "!tps" -> if (settings.tpsCommand) sendResponse("${"%.2f".format(ServerStats.getTps())} TPS")
                "!stats", "!stat" -> if (settings.dianaPartyCommands && secondArg?.lowercase() == user.lowercase()) {
                    DianaStats.sendPlayerStats(false)
                }
                "!totalstats", "!totalstat" -> if (settings.dianaPartyCommands && secondArg?.lowercase() == user.lowercase()) {
                    DianaStats.sendPlayerStats(true)
                }
                "!sessionstats", "!sessionstat" -> if (settings.dianaPartyCommands && secondArg?.lowercase() == user.lowercase()) {
                    DianaStats.sendPlayerStats(null)
                }
                "!version" -> sendResponse("SBO version: ${SBOKotlin.version} | Minecraft version: ${SBOKotlin.mcVersion}")
                "!help" -> if (settings.dianaPartyCommands) {
                    help()
                    sendResponse("Available diana party commands: ${helpCommands.joinToString(",")}")
                }
                "!since" -> handleSinceCommand(secondArg)
                else -> commandMap[command]?.let { cmd ->
                    if (cmd.isEnabled()) sendResponse(cmd.execute())
                }
            }
        }
    }

    private fun handleSinceCommand(secondArg: String?) {
        val response = when (secondArg?.lowercase()) {
            "chimera", "chim", "chims", "chimeras", "book", "books" -> "Inqs since chim: ${sboData.inqsSinceChim}"
            "stick", "sticks" -> "Minos since stick: ${sboData.minotaursSinceStick}"
            "relic", "relics" -> "Champs since relic: ${sboData.champsSinceRelic}"
            "inq", "inqs", "inquisitor", "inquisitors", "inquis" -> "Mobs since inq: ${sboData.mobsSinceInq}"
            "lschim", "chimls", "lschimera", "chimerals", "lsbook", "bookls", "lootsharechim" -> "Inqs since lootshare chim: ${sboData.inqsSinceLsChim}"
            "kings", "king" -> "Mobs since king: ${sboData.mobsSinceKing}"
            "manti" -> "Mobs since manti: ${sboData.mobsSinceManti}"
            "core", "cores" -> "Mantis since core: ${sboData.mantiSinceCore}"
            "wool", "wools" -> "Kings since wool: ${sboData.kingSinceWool}"
            "corels", "lscore" -> "Mantis since lootshare core: ${sboData.mantiSinceLsCore}"
            "woolls", "lswool" -> "Kings since lootshare wool: ${sboData.kingSinceLsWool}"
            else -> "Mobs since inq: ${sboData.mobsSinceInq}"
        }
        sendResponse(response)
    }

    private fun sendCommand(cmd: String) = Chat.command(cmd)
    private fun sendResponse(msg: String) = Chat.pc(msg)
}
