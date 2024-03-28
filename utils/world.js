import { delay } from "./threads"; // Importing delay function for asynchronous operations
import { setRegisters } from "./variables"; // Importing setRegisters function from the variables file

export function setPlayer() {
    player = Player.asPlayerMP().getEntity();
};

let world = undefined; // Variable to store the current world
export function getWorld() { return world }; // Exported function to get the current world
let tier = 0; // Variable to store the current tier (used for Kuudra and Dungeons)
export function getTier() { return tier }; // Exported function to get the current tier
let noFind = 0; // Counter to prevent infinite loops when trying to find the world
let zone = "None"; // Variable to store the current zone
export function getZone() { return zone }; // Exported function to get the current zone

// Function to find the current zone (e.g., Hub, Dungeon, Kuudra)
export function findZone() {
    let zoneLine = Scoreboard.getLines().find((line) => line.getName().includes("⏣"));
    // Rift has a different symbol
    if (zoneLine === undefined) zoneLine = Scoreboard.getLines().find((line) => line.getName().includes("ф"));
    return zoneLine === undefined ? "None" : zoneLine.getName().removeFormatting();
}

// Function to find the current world and register/unregister features based on the world
function findWorld() {
    // Infinite loop prevention
    if (noFind === 10) return;
    noFind++;
    // Get world from tab list
    world = TabList.getNames().find(tab => tab.includes("Area"));
    if (world === undefined) {
        // If the world is not found, try again after a delay
        zone = findZone();
        if (zone.includes("Catac")) {
            world = "Catacombs";
            setRegisters();
            setPlayer();
        }
        else {
            delay(() => findWorld(), 1000);
        }
    } else {
        // Get world formatted
        world = world.removeFormatting();
        world = world.substring(world.indexOf(': ') + 2);
        zone = findZone();
        // Get tier (for Kuudra and Dungeons)
        if (world === "Kuudra") {
            delay(() => {
                tier = parseInt(zone.charAt(zone.length - 2));
            }, 1000);
        }

        // Register/unregister features for the current world
        setRegisters();
        setPlayer();
    }
}

// Registering the "worldLoad" event to trigger finding the current world
register("worldLoad", () => {
    noFind = 0; // Resetting the counter when the world loads
    findWorld(); // Finding the current world and its features
});


function getAttributePrice(itemId, attribute, lvl) {
    let tier5Shard = false;
    let valueModifier = 1;
    if (itemId != "ATTRIBUTE_SHARD") {
        itemId = itemId.split("_")[1]
    }
    else {
        if (attribute != "magic_find") {
            valueModifier = 0.8;
        }
    }


    if (kuudraItems[itemId] != undefined) {
        if (itemId == "ATTRIBUTE_SHARD" && lvl >= 4) {
            if (attribute == "magic_find") {
                lvl = 4;
            }
            else {
                lvl = 3;
                tier5Shard = true;
            }
        }
        else {
            tier5Shard = false;
        }
            
        if (kuudraItems[itemId][attribute + "_" + lvl] != undefined) {
            if (tier5Shard) {
                return (kuudraItems[itemId][attribute + "_" + lvl].price * 2) * valueModifier;
            }
            return (kuudraItems[itemId][attribute + "_" + lvl].price) * valueModifier;
        }
        else {
            let counter = 0;
            for (let i = lvl; i > 0; i--) {
                counter++;
                if (kuudraItems[itemId][attribute + "_" + i] != undefined) {
                    print("attribute: " + attribute + " " + i + " price: " + kuudraItems[itemId][attribute + "_" + i].price);
                    if (tier5Shard) {
                        return (kuudraItems[itemId][attribute + "_" + i].price * (2**counter) * 2) * valueModifier;
                    }
                    return (kuudraItems[itemId][attribute + "_" + i].price * (2**counter)) * valueModifier;
                }
                else {
                    console.log("attribute: " + attribute + " " + i + " price: not found");
                
                }
            }
            console.log("attribute: " + attribute + " " + lvl + " price: not found");
            return 0;
        }
    }
    else {
        console.log("itemId: " + itemId + " price: not found");
        return 0;
    }
}