import settings from "../../settings";
import { registerWhen } from "../../utils/variables";
import { getWorld } from "../../utils/world";
import { isInSkyblock, trackItem, getTracker } from '../../utils/functions';

// mob tracker
registerWhen(register("chat", (woah, mob) => {
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
}).setCriteria("&c${woah}&eYou Dug out &2a ${mob}&e!"), () => getWorld() === "Hub" && settings.dianaMobTracker);

registerWhen(register("chat", () => {
    trackItem("Minos Inquisitor", "mobs", 1);
}).setCriteria("&e[NPC] Lumber Jack&f: &r&fA lumberjack always pays his debts!&r"), () => getWorld() === "Hub" && settings.dianaMobTracker);
// todo: 
// trackerMayor reset on new mayor
// mayor tracker nur reseten wenn neuer mayor diana ist
// vielleicht tracker für die letzten 5 mayor speichern

// make pickuplog only works while in no gui open
// lootchare books erkennen
// pickuplog updaterate vielleicht anpassen

// tracker[type][item]
// type = "mobs" or "items"

registerWhen(register("chat", (drop) => {
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
}).setCriteria("&r&6&lRARE DROP! &eYou dug out a ${drop}&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (coins) => {
    trackItem("coins", "items", coins);
    ChatLib.chat(coins);
}).setCriteria("&r&6&lRARE DROP! &eYou dug out &6${coins} coins&e!"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());

registerWhen(register("chat", (drop, mf) => {
    switch (drop) {
        case "Enchanted Book":
            trackItem("Chimera", "items", 1);
            break;
        case "Daedalus Stick":
            trackItem(drop, "items", 1);
            break;
        case "Potato":
            trackItem(drop, "items", 1);
            break;
        case "Carrot":
            trackItem(drop, "items", 1);
            break;
    }
}).setCriteria("&r&6&lRARE DROP! &r&f${drop} &r&b(+&r&b${mf}% &r&b✯ Magic Find&r&b)&r"), () => getWorld() === "Hub" && settings.dianaLootTracker && isInSkyblock());


// test command
register('command', () => {
    trackerSession = getTracker("Session");
    for (var item in trackerSession["items"]) {
        ChatLib.chat(item + ": " + trackerSession["items"][item]);
    }
}).setName("sbots");
register('command', () => {
    trackerMayor = getTracker("Mayor");
    for (var item in trackerMayor["items"]) {
        ChatLib.chat(item + ": " + trackerMayor["items"][item]);
    }
}).setName("sbotm");

register('command', () => {
    trackerTotal = getTracker("Total");
    for (var item in trackerTotal["items"]) {
        ChatLib.chat(item + ": " + trackerTotal["items"][item]);
    }
}).setName("sbott");

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