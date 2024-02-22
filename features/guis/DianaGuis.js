import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { state, loadGuiSettings, saveGuiSettings } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { getDateMayorElected } from "../../utils/mayor";

registerWhen(register("entityDeath", (entity) => {
    var dist = entity.distanceTo(Player.getPlayer());
    if (dist < 31 ) {
        state.entityDeathOccurred = true;
        setTimeout(() => {
            state.entityDeathOccurred = false;
        }, 2000);
    }
    }), () => getWorld() === "Hub" && settings.dianaLootTracker);

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
${GOLD}${BOLD}Crown of Greed: ${WHITE}
${GOLD}${BOLD}Souvenir: ${WHITE}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${WHITE}
${DARK_GREEN}${BOLD}Tiger Plushie: ${WHITE}
${DARK_GREEN}${BOLD}Antique Remedies: ${WHITE}
${BLUE}${BOLD}Ancient Claws: ${WHITE}
${BLUE}${BOLD}Enchanted Ancient Claws: ${WHITE}
${GOLD}${BOLD}Griffin Feather: ${WHITE}
${GOLD}${BOLD}Coins: ${WHITE}
${GRAY}${BOLD}Total Burrows: ${WHITE}
`

// ${GRAY}${BOLD}Enchanted Gold: 
// ${GRAY}${BOLD}Enchanted Iron: 
// ${GRAY}${BOLD}Enchanted Ancient Claw: 
// ${GRAY}${BOLD}Ancient Claw: 

guiSettings = loadGuiSettings();

let DianaMobTracker = new Overlay("dianaMobTrackerView",["Hub"], [10, 10, 0],"sbomoveMobCounter",dianaMobTrackerExample,"dianaMobTracker");
let DianaLootTracker = new Overlay("dianaLootTrackerView",["Hub"], [10, 10, 0],"sbomoveLootCounter",dianaLootTrackerExample,"dianaLootTracker");


let mobSettingsLoad = false;
/**
 * 
 * @param {string} setting 
 */
export function mobOverlay(mobTracker, setting, percentDict) {
    if (!mobSettingsLoad) {
        if(guiSettings != undefined) {
            DianaMobTracker.setX(guiSettings["MobLoc"]["x"]);
            DianaMobTracker.setY(guiSettings["MobLoc"]["y"]);
            DianaMobTracker.setScale(guiSettings["MobLoc"]["s"]);
            mobSettingsLoad = true;
        }
    }
    if( guiSettings["MobLoc"]["x"] != DianaMobTracker.X || guiSettings["MobLoc"]["y"] != DianaMobTracker.Y || guiSettings["MobLoc"]["s"] != DianaMobTracker.S)
    {
        guiSettings["MobLoc"]["x"] = DianaMobTracker.X;
        guiSettings["MobLoc"]["y"] = DianaMobTracker.Y;
        guiSettings["MobLoc"]["s"] = DianaMobTracker.S;
        saveGuiSettings(guiSettings);
    }
    if (setting == 2) {
        mobTracker = mobTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
        switch (setting) {
            case 1:
                mobTrackerType = "Total";
                break;
            case 2:
                mobTrackerType = "Event";
                break;
            case 3:
                mobTrackerType = "Session";
                break;
        };
    DianaMobTracker.message =
    `${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${BOLD}${mobTrackerType}${GRAY})
------------------
${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Inquisitor"]} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Champion: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Champion"]} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})
${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}${mobTracker["mobs"]["Minotaur"]} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})
${GREEN}${BOLD}Gaia Construct: ${AQUA}${BOLD}${mobTracker["mobs"]["Gaia Construct"]} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})
${GREEN}${BOLD}Siamese Lynx: ${AQUA}${BOLD}${mobTracker["mobs"]["Siamese Lynxes"]} ${GRAY}(${AQUA}${percentDict["Siamese Lynxes"]}%${GRAY})
${GREEN}${BOLD}Minos Hunter: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Hunter"]} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})
${GRAY}${BOLD}Total Mobs: ${AQUA}${BOLD}${mobTracker["mobs"]["TotalMobs"]}
`
    }
}

let lootSettingsLoad = false;
let mobTrackerType = undefined;
let lootTrackerType = undefined;
/**
 * 
 * @param {string} setting 
 */
export function itemOverlay(lootTracker, setting, percentDict){
    if (!lootSettingsLoad) {
        if(guiSettings != undefined) {
            DianaLootTracker.setX(guiSettings["LootLoc"]["x"]);
            DianaLootTracker.setY(guiSettings["LootLoc"]["y"]);
            DianaLootTracker.setScale(guiSettings["LootLoc"]["s"]);
            lootSettingsLoad = true;
        }
    }
    if( guiSettings["LootLoc"]["x"] != DianaLootTracker.X || guiSettings["LootLoc"]["y"] != DianaLootTracker.Y || guiSettings["LootLoc"]["s"] != DianaLootTracker.S)
    {
        guiSettings["LootLoc"]["x"] = DianaLootTracker.X;
        guiSettings["LootLoc"]["y"] = DianaLootTracker.Y;
        guiSettings["LootLoc"]["s"] = DianaLootTracker.S;
        saveGuiSettings(guiSettings);
    }
    if (setting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if(setting > 0){
        switch (setting) {
            case 1:
                lootTrackerType = "Total";
                break;
            case 2:
                lootTrackerType = "Event";
                break;
            case 3:
                lootTrackerType = "Session";
                break;
        };
    DianaLootTracker.message =
    `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${BOLD}${lootTrackerType}${GRAY})
-------------------
${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${lootTracker["items"]["Griffin Feather"]}
${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${lootTracker["items"]["coins"]}
${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ANCIENT_CLAW"]}
${BLUE}${BOLD}Enchanted Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
${GRAY}${BOLD}Total Burrows: ${AQUA}${BOLD}${lootTracker["items"]["Total Burrows"]}
`
    }
}

// ${GRAY}${BOLD}Enchanted Gold: ${GRAY}${lootTracker["items"]["ENCHANTED_GOLD"]}
// ${GRAY}${BOLD}Enchanted Iron: ${GRAY}${lootTracker["items"]["ENCHANTED_IRON"]}
// ${GRAY}${BOLD}Enchanted Ancient Claw: ${GRAY}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
// ${GRAY}${BOLD}Ancient Claw: ${GRAY}${lootTracker["items"]["ANCIENT_CLAW"]}