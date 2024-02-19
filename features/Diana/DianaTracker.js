import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock } from '../../utils/functions.js';

// mob tracker
registerWhen(register("chat", (woah, mob) => {
    switch (mob){
        case "Minos Inquisitor":
            ChatLib.chat("Minos Inquisitor");
            break;
    }
}).setCriteria("&c${woah}&eYou Dug out &2a ${mob}&e!"), () => getWorld() === "Hub" && settings.dianaMobTracker);

// loot tracker
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
export function dianaLootCounter(item, amount) {
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

registerWhen(register("chat", (drop) => {
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
}).setCriteria("&r&6&lRARE DROP! &eYou dug out a ${drop}&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (coins) => {
    ChatLib.chat(coins);
}).setCriteria("&r&6&lRARE DROP! &eYou dug out &6${coins} coins&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (drop, mf) => {
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
}).setCriteria("&r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());


// test command
register('command', () => {
    for (var item in loot) {
        ChatLib.chat(item + ": " + loot[item]);
    }
}).setName("sbotest");

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