import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase, initializeTracker, gotLootShare } from '../../utils/functions';
import { itemOverlay, mobOverlay, mythosMobHpOverlay } from "../guis/DianaGuis";
import { isActiveForOneSecond } from "../../utils/functions";
import { getSkyblockDate, getNewMayorAtDate, getDateMayorElected, setDateMayorElected, setNewMayorBool } from "../../utils/mayor";
import { trackerFileLocation, isDataLoaded, getTrackerTotal, getTrackerMayor, getTrackerSession } from "../../utils/checkData";
import { checkDiana } from "../../utils/checkDiana";
import { getGuiOpen, getRefreshOverlays } from "../../utils/overlays";

// todo: 

// todo end


// track items with pickuplog //
export function dianaLootCounter(item, amount) {
    let rareDrops = ["&9DWARF_TURTLE_SHELMET", "&5CROCHET_TIGER_PLUSHIE", "&5ANTIQUE_REMEDIES", "&5MINOS_RELIC"]; //  "&5ROTTEN_FLESH"
    let countThisIds = ["ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW"]
    let checkBool = true;
    if (isActiveForOneSecond() || gotLootShare()) {
        if (checkDiana()) {
            for (let i in countThisIds.values()) {
                if (item === i) {
                    trackItem(item, "items", amount);
                    checkBool = false;
                }
            }
            if (checkBool) {
                for (let i in rareDrops.values()) {
                    color = i.slice(0, 2);
                    if (item == "MINOS_RELIC") {
                        if (settings.lootAnnouncerScreen) {
                            Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
                        }
                    }
                    if (item === i.slice(2)) {
                        tempString = item.replace("_", " ").replace("_", " ").toLowerCase();
                        tempString = toTitleCase(tempString);
                        if (settings.copyRareDrop) {
                            let finalText = "[SBO] RARE DROP! " + tempString;
                            ChatLib.command(`ct copy ${finalText}`, true);
                            ChatLib.chat("§6[SBO] §eCopied Rare Drop Message!§r");
                        }
                        if (settings.lootAnnouncerChat) {
                            ChatLib.chat("&6[SBO] &r&6&lRARE DROP! " + color + tempString);
                        }
                        if (settings.dianaLootTracker) {
                            trackItem(item, "items", amount);
                        }
                    }
                }
            }
        }
    }
}

// save the tracker to json file //
function saveLoot(tracker, type) {
    FileLib.write("SBO", trackerFileLocation  + type + ".json", JSON.stringify(tracker, null, 4));
}

// get tracker by setting (0: default, 1: total, 2: event, 3: event) //
let trackerTotal = {};
let trackerMayor = {};
let trackerSession = {};
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

export function setTracker(setting, tracker) {
    switch (setting) {
        case 1:
            trackerTotal = tracker;
            break;
        case 2:
            trackerMayor = tracker;
            break;
        case 3:
            trackerSession = tracker;
            break;
    }
}

// refresh overlay (items, mobs) //
function refreshOverlay(tracker, setting, category) {
    if (isDataLoaded()) {
        if (setting != 0 ) {
            percentDict = calcPercent(tracker, category, setting);
            if (percentDict == undefined) return;
            if (tracker == undefined) return;
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
    if (trackerToCalc == undefined) return;
    percentDict = {};
    if(type == "mobs"){
        for (let mob in trackerToCalc["mobs"]) {
            percentDict[mob] = parseFloat((trackerToCalc["mobs"][mob] / trackerToCalc["mobs"]["TotalMobs"] * 100).toFixed(2));
        }
        return percentDict;
    }
    else {
        for (let obj in ["Minos Inquisitor", "Minos Champion", "Minotaur"].values()) {
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
    if (isDataLoaded()) {
        trackOne(trackerMayor, item, category, "Mayor", amount);
        trackOne(trackerSession, item, category, "Session", amount);
        trackOne(trackerTotal, item, category, "Total", amount);

        refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
        refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
    }
}

function trackOne(tracker, item, category, type, amount) {
    if (type == "Mayor") {
        if (getSkyblockDate() >= getNewMayorAtDate()) {    
            setNewMayorBool();   
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
    saveLoot(tracker, type);
}

// command to reset session tracker
register("command", () => {
    trackerSession = initializeTracker();
    saveLoot(trackerSession, "Session");
}).setName("sboresetsession");
    

// total burrow tracker //
register("chat", (burrow) => {
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
    if (isDataLoaded() && isInSkyblock()) {
        switch (mob) {
            case "Minos Inquisitor":
            case "Minos Champion":
            case "Minos Hunter":
            case "Minotaur":
            case "Gaia Construct":
            case "Siamese Lynxes":
                trackItem(mob, "mobs", 1);
                break;       
        }
    }
}).setCriteria("&r&c&l${woah} &r&eYou dug ${arev}&r&2${mob}&r&e!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker);


// track items from chat //
registerWhen(register("chat", (drop) => {
    if (isDataLoaded() && isInSkyblock()) {
        drop=drop.slice(2);
        switch (drop) {
            case "Griffin Feather":
            case "Crown of Greed":
            case "Washed-up Souvenir":
                trackItem(drop, "items", 1);
                break;
        }
    }
}).setCriteria("&r&6&lRARE DROP! &r&eYou dug out a &r${drop}&r&e!&r"), () => getWorld() === "Hub" && settings.dianaLootTracker);

registerWhen(register("chat", (coins) => {
    if (isDataLoaded() && isInSkyblock()) {
        let coins2 = parseInt(coins.replace(",", ""))
        trackItem("coins", "items", coins2);
    }
}).setCriteria("&r&6&lWow! &r&eYou dug out &r&6${coins} coins&r&e!&r"), () => getWorld() === "Hub" && settings.dianaLootTracker);

registerWhen(register("chat", (drop) => {
    if (isDataLoaded() && checkDiana() && isInSkyblock()) {
        drop = drop.slice(2, 16); // 8 statt 16 für potato und carrot
        switch (drop) {
            case "Enchanted Book":
                if (settings.lootAnnouncerScreen) {
                    Client.Companion.showTitle(`&d&lChimera!`, "", 0, 25, 35);
                }
                trackItem("Chimera", "items", 1);
                break;
            case "Daedalus Stick":
                if (settings.lootAnnouncerScreen) {
                    Client.Companion.showTitle(`&6&lDaedalus Stick!`, "", 0, 25, 35);
                }
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
}).setCriteria("&r&6&lRARE DROP! &r${drop}"), () => settings.dianaLootTracker);
// Party > [MVP++] LHxSeven: &r&6&lRARE DROP! &r&6Daedalus Stick &r&b(+&r&b322% &r&b✯ Magic Find&r&b)&r
// Party > [MVP++] LHxSeven: &r&6&lRARE DROP! &r&fEnchanted Book&r
// &r&6&lRARE DROP! &r&fEnchanted Book &r&b(+&r&b348% &r&b✯ Magic Find&r&b)&r

// &r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r


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
let trackerBool = false;
let tempGuiBool = false;
register("step", () => {
    if (getRefreshOverlays() && !tempGuiBool){
        tempGuiBool = true;
    }
    if (isInSkyblock() && !firstLoad) {
        if (!trackerBool) {
            if (isDataLoaded()) {
                trackerTotal = getTrackerTotal();
                trackerMayor = getTrackerMayor();
                trackerSession = getTrackerSession();
                trackerBool = true;
            }
        }
        else {
            refreshOverlay(getTracker(settings.dianaLootTrackerView), settings.dianaLootTrackerView, "items");
            refreshOverlay(getTracker(settings.dianaMobTrackerView), settings.dianaMobTrackerView, "mobs");
            mythosMobHpOverlay([]);
            firstLoad = true;
        }
    }
    if (tempGuiBool && !getRefreshOverlays()) {
        firstLoad = false;
        tempGuiBool = false;
    }
}).setFps(1);


// // test command
// register('command', () => {
//     trackerSession = getTracker(3);
//     for (let item in trackerSession["items"]) {
//         ChatLib.chat(item + ": " + trackerSession["items"][item]);
//     }
// }).setName("sbots");
// register('command', () => {
//     trackerMayor = getTracker(2);
//     for (let item in trackerMayor["items"]) {
//         ChatLib.chat(item + ": " + trackerMayor["items"][item]);
//     }
// }).setName("sbotm");

// register('command', () => {
//     trackerTotal = getTracker(1);
//     for (let item in trackerTotal["items"]) {
//         ChatLib.chat(item + ": " + trackerTotal["items"][item]);
//     }
// }).setName("sbott");
