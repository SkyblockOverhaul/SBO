package net.sbo.mod.guis
//todo: remake this with Vexel https://github.com/meowing-xyz/vexel

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.universal.UKeyboard
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.overlays.DianaLoot
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.data.DianaTracker
import net.sbo.mod.utils.data.DianaTrackerTotalData
import net.sbo.mod.utils.data.SboDataObject
import net.sbo.mod.utils.data.SboDataObject.dianaTrackerTotal
import net.sbo.mod.utils.data.SboDataObject.pastDianaEventsData
import java.awt.Color

class PastEventsGui : WindowScreen(ElementaVersion.V10) {
    private var pastEvents = pastDianaEventsData
    private val events = pastEvents.events.reversed()
    private var totalEvents = dianaTrackerTotal
    private var isOverlayHidden = true
    private lateinit var eventContainer: UIBlock
    private lateinit var scroll: ScrollComponent
    private lateinit var overviewButton: UIRoundedRectangle
    private lateinit var overviewButtonText: UIText
    private var overlayOutline: UIRoundedRectangle? = null
    private var overlayContent: UIRoundedRectangle? = null

    init {
        initMainUI()
        window.onKeyType { typedChar, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.schedule {
                    displayScreen(null)
                }
            }
        }
    }

    private fun initMainUI() {
        UIBlock().constrain { width = 100.percent; height = 100.percent }
            .setColor(Color(0,0,0,200)) childOf window

        val background = UIBlock().constrain {
            x = CenterConstraint()
            y = 10.percent
            width = 60.percent
            height = 70.percent
        }.setColor(Color(20,20,20,0)) childOf window

        val overviewOutline = UIRoundedRectangle(5f).constrain {
            x = 10.pixels; y = 10.pixels; width = 150.pixels; height = 25.pixels
        } childOf background
        overviewOutline.setColor(Color.WHITE)

        overviewButton = UIRoundedRectangle(5f).constrain {
            x = CenterConstraint(); y = CenterConstraint(); width = 148.pixels; height = 23.pixels
        } childOf overviewOutline
        overviewButton.setColor(Color(10,10,30))

        overviewButtonText = UIText("Total Overview").constrain {
            x = CenterConstraint(); y = CenterConstraint(); textScale = 1.0.pixels
        } childOf overviewButton
        overviewButtonText.setColor(Color.CYAN)

        overviewButton.onMouseEnter {  overviewButton.setColor(Color(20,20,50)) }
        overviewButton.onMouseLeave {  overviewButton.setColor(Color(10,10,30)) }
        overviewButton.onMouseClick { toggleOverlay(totalEvents, "Diana Total Overview", true) }

        scroll = ScrollComponent().constrain {
            x = 0.pixels; y = 40.pixels
            width = 100.percent
            height = SubtractiveConstraint(100.percent, 40.pixels)
        } childOf background
        scroll.setColor(Color(0,0,0,0))

        eventContainer = UIBlock().constrain {
            x = 0.pixels
            y = 0.pixels
            width = FillConstraint()
            height = (events.size * 80).pixels
        } childOf scroll
        eventContainer.setColor(Color(0,0,0,0))

        renderEvents()
    }

    private fun toggleOverlay(data: DianaTracker, title: String, showTotalLine: Boolean = false) {
        if (overlayOutline == null) {
            overlayOutline = UIRoundedRectangle(12f).constrain {
                width = 324.pixels; height = 364.pixels; x = CenterConstraint(); y = 20.percent
            } childOf window
            overlayOutline!!.setColor(Color.WHITE)

            overlayContent = UIRoundedRectangle(10f).constrain {
                width = 320.pixels; height = 360.pixels; x = CenterConstraint(); y = CenterConstraint()
            } childOf overlayOutline!!
            overlayContent!!.setColor(Color(25,25,40,230))
        }

        if (isOverlayHidden) {
            overlayContent!!.clearChildren()
            UIText(title).constrain { x = CenterConstraint(); y = 10.pixels; textScale = 1.8.pixels }
                .setColor(Color.CYAN) childOf overlayContent!!

            val blocksContainer = UIBlock().constrain {
                x = CenterConstraint(); y = 50.pixels; width = 96.percent; height = 240.pixels
            }.setColor(Color(0,0,0,0)) childOf overlayContent!!

            val lineHeight = 14
            fun createBlock(dataLines: List<String>, xStart: Int) {
                val block = UIBlock().constrain {
                    x = xStart.percent; y = 0.pixels; width = 48.percent; height = 100.percent
                }.setColor(Color(0,0,0,0)) childOf blocksContainer

                var yPos = 0
                dataLines.forEach { text ->
                    UIText(text).constrain { x = 2.percent; y = yPos.pixels; textScale = 1.1.pixels } childOf block
                    yPos += lineHeight
                }
            }

            val leftPercents = mutableListOf(
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"SHIMMERING_WOOL", "KING_MINOS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"SHIMMERING_WOOL_LS", "KING_MINOS_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MANTI_CORE", "MANTICORE")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MANTI_CORE_LS", "MANTICORE_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"CHIMERA", "MINOS_INQUISITOR")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"CHIMERA_LS", "MINOS_INQUISITOR_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOS_RELIC", "MINOS_CHAMPION")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"DAEDALUS_STICK", "MINOTAUR")}%§7)"
            )

            val leftData = mutableListOf(
                "§cDyes: ${Helper.formatNumber(data.items.MYTHOLOGICAL_DYE,true)}",
                "§cWools: ${Helper.formatNumber(data.items.SHIMMERING_WOOL,true)} ${leftPercents[0]}",
                "§cWools (LS): ${Helper.formatNumber(data.items.SHIMMERING_WOOL_LS,true)} ${leftPercents[1]}",
                "§cManti-cores: ${Helper.formatNumber(data.items.MANTI_CORE,true)} ${leftPercents[2]}",
                "§cManti-cores (LS): ${Helper.formatNumber(data.items.MANTI_CORE_LS,true)} ${leftPercents[3]}",
                "§dChimeras: ${Helper.formatNumber(data.items.CHIMERA,true)} ${leftPercents[4]}",
                "§dChimeras (LS): ${Helper.formatNumber(data.items.CHIMERA_LS,true)} ${leftPercents[5]}",
                "§5Relics: ${Helper.formatNumber(data.items.MINOS_RELIC,true)} ${leftPercents[6]}",
                "§6Sticks: ${Helper.formatNumber(data.items.DAEDALUS_STICK,true)} ${leftPercents[7]}",
                "§6Treasure: ${Helper.formatNumber(data.items.COINS - (data.items.FISH_COINS + data.items.SCAVENGER_COINS))}",
                "§6Fish Coins: ${ Helper.formatNumber(data.items.FISH_COINS)}",
                "§6Scavenger: ${ Helper.formatNumber(data.items.SCAVENGER_COINS)}",
                "§6Feathers: ${Helper.formatNumber(data.items.GRIFFIN_FEATHER)}",
                "§6Crowns: ${Helper.formatNumber(data.items.CROWN_OF_GREED,true)}",
                "§6Souvenirs: ${Helper.formatNumber(data.items.WASHED_UP_SOUVENIR,true)}",
                "§2Shelmets: ${Helper.formatNumber(data.items.DWARF_TURTLE_SHELMET,true)}",
                "§2Remedies: ${Helper.formatNumber(data.items.ANTIQUE_REMEDIES,true)}",
                "§2Plushies: ${Helper.formatNumber(data.items.CROCHET_TIGER_PLUSHIE,true)}",
                "§7Claws: ${Helper.formatNumber(data.items.ANCIENT_CLAW)}",
                "§7Ench. Claws: ${Helper.formatNumber(data.items.ENCHANTED_ANCIENT_CLAW,true)}",
                "§7Ench. Gold: ${Helper.formatNumber(data.items.ENCHANTED_GOLD)}",
                "§eBurrows: ${Helper.formatNumber(data.items.TOTAL_BURROWS)}",
                "§ePlaytime: ${Helper.formatTime(data.items.TIME)}"
            )
            if (data is DianaTrackerTotalData && showTotalLine) {
                leftData.add("§eEvents: ${pastDianaEventsData.events.size}")
            }
            leftData.add("§6Total Profit: ${Helper.formatNumber(DianaLoot.totalProfit(data))}")
            createBlock(leftData, 0)

            val rightPercents = mutableListOf(
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"KING_MINOS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"KING_MINOS_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MANTICORE")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MANTICORE_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOS_INQUISITOR")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOS_INQUISITOR_LS")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOS_CHAMPION")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOTAUR")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"GAIA_CONSTRUCT")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"SIAMESE_LYNXES")}%§7)",
                "§7(§b${Helper.calcPercentOne(data.items, data.mobs,"MINOS_HUNTER")}%§7)"
            )

            val rightData = mutableListOf(
                "§cKings: ${Helper.formatNumber(data.mobs.KING_MINOS,true)} ${rightPercents[0]}",
                "§cKings (LS): ${Helper.formatNumber(data.mobs.KING_MINOS_LS,true)} ${rightPercents[1]}",
                "§cMantis: ${Helper.formatNumber(data.mobs.MANTICORE,true)} ${rightPercents[2]}",
                "§cMantis (LS): ${Helper.formatNumber(data.mobs.MANTICORE_LS,true)} ${rightPercents[3]}",
                "§dInquisitors: ${Helper.formatNumber(data.mobs.MINOS_INQUISITOR,true)} ${rightPercents[4]}",
                "§dInquisitors (LS): ${Helper.formatNumber(data.mobs.MINOS_INQUISITOR_LS,true)} ${rightPercents[5]}",
                "§5Champions: ${Helper.formatNumber(data.mobs.MINOS_CHAMPION,true)} ${rightPercents[6]}",
                "§6Minotaurs: ${Helper.formatNumber(data.mobs.MINOTAUR,true)} ${rightPercents[7]}",
                "§2Gaias: ${Helper.formatNumber(data.mobs.GAIA_CONSTRUCT,true)} ${rightPercents[8]}",
                "§2Siamese: ${Helper.formatNumber(data.mobs.SIAMESE_LYNXES,true)} ${rightPercents[9]}",
                "§2Hunters: ${Helper.formatNumber(data.mobs.MINOS_HUNTER,true)} ${rightPercents[10]}",
                "§eTotal Mobs: ${Helper.formatNumber(data.mobs.TOTAL_MOBS)}"
            )
            createBlock(rightData, 52)

            scroll.hide()
            overlayOutline!!.unhide(true)
            overviewButtonText.setText("Hide Overview")
            isOverlayHidden = false
        } else {
            overlayOutline?.hide()
            scroll.unhide(true)
            overviewButtonText.setText("Total Overview")
            isOverlayHidden = true
        }
    }

    private fun renderEvents() {
        eventContainer.clearChildren()
        val events = pastDianaEventsData.events.reversed()

        if (events.isEmpty()) {
            UIText("No Events Recorded").constrain {
                x = CenterConstraint(); y = CenterConstraint(); textScale = 1.5.pixels
            }.setColor(Color.WHITE) childOf eventContainer
            return
        }

        events.forEachIndexed { index, event ->
            val outline = UIRoundedRectangle(6f).constrain {
                x = 0.pixels; y = (index*80).pixels; width = 100.percent; height = 60.pixels
            } childOf eventContainer
            outline.setColor(Color.WHITE)

            val eventBlock = UIRoundedRectangle(6f).constrain {
                x = 1.pixels; y = 1.pixels; width = SubtractiveConstraint(FillConstraint(),2.pixels); height = SubtractiveConstraint(FillConstraint(),2.pixels)
            } childOf outline
            eventBlock.setColor(Color(10,10,10,255))

            val leftText = UIBlock().constrain {
                x = 10.pixels; y = 10.pixels; width = SubtractiveConstraint(ChildBasedSizeConstraint(), 40.pixels); height = ChildBasedSizeConstraint()
            }.setColor(Color(0,0,0,0)) childOf eventBlock

            val rightText = UIBlock().constrain {
                x = (leftText.getRight()).pixels; y = 10.pixels; width = ChildBasedSizeConstraint(); height = ChildBasedSizeConstraint()
            }.setColor(Color(0,0,0,0)) childOf eventBlock

            UIText("§aYear: ${event.year}").constrain { x=0.pixels;y=0.pixels;textScale=1.0.pixels }.setColor(Color.WHITE) childOf leftText
            UIText("§ePlaytime: ${Helper.formatTime(event.items.TIME)}").constrain { x=0.pixels;y=SiblingConstraint(3f);textScale=0.9.pixels }.setColor(Color.YELLOW) childOf leftText
            UIText("§6Total Profit: ${Helper.formatNumber(DianaLoot.totalProfit(event))}").constrain { x=0.pixels;y=SiblingConstraint(3f);textScale=0.9.pixels }.setColor(Color.ORANGE) childOf leftText

            UIText("§dChimeras: ${(event.items.CHIMERA + event.items.CHIMERA_LS)}").constrain { x=0.pixels;y=0.pixels;textScale=0.9.pixels }.setColor(Color.CYAN) childOf rightText
            UIText("§cWools: ${(event.items.SHIMMERING_WOOL + event.items.SHIMMERING_WOOL_LS)}").constrain { x=0.pixels;y=SiblingConstraint(3f);textScale=0.9.pixels }.setColor(Color.RED) childOf rightText
            UIText("§7Burrows: ${Helper.formatNumber(event.items.TOTAL_BURROWS)}").constrain { x=0.pixels;y=SiblingConstraint(3f);textScale=0.9.pixels }.setColor(Color.LIGHT_GRAY) childOf rightText
            UIText("§7Mobs: ${Helper.formatNumber(event.mobs.TOTAL_MOBS)}").constrain { x=0.pixels;y=SiblingConstraint(3f);textScale=0.9.pixels }.setColor(Color.LIGHT_GRAY) childOf rightText

            val detailsOutline = UIRoundedRectangle(5f).constrain {
                x = SubtractiveConstraint(100.percent, 230.pixels)
                y = CenterConstraint()
                width = 98.pixels
                height = 30.pixels
            } childOf eventBlock
            detailsOutline.setColor(Color.WHITE)

            val detailsButton = UIRoundedRectangle(5f).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = 96.pixels
                height = 28.pixels
            } childOf detailsOutline
            detailsButton.setColor(Color(0,0,40))

            UIText("Details").constrain { x=CenterConstraint(); y=CenterConstraint(); textScale=1.2.pixels }.setColor(Color.CYAN) childOf detailsButton

            detailsButton.onMouseEnter { detailsButton.setColor(Color(0,0,60)) }
            detailsButton.onMouseLeave { detailsButton.setColor(Color(0,0,40)) }
            detailsButton.onMouseClick {
                toggleOverlay(event, "Event Details: Year ${event.year}")
            }

            val deleteOutline = UIRoundedRectangle(5f).constrain {
                x = SubtractiveConstraint(100.percent, 120.pixels)
                y = CenterConstraint()
                width = 98.pixels
                height = 30.pixels
            } childOf eventBlock
            deleteOutline.setColor(Color.WHITE)

            val deleteButton = UIRoundedRectangle(5f).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = 96.pixels
                height = 28.pixels
            } childOf deleteOutline
            deleteButton.setColor(Color(40,0,0))

            UIText("Delete").constrain { x=CenterConstraint(); y=CenterConstraint(); textScale=1.2.pixels }.setColor(Color.RED) childOf deleteButton

            deleteButton.onMouseEnter { deleteButton.setColor(Color(60,0,0)) }
            deleteButton.onMouseLeave { deleteButton.setColor(Color(40,0,0)) }
            deleteButton.onMouseClick {
                val confirmOverlayOutline = UIRoundedRectangle(8f).constrain {
                    width = 204.pixels
                    height = 104.pixels
                    x = CenterConstraint()
                    y = CenterConstraint()
                } childOf window
                confirmOverlayOutline.setColor(Color.WHITE)

                val confirmOverlay = UIRoundedRectangle(8f).constrain {
                    width = 200.pixels
                    height = 100.pixels
                    x = CenterConstraint()
                    y = CenterConstraint()
                } childOf confirmOverlayOutline
                confirmOverlay.setColor(Color(20, 20, 20, 255))

                UIText("Delete this event?").constrain {
                    x = CenterConstraint()
                    y = 10.pixels
                    textScale = 1.2.pixels
                }.setColor(Color.WHITE) childOf confirmOverlay

                val confirmButton = UIRoundedRectangle(5f).constrain {
                    x = 20.pixels
                    y = 50.pixels
                    width = 70.pixels
                    height = 30.pixels
                } childOf confirmOverlay
                confirmButton.setColor(Color(0,100,0))

                UIText("Yes").constrain { x = CenterConstraint(); y = CenterConstraint(); textScale = 1.0.pixels }
                    .setColor(Color.WHITE) childOf confirmButton

                val cancelButton = UIRoundedRectangle(5f).constrain {
                    x = 110.pixels
                    y = 50.pixels
                    width = 70.pixels
                    height = 30.pixels
                } childOf confirmOverlay
                cancelButton.setColor(Color(100,0,0))

                UIText("No").constrain { x = CenterConstraint(); y = CenterConstraint(); textScale = 1.0.pixels }
                    .setColor(Color.WHITE) childOf cancelButton

                confirmButton.onMouseEnter { confirmButton.setColor(Color(0,150,0)) }
                confirmButton.onMouseLeave { confirmButton.setColor(Color(0,100,0)) }
                confirmButton.onMouseClick {
                    pastDianaEventsData.events = pastDianaEventsData.events.filterNot { it.year == event.year }
                    renderEvents()
                    SboDataObject.save(SboDataObject.dataDir, pastDianaEventsData, "pastDianaEvents.json")
                    confirmOverlayOutline.hide()
                }

                cancelButton.onMouseEnter { cancelButton.setColor(Color(150,0,0)) }
                cancelButton.onMouseLeave { cancelButton.setColor(Color(100,0,0)) }
                cancelButton.onMouseClick {
                    confirmOverlayOutline.hide()
                }
            }
        }
    }
}
