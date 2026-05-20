package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt

object QOL : CategoryKt("QOL") {
    var pickuplogOverlay by boolean(false) {
        this.name = Literal("Pickup Log Overlay")
        this.description = Literal("Displays a pickup log in an overlay like sba. /sboguis to move the overlay")
    }

    var phoenixAnnouncer by boolean(true) {
        this.name = Literal("Phoenix Announcer")
        this.description = Literal("Announces on screen when you drop a phoenix pet")
    }

    var dianaMessageHider by boolean(false) {
        this.name = Literal("Diana Message Hider")
        this.description = Literal("Hides all spammy Diana messages")
    }

    var hideAutoPetMSG by boolean(false) {
        this.name = Literal("Hide AutoPet Messages")
        this.description = Literal("Hides all autopet messages")
    }

    var hideImplosionMSG by boolean(false) {
        this.name = Literal("Hide Implosion Messages")
        this.description = Literal("Hides all implosion messages")
    }

    var hideSacksMSG by boolean(false) {
        this.name = Literal("Hide Sack Messages")
        this.description = Literal("Hides all sack messages")
    }
}
