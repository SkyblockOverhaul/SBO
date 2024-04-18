import { data } from "../utils/variables";
// dowload msg beispiel
const newVersion = "0.1.6" // hier neue version eintragen wenn changelog angezeigt werden soll
const downloadMsgReg = register("step", () => {
    if (!World.isLoaded()) return
    if (data.downloadMsg) {
        downloadMsgReg.unregister()
        return
    }
    // ChatLib.chat(ChatLib.getChatBreak("&b-"))
    // ChatLib.chat(`&aThanks for importing &6 SBO`)
    // ChatLib.chat(`&7> &ayou can open the settings with /sbo`)
    // ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.downloadMsg = true
    // data.changelogVersion = newVersion
    data.save()
    
    downloadMsgReg.unregister()
}).setFps(1)

// changelog beispiel
const changeLogReg = register("step", () => {
    if (!World.isLoaded()) return
    if (data.changelogVersion === newVersion) { 
        changeLogReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&6[SBO] &r&bVersion &e${newVersion}&r`)
    ChatLib.chat(`&aChangelog:`)
    ChatLib.chat(`&7> &aAdded exit waypoint for mineshafts`)
    ChatLib.chat(`&7> &aAdded alias for !transfer (!ptme)`)
    ChatLib.chat(`&7> &aAdded command to move all guis (/sboguis)`)
    ChatLib.chat(`&7> &aSmall fossil solver update`)
    ChatLib.chat(`&7> &aFixed bug with party commands not working`)
    ChatLib.chat(`&7> &aSome other small bug fixes`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.changelogVersion = newVersion
    data.save()
    changeLogReg.unregister()
}).setFps(1)