import settings from "../../settings";
import { registerWhen, data, dianaTimerlist} from "../../utils/variables";
import { playerHasSpade, getBazaarPriceDiana,  getDianaAhPrice, formatNumber, formatNumberCommas, getTracker, calcPercent, drawRect, formatTime, setDianaMayorTotalProfit } from "../../utils/functions";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { checkDiana } from "../../utils/checkDiana";

let overlayLootTracker = new SboOverlay("dianaLootTracker", "dianaTracker", "inventory", "LootLoc", "", true)
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


let overlayMobTracker = new SboOverlay("dianaMobTracker", "dianaTracker", "inventory", "MobLoc", "", true)
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
    let percentText = percentDict[shortName].toString() != "NaN" ? `${GRAY}(${AQUA}${percentDict[shortName]}%${GRAY})` : "";
    let text = `${GRAY}- ${color}${name}: ${AQUA}${formatNumberCommas(mobTracker["mobs"][shortName])} ${percentText}`;
    if (extra && mobTracker["mobs"][shortName + " Ls"] != 0) {
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
let lootMessageLines = [];
let timerOverlayLine = null;



function getTimerMessage() {
    const timer = dianaTimerlist[settings.dianaLootTrackerView - 1];
    if (timer.trackerObject.items[timer.dataFieldName] > 0) {
        return formatTime(timer.trackerObject.items[timer.dataFieldName]);
    } else {
        return formatTime(timer.getElapsedTime());
    }
}


register("tick", () => {
    if (timerOverlayLine) {
        if (data.hideTrackerLines.includes("timer")) {
            timerOverlayLine.button = true;
            timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
        } else {
            timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage()}`);
        }
    }
});

/**
 * 
 * @param {string} setting 
 */
export function itemOverlay() {
    lootMessageLines = [];
    if (settings.dianaLootTrackerView > 0) {
        lootMessageLines = getLootMessage(settings.dianaLootTrackerView);

        timerOverlayLine = new OverlayButton(`&ePlaytime: &b${getTimerMessage()}`, true, false, true, false).onClick(() => {
            if (timerOverlayLine.button) {
                timerOverlayLine.button = false;
                timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage()}`);
                data.hideTrackerLines = data.hideTrackerLines.filter((line) => line != "timer");
            } else {
                timerOverlayLine.button = true;
                timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
                data.hideTrackerLines.push("timer");
            }
        });
        if (data.hideTrackerLines.includes("timer")) {
            timerOverlayLine.button = true;
            timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
        }
        lootMessageLines.push(timerOverlayLine);
    }
    overlayLootTracker.setLines(lootMessageLines);
}
// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell
 

const GuiUtils = Java.type("net.minecraftforge.fml.client.config.GuiUtils")
function getLootMessage(lootViewSetting) {
    const lootTrackerType = ["Total", "Event", "Session"][lootViewSetting - 1];
    const offertType = ["Instasell", "Sell Offer"][settings.bazaarSettingDiana];
    let lootTracker = getTracker(settings.dianaLootTrackerView);
    let mayorTracker = getTracker(2);
    let percentDict = calcPercent(lootTracker, "loot");


    const itemData = [
        { name: "Chimera", key: "Chimera", color: LIGHT_PURPLE, bazaarKey: "ENCHANTMENT_ULTIMATE_CHIMERA_1", hasPercent: true},
        { name: "Chimera", key: "ChimeraLs", color: LIGHT_PURPLE, bazaarKey: "ENCHANTMENT_ULTIMATE_CHIMERA_1", hasPercent: true},
        { name: "Minos Relic", key: "MINOS_RELIC", color: DARK_PURPLE, ahKey: "MINOS_RELIC", hasPercent: true },
        { name: "Daedalus Stick", key: "Daedalus Stick", color: GOLD, bazaarKey: "DAEDALUS_STICK", hasPercent: true },
        { name: "Crown of Greed", key: "Crown of Greed", color: GOLD, ahKey: "CROWN_OF_GREED" },
        { name: "Souvenir", key: "Washed-up Souvenir", color: GOLD, ahKey: "WASHED_UP_SOUVENIR" },
        { name: "Griffin Feather", key: "Griffin Feather", color: GOLD, bazaarKey: "GRIFFIN_FEATHER" },
        { name: "Turtle Shelmet", key: "DWARF_TURTLE_SHELMET", color: DARK_GREEN, ahKey: "DWARF_TURTLE_SHELMET" },
        { name: "Tiger Plushie", key: "CROCHET_TIGER_PLUSHIE", color: DARK_GREEN, ahKey: "CROCHET_TIGER_PLUSHIE" },
        { name: "Antique Remedies", key: "ANTIQUE_REMEDIES", color: DARK_GREEN, ahKey: "ANTIQUE_REMEDIES" },
        { name: "Ancient Claws", key: "ANCIENT_CLAW", color: BLUE, bazaarKey: "ANCIENT_CLAW", format: true },
        { name: "Enchanted Claws", key: "ENCHANTED_ANCIENT_CLAW", color: BLUE, bazaarKey: "ENCHANTED_ANCIENT_CLAW" },
        { name: "Enchanted Gold", key: "ENCHANTED_GOLD", color: BLUE, bazaarKey: "ENCHANTED_GOLD", format: true },
        { name: "Enchanted Iron", key: "ENCHANTED_IRON", color: BLUE, bazaarKey: "ENCHANTED_IRON", format: true }
    ];

    function getPrice(item, mayorTracker = undefined) {
        if(mayorTracker) {
            if (item.bazaarKey) {
                if (item.name === "Chimera" && item.key === "Chimera") {
                    return getBazaarPriceDiana(item.bazaarKey) * mayorTracker["items"]["Chimera"];
                }
                else if (item.name === "Chimera" && item.key === "ChimeraLs") {
                    return getBazaarPriceDiana(item.bazaarKey) * mayorTracker["items"]["ChimeraLs"];
                }
                return getBazaarPriceDiana(item.bazaarKey) * mayorTracker["items"][item.key];
            } else if (item.ahKey) {
                return getDianaAhPrice(item.ahKey) * mayorTracker["items"][item.key];
            }
            return 0;
        }
        else{
            if (item.bazaarKey) {
                if (item.name === "Chimera" && item.key === "Chimera") {
                    return getBazaarPriceDiana(item.bazaarKey) * lootTracker["items"]["Chimera"]
                }
                else if (item.name === "Chimera" && item.key === "ChimeraLs") {
                    return getBazaarPriceDiana(item.bazaarKey) * lootTracker["items"]["ChimeraLs"];
                }
                return getBazaarPriceDiana(item.bazaarKey) * lootTracker["items"][item.key];
            } else if (item.ahKey) {
                return getDianaAhPrice(item.ahKey) * lootTracker["items"][item.key];
            }
            return 0;
        }
    }

    function createLootLine(item) {
        const price = formatNumber(getPrice(item));
        const itemAmount = item.format ? formatNumber(lootTracker["items"][item.key]) : formatNumberCommas(lootTracker["items"][item.key]);
        let percent = undefined;
        let lsAmount = undefined;
        let text = "";
        if (item.hasPercent && item.key !== "ChimeraLs") {
            percent = item.hasPercent ? percentDict[item.name] : "";
        }
        else if (item.key === "ChimeraLs") {
            percent = item.hasPercent ? percentDict[item.key] : "";
            lsAmount = item.hasLS ? lootTracker["items"]["ChimeraLs"] : "";
        }
        if (item.key === "ChimeraLs") {
            text = `${GOLD}${price} ${GRAY}| ${item.color}${item.key}: ${AQUA}${itemAmount}`;
        }
        else {
            text = `${GOLD}${price} ${GRAY}| ${item.color}${item.name}: ${AQUA}${itemAmount}`;
        }

        if (percent) {
            if (percent.toString() !== "NaN") {
                text += ` ${GRAY}(${AQUA}${percent}%${GRAY})`;
            }
        }

        let line = new OverlayButton(text, true, false, true, true).onClick(() => {
            if (line.button) {
                line.button = false;
                line.setText(text);
                if (item.key === "ChimeraLs") {
                    data.hideTrackerLines = data.hideTrackerLines.filter((line) => line != item.key);
                } else {
                    data.hideTrackerLines = data.hideTrackerLines.filter((line) => line != item.name);
                }
            } else {
                line.button = true;
                line.setText("&7&m" + line.text.getString().removeFormatting());
                if (item.key === "ChimeraLs") {
                    data.hideTrackerLines.push(item.key);
                }
                else {
                    data.hideTrackerLines.push(item.name);
                }
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
    
    let treasure = formatNumber(lootTracker["items"]["coins"] - lootTracker["items"]["fishCoins"] - lootTracker["items"]["scavengerCoins"]).toString();
    let fourEyedFish = formatNumber(lootTracker["items"]["fishCoins"]).toString();
    let scavenger = formatNumber(lootTracker["items"]["scavengerCoins"]).toString();
    let hovertext = [
        "§e§lCoin Breakdown:", 
        `§6Treasure: §b${treasure}`, 
        `§6Four-Eyed Fish: §b${fourEyedFish}`, 
        `§6Scavenger: §b${scavenger}`
    ].map(item => item.toString()); // Explicitly convert each element to a string

    lootLines.push(new OverlayTextLine(totalBurrowsText, true));

    lootLines.push(totalCoinsText.onHover((overlay) => {
        // overlay.gui.drawHoveringString(hovertext, 0, 0)
        GuiUtils.drawHoveringText(hovertext, Client.getMouseX(), Client.getMouseY(), Renderer.screen.getWidth(), Renderer.screen.getHeight(), -1, Renderer.getFontRenderer());
    }));
    function getTotalValue(mayorOnly = false) {
        let totalValue = 0;
        if(mayorOnly) {
            for (let item of itemData) {
                totalValue += getPrice(item, mayorTracker);
            }
            totalValue += mayorTracker["items"]["coins"];
            return totalValue;
        }
        else {
            for (let item of itemData) {
                totalValue += getPrice(item);
            }
            totalValue += lootTracker["items"]["coins"];
            return totalValue;
        }
    }
    let totalProfitText = `${YELLOW}Total Profit: ${AQUA}${formatNumber(getTotalValue())}`;
    let totalProfitLine = new OverlayTextLine(totalProfitText, true, true);
    const timer = dianaTimerlist[settings.dianaLootTrackerView - 1];
    const timePassed = timer.getHourTime(); // in hours 
    const profitPerHour = formatNumber((getTotalValue() / timePassed).toFixed()) // in coins
    let profitText = [
        `§6${profitPerHour} coins/hour`,

    ].map(item => item.toString()); // Explicitly convert each element to a string
    totalProfitLine.onHover((overlay) => {
        GuiUtils.drawHoveringText(profitText, Client.getMouseX(), Client.getMouseY(), Renderer.screen.getWidth(), Renderer.screen.getHeight(), -1, Renderer.getFontRenderer());
    });
    setDianaMayorTotalProfit(formatNumber(getTotalValue(true)), offertType);
    lootLines.push(totalProfitLine);

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







