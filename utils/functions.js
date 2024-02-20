
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
/**
 * 
 * @param {boolean} entityDeathOccurred 
 */
export function isActiveForOneSecond(entityDeathOccurred) {
    return entityDeathOccurred;
}
register("step", () => {
    inSkyblock = checkIfInSkyblock();
}).setFps(1);



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

export function trackItem(item, category, amount) {
    trackOne(trackerMayor, item, category, "Mayor", amount);
    trackOne(trackerTotal, item, category, "Total", amount);
    trackOne(trackerSession, item, category, "Session", amount);
}

function trackOne(tracker, item, category, type, amount) {
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