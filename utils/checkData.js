import { getDateMayorElected, getNewMayorAtDate, getSkyblockDate, getMayor } from "./mayor";
import { initializeGuiSettings, initializeTracker } from "./functions";

// check if data is loaded and time is set //
export let trackerFileLocation  = "./dianaTracker";

export function isDataLoaded() {
    return dataLoaded;
}

export function getTrackerTotal() {
    return trackerTotal;
}

export function getTrackerMayor() {
    return trackerMayor;
}

export function getTrackerSession() {
    return trackerSession;
}


let trackerTotal = {};
let trackerMayor = {};
let trackerSession = {};
function checkDataLoaded() {
    if (!FileLib.exists("SBO", trackerFileLocation  + "Total.json")) {
        trackerTotal = initializeTracker();
        FileLib.write("SBO", trackerFileLocation  + "Total.json", JSON.stringify(trackerTotal, null, 4));
    }
    else {
        if (Object.keys(trackerTotal).length == 0) {
            trackerTotal = loadTracker("Total");
        }
    }

    if (!FileLib.exists("SBO", trackerFileLocation  + "Mayor.json")) {
        if (getDateMayorElected() != undefined) {
            trackerMayor[getDateMayorElected().getFullYear()] = initializeTracker();
            FileLib.write("SBO", trackerFileLocation  + "Mayor.json", JSON.stringify(trackerMayor, null, 4));
        }
    }
    else {
        // remove all old tracker that are empty 
        if (Object.keys(trackerMayor).length == 0) {
            if (getDateMayorElected() != undefined) {
                trackerMayor = loadTracker("Mayor");
                // remove all old tracker that are empty
                for (let trackerId in trackerMayor) {
                    let tracker = trackerMayor[trackerId];
                    let items = tracker.items;
                    let mobs = tracker.mobs;
            
                    let allItemsZero = true;
                    for (let key in items) {
                        if (items[key] != 0) {
                            allItemsZero = false;
                            break;
                        }
                    }
                    let allMobsZero = true;
                    for (let key in mobs) {
                        if (mobs[key] != 0) {
                            allMobsZero = false;
                            break;
                        }
                    }
                    if (allItemsZero && allMobsZero) {
                        delete trackerMayor[trackerId];
                    }
                }
                // add new year if not exist
                if (!trackerMayor.hasOwnProperty(getDateMayorElected().getFullYear())) {
                    trackerMayor[getDateMayorElected().getFullYear()] = initializeTracker();
                }
                FileLib.write("SBO", trackerFileLocation  + "Mayor.json", JSON.stringify(trackerMayor, null, 4));
            }
        }
    }

    if (!FileLib.exists("SBO", trackerFileLocation  + "Session.json")) {
        trackerSession = initializeTracker();
        FileLib.write("SBO", trackerFileLocation  + "Session.json", JSON.stringify(trackerSession, null, 4));
    }
    else {
        if (Object.keys(trackerSession).length == 0) {
            trackerSession = loadTracker("Session");
        }
    }

    if (!FileLib.exists("SBO", "guiSettings.json")) {
        let tempDict = initializeGuiSettings();
        FileLib.write("SBO", "guiSettings.json", JSON.stringify(tempDict, null, 4));
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
function loadTracker(type) {
    let loadedTracker = {};
    try {
        loadedTracker = JSON.parse(FileLib.read("SBO", trackerFileLocation  + type + ".json")) || {};
    } catch (e) {
        loadedTracker = {};
    }
    return loadedTracker;
}


let dataLoaded = false;

function checkAllCriteria() {
    checkDataLoaded();
    let check1 = FileLib.exists("SBO", trackerFileLocation  + "Total.json");
    let check2 = FileLib.exists("SBO", trackerFileLocation  + "Mayor.json");
    let check3 = FileLib.exists("SBO", trackerFileLocation  + "Session.json");
    let check4 = FileLib.exists("SBO", "guiSettings.json");
    let check5 = (getDateMayorElected() !== undefined  && getNewMayorAtDate() !== undefined && getSkyblockDate() !== undefined);
    let check6 = getMayor() !== undefined;
    let check7 = ((Object.keys(trackerMayor).length != 0) && (Object.keys(trackerTotal).length != 0) && (Object.keys(trackerSession).length != 0));
    let check8 = (trackerTotal !== undefined && trackerMayor !== undefined && trackerSession !== undefined);
    if (check1 && check2 && check3 && check4 && check5 && check6 && check7 && check8) {
        return true;
    }
    return false;
}

// register("command", () => {
//     let check1 = FileLib.exists("SBO", trackerFileLocation  + "Total.json");
//     let check2 = FileLib.exists("SBO", trackerFileLocation  + "Mayor.json");
//     let check3 = FileLib.exists("SBO", trackerFileLocation  + "Session.json");
//     let check4 = FileLib.exists("SBO", "guiSettings.json");
//     let check5 = (getDateMayorElected() !== undefined  && getNewMayorAtDate() !== undefined && getSkyblockDate() !== undefined);
//     let check6 = getMayor() !== undefined;
//     print("check1: " + check1 + " check2: " + check2 + " check3: " + check3 + " check4: " + check4 + " check5: " + check5 + " check6: " + check6);
// }).setName("sbocheck");