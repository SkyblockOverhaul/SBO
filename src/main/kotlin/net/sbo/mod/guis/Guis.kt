package net.sbo.mod.guis

import gg.essential.universal.UScreen
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.impl.partyfinder.PartyFinderOpenEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.http.Http
import java.util.concurrent.TimeUnit

object Guis {
    private var partyFinderGui: PartyFinderGUI? = null
    private var pastEventsGui: PastEventsGui? = null
    var achievementsGui: AchievementsGUI? = null
//    private var vexelGui: VexelTest? = null
    private var updating = false
    private var lastUpdate = 0L
    private val UPDATE_INTERVAL = TimeUnit.MILLISECONDS.toNanos(300_000L) // 5 minutes in ns

    fun openSboPf(calledFromGUI: Boolean = false) {
        if (!World.isInSkyblock()) {
            if (!calledFromGUI) {
                Chat.chat("§6[SBO] §cYou can only use this command in Skyblock.")
                return
            }
            SBOKotlin.toast(
                    Component.literal("SBO").setStyle(
                        Style.EMPTY.withColor(ChatFormatting.GOLD)
                    ),
                    Component.literal("Join skyblock before opening Party Finder!").setStyle(
                        Style.EMPTY.withColor(ChatFormatting.RED)
                    )
            )
            return
        }
        mc.schedule {
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
            mc.schedule {
                if (achievementsGui == null) {
                    achievementsGui = AchievementsGUI()
                }
                UScreen.displayScreen(achievementsGui!!)
            }
        }

        Register.command("sboapastdianaevents", "sbopevents", "sbopastevents", "sbopde") {
            mc.schedule {
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
            val now = System.nanoTime()
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
