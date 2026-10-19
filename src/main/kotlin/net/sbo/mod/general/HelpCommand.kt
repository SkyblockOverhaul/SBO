package net.sbo.mod.general

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent.RunCommand
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent.ShowText
import net.minecraft.network.chat.Style
import net.sbo.guilib.fabric.GuiLib
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.HubGui
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import kotlin.math.roundToInt

object HelpCommand {
    val commands = arrayOf(
        mapOf("cmd" to "sbo", "desc" to "Open the SBO start screen: settings, party finder, sounds and more"),
        mapOf("cmd" to "sbosettings", "desc" to "Open the settings (or: /sbo settings, /sboconfig)"),
        mapOf("cmd" to "sbohelp", "desc" to "Open this GUI"),
        mapOf("cmd" to "sboguis", "desc" to "Open the GUIs and move them around (or: /sbomoveguis)"),
        mapOf("cmd" to "sboclearburrows", "desc" to "Clear all burrow waypoints (or: /sbocb)"),
        mapOf("cmd" to "sboimporttracker <profilename>", "desc" to "Import skyhanni tracker"), //todo: add sboimporttracker command
        mapOf("cmd" to "sboimporttrackerundo", "desc" to "Undo the tracker import"), // todo: add sboimporttrackerundo command
        mapOf("cmd" to "sbodc", "desc" to "Diana dropchances (main server)"),
        mapOf("cmd" to "sbodcnew", "desc" to "Diana dropchances (Alpha Network)"),
        mapOf("cmd" to "sbosc", "desc" to "Diana spawnchances"),
        mapOf("cmd" to "sbopartyblacklist", "desc" to "Party commands blacklisting"), // todo: add sbopartyblacklist command
        mapOf("cmd" to "sbobacktrackachievements", "desc" to "Backtrack achievements"),
        mapOf("cmd" to "sboachievements", "desc" to "Opens the achievements GUI"),
        mapOf("cmd" to "sbolockachievements", "desc" to "Locks all Achievements (needs confirmation)"),
        mapOf("cmd" to "sboevents", "desc" to "Opens the Diana Events GUI (also /sbopde)"),
        mapOf("cmd" to "sboactiveuser", "desc" to "Shows the active user of the mod"), // todo: add sboactiveuser command
        mapOf("cmd" to "sbopf", "desc" to "Opens the PartyFinder GUI"),
        mapOf("cmd" to "sbopartycommands", "desc" to "Displays all diana partycommands"), // todo: add sbopartycommands command
        mapOf("cmd" to "sboresetavgmftracker", "desc" to "Resets the avg mf tracker"), // todo: add sboresetavgmftracker command
        mapOf("cmd" to "sboresetstatstracker", "desc" to "Resets the stats tracker"),
        mapOf("cmd" to "sboKey", "desc" to "Set your sbokey"),
        mapOf("cmd" to "sboClearKey", "desc" to "Reset your sbokey"),
        mapOf("cmd" to "sbocloud", "desc" to "Opens the Cloud Sync window (also to load backups)"),
        mapOf("cmd" to "sbobadge", "desc" to "Edit your SBO badge (supporters, maintainers and moderators)"),
        mapOf("cmd" to "sboconflicts", "desc" to "Shows mods that break SBO features and sends a report"),
        mapOf("cmd" to "sbotestmedalclip [drop|all]", "desc" to "Test Medal clip saving"),
        mapOf("cmd" to "sbosounds", "desc" to "Opens custom sounds setting Gui")
    )

    fun init() {
        Register.command("sbohelp") {
            mc.execute {
                HubGui.openCommands()
            }
        }

        dropChances()
        newDropChances()
        spawnChances()
    }

    private fun spawnChances() {
        Register.command("sbosc", "sbospawnchances") { args ->
            if (args.isEmpty()) {
                Chat.chat("§6[SBO] §ePlease provide a tracking value. /sbosc <tracking>")
                return@command
            }

            val tracking = args[0].toIntOrNull()

            if (tracking == null) {
                Chat.chat("§6[SBO] §ePlease provide valid numbers. /sbosc 75")
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
                val formattedChance = "§b%.2f%% §7(§b1/%.0f§7)".format(chance * 100, (1 / chance))
                Chat.chat("§6[SBO] §e$mob $formattedChance [TRACKING:$tracking]")
            }
        }
    }

    private fun dropChances() {
        Register.command("sbodc", "sbodropchances") { args ->
            // TODO: When the Alpha Network update is pushed to main by Hypixel, remove looting and rename newDropChances() to dropChances().
            if (args.size < 3) {
                Chat.chat("§6[SBO] §ePlease provide MF, looting, and pet rarity. /sbodc <mf> <looting> <griffinrarity>")
                return@command
            }

            val mf = args[0].toIntOrNull()
            val looting = args[1].toIntOrNull()
            val rarity = args[2].lowercase().trim()
            if (mf == null || looting == null || rarity !in listOf("epic", "legendary", "mythic")) {
                Chat.chat("§6[SBO] §ePlease provide valid values. /sbodc 500 5 <griffinrarity(epic/legendary/mythic)>")
                return@command
            }

            val items = when (rarity) {
                "epic" -> listOf("Stick" to "stick", "Relic" to "relic")
                "legendary" -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food")
                else -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food", "Wool" to "wool", "Core" to "core", "Stinger" to "stinger")
            }

            val normalChances = Helper.getChance(mf, looting, rarity)
            val lsChances = Helper.getChance(mf, looting, rarity, lootshare = true)
            listOf(false, true).forEach { isLs ->
                val chances = if (isLs) lsChances else normalChances
                val label = if (isLs) "§7[MF:$mf]" else Helper.formatMagicFindAndLooting(mf, looting)
                items.forEach { (name, key) ->
                    val lsPrefix = if (isLs) "§7[§bLS§7] " else ""
                    Chat.chat("§6[SBO] $lsPrefix§e$name ${Helper.formatChances(chances[key] ?: 0.0, label)}")
                }
            }
        }
    }

    // TODO: When the Alpha Network update is pushed to main by Hypixel, remove dropChances() and rename newDropChances() to dropChances().
    private fun newDropChances() {
        Register.command("sbodcnew", "sbodropchancesnew") { args ->
            if (args.size < 2) {
                Chat.chat("§6[SBO] §ePlease provide MF and pet rarity. /sbodcnew <mf> <griffinrarity>")
                return@command
            }

            val mf = args[0].toIntOrNull()
            val rarity = args[1].lowercase().trim()

            if (mf == null) {
                Chat.chat("§6[SBO] §ePlease provide a valid Magic Find value. /sbodcnew 500 <griffinrarity(epic/legendary/mythic)>")
                return@command
            }

            val items = when (rarity) {
                "epic" -> listOf("Stick" to "stick", "Relic" to "relic")
                "legendary" -> listOf(
                    "Chimera" to "chim",
                    "Stick" to "stick",
                    "Relic" to "relic",
                    "Food" to "food"
                )
                "mythic" -> listOf(
                    "Chimera" to "chim",
                    "Stick" to "stick",
                    "Relic" to "relic",
                    "Food" to "food",
                    "Wool" to "wool",
                    "Core" to "core",
                    "Stinger" to "stinger"
                )
                else -> {
                    Chat.chat("§6[SBO] §ePlease provide a valid griffin rarity: epic, legendary, mythic.")
                    return@command
                }
            }

            val normalChances = Helper.getChanceNew(mf, rarity)
            val lsChances = Helper.getChanceNew(mf, rarity, lootshare = true)

            listOf(false, true).forEach { isLs ->
                val chances = if (isLs) lsChances else normalChances
                val labelFunc: (String) -> String = { _ -> Helper.formatMagicFind(mf) }

                items.forEach { (name, key) ->
                    val chance = chances[key] ?: 0.0
                    val lsPrefix = if (isLs) "§7[§bLS§7] " else ""
                    Chat.chat("§6[SBO] $lsPrefix§e$name ${Helper.formatChances(chance, labelFunc(name))}")
                }
            }
        }
    }
}
