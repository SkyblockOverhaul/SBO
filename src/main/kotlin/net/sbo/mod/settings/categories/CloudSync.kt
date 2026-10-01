package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.utils.data.cloud.CloudSync

object CloudSync : CategoryKt("Cloud Sync") {
    init {
        separator {
            this.title = "Cloud Save"
            this.description = "Supporter feature: keeps your SBO settings, trackers, achievements and other SBO data online, so you can load them on any PC. Needs your SBO key (/sbokey)."
        }
    }

    var autoSync by boolean(false) {
        this.name = Literal("Auto Sync")
        this.description = Literal("Does Upload and Download for you. When you join, it loads newer data from your other PC. While playing, it saves your changes every 5 minutes, when you leave a server and when you close the game.")
    }

    init {
        separator {
            this.title = "Sync Password (optional)"
            this.description = "Extra protection: only PCs with this password can load your cloud save. Type /sbosyncpassword <password> on every PC, with the same password everywhere. Write it down, it cannot be reset. Remove it with /sboclearsyncpassword."
        }

        button {
            title = "Upload"
            description = "Saves the SBO settings and data from this PC as your cloud save."
            text = "Upload"
            onClick {
                CloudSync.upload()
            }
        }

        button {
            title = "Download"
            description = "Replaces the SBO settings and data on this PC with your cloud save. A backup is made first."
            text = "Download"
            onClick {
                CloudSync.download()
            }
        }

        button {
            title = "Status"
            description = "Shows when you last uploaded and whether this PC is up to date."
            text = "Check"
            onClick {
                CloudSync.status()
            }
        }

        button {
            title = "Delete Cloud Save"
            description = "Deletes your cloud save. The data on this PC stays. Asks for confirmation in chat."
            text = "Delete"
            onClick {
                CloudSync.delete()
            }
        }
    }
}
