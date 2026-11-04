package net.sbo.mod.settings.categories

import net.sbo.mod.config.Category

object PartyCommands : Category("Party Commands") {
    var warpCommand by boolean(false) {
        this.name = literal("Warp Party")
        this.description = literal("Enables !w, !warp command.")
    }

    var allinviteCommand by boolean(false) {
        this.name = literal("Allinvite")
        this.description = literal("Enables !allinv, !allinvite command.")
    }

    var transferCommand by boolean(false) {
        this.name = literal("Party Transfer")
        this.description = literal("Enables !transfer [Player] command. (if no player is defined it transfers the party to the command writer)")
    }

    var moteCommand by boolean(false) {
        this.name = literal("Promote/Demote")
        this.description = literal("Enables !promote/demote [Player] command. (if no player is defined it pro/demotes the command writer)")
    }

    var carrotCommand by boolean(false) {
        this.name = literal("Ask Carrot")
        this.description = literal("Enables the !carrot command.")
    }

    var timeCommand by boolean(false) {
        this.name = literal("Time Check")
        this.description = literal("Sends your time in party chat. (!time)")
    }

    var tpsCommand by boolean(true) {
        this.name = literal("Check Tps")
        this.description = literal("Sends the server tps in party chat. (!tps)")
    }

    var dianaPartyCommands by boolean(true) {
        this.name = literal("Diana Party Commands")
        this.description = literal("Enable Diana party commands. (!chim, !inq, !relic, !stick, !since, !burrow, !mob) (note: you need to have Diana tracker enabled)")
    }

    var blockUser by boolean(true) {
        this.name = "Block the user"
        this.description = "Enables the !block [Player] and !unblock [Player] command."
    }
}
