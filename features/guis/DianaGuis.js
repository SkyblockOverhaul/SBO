import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { state } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC} from "../../utils/constants";
import { getDateMayorElected } from "../../utils/mayor";

registerWhen(register("entityDeath", () => {
    state.entityDeathOccurred = true;
    setTimeout(() => {
        state.entityDeathOccurred = false;
    }, 1000);
}), () => getWorld() === "Hub" && settings.dianaMobTracker);

dianaMobTrackerExample = 
`${YELLOW}${BOLD}Diana Mob Tracker
------------------
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${GRAY}3
${DARK_PURPLE}${BOLD}Minos Champion: ${GRAY}${BOLD}2
${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}10
${GREEN}${BOLD}Gaia Construct: ${WHITE}${BOLD}100
${GREEN}${BOLD}Siamese Lynx:
${GREEN}${BOLD}Minos Hunter:
${GRAY}${BOLD}Total Mobs:
`
dianaLootTrackerExample = 
`${YELLOW}${BOLD}Diana Loot Tracker
-------------------
${LIGHT_PURPLE}${BOLD}Chimera: ${WHITE}100
${DARK_PURPLE}${BOLD}Minos Relic: ${WHITE}10
${GOLD}${BOLD}Daedalus Stick: ${GRAY}2
${GOLD}${BOLD}Griffin Feather: ${GRAY}3
${GOLD}${BOLD}Crown of Greed:
${GOLD}${BOLD}Washed-up Souvenir:
${DARK_GREEN}${BOLD}Dwarf Turtle Shelmet:
${DARK_GREEN}${BOLD}Crochet Tiger Plushies:
${DARK_GREEN}${BOLD}Antique Remedies:
${GOLD}${BOLD}Coins:
`

// ${GRAY}${BOLD}Enchanted Gold: 
// ${GRAY}${BOLD}Enchanted Iron: 
// ${GRAY}${BOLD}Enchanted Ancient Claw: 
// ${GRAY}${BOLD}Ancient Claw: 

const DianaMobTracker = new Overlay("dianaMobTrackerView",["Hub"], [10,50,1],"moveMobCoounter",dianaMobTrackerExample,"dianaMobTracker");
const DianaLootTracker = new Overlay("dianaLootTrackerView",["Hub"], [10,150,1],"moveLootCoounter",dianaLootTrackerExample,"dianaLootTracker");

/**
 * 
 * @param {string} setting 
 */
export function refreshMobOverlay(mobTracker, setting) {
    if (setting == 2) {
        mobTracker = mobTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
    DianaMobTracker.message =
    `${YELLOW}${BOLD}Diana Mob Tracker
------------------
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${mobTracker["mobs"]["Minos Inquisitor"]}
${DARK_PURPLE}${BOLD}Minos Champion: ${mobTracker["mobs"]["Minos Champion"]}
${GOLD}${BOLD}Minotaur: ${mobTracker["mobs"]["Minotaur"]}
${GREEN}${BOLD}Gaia Construct: ${mobTracker["mobs"]["Gaia Construct"]}
${GREEN}${BOLD}Siamese Lynx: ${mobTracker["mobs"]["Siamese Lynx"]}
${GREEN}${BOLD}Minos Hunter: ${mobTracker["mobs"]["Minos Hunter"]}
${GRAY}${BOLD}Total Mobs:
`
    }
}
/**
 * 
 * @param {string} setting 
 */
export function refreshItemOverlay(lootTracker, setting){
    if (setting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
    DianaLootTracker.message =
    `${YELLOW}${BOLD}Diana Loot Tracker
-------------------
${LIGHT_PURPLE}${BOLD}Chimera: ${GRAY}${lootTracker["items"]["Chimera"]}
${DARK_PURPLE}${BOLD}Minos Relic: ${GRAY}${lootTracker["items"][""]}
${GOLD}${BOLD}Daedalus Stick: ${GRAY}${lootTracker["items"]["Daedalus Stick"]}
${GOLD}${BOLD}Griffin Feather: ${GRAY}${lootTracker["items"]["Griffin Feather"]}
${GOLD}${BOLD}Crown of Greed: ${GRAY}${lootTracker["items"]["Crown of Greed"]}
${GOLD}${BOLD}Washed-up Souvenir: ${GRAY}${lootTracker["items"]["Washed-up Souvenir"]}
${DARK_GREEN}${BOLD}Dwarf Turtle Shelmet: ${GRAY}${lootTracker["items"][""]}
${DARK_GREEN}${BOLD}Crochet Tiger Plushies: ${GRAY}${lootTracker["items"][""]}
${DARK_GREEN}${BOLD}Antique Remedies: ${GRAY}${lootTracker["items"][""]}
${GOLD}${BOLD}Coins: ${GRAY}${lootTracker["items"]["coins"]}
${GRAY}${BOLD}Total Mobs: ${GRAY}${lootTracker["items"]["ROTTEN_FLESH"]}
`
    }
}

// ${GRAY}${BOLD}Enchanted Gold: ${GRAY}${lootTracker["items"]["ENCHANTED_GOLD"]}
// ${GRAY}${BOLD}Enchanted Iron: ${GRAY}${lootTracker["items"]["ENCHANTED_IRON"]}
// ${GRAY}${BOLD}Enchanted Ancient Claw: ${GRAY}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
// ${GRAY}${BOLD}Ancient Claw: ${GRAY}${lootTracker["items"]["ANCIENT_CLAW"]}