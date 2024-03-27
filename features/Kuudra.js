
import { request } from "../../requestV2";
import { toTitleCase } from "./../utils/functions";
import { Overlay } from "./../utils/overlay";
import { attributeShorts } from "./../utils/constants";
import settings from "./../settings";
import { registerWhen } from "./../utils/variables";
import { loadGuiSettings, saveGuiSettings } from "./../utils/functions";

let guiSettings = loadGuiSettings();
let loadKuudraOverlay = false;



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

let lastUpdate = 0;
let updateing = false;
let kuudraItems = undefined;
let bazaarItems = undefined;
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

// loot table (price erkennen)
// auction house items:
// Enrager ignore attribute
// Burning Kuudra Core
// Aurora Staff ignore attribute
// Hollow Wand ignore attribute
// Tentacle Dye
// all shards
// Wheel Of Fate

// bazaar items: 
// Crimson Essence (kuudrapet auslesen (mehr essencen durch perk)) 
// Kuudra Teeth (tabasco II)  32

// redsand and mycelium  
// avg key price tier 1 - 5 
// tier 1: 200k 2
// tier 2: 400k 6
// tier 3: 750k 20
// tier 4: 1.5m 60
// tier 5: 3m 120

// tier erkennen (siehe volc)

bazaarIds = [
    "MANDRAA", 
    "KUUDRA_MANDIBLE", 
    "ENCHANTMENT_STRONG_MANA_1",
    "ENCHANTMENT_STRONG_MANA_2",
    "ENCHANTMENT_STRONG_MANA_3",
    "ENCHANTMENT_STRONG_MANA_4",
    "ENCHANTMENT_STRONG_MANA_5",
    "ENCHANTMENT_FEROCIOUS_MANA_1",
    "ENCHANTMENT_FEROCIOUS_MANA_2",
    "ENCHANTMENT_FEROCIOUS_MANA_3",
    "ENCHANTMENT_FEROCIOUS_MANA_4",
    "ENCHANTMENT_FEROCIOUS_MANA_5",
    "ENCHANTMENT_HARDENED_MANA_1",  
    "ENCHANTMENT_HARDENED_MANA_2",  
    "ENCHANTMENT_HARDENED_MANA_3",
    "ENCHANTMENT_HARDENED_MANA_4",
    "ENCHANTMENT_HARDENED_MANA_5",
    "ENCHANTMENT_MANA_VAMPIRE_1",
    "ENCHANTMENT_MANA_VAMPIRE_2",
    "ENCHANTMENT_MANA_VAMPIRE_3",
    "ENCHANTMENT_MANA_VAMPIRE_4",
    "ENCHANTMENT_MANA_VAMPIRE_5",
    "ESSENCE_CRIMSON",
    "ENCHANTMENT_ULTIMATE_INFERNO_1",
    "ENCHANTMENT_ULTIMATE_FATAL_TEMPO_1",
    "ENCHANTMENT_TABASCO_2",
    "CHILI_PEPPER",
    "ENCHANTED_MYCELIUM",
    "ENCHANTED_RED_SAND",
]

function getKeyPrice(tier) {
    let avgMaterialPrice = 0;
    if (settings.keySetting == 0) {
        // buy offer
        avgMaterialPrice = (bazaarItems.products["ENCHANTED_RED_SAND"].quick_status.sellPrice + bazaarItems.products["ENCHANTED_MYCELIUM"].quick_status.sellPrice)/2;
    }
    else {
        // insta buy
        avgMaterialPrice = (bazaarItems.products["ENCHANTED_RED_SAND"].quick_status.buyPrice + bazaarItems.products["ENCHANTED_MYCELIUM"].quick_status.buyPrice)/2;
    }
    
    switch(tier) {
        case 1:
            return 200000 + avgMaterialPrice * 2;
        case 2:
            return 400000 + avgMaterialPrice * 6;
        case 3:
            return 750000 + avgMaterialPrice * 20;
        case 4:
            return 1500000 + avgMaterialPrice * 60;
        case 5:
            return 3000000 + avgMaterialPrice * 120;
    }
}

function getBazaarPrice(itemId) {
    if (bazaarItems == undefined) return 0;
    if (bazaarItems.success == false) return 0;
    let product = bazaarItems.products[itemId];
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

class ItemString {
    constructor(string, price) {
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
    let essenceValue = 0;
    if (container.getName() == "Paid Chest") {
        let tier = 5;
        let keyPrice = getKeyPrice(tier);
        totalValue -= keyPrice;
        tempString = `&r&c-${formatPrice(keyPrice)} &r&eTier ${tier} Key&r\n`;
        chestItems.push(new ItemString(tempString, -keyPrice));
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

        if (itemId == "ESSENCE_CRIMSON" && container.getName() == "Paid Chest") {
            let essencensMultiplicator = 1;
            if (settings.kuudraPetPerk == true) {
                settings.kuudraPetLevel = settings.kuudraPetLevel.replace(/\D/g, '');
                let kuudraPetLevel = parseInt(settings.kuudraPetLevel);
                if (kuudraPetLevel > 100) {
                    kuudraPetLevel = 100;
                }
                else if (kuudraPetLevel < 0) {
                    kuudraPetLevel = 0;
                }
                settings.kuudraPetLevel = kuudraPetLevel;

                if (settings.kuudraPet == 0 || settings.kuudraPet == 1) {
                    essencensMultiplicator += kuudraPetLevel * 0.2;
                }
                else if (settings.kuudraPet == 2 || settings.kuudraPet == 3) {
                    essencensMultiplicator += kuudraPetLevel * 0.15;
                }
                else if (settings.kuudraPet == 4) {
                    essencensMultiplicator += kuudraPetLevel * 0.1;
                }
            }
            
            essenceValue = Math.floor(item.getNBT().toObject().tag.display.Name * essencensMultiplicator) * getBazaarPrice(itemId);
            totalValue += essenceValue;
            tempString = `&r&6${formatPrice(essenceValue)} &r&eCrimson Essence&r\n`;
            chestItems.push(new ItemString(tempString, essenceValue));
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
                tempString = `&r&6${formatPrice(highestPrice)} &r&e${chestItem.name}&r `
                if (settings.lineSetting == 0) {
                    tempString += `\n`
                }
                tempString += `&r&b(${chestItem.att1Name}/${chestItem.att2Name} - &r&6${formatPrice(chestItem.att1Value)}/${formatPrice(chestItem.att2Value)}&b)\n`;
                chestItems.push(new ItemString(tempString, highestPrice));
            }
        }
        else if (bazaarIds.includes(itemId)) {
            // bazaar item
            let price = getBazaarPrice(itemId);
            if (price == 0) return;
            totalValue += price;
            let displayName = toTitleCase(itemId.replaceAll("_", " ").replace("ULTIMATE", "").replace("ENCHANTMENT", ""));
            tempString = `&r&6${formatPrice(price)} &r&e${displayName}&r\n`;
            chestItems.push(new ItemString(tempString, price));
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
    let counter = 1;
    chestItems.forEach((item) => {
        if (counter <= settings.maxDisplayedItems) {
            overlayString += item.string;
        }
        counter++;
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
    else if (price >= 1000000000) {
        return (price / 1000000000).toFixed(2) + "B";
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