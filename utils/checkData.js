import { registerWhen } from "./variables";
import { getDateMayorElected } from "./mayor";

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
        FileLib.write(trackerFileLocation + "Total.json", JSON.stringify(tempDict));
    }

    if (!FileLib.exists(trackerFileLocation + "Mayor.json")) {
        if (getDateMayorElected() != undefined) {
            tempDict = {};
            tempDict[getDateMayorElected().getFullYear()] = initializeTracker();
            FileLib.write(trackerFileLocation + "Mayor.json", JSON.stringify(tempDict));
        }
    }
    if (!FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json")) {
        tempDict = {
            MobLoc: {
                "x": 10,
                "y": 50,
                "s": 1
            },
            LootLoc: {
                "x": 10,
                "y": 150,
                "s": 1
            }
        };
        FileLib.write("config/ChatTriggers/modules/SBO/guiSettings.json", JSON.stringify(tempDict));
    }
}

let dataLoaded = false;
let firstMessage = true;
registerWhen(register("step", () => {
    checkDataLoaded();
    let check1 = FileLib.exists(trackerFileLocation + "Total.json");
    let check2 = FileLib.exists(trackerFileLocation + "Mayor.json");
    let check3 = FileLib.exists("config/ChatTriggers/modules/SBO/guiSettings.json");
    let check4 = (getDateMayorElected() !== undefined);
    if (check1 && check2 && check3 && check4) {
        dataLoaded = true;
        if (firstMessage) {
            ChatLib.chat("§6[SBO] §4Module Loaded");
            firstMessage = false;
        }
    }
}).setFps(1), () => !dataLoaded);

register("command", () => {
    ChatLib.chat("§6[SBO] §4Data Loaded: " + dataLoaded);
    ChatLib.chat("§6[SBO] §4Mayor Date: " + getDateMayorElected());
}).setName("sbocheck");


// initialize tracker //
export function initializeTracker() {
    tempTracker = {
        items: {
            "coins": 0,
            "Griffin Feather": 0,
            "Crown of Greed": 0,
            "Washed-up Souvenir": 0,
            "Chimera": 0,
            "Daedalus Stick": 0,
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "ENCHANTED_ANCIENT_CLAW": 0,
            "ANCIENT_CLAW": 0,
            "MINOS_RELIC": 0,

            "ROTTEN_FLESH": 0,
            "WOOD": 0,
            "Potato": 0,
            "Carrot": 0
        },
        mobs: {
            "Minos Inquisitor": 0,
            "Minos Champion": 0,
            "Minotaur": 0,
            "Gaia Construct": 0,
            "Siamese Lynx": 0,
            "Minos Hunter": 0,
            "TotalMobs": 0
        }
    };
    return tempTracker;
}