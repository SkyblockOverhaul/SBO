import { getDateMayorElected } from "./mayor";
import { initializeGuiSettings, initializeTracker } from "./functions";

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
    if (!FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json")) {
        tempDict = initializeGuiSettings();
        FileLib.write("config/ChatTriggers/modules/SBO/guiSettings.json", JSON.stringify(tempDict, null, 4));
    }
}

let dataLoaded = false;
register("step", () => {
    if (!dataLoaded) {
        checkDataLoaded();
        let check1 = FileLib.exists(trackerFileLocation + "Total.json");
        let check2 = FileLib.exists(trackerFileLocation + "Mayor.json");
        let check3 = FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json");
        let check4 = (getDateMayorElected() !== undefined);
        if (check1 && check2 && check3 && check4) {
            dataLoaded = true;
            ChatLib.chat("§6[SBO] §4Module Loaded");
        }
    }
}).setFps(1);


