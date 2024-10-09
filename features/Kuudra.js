import { getWorld } from "../utils/world";
import { toTitleCase, drawRect, getKuudraItems, getBazaarItems, getBazaarPriceKuudra, formatNumber, setTimeout } from "./../utils/functions";
import { attributeShorts, allowedItemIds, ahIds, bazaarIds } from "./../utils/constants";
import settings from "./../settings";
import { registerWhen } from "./../utils/variables";
import { OverlayTextLine, SboOverlay } from "./../utils/overlays";

let kuudraOverlay = new SboOverlay("kuudraOverlay", "attributeValueOverlay", "post", "KuudraValueLoc", "kuudraExample", true, ["GuiChest"])
let kuudraOverlayText = new OverlayTextLine("");

let kuudraItems = undefined;
let bazaarItems = undefined;    

function getKeyDiscount(price) {
    return settings.keyDiscount ? price * 0.8 : price;
}

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
            value = getKeyDiscount(200000) + avgMaterialPrice * 2;
            break;
        case 2:
            value = getKeyDiscount(400000) + avgMaterialPrice * 6;
            break;
        case 3:
            value = getKeyDiscount(750000) + avgMaterialPrice * 20;
            break;
        case 4:
            value = getKeyDiscount(1500000) + avgMaterialPrice * 60;
            break;
        case 5:
            value = getKeyDiscount(3000000) + avgMaterialPrice * 120;
            break;
        default:
            value = 0;
            break;
    }
    return value;
}

let chestItem = {
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
registerWhen(register("step", () => {
    if (Scoreboard != undefined && tier == 0) {
        let scoreBoardLines = Scoreboard.getLines();
        if (scoreBoardLines !== undefined) {
            scoreBoardLines.forEach((line) => {
                let lineString = String(line);
                let cleanLine = lineString.replace(/§[0-9a-f]/g, "").trim();

                if (cleanLine.includes("Kuudra's") && cleanLine.includes("Hollow")) {
                let match = cleanLine.match(/T(\d+)/);
                if (match) {
                    tier = parseInt(match[1], 10); 
                }
                }
            });
        }
    }
}).setFps(1), () => getWorld() === "Kuudra" && settings.attributeValueOverlay);

registerWhen(register("worldUnload", () => {
    tier = 0;
}), () => settings.attributeValueOverlay);

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
    essenceValue = Math.floor(essence * essencensMultiplicator) * getBazaarPriceKuudra("ESSENCE_CRIMSON");
    return essenceValue;
}

function readContainerItems() {
    chestItems = [];
    kuudraItems = getKuudraItems();
    bazaarItems = getBazaarItems();
    if (kuudraItems == undefined) return;
    if (bazaarItems == undefined) return;   
    const container = Player.getContainer();
    if (container == null) return;
    if (container.getName() == "container") return;
    // ChatLib.chat("&6[SBO] &6&lGUI OPENED! " + container.getName());
    const items = container.getItems();
    if (items.length == 0) return;
    // kuudraOverlayObj.renderGui = true;
    kuudraOverlay.renderGui = true;
    let highestPrice = 0;
    let tempString = "";
    let totalValue = 0;
    let essenceValue = 0;
    if (container.getName() == "Paid Chest") {
        let keyPrice = getKeyPrice(tier);
        totalValue -= keyPrice;
        tempString = `&c-${formatNumber(keyPrice)} &eTier ${tier} Key`;
        chestItems.push(new ItemString(tempString, -keyPrice, false));

        let essence = container.getStackInSlot(14).getNBT().toObject().tag.display.Name;
        essence = parseInt(essence.slice(essence.indexOf('x') + 1))
        
        essenceValue = getEsseceValue(essence);
        totalValue += essenceValue
        tempString = `&6${formatNumber(essenceValue)} &eCrimson Essence`;
        chestItems.push(new ItemString(tempString, essenceValue, false));
    }

    items.forEach((item, index) => {
        if (item == null) return;
        if (container.getName() != "container") {
            if (index >= items.length - 36) return;
        }
        let itemId = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
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
                tempString = `&6${formatNumber(highestPrice)} &e${chestItem.name} `
                if (settings.lineSetting == 0) {
                    tempString += `\n`
                }
                tempString += `&b(${chestItem.att1Name}/${chestItem.att2Name} - &6${formatNumber(chestItem.att1Value)}/${formatNumber(chestItem.att2Value)}&b)`;
                chestItems.push(new ItemString(tempString, highestPrice, index, true));
            }
        }
        else if (bazaarIds.includes(itemId)) {
            if (itemId == "ENCHANTED_MYCELIUM" || itemId == "ENCHANTED_RED_SAND") return;
            // bazaar item
            let price = getBazaarPriceKuudra(itemId);
            if (price == 0) return;
            totalValue += price;
            let displayName = toTitleCase(itemId.replaceAll("_", " ").replace("ULTIMATE", "").replace("ENCHANTMENT", ""));
            tempString = `&6${formatNumber(price)} &e${displayName}`;
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
            tempString = `&6${formatNumber(price)} &e${displayName}`;
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
                tempString = `&60 &e${toTitleCase(name.replaceAll("_", " "))} Shard ${attributeDict[Object.keys(attributeDict)[0]]}`;
                chestItems.push(new ItemString(tempString, 0, index));
            }
            else {
                let price = getAttributePrice("ATTRIBUTE_SHARD", name, attributeDict[Object.keys(attributeDict)[0]]);
                let displayName = toTitleCase(name.replaceAll("_", " ")) + " Shard";
                totalValue += price;
                tempString = `&6${formatNumber(price)} &e${displayName} ${attributeDict[Object.keys(attributeDict)[0]]}`;
                chestItems.push(new ItemString(tempString, price, index));
            }
        }


        
    });
    refreshOverlay(totalValue);
}

registerWhen(register("guiClosed", () => {
    indexToHighlight = -1;
    // kuudraOverlaywww.clearChildren();
    // kuudraOverlayObj.renderGui = false;
    kuudraOverlay.renderGui = false;
    kuudraOverlay.setLines([kuudraOverlayText.setText("")]);
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
        if (guiStrings[item.indexOfObj].isHovered) {
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
    chestItems.forEach((item) => {
        if (counter <= settings.maxDisplayedItems) {
            guiStrings.push(new OverlayTextLine(item.string, true, true));
            item.indexOfObj = guiStrings.length-1;

            guiStrings[item.indexOfObj].onMouseLeave((comp) => {
                guiStrings[item.indexOfObj].setText(item.string);
            });
            guiStrings[item.indexOfObj].onMouseEnter((comp) => {
                guiStrings[item.indexOfObj].setText(item.string.replaceAll("&6", "&c").replaceAll("&e", "&c").replaceAll("&b", "&c"));
            });
        }
        counter++;
    });
    if (counter > settings.maxDisplayedItems) {
        overlayString += `&o&7and ${counter - settings.maxDisplayedItems} more...`;
        tempObj = new OverlayTextLine(`&o&7and ${counter - settings.maxDisplayedItems} more...`);
        guiStrings.push(tempObj);
    }
    if (totalValue != 0) {
        overlayString += `&eTotal Value: &6${formatNumber(totalValue)} coins`;
        tempObj = new OverlayTextLine(`&eTotal Value: &6${formatNumber(totalValue)} coins`);
        guiStrings.push(tempObj);
    }
    kuudraOverlay.setLines(guiStrings);
    if (guiStrings.length == 0) {
        kuudraOverlay.renderGui = false;
    }
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