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
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${WHITE}
${DARK_PURPLE}${BOLD}Minos Champion: ${WHITE}
${GOLD}${BOLD}Minotaur: ${WHITE}
${GREEN}${BOLD}Gaia Construct: ${WHITE}
${GREEN}${BOLD}Siamese Lynx: ${WHITE}
${GREEN}${BOLD}Minos Hunter: ${WHITE}
${GRAY}${BOLD}Total Mobs: ${WHITE}
`
dianaLootTrackerExample = 
`${YELLOW}${BOLD}Diana Loot Tracker
-------------------
${LIGHT_PURPLE}${BOLD}Chimera: ${WHITE}
${DARK_PURPLE}${BOLD}Minos Relic: ${WHITE}
${GOLD}${BOLD}Daedalus Stick: ${WHITE}
${GOLD}${BOLD}Griffin Feather: ${WHITE}
${GOLD}${BOLD}Crown of Greed: ${WHITE}
${GOLD}${BOLD}Souvenir: ${WHITE}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${WHITE}
${DARK_GREEN}${BOLD}Tiger Plushie: ${WHITE}
${DARK_GREEN}${BOLD}Antique Remedies: ${WHITE}
${GOLD}${BOLD}Coins: ${WHITE}
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
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Inquisitor"]}
${DARK_PURPLE}${BOLD}Minos Champion: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Champion"]}
${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}${mobTracker["mobs"]["Minotaur"]}
${GREEN}${BOLD}Gaia Construct: ${AQUA}${BOLD}${mobTracker["mobs"]["Gaia Construct"]}
${GREEN}${BOLD}Siamese Lynx: ${AQUA}${BOLD}${mobTracker["mobs"]["Siamese Lynx"]}
${GREEN}${BOLD}Minos Hunter: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Hunter"]}
${GRAY}${BOLD}Total Mobs: ${AQUA}${BOLD}${mobTracker["mobs"]["TotalMobs"]}
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
${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]}
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]}
${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${lootTracker["items"]["Griffin Feather"]}
${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${lootTracker["items"]["coins"]}
${GRAY}${BOLD}Rotten Flesh: ${AQUA}${BOLD}${lootTracker["items"]["ROTTEN_FLESH"]}
`
    }
}

// ${GRAY}${BOLD}Enchanted Gold: ${GRAY}${lootTracker["items"]["ENCHANTED_GOLD"]}
// ${GRAY}${BOLD}Enchanted Iron: ${GRAY}${lootTracker["items"]["ENCHANTED_IRON"]}
// ${GRAY}${BOLD}Enchanted Ancient Claw: ${GRAY}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
// ${GRAY}${BOLD}Ancient Claw: ${GRAY}${lootTracker["items"]["ANCIENT_CLAW"]}