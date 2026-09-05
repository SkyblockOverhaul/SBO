package net.sbo.mod.guis.partyfinder.pages

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import net.sbo.mod.guis.partyfinder.Theme

class Home(private val parent: PartyFinderGUI) {
    internal fun render() {
        Window.enqueueRenderOperation { parent.noParties.hide()
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
                    .addChild(UIWrappedText("Welcome to the SBO Party Finder!").constrain {
                        x = 2.percent()
                        y = CenterConstraint()
                        width = 100.percent()
                        textScale = parent.getTextScaleOfScaleText(1.5f)
                    }.setColor(Theme.TEXT_PRIMARY))
                )
                .addChild(UIWrappedText(
                    "・ Find parties with custom requirements that Hypixel doesn't offer.\n\n" +
                            "・ Create your own party or join others.\n\n" +
                            "・ Set custom requirements and wait for players to join.\n\n" +
                            "・ Made and maintained by the Skyblock Overhaul team.\n\n" +
                            "・ We rely on a server and appreciate any support to keep it running.")
                    .constrain {
                        x = 2.percent()
                        y = SiblingConstraint()
                        width = 100.percent()
                        textScale = parent.getTextScaleOfScaleText()
                    }.setColor(Theme.TEXT_PRIMARY)
                )
            )
        }
    }
}
