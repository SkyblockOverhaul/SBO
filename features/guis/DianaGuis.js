import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import {  state } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";

registerWhen(register("entityDeath", () => {
    state.entityDeathOccurred = true;
    setTimeout(() => {
        state.entityDeathOccurred = false;
    }, 1000);
}), () => getWorld() === "Hub" && settings.dianaMobTracker);

dianaMobTrackerExample = 
`&4Diana Mob Tracker
Minos Inquisitor: 
Minos Champion:
Minotaur:
Gaia Construct:
Siamese Lynx:
Minos Hunter:
Total Mobs:
`
dianaLootTrackerExample = 
`&4Diana Loot Tracker
Griffin Feather:
Chimera:
Minos Relic:
Daedalus Stick:
Dwarf Turtle Shelmet:
Crochet Tiger Plushies:
Antique Remedies:
Crown of Greed:
Washed-up Souvenir:
Coins:
`
const DianaMobTracker = new Overlay("dianaMobTrackerView",["Hub"], [10,50,1],"moveMobCoounter",dianaMobTrackerExample,"dianaMobTracker");
const DianaLootTracker = new Overlay("dianaLootTrackerView",["Hub"], [10,130,1],"moveLootCoounter",dianaLootTrackerExample,"dianaLootTracker");

/**
 * 
 * @param {string} setting 
 */
export function refreshMobOverlay(mobTracker, setting) {
    if(setting > 0){
    DianaMobTracker.message =
    `&4Diana Mob Tracker
Minos Inquisitor: ${mobTracker["mobs"]["Minos Inquisitor"]}
Minos Champion: ${mobTracker["mobs"]["Minos Champion"]}
Minotaur: ${mobTracker["mobs"]["Minotaur"]}
Gaia Construct: ${mobTracker["mobs"]["Gaia Construct"]}
Siamese Lynx: ${mobTracker["mobs"]["Siamese Lynx"]}
Minos Hunter: ${mobTracker["mobs"]["Minos Hunter"]}
Total Mobs: 
`
    }
}
/**
 * 
 * @param {string} setting 
 */
export function refreshItemOverlay(lootTracker, setting){
    if(setting > 0){
    DianaLootTracker.message = 
    `&4Diana Loot Tracker
Griffin Feather: ${lootTracker["items"]["Griffin Feather"]}
Chimera: ${lootTracker["items"]["Chimera"]}
Minos Relic: ${lootTracker["items"][""]}
Daedalus Stick: ${lootTracker["items"]["Daedalus Stick"]}
Dwarf Turtle Shelmet: ${lootTracker["items"][""]}
Crochet Tiger Plushies: ${lootTracker["items"][""]}
Antique Remedies: ${lootTracker["items"][""]}
Crown of Greed: ${lootTracker["items"]["Crown of Greed"]}
Washed-up Souvenir: ${lootTracker["items"]["Washed-up Souvenir"]}
Enchanted Gold: ${lootTracker["items"]["ENCHANTED_GOLD"]}
Enchanted Iron: ${lootTracker["items"]["ENCHANTED_IRON"]}
Enchanted Ancient Claw: ${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
Ancient Claw: ${lootTracker["items"]["ANCIENT_CLAW"]}
Coins: ${lootTracker["items"]["coins"]}
`
    }
}