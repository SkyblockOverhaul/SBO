import settings from "../../settings";
import { registerWhen, timerCrown, data } from "../../utils/variables";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE} from "../../utils/constants";



let crownTracker = new SboOverlay("crownTracker", "crownTracker", "render", "CrownLoc");

function crownOverlay() {
    let messageLines = [];
    messageLines = getCrownMessage();
    crownTracker.setLines(messageLines);
}

function getCrownMessage() {
    let cronwLines = [];
    cronwLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Crown Tracker`, true));
    return cronwLines;
}

function getCoinsFromCrown() {
    if (Player.armor.getHelmet().getLore() == null || !Player.armor.getHelmet().getLore()) return 0;
    let helmetLore = Player.armor.getHelmet().getLore();
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
        coinsBeforeCreeperDeath = coinsAfterCreeperDeath;
        data.totalCrownCoins += coinsGained;
        data.lastCrownCoins = coinsGained;
    }
}

registerWhen(register("step", () => {
    crownOverlay();
}).setFps(4), () => settings.crownTracker);

register("command", () => {

}).setname("sboresetcrowntracker")