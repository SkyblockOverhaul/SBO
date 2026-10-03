package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.SBOKotlin
import net.sbo.mod.cloud.gui.CloudSyncGui

object CloudSync : CategoryKt("Cloud Sync") {
    init {
        separator {
            this.title = "Cloud Save"
            this.description = "Supporter feature: keeps your SBO settings, trackers, achievements and other SBO data online, so you can load them on any PC. Needs your SBO key (/sbokey)."
        }

        button {
            title = "Cloud Sync"
            text = "Open"
            description = "Open the Cloud Sync window (/sbocloud): upload, download, compare, Auto Sync and your sign key."
            onClick {
                SBOKotlin.mc.schedule { CloudSyncGui.open() }
            }
        }
    }
}
