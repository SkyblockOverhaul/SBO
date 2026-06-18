package net.sbo.mod.general

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent.RunCommand
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent.ShowText
import net.minecraft.network.chat.Style
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.SoundHandler
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register

object HelpCommand {
    val commands = arrayOf(
        mapOf("cmd" to "sbo", "desc" to "Open the Settings GUI"),
        mapOf("cmd" to "sbohelp", "desc" to "Shows this message"),
        mapOf("cmd" to "sboguis", "desc" to "Open the GUIs and move them around (or: /sbomoveguis)"),
        mapOf("cmd" to "sboclearburrows", "desc" to "Clear all burrow waypoints (or: /sbocb)"),
        mapOf("cmd" to "sbocheck <player>", "desc" to "Check a player (or: /sboc <player>)"),
        mapOf("cmd" to "sbocheckp", "desc" to "Check your party (alias /sbocp)"),
        mapOf("cmd" to "sboimporttracker <profilename>", "desc" to "Import skyhanni tracker"), //todo: add sboimporttracker command
        mapOf("cmd" to "sboimporttrackerundo", "desc" to "Undo the tracker import"), // todo: add sboimporttrackerundo command
        mapOf("cmd" to "sbodc", "desc" to "Diana dropchances"),
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
        mapOf("cmd" to "sbots <sound> [volume]", "desc" to "Test a custom sound (use filename without .ogg)")
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

                val commandToRun = if (cmd.contains(" ")) cmd.substringBefore(" ") else cmd

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
        testSound()
    }

    fun testSound() {
        Register.command("sbots", "sbotsound", "sbotestsound") { args ->
            if (args.isEmpty()) {
                val available = SoundHandler.getAvailableSoundsList().take(10).joinToString(", ")
                val more = if (SoundHandler.getAvailableSoundsList().size > 10) " and ${SoundHandler.getAvailableSoundsList().size - 10} more" else ""
                Chat.chat("§6[SBO] §cUsage: /sbots <soundname> [volume]")
                Chat.chat("§6[SBO] §eAvailable sounds: §a$available$more")
                return@command
            }

            val sound = args[0]
            val volume = args.getOrNull(1)?.toFloatOrNull() ?: 1.0f

            if (!SoundHandler.hasSound(sound)) {
                Chat.chat("§6[SBO] §cSound '$sound' not found.")
                val available = SoundHandler.getAvailableSoundsList().take(5).joinToString(", ")
                Chat.chat("§6[SBO] §eAvailable: $available")
                return@command
            }

            SoundHandler.playCustomSound(sound, volume)
            Chat.chat("§6[SBO] §aPlaying sound: §e$sound §aat volume §e$volume")
        }
    }

    fun dropChances() {
        Register.command("sbodc", "sbodropchances") { args ->
            if (args.size < 2) {
                Chat.chat("§6[SBO] §ePlease provide mf/looting values. /sbodc <mf> <looting> <griffinrairty>")
                return@command
            }

            val mf = args[0].toIntOrNull()
            val looting = args[1].toIntOrNull()
            val rarity = args[2]
            if (mf == null || looting == null || (rarity.isEmpty() && rarity.lowercase() !in listOf("epic", "legendary", "mythic"))) {
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
            val lsChances = Helper.getChance(mf, looting, rarity, true)

            (listOf(false, true)).forEach { isLs ->
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