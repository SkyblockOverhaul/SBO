
import { request } from "../../requestV2";
import { toTitleCase } from "./../utils/functions";
import { Overlay } from "./../utils/overlay";
import { attributeShorts } from "./../utils/constants";
import settings from "./../settings";
import { registerWhen } from "./../utils/variables";
import { loadGuiSettings, saveGuiSettings } from "./../utils/functions";

let guiSettings = loadGuiSettings();
let loadKuudraOverlay = false;

let lastUpdate = 0;
let updateing = false;
let kuudraItems = undefined;
register("step", () => {
    // update every 5 minutes
    kuudraOverlay();
    if (updateing) return;
    if (Date.now() - lastUpdate > 300000 || lastUpdate == 0) {
        updateing = true;
        lastUpdate = Date.now();
        updateKuudraItems()
        setTimeout(() => {
            updateing = false;
        }, 300000);
    }
}).setFps(1);

function kuudraOverlay() {
    if (settings.attributeValueOverlay) {
        if(guiSettings != undefined && !loadKuudraOverlay) {
            kuudraValueOverlay.setX(guiSettings["KuudraValueLoc"]["x"]);
            kuudraValueOverlay.setY(guiSettings["KuudraValueLoc"]["y"]);
            kuudraValueOverlay.setScale(guiSettings["KuudraValueLoc"]["s"]);
            loadKuudraOverlay = true;
        }
        if( guiSettings["KuudraValueLoc"]["x"] != kuudraValueOverlay.X || guiSettings["KuudraValueLoc"]["y"] != kuudraValueOverlay.Y || guiSettings["KuudraValueLoc"]["s"] != kuudraValueOverlay.S)
        {
            guiSettings["KuudraValueLoc"]["x"] = kuudraValueOverlay.X;
            guiSettings["KuudraValueLoc"]["y"] = kuudraValueOverlay.Y;
            guiSettings["KuudraValueLoc"]["s"] = kuudraValueOverlay.S;
            saveGuiSettings(guiSettings);
        }
    }
}

function updateKuudraItems() {
    request({
        url: "https://api.skyblockoverhaul.com/kuudraItems",
        json: true
    }).then((response)=>{
        kuudraItems = response;
    }).catch((error)=>{
        console.error(error);
    });
}

let allowedItemIds = [
    "CRIMSON_HELMET",
    "CRIMSON_CHESTPLATE",
    "CRIMSON_LEGGINGS",
    "CRIMSON_BOOTS",
    "AURORA_HElMET",
    "AURORA_CHESTPLATE",
    "AURORA_LEGGINGS",
    "AURORA_BOOTS",
    "HOLLOW_HELMET",
    "HOLLOW_CHESTPLATE",
    "HOLLOW_LEGGINGS",
    "HOLLOW_BOOTS",
    "TERROR_HELMET",
    "TERROR_CHESTPLATE",
    "TERROR_LEGGINGS",
    "TERROR_BOOTS",
    "FERVOR_HELMET",
    "FERVOR_CHESTPLATE",
    "FERVOR_LEGGINGS",
    "FERVOR_BOOTS",
    "MOLTEN_NECKLACE",
    "MOLTEN_BELT",
    "MOLTEN_BRACELET",
    "MOLTEN_CLOAK",];

let overlayStringExample = `&r&62.49M &r&eTerror Chestplate&r
&r&b(BL 5/BR 4 - &r&6100.00K/2.49M&b)
&r&62.50M &r&eTerror Boots&r
&r&b(ER 5/DO 4 - &r&61.48M/2.50M&b)
&r&eTotal Value: &r&64.99M coins`;

let kuudraValueOverlay = new Overlay("attributeValueOverlay", ["all", "misc"], [10, 10, 0], "sbomoveValueOverlay", overlayStringExample, "attributeValueOverlay");
kuudraValueOverlay.setRenderGuiBool(false);

register("guiClosed", () => {
    kuudraValueOverlay.setRenderGuiBool(false);
});

chestItem = {
    name: "",
    value: 0,
    att1Name: "",
    att1Value: 0,
    att2Name: "",
    att2Value: 0,
}

class KuudraItem {
    constructor(string, price, index) {
        this.index = index
        this.string = string;
        this.price = price;
    }
}


registerWhen(register("guiMouseClick", (x, y, button, gui) => {
    setTimeout(() => {
        readContainerItems();
    }, 200);
}), () => settings.attributeValueOverlay);

let chestItems = [];
registerWhen(register("guiOpened", () => {
    setTimeout(() => {
        readContainerItems();
    }, 100);
}), () => settings.attributeValueOverlay);

function readContainerItems() {
    chestItems = [];
    if (kuudraItems == undefined) return;
    const container = Player.getContainer();
    if (container == null) return;
    if (container.getName() == "container") return;
    // ChatLib.chat("&r&6[SBO] &r&6&lGUI OPENED! " + container.getName());
    const items = container.getItems();
    if (items.length == 0) return;
    
    let highestPrice = 0;
    let tempString = "";
    let totalValue = 0;
    items.forEach((item, index) => {
        if (item == null) return;
        if (container.getName() != "container") {
            if (index >= items.length - 36) return;
        }
        attributeDict = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes").toObject();
        if (attributeDict == null) return;

        let first = true;
        highestPrice = 0;
        
        for (let name in attributeDict) {
            itemId = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
            if (!allowedItemIds.includes(itemId)) return;
            let lvl = attributeDict[name];
            if (name == "mending") {
                name = "vitality";
            }
            let price = getPrice(itemId, name, lvl);
            if (price >= highestPrice) {
                highestPrice = price;
            }
            
            if (first) {
                first = false;
                let displayName = toTitleCase(itemId.replace("_", " "));
                chestItem.name = displayName;
                chestItem.att1Name = attributeShorts[name]+ " " + lvl;
                chestItem.att1Value = price;
            }
            else {
                chestItem.att2Name = attributeShorts[name] + " " + lvl;
                chestItem.att2Value = price;
                chestItem.value = highestPrice;
            }     
        }
        if (highestPrice != 0) {
            totalValue += highestPrice;
            tempString = `&r&6${formatPrice(highestPrice)} &r&e${chestItem.name}&r `
            if (settings.monitorSetting == 0) {
                tempString += `\n`
            }
            tempString += `&r&b(${chestItem.att1Name}/${chestItem.att2Name} - &r&6${formatPrice(chestItem.att1Value)}/${formatPrice(chestItem.att2Value)}&b)\n`;
            chestItems.push(new KuudraItem(tempString, highestPrice));
        }
    });
    refreshOverlay(totalValue);
}

function refreshOverlay(totalValue) {
    // sort itemStrings by price
    chestItems.sort((a, b) => {
        return b.price - a.price;
    });

    let overlayString = "";
    let counter = 0;
    chestItems.forEach((item) => {
        counter++;
        if (counter <= settings.maxDisplayedItems) {
            overlayString += item.string;
        }
    });
    if (counter > settings.maxDisplayedItems) {
        overlayString += `&r&o&7and ${counter - settings.maxDisplayedItems} more...\n`;
    }
    if (totalValue > 0) {
        overlayString += `&r&eTotal Value: &r&6${formatPrice(totalValue)} coins`;
    }
    kuudraValueOverlay.message = overlayString;
    kuudraValueOverlay.setRenderGuiBool(true);
}

function formatPrice(price) {
    if (price >= 1000000) {
        return (price / 1000000).toFixed(2) + "M";
    }
    else if (price >= 1000) {
        return (price / 1000).toFixed(2) + "K";
    }
    return price;
}

function getAttributePrice(itemId, attribute, lvl) {
    if (kuudraItems[itemId.split("_")[1]][attribute + "_" + lvl] != undefined) {
        return kuudraItems[itemId.split("_")[1]][attribute + "_" + lvl].price;
    }
    else {
        let counter = 0;
        for (let i = lvl; i > 0; i--) {
            counter++;
            if (kuudraItems[itemId.split("_")[1]][attribute + "_" + i] != undefined) {
                return kuudraItems[itemId.split("_")[1]][attribute + "_" + i].price * (2**counter);
            }
        }
        console.log("attribute: " + attribute + " " + lvl + " price: not found");
        return 0;
    }
}

function getPrice(itemId, attribute, lvl) {
    if (lvl <= 5) {
       return getAttributePrice(itemId, attribute, lvl);
    }
    else {
        // calc price from lvl 5 up to lvl
        let price = getAttributePrice(itemId, attribute, 5)
        for (let i = 5; i < lvl; i++) {
            price *= 2;
        }
        return price;
    }
}