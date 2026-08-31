package net.sbo.mod.general

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent.RunCommand
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent.ShowText
import net.minecraft.network.chat.Style
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import kotlin.math.roundToInt

object HelpCommand {
    private val commands = arrayOf(
        mapOf("cmd" to "sbo", "desc" to "Open the Settings GUI"),
        mapOf("cmd" to "sbohelp", "desc" to "Shows this message"),
        mapOf("cmd" to "sboguis", "desc" to "Open the GUIs and move them around (or: /sbomoveguis)"),
        mapOf("cmd" to "sboclearburrows", "desc" to "Clear all burrow waypoints (or: /sbocb)"),
        mapOf("cmd" to "sbocheck <player>", "desc" to "Check a player (or: /sboc <player>)"),
        mapOf("cmd" to "sbocheckp", "desc" to "Check your party (alias /sbocp)"),
        mapOf("cmd" to "sboimporttracker <profilename>", "desc" to "Import skyhanni tracker"), //todo: add sboimporttracker command
        mapOf("cmd" to "sboimporttrackerundo", "desc" to "Undo the tracker import"), // todo: add sboimporttrackerundo command
        mapOf("cmd" to "sbodc", "desc" to "Diana dropchances"),
        mapOf("cmd" to "sbosc", "desc" to "Diana spawnchances"),
        mapOf("cmd" to "sbopartyblacklist", "desc" to "Party commands blacklisting"), // todo: add sbopartyblacklist command
        mapOf("cmd" to "sbobacktrackachievements", "desc" to "Backtrack achievements"),
        mapOf("cmd" to "sboachievements", "desc" to "Opens the achievements GUI"),
        mapOf("cmd" to "sbolockachievements", "desc" to "Locks all Achievements (needs confirmation)"),
        mapOf("cmd" to "sbopde", "desc" to "Opens the Past Diana Events GUI"),
        mapOf("cmd" to "sboactiveuser", "desc" to "Shows the active user of the mod"), // todo: add sboactiveuser command
        mapOf("cmd" to "sbopf", "desc" to "Opens the PartyFinder GUI"),
        mapOf("cmd" to "sbopartycommands", "desc" to "Displays all diana partycommands"), // todo: add sbopartycommands command
        mapOf("cmd" to "sboresetavgmftracker", "desc" to "Resets the avg mf tracker"), // todo: add sboresetavgmftracker command
        mapOf("cmd" to "sboresetstatstracker", "desc" to "Resets the stats tracker"),
        mapOf("cmd" to "sboKey", "desc" to "Set your sbokey"),
        mapOf("cmd" to "sboClearKey", "desc" to "Reset your sbokey"),
        mapOf("cmd" to "sbotestmedalclip [drop|all]", "desc" to "Test Medal clip saving"),
        mapOf("cmd" to "sbosounds", "desc" to "Opens custom sounds setting Gui")
    )

    fun init() {
        Register.command("sbohelp") {
            val headerText = Component.literal("[SBO] ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Commands:").withStyle(ChatFormatting.YELLOW))

            Chat.chat(headerText)

            commands.forEach { command ->
                val cmd = command["cmd"]!!
                val description = command["desc"]!!

                val commandToRun = if (" " in cmd) cmd.substringBefore(" ") else cmd

                val fullLineText = Component.literal("> ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("/$cmd").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(description).withStyle(ChatFormatting.YELLOW))

                val styledText = fullLineText.setStyle(
                    Style.EMPTY
                        .withClickEvent(RunCommand("/$commandToRun"))
                        .withHoverEvent(
                            ShowText(
                                Component.literal("Click to run /$commandToRun").withStyle(ChatFormatting.GRAY)
                            )
                        )
                )

                Chat.chat(styledText)
            }
        }

        dropChances()
        spawnChances()
    }

    private fun spawnChances() {
        Register.command("sbosc", "sbospawnchances") { args ->
            if (args.isEmpty()) {
                Chat.chat("§6[SBO] §ePlease provide a tracking value. /sbodc <tracking>")
                return@command
            }

            val tracking = args[0].toIntOrNull()

            if (tracking == null) {
                Chat.chat("§6[SBO] §ePlease provide valid numbers. /sbodc 75")
                return@command
            }

            val defaultChances =  mapOf(
                "Inquisitor" to 0.013,
                "Sphinx" to 0.013,
                "Manticore" to 0.0026,
                "Minos King" to 0.0026
            )

            val postTrackingChances = defaultChances.mapValues { (_, chance) ->
                chance * (1 + tracking / 100.0)
            }

            postTrackingChances.forEach { (mob, chance) ->
                val formattedChance = "§b%.2f%% §7(§b1/%.0f§7)".format(chance * 100, (1 / chance * 10))
                Chat.chat("§6[SBO] §e$mob $formattedChance [TRACKING:$tracking]")
            }
        }
    }

    private fun dropChances() {
        Register.command("sbodc", "sbodropchances") { args ->
            if (args.size < 3) {
                Chat.chat("§6[SBO] §ePlease provide mf/looting values aswell as pet rarity. /sbodc <mf> <looting> <griffinrairty>")
                return@command
            }

            val mf = args[0].toIntOrNull()
            val looting = args[1].toIntOrNull()
            val rarity = args[2]
            if (mf == null || looting == null || rarity.isEmpty() && rarity.lowercase() !in listOf(
                    "epic",
                    "legendary",
                    "mythic"
                )
            ) {
                Chat.chat("§6[SBO] §ePlease provide valid numbers. /sbodc 500 5 <griffinrarity(epic/legendary/mythic)>")
                return@command
            }

            val items = when (rarity.lowercase()) {
                "epic" -> listOf("Stick" to "stick", "Relic" to "relic")
                "legendary" -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food")
                "mythic" -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food", "Wool" to "wool", "Core" to "core")
                else -> {
                    Chat.chat("§6[SBO] §ePlease provide a valid griffin rarity: epic, legendary, mythic.")
                    return@command
                }
            }

            val normalChances = Helper.getChance(mf, looting, rarity)
            val lsChances = Helper.getChance(mf, looting, rarity, lootshare = true)

            listOf(false, true).forEach { isLs ->
                val chances = if (isLs) lsChances else normalChances
                val labelFunc: (String) -> String = if (isLs) { _ -> "§7[MF:$mf]" } else { _ -> Helper.getMagicFindAndLooting(mf, looting) }

                items.forEach { (name, key) ->
                    val chance = chances[key] ?: 0.0
                    val lsPrefix = if (isLs) "§7[§bLS§7] " else ""
                    Chat.chat("§6[SBO] $lsPrefix§e$name ${Helper.formatChances(chance, labelFunc(name))}")
                }
            }
        }
    }
}
