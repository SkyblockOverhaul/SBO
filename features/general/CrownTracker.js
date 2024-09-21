import settings from "../../settings";
import { registerWhen, timerCrown, timerCrownSession, data } from "../../utils/variables";
import { formatNumber, formatNumberCommas, formatTime, isInSkyblock, printDev, getMagicFind } from "../../utils/functions";
import { getZone, getWorld } from "../../utils/world";
import { isDataLoaded } from "../../utils/checkData";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";
import { YELLOW, BOLD, GOLD, DARK_GREEN, LIGHT_PURPLE, DARK_PURPLE, GREEN, DARK_GRAY, GRAY, WHITE, AQUA, ITALIC, BLUE, UNDERLINE, RED} from "../../utils/constants";

let crownTracker = new SboOverlay("crownTracker", "crownTracker", "render", "CrownLoc");
let timerOverlayLine = new OverlayTextLine(`&ePlaytime: &b${getTimerMessage()}`, true);

let crownTimers = [timerCrown, timerCrownSession];

function crownOverlay() {
    calculateCrownCoins();
    let messageLines = [];
    messageLines = getCrownMessage();
    messageLines.push(timerOverlayLine);
    crownTracker.setLines(messageLines);
}

const crownTiers = [1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000];

let profitPerHourSession = 0;
let ghostKillsSession = 0;
let sorrowDropsSession = 0;
let profitPerHour = 0;
let nextTier = 0;
let ghostsTillTier = 0;
let oneMillDropsSession = 0;
function getCrownMessage() {
    let crownLines = [];
    let timePassed = timerCrown.getHourTime();
    let timePassedSession = timerCrownSession.getHourTime();
    let timeUntilNextTier = 0;
    let currentTier = 0;
    let totalPerecent = 0;
    let percentToNextTier = 0;

    for (let i = 0; i < crownTiers.length; i++) {
        if (data.totalCrownCoins < crownTiers[i]) {
            currentTier = i - 1;
            break;
        }
    }
    if (currentTier < 0) currentTier = 0;
    nextTier = crownTiers[currentTier + 1];

    if (timePassed != "NaN" && timePassed != 0) {
        profitPerHour = (data.totalCrownCoinsGained / timePassed).toFixed();
    }
    if(timePassedSession != "NaN" && timePassedSession != 0) {
        profitPerHourSession = (data.totalCrownCoinsSession / timePassedSession).toFixed();
    }
    if (profitPerHour > 0 && nextTier > data.totalCrownCoins) {
        let coinsNeeded = nextTier - data.totalCrownCoins;
        let hoursUntilNextTier = coinsNeeded / profitPerHour;
        let millisecondsUntilNextTier = hoursUntilNextTier * 60 * 60 * 1000;
        timeUntilNextTier = formatTime(millisecondsUntilNextTier);
    }


    totalPerecent = (data.totalCrownCoins / crownTiers[crownTiers.length - 1]) * 100;
    percentToNextTier = (data.totalCrownCoins / nextTier) * 100;
    totalPerecent = totalPerecent.toFixed(2);
    percentToNextTier = percentToNextTier.toFixed(2);

    crownLines.push(new OverlayTextLine(`${YELLOW}${BOLD}Crown Tracker`, true));

    if (!hasCrown()) {
        crownLines.push(new OverlayTextLine(`${GOLD}Total Coins: ${AQUA}0`, true));
        crownLines.push(new OverlayTextLine(`${GOLD}Tracked Coins: ${AQUA}0`, true));
        crownLines.push(new OverlayTextLine(`${GOLD}Last Coins: ${AQUA}0`, true));
        crownLines.push(new OverlayTextLine(`${GOLD}Coins/hr: ${AQUA}0`, true));
        if (settings.crownGhostMode) {
            crownLines.push(new OverlayTextLine(`${GOLD}Ghosts/Tier: ${AQUA}0`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}Ghost Kills: ${AQUA}0`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}Sorrows: ${AQUA}0`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}1m drops: ${AQUA}0`, true));
        }
        crownLines.push(new OverlayTextLine(`${GOLD}0 in ${AQUA}0s`, true));
        crownLines.push(new OverlayTextLine(`${GOLD}Tier: ${AQUA}0`, true));
        return crownLines;
    }
    crownLines.push(new OverlayTextLine(`${GOLD}Total Coins: ${AQUA}${formatNumberCommas(data.totalCrownCoins)} &7(&b${totalPerecent}%&7)`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Tracked Coins: ${AQUA}${formatNumber(data.totalCrownCoinsGained)} &7(${formatNumber(data.totalCrownCoinsSession)})`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Last Coins: ${AQUA}${formatNumberCommas(data.lastCrownCoins)}`, true));
    crownLines.push(new OverlayTextLine(`${GOLD}Coins/hr: ${AQUA}${formatNumber(profitPerHour)} &7(${formatNumber(profitPerHourSession)})`, true));
    
    function nextTierMessage() {
        if (nextTier != crownTiers[crownTiers.length - 1]) {
            return new OverlayTextLine(`${GOLD}${formatNumber(nextTier)} in: ${AQUA}${timeUntilNextTier} &7(&b${percentToNextTier}%&7)`, true);
        } else {
            return new OverlayTextLine(`${GOLD}${formatNumber(nextTier)} in: ${AQUA}${timeUntilNextTier}`, true);
        }
    }

    if (settings.crownGhostMode) {
        if (getZone().includes("The Mist")) {
            crownLines.push(new OverlayTextLine(`${GOLD}Ghosts/Tier: ${AQUA}${formatNumberCommas(ghostsTillTier.toFixed())}`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}Ghost Kills: ${AQUA}${formatNumber(data.ghostKills)} &7(${formatNumber(ghostKillsSession)})`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}Sorrows: ${AQUA}${formatNumber(data.sorrowDrops)} &7(${formatNumber(sorrowDropsSession)})`, true));
            crownLines.push(new OverlayTextLine(`${GOLD}1m drops: ${AQUA}${formatNumber(data.crownOneMilCoins)} &7(${formatNumber(oneMillDropsSession)})`, true));
        }
    }

    if (timeUntilNextTier) {
        crownLines.push(nextTierMessage());
    } 
    else if (data.totalCrownCoinsGained == 0) {
        crownLines.push(new OverlayTextLine(`${GOLD}${formatNumber(nextTier)} in: ${AQUA}Unknown`, true));
    }
    else if (currentTier == crownTiers[crownTiers.length - 1]) {
        crownLines.push(new OverlayTextLine(`${GOLD}Tier: ${RED}MAX!`, true));
    }

    return crownLines;
}

function getTimerMessage(timer) {
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
        if (line == null || !line) continue;
        if (line.includes("Collected Coins")) {
            let coins = line.split(": ")[1];
            coins = coins.replace(/§./, "").replaceAll(",", "");
            coinsFound = parseInt(coins);
            break;
        }
    }
    return coinsFound;
}
let coinsBeforeCreeperDeath = 0;
if (data.totalCrownCoins > 0) coinsBeforeCreeperDeath = data.totalCrownCoins;
function calculateCrownCoins() {
    if (!hasCrown()) return;
    let coinsAfterCreeperDeath = getCoinsFromCrown();
    let coinsGained = coinsAfterCreeperDeath - coinsBeforeCreeperDeath;
    if (coinsGained > 0 && coinsGained != coinsAfterCreeperDeath) {
        printDev("[CTT] coins gained");
        for (let timer of crownTimers) {
            timer.start();
            timer.continue();
            timer.updateActivity();
        }
        data.totalCrownCoinsGained += coinsGained;
        data.totalCrownCoinsSession += coinsGained;
        data.lastCrownCoins = coinsGained;
        data.totalCrownCoins = coinsAfterCreeperDeath;
        coinsBeforeCreeperDeath = coinsAfterCreeperDeath;
        data.save();
    }
}

function hasCrown() {
    let helmet = Player.armor.getHelmet();
    if (!helmet) return false;
    let helmetLore = helmet.getLore();
    if (!helmetLore) return false;
    let helmetName = helmet.getName();
    if (!helmetName) return false;
    helmetName = helmetName.trim();
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

function countGhostKills(entity) {
    let name = entity.getName();
    if (!name) return;
    if (!name.includes("Creeper")) return;
    const distance = Math.sqrt(
        (Player.getX() - entity.getX())**2 +
        (Player.getY() - entity.getY())**2 +
        (Player.getZ() - entity.getZ())**2
    );
    if (distance > 10) return;
    calculateGhostsTillTier();
    data.ghostKills++;
    ghostKillsSession++;
    data.save();
}

let ghostCoinsSum = 0;
let maxListSize = 500;
let insertIndex = 0;
let ghostCoinsList = [];
let pushIndex = maxListSize / 10;
function calculateGhostsTillTier() {
    let ghostCoins = data.lastCrownCoins;
    let coinsNeeded = nextTier - data.totalCrownCoins;

    let ghostCoinsAVG = ghostCoinsList.length > 0 ? ghostCoinsSum / ghostCoinsList.length : 0;

    if ((ghostCoinsList.length < pushIndex || Math.abs(ghostCoins.toFixed() - ghostCoinsAVG.toFixed()) > 500) && ghostCoins < 1000000) {
        if (ghostCoinsList.length < maxListSize) {
            ghostCoinsList.push(ghostCoins);
            ghostCoinsSum += ghostCoins;
        } else {
            ghostCoinsSum -= ghostCoinsList[insertIndex];
            ghostCoinsList[insertIndex] = ghostCoins;
            ghostCoinsSum += ghostCoins;
            insertIndex = (insertIndex + 1) % maxListSize;
        }
    }

    ghostsTillTier = ghostCoinsAVG > 0 ? coinsNeeded / ghostCoinsAVG : 0;
}


let firsLoadCrown = register("tick", () => {
    crownOverlay();
    firsLoadCrown.unregister();
});

registerWhen(register("entityDeath", (entity) => {
    if (getZone().includes("The Mist")){
        countGhostKills(entity);
    }
}), () => settings.crownTracker && settings.crownGhostMode && getWorld() == "Dwarven Mines");

registerWhen(register("chat", (mf) => {
    let magicFind = getMagicFind(mf);
    data.sorrowDrops++;
    sorrowDropsSession++;
    data.save();
}).setCriteria("&r&6&lRARE DROP! &r&9Sorrow${mf}"), () => settings.crownTracker && settings.crownGhostMode);

registerWhen(register("chat", () => {
    data.crownOneMilCoins++;
    oneMillDropsSession++;
    data.save();
}).setCriteria("&r&eThe ghost's death materialized &r&61,000,000 coins &r&efrom the mists!&r"), () => settings.crownTracker && settings.crownGhostMode);

registerWhen(register("tick", () => {
    if (hasCrown()) {
        if (timerOverlayLine) {
            timerOverlayLine.setText(`&ePlaytime: &b${getTimerMessage(timerCrown)} &7(${getTimerMessage(timerCrownSession)})`);
        }
    }
}), () => settings.crownTracker);

registerWhen(register("step", () => {
    firsLoadCrown.register();
    if (!hasCrown() || !isDataLoaded() || !isInSkyblock()) {
        crownTracker.renderGui = false;
    }
    else {
        crownTracker.renderGui = true;
        cronwInitilization();
        crownOverlay();
    }
}).setFps(4), () => settings.crownTracker);

register("command", () => {
    data.totalCrownCoinsGained = 0;
    data.totalCrownCoinsSession = 0;
    data.totalCrownCoins = 0;
    data.lastCrownCoins = 0;
    data.ghostKills = 0;
    data.sorrowDrops = 0;
    ghostKillsSession = 0;
    sorrowDropsSession = 0;
    profitPerHour = 0;
    profitPerHourSession = 0;
    oneMillDropsSession = 0;
    data.crownOneMilCoins
    timerCrown.reset();
    timerCrownSession.reset();
    data.save();
}).setName("sboresetcrowntracker")

register("gameUnload", () => {
    data.totalCrownCoinsSession = 0;
    oneMillDropsSession = 0;
    profitPerHourSession = 0;
    ghostKillsSession = 0;
    sorrowDropsSession = 0;
    timerCrownSession.reset();
    data.save();
});
