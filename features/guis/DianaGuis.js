import settings from "../../settings";
import { registerWhen, data } from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana, formatNumber, getDianaAhPrice } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE} from "../../utils/constants";
import { UIWrappedText } from "../../../Elementa";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";



let dianaMobOverlayObj = newOverlay("dianaMobTracker", "dianaTracker", "dianaMobTrackerExample", "render", "MobLoc");
let dianaMobOverlay = dianaMobOverlayObj.overlay;

let dianaLootOverlayObj = newOverlay("dianaLootTracker", "dianaTracker", "dianaLootTrackerExample", "render", "LootLoc");
let dianaLootOverlay = dianaLootOverlayObj.overlay;

let dianaStatsOverlayObj = newOverlay("dianaStats", "dianaStatsTracker", "dianaStatsExample", "render", "StatsLoc");
let dianaStatsOverlay = dianaStatsOverlayObj.overlay;

let dianaAvgMagicFindOverlayObj = newOverlay("dianaAvgMagicFind", "dianaAvgMagicFind", "dianaAvgMagicFindExample", "render", "AvgMagicFindLoc");
let dianaAvgMagicFindOverlay = dianaAvgMagicFindOverlayObj.overlay;


let dianaMobTrackerText = new UIWrappedText("");
let dianaLootTrackerText = new UIWrappedText("");
let dianaStatsText = new UIWrappedText("");
let dianaAvgMagicFindText = new UIWrappedText("");

export function avgMagicFindOverlay() {
    if(getGuiOpen()) return;
    if (!dianaAvgMagicFindOverlay.children.includes(dianaAvgMagicFindText)) {
        dianaAvgMagicFindOverlay.clearChildren();
        dianaAvgMagicFindOverlay.addChild(dianaAvgMagicFindText);
    }
    let message = `${YELLOW}${BOLD}Diana Magic Find ${GRAY}(${YELLOW}${BOLD}Avg${GRAY})
${GRAY}- ${LIGHT_PURPLE}Chimera: ${AQUA}${data.avgChimMagicFind}%
${GRAY}- ${GOLD}Sticks: ${AQUA}${data.avgStickMagicFind}%
    `

    dianaAvgMagicFindText.setText(message);
    dianaAvgMagicFindText.setTextScale((dianaAvgMagicFindOverlayObj.scale).pixels());
}

export function statsOverlay() {
    if(getGuiOpen()) return;
    if (!dianaStatsOverlay.children.includes(dianaStatsText)) {
        dianaStatsOverlay.clearChildren();
        dianaStatsOverlay.addChild(dianaStatsText);
    }
    let message = `${YELLOW}${BOLD}Diana Stats Tracker
${GRAY}- ${LIGHT_PURPLE}Mobs since Inq: ${AQUA}${data.mobsSinceInq}
${GRAY}- ${LIGHT_PURPLE}Inqs since Chimera: ${AQUA}${data.inqsSinceChim}
${GRAY}- ${GOLD}Minos since Stick: ${AQUA}${data.minotaursSinceStick}
${GRAY}- ${DARK_PURPLE}Champs since Relic: ${AQUA}${data.champsSinceRelic}
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
    `${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${mobTrackerType}${GRAY})
${GRAY}- ${LIGHT_PURPLE}Minos Inquisitor: ${AQUA}${mobTracker["mobs"]["Minos Inquisitor"]} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY})
${GRAY}- ${DARK_PURPLE}Minos Champion: ${AQUA}${mobTracker["mobs"]["Minos Champion"]} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})
${GRAY}- ${GOLD}Minotaur: ${AQUA}${mobTracker["mobs"]["Minotaur"]} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})
${GRAY}- ${GREEN}Gaia Construct: ${AQUA}${mobTracker["mobs"]["Gaia Construct"]} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})
${GRAY}- ${GREEN}Siamese Lynx: ${AQUA}${mobTracker["mobs"]["Siamese Lynxes"]} ${GRAY}(${AQUA}${percentDict["Siamese Lynxes"]}%${GRAY})
${GRAY}- ${GREEN}Minos Hunter: ${AQUA}${mobTracker["mobs"]["Minos Hunter"]} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})
${GRAY}- ${GRAY}Total Mobs: ${AQUA}${mobTracker["mobs"]["TotalMobs"]}
`
    }
    dianaMobTrackerText.setText(message);
    dianaMobTrackerText.setTextScale((dianaMobOverlayObj.scale).pixels());
}
let mobTrackerType = undefined;
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
        message = getLootMessage(lootTracker, lootViewSetting, settings.dianaTracker, percentDict);
    }
    dianaLootTrackerText.setText(message);
    dianaLootTrackerText.setTextScale((dianaLootOverlayObj.scale).pixels());
}

// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell

function getLootMessage(lootTracker, lootViewSetting, mobSetting, percentDict) {
    const lootTrackerType = ["Total", "Event", "Session"][lootViewSetting - 1];
    let totalChimera = 0;
    for (let key of ["Chimera", "ChimeraLs"]) {
        if (lootTracker.items[key] !== undefined) {
            totalChimera += lootTracker.items[key];
        }
    }
    let relicPrice = formatNumber(getDianaAhPrice("MINOS_RELIC") * lootTracker["items"]["MINOS_RELIC"])
    let chimeraPrice = formatNumber(getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera)
    let daedalusPrice = formatNumber(getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"])
    let griffinPrice = formatNumber(getBazaarPriceDiana("GRIFFIN_FEATHER") * lootTracker["items"]["Griffin Feather"])
    let clawPrice = formatNumber(getBazaarPriceDiana("ANCIENT_CLAW") * lootTracker["items"]["ANCIENT_CLAW"])
    let echClawPrice = formatNumber(getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"])
    let goldPrice = formatNumber(getBazaarPriceDiana("ENCHANTED_GOLD") * lootTracker["items"]["ENCHANTED_GOLD"])
    let ironPrice = formatNumber(getBazaarPriceDiana("ENCHANTED_IRON") * lootTracker["items"]["ENCHANTED_IRON"])
    let dwarfPrice = formatNumber(getDianaAhPrice("DWARF_TURTLE_SHELMET") * lootTracker["items"]["DWARF_TURTLE_SHELMET"])
    let tigerPrice = formatNumber(getDianaAhPrice("CROCHET_TIGER_PLUSHIE") * lootTracker["items"]["CROCHET_TIGER_PLUSHIE"])
    let antiquePrice = formatNumber(getDianaAhPrice("ANTIQUE_REMEDIES") * lootTracker["items"]["ANTIQUE_REMEDIES"])
    let crownPrice = formatNumber(getDianaAhPrice("CROWN_OF_GREED") * lootTracker["items"]["Crown of Greed"])
    let souvenirPrice = formatNumber(getDianaAhPrice("WASHED_UP_SOUVENIR") * lootTracker["items"]["Washed-up Souvenir"])

    function getMessagePart(price, color, itemName, itemAmount, percent = '') {
        if (percent === ''){
            return `${GOLD}${price} ${GRAY}| ${color}${itemName}: ${AQUA}${itemAmount}\n`
        }
        else{
            return `${GOLD}${price} ${GRAY}| ${color}${itemName}: ${AQUA}${itemAmount} ${GRAY}(${AQUA}${percent}%${GRAY})\n`
        }
    }
    
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${lootTrackerType}${GRAY})
`;
    lootMessage += getMessagePart(chimeraPrice, LIGHT_PURPLE, "Chimera", lootTracker["items"]["Chimera"], percentDict["Chimera"]);
    lootMessage += getMessagePart(relicPrice, DARK_PURPLE, "Minos Relic", lootTracker["items"]["MINOS_RELIC"], percentDict["Minos Relic"]);
    lootMessage += getMessagePart(daedalusPrice, GOLD, "Daedalus Stick", lootTracker["items"]["Daedalus Stick"], percentDict["Daedalus Stick"]);
    lootMessage += getMessagePart(crownPrice, GOLD, "Crown of Greed", lootTracker["items"]["Crown of Greed"]);
    lootMessage += getMessagePart(souvenirPrice, GOLD, "Souvenir", lootTracker["items"]["Washed-up Souvenir"]);
    lootMessage += getMessagePart(griffinPrice, GOLD, "Griffin Feather", lootTracker["items"]["Griffin Feather"]);
    lootMessage += getMessagePart(dwarfPrice, DARK_GREEN, "Turtle Shelmet", lootTracker["items"]["DWARF_TURTLE_SHELMET"]);
    lootMessage += getMessagePart(tigerPrice, DARK_GREEN, "Tiger Plushie", lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]);
    lootMessage += getMessagePart(antiquePrice, DARK_GREEN, "Antique Remedies", lootTracker["items"]["ANTIQUE_REMEDIES"]);
    lootMessage += getMessagePart(clawPrice, BLUE, "Ancient Claws", lootTracker["items"]["ANCIENT_CLAW"]);
    lootMessage += getMessagePart(echClawPrice, BLUE, "Enchanted Claws", lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]);
    lootMessage += getMessagePart(goldPrice, BLUE, "Enchanted Gold", lootTracker["items"]["ENCHANTED_GOLD"]);
    lootMessage += getMessagePart(ironPrice, BLUE, "Enchanted Iron", lootTracker["items"]["ENCHANTED_IRON"]);
    lootMessage += `${GRAY}Total Burrows: ${AQUA}${lootTracker["items"]["Total Burrows"]}
${GOLD}Coins: ${AQUA}${formatNumber(lootTracker["items"]["coins"])}
`
    let totalValue = 0;
    totalValue += getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera;
    totalValue += getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"];
    totalValue += getBazaarPriceDiana("GRIFFIN_FEATHER") * lootTracker["items"]["Griffin Feather"];
    totalValue += getBazaarPriceDiana("ANCIENT_CLAW") * lootTracker["items"]["ANCIENT_CLAW"];
    totalValue += getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"];
    totalValue += getBazaarPriceDiana("ENCHANTED_GOLD") * lootTracker["items"]["ENCHANTED_GOLD"];
    totalValue += getBazaarPriceDiana("ENCHANTED_IRON") * lootTracker["items"]["ENCHANTED_IRON"];
    totalValue += getDianaAhPrice("DWARF_TURTLE_SHELMET") * lootTracker["items"]["DWARF_TURTLE_SHELMET"];
    totalValue += getDianaAhPrice("CROCHET_TIGER_PLUSHIE") * lootTracker["items"]["CROCHET_TIGER_PLUSHIE"];
    totalValue += getDianaAhPrice("ANTIQUE_REMEDIES") * lootTracker["items"]["ANTIQUE_REMEDIES"];
    totalValue += getDianaAhPrice("MINOS_RELIC") * lootTracker["items"]["MINOS_RELIC"];
    totalValue += getDianaAhPrice("CROWN_OF_GREED") * lootTracker["items"]["Crown of Greed"];
    totalValue += getDianaAhPrice("WASHED_UP_SOUVENIR") * lootTracker["items"]["Washed-up Souvenir"];
    totalValue += lootTracker["items"]["coins"];
    lootMessage += `${GOLD}Total Profit: ${AQUA}${formatNumber(totalValue)}`

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
}).setFps(1), () => settings.dianaTracker || settings.dianaStatsTracker);