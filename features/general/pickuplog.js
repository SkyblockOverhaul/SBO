import settings from "../../settings";
import { readPlayerInventory, isInSkyblock, isWorldLoaded } from '../../utils/functions';
import { registerWhen } from '../../utils/variables';
import { getWorld } from '../../utils/world';
import { dianaLootCounter } from '../diana/DianaTracker';
import { isDataLoaded } from "../../utils/checkData";
import { checkDiana } from "../../utils/checkDiana";




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

registerWhen(register('step', () => {
    if (isDataLoaded() && isWorldLoaded() && isInSkyblock() && checkDiana()) {
        pickuplog();
    }
}).setFps(10), () => settings.dianaLootTracker);

