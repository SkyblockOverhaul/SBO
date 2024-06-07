import settings from "../../settings";
import { registerWhen, data } from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana, formatNumber } from "../../utils/functions";
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

// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell

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
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} ${GRAY}(${AQUA}${percentDict["Chimera"]}%${GRAY}) [${AQUA}LS${GRAY}: ${AQUA}${BOLD}${lootTracker["items"]["ChimeraLs"]}${GRAY}] ${GOLD}${formatNumber(getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera)}
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]} ${GRAY}(${AQUA}${percentDict["Minos Relic"]}%${GRAY})
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GRAY}(${AQUA}${percentDict["Daedalus Stick"]}%${GRAY}) ${GOLD}${formatNumber(getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"])}
`
    }
    else {
        lootMessage += `${GRAY}- ${LIGHT_PURPLE}${BOLD}Chimera: ${AQUA}${BOLD}${lootTracker["items"]["Chimera"]} [${AQUA}LS${GRAY}: ${AQUA}${BOLD}${lootTracker["items"]["ChimeraLs"]}${GRAY}] ${GOLD}${formatNumber(getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera)}
${GRAY}- ${DARK_PURPLE}${BOLD}Minos Relic: ${AQUA}${BOLD}${lootTracker["items"]["MINOS_RELIC"]}
${GRAY}- ${GOLD}${BOLD}Daedalus Stick: ${AQUA}${BOLD}${lootTracker["items"]["Daedalus Stick"]} ${GOLD}${formatNumber(getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"])}
`
    }
    lootMessage += `${GRAY}- ${GOLD}${BOLD}Crown of Greed: ${AQUA}${BOLD}${lootTracker["items"]["Crown of Greed"]} 
${GRAY}- ${GOLD}${BOLD}Souvenir: ${AQUA}${BOLD}${lootTracker["items"]["Washed-up Souvenir"]}
${GRAY}- ${GOLD}${BOLD}Griffin Feather: ${AQUA}${BOLD}${formatNumber(lootTracker["items"]["Griffin Feather"])} ${GOLD}${formatNumber(getBazaarPriceDiana("GRIFFIN_FEATHER") * lootTracker["items"]["Griffin Feather"])}
`
    lootMessage += `${GRAY}- ${GOLD}${BOLD}Coins: ${AQUA}${BOLD}${(formatNumber(lootTracker["items"]["coins"]))}
`
    lootMessage += `${GRAY}- ${DARK_GREEN}${BOLD}Turtle Shelmet: ${AQUA}${BOLD}${lootTracker["items"]["DWARF_TURTLE_SHELMET"]}
${GRAY}- ${DARK_GREEN}${BOLD}Tiger Plushie: ${AQUA}${BOLD}${lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]}
${GRAY}- ${DARK_GREEN}${BOLD}Antique Remedies: ${AQUA}${BOLD}${lootTracker["items"]["ANTIQUE_REMEDIES"]}
`

    lootMessage += `${GRAY}- ${BLUE}${BOLD}Ancient Claws: ${AQUA}${BOLD}${formatNumber(lootTracker["items"]["ANCIENT_CLAW"])} ${GOLD}${formatNumber(getBazaarPriceDiana("ANCIENT_CLAW") * lootTracker["items"]["ANCIENT_CLAW"])}
`

    lootMessage += `${GRAY}- ${BLUE}${BOLD}Enchanted Claws: ${AQUA}${BOLD}${formatNumber(lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"])} ${GOLD}${formatNumber(getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"])}
${GRAY}- ${BLUE}${BOLD}Enchanted Gold: ${AQUA}${BOLD}${formatNumber(lootTracker["items"]["ENCHANTED_GOLD"])} ${GOLD}${formatNumber(getBazaarPriceDiana("ENCHANTED_GOLD") * lootTracker["items"]["ENCHANTED_GOLD"])}
${GRAY}- ${BLUE}${BOLD}Enchanted Iron: ${AQUA}${BOLD}${formatNumber(lootTracker["items"]["ENCHANTED_IRON"])} ${GOLD}${formatNumber(getBazaarPriceDiana("ENCHANTED_IRON") * lootTracker["items"]["ENCHANTED_IRON"])}
${GRAY}- ${GRAY}${BOLD}Total Burrows: ${AQUA}${BOLD}${lootTracker["items"]["Total Burrows"]}
`
    let totalValue = 0;
    totalValue += getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera;
    totalValue += getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"];
    totalValue += getBazaarPriceDiana("GRIFFIN_FEATHER") * lootTracker["items"]["Griffin Feather"];
    totalValue += getBazaarPriceDiana("ANCIENT_CLAW") * lootTracker["items"]["ANCIENT_CLAW"];
    totalValue += getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"];
    totalValue += getBazaarPriceDiana("ENCHANTED_GOLD") * lootTracker["items"]["ENCHANTED_GOLD"];
    totalValue += getBazaarPriceDiana("ENCHANTED_IRON") * lootTracker["items"]["ENCHANTED_IRON"];
    totalValue += lootTracker["items"]["coins"];
    lootMessage += `${GRAY}- ${GOLD}${BOLD}Total Profit: ${GOLD}${formatNumber(totalValue)}
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