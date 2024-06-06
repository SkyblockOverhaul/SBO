import settings from "../../settings";
import { registerWhen, data } from "../../utils/variables";
import { playerHasSpade } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { UIWrappedText } from "../../../Elementa";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";



let dianaMobOverlayObj = newOverlay("dianaMobTracker", "dianaMobTracker", "dianaMobTrackerExample", "render", "MobLoc");
let dianaMobOverlay = dianaMobOverlayObj.overlay;

let dianaLootOverlayObj = newOverlay("dianaLootTracker", "dianaLootTracker", "dianaLootTrackerExample", "render", "LootLoc");
let dianaLootOverlay = dianaLootOverlayObj.overlay;

let dianaStatsOverlayObj = newOverlay("dianaStats", "dianaStatsTracker", "dianaStatsExample", "render", "StatsLoc");
let dianaStatsOverlay = dianaStatsOverlayObj.overlay;


let dianaMobTrackerText = new UIWrappedText("");
let dianaLootTrackerText = new UIWrappedText("");
let dianaStatsText = new UIWrappedText("");

export function statsOverlay() {
    if(getGuiOpen()) return;
    if (!dianaStatsOverlay.children.includes(dianaStatsText)) {
        dianaStatsOverlay.clearChildren();
        dianaStatsOverlay.addChild(dianaStatsText);
    }
    let message = `${YELLOW}${BOLD}Diana Stats Tracker
${GRAY}- ${LIGHT_PURPLE}${BOLD}Mobs since Inq: ${AQUA}${BOLD}${data.mobsSinceInq}
${GRAY}- ${LIGHT_PURPLE}${BOLD}Inqs since Chimera: ${AQUA}${BOLD}${data.inqsSinceChim}
${GRAY}- ${GOLD}${BOLD}Minos since Stick: ${AQUA}${BOLD}${data.minotaursSinceStick}
${GRAY}- ${DARK_PURPLE}${BOLD}Champs since Relic: ${AQUA}${BOLD}${data.champsSinceRelic}
`
    dianaStatsText.setText(message);
    dianaStatsText.setTextScale((dianaStatsOverlayObj.scale).pixels());
}

/**
 * 
 * @param {string} setting 
 */
export function mobOverlay(mobTracker, setting, percentDict) {
    if(getGuiOpen()) return;
    if (!dianaMobOverlay.children.includes(dianaMobTrackerText)) {
        dianaMobOverlay.clearChildren();
        dianaMobOverlay.addChild(dianaMobTrackerText);
    }
    let message = "";
    if (setting > 0) {
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
    message =
    `${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${BOLD}${mobTrackerType}${GRAY})
${GRAY}- ${LIGHT_PURPLE}${BOLD}Minos Inquisitor: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Inquisitor"]} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY})
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Champion: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Champion"]} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})
${GRAY}- ${GOLD}${BOLD}Minotaur: ${AQUA}${BOLD}${mobTracker["mobs"]["Minotaur"]} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})
${GRAY}- ${GREEN}${BOLD}Gaia Construct: ${AQUA}${BOLD}${mobTracker["mobs"]["Gaia Construct"]} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})
${GRAY}- ${GREEN}${BOLD}Siamese Lynx: ${AQUA}${BOLD}${mobTracker["mobs"]["Siamese Lynxes"]} ${GRAY}(${AQUA}${percentDict["Siamese Lynxes"]}%${GRAY})
${GRAY}- ${GREEN}${BOLD}Minos Hunter: ${AQUA}${BOLD}${mobTracker["mobs"]["Minos Hunter"]} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})
${GRAY}- ${GRAY}${BOLD}Total Mobs: ${AQUA}${BOLD}${mobTracker["mobs"]["TotalMobs"]}
`
    }
    dianaMobTrackerText.setText(message);
    dianaMobTrackerText.setTextScale((dianaMobOverlayObj.scale).pixels());
}
let mobTrackerType = undefined;
let lootTrackerType = undefined;
/**
 * 
 * @param {string} setting 
 */
export function itemOverlay(lootTracker, lootViewSetting, percentDict){
    if(getGuiOpen()) return;
    if (!dianaLootOverlay.children.includes(dianaLootTrackerText)) {
        dianaLootOverlay.clearChildren();
        dianaLootOverlay.addChild(dianaLootTrackerText);    
    }
    let message = "";
    if (lootViewSetting > 0) {
        message = getLootMessage(lootTracker, lootViewSetting, settings.dianaMobTracker, percentDict);
    }
    dianaLootTrackerText.setText(message);
    dianaLootTrackerText.setTextScale((dianaLootOverlayObj.scale).pixels());
}

function getLootMessage(lootTracker, lootViewSetting, mobSetting, percentDict) {
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
    let totalChimera = 0;
    if (lootTracker["items"]["Chimera"] != undefined) {
        totalChimera += lootTracker["items"]["Chimera"];
    }
    if (lootTracker["items"]["ChimeraLs"] != undefined) {
        totalChimera += lootTracker["items"]["ChimeraLs"];
    }
    
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${BOLD}${lootTrackerType}${GRAY})
`;
    if (mobSetting) {
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY}) [${AQUA}LS${GRAY}: ${AQUA}${BOLD}${lootTracker["items"]["ChimeraLs"]}${GRAY}]  
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
`
    }
    else {
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY}) [${AQUA}LS${GRAY}: ${AQUA}${BOLD}${lootTracker["items"]["ChimeraLs"]}${GRAY}]
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]}
`
    }
    lootMessage += `${GRAY}- ${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GRAY}- ${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${GRAY}- ${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${lootTracker["items"]["Griffin Feather"]}
`
    if (lootTracker["items"]["coins"] > 1000000) {
        lootMessage += `${GRAY}- ${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${(lootTracker["items"]["coins"]/1000000).toFixed(2)}M
`
    }
    else if (lootTracker["items"]["coins"] > 1000) {
        lootMessage += `${GRAY}- ${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${Math.round(lootTracker["items"]["coins"]/1000)}K
`
    }
    else {
        lootMessage += `${GRAY}- ${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${lootTracker["items"]["coins"]}
`
    }
    lootMessage += `${GRAY}- ${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${GRAY}- ${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${GRAY}- ${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
`
    if (lootTracker["items"]["ANCIENT_CLAW"] > 1000000) {
    lootMessage += `${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${Math.round((lootTracker["items"]["ANCIENT_CLAW"]/1000000).toFixed(2))}M
`
    }
    else if (lootTracker["items"]["ANCIENT_CLAW"] > 1000) {
        lootMessage += `${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${Math.round((lootTracker["items"]["ANCIENT_CLAW"]/1000).toFixed(2))}K
`
    }
    else {
        lootMessage += `${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ANCIENT_CLAW"]}
`
    }
    lootMessage += `${GRAY}- ${BLUE}${BOLD}Enchanted Claws: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
${GRAY}- ${BLUE}${BOLD}Enchanted Gold: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_GOLD"]}
${GRAY}- ${BLUE}${BOLD}Enchanted Iron: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_IRON"]}
${GRAY}- ${GRAY}${BOLD}Total Burrows: ${AQUA}${BOLD}${lootTracker["items"]["Total Burrows"]}
`
    return lootMessage;
}

let mythosHpOverlayObj = newOverlay("mythosMobHp", "mythosMobHp", "mythosMobHpExample", "render", "MythosHpLoc");
let mythosHpOverlay = mythosHpOverlayObj.overlay

let mythosMobHpText = new UIWrappedText("");

export function mythosMobHpOverlay(mobNamesWithHp) {
    // if (!renderGui) {
    //     mythosHpOverlayObj.renderGui = false;
    //     return;
    // }
    // else {
    //     mythosHpOverlayObj.renderGui = true;
    // }
    if(getGuiOpen()) return
    if(!mythosHpOverlay.children.includes(mythosMobHpText)) {
        mythosHpOverlay.clearChildren();
        mythosHpOverlay.addChild(mythosMobHpText);
    }
    let message = "";
    if (mobNamesWithHp.length > 0) {
        message = "";
        mobNamesWithHp.forEach((mob) => {
            message += `${mob}\n`;
        });
    }
    else {
        message = "";
    }
    mythosMobHpText.setText(message);
    mythosMobHpText.setTextScale((mythosHpOverlayObj.scale).pixels());
}

registerWhen(register("step", () => {
    if (playerHasSpade() || checkDiana()) {
        dianaMobOverlayObj.renderGui = true;
        dianaLootOverlayObj.renderGui = true;
        dianaStatsOverlayObj.renderGui = true;
    }
    else {
        dianaMobOverlayObj.renderGui = false;
        dianaLootOverlayObj.renderGui = false;
        dianaStatsOverlayObj.renderGui = false;
    }
}).setFps(1), () => settings.dianaMobTracker || settings.dianaLootTracker || settings.dianaStatsTracker);