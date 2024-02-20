import settings from "../../settings";
import { getSBID, isInSkyblock, dianaLootCounter } from '../../utils/functions';
import { registerWhen } from '../../utils/variables';
import { getWorld } from '../../utils/world';


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

function compareInventories(oldPlayerItems, newPlayerItems) {
    for (var item in newPlayerItems) {
        if (oldPlayerItems[item]) {
            diff = newPlayerItems[item] - oldPlayerItems[item];
            if (diff > 0 || diff < 0) {
                if (diff > 0) {
                    dianaLootCounter(item, diff);
                    ChatLib.chat("+ " + diff + "x " + item);
                }
                else {
                    ChatLib.chat("- " + diff*(-1) + "x " + item);
                }
            }
        }
        else {
            dianaLootCounter(item, newPlayerItems[item]);
            ChatLib.chat("+ " + newPlayerItems[item] + "x " + item);
        }
    }
    for (var item in oldPlayerItems) {
        if (!newPlayerItems[item]) {
            ChatLib.chat("- " + oldPlayerItems[item] + "x " + item);
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

registerWhen(register('step', () => {
    pickuplog();
}).setFps(10), () => settings.dianaLootTracker && isInSkyblock() && getWorld() === "Hub");