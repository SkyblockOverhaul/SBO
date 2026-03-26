package net.sbo.mod.guis

import gg.essential.universal.UScreen
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderOpenEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.Formatting

object Guis {
    private var partyFinderGui: PartyFinderGUI? = null
    private var pastEventsGui: PastEventsGui? = null
    internal var achievementsGui: AchievementsGUI? = null
//    private var vexelGui: VexelTest? = null
    private var updating = false
    private var lastUpdate = 0L
    private const val UPDATE_INTERVAL = 300_000L // 5 minutes in ms

    fun openSboPf(calledFromGUI: Boolean = false) {
        if (!World.isInSkyblock()) {
            if (!calledFromGUI) {
                Chat.chat("§6[SBO] §cYou can only use this command in Skyblock.")
                return
            }
            mc.getToastManager().add(SystemToast.create(mc, SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal("SBO").setStyle(Style.EMPTY.withColor(Formatting.GOLD)), Text.literal("Join skyblock before opening Party Finder!").setStyle(Style.EMPTY.withColor(Formatting.RED))))
            return
        }
        mc.send {
            if (partyFinderGui == null) {
                partyFinderGui = PartyFinderGUI()
            }
            UScreen.displayScreen(partyFinderGui!!)
            SBOEvent.emit(PartyFinderOpenEvent())
        }
    }

    fun register() {
        Register.command("sbopf") {
            openSboPf()
        }

        Register.command("sboachievements") {
            mc.send {
                if (achievementsGui == null) {
                    achievementsGui = AchievementsGUI()
                }
                UScreen.displayScreen(achievementsGui!!)
            }
        }

        Register.command("sboapastdianaevents", "sbopevents", "sbopastevents", "sbopde") {
            mc.send {
                if (pastEventsGui == null) {
                    pastEventsGui = PastEventsGui()
                }
                UScreen.displayScreen(pastEventsGui!!)
            }
        }

//        Register.command("vexeltest") {
//            mc.send {
//                if (vexelGui == null) {
//                    vexelGui = VexelTest()
//                }
//                mc.setScreen(vexelGui!!)
//            }
//        }

        Register.onTick(20) {
            val now = System.currentTimeMillis()
            if (now - lastUpdate > UPDATE_INTERVAL && !updating && World.isInSkyblock()) {
                lastUpdate = now
                updating = true
                countActivePlayers()
            }
        }
    }

    private fun countActivePlayers() {
        Http.sendGetRequest("https://api.skyblockoverhaul.com/countActiveUsers")
            .result { response ->
                if (!response.isSuccessful) {
                    SBOKotlin.logger.error("Failed to count active players: ${response.code} ${response.message}")
                }
                updating = false
            }
            .error { exception ->
                SBOKotlin.logger.error("Error while counting active players", exception)
                updating = false
            }
    }
}