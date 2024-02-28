
// Importing constants and utility functions from other files
import { delay } from "./threads";

// Importing the PogObject class from another file named "PogData"
import PogObject from "../../PogData";


// --- PERSISTENT DATA ---

// Initializing a persistent data object using the PogObject class
export let data = new PogObject("SBO", {
    "effects": []
}, "SboData.json");





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
 * Marks that the VolcAddons GUI has been opened.
 */
export function opened() {
    openVA = true;
}

// Event handler for GUI settings close.
register("guiClosed", (event) => {
    if (event.toString().includes("vigilance"))
        setRegisters()
});

// Variable to store the pause state
let paused = false;

/**
 * Returns the current paused state.
 *
 * @returns {boolean} - The current paused state.
 */
export function getPaused() {
    return paused;
}



// MVP+/++ Check
let isMVP = false;

/**
 * Returns whether the player is MVP+ or MVP++ based on chat messages.
 *
 * @returns {boolean} - Whether the player is MVP+ or MVP++.
 */
export function getMVP() {
    return isMVP;
}

// Event handler for chat messages to check MVP status
register("chat", (player) => {
    if (player == Player.getName()) {
        isMVP = true;
    }
}).setCriteria(">>> [MVP++] ${player} joined the lobby! <<<");

// Stats tracking class
export class Stat {
    constructor() {
        // Initializing properties for tracking statistics
        this.reset();
    }

    // Method to reset stat properties
    reset() {
        this.start = 0.00; // Starting Amount
        this.now = 0.00; // Current Amount
        this.gain = 0.00; // Current - Starting Amount
        this.next = 0.00; // Next Level
        this.time = 0.00; // Time passed
        this.rate = 0.00; // Amount/hr
        this.since = 600; // Time since last Amount earn
    }
}

// Saving data to persistent storage upon game unload
register("gameUnload", () => {
    data.save();
});
