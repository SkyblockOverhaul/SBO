import { getDateMayorElected, getNewMayorAtDate, getSkyblockDate, getMayor } from "./mayor";
import { initializeGuiSettings, initializeTracker } from "./functions";
import { registerWhen } from "./variables";

// check if data is loaded and time is set //
export let trackerFileLocation = "config/ChatTriggers/modules/SBO/dianaTracker";

export function isDataLoaded() {
    return dataLoaded;
}

function checkDataLoaded() {
    // check if file exists if not create it
    let tempDict = {};
    if (!FileLib.exists(trackerFileLocation + "Total.json")) {
        tempDict = {};
        tempDict = initializeTracker();
        FileLib.write(trackerFileLocation + "Total.json", JSON.stringify(tempDict, null, 4));
    }

    if (!FileLib.exists(trackerFileLocation + "Mayor.json")) {
        if (getDateMayorElected() != undefined) {
            tempDict = {};
            tempDict[getDateMayorElected().getFullYear()] = initializeTracker();
            FileLib.write(trackerFileLocation + "Mayor.json", JSON.stringify(tempDict, null, 4));
        }
    }

    if (!FileLib.exists(trackerFileLocation + "Session.json")) {
        tempDict = {};
        tempDict = initializeTracker();
        FileLib.write(trackerFileLocation + "Session.json", JSON.stringify(tempDict, null, 4));
    }

    if (!FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json")) {
        tempDict = initializeGuiSettings();
        FileLib.write("config/ChatTriggers/modules/SBO/guiSettings.json", JSON.stringify(tempDict, null, 4));
    }
}


register("step", () => {
    if (!dataLoaded) {
        if (checkAllCriteria()) {
            dataLoaded = true;
            ChatLib.chat("§6[SBO] §4Module Loaded");
        }
    }
}).setFps(1);

// register("worldUnload", () => {
//     dataLoaded = false;
// });

let dataLoaded = false;
function checkAllCriteria() {
    checkDataLoaded();
    let check1 = FileLib.exists(trackerFileLocation + "Total.json");
    let check2 = FileLib.exists(trackerFileLocation + "Mayor.json");
    let check3 = FileLib.exists(trackerFileLocation + "Session.json");
    let check4 = FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json");
    let check5 = (getDateMayorElected() !== undefined  && getNewMayorAtDate() !== undefined && getSkyblockDate() !== undefined);
    let check6 = getMayor() !== undefined;
    if (check1 && check2 && check3 && check4 && check5 && check6) {
        return true;
    }
    return false;
}

register("command", () => {
    let check1 = FileLib.exists(trackerFileLocation + "Total.json");
    let check2 = FileLib.exists(trackerFileLocation + "Mayor.json");
    let check3 = FileLib.exists(trackerFileLocation + "Session.json");
    let check4 = FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json");
    let check5 = (getDateMayorElected() !== undefined  && getNewMayorAtDate() !== undefined && getSkyblockDate() !== undefined);
    let check6 = getMayor() !== undefined;
    print("check1: " + check1 + " check2: " + check2 + " check3: " + check3 + " check4: " + check4 + " check5: " + check5 + " check6: " + check6);
}).setName("sbocheck");