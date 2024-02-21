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
fileLocation = "config/ChatTriggers/modules/SBO/guiSettings.json";
function loadGuiSettings() {
    let loadedSettings = {};
    try {
        loadedSettings = JSON.parse(FileLib.read(fileLocation)) || {};
    } catch (e) {
        loadedSettings = {
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
        saveGuiSettings(loadedSettings);
    }
    return loadedSettings;
}
function saveGuiSettings(guiSettings) {
    FileLib.write(fileLocation, JSON.stringify(guiSettings));
}
guiSettings = loadGuiSettings();


const DianaMobTracker = new Overlay("dianaMobTrackerView",["Hub"], [guiSettings["MobLoc"]["x"], guiSettings["MobLoc"]["y"], guiSettings["MobLoc"]["s"]],"moveMobCounter",dianaMobTrackerExample,"dianaMobTracker");
const DianaLootTracker = new Overlay("dianaLootTrackerView",["Hub"], [guiSettings["LootLoc"]["x"],guiSettings["LootLoc"]["y"],guiSettings["LootLoc"]["s"]],"moveLootCounter",dianaLootTrackerExample,"dianaLootTracker");


/**
 * 
 * @param {string} setting 
 */
export function mobOverlay(mobTracker, setting, percentDict) {
    guiSettings["MobLoc"]["x"] = DianaMobTracker.X;
    guiSettings["MobLoc"]["y"] = DianaMobTracker.Y;
    guiSettings["MobLoc"]["s"] = DianaMobTracker.S;
    saveGuiSettings(guiSettings);
    if (setting == 2) {
        mobTracker = mobTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
    DianaMobTracker.message =
    `${YELLOW}${BOLD}Diana Mob Tracker
------------------
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Inquisitor"]} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Champion: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Champion"]} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})
${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}${mobTracker["mobs"]["Minotaur"]} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})
${GREEN}${BOLD}Gaia Construct: ${AQUA}${BOLD}${mobTracker["mobs"]["Gaia Construct"]} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})
${GREEN}${BOLD}Siamese Lynx: ${AQUA}${BOLD}${mobTracker["mobs"]["Siamese Lynx"]} ${GRAY}(${AQUA}${percentDict["Siamese Lynx"]}%${GRAY})
${GREEN}${BOLD}Minos Hunter: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Hunter"]} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})
${GRAY}${BOLD}Total Mobs: ${AQUA}${BOLD}${mobTracker["mobs"]["TotalMobs"]}
`
    }
}
/**
 * 
 * @param {string} setting 
 */
export function itemOverlay(lootTracker, setting, percentDict){
    guiSettings["LootLoc"]["x"] = DianaLootTracker.X;
    guiSettings["LootLoc"]["y"] = DianaLootTracker.Y;
    guiSettings["LootLoc"]["s"] = DianaLootTracker.S;
    saveGuiSettings(guiSettings);
    if (setting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
    DianaLootTracker.message =
    `${YELLOW}${BOLD}Diana Loot Tracker
-------------------
${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
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