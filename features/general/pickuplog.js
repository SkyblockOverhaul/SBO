import settings from "../../settings";
import { readPlayerInventory, isInSkyblock, isWorldLoaded, getPurse } from '../../utils/functions';
import { registerWhen } from '../../utils/variables';
import { dianaLootCounter, trackLootWithSacks, trackScavengerCoins } from '../Diana/DianaTracker';
import { isDataLoaded } from "../../utils/checkData";
import { checkDiana } from "../../utils/checkDiana";

let newPurse = 0;
let oldPurse = 0;
function compareInventories(oldPlayerItems, newPlayerItems) {
    for (let item in newPlayerItems) {
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
    for (let item in oldPlayerItems) {
        if (!newPlayerItems[item]) {
            // ChatLib.chat("- " + oldPlayerItems[item] + "x " + item);
        }
    }
    // compare purse
    diff = newPurse - oldPurse;
    
    if (diff > 0) {
        trackScavengerCoins(diff);
    }
}

oldPlayerItems = {};
function pickuplog() {
    if (Object.keys(oldPlayerItems).length == 0) {
        oldPurse = getPurse();
        oldPlayerItems = readPlayerInventory();
        // ChatLib.chat("old inventory read");
    }
    else {
        // ChatLib.chat("comparing inventories");
        newPlayerItems = readPlayerInventory();
        newPurse = getPurse();
        if (newPlayerItems["SKYBLOCK_MENU"]) {
            compareInventories(oldPlayerItems, newPlayerItems);
            oldPlayerItems = readPlayerInventory();
            oldPurse = getPurse();
        }
    }
}

registerWhen(register('step', () => {
    if (isDataLoaded() && isWorldLoaded() && isInSkyblock() && checkDiana()) {
        pickuplog();
    }
}).setFps(10), () => settings.dianaTracker || settings.lootAnnouncerChat || settings.lootAnnouncerScreen || settings.copyRareDrop);
// const S30PacketWindowItems = net.minecraft.network.play.server.S30PacketWindowItems;
// registerWhen(register("packetReceived", (packet) => {
//     print("packet received");
//     if (isDataLoaded() && isWorldLoaded() && isInSkyblock() && checkDiana()) {
//         pickuplog();
//     }
// }).setFilteredClass(S30PacketWindowItems), () => settings.dianaTracker || settings.lootAnnouncerChat || settings.lootAnnouncerScreen || settings.copyRareDrop);



// sack detection
register("chat", (ammount, trash, time, event) => {
    message = new Message(event)
    messageParts = message.getMessageParts();
    // hide message
    if (settings.hideSackMessage) cancel(event);
    messageParts.forEach(part => {
        if (part.getText() == "§e items§r") {
            let regex = /\+([\d,]+) ([^\(]+)/g;
            let match;
            let items = [];

            while ((match = regex.exec(part.getHoverValue().removeFormatting())) !== null) {
                let amount = match[1];
                let item = match[2].trim().replace(",", "");
                items.push({amount: amount, item: item});
            }
            items.forEach(item => {
                // print("item: " + item.item + " ammount: " + item.amount);
                trackLootWithSacks(item.amount, item.item);
            });
        }
    });
}).setCriteria("&6[Sacks] ${ammount} item${trash} ${time}");
