package net.sbo.mod.general

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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date

object PartyCommands {

    val commandRegex = Regex("^§[0-9a-fk-or]Party §[0-9a-fk-or]> (.*?)§[0-9a-fk-or]*: ?(.*)$")

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
        "!chim",
        "!chimls",
        "!stick",
        "!relic",
        "!feathers",
        "!profit",
        "!playtime",
        "!mobs",
        "!burrows",
        "!stats <playername>",
        "!since (chim, chimls, relic, stick, inq, king, manti, core, corels, wool, woolls)"
    )

    fun help() {
        Chat.chat("§6[SBO] §eDiana party commands:")
        helpCommands.forEach {
            Chat.chat("§7> §a$it")
        }
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
                "!w", "!warp" -> {
                    if (!settings.warpCommand) return@onChatMessage
                    sleep(200) {
                        Chat.command("p warp")
                    }
                }
                "!allinv", "!allinvite" -> {
                    if (!settings.allinviteCommand) return@onChatMessage
                    sleep(200) {
                        Chat.command("p setting allinvite")
                    }
                }
                "!ptme", "!transfer" -> {
                    if (!settings.transferCommand) return@onChatMessage
                    sleep(200) {
                        Chat.command("p transfer $playerName")
                    }
                }
                "!demote" -> {
                    if (!settings.moteCommand) return@onChatMessage
                    val targetPlayer = secondArg ?: playerName
                    sleep(200) {
                        Chat.command("p demote $targetPlayer")
                    }
                }
                "!promote" -> {
                    if (!settings.moteCommand) return@onChatMessage
                    val targetPlayer = secondArg ?: playerName
                    sleep(200) {
                        Chat.command("p promote $targetPlayer")
                    }
                }
                "!c", "!carrot" -> {
                    if (!settings.carrotCommand) return@onChatMessage
                    val randomIndex = (carrot.indices).random()
                    val response = carrot[randomIndex]
                    sleep(200) {
                        Chat.command("pc $response")
                    }
                }
                "!time" -> {
                    if (!settings.timeCommand) return@onChatMessage
                    sleep(200) {
                        val currentTime = SimpleDateFormat("HH:mm:ss").format(Date())
                        Chat.command("pc $currentTime")
                    }
                }
                "!tps" -> {
                    if (!settings.tpsCommand) return@onChatMessage
                    sleep(200) {
                        val tps = ServerStats.getTps()
                        Chat.command("pc ${"%.2f".format(tps)} TPS")
                    }
                }
                "!chim", "!chimera", "!chims", "!chimeras", "!book", "!books" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val chimeraCount = dianaTrackerMayor.items.CHIMERA
                    val chimeraLsCount = dianaTrackerMayor.items.CHIMERA_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "CHIMERA", "MINOS_INQUISITOR")
                    sleep(200) {
                        Chat.command("pc Chimera: $chimeraCount ($percent%) +$chimeraLsCount LS")
                    }
                }
                "!inqsls", "!inquisitorls", "!inquisls", "!lsinq", "!lsinqs", "!lsinquisitor", "!lsinquis" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    sleep(200) {
                        Chat.command("pc Inquisitor LS: ${dianaTrackerMayor.mobs.MINOS_INQUISITOR_LS}")
                    }
                }
                "!inq", "!inqs", "!inquisitor", "!inquis" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val inquisCount = dianaTrackerMayor.mobs.MINOS_INQUISITOR
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "MINOS_INQUISITOR")
                    sleep(200) {
                        Chat.command("pc Inquisitor: $inquisCount ($percent%)")
                    }
                }
                "!burrows", "!burrow" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val burrows = dianaTrackerMayor.items.TOTAL_BURROWS
                    val timer = SboTimerManager.timerMayor
                    val burrowsPerHr = Helper.getBurrowsPerHr(dianaTrackerMayor, timer)
                    sleep(200) {
                        Chat.command("pc Burrows: $burrows ($burrowsPerHr/h)")
                    }
                }
                "!relic", "!relics" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val relicCount = dianaTrackerMayor.items.MINOS_RELIC
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "MINOS_RELIC", "MINOS_CHAMPION")
                    sleep(200) {
                        Chat.command("pc Relics: $relicCount ($percent%)")
                    }
                }
                "!chimls", "!chimerals", "!bookls", "!lschim", "!lsbook", "!lootsharechim", "!lschimera" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val chimsLs = dianaTrackerMayor.items.CHIMERA_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "CHIMERALS", "MINOS_INQUISITOR_LS")
                    sleep(200) {
                        Chat.command("pc Chimera LS: $chimsLs ($percent%)")
                    }
                }
                "!core", "!manticore" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val coreCount = dianaTrackerMayor.items.MANTI_CORE
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "MANTI_CORE", "MANTICORE")
                    sleep(200) {
                        Chat.command("pc Cores: $coreCount ($percent%)")
                    }
                }
                "!corels", "!manticorels", "!lscore", "!lsmanticore" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val coreLsCount = dianaTrackerMayor.items.MANTI_CORE_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "MANTI_CORE_LS", "MANTICORE_LS")
                    sleep(200) {
                        Chat.command("pc Core LS: $coreLsCount ($percent%)")
                    }
                }
                "!stinger", "!fatefulstinger" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val stingerCount = dianaTrackerMayor.items.FATEFUL_STINGER
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "FATEFUL_STINGER", "MANTICORE")
                    sleep(200) {
                        Chat.command("pc Stingers: $stingerCount ($percent%)")
                    }
                }
                "!stingerls", "!fatefulstingerls", "!lsstinger", "!lsfatefulstinger" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val stingerLsCount = dianaTrackerMayor.items.FATEFUL_STINGER_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "FATEFUL_STINGER_LS", "MANTICORE_LS")
                    sleep(200) {
                        Chat.command("pc Stinger LS: $stingerLsCount ($percent%)")
                    }
                }
                "!wool", "!shimmering", "!shimmeringwool" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val woolCount = dianaTrackerMayor.items.SHIMMERING_WOOL
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "SHIMMERING_WOOL", "KING_MINOS")
                    sleep(200) {
                        Chat.command("pc Wool: $woolCount ($percent%)")
                    }
                }
                "!woolls", "!shimmeringwoolls", "!lsshimmering", "!lsshimmeringwool" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val woolLsCount = dianaTrackerMayor.items.SHIMMERING_WOOL_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "SHIMMERING_WOOL_LS", "KING_MINOS_LS")
                    sleep(200) {
                        Chat.command("pc Wool LS: $woolLsCount ($percent%)")
                    }
                }
                "!food", "!brainfood", "!brain" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val foodCount = dianaTrackerMayor.items.BRAIN_FOOD
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "BRAIN_FOOD", "SPHINX")
                    sleep(200) {
                        Chat.command("pc Brain Food: $foodCount ($percent%)")
                    }
                }
                "!foodls", "!brainfoodls", "!lsbrainfood", "!lsbrain" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val foodLsCount = dianaTrackerMayor.items.BRAIN_FOOD_LS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "BRAIN_FOOD_LS", "SPHINX_LS")
                    sleep(200) {
                        Chat.command("pc Brain Food LS: $foodLsCount ($percent%)")
                    }
                }
                "!kingshard", "!kingshards" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val kingShardCount = dianaTrackerMayor.items.KING_MINOS_SHARD
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "KING_MINOS_SHARD", "KING_MINOS")
                    sleep(200) {
                        Chat.command("pc King Shards: $kingShardCount ($percent%)")
                    }
                }
                "!sphinxshard", "!sphinxshards" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val sphinxShardCount = dianaTrackerMayor.items.SPHINX_SHARD
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "SPHINX_SHARD", "SPHINX")
                    sleep(200) {
                        Chat.command("pc Sphinx Shards: $sphinxShardCount ($percent%)")
                    }
                }
                "!minotaurshard", "!minotaurshards" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val minotaurShardCount = dianaTrackerMayor.items.MINOTAUR_SHARD
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "MINOTAUR_SHARD", "MINOTAUR")
                    sleep(200) {
                        Chat.command("pc Minotaur Shards: $minotaurShardCount ($percent%)")
                    }
                }
                "!certanshard", "!certanshards" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val certanShardCount = dianaTrackerMayor.items.CRETAN_BULL_SHARD
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "CRETAN_BULL_SHARD", "CRETAN_BULL")
                    sleep(200) {
                        Chat.command("pc Certan Shards: $certanShardCount ($percent%)")
                    }
                }
                "!mythofrag", "!frags" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val mythoFragCount = dianaTrackerMayor.items.MYTHOS_FRAGMENT
                    sleep(200) {
                        Chat.command("pc Mytho Frags: $mythoFragCount")
                    }
                }
                "!urns", "!urn", "!cretanurn" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val urnCount = dianaTrackerMayor.items.CRETAN_URN
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "CRETAN_URN", "CRETAN_BULL")
                    sleep(200) {
                        Chat.command("pc Urns: $urnCount ($percent%)")
                    }
                }
                "!hilt", "!hiltofrevelations" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val hiltCount = dianaTrackerMayor.items.HILT_OF_REVELATIONS
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "HILT_OF_REVELATIONS", "MINOS_HUNTER")
                    sleep(200) {
                        Chat.command("pc Hilts: $hiltCount ($percent%)")
                    }
                }
                "!sticks", "!stick" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val stickCount = dianaTrackerMayor.items.DAEDALUS_STICK
                    val percent = calcPercentOne(dianaTrackerMayor.items, dianaTrackerMayor.mobs, "DAEDALUS_STICK", "MINOTAUR")
                    sleep(200) {
                        Chat.command("pc Sticks: $stickCount ($percent%)")
                    }
                }
                "!feathers", "!feather" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val featherCount = dianaTrackerMayor.items.GRIFFIN_FEATHER
                    sleep(200) {
                        Chat.command("pc Feathers: $featherCount")
                    }
                }
                "!coins", "!coin" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    sleep(200) {
                        Chat.command("pc Coins: ${formatNumber(dianaTrackerMayor.items.COINS, withCommas = true)}")
                    }
                }
                "!mobs", "!mob" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val totalMobs = dianaTrackerMayor.mobs.TOTAL_MOBS
                    val timer = SboTimerManager.timerMayor
                    val mobsPerHr = Helper.getMobsPerHr(dianaTrackerMayor, timer)
                    sleep(200) {
                        Chat.command("pc Mobs: $totalMobs ($mobsPerHr/h)")
                    }
                }
                "!mf", "!magicfind" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    sleep(200) {
                        Chat.command("pc Chims (${sboData.highestChimMagicFind}% ✯) Sticks (${sboData.highestStickMagicFind}% ✯)")
                    }
                }
                "!playtime" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    sleep(200) {
                        Chat.command("pc Playtime: ${formatTime(dianaTrackerMayor.items.TIME)}")
                    }
                }
                "!profits", "!profit" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    val playtime = dianaTrackerMayor.items.TIME
                    val playTimeHrs = playtime.toDouble() / TimeUnit.HOURS.toMillis(1)
                    sleep(200) {
                        val profit = DianaLoot.totalProfit(dianaTrackerMayor)
                        val offerType = Diana.bazaarSettingDiana.toString()
                        val profitHour = profit / playTimeHrs
                        Chat.command("pc Profit: ${formatNumber(profit)} (${Helper.toTitleCase(offerType)}) ${formatNumber(profitHour)}/h")
                    }
                }
                "!stats", "!stat" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    if (secondArg?.lowercase() == user.lowercase()) {
                        sleep(200) {
                            DianaStats.sendPlayerStats(false)
                        }
                    }
                }
                "!totalstats", "!totalstat" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    if (secondArg?.lowercase() == user.lowercase()) {
                        sleep(200) {
                            DianaStats.sendPlayerStats(true)
                        }
                    }
                }
                "!sessionstats", "!sessionstat" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    if (secondArg?.lowercase() == user.lowercase()) {
                        sleep(200) {
                            DianaStats.sendPlayerStats(null)
                        }
                    }
                }
                "!since" -> {
                    val secondArg = messageParts.getOrNull(1)?.lowercase()
                    when (secondArg) {
                        "chimera", "chim", "chims", "chimeras", "book", "books" -> sleep(200) {
                            Chat.command("pc Inqs since chim: ${sboData.inqsSinceChim}")
                        }
                        "stick", "sticks" -> sleep(200) {
                            Chat.command("pc Minos since stick: ${sboData.minotaursSinceStick}")
                        }
                        "relic", "relics" -> sleep(200) {
                            Chat.command("pc Champs since relic: ${sboData.champsSinceRelic}")
                        }
                        "inq", "inqs", "inquisitor", "inquisitors", "inquis" -> sleep(200) {
                            Chat.command("pc Mobs since inq: ${sboData.mobsSinceInq}")
                        }
                        "lschim", "chimls", "lschimera", "chimerals", "lsbook", "bookls", "lootsharechim" -> sleep(200) {
                            Chat.command("pc Inqs since lootshare chim: ${sboData.inqsSinceLsChim}")
                        }
                        "kings", "king" -> sleep(200) {
                            Chat.command("pc Mobs since king: ${sboData.mobsSinceKing}")
                        }
                        "manti" -> sleep(200) {
                            Chat.command("pc Mobs since manti: ${sboData.mobsSinceManti}")
                        }
                        "core", "cores" -> sleep(200) {
                            Chat.command("pc Mantis since core: ${sboData.mantiSinceCore}")
                        }
                        "wool", "wools" -> sleep(200) {
                            Chat.command("pc Kings since wool: ${sboData.kingSinceWool}")
                        }
                        "corels", "lscore" -> sleep(200) {
                            Chat.command("pc Mantis since lootshare core: ${sboData.mantiSinceLsCore}")
                        }
                        "woolls", "lswool" -> sleep(200) {
                            Chat.command("pc Kings since lootshare wool: ${sboData.kingSinceLsWool}")
                        }
                    }
                    if (secondArg == null) {
                        sleep(200) {
                            Chat.command("pc Mobs since inq: ${sboData.mobsSinceInq}")
                        }
                    }
                }
                "!help" -> {
                    if (!settings.dianaPartyCommands) return@onChatMessage
                    sleep(200) {
                        help() // will work for people with SBO

                        // Also send in party chat in case someone is curious what diana party commands SBO has without having SBO
                        val commands = helpCommands.joinToString(",")
                        Chat.command("pc Available diana party commands: $commands")
                    }
                }
            }
        }
    }

}
