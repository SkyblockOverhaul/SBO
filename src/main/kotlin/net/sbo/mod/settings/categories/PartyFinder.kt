package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.guis.Guis

object PartyFinder : CategoryKt("PartyFinder") {
    var autoInvite by boolean(true) {
        this.name = Literal("Auto Invite")
        this.description = Literal("Auto invites players that send you a join request and meet the party requirements")
    }

    var autoRequeue by boolean(true) {
        this.name = Literal("Auto Requeue")
        this.description = Literal("Automatically requeues the party after a member leaves")
    }

    var scaleText by float(0f) {
        this.name = Literal("Text Scale")
        this.description = Literal("Change the size of the text")
        this.range = -2f..2f
        this.slider = true
    }

    init {
        button {
            title = "Open Party Finder"
            text = "Open Party Finder"
            description = "Opens the Party Finder, alternatively use /sbopf. NOTE: You need to be in Skyblock for it to open!"
            onClick {
                Guis.openSboPf(true)
            }
        }
    }
}
