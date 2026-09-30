package net.sbo.mod.settings.categories

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import net.sbo.mod.utils.data.cloud.CloudSync

object CloudSync : CategoryKt("Cloud Sync") {
    init {
        separator {
            this.title = "Cloud Save"
            this.description = "Supporter feature: save your SBO config, trackers, achievements and other SBO data on the SBO server and load them on any PC. Needs your sbo key (/sbokey)."
        }
    }

    var autoSync by boolean(false) {
        this.name = Literal("Auto Sync")
        this.description = Literal("Loads a newer cloud save from another PC when you join a world, and uploads your changes every 5 minutes while playing, when you leave a server and when you close the game.")
    }

    init {
        separator {
            this.title = "Sync Password (optional)"
            this.description = "Set it with /sbosyncpassword <password> on every PC. Uploads are then signed and a save changed on the server is refused. SBO cannot recover it. Remove it with /sboclearsyncpassword."
        }

        button {
            title = "Upload"
            description = "Saves your current SBO config and data in the cloud."
            text = "Upload"
            onClick {
                CloudSync.upload()
            }
        }

        button {
            title = "Download"
            description = "Replaces your SBO config and data with the cloud save. Your current state is backed up first."
            text = "Download"
            onClick {
                CloudSync.download()
            }
        }

        button {
            title = "Status"
            description = "Shows what is stored in the cloud."
            text = "Check"
            onClick {
                CloudSync.status()
            }
        }

        button {
            title = "Delete Cloud Save"
            description = "Deletes your save from the cloud. Asks for confirmation in chat."
            text = "Delete"
            onClick {
                CloudSync.delete()
            }
        }
    }
}
