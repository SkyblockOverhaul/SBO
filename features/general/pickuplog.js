import settings from "../../settings";
import { readPlayerInventory, isInSkyblock, isWorldLoaded, getPurse, printDev, getSBID } from '../../utils/functions';
import { registerWhen } from '../../utils/variables';
import { dianaLootCounter, trackLootWithSacks, trackScavengerCoins } from '../Diana/DianaTracker';
import { isDataLoaded } from "../../utils/checkData";
import { checkDiana } from "../../utils/checkDiana";
import { OverlayTextLine, SboOverlay, isGuiOpened } from "../../utils/overlays";

let pickuplogOverlay = new SboOverlay("pickuplog", "pickuplogOverlay", "render", "PickupLogLoc", "pickupLogExample");
let pickuplogOverlayText = new OverlayTextLine("");

let itemsShownAdded = {};
let itemsShownRemoved = {};
let guiBool = false;
let trackBool = false;
let refreshBool = false;

function refreshPickuplogOverlay(item, displayName, ammount) {
    // if (isGuiOpened()) return;
    // if (refreshBool) return;

    if (!isGuiOpened() && !refreshBool) {
        if (item != "null" && ammount != undefined) {
            if (ammount > 0) {
                if (itemsShownAdded[item]) {
                    itemsShownAdded[item][0] += ammount;
                    itemsShownAdded[item][2] = Date.now();
                }
                else {
                    itemsShownAdded[item] = [ammount, displayName, Date.now()];
                }
            }
            else {
                if (itemsShownRemoved[item]) {
                    itemsShownRemoved[item][0] += ammount;
                    itemsShownRemoved[item][2] = Date.now();
                }
                else {
                    itemsShownRemoved[item] = [ammount, displayName, Date.now()];
                }
            }
        }
    }
    let text = "";
    text = createPickupLogText(itemsShownAdded);
    text += createPickupLogText(itemsShownRemoved);
    pickuplogOverlay.setLines([pickuplogOverlayText.setText(text)]);
}


function createPickupLogText(dict) {
    let text = "";
    for (let item in dict) {
        if (Date.now() - dict[item][2] > 6000) {
            delete dict[item];
            continue;
        }
        if (dict[item][0] > 0) {
            text += "&a+ " + dict[item][0] + "x &r" + dict[item][1] + "\n";
        }
        else {
            text += "&c- " + dict[item][0]*(-1) + "x &r" + dict[item][1] + "\n";
        }
    }
    return text
}

let newPurse = 0;
let oldPurse = 0;
let tempInv = {};   
function compareInventories(oldPlayerItems, newPlayerItems, onlyOverlay) {
    for (let item in newPlayerItems) {
        if (oldPlayerItems[item]) {
            diff = newPlayerItems[item][0] - oldPlayerItems[item][0];
            if (diff > 0 || diff < 0) {
                if (diff > 0) {
                    if (!onlyOverlay) dianaLootCounter(item, diff);
                    refreshPickuplogOverlay(item, newPlayerItems[item][1], diff);
                    // ChatLib.chat("+ " + diff + "x " + newPlayerItems[item][1]);
                }
                else {
                    refreshPickuplogOverlay(item, newPlayerItems[item][1], diff);
                    // ChatLib.chat("- " + diff*(-1) + "x " + newPlayerItems[item][1]);
                }
            }
        }
        else {
            if (!onlyOverlay) dianaLootCounter(item, newPlayerItems[item][0]);
            refreshPickuplogOverlay(item, newPlayerItems[item][1], newPlayerItems[item][0]);
            // ChatLib.chat("+ " + newPlayerItems[item] + "x " + newPlayerItems[item][1]);
        }
    }
    for (let item in oldPlayerItems) {
        if (!newPlayerItems[item]) {
            refreshPickuplogOverlay(item, oldPlayerItems[item][1], oldPlayerItems[item][0]*-1);
            // ChatLib.chat("- " + oldPlayerItems[item] + "x " + item);
        }
    }
    // compare purse
    diff = newPurse - oldPurse;
    
    if (diff > 0) {
        trackScavengerCoins(diff);
    }
    if (onlyOverlay) {
        refreshBool = true;
    }
    if (refreshBool && !onlyOverlay) {
        refreshBool = false;
    }
}

let oldPlayerItems = {};
let firstLoad = false;
function pickuplog() {
    if (!firstLoad) {
        oldPurse = getPurse();
        oldPlayerItems = readPlayerInventory("");
        firstLoad = true;
    }
    if (!guiBool && isGuiOpened()) {
        guiBool = true;
        tempInv = Object.assign({}, oldPlayerItems);
    }
    else if (guiBool && !isGuiOpened()) {
        trackBool = true
        guiBool = false;
    }

    newPlayerItems = readPlayerInventory("");
    newPurse = getPurse();
    if (trackBool) {
        compareInventories(tempInv, newPlayerItems, true);
        trackBool = false;
        tempInv = {};
    }
    compareInventories(oldPlayerItems, newPlayerItems, false);
    oldPlayerItems = readPlayerInventory("");
    oldPurse = getPurse();

    refreshPickuplogOverlay("null", 0); 
}

registerWhen(register('step', () => {
    if (isDataLoaded() && isWorldLoaded() && isInSkyblock() && (checkDiana() || settings.pickuplogOverlay)) {
        // print("pickuplog step")
        pickuplog();
    }
}).setFps(10), () => settings.dianaTracker || settings.lootAnnouncerChat || settings.lootAnnouncerScreen || settings.copyRareDrop || settings.pickuplogOverlay || settings.dianaStatsTracker);

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
