/// <reference types="../CTAutocomplete" />
import Settings from "./settings";

register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

export function getSBID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("id") || null;
}
export function getSBUUID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("uuid") || null;
}

function readPlayerInventory() {
    playerItems = {}
    var playerInv = Player.getInventory();
    var playerInvItems = playerInv.getItems();
    for (var i in playerInv.getItems()) {
        if (playerInvItems[i] !== null) {
            if (playerItems[getSBID(playerInvItems[i])]) {
                playerItems[getSBID(playerInvItems[i])] += playerInvItems[i].getStackSize();
            }
            else {
                playerItems[getSBID(playerInvItems[i])] = playerInvItems[i].getStackSize();
            }
        }
    }
    return playerItems;
}

fileLocation = "config/ChatTriggers/modules/SBO/dianaLootCounter.json";
function loadLoot() {
    try {
    loot = JSON.parse(FileLib.read(fileLocation));
    }
    catch (e) {
        loot = {};
        saveLoot(loot);
    }
    if (loot === null) {
        loot = {};
    }
    return loot;
}


function saveLoot(loot) {
    FileLib.write(fileLocation, JSON.stringify(loot));
}

var loot = loadLoot();
function dianaLootCounter(item, amount) {
    countThisIds = ["ROTTEN_FLESH", "WOOD"]
    for (var i in countThisIds.values()) {
        if (item === i) {
            if (loot[item]) {
                loot[item] += amount;
                // ChatLib.chat("counted loot " + item + " " + amount);
            }
            else {
                loot[item] = amount;
                // ChatLib.chat("counted loot " + item + " " + amount);
            }
            saveLoot(loot);
        }
    }
}

register("chat", (woah, mob) => {
    switch (mob){
        case "Minos Inquisitor":
            ChatLib.chat("Minos Inquisitor");
            break;
    }
}).setCriteria("&c${woah}&eYou Dug out &2a ${mob}&e!")

register("chat", (drop) => {
    drop = drop.removeFormatting();
    switch (drop) {
        case "Griffin Feather":
            ChatLib.chat("Griffin Feather");
            break;
        case "Crown of Greed":
            ChatLib.chat("Crown of Greed");
            break;
        case "Washed-up Souvenir":
            ChatLib.chat("Washed-up Souvenir");
            break;
    }
}).setCriteria("&r&6&lRARE DROP! &eYou dug out a ${drop}&e!")

register("chat", (coins) => {
    ChatLib.chat(coins);
}).setCriteria("&r&6&lRARE DROP! &eYou dug out &6${coins} coins&e!")

register("chat", (drop, mf) => {
    switch (drop) {
        case "Enchanted Book":
            ChatLib.chat("Chimera");
            break;
        case "Potato":
            ChatLib.chat("Potato");
            break;
        case "Carrot":
            ChatLib.chat("Carrot");
            break;
    }
}).setCriteria("&r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r") // &r&6&lRARE DROP! &r&fCarrot &r&b(+&r&b313% &r&b✯ Magic Find&r&b)&r

function compareInventories(oldPlayerItems, newPlayerItems) {
    for (var item in newPlayerItems) {
        if (oldPlayerItems[item]) {
            diff = newPlayerItems[item] - oldPlayerItems[item];
            if (diff > 0 || diff < 0) {
                if (diff > 0) {
                    dianaLootCounter(item, diff);
                    // ChatLib.chat("+ " + diff + "x " + item);
                }
                else {
                    // ChatLib.chat("- " + diff*(-1) + "x " + item);
                }
            }
        }
        else {
            dianaLootCounter(item, newPlayerItems[item]);
            // ChatLib.chat("+ " + newPlayerItems[item] + "x " + item);
        }
    }
    for (var item in oldPlayerItems) {
        if (!newPlayerItems[item]) {
            // ChatLib.chat("- " + oldPlayerItems[item] + "x " + item);
        }
    }
}

oldPlayerItems = {};
function pickuplog() {
    if (oldPlayerItems.length === 0) {
        oldPlayerItems = readPlayerInventory();
        // ChatLib.chat("old inventory read");
    }
    else {
        // ChatLib.chat("comparing inventories");
        newPlayerItems = readPlayerInventory();
        if (newPlayerItems["SKYBLOCK_MENU"]) {
            compareInventories(oldPlayerItems, newPlayerItems);
            oldPlayerItems = readPlayerInventory();
        }
    }
}


register('step', () => {
    isInSkyblock = Scoreboard.getTitle()?.removeFormatting().includes("SKYBLOCK");
    if (isInSkyblock) { // Überprüfen Sie, ob der Spieler in SkyBlock ist
        if (World.getWorld() != null) { // Überprüfen Sie, ob der Spieler in einer Welt ist
            pickuplog();
        }
    }
}).setFps(20);

register('command', () => {
    for (var item in loot) {
        ChatLib.chat(item + ": " + loot[item]);
    }
}).setName("sbotest").setAliases("sbot");


  
// register('packetReceived', () => {
//     ChatLib.chat("item pickup packet received!");

//     try {
//         newIventory = readPlayerInventory();
//         while (oldIventory === newIventory) {
//             newIventory = readPlayerInventory();
//         }
//     ChatLib.chat("found new item!")
//     }
//     catch (e) {
//         ChatLib.chat(e);
//     }
// }).setPacketClasses([net.minecraft.network.play.server.S0DPacketCollectItem]);;



    

// simpleTracker = {}
// register("pickupItem", (item) => {
//     if (simpleTracker[item.getName()]) {
//         simpleTracker[item.getName()] += item.getStackSize();
//     } else {
//         simpleTracker[item.getName()] = item.getStackSize();
//     }
//     // ChatLib.chat("Name: " +  item.getName() + " Id: " + item.getID() + " stack size: " + item.getStackSize());
// });

// 


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