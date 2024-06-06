
// Importing constants and utility functions from other files
import { delay } from "./threads";

// Importing the PogObject class from another file named "PogData"
import PogObject from "../../PogData";


// --- PERSISTENT DATA ---

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
    "trackerMigration": false,
}, "SboData.json");

export let dianaTrackerTotal = new PogObject("SBO", {
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
        "Total Burrows": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0
    }
}, "dianaTrackerTotal.json");

export let dianaTrackerSession = new PogObject("SBO", {
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
        "Total Burrows": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0
    }
}, "dianaTrackerSession.json");

export let pastDianaEvents = new PogObject("SBO", {
    "events": []
}, "pastDianaEvents.json");

if (!data.trackerMigration) {
    data.trackerMigration = true;
    if (FileLib.exists("SBO", "dianaTrackerMayor.json")) {
        // load old mayor tracker
        let oldTracker = {};
        try {
            oldTracker = JSON.parse(FileLib.read("SBO", "dianaTrackerMayor.json")) || {};
        } catch (e) {
            oldTracker = {};
        }
        // for each key in old tracker
        for (let key in oldTracker) {
            pastDianaEvents.events.push({
                year: key,
                items: oldTracker[key].items,
                mobs: oldTracker[key].mobs
            });
        }
        FileLib.delete("SBO", "dianaTrackerMayor.json");
    }
    pastDianaEvents.save();
}

export let dianaTrackerMayor = new PogObject("SBO", {
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
        "Total Burrows": 0
    },
    mobs: {
        "Minos Inquisitor": 0,
        "Minos Champion": 0,
        "Minotaur": 0,
        "Gaia Construct": 0,
        "Siamese Lynxes": 0,
        "Minos Hunter": 0,
        "TotalMobs": 0
    }
}, "dianaTrackerMayor.json");


// --- TRIGGER CONTROL ---

// An array to store registered triggers and their dependencies
let registers = [];
let openVA = false;

/**
 * Adds a trigger with its associated dependency to the list of registered triggers.
 *
 * @param {Trigger} trigger - The trigger to be added.
 * @param {function} dependency - The function representing the dependency of the trigger.
 */
export function registerWhen(trigger, dependency) {
    registers.push([trigger.unregister(), dependency, false]);
}

// Updates trigger registrations based on world or GUI changes
export function setRegisters() {
    registers.forEach(trigger => {
        if ((!trigger[1]() && trigger[2]) || !Scoreboard.getTitle().removeFormatting().includes("SKYBLOCK")) {
            trigger[0].unregister();
            trigger[2] = false;
        } else if (trigger[1]() && !trigger[2]) {
            trigger[0].register();
            trigger[2] = true;
        }
    });
}
delay(() => setRegisters(), 1000);

/**
 * Marks that the SBO GUI has been opened.
 */
export function opened() {
    openVA = true;
}

// Event handler for GUI settings close.
register("guiClosed", (event) => {
    // || event.toString().includes("JSGui")
    if (event.toString().includes("vigilance")) {
        setRegisters()
    }
});

// Saving data to persistent storage upon game unload
register("gameUnload", () => {
    data.save();
    dianaTrackerTotal.save();
    dianaTrackerSession.save();
    dianaTrackerMayor.save();
});

