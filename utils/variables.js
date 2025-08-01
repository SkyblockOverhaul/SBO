// Importing constants and utility functions from other files
import { delay } from "./threads";
import { getDateMayorElected, setNewMayorBool } from "./mayor";
// Importing the PogObject class from another file named "PogData"
import PogObject from "../../PogData";
import FU from "../../FileUtilities/main";
import settings from "../settings";


// initialize tracker //
export function initializeTracker() {
    tempTracker = {
        items: {
            "coins": 0,
            "Griffin Feather": 0,
            "Crown of Greed": 0,
            "Washed-up Souvenir": 0,
            "Chimera": 0,
            "ChimeraLs": 0,
            "Daedalus Stick": 0,
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "ENCHANTED_ANCIENT_CLAW": 0,
            "ANCIENT_CLAW": 0,
            "MINOS_RELIC": 0,
            "ENCHANTED_GOLD": 0,
            "ENCHANTED_IRON": 0,
            "Total Burrows": 0,
            "scavengerCoins": 0,
            "fishCoins": 0
        },
        mobs: {
            "Minos Inquisitor": 0,
            "Minos Champion": 0,
            "Minotaur": 0,
            "Gaia Construct": 0,
            "Siamese Lynxes": 0,
            "Minos Hunter": 0,
            "TotalMobs": 0,
            "Minos Inquisitor Ls": 0
        },
        inquis: {
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "DWARF_TURTLE_SHELMET_LS": 0,
            "CROCHET_TIGER_PLUSHIE_LS": 0,
            "ANTIQUE_REMEDIES_LS": 0
        }
    };
    return tempTracker;
}

export function initializeTrackerMayor() {
    tempTracker = {
        year: getDateMayorElected().getFullYear(),
        items: {
            "coins": 0,
            "Griffin Feather": 0,
            "Crown of Greed": 0,
            "Washed-up Souvenir": 0,
            "Chimera": 0,
            "ChimeraLs": 0,
            "Daedalus Stick": 0,
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "ENCHANTED_ANCIENT_CLAW": 0,
            "ANCIENT_CLAW": 0,
            "MINOS_RELIC": 0,
            "ENCHANTED_GOLD": 0,
            "ENCHANTED_IRON": 0,
            "Total Burrows": 0,
            "scavengerCoins": 0,
            "fishCoins": 0,
            "mayorTime": 0
        },
        mobs: {
            "Minos Inquisitor": 0,
            "Minos Champion": 0,
            "Minotaur": 0,
            "Gaia Construct": 0,
            "Siamese Lynxes": 0,
            "Minos Hunter": 0,
            "TotalMobs": 0,
            "Minos Inquisitor Ls": 0
        },
        inquis: {
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "DWARF_TURTLE_SHELMET_LS": 0,
            "CROCHET_TIGER_PLUSHIE_LS": 0,
            "ANTIQUE_REMEDIES_LS": 0
        }
    };
    return tempTracker;
}

// --- PERSISTENT DATA ---
// sbo config folder
let configFolderPath = "./config/sbo";
if (!FU.exists(configFolderPath)) {
    FU.newDirectory(configFolderPath);
}
if (!FU.exists("./config/sbo/backup")) {
    FU.newDirectory("./config/sbo/backup");
}

// Initializing a persistent data object using the PogObject class
export const resetVersion = "0.1.3"; // change this to the new version for config.toml reset
export let data = new PogObject("SBO", {
    "effects": [],
    "resetVersion": resetVersion,
    "changelogVersion": "0.0.0",
    "downloadMsg": false,
    "mobsSinceInq": 0,
    "inqsSinceChim": 0,
    "minotaursSinceStick": 0,
    "champsSinceRelic": 0,
    "inqsSinceLsChim": 0,
    "trackerMigration": false,
    "trackerMigration2": false,
    "highestChimMagicFind": 0,
    "highestStickMagicFind": 0,
    "hideTrackerLines": [],
    "partyBlacklist": [],
    "crownTimer": 0,
    "totalCrownCoins" : 0,
    "lastCrownCoins" : 0,
    "totalCrownCoinsGained" : 0,
    "totalCrownCoinsSession" : 0,
    "cronwTimerSession" : 0,
    "ghostKills" : 0,
    "sorrowDrops" : 0,
    "crownOneMilCoins" : 0,
    "achievementFilter": "Rarity",
    "achievementFix1": false,
    "dianaStats": {},
    "dianaStatsUpdated": 0,
    "lastInqDate": 0,
    "b2bStick": false,
    "b2bChim": false,
    "b2bChimLs": false,
    "b2bInq": false,
}, "SboData.json");




let oldMayorTracker = {};
let oldTotalTracker = {};
let oldSessionTracker = {};
if (!data.trackerMigration) {
    if (!FileLib.exists("SBO", "dianaTrackerMayor.json") && !FileLib.exists("SBO", "dianaTrackerTotal.json") && !FileLib.exists("SBO", "dianaTrackerSession.json")) {
        data.trackerMigration = true;
        data.save();
    }
    else {
        if (FileLib.exists("SBO", "dianaTrackerMayor.json")) {
            // load old mayor tracker
            let tempTracker = {};
            try {
                oldMayorTracker = JSON.parse(FileLib.read("SBO", "dianaTrackerMayor.json")) || {};
            } catch (e) {
                oldMayorTracker = {};
            }
            // for each key in old tracker
            for (let key in oldMayorTracker) {
                pastDianaEvents.events.push({
                    year: key,
                    items: oldMayorTracker[key].items,
                    mobs: oldMayorTracker[key].mobs
                });
                if (key === Object.keys(oldMayorTracker)[Object.keys(oldMayorTracker).length - 1]) {
                    tempTracker = oldMayorTracker[key];
                }
            }
            oldMayorTracker = tempTracker;
            FileLib.delete("SBO", "dianaTrackerMayor.json");
            pastDianaEvents.save();
        }

        if (FileLib.exists("SBO", "dianaTrackerTotal.json")) {
            // load old total tracker
            try {
                oldTotalTracker = JSON.parse(FileLib.read("SBO", "dianaTrackerTotal.json")) || {};
            } catch (e) {
                oldTotalTracker = {};
            }
            FileLib.delete("SBO", "dianaTrackerTotal.json");
        }

        if (FileLib.exists("SBO", "dianaTrackerSession.json")) {
            // load old session tracker
            try {
                oldSessionTracker = JSON.parse(FileLib.read("SBO", "dianaTrackerSession.json")) || {};
            } catch (e) {
                oldSessionTracker = {};
            }
            FileLib.delete("SBO", "dianaTrackerSession.json");
        }
    }
}

let sboFiles = [
    "SboData.json",
    "pastDianaEvents.json",
    "sbo_achievements.json",
    "dianaTrackerTotal.json",
    "dianaTrackerSession.json",
    "dianaTrackerMayor.json"
];


if (!data.trackerMigration2) {
    if (FU.exists("./config/dianaTrackerTotal.json")) {
        for (let file of sboFiles) {
            if (FU.exists("./config/" + file)) {
                FU.moveFile("./config/" + file, "./config/sbo/" + file);
            }
        }
    }
    data.trackerMigration2 = true;
    data.save();
}

export let pastDianaEvents = new PogObject("../../../config/sbo", {
    "events": []
}, "pastDianaEvents.json");

export let dianaTrackerTotal = new PogObject("../../../config/sbo", {
    items: {
        "coins": 0,
        "Griffin Feather": 0,
        "Crown of Greed": 0,
        "Washed-up Souvenir": 0,
        "Chimera": 0,
        "ChimeraLs": 0,
        "Daedalus Stick": 0,
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "ENCHANTED_ANCIENT_CLAW": 0,
        "ANCIENT_CLAW": 0,
        "MINOS_RELIC": 0,
        "ENCHANTED_GOLD": 0,
        "ENCHANTED_IRON": 0,
        "Total Burrows": 0,
        "scavengerCoins": 0,
        "fishCoins": 0,
        "totalTime": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0,
        "Minos Inquisitor Ls": 0
    },
    inquis: {
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "DWARF_TURTLE_SHELMET_LS": 0,
        "CROCHET_TIGER_PLUSHIE_LS": 0,
        "ANTIQUE_REMEDIES_LS": 0
    }
}, "dianaTrackerTotal.json");

export let dianaTrackerSession = new PogObject("../../../config/sbo", {
    items: {
        "coins": 0,
        "Griffin Feather": 0,
        "Crown of Greed": 0,
        "Washed-up Souvenir": 0,
        "Chimera": 0,
        "ChimeraLs": 0,
        "Daedalus Stick": 0,
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "ENCHANTED_ANCIENT_CLAW": 0,
        "ANCIENT_CLAW": 0,
        "MINOS_RELIC": 0,
        "ENCHANTED_GOLD": 0,
        "ENCHANTED_IRON": 0,
        "Total Burrows": 0,
        "scavengerCoins": 0,
        "fishCoins": 0,
        "sessionTime": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0,
        "Minos Inquisitor Ls": 0
    },
    inquis: {
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "DWARF_TURTLE_SHELMET_LS": 0,
        "CROCHET_TIGER_PLUSHIE_LS": 0,
        "ANTIQUE_REMEDIES_LS": 0
    }
}, "dianaTrackerSession.json");

export let dianaTrackerMayor = new PogObject("../../../config/sbo", {
    year: 0,
    items: {
        "coins": 0,
        "Griffin Feather": 0,
        "Crown of Greed": 0,
        "Washed-up Souvenir": 0,
        "Chimera": 0,
        "ChimeraLs": 0,
        "Daedalus Stick": 0,
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "ENCHANTED_ANCIENT_CLAW": 0,
        "ANCIENT_CLAW": 0,
        "MINOS_RELIC": 0,
        "ENCHANTED_GOLD": 0,
        "ENCHANTED_IRON": 0,
        "Total Burrows": 0,
        "scavengerCoins": 0,
        "fishCoins": 0,
        "mayorTime": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0,
        "Minos Inquisitor Ls": 0
    },
    inquis: {
        "DWARF_TURTLE_SHELMET": 0,
        "CROCHET_TIGER_PLUSHIE": 0,
        "ANTIQUE_REMEDIES": 0,
        "DWARF_TURTLE_SHELMET_LS": 0,
        "CROCHET_TIGER_PLUSHIE_LS": 0,
        "ANTIQUE_REMEDIES_LS": 0
    }
}, "dianaTrackerMayor.json");

export let achievementsData = new PogObject("../../../config/sbo", {
    "unlocked": []
}, "sbo_achievements.json");

if (!data.trackerMigration) {
    // check if old tracker exists and is not empty
    if (Object.keys(oldMayorTracker).length != 0) {
        for (let key in oldMayorTracker.items) {
            dianaTrackerMayor.items[key] = oldMayorTracker.items[key];
        }
        for (let key in oldMayorTracker.mobs) {
            dianaTrackerMayor.mobs[key] = oldMayorTracker.mobs[key];
        }
    }
    if (Object.keys(oldTotalTracker).length != 0) {
        for (let key in oldTotalTracker.items) {
            dianaTrackerTotal.items[key] = oldTotalTracker.items[key];
        }
        for (let key in oldTotalTracker.mobs) {
            dianaTrackerTotal.mobs[key] = oldTotalTracker.mobs[key];
        }
    }
    if (Object.keys(oldSessionTracker).length != 0) {
        for (let key in oldSessionTracker.items) {
            dianaTrackerSession.items[key] = oldSessionTracker.items[key];
        }
        for (let key in oldSessionTracker.mobs) {
            dianaTrackerSession.mobs[key] = oldSessionTracker.mobs[key];
        }
    }   
    data.trackerMigration = true;
}
data.save();
dianaTrackerTotal.save();
dianaTrackerSession.save();
dianaTrackerMayor.save();
pastDianaEvents.save();

export function checkMayorTracker() {
    if (dianaTrackerMayor.year < getDateMayorElected().getFullYear()) {  
        let tempTracker = initializeTrackerMayor();
        for (let key in dianaTrackerMayor) {
            tempTracker[key] = dianaTrackerMayor[key];
        }
        if (dianaTrackerMayor.year != 0) {
            // check if all keys have the value 0
            let allZero = true;
            for (let key in tempTracker.items) {
                if (tempTracker.items[key] != 0) {
                    allZero = false;
                    break;
                }
            }
            for (let key in tempTracker.mobs) {
                if (tempTracker.mobs[key] != 0) {
                    allZero = false;
                    break;
                }
            }
            if (!allZero) {
                pastDianaEvents["events"].push(tempTracker);
            }
        }
        let newTracker = initializeTrackerMayor();
        for (let key in newTracker) {
            dianaTrackerMayor[key] = newTracker[key];
        }
        pastDianaEvents.save();
        timerMayor.reset();
        dianaTrackerMayor.save();
        data.save();
    }
}

export function resetTracker(type) {
    let trackerToReset = undefined;
    if (type == "total") {
        trackerToReset = dianaTrackerTotal;
        timerTotal.reset();
    } else if (type == "session") {
        trackerToReset = dianaTrackerSession;
        timerSession.reset();
    } else if (type == "mayor") {
        trackerToReset = dianaTrackerMayor;
        timerMayor.reset();
    }
    if (trackerToReset && type != "mayor") {
        let newTracker = initializeTracker();
        for (let key in newTracker.items) {
            trackerToReset.items[key] = newTracker.items[key];
        }
        for (let key in newTracker.mobs) {
            trackerToReset.mobs[key] = newTracker.mobs[key];
        }
        trackerToReset.save();
    } else if (trackerToReset && type == "mayor") {
        let newTracker = initializeTrackerMayor();
        for (let key in newTracker.items) {
            trackerToReset.items[key] = newTracker.items[key];
        }
        for (let key in newTracker.mobs) {
            trackerToReset.mobs[key] = newTracker.mobs[key];
        }
        trackerToReset.year = newTracker.year;
        trackerToReset.save();
    }

}

let lastyear = 0;   
register("chat", () => {
    lastyear = dianaTrackerMayor.year;
    let tempTracker = initializeTrackerMayor();
    for (let key in dianaTrackerMayor) {
        tempTracker[key] = dianaTrackerMayor[key];
    }
    if (dianaTrackerMayor.year != 0) {
        let allZero = true;
        for (let key in tempTracker.items) {
            if (tempTracker.items[key] != 0) {
                allZero = false;
                break;
            }
        }
        for (let key in tempTracker.mobs) {
            if (tempTracker.mobs[key] != 0) {
                allZero = false;
                break;
            }
        }
        if (!allZero) {
            pastDianaEvents["events"].push(tempTracker);
            ChatLib.chat("&7[&6SBO&7] &eMayor " + dianaTrackerMayor.year + " has ended.");
            new TextComponent("&7[&6SBO&7] &eYou can view the stats for this event with &6/sbopastdianaevents").setClick("run_command", "/sbopde").setHoverValue("&7Click to view the stats").chat();
        }
    }
    let newTracker = initializeTrackerMayor();
    for (let key in newTracker) {
        dianaTrackerMayor[key] = newTracker[key];
    }
    if (lastyear == dianaTrackerMayor.year) {
        dianaTrackerMayor.year++;
    }
    pastDianaEvents.save();
    timerMayor.reset();
    dianaTrackerMayor.save();
    setNewMayorBool();
}).setCriteria("&r&eThe election room is now closed. Clerk Seraphine is doing a final count of the votes...&r");

/**
 * Adds a trigger with its associated dependency to the list of registered triggers.
 *
 * @param {string} name - name of the timer.
 * @param {number} inactiveTimeLimit - the time limit in minutes for inactivity.
 * @param {object} trackerObject - the Pog object to be updated.
 * @param {string} dataFieldName - the name of the field in the Pog object.
 * @param {string} dataFieldClass - the class of the field in the Pog object. (Optional)
 */
export class SBOTimer {
    static timerList = [];
    constructor(name, inactiveTimeLimit, trackerObject, dataFieldName, dataFieldClass = false) {
        this.name = name;
        this.startTime = 0;
        this.elapsedTime = 0;
        this.running = false;
        this.startedOnce = false;
        this.lastActivityTime = Date.now(); 
        this.INACTIVITY_LIMIT = inactiveTimeLimit * 60 * 1000; // milliseconds
        this.tickEvent = null; // Timeout-ID
        this.trackerObject = trackerObject; // Tracker object (total/session/mayor)
        this.dataFieldName = dataFieldName; // Name of the field in the tracker object#
        this.dataFieldClass = dataFieldClass;
        this.inactivityFlag = false;

        SBOTimer.timerList.push(this);
    }

    // Starts the timer
    start() {
        if (this.running || this.startedOnce) return;
        this.startTime = Date.now();
        if (this.dataFieldClass) {
            if (this.trackerObject[this.dataFieldClass][this.dataFieldName] > 0) {
                this.elapsedTime = this.trackerObject[this.dataFieldClass][this.dataFieldName];
            }
        }
        else {
            if (this.trackerObject[this.dataFieldName] > 0) {
                this.elapsedTime = this.trackerObject[this.dataFieldName];
            }
        }
        this.running = true;
        this.startedOnce = true;
        this.updateElapsedTime();
        this.startInactivityCheck();
    }

    // Updates the elapsed time based on the time since the last update
    updateElapsedTime() {
        if (!this.running) return;
        const now = Date.now(); 
        this.elapsedTime += now - this.startTime;
        this.startTime = now;
        if (this.dataFieldClass) {
            this.trackerObject[this.dataFieldClass][this.dataFieldName] = this.elapsedTime;
        }
        else {
            this.trackerObject[this.dataFieldName] = this.elapsedTime;
        }
    }

    // Pauses the timer
    pause() {
        if (!this.running) return;
        this.updateElapsedTime();
        this.running = false;
        this.stopInactivityCheck();
    }

    // Continues the timer from where it was paused
    continue() {
        if (this.running) return;
        if(this.inactivityFlag) {
            this.elapsedTime -= this.INACTIVITY_LIMIT;
        }
        this.startTime = Date.now();
        this.running = true;
        this.startInactivityCheck();
    }

    // Resets the timer to 0
    reset() {
        this.running = false;
        this.startedOnce = false;
        if (this.dataFieldClass) {
            this.trackerObject[this.dataFieldClass][this.dataFieldName] = 0;
        }
        else {
            this.trackerObject[this.dataFieldName] = 0;
        }
        this.elapsedTime = 0;
        this.startTime = 0;
        this.stopInactivityCheck();
    }

    // Returns the elapsed time in milliseconds
    getElapsedTime() {
        return this.elapsedTime;
    }

    getHourTime() {
        let millisecondTime = 0;
        if (this.dataFieldClass) {
            millisecondTime = this.trackerObject[this.dataFieldClass][this.dataFieldName];
        }
        else {
            millisecondTime = this.trackerObject[this.dataFieldName];
        }
        let hours = (millisecondTime / 3600000).toFixed(6);
        return hours;
    }

    // Updates the last activity time to the current time
    updateActivity() {
        this.lastActivityTime = Date.now();
    }

    // Starts the inactivity check
    startInactivityCheck() {
        if (!this.tickEvent && this.running) {
            this.tickEvent = register("tick", () => {
                this.updateElapsedTime();
                if (Date.now() - this.lastActivityTime > this.INACTIVITY_LIMIT && this.running) {
                    this.pause();
                    if(!this.inactivityFlag) {
                        if (this.dataFieldClass) {
                            this.trackerObject[this.dataFieldClass][this.dataFieldName] -= this.INACTIVITY_LIMIT;
                        }
                        else {
                            this.trackerObject[this.dataFieldName] -= this.INACTIVITY_LIMIT;
                        }
                        this.inactivityFlag = true;
                    }
                }
            });
        }
    }

    // Stops the inactivity check
    stopInactivityCheck() {
        if (this.tickEvent) {
            this.tickEvent.unregister();
            this.tickEvent = null;
            this.inactivityFlag = false;
        }
    }
}

const timerTotal = new SBOTimer("Total", 1.5, dianaTrackerTotal, "totalTime", "items");
const timerSession = new SBOTimer("Session", 1.5, dianaTrackerSession, "sessionTime", "items");
const timerMayor = new SBOTimer("Mayor", 1.5, dianaTrackerMayor, "mayorTime", "items");
export let dianaTimerlist = [timerTotal, timerMayor, timerSession];

export const timerCrown = new SBOTimer("Crown", 1, data, "crownTimer");
export const timerCrownSession = new SBOTimer("CrownSession", 1, data, "cronwTimerSession");

register("serverDisconnect" , () => {
    for (let timer of SBOTimer.timerList) {
        timer.pause();
    }
});

// --- TRIGGER CONTROL ---

function getPropertyName(name) {
    if (settings.__config_props__[name]) {
        try {
            return settings.__config_props__[name].getName();
        }
        catch (e) {
            return null;
        }
    }
}

// An array to store registered triggers and their dependencies
let worldRegisters = [];
let registerListeners = {};

/**
 * Adds a trigger with its associated dependency to the list of registered triggers.
 *
 * @param {Trigger} trigger - The trigger to be added.
 * @param {function} dependency - The function representing the dependency of the trigger.
 * @param {string} context - The context of the dependency. (Optional)
 */
export function registerWhen(trigger, dependency, context = null) {
    const dependencyStr = dependency.toString();

    const matches = dependencyStr.match(/settings\.(\w+)/g) || [];

    let fieldnames = [...new Set(matches.map(match => match.split('.')[1]))];

    if (context) fieldnames.push(context);  

    if (dependencyStr.includes("getWorld(")) worldRegisters.push([trigger, dependency]);

    let propnames = fieldnames.map(fieldname => getPropertyName(fieldname));
    
    propnames.forEach(propname => {
        if (registerListeners[propname]) {
            registerListeners[propname].push([trigger.unregister(), dependency]);
        }
        else {
            registerListeners[propname] = [[trigger.unregister(), dependency]];
        }
    });
}

function setTrigger(trigger) {
    if (trigger[1]()) {
        trigger[0].register();
    }
    else {
        trigger[0].unregister();
    }
}

export function setListener() {
    Object.keys(registerListeners).forEach(propname => {
        let triggers = registerListeners[propname];
        settings.registerListener(propname, bool => {
            triggers.forEach(trigger => {
                setTimeout(() => {
                    setTrigger(trigger);
                }, 300);
            });
        });
        triggers.forEach(trigger => {
            setTrigger(trigger);
        });
        
    });
}

export function setWorldRegisters() {
    worldRegisters.forEach(trigger => {
        setTrigger(trigger);
    });
}

// Saving data to persistent storage upon game unload
register("gameUnload", () => {
    backUpData();
});

export function checkPastDianaEvents() {
    // remove all event that have 0 TotalBurrows
    let tempEvents = [];
    for (let event of pastDianaEvents.events) {
        if (event.items["Total Burrows"]) {
            if (event.items["Total Burrows"] != 0) {
                tempEvents.push(event);
            }
        }
    }
    pastDianaEvents.events = tempEvents;
    pastDianaEvents.save();
}

const File = Java.type("java.io.File");
const FileInputStream = Java.type("java.io.FileInputStream");
const FileOutputStream = Java.type("java.io.FileOutputStream");
const ZipEntry = Java.type("java.util.zip.ZipEntry");
const ZipOutputStream = Java.type("java.util.zip.ZipOutputStream");
const ArrayClass = Java.type("java.lang.reflect.Array");
const ByteType = Java.type("java.lang.Byte").TYPE;

function addFolderToZip(folder, parentPath, zipOut) {
    let files = folder.listFiles();
    if (!files) return;
    for (let i = 0; i < files.length; i++) {
        let file = files[i];
        let zipName = parentPath + file.getName();
        if (file.isDirectory()) {
            zipOut.putNextEntry(new ZipEntry(zipName + "/"));
            zipOut.closeEntry();
            addFolderToZip(file, zipName + "/", zipOut);
        } else {
            let fis = new FileInputStream(file);
            zipOut.putNextEntry(new ZipEntry(zipName));
            let buffer = ArrayClass.newInstance(ByteType, 1024);
            let bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zipOut.write(buffer, 0, bytesRead);
            }
            fis.close();
            zipOut.closeEntry();
        }
    }
}

function zipFolder(folderPath, zipFilePath) {
    let folder = new File(folderPath);
    let fileOut = new FileOutputStream(zipFilePath);
    let zipOut = new ZipOutputStream(fileOut);
    addFolderToZip(folder, "", zipOut);
    zipOut.close();
    fileOut.close();
}

function backUpData() {
    let date = new Date();
    let dateString = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate();
    let timeString = date.getHours() + "-" + date.getMinutes() + "-" + date.getSeconds();
    let folderName = "SBOBackup_" + dateString + "_" + timeString;
    FU.newDirectory("./config/sbo/backup/" + folderName);

    FileLib.write("./config/sbo/backup/" + folderName + "/sboData.json", JSON.stringify(data, null, 4));
    FileLib.write("./config/sbo/backup/" + folderName + "/dianaTrackerTotal.json", JSON.stringify(dianaTrackerTotal, null, 4));
    FileLib.write("./config/sbo/backup/" + folderName + "/dianaTrackerSession.json", JSON.stringify(dianaTrackerSession, null, 4));
    FileLib.write("./config/sbo/backup/" + folderName + "/dianaTrackerMayor.json", JSON.stringify(dianaTrackerMayor, null, 4));
    FileLib.write("./config/sbo/backup/" + folderName + "/pastDianaEvents.json", JSON.stringify(pastDianaEvents, null, 4));
    FileLib.write("./config/sbo/backup/" + folderName + "/sbo_achievements.json", JSON.stringify(achievementsData, null, 4));

    zipFolder("./config/sbo/backup/" + folderName, "./config/sbo/backup/" + folderName + ".zip");
    FU.delete("./config/sbo/backup/" + folderName);

    let files = FU.listFiles("./config/sbo/backup");
    if (files.length > 5) {
        let oldest = files[0];
        for (let file of files) {
            if (FU.getLastModifiedTime(file) < FU.getLastModifiedTime(oldest)) {
                oldest = file;
            }
        }
        FU.delete(oldest);
    }
}