import settings from "../../settings";
import { registerWhen, data } from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana,  getDianaAhPrice, formatNumber, formatNumberCommas, getTracker, calcPercent, drawRect } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";




let overlayMobTracker = new SboOverlay("dianaMobTracker", "dianaTracker", "inventory", "MobLoc", "", true)
let textOverlayLineMob = new OverlayTextLine("", true)
let buttonChangeMobView = new OverlayButton("&eChange View", true, true, true, true)
buttonChangeMobView.onClick(() => {
    settings.dianaMobTrackerView += 1;
    if (settings.dianaMobTrackerView > 3) {
        settings.dianaMobTrackerView = 1;
    }
    mobOverlay();
})
buttonChangeMobView.onMouseEnter(() => {
    buttonChangeMobView.setText(`&e&nChange View`);
})
buttonChangeMobView.onMouseLeave(() => {
    buttonChangeMobView.setText(`&eChange View`);
})


let overlayLootTracker = new SboOverlay("dianaLootTracker", "dianaTracker", "inventory", "LootLoc", "", true)
let lootMessageLine = new OverlayTextLine("", true)
let buttonChangeLootView = new OverlayButton("&eChange View", true, true, true, true)
buttonChangeLootView.onClick(() => {  
    settings.dianaLootTrackerView += 1;
    if (settings.dianaLootTrackerView > 3) {
        settings.dianaLootTrackerView = 1;
    }
    itemOverlay();
})
buttonChangeLootView.onMouseEnter(() => {
    buttonChangeLootView.setText(`&e&nChange View`);
})
buttonChangeLootView.onMouseLeave(() => {
    buttonChangeLootView.setText(`&eChange View`);
})

let buttonBazaarSetting = new OverlayButton("Sell", true, true, false, true)
buttonBazaarSetting.onClick(() => {
    settings.bazaarSettingDiana += 1;
    if (settings.bazaarSettingDiana > 1) {
        settings.bazaarSettingDiana = 0;
    }
    if (buttonBazaarSetting.isHovered) {
        setSellText("hover");
    }
    else {
        setSellText();
    }
    itemOverlay();
})
buttonBazaarSetting.onMouseEnter(() => {
    setSellText("hover");
})
buttonBazaarSetting.onMouseLeave(() => {
    setSellText();
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
    let messageLines = []
    if (settings.dianaMobTrackerView > 0) {
        messageLines = getMobMassage(settings.dianaMobTrackerView);
    }
    overlayMobTracker.setLines(messageLines);
}

function createMobLine(name, color, shortName, extra, mobTracker, percentDict) {
    let text = `${GRAY}- ${color}${name}: ${AQUA}${formatNumberCommas(mobTracker["mobs"][shortName])} ${GRAY}(${AQUA}${percentDict[shortName]}%${GRAY})`;
    if (extra) {
        text += ` ${GRAY}[${AQUA}LS${GRAY}:${AQUA}${formatNumberCommas(mobTracker["mobs"][shortName + " Ls"])}${GRAY}]`;
    }
    let line = new OverlayButton(text, true, false, true, true).onClick(() => {
        if (line.button) {
            line.button = false;
            line.setText(text);
            data.hideTrackerLines = data.hideTrackerLines.filter((line) => line != name);
        } else {
            line.button = true;
            line.setText("&7&m" + line.text.getString().removeFormatting());
            data.hideTrackerLines.push(name);
        }
    });
    if (data.hideTrackerLines.includes(name)) {
        line.button = true;
        line.setText("&7&m" + line.text.getString().removeFormatting());
    }
    return line;
}

function getMobMassage(setting) {
    const mobTrackerType = ["Total", "Event", "Session"][setting - 1];
    let mobTracker = getTracker(setting);
    let percentDict = calcPercent(mobTracker, "mobs");
    let mobLines = [];
    
    mobLines.push(buttonChangeMobView);
    mobLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Diana Mob Tracker ${GRAY}(${YELLOW}${mobTrackerType}${GRAY})`, true));
    
    const mobData = [
        { name: "Inquisitor", color: LIGHT_PURPLE, shortName: "Minos Inquisitor", extra: true },
        { name: "Champion", color: DARK_PURPLE, shortName: "Minos Champion", extra: false },
        { name: "Minotaur", color: GOLD, shortName: "Minotaur", extra: false },
        { name: "Gaia Construct", color: GREEN, shortName: "Gaia Construct", extra: false },
        { name: "Siamese Lynx", color: GREEN, shortName: "Siamese Lynxes", extra: false },
        { name: "Hunter", color: GREEN, shortName: "Minos Hunter", extra: false }
    ];

    for (let mob of mobData) {
        mobLines.push(createMobLine(mob.name, mob.color, mob.shortName, mob.extra, mobTracker, percentDict));
    }

    let totalText = `${GRAY}- ${GRAY}Total Mobs: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["TotalMobs"])}`;
    let totalLine = new OverlayTextLine(totalText, true);
    mobLines.push(totalLine);

    return mobLines;
}

/**
 * 
 * @param {string} setting 
 */
export function itemOverlay() {
    let messageLines = [];
    if (settings.dianaLootTrackerView > 0) {
        messageLines = getLootMessage(settings.dianaLootTrackerView);
    }
    overlayLootTracker.setLines(messageLines);
}

// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell

let coinHoverText = new hoverText("")


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

    const itemData = [
        { name: "Chimera", key: "Chimera", color: LIGHT_PURPLE, bazaarKey: "ENCHANTMENT_ULTIMATE_CHIMERA_1", hasPercent: true, hasLS: true },
        { name: "Minos Relic", key: "MINOS_RELIC", color: DARK_PURPLE, ahKey: "MINOS_RELIC", hasPercent: true },
        { name: "Daedalus Stick", key: "Daedalus Stick", color: GOLD, bazaarKey: "DAEDALUS_STICK", hasPercent: true },
        { name: "Crown of Greed", key: "Crown of Greed", color: GOLD, ahKey: "CROWN_OF_GREED" },
        { name: "Souvenir", key: "Washed-up Souvenir", color: GOLD, ahKey: "WASHED_UP_SOUVENIR" },
        { name: "Griffin Feather", key: "Griffin Feather", color: GOLD, bazaarKey: "GRIFFIN_FEATHER" },
        { name: "Turtle Shelmet", key: "DWARF_TURTLE_SHELMET", color: DARK_GREEN, ahKey: "DWARF_TURTLE_SHELMET" },
        { name: "Tiger Plushie", key: "CROCHET_TIGER_PLUSHIE", color: DARK_GREEN, ahKey: "CROCHET_TIGER_PLUSHIE" },
        { name: "Antique Remedies", key: "ANTIQUE_REMEDIES", color: DARK_GREEN, ahKey: "ANTIQUE_REMEDIES" },
        { name: "Ancient Claws", key: "ANCIENT_CLAW", color: BLUE, bazaarKey: "ANCIENT_CLAW" },
        { name: "Enchanted Claws", key: "ENCHANTED_ANCIENT_CLAW", color: BLUE, bazaarKey: "ENCHANTED_ANCIENT_CLAW" },
        { name: "Enchanted Gold", key: "ENCHANTED_GOLD", color: BLUE, bazaarKey: "ENCHANTED_GOLD" },
        { name: "Enchanted Iron", key: "ENCHANTED_IRON", color: BLUE, bazaarKey: "ENCHANTED_IRON" }
    ];

    function getPrice(item) {
        if (item.bazaarKey) {
            if (item.name === "Chimera") {
                return getBazaarPriceDiana(item.bazaarKey) * totalChimera;
            }
            return getBazaarPriceDiana(item.bazaarKey) * lootTracker["items"][item.key];
        } else if (item.ahKey) {
            return getDianaAhPrice(item.ahKey) * lootTracker["items"][item.key];
        }
        return 0;
    }

    function createLootLine(item) {
        const price = formatNumber(getPrice(item));
        const itemAmount = formatNumberCommas(lootTracker["items"][item.key]);
        const percent = item.hasPercent ? percentDict[item.name] : "";
        const lsAmount = item.hasLS ? lootTracker["items"]["ChimeraLs"] : "";
        let text = `${GOLD}${price} ${GRAY}| ${item.color}${item.name}: ${AQUA}${itemAmount}`;

        if (percent) {
            text += ` ${GRAY}(${AQUA}${percent}%${GRAY})`;
            if (item.hasLS) {
                text += ` ${GRAY}[${AQUA}LS${GRAY}:${AQUA}${lsAmount}${GRAY}]`;
            }
        }

        let line = new OverlayButton(text, true, false, true, true).onClick(() => {
            if (line.button) {
                line.button = false;
                line.setText(text);
                data.hideTrackerLines = data.hideTrackerLines.filter((line) => line != item.name);
            } else {
                line.button = true;
                line.setText("&7&m" + line.text.getString().removeFormatting());
                data.hideTrackerLines.push(item.name);
            }
        });
        if (data.hideTrackerLines.includes(item.name)) {
            line.button = true;
            line.setText("&7&m" + line.text.getString().removeFormatting());
        }
        return line;
    }

    let lootLines = [];
    lootLines.push(buttonChangeLootView);
    lootLines.push(buttonBazaarSetting);
    lootLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Diana Loot Tracker ${GRAY}(${YELLOW}${lootTrackerType}${GRAY})`, true));

    for (let item of itemData) {
        lootLines.push(createLootLine(item));
    }

    let totalBurrowsText = `${GRAY}Total Burrows: ${AQUA}${formatNumberCommas(lootTracker["items"]["Total Burrows"])}`;
    let totalCoinsText = new OverlayTextLine(`${GOLD}Total Coins: ${AQUA}${formatNumber(lootTracker["items"]["coins"])}`, true, true)
    
    lootLines.push(new OverlayTextLine(totalBurrowsText, true));
    coinHoverText.setText("totalCoinsText");
    lootLines.push(totalCoinsText.onHover((overlay) => {
        // print("hovering")
        // coinHoverText.setXYScale(totalCoinsText.X, totalCoinsText.Y, totalCoinsText.scale)
        // coinHoverText.draw();
        overlay.gui.drawHoveringString(["test"], 0, 0)
        // draw rectangle 
    }));
    let totalValue = 0;
    for (let item of itemData) {
        totalValue += getPrice(item);
    }
    totalValue += lootTracker["items"]["coins"];
    let totalProfitText = `${YELLOW}Total Profit: ${AQUA}${formatNumber(totalValue)}`;
    lootLines.push(new OverlayTextLine(totalProfitText, true));

    return lootLines;
}


let mythosHpOverlay= new SboOverlay("mythosMobHp", "mythosMobHp", "render", "MythosHpLoc", "mythosMobHpExample");
let mythosHpText = new OverlayTextLine("", true);


export function mythosMobHpOverlay(mobNamesWithHp) {
    let message = "";
    if (mobNamesWithHp.length > 0) {
        mobNamesWithHp.forEach((mob) => {
            message += `${mob}\n`;
        });
        mythosHpOverlay.renderGui = true;
    }
    else {
        mythosHpOverlay.renderGui = false;
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







