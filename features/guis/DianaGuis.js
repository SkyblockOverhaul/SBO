import settings from "../../settings";
import { registerWhen, data } from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana,  getDianaAhPrice, formatNumber, formatNumberCommas, getTracker, calcPercent } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";
import { SboOverlay, OverlayTextLine, OverlayButton } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";




let overlayMobTracker = new SboOverlay("dianaMobTracker", "dianaTracker", "inventory", "MobLoc")
let textOverlayLineMob = new OverlayTextLine("", true)
let buttonChangeMobView = new OverlayButton("&eChange View", true, true)
buttonChangeMobView.onClick(() => {
    settings.dianaMobTrackerView += 1;
    if (settings.dianaMobTrackerView > 3) {
        settings.dianaMobTrackerView = 1;
    }
    mobOverlay();
})

let overlayLootTracker = new SboOverlay("dianaLootTracker", "dianaTracker", "inventory", "LootLoc")
let lootMessageLine = new OverlayTextLine("", true)
let buttonChangeLootView = new OverlayButton("&eChange View", true, true)
buttonChangeLootView.onClick(() => {  
    settings.dianaLootTrackerView += 1;
    if (settings.dianaLootTrackerView > 3) {
        settings.dianaLootTrackerView = 1;
    }
    itemOverlay();
})
let buttonBazaarSetting = new OverlayButton("Sell", true, true, false)
buttonBazaarSetting.onClick(() => {
    settings.bazaarSettingDiana += 1;
    if (settings.bazaarSettingDiana > 1) {
        settings.bazaarSettingDiana = 0;
    }
    setSellText();
    itemOverlay();
})

function setSellText(type = "") {
    if (type == "hover") {
        if (settings.bazaarSettingDiana == 0) {
            buttonBazaarSetting.setText(`&e&nInstasell`);
            
        }
        else {
            buttonBazaarSetting.setText(`&e&nSell Offer`);
        }
    }
    else {
        if (settings.bazaarSettingDiana == 0) {
            buttonBazaarSetting.setText(`&eInstasell`);
        }
        else {
            buttonBazaarSetting.setText(`&eSell Offer`);
        }
    }
}
setSellText();

let dianaStatsOverlay = new SboOverlay("dianaStats", "dianaStatsTracker", "render", "StatsLoc");
let dianaStatsText = new OverlayTextLine("", true);

let dianaAvgMagicFindOverlay = new SboOverlay("dianaAvgMagicFind", "dianaAvgMagicFind", "render", "AvgMagicFindLoc");
let dianaAvgMagicFindText = new OverlayTextLine("", true);

export function avgMagicFindOverlay() {
    let message = `${YELLOW}${BOLD}Diana Magic Find ${GRAY}(${YELLOW}${BOLD}Avg${GRAY})
${GRAY}- ${LIGHT_PURPLE}Chimera: ${AQUA}${data.avgChimMagicFind}%
${GRAY}- ${GOLD}Sticks: ${AQUA}${data.avgStickMagicFind}%`
    dianaAvgMagicFindOverlay.setLines([dianaAvgMagicFindText.setText(message)]);
}

export function statsOverlay() {
    let message = `${YELLOW}${BOLD}Diana Stats Tracker
${GRAY}- ${LIGHT_PURPLE}Mobs since Inq: ${AQUA}${data.mobsSinceInq}
${GRAY}- ${LIGHT_PURPLE}Inqs since Chimera: ${AQUA}${data.inqsSinceChim}
${GRAY}- ${GOLD}Minos since Stick: ${AQUA}${formatNumberCommas(data.minotaursSinceStick)}
${GRAY}- ${DARK_PURPLE}Champs since Relic: ${AQUA}${formatNumberCommas(data.champsSinceRelic)}`
    dianaStatsOverlay.setLines([dianaStatsText.setText(message)]);
}

/**
 * 
 * @param {string} setting 
 */
export function mobOverlay() {
    let message = "";
    if (settings.dianaMobTrackerView > 0) {
        message = getMobMassage(settings.dianaMobTrackerView);
    }
    overlayMobTracker.setLines([buttonChangeMobView, textOverlayLineMob.setText(message)]);
}

function getMobMassage(setting) {
    const mobTrackerType = ["Total", "Event", "Session"][setting - 1];
    let mobTracker = getTracker(setting);
    let percentDict = calcPercent(mobTracker, "mobs");
    let mobMessage = `${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${mobTrackerType}${GRAY})\n`
    mobMessage += `${GRAY}- ${LIGHT_PURPLE}Inquisitor: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Minos Inquisitor"])} ${GRAY}(${AQUA}${percentDict["Minos Inquisitor"]}%${GRAY}) ${GRAY}[${AQUA}LS${GRAY}:${AQUA}${formatNumberCommas(mobTracker["mobs"]["Minos Inquisitor Ls"])}${GRAY}]\n`
    mobMessage += `${GRAY}- ${DARK_PURPLE}Champion: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Minos Champion"])} ${GRAY}(${AQUA}${percentDict["Minos Champion"]}%${GRAY})\n`
    mobMessage += `${GRAY}- ${GOLD}Minotaur: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Minotaur"])} ${GRAY}(${AQUA}${percentDict["Minotaur"]}%${GRAY})\n`
    mobMessage += `${GRAY}- ${GREEN}Gaia Construct: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Gaia Construct"])} ${GRAY}(${AQUA}${percentDict["Gaia Construct"]}%${GRAY})\n`
    mobMessage += `${GRAY}- ${GREEN}Siamese Lynx: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Siamese Lynxes"])} ${GRAY}(${AQUA}${percentDict["Siamese Lynxes"]}%${GRAY})\n`
    mobMessage += `${GRAY}- ${GREEN}Hunter: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["Minos Hunter"])} ${GRAY}(${AQUA}${percentDict["Minos Hunter"]}%${GRAY})\n`
    mobMessage += `${GRAY}- ${GRAY}Total Mobs: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["TotalMobs"])}`
    return mobMessage;
}
/**
 * 
 * @param {string} setting 
 */
export function itemOverlay() {
    let message = "";
    if (settings.dianaLootTrackerView > 0) {
        message = getLootMessage(settings.dianaLootTrackerView);
    }
    overlayLootTracker.setLines([buttonChangeLootView, buttonBazaarSetting, lootMessageLine.setText(message)]);
}

// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell


function getLootMessage(lootViewSetting) {
    const lootTrackerType = ["Total", "Event", "Session"][lootViewSetting - 1];
    let lootTracker = getTracker(settings.dianaLootTrackerView);
    let percentDict = calcPercent(lootTracker, "loot");
    let totalChimera = 0;
    for (let key of ["Chimera", "ChimeraLs"]) {
        if (lootTracker.items[key] !== undefined) {
            totalChimera += lootTracker.items[key];
        }
    }
    let relicPrice = getDianaAhPrice("MINOS_RELIC") * lootTracker["items"]["MINOS_RELIC"]
    let chimeraPrice = getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1") * totalChimera
    let daedalusPrice = getBazaarPriceDiana("DAEDALUS_STICK") * lootTracker["items"]["Daedalus Stick"]
    let griffinPrice = getBazaarPriceDiana("GRIFFIN_FEATHER") * lootTracker["items"]["Griffin Feather"]
    let clawPrice = getBazaarPriceDiana("ANCIENT_CLAW") * lootTracker["items"]["ANCIENT_CLAW"]
    let echClawPrice = getBazaarPriceDiana("ENCHANTED_ANCIENT_CLAW") * lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]
    let goldPrice = getBazaarPriceDiana("ENCHANTED_GOLD") * lootTracker["items"]["ENCHANTED_GOLD"]
    let ironPrice = getBazaarPriceDiana("ENCHANTED_IRON") * lootTracker["items"]["ENCHANTED_IRON"]
    let dwarfPrice = getDianaAhPrice("DWARF_TURTLE_SHELMET") * lootTracker["items"]["DWARF_TURTLE_SHELMET"]
    let tigerPrice = getDianaAhPrice("CROCHET_TIGER_PLUSHIE") * lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]
    let antiquePrice = getDianaAhPrice("ANTIQUE_REMEDIES") * lootTracker["items"]["ANTIQUE_REMEDIES"]
    let crownPrice = getDianaAhPrice("CROWN_OF_GREED") * lootTracker["items"]["Crown of Greed"]
    let souvenirPrice = getDianaAhPrice("WASHED_UP_SOUVENIR") * lootTracker["items"]["Washed-up Souvenir"]
    
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${lootTrackerType}${GRAY})\n`;
    function getMessagePart(price, color, itemName, itemAmount, percent = "") {
        if (percent == ""){
            return `${GOLD}${price} ${GRAY}| ${color}${itemName}: ${AQUA}${itemAmount}\n`
        }
        else if (itemName == "Chimera") {
            return `${GOLD}${price} ${GRAY}| ${color}${itemName}: ${AQUA}${itemAmount} ${GRAY}(${AQUA}${percent}%${GRAY}) ${GRAY}[${AQUA}LS${GRAY}:${AQUA}${lootTracker["items"]["ChimeraLs"]}${GRAY}]\n`
        }
        else {
            return `${GOLD}${price} ${GRAY}| ${color}${itemName}: ${AQUA}${itemAmount} ${GRAY}(${AQUA}${percent}%${GRAY})\n`
        }

    }
    
    lootMessage += getMessagePart(formatNumber(chimeraPrice), LIGHT_PURPLE, "Chimera", lootTracker["items"]["Chimera"], percentDict["Chimera"]);
    lootMessage += getMessagePart(formatNumber(relicPrice), DARK_PURPLE, "Minos Relic", lootTracker["items"]["MINOS_RELIC"], percentDict["Minos Relic"]);
    lootMessage += getMessagePart(formatNumber(daedalusPrice), GOLD, "Daedalus Stick", lootTracker["items"]["Daedalus Stick"], percentDict["Daedalus Stick"]);
    lootMessage += getMessagePart(formatNumber(crownPrice), GOLD, "Crown of Greed", formatNumberCommas(lootTracker["items"]["Crown of Greed"]));
    lootMessage += getMessagePart(formatNumber(souvenirPrice), GOLD, "Souvenir", formatNumberCommas(lootTracker["items"]["Washed-up Souvenir"]));
    lootMessage += getMessagePart(formatNumber(griffinPrice), GOLD, "Griffin Feather", formatNumberCommas(lootTracker["items"]["Griffin Feather"]));
    lootMessage += getMessagePart(formatNumber(dwarfPrice), DARK_GREEN, "Turtle Shelmet", formatNumberCommas(lootTracker["items"]["DWARF_TURTLE_SHELMET"]));
    lootMessage += getMessagePart(formatNumber(tigerPrice), DARK_GREEN, "Tiger Plushie", formatNumberCommas(lootTracker["items"]["CROCHET_TIGER_PLUSHIE"]));
    lootMessage += getMessagePart(formatNumber(antiquePrice), DARK_GREEN, "Antique Remedies", formatNumberCommas(lootTracker["items"]["ANTIQUE_REMEDIES"]));
    lootMessage += getMessagePart(formatNumber(clawPrice), BLUE, "Ancient Claws", formatNumber(lootTracker["items"]["ANCIENT_CLAW"]));
    lootMessage += getMessagePart(formatNumber(echClawPrice), BLUE, "Enchanted Claws", formatNumberCommas(lootTracker["items"]["ENCHANTED_ANCIENT_CLAW"]));
    lootMessage += getMessagePart(formatNumber(goldPrice), BLUE, "Enchanted Gold", formatNumber(lootTracker["items"]["ENCHANTED_GOLD"]));
    lootMessage += getMessagePart(formatNumber(ironPrice), BLUE, "Enchanted Iron", formatNumber(lootTracker["items"]["ENCHANTED_IRON"]));
        lootMessage += `${GRAY}Total Burrows: ${AQUA}${formatNumberCommas(lootTracker["items"]["Total Burrows"])}\n`
    lootMessage += `${GOLD}Total Coins: ${AQUA}${formatNumber(lootTracker["items"]["coins"])}\n`
    let totalValue = 0;
    totalValue = relicPrice + chimeraPrice + daedalusPrice + griffinPrice + dwarfPrice + tigerPrice + antiquePrice + crownPrice + souvenirPrice + clawPrice + echClawPrice + goldPrice + ironPrice + lootTracker["items"]["coins"];
    lootMessage += `${YELLOW}Total Profit: ${AQUA}${formatNumber(totalValue)}`

    return lootMessage;
}

let mythosHpOverlay= new SboOverlay("mythosMobHp", "mythosMobHp", "render", "MythosHpLoc", "mythosMobHpExample");
let mythosHpText = new OverlayTextLine("", true);


export function mythosMobHpOverlay(mobNamesWithHp) {
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
    mythosHpOverlay.setLines([mythosHpText.setText(message)]);
}

registerWhen(register("step", () => {
    if (playerHasSpade() || checkDiana()) {
        overlayMobTracker.renderGui = true;
        overlayLootTracker.renderGui = true;
        dianaStatsOverlay.renderGui = true;
        dianaAvgMagicFindOverlay.renderGui = true;
    }
    else {
        overlayMobTracker.renderGui = false;
        overlayLootTracker.renderGui = false;
        dianaStatsOverlay.renderGui = false;
        dianaAvgMagicFindOverlay.renderGui = false;
    }
}).setFps(1), () => settings.dianaTracker || settings.dianaStatsTracker || settings.dianaAvgMagicFind);


register('guiClosed', (gui) => {
    gui = gui.toString();
    if (gui.includes("vigilance")) {
        setSellText();
    }
});







