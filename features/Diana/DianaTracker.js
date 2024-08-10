import settings from "../../settings";
import { checkMayorTracker, data, initializeTrackerMayor, registerWhen, resetTracker } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, toTitleCase, gotLootShare, getAllowedToTrackSacks, playCustomSound, calcPercent, mobDeath4SecsTrue, getBazaarPriceDiana, getDianaAhPrice, formatNumber, getMagicFind } from '../../utils/functions';
import { itemOverlay, mobOverlay, mythosMobHpOverlay, statsOverlay, avgMagicFindOverlay } from "../guis/DianaGuis";
import { mobDeath2SecsTrue } from "../../utils/functions";
import { isDataLoaded } from "../../utils/checkData";
import { dianaTrackerMayor as trackerMayor, dianaTrackerSession as trackerSession, dianaTrackerTotal as trackerTotal, initializeTracker, dianaTimerlist } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";
import { trackSinceItem, trackSinceMob, unlockAchievement } from "./DianaAchievements";

// todo: 
// todo end

// track items with pickuplog //
let b2bStick = false;
let b2bChim = false;
export function dianaLootCounter(item, amount) {
    let rareDrops = ["&9DWARF_TURTLE_SHELMET", "&5CROCHET_TIGER_PLUSHIE", "&5ANTIQUE_REMEDIES", "&5MINOS_RELIC"]; //  "&5ROTTEN_FLESH"
    let countThisIds = ["ENCHANTED_ANCIENT_CLAW", "ANCIENT_CLAW", "ENCHANTED_GOLD", "ENCHANTED_IRON"]
    let checkBool = true;
    if (mobDeath2SecsTrue() || gotLootShare()) {
        if (checkDiana()) {
            for (let i in countThisIds.values()) {
                if (item === i) {
                    trackItem(item, "items", amount);
                    checkBool = false;
                }
            }
            if (checkBool) {
                if (item == "MINOS_RELIC") {
                    if(settings.sendSinceMassage) {
                        new TextComponent(`&6[SBO] &r&eTook &r&c${data.champsSinceRelic} &r&eChampions to get a Relic!`).setClick("run_command", `/ct copy [SBO] Took ${data.champsSinceRelic} Champions to get a Relic!`).setHover("show_text", "&eClick To Copy").chat();
                    }
                    if (data.champsSinceRelic == 1) {
                        ChatLib.chat("&6[SBO] &cb2b Relic!")
                        unlockAchievement(5) // b2b relic
                    }
                    if (gotLootShare()) unlockAchievement(17) // relic ls
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
    if (mobDeath4SecsTrue() || gotLootShare()) {
        if (!forbiddenCoins.includes(coins) && coins < 65000) {
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

        if (category === "mobs") {
            data.mobsSinceInq += 1;
            if (data.mobsSinceInq == 2) b2bInq = false;
        }
        trackOne(trackerMayor, item, category, "Mayor", amount);
        trackOne(trackerSession, item, category, "Session", amount);
        trackOne(trackerTotal, item, category, "Total", amount);

        itemOverlay();
        mobOverlay();
        statsOverlay();
        avgMagicFindOverlay();
        data.save();
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
    if (type == "Mayor") {
        trackAchievements(tracker, item);
        if (category == "mobs") {
            trackSinceMob();
        }
    }
}

function checkCustomChimMessage(magicFind) {
    let text = settings.customChimMessage;
    if (!settings.chimMessageBool) return [false, ""];
    if (text != "") {
        if (text.includes("{mf}")) {
            let mfMessage = "";
            if (magicFind > 0) {
                mfMessage = "(+" + magicFind + "%" + " ✯ Magic Find)";
            } else {
                mfMessage = "";
            }
            text = text.replace(/{mf}/g, mfMessage);
        }
        if (text.includes("{amount}")) {
            let amount = trackerMayor["items"]["Chimera"] + trackerMayor["items"]["ChimeraLs"];
            text = text.replace(/{amount}/g, amount);
        }
        return [true, text];
    } else {
        return [false, ""];
    }
}

register("command", () => {
    let [replaceChimMessage, customChimMessage] = checkCustomChimMessage(400);
    if(replaceChimMessage) {
        ChatLib.chat(customChimMessage);
    }
}).setName("sbochimtest");

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
let b2bInq = false;
registerWhen(register("chat", (woah, arev, mob, event) => {
    if (isDataLoaded() && isInSkyblock()) {
        switch (mob) {
            case "Minos Inquisitor":
                data.inqsSinceChim += 1;
                trackItem(mob, "mobs", 1);
                if(settings.sendSinceMassage) {
                    new TextComponent(`&6[SBO] &r&eTook &r&c${data.mobsSinceInq} &r&eMobs to get a Inquis!`).setClick("run_command", `/ct copy [SBO] Took ${data.mobsSinceInq} Mobs to get a Inquis!`).setHover("show_text", "&eClick To Copy").chat();
                }
                if (data.mobsSinceInq == 1 && !b2bInq) {
                    ChatLib.chat("&6[SBO] &cb2b Inquisitor!")
                    unlockAchievement(6) // b2b inq
                    b2bInq = true;
                }
                if (data.inqsSinceChim == 2) b2bChim = false;
                if (b2bInq && data.mobsSinceInq == 1) {
                    ChatLib.chat("&6[SBO] &cb2b2b Inquisitor!")
                    unlockAchievement(7) // b2b2b inq
                }
                data.mobsSinceInq = 0;        
                break;
            case "Minos Champion":
                data.champsSinceRelic += 1;
                trackItem(mob, "mobs", 1);
                break;
            case "Minotaur":
                data.minotaursSinceStick += 1;
                if (data.minotaursSinceStick == 2) b2bStick = false;
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
        let magicFind = getMagicFind(drop);
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
                    let [replaceChimMessage, customChimMessage] = checkCustomChimMessage(magicFind);
                    if(replaceChimMessage) {
                        cancel(event)
                        ChatLib.chat(customChimMessage);
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
                    if (data.inqsSinceChim == 1 && !b2bChim) {
                        ChatLib.chat("&6[SBO] &cb2b Chimera!")
                        b2bChim = true;
                        unlockAchievement(1) // b2b chim
                    }
                    if (b2bChim && data.inqsSinceChim == 1) {
                        ChatLib.chat("&6[SBO] &cb2b2b Chimera!")
                        unlockAchievement(2) // b2b2b chim
                    }
                    data.inqsSinceChim = 0;
                    let [replaceChimMessage, customChimMessage] = checkCustomChimMessage(magicFind);
                    if(replaceChimMessage) {
                        cancel(event)
                        ChatLib.chat(customChimMessage);
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
                if (data.minotaursSinceStick == 1 && !b2bStick) {
                    ChatLib.chat("&6[SBO] &cb2b Daedalus Stick!")
                    b2bStick = true;
                    unlockAchievement(3) // b2b stick
                }
                if (b2bStick && data.minotaursSinceStick == 1) {
                    ChatLib.chat("&6[SBO] &cb2b2b Daedalus Stick!")
                    unlockAchievement(4) // b2b2b stick
                }
                if (gotLootShare()) unlockAchievement(15) // ls stick
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
}).setCriteria("&r&6&lRARE DROP! &r${drop}"), () => settings.dianaTracker || (settings.dianaStatsTracker || settings.sendSinceMassage || settings.dianaAvgMagicFind || settings.chimMessageBool));

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

// import tracker feature
const translateDictSh = {
    "SKYBLOCK_COIN": "coins",
    "GRIFFIN_FEATHER": "Griffin Feather",
    "CROWN_OF_GREED": "Crown of Greed",
    "WASHED_UP_SOUVENIR": "Washed-up Souvenir",
    "ULTIMATE_CHIMERA;1": "Chimera",
    "DAEDALUS_STICK": "Daedalus Stick",
    "MINOS_INQUISITOR": "Minos Inquisitor",
    "MINOS_CHAMPION": "Minos Champion",
    "MINOTAUR": "Minotaur",
    "GAIA_CONSTRUCT": "Gaia Construct",
    "SIAMESE_LYNXES": "Siamese Lynxes",
    "MINOS_HUNTER": "Minos Hunter"
};

const translateDictSt = {
    "COINS": "coins",
    "GRIFFIN_FEATHER": "Griffin Feather",
    "CROWN_OF_GREED": "Crown of Greed",
    "WASHED_UP_SOUVENIR": "Washed-up Souvenir",
    "ENCHANTED_BOOK-ULTIMATE_CHIMERA-1": "Chimera",
    "DAEDALUS_STICK": "Daedalus Stick",
    "MINOS_INQUISITOR": "Minos Inquisitor",
    "MINOS_CHAMPION": "Minos Champion",
    "MINOTAUR": "Minotaur",
    "GAIA_CONSTRUCT": "Gaia Construct",
    "SIAMESE_LYNXES": "Siamese Lynxes",
    "MINOS_HUNTER": "Minos Hunter"
};

let totalImportedMobs = 0;
function importDianaTracker(type, profileName, importType) {
    totalImportedMobs = 0;
    if (type == "sh") {
        ChatLib.chat("&6[SBO] &aImporting from SkyHanni...");
        let shConfig = JSON.parse(FileLib.read("./config/skyhanni/config.json"));
        let activePlayer = shConfig.storage.players[Player.getUUID()] 
        let activeProfile = activePlayer.profiles[profileName]; 
        let dianaShTracker = activeProfile.diana
        if (shConfig == undefined) {
            ChatLib.chat("&6[SBO] &cSkyHanni not found. cant import tracker");
            return;
        }
        if (activePlayer == undefined || activeProfile == undefined) {
            ChatLib.chat("&6[SBO] &cProfile not found. Please check if the profile name is correct.");
            return;
        }
        if (importType == "overwrite") {
            resetTracker("total");
            resetTracker("mayor");
            resetTracker("session");
        }

        Object.keys(dianaShTracker.dianaProfitTracker.items).forEach((item) => {
            let itemKey = item;
            if (translateDictSh[item]) itemKey = translateDictSh[item];
            transferData("items", item, itemKey, dianaShTracker.dianaProfitTracker.items[item].totalAmount, importType);
        });

        Object.keys(dianaShTracker.mythologicalMobTracker.count).forEach((mob) => {
            let itemKey = mob;
            if (translateDictSh[mob]) itemKey = translateDictSh[mob];
            transferData("mobs", mob, itemKey, dianaShTracker.mythologicalMobTracker.count[mob], importType);
        });
        if (importType == "overwrite") {
            trackerMayor.mobs["TotalMobs"] = totalImportedMobs;
            trackerTotal.mobs["TotalMobs"] = totalImportedMobs;
            trackerSession.mobs["TotalMobs"] = totalImportedMobs;

            trackerMayor.items["Total Burrows"] = activeProfile.diana.dianaProfitTracker.burrowsDug;
            trackerTotal.items["Total Burrows"] = activeProfile.diana.dianaProfitTracker.burrowsDug;
            trackerSession.items["Total Burrows"] = activeProfile.diana.dianaProfitTracker.burrowsDug;
        }
        else if (importType == "add") {
            trackerMayor.mobs["TotalMobs"] += totalImportedMobs;
            trackerTotal.mobs["TotalMobs"] += totalImportedMobs;
            trackerSession.mobs["TotalMobs"] += totalImportedMobs;

            trackerMayor.items["Total Burrows"] += activeProfile.diana.dianaProfitTracker.burrowsDug;
            trackerTotal.items["Total Burrows"] += activeProfile.diana.dianaProfitTracker.burrowsDug;
            trackerSession.items["Total Burrows"] += activeProfile.diana.dianaProfitTracker.burrowsDug;
        }
        ChatLib.chat("&6[SBO] &aTracker imported!");
    }
    else if (type == "st") {
        ChatLib.chat("&6[SBO] &aImporting from Skytils...");
        let dianaStTracker = JSON.parse(FileLib.read("./config/skytils/trackers/mythological.json"));
        if (dianaStTracker == undefined) {
            ChatLib.chat("&6[SBO] &cSkytils not found. cant import tracker");
            return;
        }
        if (importType == "overwrite") {
            resetTracker("total");
            resetTracker("mayor");
            resetTracker("session");
        }

        Object.keys(dianaStTracker.items).forEach((item) => {
            let itemKey = item;
            if (translateDictSt[item]) itemKey = translateDictSt[item];
            transferData("items", item, itemKey, dianaStTracker.items[item], importType);
        });
        
        Object.keys(dianaStTracker.mobs).forEach((mob) => {
            let itemKey = mob;
            if (translateDictSt[mob]) itemKey = translateDictSt[mob];
            transferData("mobs", mob, itemKey, dianaStTracker.mobs[mob], importType);
        });

        if (importType == "overwrite") {
            trackerMayor.mobs["TotalMobs"] = totalImportedMobs;
            trackerTotal.mobs["TotalMobs"] = totalImportedMobs;
            trackerSession.mobs["TotalMobs"] = totalImportedMobs;

            trackerMayor.items["Total Burrows"] = dianaStTracker.dug;
            trackerTotal.items["Total Burrows"] = dianaStTracker.dug;
            trackerSession.items["Total Burrows"] = dianaStTracker.dug;
        }
        ChatLib.chat("&6[SBO] &aTracker imported!");
    }
    trackerMayor.save();
    trackerTotal.save();
    itemOverlay();
    mobOverlay();
}

function transferData(type, item, itemKey, ammount, importType) {
    if (type == "items") {
        if (importType == "overwrite") {
            trackerMayor.items[itemKey] = ammount;
            trackerTotal.items[itemKey] = ammount;
            trackerSession.items[itemKey] = ammount;
        }
        else if (importType == "add") {
            trackerMayor.items[itemKey] += ammount;
            trackerTotal.items[itemKey] += ammount;
            trackerSession.items[itemKey] += ammount;
        }
    }
    else if (type == "mobs") {
        if (importType == "overwrite") {
            trackerMayor.mobs[itemKey] = ammount;
            trackerTotal.mobs[itemKey] = ammount;
            trackerSession.mobs[itemKey] = ammount;
        }
        else if (importType == "add") {
            trackerMayor.mobs[itemKey] += ammount;
            trackerTotal.mobs[itemKey] += ammount;
            trackerSession.mobs[itemKey] += ammount;
        }
        totalImportedMobs += ammount;
    }
}


register("command", (args1, ...args) => {
    if (args1 != undefined) {
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
        ChatLib.chat("&aFrom which mod do you want to import your Diana tracker?");
        new TextComponent("&bSkyHanni, click here").setClick("run_command", "/dianatrackerimportcheck sh " + args1).setHover("show_text", "Click Me").chat();
        new TextComponent("&bSkytils, click here").setClick("run_command", "/dianatrackerimportcheck st " + args1).setHover("show_text", "Click Me").chat();  
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
    } else {
        ChatLib.chat("&6[SBO] &eEnter your Profile name to import your Diana tracker. /sboimporttracker <profilename>");
    }
}).setName("sboimporttracker");

register("command", (args1, args2, ...args) => {
    if (args1 != undefined && args2 != undefined) {
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
        if (args1 == "sh")  ChatLib.chat("&a&lSkyHanni:");
        else if (args1 == "st") ChatLib.chat("&a&lSkytils:");
        ChatLib.chat("&aDo you want to overwrite or add on top of your current diana tracker");
        new TextComponent("&bTo overwrite, click here").setClick("run_command", "/dianatrackerimportcheck2 " + args1 + " " + args2 + " overwrite").setHover("show_text", "Click Me").chat();
        new TextComponent("&bTo add to your tracker, click here").setClick("run_command", "/dianatrackerimportcheck2 " + args1 + " " + args2 + " add").setHover("show_text", "Click Me").chat();
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
    } else {
        ChatLib.chat("&6[SBO] &cInvalid arguments. use /sboimporttracker <profilename>");
    }
}).setName("dianatrackerimportcheck");

register("command", (args1, args2, args3, ...args) => {
    if (args1 != undefined && args2 != undefined && args3 != undefined) {
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
        if (args3 == "overwrite") {
            ChatLib.chat("&aAre you sure you want to overwrite your diana tracker? (Total, Mayor, Session)");
        } 
        else if (args3 == "add") {
            ChatLib.chat("&aAre you sure you want to add the imported tracker to your diana tracker? (Total, Mayor, Session)");
        }
        new TextComponent("&bYES").setClick("run_command", "/dianatrackerimporttracker " + args1 + " " + args2 + " " + args3).setHover("show_text", "Click Me").chat();
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
    } else {
        ChatLib.chat("&6[SBO] &cInvalid arguments. use /sboimporttracker <profilename>");
    }
}).setName("dianatrackerimportcheck2");

register("command", (args1, args2, args3, ...args) => {
    if (args1 != undefined && args2 != undefined && args3 != undefined) {
        importDianaTracker(args1, args2, args3);
    }
    else {
        ChatLib.chat("&6[SBO] &cInvalid arguments. use /sboimporttracker <profilename>");
    }
}).setName("dianatrackerimporttracker");