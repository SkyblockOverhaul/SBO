package net.sbo.mod.general

import net.sbo.mod.SBOKotlin
import net.sbo.mod.diana.DianaStats
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.PartyCommands
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.calcPercentOne
import net.sbo.mod.utils.Helper.formatNumber
import net.sbo.mod.utils.Helper.formatTime
import net.sbo.mod.utils.Helper.getPlayerName
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SboTimerManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.data.DataManager.dianaTrackerMayorData
import net.sbo.mod.utils.data.DataManager.sboData
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.game.ServerStats
import net.sbo.mod.utils.version.UpdateChecker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PartyCommands {
    // How to add a new party command:
    //   PartyCommand(listOf("!alias"), { settings.dianaPartyCommands }) { "Response" }
    //   PartyCommand(listOf("!alias1", "!alias2"), { settings.someSetting }) {
    //       val count = dianaTrackerMayor.items.SOME_ITEM
    //       "Your message: $count"
    //   }
    // For count+percent: use fmt("Label", count, "ITEM_KEY", "MOB_KEY")

    private val commandRegex = Regex("^§9[^§]+ §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")

    val settings = PartyCommands

    private val carrot = listOf(
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

    private fun partyCommands() {
        Register.command("sbopartycommands", "sbopcom") {
            help()
        }
    }

    private val helpCommands = listOf(
        "!chim", "!chimls", "!stick", "!relic", "!feathers",
        "!profit", "!playtime", "!mobs", "!burrows", "!mf",
        "!stats <playername>",
        "!since (chim, chimls, relic, stick, inq, king, manti, core, corels, wool, woolls)"
    )

    private fun help() {
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
            fmt("Chimera", dianaTrackerMayorData.items.CHIMERA, dianaTrackerMayorData.mobs.MINOS_INQUISITOR) + " +${dianaTrackerMayorData.items.CHIMERA_LS} LS"
        },
        PartyCommand(listOf("!inqsls", "!inquisitorls", "!inquisls", "!lsinq", "!lsinqs", "!lsinquisitor", "!lsinquis"), { settings.dianaPartyCommands }) {
            "Inquisitor LS: ${dianaTrackerMayorData.mobs.MINOS_INQUISITOR_LS}"
        },
        PartyCommand(listOf("!inq", "!inqs", "!inquisitor", "!inquis"), { settings.dianaPartyCommands }) {
            fmt("Inquisitor", dianaTrackerMayorData.mobs.MINOS_INQUISITOR, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!kingls", "!kingsls"), { settings.dianaPartyCommands }) {
            "King LS: ${dianaTrackerMayorData.mobs.KING_MINOS_LS}"
        },
        PartyCommand(listOf("!king", "!kings"), { settings.dianaPartyCommands }) {
            fmt("King", dianaTrackerMayorData.mobs.KING_MINOS, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!sphinxls", "!sphinxsls"), { settings.dianaPartyCommands }) {
            "Sphinx LS: ${dianaTrackerMayorData.mobs.SPHINX_LS}"
        },
        PartyCommand(listOf("!sphinx", "!sphinxs"), { settings.dianaPartyCommands }) {
            fmt("Sphinx", dianaTrackerMayorData.mobs.SPHINX, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!mantils", "!mantisls"), { settings.dianaPartyCommands }) {
            "Manticore LS: ${dianaTrackerMayorData.mobs.MANTICORE_LS}"
        },
        PartyCommand(listOf("!manti", "!mantis"), { settings.dianaPartyCommands }) {
            fmt("Manticore", dianaTrackerMayorData.mobs.MANTICORE, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!dye", "!dyes"), { settings.dianaPartyCommands }) {
            fmt("Dye", dianaTrackerMayorData.items.MYTHOLOGICAL_DYE, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!burrows", "!burrow"), { settings.dianaPartyCommands }) {
            val burrows = dianaTrackerMayorData.items.TOTAL_BURROWS
            val perHr = Helper.getBurrowsPerHr(dianaTrackerMayorData, SboTimerManager.timerMayor)
            "Burrows: ${formatNumber(burrows, withCommas = true)} ($perHr/h)"
        },
        PartyCommand(listOf("!relic", "!relics"), { settings.dianaPartyCommands }) {
            fmt("Relics", dianaTrackerMayorData.items.MINOS_RELIC, dianaTrackerMayorData.mobs.MINOS_CHAMPION)
        },
        PartyCommand(listOf("!chimls", "!chimerals", "!bookls", "!lschim", "!lsbook", "!lootsharechim", "!lschimera"), { settings.dianaPartyCommands }) {
            fmt("Chimera LS", dianaTrackerMayorData.items.CHIMERA_LS, dianaTrackerMayorData.mobs.MINOS_INQUISITOR_LS)
        },
        PartyCommand(listOf("!core", "!manticore"), { settings.dianaPartyCommands }) {
            fmt("Cores", dianaTrackerMayorData.items.MANTI_CORE, dianaTrackerMayorData.mobs.MANTICORE)
        },
        PartyCommand(listOf("!corels", "!manticorels", "!lscore", "!lsmanticore"), { settings.dianaPartyCommands }) {
            fmt("Core LS", dianaTrackerMayorData.items.MANTI_CORE_LS, dianaTrackerMayorData.mobs.MANTICORE_LS)
        },
        PartyCommand(listOf("!stinger", "!fatefulstinger"), { settings.dianaPartyCommands }) {
            fmt("Stingers", dianaTrackerMayorData.items.FATEFUL_STINGER, dianaTrackerMayorData.mobs.MANTICORE)
        },
        PartyCommand(listOf("!stingerls", "!fatefulstingerls", "!lsstinger", "!lsfatefulstinger"), { settings.dianaPartyCommands }) {
            fmt("Stinger LS", dianaTrackerMayorData.items.FATEFUL_STINGER_LS, dianaTrackerMayorData.mobs.MANTICORE_LS)
        },
        PartyCommand(listOf("!wool", "!shimmering", "!shimmeringwool"), { settings.dianaPartyCommands }) {
            fmt("Wool", dianaTrackerMayorData.items.SHIMMERING_WOOL, dianaTrackerMayorData.mobs.KING_MINOS)
        },
        PartyCommand(listOf("!woolls", "!shimmeringwoolls", "!lsshimmering", "!lsshimmeringwool"), { settings.dianaPartyCommands }) {
            fmt("Wool LS", dianaTrackerMayorData.items.SHIMMERING_WOOL_LS, dianaTrackerMayorData.mobs.KING_MINOS_LS)
        },
        PartyCommand(listOf("!food", "!brainfood", "!brain"), { settings.dianaPartyCommands }) {
            fmt("Brain Food", dianaTrackerMayorData.items.BRAIN_FOOD, dianaTrackerMayorData.mobs.SPHINX)
        },
        PartyCommand(listOf("!foodls", "!brainfoodls", "!lsbrainfood", "!lsbrain"), { settings.dianaPartyCommands }) {
            fmt("Brain Food LS", dianaTrackerMayorData.items.BRAIN_FOOD_LS, dianaTrackerMayorData.mobs.SPHINX_LS)
        },
        PartyCommand(listOf("!braided", "!braideds"), { settings.dianaPartyCommands }) {
            fmt("Braided feathers", dianaTrackerMayorData.items.BRAIDED_GRIFFIN_FEATHER, dianaTrackerMayorData.mobs.TOTAL_MOBS)
        },
        PartyCommand(listOf("!kingshard", "!kingshards"), { settings.dianaPartyCommands }) {
            fmt("King Shards", dianaTrackerMayorData.items.KING_MINOS_SHARD, dianaTrackerMayorData.mobs.KING_MINOS)
        },
        PartyCommand(listOf("!sphinxshard", "!sphinxshards"), { settings.dianaPartyCommands }) {
            fmt("Sphinx Shards", dianaTrackerMayorData.items.SPHINX_SHARD, dianaTrackerMayorData.mobs.SPHINX)
        },
        PartyCommand(listOf("!minotaurshard", "!minotaurshards"), { settings.dianaPartyCommands }) {
            fmt("Minotaur Shards", dianaTrackerMayorData.items.MINOTAUR_SHARD, dianaTrackerMayorData.mobs.MINOTAUR)
        },
        PartyCommand(listOf("!certanshard", "!certanshards"), { settings.dianaPartyCommands }) {
            fmt("Certan Shards", dianaTrackerMayorData.items.CRETAN_BULL_SHARD, dianaTrackerMayorData.mobs.CRETAN_BULL)
        },
        PartyCommand(listOf("!mythofrag", "!frags"), { settings.dianaPartyCommands }) {
            "Mytho Frags: ${dianaTrackerMayorData.items.MYTHOS_FRAGMENT}"
        },
        PartyCommand(listOf("!urns", "!urn", "!cretanurn"), { settings.dianaPartyCommands }) {
            fmt("Urns", dianaTrackerMayorData.items.CRETAN_URN, dianaTrackerMayorData.mobs.CRETAN_BULL)
        },
        PartyCommand(listOf("!hilt", "!hiltofrevelations"), { settings.dianaPartyCommands }) {
            fmt("Hilts", dianaTrackerMayorData.items.HILT_OF_REVELATIONS, dianaTrackerMayorData.mobs.MINOS_HUNTER)
        },
        PartyCommand(listOf("!sticks", "!stick"), { settings.dianaPartyCommands }) {
            fmt("Sticks", dianaTrackerMayorData.items.DAEDALUS_STICK, dianaTrackerMayorData.mobs.MINOTAUR)
        },
        PartyCommand(listOf("!feathers", "!feather"), { settings.dianaPartyCommands }) {
            "Feathers: ${dianaTrackerMayorData.items.GRIFFIN_FEATHER}"
        },
        PartyCommand(listOf("!coins", "!coin"), { settings.dianaPartyCommands }) {
            "Coins: ${formatNumber(dianaTrackerMayorData.items.COINS, withCommas = true)}"
        },
        PartyCommand(listOf("!mobs", "!mob"), { settings.dianaPartyCommands }) {
            val totalMobs = dianaTrackerMayorData.mobs.TOTAL_MOBS
            val perHr = Helper.getMobsPerHr(dianaTrackerMayorData, SboTimerManager.timerMayor)
            "Mobs: $totalMobs ($perHr/h)"
        },
        PartyCommand(listOf("!mf", "!magicfind"), { settings.dianaPartyCommands }) {
            "Wool (${sboData.highestWoolMagicFind}% ✯) Manticore (${sboData.highestCoreMagicFind}% ✯) Stinger (${sboData.highestStingerMagicFind}% ✯) Chim (${sboData.highestChimMagicFind}% ✯) Relic (${sboData.highestRelicMagicFind}% ✯) Food (${sboData.highestFoodMagicFind}% ✯) Stick (${sboData.highestStickMagicFind}% ✯)"
        },
        PartyCommand(listOf("!playtime"), { settings.dianaPartyCommands }) {
            "Playtime: ${formatTime(dianaTrackerMayorData.items.TIME)}"
        },
        PartyCommand(listOf("!profits", "!profit"), { settings.dianaPartyCommands }) {
            val playtime = dianaTrackerMayorData.items.TIME
            val playTimeHrs = playtime.toDouble() / TimeUnit.HOURS.toMillis(1)
            val profit = DianaLoot.totalProfit(dianaTrackerMayorData)
            val offerType = if (Diana.ironmanOverrides) "NPC Sell" else Diana.bazaarSettingDiana.toString()
            val profitHour = profit / playTimeHrs
            "Profit: ${formatNumber(profit)} (${Helper.toTitleCase(offerType)}) ${formatNumber(profitHour)}/h"
        },
    )

    private val commandMap: Map<String, PartyCommand> by lazy {
        dianaCommands.flatMap { cmd -> cmd.aliases.map { it to cmd } }.toMap()
    }

    private fun fmt(label: String, count: Int, denominator: Int): String {
        val percent = calcPercentOne(count, denominator)
        return "$label: $count ($percent%)"
    }

    private fun registerPartyChatListeners() {
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
                "!tps" -> if (settings.tpsCommand) sendResponse(ServerStats.getTpsString())
                "!stats", "!stat" -> if (settings.dianaPartyCommands && secondArg.equals(user, ignoreCase = true)) {
                    DianaStats.sendPlayerStats()
                }
                "!totalstats", "!totalstat" -> if (settings.dianaPartyCommands && secondArg.equals(user, ignoreCase = true)) {
                    DianaStats.sendPlayerStats(total = true)
                }
                "!sessionstats", "!sessionstat" -> if (settings.dianaPartyCommands && secondArg.equals(user, ignoreCase = true)) {
                    DianaStats.sendPlayerStats(null)
                }
                "!version" -> sendResponse("SBO version: ${SBOKotlin.version} | Minecraft version: ${SBOKotlin.mcVersion}")
                "!mod" -> sendResponse("https://modrinth.com/mod/${UpdateChecker.MODRINTH_ID}")
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
            else -> return
        }
        sendResponse(response)
    }

    private fun sendCommand(cmd: String) = Chat.command(cmd)
    private fun sendResponse(msg: String) {
        Chat.pc("[SBO] $msg")
    }
}
