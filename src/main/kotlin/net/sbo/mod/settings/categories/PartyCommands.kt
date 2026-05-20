package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object PartyCommands : CategoryKt("Party Commands") {
    var warpCommand by boolean(false) {
        this.name = Literal("Warp Party")
        this.description = Literal("!w, !warp")
    }

    var allinviteCommand by boolean(false) {
        this.name = Literal("Allinvite")
        this.description = Literal("!allinv, !allinvite")
    }

    var transferCommand by boolean(false) {
        this.name = Literal("Party Transfer")
        this.description = Literal("!transfer [Player] (if no player is defined it transfers the party to the command writer)")
    }

    var moteCommand by boolean(false) {
        this.name = Literal("Promote/Demote")
        this.description = Literal("!promote/demote [Player] (if no player is defined it pro/demotes the command writer)")
    }

    var carrotCommand by boolean(false) {
        this.name = Literal("Ask Carrot")
        this.description = Literal("Enable !carrot Command")
    }

    var timeCommand by boolean(false) {
        this.name = Literal("Time Check")
        this.description = Literal("Sends your time in party chat (!time)")
    }

    var tpsCommand by boolean(true) {
        this.name = Literal("Check Tps")
        this.description = Literal("Sends the server tps in party chat (!tps)")
    }

    var dianaPartyCommands by boolean(true) {
        this.name = Literal("Diana Party Commands")
        this.description = Literal("Enable Diana party commands (!chim, !inq, !relic, !stick, !since, !burrow, !mob) (note: you need to have Diana tracker enabled)")
    }
}
