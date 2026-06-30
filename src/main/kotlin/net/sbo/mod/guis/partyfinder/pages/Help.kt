package net.sbo.mod.guis.partyfinder.pages
//todo: remake this with Vexel https://github.com/meowing-xyz/vexel

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.percent
import net.sbo.mod.guis.partyfinder.PartyFinderGUI
import java.awt.Color

class Help(private val parent: PartyFinderGUI) {
    internal fun render() {
        Window.enqueueRenderOperation {
            parent.noParties.hide()
            parent.contentBlock.addChild(ScrollComponent().constrain {
                x = 0.percent()
                y = 0.percent()
                width = 100.percent()
                height = 100.percent()
            }.setColor(Color(0, 0, 0, 0))
                .addChild(UIBlock().constrain {
                    width = 100.percent()
                    height = 9.percent()
                }.setColor(Color(0, 0, 0, 0))
                    .addChild(UIWrappedText("Help Page!").constrain {
                        x = 2.percent()
                        y = CenterConstraint()
                        width = 100.percent()
                        textScale = parent.getTextScale(1.5f)
                    }.setColor(Color(255, 255, 255, 255)))
                )
                .addChild(UIWrappedText(
                    "・ Not Getting any Join Requests?\n\n" +
                            "   ・ Enable private Messages!\n\n" +
                            "   ・ /settings -> Social Settings.\n\n" +
                            "・ Requirements don't update?\n\n" +
                            "   ・ Wait 10mins and make sure you have all API enabled in SkyBlock settings.\n\n" +
                            "・ Text or Icons too small or too big?\n\n" +
                            "   ・ Open party finder settings\n\n" +
                            "・ Not seeing your party in the list?\n\n" +
                            "   ・ Make sure you have the right filters set.\n\n" +
                            "・ Still having issues?\n\n" +
                            "   ・ Join our discord and ask for help."
                ).constrain {
                    x = 2.percent()
                    y = SiblingConstraint()
                    width = 100.percent()
                    textScale = parent.getTextScale()
                }.setColor(Color(255, 255, 255, 255)))
            )
        }
    }
}
