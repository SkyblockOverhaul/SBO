package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category
import net.sbo.mod.partyfinder.gui.PartyFinderGui

object PartyFinder : Category("Party Finder") {
    var autoInvite by boolean(true) {
        this.name = Literal("Auto Invite")
        this.description = Literal("Auto invites players that send you a join request and meet the party requirements.")
    }

    var autoRequeue by boolean(true) {
        this.name = Literal("Auto Requeue")
        this.description = Literal("Automatically requeues the party after a member leaves.")
    }

    init {
        button {
            title = "Open Party Finder"
            text = "Open Party Finder"
            description = "Opens the Party Finder, alternatively use /sbopf. NOTE: You need to be in Skyblock for it to open!"
            onClick {
                PartyFinderGui.open()
            }
        }
    }
}
