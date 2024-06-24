import settings from "../../settings";
import { registerWhen, data, setRegisters } from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana,  getDianaAhPrice, formatNumber, formatNumberCommas } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";
import { UIWrappedText } from "../../../Elementa";
import { getGuiOpen, newOverlay } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";



let dianaMobOverlayObj = newOverlay("dianaMobTracker", "dianaTracker", "dianaMobTrackerExample", "render", "MobLoc");
let dianaMobOverlay = dianaMobOverlayObj.overlay;

let dianaLootOverlayObj = newOverlay("dianaLootTracker", "dianaTracker", "dianaLootTrackerExample", "render", "LootLoc");
let dianaLootOverlay = dianaLootOverlayObj.overlay;
let lootChangeButton = new UIWrappedText(`${YELLOW}Click To Change View`);
lootChangeButton.setX((0).pixels()).setY((0).pixels()).onMouseClick(() => {
})
lootChangeButton.onMouseLeave((comp) => {
    lootChangeButton.setText(`${YELLOW}Click To Change View`);
});
lootChangeButton.onMouseEnter((comp) => {
    lootChangeButton.setText(`${YELLOW}${UNDERLINE}Click To Change View`);
});
dianaLootOverlay.addChild(lootChangeButton);

let dianaStatsOverlayObj = newOverlay("dianaStats", "dianaStatsTracker", "dianaStatsExample", "render", "StatsLoc");
let dianaStatsOverlay = dianaStatsOverlayObj.overlay;

let dianaAvgMagicFindOverlayObj = newOverlay("dianaAvgMagicFind", "dianaAvgMagicFind", "dianaAvgMagicFindExample", "render", "AvgMagicFindLoc");
let dianaAvgMagicFindOverlay = dianaAvgMagicFindOverlayObj.overlay;


let dianaMobTrackerText = new UIWrappedText("");
let dianaLootTrackerText = new UIWrappedText("");
dianaLootTrackerText.setY((9).pixels());
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
${GRAY}- ${GOLD}Minos since Stick: ${AQUA}${formatNumberCommas(data.minotaursSinceStick)}
${GRAY}- ${DARK_PURPLE}Champs since Relic: ${AQUA}${formatNumberCommas(data.champsSinceRelic)}
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
        message = getMobMassage(mobTracker, setting, percentDict);
    }
    dianaMobTrackerText.setText(message);
    dianaMobTrackerText.setTextScale((dianaMobOverlayObj.scale).pixels());
}

function getMobMassage(mobTracker, setting, percentDict) {
    const mobTrackerType = ["Total", "Event", "Session"][setting - 1];
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
export function itemOverlay(lootTracker, lootViewSetting, percentDict){
    if(getGuiOpen()) return;
    if (!dianaLootOverlay.children.includes(dianaLootTrackerText)) {
        dianaLootOverlay.clearChildren();
        dianaLootOverlay.addChild(dianaLootTrackerText);   
        dianaLootOverlay.addChild(lootChangeButton); 
    }
    let message = "";
    if (lootViewSetting > 0) {
        message = getLootMessage(lootTracker, lootViewSetting, percentDict);
    }
    dianaLootTrackerText.setText(message);
    dianaLootTrackerText.setTextScale((dianaLootOverlayObj.scale).pixels());
}

// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell


function getLootMessage(lootTracker, lootViewSetting, percentDict) {
    const lootTrackerType = ["Total", "Event", "Session"][lootViewSetting - 1];
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
    
    let lootMessage = `${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${lootTrackerType}${GRAY})
`;
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
        dianaAvgMagicFindOverlayObj.renderGui = true;
    }
    else {
        dianaMobOverlayObj.renderGui = false;
        dianaLootOverlayObj.renderGui = false;
        dianaStatsOverlayObj.renderGui = false;
        dianaAvgMagicFindOverlayObj.renderGui = false;
    }
}).setFps(1), () => settings.dianaTracker || settings.dianaStatsTracker || settings.dianaAvgMagicFind);

// button
// oben links 41 | Y: 146
// unten links 41 | Y: 156
// oben rechts 102 | Y: 146
// unten rechts 102 | Y: 156
register("guiMouseClick" , (x, y, button, gui) => {
    gui = gui.toString();
    if (gui.includes("GuiChat") || gui.includes("GuiInventory")) {
        print(`Mouse Click: X: ${x} | Y: ${y} | Button: ${button} | GUI: ${gui}`)
        // if x and y are in the lootChangeButton then change the lootViewSetting
        print(dianaLootOverlayObj.X)
        print(dianaLootOverlayObj.Y)
        if (x >= dianaLootOverlayObj.X && x <= dianaLootOverlayObj.X + 100 && y >= dianaLootOverlayObj.Y && y <= dianaLootOverlayObj.Y + 10) {
            print("Clicked");
            settings.dianaLootTrackerView += 1;
            if (settings.dianaLootTrackerView > 3) {
                settings.dianaLootTrackerView = 1;
            }
            setRegisters(); // anders machen ist zu verzögert
        }
    }
})