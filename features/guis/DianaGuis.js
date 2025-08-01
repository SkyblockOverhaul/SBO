import settings from "../../settings";
import { registerWhen, data, dianaTimerlist, pastDianaEvents} from "../../utils/variables";
import { playerHasSpade, getPrice, formatNumber, formatNumberCommas, 
    getTracker, calcPercent, drawRect, formatTime, setDianaMayorTotalProfit, setBurrowsPerHour,
    setMobsPerHour, getTotalValue, itemData 
} from "../../utils/functions";
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
    let message = `${YELLOW}${BOLD}Diana Magic Find
${GRAY}- ${LIGHT_PURPLE}Chimera: ${AQUA}${data.highestChimMagicFind}%
${GRAY}- ${GOLD}Sticks: ${AQUA}${data.highestStickMagicFind}%`
    dianaAvgMagicFindOverlay.setLines([dianaAvgMagicFindText.setText(message)]);
}

export function statsOverlay() {
    let message = `${YELLOW}${BOLD}Diana Stats Tracker
${GRAY}- ${LIGHT_PURPLE}Mobs since Inq: ${AQUA}${data.mobsSinceInq}
${GRAY}- ${LIGHT_PURPLE}Inqs since Chimera: ${AQUA}${data.inqsSinceChim}
${GRAY}- ${LIGHT_PURPLE}Inqs since Chimera &7[&bLS&7]: ${AQUA}${data.inqsSinceLsChim}
${GRAY}- ${GOLD}Minos since Stick: ${AQUA}${formatNumberCommas(data.minotaursSinceStick)}
${GRAY}- ${DARK_PURPLE}Champs since Relic: ${AQUA}${formatNumberCommas(data.champsSinceRelic)}`
    dianaStatsOverlay.setLines([dianaStatsText.setText(message)]);
}

export function mobOverlay() {
    let messageLines = []
    if (settings.dianaMobTrackerView > 0) {
        messageLines = getMobMassage(settings.dianaMobTrackerView);
    }
    else {
        messageLines = getMobMassage(1);
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
    let mayorTracker = getTracker(2);
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

    //mobs/hr
    let mobsPerHour = mobTracker["mobs"]["TotalMobs"] / dianaTimerlist[setting - 1].getHourTime();
    let mobsPerHourMayor = mayorTracker["mobs"]["TotalMobs"] / dianaTimerlist[1].getHourTime();
    setMobsPerHour(mobsPerHourMayor.toFixed());
    let mobsPerHourText = isNaN(mobsPerHour) ? "" : `${GRAY}[${AQUA}${mobsPerHour.toFixed(2)}${GRAY}/${AQUA}hr${GRAY}]`;
    let totalText = `${GRAY}- ${GRAY}Total Mobs: ${AQUA}${formatNumberCommas(mobTracker["mobs"]["TotalMobs"])} ${mobsPerHourText}`;
    let totalLine = new OverlayTextLine(totalText, true, true);
    mobLines.push(totalLine);

    return mobLines;
}
let lootMessageLines = [];
let timerOverlayLine = null;

function getTimerMessage(viewSetting) {
    let timer = dianaTimerlist[viewSetting - 1];
    if (timer === undefined) return "00:00:00";
    
    let elapsedTime;
    if (timer.trackerObject.items[timer.dataFieldName] === undefined) {
        elapsedTime = timer.getElapsedTime();
    } else if (timer.trackerObject.items[timer.dataFieldName] > 0) {
        elapsedTime = timer.trackerObject.items[timer.dataFieldName];
    } else {
        elapsedTime = timer.getElapsedTime();
    }
    
    return timer.running ? formatTime(elapsedTime) : `${formatTime(elapsedTime)} &7[&cPAUSED&7]`;
}

register("tick", () => {
    if (timerOverlayLine) {
        let viewSetting = settings.dianaLootTrackerView;
        if (settings.dianaLootTrackerView == 0) {
            viewSetting = 1;
        }
        if (data.hideTrackerLines.includes("timer")) {
            timerOverlayLine.button = true;
            timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
        } else {
            timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage(viewSetting)}`);
        }
    }
});

let totalEventsLine = new OverlayTextLine("", true);
export function itemOverlay() {
    lootMessageLines = [];
    let viewSetting = settings.dianaLootTrackerView;    
    if (settings.dianaLootTrackerView == 0) {
        viewSetting = 1;
    } 
    lootMessageLines = getLootMessage(viewSetting);


    timerOverlayLine = new OverlayButton(`&ePlaytime: &b${getTimerMessage(viewSetting)}`, true, false, true, false).onClick(() => {
        if (timerOverlayLine.button) {
            timerOverlayLine.button = false;
            timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage(viewSetting)}`);
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

    let resetSessionButton = new OverlayButton("&cReset Session", true, true, true, true).onClick(() => {
        ChatLib.command("sboresetsession", true);
    });
    resetSessionButton.onMouseEnter(() => {
        resetSessionButton.setText(`&c&nReset Session`);
    })
    resetSessionButton.onMouseLeave(() => {
        resetSessionButton.setText(`&cReset Session`);
    })
    if (viewSetting == 3) lootMessageLines.push(resetSessionButton);
    if (viewSetting == 1) {
        totalEventsLine.setText(`${YELLOW}Total Events: ${AQUA}${pastDianaEvents.events.length}`);
        lootMessageLines.push(totalEventsLine);
    }
    overlayLootTracker.setLines(lootMessageLines);
}
// .quick_status.buyPrice -> selloffer / instabuy
// .quick_status.sellPrice -> buyorder / instasell
 
const GuiUtils = Java.type("net.minecraftforge.fml.client.config.GuiUtils")
function getLootMessage(lootViewSetting) {
    const lootTrackerType = ["Total", "Event", "Session"][lootViewSetting - 1];
    const offertType = ["Instasell", "Sell Offer"][settings.bazaarSettingDiana];
    let lootTracker = getTracker(lootViewSetting);
    let mayorTracker = getTracker(2);
    let sessionTracker = getTracker(3);
    let percentDict = calcPercent(lootTracker, "loot");

    function createLootLine(item) {
        const price = formatNumber(getPrice(item, lootTracker));
        const itemAmount = item.format ? formatNumber(lootTracker["items"][item.key]) : formatNumberCommas(lootTracker["items"][item.key]);
        let percent = undefined;
        let text = "";
        if (item.hasPercent) {
            percent = item.hasPercent ? percentDict[item.key] : "";
        }
        text = `${GOLD}${price} ${GRAY}| ${item.color}${item.name}: ${AQUA}${itemAmount}`;

        if (percent) {
            if (percent.toString() !== "NaN") {
                text += ` ${GRAY}(${AQUA}${percent}%${GRAY})`;
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

    let totalCoinsText = new OverlayTextLine(`${GOLD}Total Coins: ${AQUA}${formatNumber(lootTracker["items"]["coins"])}`, true, true)
    let burrowsPerHour = lootTracker["items"]["Total Burrows"] / dianaTimerlist[lootViewSetting - 1].getHourTime();
    let burrowsPerHourMayor = mayorTracker["items"]["Total Burrows"] / dianaTimerlist[1].getHourTime();
    let burrowsPerHourText = isNaN(burrowsPerHour) ? "" : `${GRAY}[${AQUA}${burrowsPerHour.toFixed(2)}${GRAY}/${AQUA}hr${GRAY}]`;
    let totalBurrows = new OverlayTextLine(`${GRAY}Total Burrows: ${AQUA}${formatNumberCommas(lootTracker["items"]["Total Burrows"])} ${burrowsPerHourText}`, true, true);
    lootLines.push(totalBurrows);
    setBurrowsPerHour(burrowsPerHourMayor.toFixed());

    let treasure = formatNumber(lootTracker["items"]["coins"] - lootTracker["items"]["fishCoins"] - lootTracker["items"]["scavengerCoins"]).toString();
    let fourEyedFish = formatNumber(lootTracker["items"]["fishCoins"]).toString();
    let scavenger = formatNumber(lootTracker["items"]["scavengerCoins"]).toString();
    let hovertext = [
        "§e§lCoin Breakdown:", 
        `§6Treasure: §b${treasure}`, 
        `§6Four-Eyed Fish: §b${fourEyedFish}`, 
        `§6Scavenger: §b${scavenger}`
    ].map(item => item.toString()); 

    lootLines.push(totalCoinsText.onHover((overlay) => {
        // overlay.gui.drawHoveringString(hovertext, 0, 0)
        GuiUtils.drawHoveringText(hovertext, Client.getMouseX(), Client.getMouseY(), Renderer.screen.getWidth(), Renderer.screen.getHeight(), -1, Renderer.getFontRenderer());
    }));
    let totalProfitText = `${YELLOW}Total Profit: ${AQUA}${formatNumber(getTotalValue(lootTracker))}`;
    let totalProfitLine = new OverlayTextLine(totalProfitText, true, true);
    const timer = dianaTimerlist[lootViewSetting - 1];
    let timePassed = timer.getHourTime();
    let profitPerHour = 0;
    let profitPerBurrow = 0;
    let profitText = [];
    if (timePassed != "NaN" && timePassed != 0) {
        let value = getTotalValue(lootTracker);
        profitPerHour = formatNumber((value / timePassed).toFixed())
        profitPerBurrow = formatNumber((value / lootTracker["items"]["Total Burrows"]).toFixed())

        profitText = [
            `§6${profitPerHour} coins/hour`,
            `§6${profitPerBurrow} coins/burrow`
        ].map(item => item.toString()); // Explicitly convert each element to a string
    }
    totalProfitLine.onHover((overlay) => {
        GuiUtils.drawHoveringText(profitText, Client.getMouseX(), Client.getMouseY(), Renderer.screen.getWidth(), Renderer.screen.getHeight(), -1, Renderer.getFontRenderer());
    });
    let mayorValue = getTotalValue(mayorTracker);
    setDianaMayorTotalProfit(formatNumber(mayorValue), offertType, formatNumber((mayorValue / (mayorTracker["items"]["mayorTime"] / 3600000)).toFixed()));
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

let inquisTracker = new SboOverlay("inquisTracker", "dianaTracker", "inventory", "InquisLoc", "", true);
let buttonChangeInquisView = new OverlayButton("&eChange View", true, true, true, true)
buttonChangeInquisView.onClick(() => {
    settings.inquisTracker += 1;
    if (settings.inquisTracker > 3) {
        settings.inquisTracker = 1;
    }
    inquisOverlay();
})
buttonChangeInquisView.onMouseEnter(() => {
    buttonChangeInquisView.setText(`&e&nChange View`);
})
buttonChangeInquisView.onMouseLeave(() => {
    buttonChangeInquisView.setText(`&eChange View`);
})

let inquisLines = [];
export function inquisOverlay() {
    inquisLines = [];
    let viewSetting = settings.inquisTracker;
    if (settings.inquisTracker == 0) {
        viewSetting = 1;
    }
    inquisLines = getInquisMessage(viewSetting);
    inquisTracker.setLines(inquisLines);
}

function getInquisMessage(viewSetting) {
    const inquisTrackerType = ["Total", "Event", "Session"][viewSetting - 1];
    let inquisTracker = getTracker(viewSetting);
    let percentDict = calcPercent(inquisTracker, "inquis");

    const inquisData = [
        { name: "Turtle Shelmet", key: "DWARF_TURTLE_SHELMET", color: DARK_GREEN, hasPercent: true },
        { name: "Tiger Plushie", key: "CROCHET_TIGER_PLUSHIE", color: DARK_GREEN, hasPercent: true },
        { name: "Antique Remedies", key: "ANTIQUE_REMEDIES", color: DARK_GREEN, hasPercent: true },
        { name: "Turtle Shelmet &7[&bLS&7]", key: "DWARF_TURTLE_SHELMET_LS", color: BLUE, hasPercent: true },
        { name: "Tiger Plushie &7[&bLS&7]", key: "CROCHET_TIGER_PLUSHIE_LS", color: BLUE, hasPercent: true },
        { name: "Antique Remedies &7[&bLS&7]", key: "ANTIQUE_REMEDIES_LS", color: BLUE, hasPercent: true },
    ];

    function createInquisLine(item) {
        const itemAmount = formatNumberCommas(inquisTracker["inquis"][item.key]);
        let percent = undefined;
        let text = "";
        if (item.hasPercent) {
            percent = item.hasPercent ? percentDict[item.key] : "";
        }
        text = `&7- ${item.color}${item.name}: ${AQUA}${itemAmount}`;

        if (percent) {
            if (percent.toString() !== "NaN") {
                text += ` ${GRAY}(${AQUA}${percent}%${GRAY})`;
            }
        }

        let line = new OverlayButton(text, true, false, true, true).onClick(() => {
            if (line.button) {
                line.button = false;
                line.setText(text);
                data.hideTrackerLines = data.hideTrackerLines.filter((line) => line !== `${item.name} inquis`);
            } else {
                line.button = true;
                line.setText("&7&m" + line.text.getString().removeFormatting());
                data.hideTrackerLines.push(`${item.name} inquis`);
            }
        });
        if (data.hideTrackerLines.includes(`${item.name} inquis`)) {
            line.button = true;
            line.setText("&7&m" + line.text.getString().removeFormatting());
        }
        return line;
    }

    let inquisLines = [];
    inquisLines.push(buttonChangeInquisView);
    inquisLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Inquis Loot Tracker ${GRAY}(${YELLOW}${inquisTrackerType}${GRAY})`, true));

    for (let item of inquisData) {
        inquisLines.push(createInquisLine(item));
    }

    return inquisLines;
}



registerWhen(register("step", () => {
    if (playerHasSpade() || checkDiana()) {
        if (settings.dianaLootTrackerView > 0) {
            overlayLootTracker.renderGui = true;
        } else {
            overlayLootTracker.renderGui = false;
        }
        if (settings.dianaMobTrackerView > 0) {
            overlayMobTracker.renderGui = true;
        } else {
            overlayMobTracker.renderGui = false;
        }
        if (settings.inquisTracker > 0) {
            inquisTracker.renderGui = true;
        }
        else {
            inquisTracker.renderGui = false;
        }
        dianaStatsOverlay.renderGui = true;
        dianaAvgMagicFindOverlay.renderGui = true;
    }
    else {
        overlayMobTracker.renderGui = false;
        overlayLootTracker.renderGui = false;
        dianaStatsOverlay.renderGui = false;
        dianaAvgMagicFindOverlay.renderGui = false;
        inquisTracker.renderGui = false;
    }
}).setFps(1), () => settings.dianaTracker || settings.dianaStatsTracker || settings.dianaAvgMagicFind || settings.inquisTracker);

register('guiClosed', (gui) => {
    gui = gui.toString();
    if (gui.includes("vigilance")) {
        setSellText();
    }
});







