/// <reference types="../CTAutocomplete" />
import "./features/Diana/DianaBurrows";
import "./features/Kuudra";
import "./features/Diana/DianaMobDetect";
import "./features/general/CopyMessage";
import "./features/general/PartyCommands";
import "./features/general/Waypoints";
import "./features/general/fossilSolver";
import "./features/general/messageHider";
import "./features/general/pickuplog";
import "./features/guis/BobberCounter";
import "./features/guis/LegionCounter";
import "./features/slayer/BlazeSlayer";
import "./utils/overlays";
import "./features/Diana/PartyFinder";
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import "./features/general/CrownTracker";
import { data, registerWhen } from "./utils/variables";
import { isDataLoaded } from "./utils/checkData";
import Settings from "./settings";



register("command", (args1, ...args) => {
    if (args1 == undefined) {
        Settings.openGUI()
    } else {
        switch (args1.toLowerCase()) { 
            case "help":
                ChatLib.chat("&6[SBO] &eCommands:")
                ChatLib.chat("&7> &a/sbo &7- &eOpen the settings")
                ChatLib.chat("&7> &a/sbo help &7- &eShow this message")
                ChatLib.chat("&7> &a/sboguis &7- &eOpen the GUIs and move them around (alias /sbomoveguis)")
                ChatLib.chat("&7> &a/sboclearburrows &7- &eClear all burrow waypoints (alias /sbocb)")
                ChatLib.chat("&7> &a/sbocheck <player> &7- &eCheck a player (alias /sboc <player>)")
                ChatLib.chat("&7> &a/sbocheckp &7- &eCheck your party (alias /sbocp)")
                ChatLib.chat("&7> &a/sboimporttracker <profilename> &7- &eImport skyhanni/skytils tracker")
                ChatLib.chat("&7> &a/sbodc &7- &eCommand for diana dropchances")
                ChatLib.chat("&7> &a/sbopartyblacklist &7- &eCommand for party commands blacklisting")
                break;
            default:
                ChatLib.chat("&6[SBO] &eUnknown command. Use /sbo help for a list of commands")
                break;
        }
    }
}).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

// dowload msg beispiel
const newVersion = "0.3.3" // hier neue version eintragen wenn changelog angezeigt werden soll
const downloadMsgReg = register("step", () => {
    if (!World.isLoaded()) return
    if (!isDataLoaded()) return
    if (data.downloadMsg) {
        downloadMsgReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&aThanks for importing &6SBO`)
    ChatLib.chat(`&7> &ayou can open the settings with /sbo`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.downloadMsg = true
    data.changelogVersion = newVersion
    data.save()
    
    downloadMsgReg.unregister()
}).setFps(1)

// changelog beispiel
const changeLogReg = register("step", () => {
    if (!World.isLoaded()) return
    if (!isDataLoaded()) return
    if (!data.downloadMsg) return
    if (data.changelogVersion === newVersion) { 
        changeLogReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&6[SBO] &r&bVersion &e${newVersion}&r`)
    ChatLib.chat(`&aChangelog:`)
    ChatLib.chat(`&7> &a--QOL-- `)
    ChatLib.chat(`&7> &aAdded Crown session stats and ghost mode`)
    ChatLib.chat(`&7> &aAdded extra stats button when checking a player with /sbocheck, /sboc`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.changelogVersion = newVersion
    data.save()
    changeLogReg.unregister()
}).setFps(1)

registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    // printDev(`Sound: ${name} | Volume: ${volume} | Pitch: ${pitch} | Category: ${categoryName}`)
    if (name == "mob.ghast.scream" || name == "mob.ghast.charge") {
        ChatLib.chat("sound for rag axe " + name)
        Client.showTitle("RAG AXE", "", 0, 90, 20);
    }
}), () => Settings.testFeatures);

// dojo sounds:
// [DEV]: Sound: mob.cat.hiss | Volume: 2 | Pitch: 1.4920635223388672 | Category: ANIMALS
// [DEV]: Sound: mob.zombie.woodbreak | Volume: 1.5 | Pitch: 1 | Category: MOBS

// register("chat", (message, event) => {
//     message = message.removeFormatting();
//     if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
//         cancel(event);
//     }
//     if (message.includes("Refelctor")) {
//         Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
//     }
//     if (message.includes("Blue Goblin Egg")) {
//         Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
//     }
// }).setCriteria("&r&aYou received ${message}");




