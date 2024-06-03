import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { state, playerHasSpade } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { getDateMayorElected } from "../../utils/mayor";
import { UIWrappedText } from "../../../Elementa";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";

registerWhen(register("entityDeath", (entity) => {
    let dist = entity.distanceTo(Player.getPlayer());
    if (dist < 30 ) {
        state.entityDeathOccurred = true;
        setTimeout(() => {
            state.entityDeathOccurred = false;
        }, 2000);
    }
}), () => getWorld() === "Hub" && settings.dianaLootTracker);

let dianaMobOverlayObj = newOverlay("dianaMobTracker", "dianaMobTracker", "dianaMobTrackerExample", "render", "MobLoc");
let dianaMobOverlay = dianaMobOverlayObj.overlay;

let dianaLootOverlayObj = newOverlay("dianaLootTracker", "dianaLootTracker", "dianaLootTrackerExample", "render", "LootLoc");
let dianaLootOverlay = dianaLootOverlayObj.overlay;

// let dianaStatsOverlayObj = newOverlay("dianaStats", "dianaStats", "dianaStatsExample", "render", "StatsLoc");
// let dianaStatsOverlay = dianaStatsOverlayObj.overlay;


let dianaMobTrackerText = new UIWrappedText("");
let dianaLootTrackerText = new UIWrappedText("");
// let dianaStatsText = new UIWrappedText("");


// export function statsOverlay(mobTracker, lootTracker, percentDict) {
//     if(getGuiOpen()) return;
//     if (!dianaStatsOverlay.children.includes(dianaStatsText)) {
//         dianaStatsOverlay.clearChildren();
//         dianaStatsOverlay.addChild(dianaStatsText);
//     }
// }

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
    if (setting == 2) {
        mobTracker = mobTracker[getDateMayorElected().getFullYear()] 
    }
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
    if (lootViewSetting == 2) {
        lootTracker = lootTracker[getDateMayorElected().getFullYear()] 
    }
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
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${BOLD}${lootTrackerType}${GRAY})
`;
    if (mobSetting) {
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY})
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY})
`
    }
    else {
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]}
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]}
`
    }
    lootMessage += `${GRAY}- ${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]}
${GRAY}- ${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${GRAY}- ${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${GRAY}- ${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${GRAY}- ${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
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
    lootMessage += `${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ANCIENT_CLAW"]}
${GRAY}- ${BLUE}${BOLD}Enchanted Ancient Claws: ${AQUA}${BOLD}${lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]}
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
    }
    else {
        dianaMobOverlayObj.renderGui = false;
        dianaLootOverlayObj.renderGui = false;
    }
}).setFps(1), () => settings.dianaMobTracker || settings.dianaLootTracker);