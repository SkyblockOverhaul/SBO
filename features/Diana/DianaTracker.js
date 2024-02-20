import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock } from '../../utils/functions.js';
import axios from "./../../../axios"


// mob tracker
registerWhen(register("chat", (woah, mob) => {
    switch (mob) {
        case "Minos Inquisitor":
            trackMob(mob, "mobs", 1);
            break;
        case "Minos Champion":
            trackMob(mob, "mobs", 1);
            break;
        case "Minos Hunter":
            trackMob(mob, "mobs", 1);
            break;
        case "Minotaur":
            trackMob(mob, "mobs", 1);
            break;
        case "Gaia Construct":
            trackMob(mob, "mobs", 1);
            break;
        case "Siamese Lynx":
            trackMob(mob, "mobs", 1);
            break;       
    }
}).setCriteria("&c${woah}&eYou Dug out &2a ${mob}&e!"), () => getWorld() === "Hub" && settings.dianaMobTracker);

registerWhen(register("chat", () => {
    trackItem("Minos Inquisitor", "mobs", 1);
}).setCriteria("&e[NPC] Lumber Jack&f: &r&fA lumberjack always pays his debts!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker);

function trackItem(item, category, amount) {
    trackOne(trackerMayor, item, category, "Mayor");
    trackOne(trackerTotal, item, category, "Total");
    trackOne(trackerSession, item, category, "Session");
}

function trackOne(tracker, item, category, type) {
    if (tracker[category][item]) {
        tracker[category][item] += 1;
    }
    else {
        tracker[category][item] = 1;
    }
    if (type !== "Session") {
        saveLoot(tracker, type);
    }
}

// loot tracker
fileLocation = "config/ChatTriggers/modules/SBO/dianaTracker";
function loadTracker(type) {
    let loadedTracker;
    try {
        loadedTracker = JSON.parse(FileLib.read(fileLocation + type + ".json")) || {};
    } catch (e) {
        loadedTracker = {};
    }
    return loadedTracker;
}

function saveLoot(tracker, type) {
    FileLib.write(fileLocation + type + ".json", JSON.stringify(tracker));
}

year = 0;
function getYear() { // not working
    if (year === 0) {
        try {
            const response = axios.get('https://api.hypixel.net/v2/resources/skyblock/election');
            year = response.data;
            console.log(year);
            return year;
        } catch (error) {
            console.error("Error fetching year:", error);
            return null;
        }
    } else {
        return year;
    }
}


function convertDate(dateStr) {
    var seasonToMonth = {
        'Early Spring': '1',
        "Spring": '2',
        "Late Spring": '3',
        "Early Summer": '4',
        "Summer": '5',
        "Late Summer": '6',
        "Early Autumn": '7',
        "Autumn": '8',
        "Late Autumn": '9',
        "Early Winter": '10',
        "Winter": '11',
        "Late Winter": '12'
    };

    dateStr = dateStr.toString().replace(/[^a-z0-9\s-]/gi, '');
    var parts = dateStr.split(' ');
    if (parts.length === 4) {
        season = parts[1] + ' ' + parts[2];
        day = parts[3];
    }
    else {
        season = parts[1];
        day = parts[2];
    }
    
    day = day.replace(/(st|nd|rd|th)$/, '');

    var month = seasonToMonth[season] || '';
    year = getYear();
    date = day + '.' + month + '.' + year;
    return date
}


register("command", () => {
    skyblockDate = convertDate(Scoreboard.getLines()[7]);
    ChatLib.chat(skyblockDate);
}).setName("sbodate");

export var trackerMayor = loadTracker("Mayor");
export var trackerTotal = loadTracker("Total");
export var trackerSession = {};

// mayor tracker
if (!trackerMayor.hasOwnProperty('items')) {
    trackerMayor.items = {};
}
if (!trackerMayor.hasOwnProperty('mobs')) {
    trackerMayor.mobs = {};
}
if (!trackerMayor.hasOwnProperty('election')) {
    trackerMayor.election = {};
}
// total tracker
if (!trackerTotal.hasOwnProperty('items')) {
    trackerTotal.items = {};
}
if (!trackerTotal.hasOwnProperty('mobs')) {
    trackerTotal.mobs = {};
}
// session tracker
if (!trackerSession.hasOwnProperty('items')) {
    trackerSession.items = {};
}
if (!trackerSession.hasOwnProperty('mobs')) {
    trackerSession.mobs = {};
}

export function dianaLootCounter(item, amount) {
    countThisIds = ["ROTTEN_FLESH", "WOOD"]
    for (var i in countThisIds.values()) {
        if (item === i) {
            trackItem(item, "items", amount);
        }
    }
}

registerWhen(register("chat", (drop) => {
    drop = drop.removeFormatting();
    switch (drop) {
        case "Griffin Feather":
            trackItem(drop, "items", 1);
            break;
        case "Crown of Greed":
            trackItem(drop, "items", 1);
            break;
        case "Washed-up Souvenir":
            trackItem(drop, "items", 1);
            break;
    }
}).setCriteria("&r&6&lRARE DROP! &eYou dug out a ${drop}&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (coins) => {
    trackItem("coins", "items", coins);
    ChatLib.chat(coins);
}).setCriteria("&r&6&lRARE DROP! &eYou dug out &6${coins} coins&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (drop, mf) => {
    switch (drop) {
        case "Enchanted Book":
            trackItem("Chimera", "items", 1);
            break;
        case "Daedalus Stick":
            trackItem(drop, "items", 1);
            break;
        case "Potato":
            trackItem(drop, "items", 1);
            break;
        case "Carrot":
            trackItem(drop, "items", 1);
            break;
    }
}).setCriteria("&r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());


// test command
register('command', () => {
    for (var item in trackerSession["items"]) {
        ChatLib.chat(item + ": " + trackerSession["items"][item]);
    }
}).setName("sbots");
register('command', () => {
    for (var item in trackerMayor["items"]) {
        ChatLib.chat(item + ": " + trackerMayor["items"][item]);
    }
}).setName("sbotm");
register('command', () => {
    for (var item in trackerTotal["items"]) {
        ChatLib.chat(item + ": " + trackerTotal["items"][item]);
    }
}).setName("sbott");

// if (getSBUUID(playerInvItems[i]) === null) {
//     if (playerItems[getSBID(playerInvItems[i])]) {
//         playerItems[getSBID(playerInvItems[i])] += playerInvItems[i].getStackSize();
//     }
//     else {
//         playerItems[getSBID(playerInvItems[i])] = playerInvItems[i].getStackSize();
//     }
// }
// else {
//     playerItems[getSBUUID(playerInvItems[i])] = playerInvItems[i].getStackSize();
// }