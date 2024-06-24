import settings from "../../settings";
import { checkMayorTracker, data, registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase, gotLootShare, getAllowedToTrackSacks, playCustomSound, calcPercent, getTracker } from '../../utils/functions';
import { itemOverlay, mobOverlay, mythosMobHpOverlay, statsOverlay, avgMagicFindOverlay } from "../guis/DianaGuis";
import { isActiveForOneSecond as mobDeath2SecsTrue } from "../../utils/functions";
import { isDataLoaded } from "../../utils/checkData";
import { dianaTrackerMayor as trackerMayor, dianaTrackerSession as trackerSession, dianaTrackerTotal as trackerTotal, initializeTracker } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";
import { getRefreshOverlays } from "../../utils/overlays";
import { playerHasSpade } from "../../utils/functions";

// todo: 
// todo end
let lastMob = "";
let lastInqDroped = false;
let lastInqLsDroped = false;
let lastChampDroped = false;
let lastMinotaurDroped = false;

// track items with pickuplog //
export function dianaLootCounter(item, amount) {
    let rareDrops = ["&9DWARF_TURTLE_SHELMET", "&5CROCHET_TIGER_PLUSHIE", "&5ANTIQUE_REMEDIES", "&5MINOS_RELIC"]; //  "&5ROTTEN_FLESH"
    let countThisIds = ["ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW", "ENCHANTED_GOLD", "ENCHANTED_IRON"]
    let checkBool = true;
    if (mobDeath2SecsTrue() || gotLootShare()) {
        if (checkDiana()) {
            for (let i in countThisIds.values()) {
                if (item === i) {
                    if (item == "ANCIENT_CLAW") {
                        if ([48, 96, 144].includes(parseInt(amount)) && gotLootShare()) {
                            trackItem("Minos Inquisitor Ls", "mobs", 1); // ls inq
                        }
                    }
                    trackItem(item, "items", amount);
                    checkBool = false;
                }
            }
            if (checkBool) {
                if (item == "MINOS_RELIC") {
                    if(settings.sendSinceMassage) {
                        new TextComponent(`&6[SBO] &r&eTook &r&c${data.champsSinceRelic} &r&eChampions to get a Relic!`).setClick("run_command", `/ct copy [SBO] Took ${data.champsSinceRelic} Champions to get a Relic!`).setHover("show_text", "&eClick To Copy").chat();
                    }
                    data.champsSinceRelic = 0;
                    if (settings.lootAnnouncerScreen) {
                        Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
                    }
                    playCustomSound(settings.relicSound, settings.relicVolume);
                }
                for (let i in rareDrops.values()) {
                    color = i.slice(0, 2);
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
                        if (item != "MINOS_RELIC") {
                            playCustomSound(settings.sprSound, settings.sprVolume);
                        }
                        if (settings.dianaTracker) {
                            trackItem(item, "items", amount);
                        }
                    }
                }
            }
        }
    }
}

export function trackLootWithSacks(ammount, item) {
    let countThisIds = ["Enchanted Gold", "Enchanted Iron"] // , "Rotten Flesh"
    if (getAllowedToTrackSacks()) {
        for (let i in countThisIds.values()) {
            if (item == i) {
                trackItem(item.replaceAll(" ", "_").toUpperCase(), "items", parseInt(ammount));
            }
        }
    }
}

let forbiddenCoins = [1000, 2000, 3000, 4000, 5000, 7500, 8000, 10000, 12000, 15000, 20000, 25000, 40000, 50000]
export function trackScavengerCoins(coins) {
    if (mobDeath2SecsTrue()) {
        if (!forbiddenCoins.includes(coins) && coins < 60000) {
            trackItem("scavengerCoins", "items", coins);
            trackItem("coins", "items", coins);
        }
    }
}

// get tracker by setting (0: default, 1: total, 2: event, 3: event) //


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

// track logic //
export function trackItem(item, category, amount) {
    if (isDataLoaded()) {
        if (lastMob == item && item != "Minos Inquisitor Ls") {
            if (item == "Minos Inquisitor") {
                ChatLib.chat("&6[SBO] &r&cb2b Inquisitor!")
            };
            // ChatLib.chat("&6[SBO] &r&cYou killed the same mob twice in a row!");
        }
        // if item in lastMobDrobs
        
        if (category === "mobs" && item != "Minos Inquisitor Ls") {
            lastMob = item;
            if (item == "Minos Inquisitor") {
                lastInqDrops = [];
            }
            else if (item == "Minos Champion") {
                lastChampDrops = [];
            }
            else if (item == "Minotaur") {
                lastMinotaurDrops = [];
            }
            data.mobsSinceInq += 1;
        }
        if (item == "Minos Inquisitor Ls") {
            lastInqDroped = false;
        }
        if (category === "items") {
            if (item == "Chimera") lastInqDroped = true;
            if (item == "ChimeraLs") lastInqLsDroped = true;
            if (item == "Minos Relic") lastChampDroped = true;
            if (item == "Daedalus Stick") lastMinotaurDroped = true;
        }
        trackOne(trackerMayor, item, category, "Mayor", amount);
        trackOne(trackerSession, item, category, "Session", amount);
        trackOne(trackerTotal, item, category, "Total", amount);

        itemOverlay();
        mobOverlay();
        statsOverlay();
        avgMagicFindOverlay();
    }
}

function trackOne(tracker, item, category, type, amount) {
    if (type == "Mayor") {
        checkMayorTracker();
    }
    if (tracker[category][item] != null) {
        tracker[category][item] += amount;
    }
    else {
        tracker[category][item] = amount;
    }

    if (category === "mobs"  && item != "Minos Inquisitor Ls") {
        if (tracker["mobs"]["TotalMobs"] != null) {
            tracker["mobs"]["TotalMobs"] += amount;
        }
        else {
            tracker["mobs"]["TotalMobs"] = amount;
        }
    }
    tracker.save();
}

// command to reset session tracker
register("command", () => {
    let tempTracker = initializeTracker();
    for (let key in tempTracker) {
        trackerSession[key] = tempTracker[key];
    }
    trackerSession.save();
    itemOverlay();
    mobOverlay();
}).setName("sboresetsession");
    
// total burrow tracker //
registerWhen(register("chat", (burrow, event) => {
    if (isDataLoaded()) {
        trackItem("Total Burrows", "items", 1);
        burrow = burrow.removeFormatting();
        if (settings.fourEyedFish) {
            if (burrow == "(2/4)" || burrow == "(3/4)") {
                trackItem("coins", "items", 4000);
                trackItem("fishCoins", "items", 4000);
            }
            else {
                trackItem("coins", "items", 2000);
                trackItem("fishCoins", "items", 2000);
            }
        }
    }
    if (settings.cleanDianaChat) cancel(event);
}).setCriteria("&r&eYou dug out a Griffin Burrow! &r&7${burrow}&r"), () => getWorld() === "Hub" && settings.dianaTracker);

registerWhen(register("chat", (burrow, event) => {
    if (isDataLoaded()) {
        trackItem("Total Burrows", "items", 1);
        if (settings.fourEyedFish) {
            trackItem("coins", "items", 2000);
            trackItem("fishCoins", "items", 2000);
        }
    }
    if (settings.cleanDianaChat) cancel(event);
}).setCriteria("&r&eYou finished the Griffin burrow chain!${burrow}"), () => getWorld() === "Hub" && settings.dianaTracker);

register("chat", (event) => {
    if (settings.cleanDianaChat) cancel(event);
}).setCriteria("&r&eFollow the arrows to find the &r&6treasure&r&e!&r");

// --for spade spam
registerWhen(register("chat", (time, event) => {
    if (settings.cleanDianaChat) cancel(event);
}).setCriteria("&r&cThis ability is on cooldown for ${time}"), () => getWorld() === "Hub" && playerHasSpade());


// mob tracker
registerWhen(register("chat", (woah, arev, mob, event) => {
    if (isDataLoaded() && isInSkyblock()) {
        switch (mob) {
            case "Minos Inquisitor":
                data.inqsSinceChim += 1;
                trackItem(mob, "mobs", 1);
                if(settings.sendSinceMassage) {
                    new TextComponent(`&6[SBO] &r&eTook &r&c${data.mobsSinceInq} &r&eMobs to get a Inquis!`).setClick("run_command", `/ct copy [SBO] Took ${data.mobsSinceInq} Mobs to get a Inquis!`).setHover("show_text", "&eClick To Copy").chat();
                }
                data.mobsSinceInq = 0;        
                break;
            case "Minos Champion":
                data.champsSinceRelic += 1;
                trackItem(mob, "mobs", 1);
                break;
            case "Minotaur":
                data.minotaursSinceStick += 1;
                trackItem(mob, "mobs", 1);
                break;
            case "Minos Hunter":
            case "Gaia Construct":
            case "Siamese Lynxes":
                trackItem(mob, "mobs", 1);
                break;       
        }
    }
    if (settings.cleanDianaChat) cancel(event);
}).setCriteria("&r&c&l${woah} &r&eYou dug ${arev}&r&2${mob}&r&e!&r"), () => getWorld() === "Hub" && (settings.dianaTracker || (settings.dianaStatsTracker || settings.sendSinceMassage)));


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
}).setCriteria("&r&6&lRARE DROP! &r&eYou dug out a &r${drop}&r&e!&r"), () => getWorld() === "Hub" && settings.dianaTracker);

registerWhen(register("chat", (coins) => {
    if (isDataLoaded() && isInSkyblock()) {
        let coins2 = parseInt(coins.replace(",", ""))
        trackItem("coins", "items", coins2);
    }
}).setCriteria("&r&6&lWow! &r&eYou dug out &r&6${coins} coins&r&e!&r"), () => getWorld() === "Hub" && settings.dianaTracker);

registerWhen(register("chat", (drop, event) => {
    if (isDataLoaded() && checkDiana() && isInSkyblock()) {
        let magicFindMatch = drop.match(/\+(&r&b)?(\d+)%/);
        let magicFind = parseInt((magicFindMatch ? magicFindMatch[2] : 0));
        drop = drop.slice(2, 16); // 8 statt 16 für potato und carrot
        switch (drop) {
            case "Enchanted Book":
                if (settings.lootAnnouncerScreen) {
                    Client.Companion.showTitle(`&d&lChimera!`, "", 0, 25, 35);
                }

                playCustomSound(settings.chimSound, settings.chimVolume);
                if (gotLootShare()) {
                    trackItem("ChimeraLs", "items", 1); // ls chim
                }
                else {
                    if(settings.dianaAvgMagicFind){
                        if(magicFind > 0){
                            if(data.last10ChimMagicFind.length >= 10){
                                data.last10ChimMagicFind.shift();
                            }
                            data.last10ChimMagicFind.push(magicFind);
                        
                            let sum = data.last10ChimMagicFind.reduce((a, b) => a + b, 0);
                            data.avgChimMagicFind = parseInt(sum / data.last10ChimMagicFind.length);
                        }
                    }
                    if(settings.replaceChimMessage) {
                        cancel(event)
                        ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &r&d&lChimera! &r&b(+&r&b" + magicFind + "%" +" &r&b✯ Magic Find&r&b)&r");
                    }
                    
                    trackItem("Chimera", "items", 1);
                    if(settings.sendSinceMassage) {
                        new TextComponent(`&6[SBO] &r&eTook &r&c${data.inqsSinceChim} &r&eInquisitors to get a Chimera!`).setClick("run_command", `/ct copy [SBO] Took ${data.inqsSinceChim} Inquisitors to get a Chimera!`).setHover("show_text", "&eClick To Copy").chat();
                    }
                    data.inqsSinceChim = 0;
                }
                break;
            case "Daedalus Stick":
                if(settings.dianaAvgMagicFind){
                    if(magicFind > 0){
                        if(data.last10StickMagicFind.length >= 10){
                            data.last10StickMagicFind.shift();
                        }
                        data.last10StickMagicFind.push(magicFind);
                    
                        let sum = data.last10StickMagicFind.reduce((a, b) => a + b, 0);
                        data.avgStickMagicFind = parseInt(sum / data.last10StickMagicFind.length);
                    }
                }

                if(settings.sendSinceMassage) {
                    new TextComponent(`&6[SBO] &r&eTook &r&c${data.minotaursSinceStick} &r&eMinotaurs to get a Daedalus Stick!`).setClick("run_command", `/ct copy [SBO] Took ${data.minotaursSinceStick} Minotaurs to get a Daedalus Stick!`).setHover("show_text", "&eClick To Copy").chat();
                }
                data.minotaursSinceStick = 0;
                if (settings.lootAnnouncerScreen) {
                    Client.Companion.showTitle(`&6&lDaedalus Stick!`, "", 0, 25, 35);
                }

                playCustomSound(settings.stickSound, settings.stickVolume);
                trackItem(drop, "items", 1);
                break;
        }
    }
}).setCriteria("&r&6&lRARE DROP! &r${drop}"), () => settings.dianaTracker || (settings.dianaStatsTracker || settings.sendSinceMassage || settings.dianaAvgMagicFind || settings.replaceChimMessage));

// refresh overlay //
let tempSettingLoot = -1;
let tempSettingBazzar = -1;
registerWhen(register("step", () => {
    tempSettingLoot = settings.dianaLootTrackerView;
    tempSettingBazzar = settings.bazaarSettingDiana
    itemOverlay();
}).setFps(1), () => settings.dianaTracker && (tempSettingLoot !== settings.dianaLootTrackerView || tempSettingBazzar !== settings.bazaarSettingDiana));

let tempSettingMob = -1;
registerWhen(register("step", () => {
    tempSettingMob = settings.dianaMobTrackerView;
    mobOverlay();
}).setFps(1), () => settings.dianaTracker && tempSettingMob !== settings.dianaMobTrackerView);

let firstLoad = false;
let tempGuiBool = false;
register("step", () => {
    if (getRefreshOverlays() && !tempGuiBool) {
        tempGuiBool = true;
    }
    if (isInSkyblock() && !firstLoad && isDataLoaded()) {
        itemOverlay();
        mobOverlay();
        statsOverlay();
        avgMagicFindOverlay();
        mythosMobHpOverlay([]);
        firstLoad = true;
    }
    if (tempGuiBool && !getRefreshOverlays()) {
        firstLoad = false;
        tempGuiBool = false;
    }
}).setFps(1);


// // test command
// register('command', () => {
    // trackerSession = getTracker(3);
    // for (let item in trackerSession["items"]) {
    //     ChatLib.chat(item + ": " + trackerSession["items"][item]);
    // }
//     mobsSinceInqMSG = new TextComponent(`&6[SBO] &r&eTook ${data.mobsSinceInq} Mobs to get a Inquis!`).setClick("run_command", `/ct copy [SBO] &r&eTook ${data.mobsSinceInq} Mobs to get a Inquis!`).setHover("show_text", "click to copy").chat();
//     if(settings.sendSinceMassage) mobsSinceInqMSG;
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


