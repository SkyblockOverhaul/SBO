import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase, initializeTracker } from '../../utils/functions';
import { itemOverlay, mobOverlay } from "../guis/DianaGuis";
import { isActiveForOneSecond } from "../../utils/functions";
import { getSkyblockDate, getNewMayorAtDate, getDateMayorElected, setDateMayorElected } from "../../utils/mayor";
import { trackerFileLocation, isDataLoaded } from "../../utils/checkData";
import { checkDiana } from "../../utils/checkDiana";

// todo: 
// bei dem tracker gui anzeigen was für ein tracker es ist

// burrow count: fertig vielleicht
// (1/4) start burrow
// (2/4), (3/4) treasure burrow or mob burrow
// (4/4) treasure burrow

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
    let rareDrops = ["&9DWARF_TURTLE_SHELMET", "&5CROCHET_TIGER_PLUSHIE", "&5ANTIQUE_REMEDIES", "&5MINOS_RELIC"];
    let countThisIds = ["ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW"]
    var checkBool = true;
    if (isActiveForOneSecond()) {
        if (checkDiana()) {
            for (var i in countThisIds.values()) {
                if (item === i) {
                    trackItem(item, "items", amount);
                    checkBool = false;
                }
            }
            if (checkBool) {
                for (var i in rareDrops.values()) {
                    color = i.slice(0, 2);
                    if (item == "MINOS_RELIC") {
                        Client.Companion.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
                    }
                    if (item === i.slice(2)) {
                        tempString = item.replace("_", " ").replace("_", " ").toLowerCase();
                        tempString = toTitleCase(tempString);
                        ChatLib.chat("&6[SBO] &r&6&lRARE DROP! " + color + tempString);
                        trackItem(item, "items", amount);
                    }
                }
            }
        }
    }
    else {
        let tempBool = true;
        for (var i in countThisIds.values()) {
            if (item === i) {
                ChatLib.chat(item + " not counted because no mod died in the last 2 seconds");
                tempBool = false;
            }
        }
        if (tempBool) {
            for (var i in rareDrops.values()) {
                if (item === i.slice(2)) {
                    ChatLib.chat(item + " not counted because no mod died in the last 2 seconds");
                }
            }
        }
    }
}

// save the tracker to json file //
function saveLoot(tracker, type) {
    FileLib.write(trackerFileLocation + type + ".json", JSON.stringify(tracker, null, 4));
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
    if (isDataLoaded()) {
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
}

// calc percent from tracker //
function calcPercent(trackerToCalc, type, setting) {
    if (setting == 2) {
        trackerToCalc = trackerToCalc[getDateMayorElected().getFullYear()];
    }
    percentDict = {};
    if(type == "mobs"){
        for (var mob in trackerToCalc["mobs"]) {
            percentDict[mob] = parseFloat((trackerToCalc["mobs"][mob] / trackerToCalc["mobs"]["TotalMobs"] * 100).toFixed(2));
        }
        return percentDict;
    }
    else {
        for (var obj in ["Minos Inquisitor", "Minos Champion", "Minotaur"].values()) {
            switch (obj) {
                case "Minos Inquisitor":
                    percentDict["Chimera"] = parseFloat((trackerToCalc["items"]["Chimera"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
                    break;
                case "Minos Champion":
                    percentDict["Minos Relic"] = parseFloat((trackerToCalc["items"]["MINOS_RELIC"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
                    break;
                case "Minotaur":
                    percentDict["Daedalus Stick"] = parseFloat((trackerToCalc["items"]["Daedalus Stick"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
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
        if (getSkyblockDate().getTime() >= getNewMayorAtDate().getTime()) {       
            setDateMayorElected("27.3." + (getSkyblockDate().getFullYear()));       
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

// total burrow tracker //
register("chat", (dug, chain, burrow) => {
    if (isDataLoaded()) {
        trackItem("Total Burrows", "items", 1);
    }
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r");

register("chat", (burrow) => {
    if (isDataLoaded()) {
        trackItem("Total Burrows", "items", 1);
    }
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}");
// &eYou finished the Griffin burrow chain!${burrow}
// &eYou dug out a Griffin Burrow!${burrow}

// mob tracker
registerWhen(register("chat", (woah, arev, mob) => {
    if (isDataLoaded()) {
        switch (mob) {
            case "Minos Inquisitor":
                trackItem(mob, "mobs", 1);
                break;
            case "Minos Champion":
                trackItem(mob, "mobs", 1);
                break;
            case "Minos Hunter":
                trackItem(mob, "mobs", 1);
                break;
            case "Minotaur":
                trackItem(mob, "mobs", 1);
                break;
            case "Gaia Construct":
                trackItem(mob, "mobs", 1);
                break;
            case "Siamese Lynxes":
                trackItem(mob, "mobs", 1);
                break;       
        }
    }
}).setCriteria("&r&c&l${woah} &r&eYou dug ${arev}&r&2${mob}&r&e!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker && isInSkyblock());


// track items from chat //
registerWhen(register("chat", (drop) => {
    if (isDataLoaded()) {
        drop=drop.slice(2);
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
}).setCriteria("&r&6&lRARE DROP! &r&eYou dug out a &r${drop}&r&e!&r"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (coins) => {
    if (isDataLoaded()) {
        var coins2 = parseInt(coins.replace(",", ""))
        trackItem("coins", "items", coins2);
    }
}).setCriteria("&r&6&lWow! &r&eYou dug out &r&6${coins} coins&r&e!&r"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (drop) => {
    if (isDataLoaded() && checkDiana()) {
        drop = drop.slice(2, 16); // 8 statt 16 für potato und carrot
        switch (drop) {
            case "Enchanted Book":
                Client.Companion.showTitle(`&d&lChimera!`, "", 0, 25, 35);
                trackItem("Chimera", "items", 1);
                break;
            case "Daedalus Stick":
                Client.Companion.showTitle(`&6&lDaedalus Stick!`, "", 0, 25, 35);
                trackItem(drop, "items", 1);
                break;
            // case "Potato":
            //     Client.Companion.showTitle(`&d&lChimera!`, "", 0, 25, 35);
            //     trackItem(drop, "items", 1);
            //     break;
            // case "Carrot":
            //     Client.Companion.showTitle(`&6&lDaedalus Stick!`, "", 0, 25, 35);
            //     trackItem(drop, "items", 1);
            //     break;
        }
    }
}).setCriteria("&r&6&lRARE DROP! &r${drop}"), () => getWorld() === "Hub" && isInSkyblock() && settings.dianaLootTracker);
// Party > [MVP++] LHxSeven: &r&6&lRARE DROP! &r&6Daedalus Stick &r&b(+&r&b322% &r&b✯ Magic Find&r&b)&r
// Party > [MVP++] LHxSeven: &r&6&lRARE DROP! &r&fEnchanted Book&r
// &r&6&lRARE DROP! &r&fEnchanted Book &r&b(+&r&b348% &r&b✯ Magic Find&r&b)&r

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
    tempSettingLoot = settings.dianaLootTrackerView;
    refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
}).setFps(1), () => settings.dianaLootTracker && tempSettingLoot !== settings.dianaLootTrackerView);

let tempSettingMob = -1;
registerWhen(register("step", () => {
    tempSettingMob = settings.dianaMobTrackerView;
    refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
}).setFps(1), () => settings.dianaMobTracker && tempSettingMob !== settings.dianaMobTrackerView);

let firstLoad = false;
registerWhen(register("step", () => {
    if (isDataLoaded()) {
        refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
        refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
        firstLoad = true;
    }
}).setFps(1), () => !firstLoad);


// // test command
// register('command', () => {
//     trackerSession = getTracker(3);
//     for (var item in trackerSession["items"]) {
//         ChatLib.chat(item + ": " + trackerSession["items"][item]);
//     }
// }).setName("sbots");
// register('command', () => {
//     trackerMayor = getTracker(2);
//     for (var item in trackerMayor["items"]) {
//         ChatLib.chat(item + ": " + trackerMayor["items"][item]);
//     }
// }).setName("sbotm");

// register('command', () => {
//     trackerTotal = getTracker(1);
//     for (var item in trackerTotal["items"]) {
//         ChatLib.chat(item + ": " + trackerTotal["items"][item]);
//     }
// }).setName("sbott");

// registerWhen(register("chat", () => {
//     if (isDataLoaded()) {
//         trackItem("Total Burrows", "items", 1);
//     }
// }).setCriteria("&e[NPC] Lumber Jack&f: &r&fA lumberjack always pays his debts!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker && isInSkyblock());
