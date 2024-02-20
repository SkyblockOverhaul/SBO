import { request } from "../../requestV2";
import { registerWhen } from "./variables";

export function getClosest(origin, positions) {
    let closestPosition = positions.length > 0 ? positions[0] : [0, 0, 0];
    let closestDistance = 999;
    let distance = 999;

    positions.forEach(position => {
        distance = Math.hypot(origin[1] - position[1], origin[2] - position[2], origin[3] - position[3]);
        if (distance < closestDistance) {
            closestDistance = distance;
            closestPosition = position;
        }
    });

    return [closestPosition, closestDistance];
};
export function convertToPascalCase(input) {
    if (!input) return; 

    return input
        .split(" ")
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join("");
}
/** 
 * @param {string} chat
 * @param {string} mob
 * @param {number} x
 * @param {number} y
 * @param {number} z
 */
export function mobAnnouncement(chat,mob,x,y,z){
    x = Math.round(x);
    y = Math.round(y);
    z = Math.round(z);

    let zone = Scoreboard.getLines().find(line => line.getName().includes("⏣"));
    if(zone === undefined) zone = Scoreboard.getLines().find(line => line.getName().includes("ф"));
    const area = zone === undefined ? "None" : zone.getName().removeFormatting();

    ChatLib.command(`pc x: ${x} y: ${y} z: ${z} | ${mob} found at ${area}!`);
}

export function getSBID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("id") || null;
}
export function getSBUUID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("uuid") || null;
}

export function checkIfInSkyblock() {
    return Scoreboard.getTitle()?.removeFormatting().includes("SKYBLOCK");
}

let inSkyblock = false;

export function isInSkyblock() {
    return inSkyblock;
}
export let state = {
    entityDeathOccurred: false
}
export function isActiveForOneSecond() {
    return state.entityDeathOccurred;
}
register("step", () => {
    inSkyblock = checkIfInSkyblock();
}).setFps(1);


////// mayor and date //////
let mayor = undefined;
let year = 0;
let skyblockDate = undefined;
let skyblockDateString = "";
let perks = new Set([]);
let lastMonth = 0;
let dateMayorElected = undefined;
let newMayorAtDate = undefined;

register("worldLoad", () => {
    year, mayor = getYearMayorRequestV2();
});

register("step", () => {
    skyblockDateString, skyblockDate = convertDate(Scoreboard.getLines()[7]);
    ChatLib.chat(skyblockDateString);
}).setFps(1);

/**
 * Gets the array of mayor's perks.
 *
 * @returns {string[]} - An array containing the names of the mayor's perks.
 */
export function getPerks() {
    return perks;
}

export function getMayor() {
    return mayor;
}

export function getYear() {
    return year;
}

/**
 * @returns {date} - skyblock date
 */
export function getSkyblockDate() {
    return skyblockDate;
}

export function getSkyblockDateString() {
    return skyblockDateString;
}


export function checkDiana() {
    mayor = getMayor();
    perks = getPerks();
    return getWorld() === "Hub" && mayor === "Diana" && perks.has("Mythological Ritual");
}


function getYearMayorRequestV2() {
    request({
        url: "https://api.hypixel.net/resources/skyblock/election",
        json: true
    }).then((response)=>{
        mayor = response.mayor.name;
        dateMayorElected = convertStringToDate("27.3." + (response.mayor.election.year + 1));
        newMayorAtDate = convertStringToDate("27.3." + (response.mayor.election.year + 2));
        year = response.current.year;
        perks = new Set([...response.mayor.perks.map(perk => perk.name)]);
    }).catch((error)=>{
        console.error(error);
    });

    return year, mayor;
}

function convertDate(dateStr) {
    if (dateStr === undefined || dateStr == "") return "";
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
    
    if (lastMonth == 12 && month == 1) {  // if the last month was December, and the current month is January, then the year has changed
        year++;
    }
    else {
        year = getYear();
    }
    skyblockDateString = day + '.' + month + '.' + year;
    skyblockDate = convertStringToDate(skyblockDateString);
    lastMonth = month;
    return skyblockDateString, skyblockDate;
}

////// diana //////
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


export function dianaLootCounter(item, amount) {
    countThisIds = ["ROTTEN_FLESH", "WOOD"]
    ChatLib.chat("counting " + item);
    ChatLib.chat("active " + isActiveForOneSecond());
    if (isActiveForOneSecond()) {
        for (var i in countThisIds.values()) {
            if (item === i) {
                trackItem(item, "items", amount);
            }
        }
    }
}

function saveLoot(tracker, type) {
    FileLib.write(fileLocation + type + ".json", JSON.stringify(tracker));
}

export function getTracker(type) {
    switch (type) {
        case "Mayor":
            return trackerMayor;
        case "Total":
            return trackerTotal;
        case "Session":
            return trackerSession;
    }
}

let trackerMayor = loadTracker("Mayor");
let trackerTotal = loadTracker("Total");
let trackerSession = {};


// mayor tracker
if (!trackerMayor.hasOwnProperty('items')) {
    trackerMayor.items = {};
}
if (!trackerMayor.hasOwnProperty('mobs')) {
    trackerMayor.mobs = {};
}
if (!trackerMayor.hasOwnProperty('election')) {
    trackerMayor.election = dateMayorElected;
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

export function trackItem(item, category, amount) {
    trackOne(trackerMayor, item, category, "Mayor", amount);
    trackOne(trackerTotal, item, category, "Total", amount);
    trackOne(trackerSession, item, category, "Session", amount);
}

function convertStringToDate(str) {
    var parts = str.split(".");
    var date = new Date(parts[2], parts[1] - 1, parts[0]);
    return date;
}

register("command", () => {
    ChatLib.chat("new Mayor at: " + newMayorAtDate);
    ChatLib.chat("last Mayor elected: " + dateMayorElected);
    ChatLib.chat("current date: " + skyblockDate);
    ChatLib.chat("new mayor now?: " + ((skyblockDate.getTime() / 1000) > (newMayorAtDate.getTime() / 1000)))
}).setName("sbottt");

function trackOne(tracker, item, category, type, amount) {
    if (type == "Mayor") {
        if (((skyblockDate.getTime() / 1000) > (newMayorAtDate.getTime() / 1000))) {
            ChatLib.chat("new mayor now?: " + ((skyblockDate.getTime() / 1000) > (newMayorAtDate.getTime() / 1000)));
            tracker = {};
            tracker.items = {};
            tracker.mobs = {};
            tracker.election = dateMayorElected;
        }
    }
    if (tracker[category][item]) {
        tracker[category][item] += amount;
    }
    else {
        tracker[category][item] = amount;
    }
    if (type !== "Session") {
        saveLoot(tracker, type);
    }
}