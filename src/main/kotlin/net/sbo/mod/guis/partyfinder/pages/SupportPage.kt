package net.sbo.mod.guis.partyfinder.pages

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import net.sbo.mod.SBOKotlin
import net.sbo.mod.guis.partyfinder.GuiHandler
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import net.sbo.mod.guis.partyfinder.Theme
import java.awt.Color

class SupportPage(private val parent: PartyFinderGUI) : PartyPage {
    override val pageName: String = "Donate"
    override val partyType: String = ""
    override val listDisplayName: String = ""
    override val pageOrder: Int get() = 103

    override fun render() {
        Window.enqueueRenderOperation {
            parent.noParties.hide()
            parent.contentBlock.addChild(ScrollComponent().constrain {
                x = 0.percent()
                y = 0.percent()
                width = 100.percent()
                height = 100.percent()
            }.setColor(Theme.TRANSPARENT)
                .addChild(UIBlock().constrain {
                    width = 100.percent()
                    height = 9.percent()
                }.setColor(Theme.TRANSPARENT)
                    .addChild(UIWrappedText("Donate Page").constrain {
                        x = 2.percent()
                        y = CenterConstraint()
                        width = 100.percent()
                        textScale = parent.getTextScaleOfScaleText(1.5f)
                    }.setColor(Theme.TEXT_PRIMARY))
                )
                .addChild(UIWrappedText(
                    "☕ Help Us Keep the Server Running\n\n" +
                    "Running Skyblock Overhaul's services isn't free, we rely on donations to cover\n" +
                    "the costs of our dedicated server, which handles party finding, statistics\n" +
                    "tracking, and all the features that make SBO what it is.\n\n" +
                    "Every bit of support helps us keep things online and continuously improve\n" +
                    "the mod. Whether you choose Patreon or Ko-fi, your support means the world\n" +
                    "to us and keeps SBO going!\n\n" +
                    "Thank you for being part of our community! 💜"
                ).constrain {
                    x = 2.percent()
                    y = SiblingConstraint()
                    width = 100.percent()
                    textScale = parent.getTextScaleOfScaleText()
                }.setColor(Theme.TEXT_PRIMARY))
                .addChild(createButtonsBlock())
            )
        }
    }

    private fun createButtonsBlock(): UIComponent {
        val buttonsBlock = UIBlock().constrain {
            width = 100.percent()
            height = 15.percent()
            x = CenterConstraint()
            y = SiblingConstraint(5f)
        }.setColor(Theme.TRANSPARENT)

        val patreon = GuiHandler.Button(
            text = "Patreon",
            x = 2.percent(),
            y = CenterConstraint(),
            width = 140.pixels,
            height = 30.pixels,
            color = Color(200, 50, 50),
            textColor = Theme.TEXT_PRIMARY,
            parent = buttonsBlock,
            rounded = true,
        )
            .hoverEffect(Color(200, 50, 50), Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN)
            .setOnClick {
                SBOKotlin.openInBrowser("https://www.patreon.com/Skyblock_Overhaul")
            }

        val kofi = GuiHandler.Button(
            text = "Ko-fi",
            x = SiblingConstraint(15f),
            y = CenterConstraint(),
            width = 140.pixels,
            height = 30.pixels,
            color = Color(30, 150, 180),
            textColor = Theme.TEXT_PRIMARY,
            parent = buttonsBlock,
            rounded = true,
        )
            .hoverEffect(Color(30, 150, 180), Theme.BUTTON_TITLE_DISC_GIT_PAT_HOVER_IN)
            .setOnClick {
                SBOKotlin.openInBrowser("https://ko-fi.com/skyblock_overhaul")
            }

        return buttonsBlock
    }
}
