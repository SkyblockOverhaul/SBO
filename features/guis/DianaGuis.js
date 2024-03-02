import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { state, loadGuiSettings, saveGuiSettings, playerHasSpade } from "../../utils/functions";
import { Overlay } from "../../utils/Overlay";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { getDateMayorElected } from "../../utils/mayor";

registerWhen(register("entityDeath", (entity) => {
    var dist = entity.distanceTo(Player.getPlayer());
    if (dist < 10 ) {
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
    if (guiSettings["MobLoc"]["x"] != DianaMobTracker.X || guiSettings["MobLoc"]["y"] != DianaMobTracker.Y || guiSettings["MobLoc"]["s"] != DianaMobTracker.S) {
        if (!dontSaveSettings) {
            guiSettings["MobLoc"]["x"] = DianaMobTracker.X;
            guiSettings["MobLoc"]["y"] = DianaMobTracker.Y;
            guiSettings["MobLoc"]["s"] = DianaMobTracker.S;
            saveGuiSettings(guiSettings);
        }
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
export function itemOverlay(lootTracker, lootViewSetting, percentDict){
    if (!lootSettingsLoad) {
        if(guiSettings != undefined) {
            DianaLootTracker.setX(guiSettings["LootLoc"]["x"]);
            DianaLootTracker.setY(guiSettings["LootLoc"]["y"]);
            DianaLootTracker.setScale(guiSettings["LootLoc"]["s"]);
            lootSettingsLoad = true;
        }
    }
    if (guiSettings["LootLoc"]["x"] != DianaLootTracker.X || guiSettings["LootLoc"]["y"] != DianaLootTracker.Y || guiSettings["LootLoc"]["s"] != DianaLootTracker.S) {
        if (!dontSaveSettings) {
            guiSettings["LootLoc"]["x"] = DianaLootTracker.X;
            guiSettings["LootLoc"]["y"] = DianaLootTracker.Y;
            guiSettings["LootLoc"]["s"] = DianaLootTracker.S;
            saveGuiSettings(guiSettings);
        }
    }
    if (lootViewSetting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
    if(lootViewSetting > 0){
        DianaLootTracker.message = getLootMessage(lootTracker, lootViewSetting, settings.dianaMobTracker, percentDict);
    }
}

function getLootMessage(lootTracker, lootViewSetting, mobSetting, percentDict) {
    let lootTrackerType = "";
    switch (lootViewSetting) {
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
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${BOLD}${lootTrackerType}${GRAY})
-------------------
`;
    if (mobSetting) {
        lootMessage += `${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY})
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
`
    }
    else {
        lootMessage += `${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]}
${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]}
`
    }
    lootMessage += `${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${lootTracker["items"]["Griffin Feather"]}
`
    if (lootTracker["items"]["coins"] > 1000000) {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${(lootTracker["items"]["coins"]/1000000).toFixed(2)}M
`
    }
    else if (lootTracker["items"]["coins"] > 1000) {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${Math.round(lootTracker["items"]["coins"]/1000)}K
`
    }
    else {
        lootMessage += `${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${lootTracker["items"]["coins"]}
`
    }
    lootMessage += `${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ANCIENT_CLAW"]}
${BLUE}${BOLD}Enchanted Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
${GRAY}${BOLD}Total Burrows: ${AQUA}${BOLD}${lootTracker["items"]["Total Burrows"]}
`
    return lootMessage;
}

// ${GRAY}${BOLD}Enchanted Gold: ${GRAY}${lootTracker["items"]["ENCHANTED_GOLD"]}
// ${GRAY}${BOLD}Enchanted Iron: ${GRAY}${lootTracker["items"]["ENCHANTED_IRON"]}
// ${GRAY}${BOLD}Enchanted Ancient Claw: ${GRAY}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
// ${GRAY}${BOLD}Ancient Claw: ${GRAY}${lootTracker["items"]["ANCIENT_CLAW"]}

mythonsMobHpExample = 
`&8[&7Lv750&8] &2Exalted Minos Inquisitor &a40M&f/&a40M`

let MythosMobHp = new Overlay("mythosMobHp",["Hub"], [10, 10, 0],"sbomoveMythosHp",mythonsMobHpExample,"mythosMobHp");

let mythosMobHpSettingsLoad = false;
export function mythosMobHpOverlay(mobNamesWithHp) {
    if (!mythosMobHpSettingsLoad) {
        if(guiSettings != undefined) {
            MythosMobHp.setX(guiSettings["MythosHpLoc"]["x"]);
            MythosMobHp.setY(guiSettings["MythosHpLoc"]["y"]);
            MythosMobHp.setScale(guiSettings["MythosHpLoc"]["s"]);
            mythosMobHpSettingsLoad = true;
        }
    }
    if (guiSettings["MythosHpLoc"]["x"] != MythosMobHp.X || guiSettings["MythosHpLoc"]["y"] != MythosMobHp.Y || guiSettings["MythosHpLoc"]["s"] != MythosMobHp.S) {
        guiSettings["MythosHpLoc"]["x"] = MythosMobHp.X;
        guiSettings["MythosHpLoc"]["y"] = MythosMobHp.Y;
        guiSettings["MythosHpLoc"]["s"] = MythosMobHp.S;
        saveGuiSettings(guiSettings);
    }
    if (mobNamesWithHp.length > 0) {
        MythosMobHp.message = "";
        mobNamesWithHp.forEach((mob) => {
            MythosMobHp.message += `${mob}\n`;
        });
    }
    else {
        MythosMobHp.message = "";
    }
}


let effectsGuiExample = 
`${YELLOW}${BOLD}Active Effects
-------------------
${AQUA}${BOLD}Wisp's Water: ${WHITE}2520s`

let EffectsGui = new Overlay("effectsGui",["Crimson Isle"], [10, 10, 1],"sbomoveEffects",effectsGuiExample,"effectsGui");

let effectsSettingsLoad = false;
export function effectsOverlay(effects) {
    if (!effectsSettingsLoad) {
        if(guiSettings != undefined) {
            EffectsGui.setX(guiSettings["EffectsLoc"]["x"]);
            EffectsGui.setY(guiSettings["EffectsLoc"]["y"]);
            EffectsGui.setScale(guiSettings["EffectsLoc"]["s"]);
            effectsSettingsLoad = true;
        }
    }
    if (guiSettings["EffectsLoc"]["x"] != EffectsGui.X || guiSettings["EffectsLoc"]["y"] != EffectsGui.Y || guiSettings["EffectsLoc"]["s"] != EffectsGui.S) {
        guiSettings["EffectsLoc"]["x"] = EffectsGui.X;
        guiSettings["EffectsLoc"]["y"] = EffectsGui.Y;
        guiSettings["EffectsLoc"]["s"] = EffectsGui.S;
        saveGuiSettings(guiSettings);
    }
    if (effects.length > 0) {
        EffectsGui.message = `${YELLOW}${BOLD}Active Effects
---------------
`;
        // add to message each effect and duration and if duration is over 60s convert to minutes and if over 3600s convert to hours
        effects.forEach((effect) => {
            let duration = effect.duration;
            let durationMessage = "";
            if (duration > 3600) {
                durationMessage = `${Math.floor(duration/3600)}h `;
                duration = duration % 3600;
            }
            if (duration > 60) {
                durationMessage += `${Math.floor(duration/60)}m `;
                duration = duration % 60;
            }
            if (duration > 0) {
                durationMessage += `${Math.floor(duration)}s`;
            }
            EffectsGui.message += `${AQUA}${BOLD}${effect.name}: ${WHITE}${durationMessage}\n`;
        });
    }
    else {
        EffectsGui.message = "";
    }
}

let dontSaveSettings = false;
register("step", () => {
    if (playerHasSpade() && (DianaMobTracker.S == 0 || DianaLootTracker.S == 0)) {
        DianaMobTracker.setLoc(guiSettings["MobLoc"]["x"], guiSettings["MobLoc"]["y"], guiSettings["MobLoc"]["s"]);
        DianaLootTracker.setLoc(guiSettings["LootLoc"]["x"], guiSettings["LootLoc"]["y"], guiSettings["LootLoc"]["s"]);
    }
    else if (!playerHasSpade() && (DianaMobTracker.S != 0 || DianaLootTracker.S != 0)) {
        DianaMobTracker.setScale(0);
        DianaLootTracker.setScale(0);
        dontSaveSettings = true;
    }

}).setFps(1);

