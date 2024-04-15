import { getWorld } from "../utils/world";
import { request } from "../../requestV2";
import { toTitleCase, drawRect } from "./../utils/functions";
import { attributeShorts, allowedItemIds, ahIds, bazaarIds } from "./../utils/constants";
import settings from "./../settings";
import { registerWhen } from "./../utils/variables";
import { newOverlay } from "./../utils/overlays";
import { UIWrappedText } from "../../Elementa";

let kuudraOverlayObj = newOverlay("kuudraOverlay", settings.attributeValueOverlay, "kuudraExample", "post", "KuudraValueLoc");
let kuudraOverlay = kuudraOverlayObj.overlay


let lastUpdate = 0;
let updateing = false;
let kuudraItems = undefined;
let bazaarItems = undefined;
registerWhen(register("step", () => {
    // update every 5 minutes
    if (updateing) return;
    if (Date.now() - lastUpdate > 300000 || lastUpdate == 0) {
        // print("updating kuudra items with api");
        updateing = true;
        lastUpdate = Date.now();
        updateKuudraItems()
        setTimeout(() => {
            updateing = false;
        }, 300000);
    }
}).setFps(1), () => settings.attributeValueOverlay);

// to do:
// bazaar items: 
// Kuudra Teeth (tabasco II)  32

function getKeyPrice(tier) {
    let value = 0;
    let avgMaterialPrice = 0;
    if (settings.keySetting == 0) {
        // buy offer
        avgMaterialPrice = (bazaarItems.products["ENCHANTED_RED_SAND"].quick_status.sellPrice + bazaarItems.products["ENCHANTED_MYCELIUM"].quick_status.sellPrice)/2;
    }
    else {
        // insta buy
        avgMaterialPrice = (bazaarItems.products["ENCHANTED_RED_SAND"].quick_status.buyPrice + bazaarItems.products["ENCHANTED_MYCELIUM"].quick_status.buyPrice)/2;
    }
    // default value is 0 cases 1-5
    switch(tier) {
        case 1:
            value = 200000 + avgMaterialPrice * 2;
            break;
        case 2:
            value = 400000 + avgMaterialPrice * 6;
            break;
        case 3:
            value = 750000 + avgMaterialPrice * 20;
            break;
        case 4:
            value = 1500000 + avgMaterialPrice * 60;
            break;
        case 5:
            value = 3000000 + avgMaterialPrice * 120;
            break;
        default:
            value = 0;
            break;
    }
    return value;
}

function getBazaarPrice(itemId) {
    if (bazaarItems == undefined) return 0;
    if (bazaarItems.success == false) return 0;
    let product = bazaarItems.products[itemId];
    if (product == undefined) return 0;
    if (settings.bazaarSetting == 0) {
        return product.quick_status.sellPrice;
    }
    else {
        return product.quick_status.buyPrice;
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

    request({
        url: "https://api.hypixel.net/skyblock/bazaar?product",
        json: true
    }).then((response)=>{
        bazaarItems = response;
    }).catch((error)=>{
        console.error(error);
    });
}

chestItem = {
    name: "",
    value: 0,
    att1Name: "",
    att1Value: 0,
    att2Name: "",
    att2Value: 0,
}

class ItemString {
    constructor(string, price, index, attributeItem) {
        this.string = string;
        this.price = price;
        this.index = index;
        this.attributeItem = attributeItem;
        this.indexOfObj = undefined
        
    }
}
let tier = 0;
let cooldown = false;
registerWhen(register("step", () => {
    if (!cooldown) {
        cooldown = true;
        let scoreBoardLines = Scoreboard.getLines();
        if (scoreBoardLines != undefined) {
            if (scoreBoardLines[scoreBoardLines.length - 4] != undefined) {
                let tierString = `${scoreBoardLines[scoreBoardLines.length - 4]}`;
                tier = parseInt(tierString.slice(-2, -1));
            }
        }
        setTimeout(() => {
            cooldown = false;
        }, 30000);
    }
}).setFps(1), () => getWorld() == "Kuudra" && settings.recognizeRareRoom);

registerWhen(register("guiMouseClick", (x, y, button, gui) => {
    setTimeout(() => {
        readContainerItems();
    }, 400);
}), () => settings.attributeValueOverlay);

let chestItems = [];
registerWhen(register("guiOpened", () => {
    setTimeout(() => {
        readContainerItems();
    }, 300);
}), () => settings.attributeValueOverlay);

function getEsseceValue(essence) {
    let essenceValue = 0;
    let essencensMultiplicator = 1;
    if (settings.kuudraPetPerk == true) {
        // settings.kuudraPetLevel = settings.kuudraPetLevel.replace(/\D/g, '');
        let kuudraPetLevel = parseInt(settings.kuudraPetLevel);
        if (kuudraPetLevel > 100) {
            kuudraPetLevel = 100;
        }
        else if (kuudraPetLevel < 0) {
            kuudraPetLevel = 0;
        }
        settings.kuudraPetLevel = kuudraPetLevel;

        if (settings.kuudraPet == 0 || settings.kuudraPet == 1) {
            essencensMultiplicator += kuudraPetLevel * 0.002;
        }
        else if (settings.kuudraPet == 2 || settings.kuudraPet == 3) {
            essencensMultiplicator += kuudraPetLevel * 0.0015;
        }
        else if (settings.kuudraPet == 4) {
            essencensMultiplicator += kuudraPetLevel * 0.001;
        }
    }
    essenceValue = Math.floor(essence * essencensMultiplicator) * getBazaarPrice("ESSENCE_CRIMSON");
    return essenceValue;
}

function readContainerItems() {
    chestItems = [];
    if (kuudraItems == undefined) return;
    const container = Player.getContainer();
    if (container == null) return;
    if (container.getName() == "container") return;
    // ChatLib.chat("&6[SBO] &6&lGUI OPENED! " + container.getName());
    const items = container.getItems();
    if (items.length == 0) return;
    kuudraOverlayObj.renderGui = true;
    let highestPrice = 0;
    let tempString = "";
    let totalValue = 0;
    let essenceValue = 0;
    if (container.getName() == "Paid Chest") {
        let keyPrice = getKeyPrice(tier);
        totalValue -= keyPrice;
        tempString = `&c-${formatPrice(keyPrice)} &eTier ${tier} Key\n`;
        chestItems.push(new ItemString(tempString, -keyPrice, false));

        let essence = container.getStackInSlot(14).getNBT().toObject().tag.display.Name;
        essence = parseInt(essence.slice(essence.indexOf('x') + 1))
        
        essenceValue = getEsseceValue(essence);
        totalValue += essenceValue
        tempString = `&6${formatPrice(essenceValue)} &eCrimson Essence\n`;
        chestItems.push(new ItemString(tempString, essenceValue, false));
    }

    items.forEach((item, index) => {
        if (item == null) return;
        if (container.getName() != "container") {
            if (index >= items.length - 36) return;
        }
        itemId = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
        if (itemId == "ENCHANTED_BOOK") {
            let enchants = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("enchantments").toObject();
            itemId = ("ENCHANTMENT_" + Object.keys(enchants)[0] + "_" + enchants[Object.keys(enchants)[0]]).toUpperCase();
        } 

        if (allowedItemIds.includes(itemId)) {
            // kuudra item
            attributeDict = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes").toObject();
            if (attributeDict == null) return;

            let first = true;
            highestPrice = 0;
            
            for (let name in attributeDict) {
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
                    let displayName = toTitleCase(itemId.replaceAll("_", " "));
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
                tempString = `&6${formatPrice(highestPrice)} &e${chestItem.name} `
                if (settings.lineSetting == 0) {
                    tempString += `\n`
                }
                tempString += `&b(${chestItem.att1Name}/${chestItem.att2Name} - &6${formatPrice(chestItem.att1Value)}/${formatPrice(chestItem.att2Value)}&b)\n`;
                chestItems.push(new ItemString(tempString, highestPrice, index, true));
            }
        }
        else if (bazaarIds.includes(itemId)) {
            if (itemId == "ENCHANTED_MYCELIUM" || itemId == "ENCHANTED_RED_SAND") return;
            // bazaar item
            let price = getBazaarPrice(itemId);
            if (price == 0) return;
            totalValue += price;
            let displayName = toTitleCase(itemId.replaceAll("_", " ").replace("ULTIMATE", "").replace("ENCHANTMENT", ""));
            tempString = `&6${formatPrice(price)} &e${displayName}\n`;
            chestItems.push(new ItemString(tempString, price, index, false));
        }
        else if (ahIds.includes(itemId)) {
            // auction house item
            let price = getAhPrice(itemId);
            totalValue += price;
            if (itemId == "RUNIC_STAFF") {
                itemId = "AURORA_STAFF";
            }

            let displayName = toTitleCase(itemId.replaceAll("_", " "));
            tempString = `&6${formatPrice(price)} &e${displayName}\n`;
            chestItems.push(new ItemString(tempString, price, index, false));
        }
        else if (itemId == "ATTRIBUTE_SHARD") {
            attributeDict = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes").toObject();
            if (attributeDict == null) return;
            let name = Object.keys(attributeDict)[0];
            if (name == "mending") {
                name = "vitality";
            }
            if (!settings.attributeShards && container.getName() == "Paid Chest") {
                tempString = `&60 &e${toTitleCase(name.replaceAll("_", " "))} Shard ${attributeDict[Object.keys(attributeDict)[0]]}\n`;
                chestItems.push(new ItemString(tempString, 0, index));
            }
            else {
                let price = getAttributePrice("ATTRIBUTE_SHARD", name, attributeDict[Object.keys(attributeDict)[0]]);
                let displayName = toTitleCase(name.replaceAll("_", " ")) + " Shard";
                totalValue += price;
                tempString = `&6${formatPrice(price)} &e${displayName} ${attributeDict[Object.keys(attributeDict)[0]]}\n`;
                chestItems.push(new ItemString(tempString, price, index));
            }
        }


        
    });
    refreshOverlay(totalValue);
}

registerWhen(register("guiClosed", () => {
    indexToHighlight = -1;
    kuudraOverlay.clearChildren();
    kuudraOverlayObj.renderGui = false;
    chestItems = [];
    guiStrings = [];
}), () => settings.attributeValueOverlay);


let indexToHighlight = -1;
registerWhen(register("renderSlot", (slot) => {
    if (indexToHighlight != -1) {
        if (slot.getIndex() == indexToHighlight) {
            let x = slot.getDisplayX();
            let y = slot.getDisplayY();
            drawRect(x, y, 2.5, 200);
        }
    }
}), () => settings.attributeValueOverlay);

let guiStrings = [];
registerWhen(register("step", () => {
    let tempBool = false;
    if (guiStrings.length == 0) return;
    chestItems.forEach((item) => {
        if (guiStrings[item.indexOfObj] == undefined) return;
        if (guiStrings[item.indexOfObj].isHovered()) {
            tempBool = true;
            indexToHighlight = item.index;
        }
    })
    if (!tempBool) {
        indexToHighlight = -1;
    }
}).setFps(20), () => settings.attributeValueOverlay);


function refreshOverlay(totalValue) {
    // sort itemStrings by price
    chestItems.sort((a, b) => {
        return b.price - a.price;
    });
    guiStrings = [];
    let overlayString = "";
    let counter = 1;
    let tempObj = undefined;
    let pixel = 0;
    let pixelIncrementOne = 9;
    let pixelIncrementTwo = 18;
    let tempPixel = 0;
    let maxStringWidth = 0;
    let withMultiplicator = 5.3;
    chestItems.forEach((item) => {
        if (settings.lineSetting == 0) {
            if (item.attributeItem) {
                tempPixel = pixelIncrementTwo;
            }
            else {
                tempPixel = pixelIncrementOne;
            }
        }
        else {
            tempPixel = pixelIncrementOne;
        }
        if (counter <= settings.maxDisplayedItems) {
            // tempObj = ;
            guiStrings.push(new UIWrappedText(item.string));
            item.indexOfObj = guiStrings.length-1;
            guiStrings[item.indexOfObj].setX((0).pixels());
            guiStrings[item.indexOfObj].setY((pixel).pixels());
            maxStringWidth = item.string.split("\n").reduce((a, b) => a.length > b.length ? a : b).length;
            guiStrings[item.indexOfObj].setWidth((maxStringWidth * withMultiplicator).pixels());
            guiStrings[item.indexOfObj].setHeight((tempPixel).pixels());

            guiStrings[item.indexOfObj].onMouseLeave((comp) => {
                maxStringWidth = item.string.split("\n").reduce((a, b) => a.length > b.length ? a : b).length;
                guiStrings[item.indexOfObj].setWidth((maxStringWidth * withMultiplicator).pixels());
                guiStrings[item.indexOfObj].setText(item.string);
            });
            guiStrings[item.indexOfObj].onMouseEnter((comp) => {
                maxStringWidth = item.string.replaceAll("&6", "&6&l").replaceAll("&e", "&e&l").replaceAll("&b", "&b&l").split("\n").reduce((a, b) => a.length > b.length ? a : b).length;
                guiStrings[item.indexOfObj].setWidth((maxStringWidth * withMultiplicator).pixels());
                // guiStrings[item.indexOfObj].effects;
                guiStrings[item.indexOfObj].setText(item.string.replaceAll("&6", "&6&l").replaceAll("&e", "&e&l").replaceAll("&b", "&b&l"));
            });
            // check if gui object is hovered
            

            // guiStrings.push(tempObj);
            pixel += tempPixel;
        }
        counter++;
    });
    if (counter > settings.maxDisplayedItems) {
        overlayString += `&o&7and ${counter - settings.maxDisplayedItems} more...\n`;
        tempObj = new UIWrappedText(`&o&7and ${counter - settings.maxDisplayedItems} more...\n`);
        tempObj.setX((0).pixels());
        tempObj.setY((pixel).pixel());
        guiStrings.push(tempObj);
        pixel += pixelIncrementOne;
    }
    if (totalValue != 0) {
        overlayString += `&eTotal Value: &6${formatPrice(totalValue)} coins`;
        tempObj = new UIWrappedText(`&eTotal Value: &6${formatPrice(totalValue)} coins`);
        tempObj.setX((0).pixels());
        tempObj.setY((pixel).pixel());
        guiStrings.push(tempObj);
        pixel += pixelIncrementOne;
    }
    kuudraOverlay.clearChildren();
    // let kuudraText = new UIWrappedText(overlayString);
    // kuudraText.setX(new SiblingConstraint());
    // kuudraText.setY(new SiblingConstraint());
    for (let i = 0; i < guiStrings.length; i++) {
        kuudraOverlay.addChild(guiStrings[i]);
    }
    // testOverlay.addChild(kuudraText);
}

function formatPrice(price) {
    if (price >= 1000000000) {
        return (price / 1000000000).toFixed(2) + "B";
    }
    else if (price >= 1000000) {
        return (price / 1000000).toFixed(2) + "M";  
    }
    else if (price >= 1000) {
        return Math.floor(price / 1000) + "K";
    }
    return price;
}

function getAttributePrice(itemId, attribute, lvl) {
    let tier5Shard = false;
    let valueModifier = 1;
    if (itemId != "ATTRIBUTE_SHARD") {
        itemId = itemId.split("_")[1]
    }
    else {
        if (attribute != "magic_find") {
            valueModifier = 0.8;
        }
    }

    if (kuudraItems[itemId] != undefined) {
        if (itemId == "ATTRIBUTE_SHARD" && lvl >= 4) {
            if (attribute == "magic_find") {
                lvl = 4;
            }
            else {
                lvl = 3;
                tier5Shard = true;
            }
        }
        else {
            tier5Shard = false;
        }
            
        if (kuudraItems[itemId][attribute + "_" + lvl] != undefined) {
            if (tier5Shard) {
                return (kuudraItems[itemId][attribute + "_" + lvl].price * 2) * valueModifier;
            }
            return (kuudraItems[itemId][attribute + "_" + lvl].price) * valueModifier;
        }
        else {
            let counter = 0;
            for (let i = lvl; i > 0; i--) {
                counter++;
                if (kuudraItems[itemId][attribute + "_" + i] != undefined) {
                    // print("attribute: " + attribute + " " + i + " price: " + kuudraItems[itemId][attribute + "_" + i].price);
                    if (tier5Shard) {
                        return (kuudraItems[itemId][attribute + "_" + i].price * (2**counter) * 2) * valueModifier;
                    }
                    return (kuudraItems[itemId][attribute + "_" + i].price * (2**counter)) * valueModifier;
                }
                else {
                    console.log("attribute: " + attribute + " " + i + " price: not found");
                
                }
            }
            console.log("attribute: " + attribute + " " + lvl + " price: not found");
            return 0;
        }
    }
    else {
        console.log("itemId: " + itemId + " price: not found");
        return 0;
    }
}

function getAhPrice(itemId) {
    if (kuudraItems["OTHER"][itemId] != undefined) {
        return kuudraItems["OTHER"][itemId].price;
    }
    else {
        console.log("itemId: " + itemId + " price: not found");
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