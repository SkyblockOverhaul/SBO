import settings from "../../settings";
import { registerWhen, timerCrown, data } from "../../utils/variables";
import { formatNumber, formatNumberCommas, formatTime } from "../../utils/functions";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";

let crownTracker = new SboOverlay("crownTracker", "crownTracker", "render", "CrownLoc");
let timerOverlayLine = new OverlayTextLine(`&ePlaytime: &b${getTimerMessage()}`, true);

function crownOverlay() {
    calculateCrownCoins();
    let messageLines = [];
    messageLines = getCrownMessage();
    messageLines.push(timerOverlayLine);
    crownTracker.setLines(messageLines);
}

function getCrownMessage() {
    let cronwLines = [];
    let timePassed = timerCrown.getHourTime();
    let profitPerHour = 0;
    if (timePassed != "NaN" && timePassed != 0) {
        profitPerHour = (data.totalCrownCoins / timePassed).toFixed();
    }
    cronwLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Crown Tracker`, true));
    cronwLines.push(new OverlayTextLine(`${GOLD}Tracked Coins: ${AQUA}${formatNumber(data.totalCrownCoins)}`, true));
    cronwLines.push(new OverlayTextLine(`${GOLD}Last Coins: ${AQUA}${formatNumber(data.lastCrownCoins)}`, true));
    cronwLines.push(new OverlayTextLine(`${GOLD}Coins/hr: ${AQUA}${formatNumber(profitPerHour)}`, true));

    return cronwLines;
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
    if (!Player.armor.getHelmet()) return 0;
    if (!Player.armor.getHelmet().getLore()) return 0;
    let helmet = Player.armor.getHelmet();
    let helmetName = helmet.getName().trim();
    if (!helmetName.includes("Crown of Avarice")) return 0
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
    if (coinsGained > 0) {
        timerCrown.start();
        timerCrown.continue();
        timerCrown.updateActivity();
        coinsBeforeCreeperDeath = coinsAfterCreeperDeath;
        data.totalCrownCoins += coinsGained;
        data.lastCrownCoins = coinsGained;
    }
}

registerWhen(register("tick", () => {
    if (timerOverlayLine) {
        if (data.hideTrackerLines.includes("timer")) {
            timerOverlayLine.button = true;
            timerOverlayLine.setText("&7&m" + timerOverlayLine.text.getString().removeFormatting());
        } else {
            timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage()}`);
        }
    }
}), () => settings.crownTracker);

registerWhen(register("step", () => {
    crownOverlay();
}).setFps(4), () => settings.crownTracker);

register("command", () => {
    data.totalCrownCoins = 0;
    data.lastCrownCoins = 0;
    timerCrown.reset();
}).setName("sboresetcrowntracker")
