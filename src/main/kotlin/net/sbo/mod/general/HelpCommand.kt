package net.sbo.mod.general

import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.HubGui
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import java.util.Locale

object HelpCommand {
    val commands = arrayOf(
        mapOf("cmd" to "sbo", "desc" to "Open the SBO start screen: settings, party finder, sounds and more"),
        mapOf("cmd" to "sbosettings", "desc" to "Open the settings (or: /sbo settings, /sboconfig)"),
        mapOf("cmd" to "sbohelp", "desc" to "Open this GUI"),
        mapOf("cmd" to "sboguis", "desc" to "Open the GUIs and move them around (or: /sbomoveguis)"),
        mapOf("cmd" to "sboclearburrows", "desc" to "Clear all burrow waypoints (or: /sbocb)"),
        mapOf("cmd" to "sboimporttracker <profilename>", "desc" to "Import skyhanni tracker"), //todo: add sboimporttracker command
        mapOf("cmd" to "sboimporttrackerundo", "desc" to "Undo the tracker import"), // todo: add sboimporttrackerundo command
        mapOf("cmd" to "sbodc <mf> <griffinrarity>", "desc" to "Diana dropchances"),
        mapOf("cmd" to "sbosc", "desc" to "Diana spawnchances"),
        mapOf("cmd" to "sbopartyblacklist", "desc" to "Party commands blacklisting"), // todo: add sbopartyblacklist command
        mapOf("cmd" to "sbobacktrackachievements", "desc" to "Backtrack achievements"),
        mapOf("cmd" to "sboachievements", "desc" to "Opens the achievements GUI"),
        mapOf("cmd" to "sboresetachievements", "desc" to "Resets all achievements (asks first)"),
        mapOf("cmd" to "sboevents", "desc" to "Opens the Diana Events GUI (also /sbopde)"),
        mapOf("cmd" to "sboactiveuser", "desc" to "Shows the active user of the mod"), // todo: add sboactiveuser command
        mapOf("cmd" to "sbopf", "desc" to "Opens the PartyFinder GUI"),
        mapOf("cmd" to "sbopv [name]", "desc" to "Shows the stats of a player, or of your party with /sbopv party"),
        mapOf("cmd" to "sbopartycommands", "desc" to "Displays all diana partycommands"), // todo: add sbopartycommands command
        mapOf("cmd" to "sboresetavgmftracker", "desc" to "Resets the avg mf tracker"), // todo: add sboresetavgmftracker command
        mapOf("cmd" to "sboresetstatstracker", "desc" to "Resets the stats tracker"),
        mapOf("cmd" to "sbolegacylook <yes|no>", "desc" to "Switch all trackers to the legacy look (yes) or keep the new one (no)"),
        mapOf("cmd" to "sboKey", "desc" to "Set your sbokey"),
        mapOf("cmd" to "sboClearKey", "desc" to "Reset your sbokey"),
        mapOf("cmd" to "sbocloud", "desc" to "Opens the Cloud Sync window (also to load backups)"),
        mapOf("cmd" to "sbocustomizer", "desc" to "Your customizer, name look and player size (supporters, maintainers and moderators)"),
        mapOf("cmd" to "sboinforeport", "desc" to "Sends your SBO settings and mods that break SBO features to the SBO team"),
        mapOf("cmd" to "sbotestmedalclip [drop|all]", "desc" to "Test Medal clip saving"),
        mapOf("cmd" to "sbosounds", "desc" to "Opens custom sounds setting Gui"),
        mapOf("cmd" to "sbosetprimarycoa", "desc" to "Set your primary Crown of Avarice to the equipped helmet"),
        mapOf("cmd" to "sboresetcoasession", "desc" to "Resets only the CoA session tracker"),
        mapOf("cmd" to "sboresetcoaevent", "desc" to "Resets only the CoA event tracker")
    )

    fun init() {
        Register.command("sbohelp") {
            mc.execute {
                HubGui.openCommands()
            }
        }

        dropChances()
        spawnChances()
    }

    /** The griffin rarities /sbodc takes. */
    private val RARITIES = listOf("epic", "legendary", "mythic")

    private fun spawnChances() {
        Register.command("sbosc", "sbospawnchances", argName = "tracking") { args ->
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
                val formattedChance = "§b%.2f%% §7(§b1/%.0f§7)".format(Locale.ROOT, chance * 100, 1 / chance)
                Chat.chat("§6[SBO] §e$mob $formattedChance [TRACKING:$tracking]")
            }
        }
    }

    private fun dropChances() {
        Register.command("sbodc", "sbodropchances", argName = "mf> <griffinrarity", completions = { if (it.size == 1) RARITIES else emptyList() }) { args ->
            val mf = args.getOrNull(0)?.toIntOrNull()
            val rarity = args.getOrNull(1)?.lowercase()?.trim().orEmpty()
            if (mf == null || rarity !in RARITIES) {
                Chat.chat("§6[SBO] §ePlease provide MF and griffin rarity. /sbodc 500 <epic|legendary|mythic>")
                return@command
            }

            val items = when (rarity) {
                "epic" -> listOf("Stick" to "stick", "Relic" to "relic")
                "legendary" -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food")
                else -> listOf("Chimera" to "chim", "Stick" to "stick", "Relic" to "relic", "Food" to "food", "Wool" to "wool", "Core" to "core", "Stinger" to "stinger")
            }

            val label = Helper.formatMagicFind(mf)
            listOf(false, true).forEach { lootshare ->
                val chances = Helper.getChance(mf, rarity, lootshare)
                val lsPrefix = if (lootshare) "§7[§bLS§7] " else ""
                items.forEach { (name, key) ->
                    Chat.chat("§6[SBO] $lsPrefix§e$name ${Helper.formatChances(chances[key] ?: 0.0, label)}")
                }
            }
        }
    }
}
