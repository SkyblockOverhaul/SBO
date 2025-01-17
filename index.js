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
import "./features/Diana/DianaAchievements";
import "./features/guis/Achievements";
import "./features/guis/PastDianaEvents";
import "./features/guis/PartyFinderGUI";
import "./features/general/TestFeatures";
import { data, registerWhen } from "./utils/variables";
import { isDataLoaded } from "./utils/checkData";
import settings from "./settings";

const commands = [
    {cmd: "sbo", description: "Open the settings"},
    {cmd: "sbo help", description: "Show this message"},
    {cmd: "sboguis", description: "Open the GUIs and move them around (or: /sbomoveguis)"},
    {cmd: "sboclearburrows", description: "Clear all burrow waypoints (or: /sbocb)"},
    {cmd: "sbocheck <player>", description: "Check a player (or: /sboc <player>)"},
    {cmd: "sbocheckp", description: "Check your party (alias /sbocp)"},
    {cmd: "sboimporttracker <profilename>", description: "Import skyhanni tracker"},
    {cmd: "sboimporttrackerundo", description: "Undo the tracker import"},
    {cmd: "sbodc", description: "Diana dropchances"},
    {cmd: "sbopartyblacklist", description: "Party commands blacklisting"},
    {cmd: "sbobacktrackachivements", description: "Backtrack achievements"},
    {cmd: "sboachievements", description: "Opens the achievements GUI"},
    {cmd: "sbolockachievements", description: "Locks all Achievements (needs confirmation)"},
    {cmd: "sbopde", description: "Opens the Past Diana Events GUI"},
    {cmd: "sboactiveuser", description: "Shows the active user of the mod"},
    {cmd: "sbopf", description: "Opens the PartyFinder GUI"},
    {cmd: "sbopartycommands", description: "Displays all diana partycommands"},
    {cmd: "sboresetavgmftracker", description: "Resets the avg mf tracker"},
    {cmd: "sboresetstatstracker", description: "Resets the stats tracker"},
];

register("command", (args1, ...args) => {
    if (args1 == undefined) {
        settings.openGUI()
    } else {
        switch (args1.toLowerCase()) { 
            case "help":
                ChatLib.chat("&6[SBO] &eCommands:")
                commands.forEach(({ cmd, description }) => {
                    let text = new TextComponent("&7> &a/" + cmd + " &7- &e" + description)
                    .setClick("run_command", "/" + cmd)
                    .setHover("show_text", `&7Click to run &a/${cmd}`)
                    text.chat()
                }); break;
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
const newVersion = "0.4.8" // hier neue version eintragen wenn changelog angezeigt werden soll
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
    ChatLib.chat(`&7> &athe party finder with /sbopf`)
    ChatLib.chat(`&7> &aa list of useful commands with /sbo help`)
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
    ChatLib.chat(`&7> &7- &aAdded a setting to disable dark auction warp`)
    ChatLib.chat(`&7> &7- &aFixed that importing tracker from skyhanni doesn't work`)
    ChatLib.chat(`&7> &7- &aFixed a bug with achievements having the wrong requirements`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.changelogVersion = newVersion
    data.save()
    changeLogReg.unregister()
}).setFps(1)