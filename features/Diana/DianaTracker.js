import settings from "../../settings";
import { checkMayorTracker, data, registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase, gotLootShare, getAllowedToTrackSacks, playCustomSound, calcPercent, mobDeath4SecsTrue, getBazaarPriceDiana, getDianaAhPrice, formatNumber } from '../../utils/functions';
import { itemOverlay, mobOverlay, mythosMobHpOverlay, statsOverlay, avgMagicFindOverlay } from "../guis/DianaGuis";
import { mobDeath2SecsTrue } from "../../utils/functions";
import { isDataLoaded } from "../../utils/checkData";
import { dianaTrackerMayor as trackerMayor, dianaTrackerSession as trackerSession, dianaTrackerTotal as trackerTotal, initializeTracker, dianaTimerlist } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";
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
                        if ([10, 20, 30].includes(parseInt(amount)) && gotLootShare()) {
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
                        if (settings.lootAnnouncerPrice) {
                            Client.Companion.showTitle(`&5&lMinos Relic!`, `&6${formatNumber(getDianaAhPrice("MINOS_RELIC"))} coins`, 0, 25, 35);
                        }
                        else {
                            Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
                        }
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
    let countThisIds = ["Enchanted Gold", "Enchanted Iron", "Ancient Claw"] // , "Rotten Flesh"
    if (getAllowedToTrackSacks()) {
        for (let i in countThisIds.values()) {
            if (item == i) {
                trackItem(item.replaceAll(" ", "_").toUpperCase(), "items", parseInt(ammount));
            }
        }
    }
}

let forbiddenCoins = [1, 5, 20, 1000, 2000, 3000, 4000, 5000, 7500, 8000, 10000, 12000, 15000, 20000, 25000, 40000, 50000]
export function trackScavengerCoins(coins) {
    if (mobDeath4SecsTrue()) {
        if (!forbiddenCoins.includes(coins) && coins < 60000) {
            trackItem("scavengerCoins", "items", coins);
            trackItem("coins", "items", coins);
        }
    }
}

// track logic //
export function trackItem(item, category, amount) {
    if (isDataLoaded()) {
        dianaTimerlist.forEach(timer => {
            timer.start();
            timer.continue();
            timer.updateActivity();
        });
        if (lastMob == item && item != "Minos Inquisitor Ls") {
            if (item == "Minos Inquisitor") {
                ChatLib.chat("&6[SBO] &r&cb2b Inquisitor!")
            };
            // ChatLib.chat("&6[SBO] &r&cYou killed the same mob twice in a row!");
        }
        // if item in lastMobDrobs
        
        if (category === "mobs" && item != "Minos Inquisitor Ls") {
            lastMob = item;
            // if (item == "Minos Inquisitor") {
            //     lastInqDroped = false;
            // }
            // else if (item == "Minos Champion") {
            //     lastChampDroped = false;
            // }
            // else if (item == "Minotaur") {
            //     lastMinotaurDroped = false;
            // }
            data.mobsSinceInq += 1;
        }
        // if (item == "Minos Inquisitor Ls") {
        //     lastInqDroped = false;
        // }
        // if (category === "items") {
        //     if (item == "Chimera") lastInqDroped = true;
        //     if (item == "ChimeraLs") lastInqLsDroped = true;
        //     if (item == "Minos Relic") lastChampDroped = true;
        //     if (item == "Daedalus Stick") lastMinotaurDroped = true;
        // }
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
    dianaTimerlist[2].reset();
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
    cancel(event);
}).setCriteria("&r&cThis ability is on cooldown for${time}"), () => getWorld() === "Hub" && settings.cleanDianaChat);

registerWhen(register("chat", (waste, event) => {
    cancel(event);
}).setCriteria("&r&7Warping${waste}"), () => getWorld() === "Hub" && settings.cleanDianaChat);

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
"&7[12:22] &r&r&r&c&lYikes! &r&eYou dug out a &r&2Minos Inquisitor&r&e!&r"

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
                    if (settings.lootAnnouncerPrice) {
                        Client.Companion.showTitle(`&d&lChimera!`, `&6${formatNumber(getBazaarPriceDiana("ENCHANTMENT_ULTIMATE_CHIMERA_1"))} coins`, 0, 25, 35);
                    }
                    else {
                        Client.Companion.showTitle(`&d&lChimera!`, "", 0, 25, 35);
                    }
                }

                playCustomSound(settings.chimSound, settings.chimVolume);
                if (gotLootShare() && settings.dianaTracker) {
                    trackItem("ChimeraLs", "items", 1); // ls chim
                    if(settings.replaceChimMessage) {
                        cancel(event)
                        if(magicFind > 0) {
                        ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &r&d&lChimera! &r&b(+&r&b" + magicFind + "%" +" &r&b✯ Magic Find&r&b)&r");
                        }
                        else {
                            ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &r&d&lChimera!&r");
                        }
                    }
                }
                else {
                    if(settings.dianaAvgMagicFind) {
                        if(magicFind > 0) {
                            if(data.last10ChimMagicFind.length >= 10) {
                                data.last10ChimMagicFind.shift();
                            }
                            data.last10ChimMagicFind.push(magicFind);
                        
                            let sum = data.last10ChimMagicFind.reduce((a, b) => a + b, 0);
                            data.avgChimMagicFind = parseInt(sum / data.last10ChimMagicFind.length);
                            avgMagicFindOverlay();
                        }
                    }
                    if(settings.dianaTracker) {
                        trackItem("Chimera", "items", 1);
                    }
                    if(settings.sendSinceMassage) {
                        new TextComponent(`&6[SBO] &r&eTook &r&c${data.inqsSinceChim} &r&eInquisitors to get a Chimera!`).setClick("run_command", `/ct copy [SBO] Took ${data.inqsSinceChim} Inquisitors to get a Chimera!`).setHover("show_text", "&eClick To Copy").chat();
                    }
                    data.inqsSinceChim = 0;
                    if(settings.replaceChimMessage) {
                        cancel(event)
                        if(magicFind > 0) {
                        ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &r&d&lChimera! &r&b(+&r&b" + magicFind + "%" +" &r&b✯ Magic Find&r&b)&r");
                        }
                        else {
                            ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &r&d&lChimera!&r");
                        }
                    }
                }
                break;
            case "Daedalus Stick":
                if(settings.dianaAvgMagicFind) {
                    if(magicFind > 0) {
                        if(data.last10StickMagicFind.length >= 10) {
                            data.last10StickMagicFind.shift();
                        }
                        data.last10StickMagicFind.push(magicFind);
                    
                        let sum = data.last10StickMagicFind.reduce((a, b) => a + b, 0);
                        data.avgStickMagicFind = parseInt(sum / data.last10StickMagicFind.length);
                        avgMagicFindOverlay();
                    }
                }

                if(settings.sendSinceMassage) {
                    new TextComponent(`&6[SBO] &r&eTook &r&c${data.minotaursSinceStick} &r&eMinotaurs to get a Daedalus Stick!`).setClick("run_command", `/ct copy [SBO] Took ${data.minotaursSinceStick} Minotaurs to get a Daedalus Stick!`).setHover("show_text", "&eClick To Copy").chat();
                }
                data.minotaursSinceStick = 0;
                if (settings.lootAnnouncerScreen) {
                    if (settings.lootAnnouncerPrice) {
                        Client.Companion.showTitle(`&6&lDaedalus Stick!`, `&6${formatNumber(getBazaarPriceDiana("DAEDALUS_STICK"))} coins`, 0, 25, 35);
                    }
                    else {
                        Client.Companion.showTitle(`&6&lDaedalus Stick!`, "", 0, 25, 35);
                    }
                }

                playCustomSound(settings.stickSound, settings.stickVolume);
                if (settings.dianaTracker) {
                    trackItem(drop, "items", 1);
                }
                break;
        }
    }
}).setCriteria("&r&6&lRARE DROP! &r${drop}"), () => settings.dianaTracker || (settings.dianaStatsTracker || settings.sendSinceMassage || settings.dianaAvgMagicFind || settings.replaceChimMessage));
"&r&6&lRARE DROP! &r&fEnchanted Book&r"

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

let firstLoadReg = register("tick", () => {
    if (isInSkyblock() && isDataLoaded()) {
        itemOverlay();
        mobOverlay();
        statsOverlay();
        avgMagicFindOverlay();
        mythosMobHpOverlay([]);
        firstLoadReg.unregister();
    }
})

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


