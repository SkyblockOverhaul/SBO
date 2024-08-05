import settings from "../../settings";
import { registerWhen, timerCrown, data } from "../../utils/variables";
import { formatNumber, formatNumberCommas, formatTime, isInSkyblock } from "../../utils/functions";
import { isDataLoaded } from "../../utils/checkData";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE, RED} from "../../utils/constants";

let crownTracker = new SboOverlay("crownTracker", "crownTracker", "render", "CrownLoc");
let timerOverlayLine = new OverlayTextLine(`&ePlaytime: &b${getTimerMessage()}`, true);

function crownOverlay() {
    calculateCrownCoins();
    let messageLines = [];
    messageLines = getCrownMessage();
    messageLines.push(timerOverlayLine);
    crownTracker.setLines(messageLines);
}

const crownTiers = [1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000];

function getCrownMessage() {
    let crownLines = [];
    let timePassed = timerCrown.getHourTime();
    let profitPerHour = 0;
    let timeUntilNextTier = 0;
    let currentTier = 0;

    for (let i = 0; i < crownTiers.length; i++) {
        if (data.totalCrownCoins < crownTiers[i]) {
            currentTier = i - 1;
            break;
        }
    }
    if (currentTier < 0) currentTier = 0;
    let nextTier = crownTiers[currentTier + 1];

    if (timePassed != "NaN" && timePassed != 0) {
        profitPerHour = (data.totalCrownCoinsGained / timePassed).toFixed();
    }

    if (profitPerHour > 0 && nextTier > data.totalCrownCoins) {
        let coinsNeeded = nextTier - data.totalCrownCoins;
        let hoursUntilNextTier = coinsNeeded / profitPerHour;
        let millisecondsUntilNextTier = hoursUntilNextTier * 60 * 60 * 1000;
        timeUntilNextTier = formatTime(millisecondsUntilNextTier);
    }

    crownLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Crown Tracker`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Total Coins: ${AQUA}${formatNumberCommas(data.totalCrownCoins)}`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Tracked Coins: ${AQUA}${formatNumber(data.totalCrownCoinsGained)}`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Last Coins: ${AQUA}${formatNumber(data.lastCrownCoins)}`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Coins/hr: ${AQUA}${formatNumber(profitPerHour)}`, true));

    if (timeUntilNextTier) {
        crownLines.push(new OverlayTextLine(`${GOLD}${formatNumber(nextTier)} in: ${AQUA}${timeUntilNextTier}`, true));
    } 
    else if (data.totalCrownCoinsGained == 0) {
        crownLines.push(new OverlayTextLine(`${GOLD}${formatNumber(nextTier)} in: ${AQUA}Unknown`, true));
    }
    else if (currentTier == crownTiers.length - 1) {
        crownLines.push(new OverlayTextLine(`${GOLD}Tier: ${RED}MAX!`, true));
    }

    return crownLines;
}

function getTimerMessage() {
    const timer = timerCrown;
    if (timer === undefined) return "00:00:00";
    if (timer.trackerObject[timer.dataFieldName] === undefined) return formatTime(timer.getElapsedTime());
    if (timer.trackerObject[timer.dataFieldName] > 0) {
        return formatTime(timer.trackerObject[timer.dataFieldName]);
    } else {
        return formatTime(timer.getElapsedTime());
    }
}

function getCoinsFromCrown() {
    let helmet = hasCrown();
    if (!helmet) return 0;
    let helmetLore = helmet.getLore();
    let coinsFound = 0;
    for (let line of helmetLore) {
        if (line == null) continue;
        if (line.includes("Collected Coins")) {
            let coins = line.split(": ")[1];
            coins = coins.replace(/§./, "").replaceAll(",", "");
            coinsFound = parseInt(coins);
            break;
        }
    }
    return coinsFound;
}

let coinsBeforeCreeperDeath = getCoinsFromCrown();
function calculateCrownCoins() {
    let coinsAfterCreeperDeath = getCoinsFromCrown();
    let coinsGained = coinsAfterCreeperDeath - coinsBeforeCreeperDeath;
    if (coinsGained > 0 && coinsGained != coinsAfterCreeperDeath) {
        timerCrown.start();
        timerCrown.continue();
        timerCrown.updateActivity();
        data.totalCrownCoinsGained += coinsGained;
        data.lastCrownCoins = coinsGained;
        data.totalCrownCoins = coinsAfterCreeperDeath;
        coinsBeforeCreeperDeath = coinsAfterCreeperDeath;
    }
}

function hasCrown() {
    if (!Player.armor.getHelmet() || Player.armor.getHelmet() == null) return false;
    if (!Player.armor.getHelmet().getLore() || Player.armor.getHelmet().getLore() == null) return false;
    let helmet = Player.armor.getHelmet();
    if (!helmet.getName() || helmet.getName() == null) return false;
    let helmetName = helmet.getName().trim();
    if (!helmetName.includes("Crown of Avarice")) return false;
    return helmet;
}

let isInitilized = false;
function cronwInitilization() {
    if (isInitilized) return;
    if (isInSkyblock() && isDataLoaded()) {
        data.totalCrownCoins = getCoinsFromCrown();
        if (data.totalCrownCoins > 0) {
            isInitilized = true;
        }
    }
}

let firstLoadReg = register("tick", () => {
    if (isInSkyblock() && isDataLoaded()) {
        crownOverlay();
        firstLoadReg.unregister();
    }
})

registerWhen(register("tick", () => {
    if (hasCrown()) {
        if (timerOverlayLine) {
            if (data.hideTrackerLines.includes("timer")) {
                timerOverlayLine.button = true;
                timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
            } else {
                timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage()}`);
            }
        }
    }
}), () => settings.crownTracker);

registerWhen(register("step", () => {
    if (!hasCrown()) {
        crownTracker.renderGui = false;
    }
    else {
        crownTracker.renderGui = true
        cronwInitilization();
        crownOverlay();
    }
}).setFps(4), () => settings.crownTracker);

register("command", () => {
    data.totalCrownCoinsGained = 0;
    data.lastCrownCoins = 0;
    timerCrown.reset();
}).setName("sboresetcrowntracker")
