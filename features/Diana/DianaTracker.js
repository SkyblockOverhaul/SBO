import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase } from '../../utils/functions';
import { itemOverlay, mobOverlay } from "../guis/DianaGuis";
import { isActiveForOneSecond } from "../../utils/functions";
import { getSkyblockDate, getNewMayorAtDate, getDateMayorElected, setDateMayorElected } from "../../utils/mayor";
import { trackerFileLocation, initializeTracker, isDataLoaded } from "../../utils/checkData";

// todo: 

// todo end

// load loot tracker from json file //
function loadTracker(type) {
    let loadedTracker = {};
    try {
        loadedTracker = JSON.parse(FileLib.read(trackerFileLocation + type + ".json")) || {};
    } catch (e) {
        loadedTracker = {};
    }
    return loadedTracker;
}

// track items with pickuplog //

export function dianaLootCounter(item, amount) {
    let rareDrops = ["&9DWARF_TURTLE_SHELMET", "&5CROCHET_TIGER_PLUSHIE", "&5ANTIQUE_REMEDIES", "&5MINOS_RELIC", "&5ROTTEN_FLESH"];
    let countThisIds = ["ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW"]
    var checkBool = true;
    if (isActiveForOneSecond()) {
        for (var i in countThisIds.values()) {
            if (item === i) {
                trackItem(item, "items", amount);
                checkBool = false;
            }
        }
        if (checkBool) {
            for (var i in rareDrops.values()) {
                color = i.slice(0, 2);
                if (item === i.slice(2)) {
                    tempString = toTitleCase(item.replace("_", " ").toLowerCase());
                    ChatLib.chat("&6[SBO] &r&6&lRARE DROP! " + color + tempString);
                    trackItem(item, "items", amount);
                }
            }
        }
    }
}

// save the tracker to json file //
function saveLoot(tracker, type) {
    FileLib.write(trackerFileLocation + type + ".json", JSON.stringify(tracker));
}

// get tracker by setting (0: default, 1: total, 2: event, 3: event) //
export function getTracker(setting) {
    switch (setting) {
        case 1:
            return trackerTotal;
        case 2:
            return trackerMayor;
        case 3:
            return trackerSession;
    }
}

// refresh overlay (items, mobs) //
function refreshOverlay(tracker, setting, category) {
    if (setting != 0 ) {
        percentDict = calcPercent(tracker, category, setting);
        if (category === "items") {
            itemOverlay(tracker, setting, percentDict);
        }
        else {
            mobOverlay(tracker, setting, percentDict);
        }
    }
}

// calc percent from tracker //
function calcPercent(trackerToCalc, type, setting) {
    if (setting == 2) {
        trackerToCalc = trackerToCalc[getDateMayorElected().getFullYear()];
    }
    percentDict = {};
    if(type == "mobs"){
        for (var mob in trackerToCalc["mobs"]) {
            percentDict[mob] = Math.round((trackerToCalc["mobs"][mob] / trackerToCalc["mobs"]["TotalMobs"]) * 100);
        }
        return percentDict;
    }
    else {
        for (var obj in ["Minos Inquisitor", "Minos Champion", "Minotaur"].values()) {
            switch (obj) {
                case "Minos Inquisitor":
                    percentDict["Chimera"] = Math.round((trackerToCalc["items"]["Chimera"] / trackerToCalc["mobs"][obj]) * 100);
                    break;
                case "Minos Champion":
                    percentDict["Minos Relic"] = Math.round((trackerToCalc["items"]["MINOS_RELIC"] / trackerToCalc["mobs"][obj]) * 100);
                    break;
                case "Minotaur":
                    percentDict["Daedalus Stick"] = Math.round((trackerToCalc["items"]["Daedalus Stick"] / trackerToCalc["mobs"][obj]) * 100);
                    break;
            }
        }
        return percentDict;
    }
}

// track logic //
export function trackItem(item, category, amount) {
    trackOne(trackerMayor, item, category, "Mayor", amount);
    trackOne(trackerSession, item, category, "Session", amount);
    trackOne(trackerTotal, item, category, "Total", amount);

    refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
    refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
}


function trackOne(tracker, item, category, type, amount) {
    if (type == "Mayor") {
        if (((getSkyblockDate().getTime() / 1000) > (getNewMayorAtDate().getTime() / 1000))) {       
            ChatLib.chat("new mayor now?: " + ((getSkyblockDate().getTime() / 1000) > (getNewMayorAtDate().getTime() / 1000)));
            setDateMayorElected("27.3." + (getSkyblockDate().getFullYear() + 1));
            tracker[getDateMayorElected().getFullYear()] = initializeTracker();
        }
        tracker[getDateMayorElected().getFullYear()][category][item] += amount;
        if (category === "mobs") {
            tracker[getDateMayorElected().getFullYear()]["mobs"]["TotalMobs"] += amount;
        }
    }
    else {
        tracker[category][item] += amount;
        if (category === "mobs") {
            tracker["mobs"]["TotalMobs"] += amount;
        }
    }
    if (type !== "Session") {
        saveLoot(tracker, type);
    }
}

// mob tracker
registerWhen(register("chat", (woah, mob) => {
    if (isDataLoaded()) {
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
    }
}).setCriteria("&c${woah}&eYou Dug out &2a ${mob}&e!"), () => getWorld() === "Hub" && settings.dianaMobTracker && isInSkyblock());


// track items from chat //
registerWhen(register("chat", (drop) => {
    if (isDataLoaded()) {
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
    }
}).setCriteria("&r&6&lRARE DROP! &eYou dug out a ${drop}&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (coins) => {
    if (isDataLoaded()) {
        trackItem("coins", "items", coins);
    }
}).setCriteria("&r&6&lRARE DROP! &eYou dug out &6${coins} coins&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (drop) => {
    drop = drop.slice(0, 14); // 6 statt 14 für potato und carrot
    if (isDataLoaded()) {
        switch (drop) {
            case "Enchanted Book":
                trackItem("Chimera", "items", 1);
                break;
            case "Daedalus Stick":
                trackItem(drop, "items", 1);
                break;
            // case "Potato":
            //     trackItem(drop, "items", 1);
            //     break;
            // case "Carrot":
            //     trackItem(drop, "items", 1);
            //     break;
        }
    }
}).setCriteria("&r&6&lRARE DROP! &r&f${drop}"), () => getWorld() === "Hub" && isInSkyblock() && settings.dianaLootTracker);
// &r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r

// mayor tracker //
let trackerMayor = loadTracker("Mayor");
let trackerBool = false;
registerWhen(register("step", () => {
    if (isDataLoaded()) {
        if (getDateMayorElected() != undefined) {
            if (!trackerMayor.hasOwnProperty(getDateMayorElected().getFullYear())) {
            trackerMayor[getDateMayorElected().getFullYear()] = initializeTracker();
            }
            else {
                trackerBool = true;
            }
        }
        else {
            ChatLib.chat("No date for mayor election found (undefined)");
        }
    }
}).setFps(1), () => !trackerBool);
// total tracker //
let trackerTotal = loadTracker("Total");
if (Object.keys(trackerTotal).length == 0) {
    trackerTotal = initializeTracker();
}
// session tracker //
let trackerSession = {};
trackerSession = initializeTracker();

// refresh overlay //
let tempSettingLoot = -1;
registerWhen(register("step", () => {
    if (isDataLoaded()) {
        tempSettingLoot = settings.dianaLootTrackerView;
        refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
    }
}).setFps(1), () => settings.dianaLootTracker && tempSettingLoot !== settings.dianaLootTrackerView);

let tempSettingMob = -1;
registerWhen(register("step", () => {
    if (isDataLoaded()) {
        tempSettingMob = settings.dianaMobTrackerView;
        refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
    }
}).setFps(1), () => settings.dianaMobTracker && tempSettingMob !== settings.dianaMobTrackerView);

let firstLoad = false;
registerWhen(register("step", () => {
    if (isDataLoaded()) {
        refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
        refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
        firstLoad = true;
    }
}).setFps(1), () => !firstLoad);


// test command
register('command', () => {
    trackerSession = getTracker(3);
    for (var item in trackerSession["items"]) {
        ChatLib.chat(item + ": " + trackerSession["items"][item]);
    }
}).setName("sbots");
register('command', () => {
    trackerMayor = getTracker(2);
    for (var item in trackerMayor["items"]) {
        ChatLib.chat(item + ": " + trackerMayor["items"][item]);
    }
}).setName("sbotm");

register('command', () => {
    trackerTotal = getTracker(1);
    for (var item in trackerTotal["items"]) {
        ChatLib.chat(item + ": " + trackerTotal["items"][item]);
    }
}).setName("sbott");

registerWhen(register("chat", () => {
    if (isDataLoaded()) {
        trackItem("Minos Inquisitor", "mobs", 1);
    }
}).setCriteria("&e[NPC] Lumber Jack&f: &r&fA lumberjack always pays his debts!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker && isInSkyblock());
