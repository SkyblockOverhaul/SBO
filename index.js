/// <reference types="../CTAutocomplete" />
import Settings from "./settings";
import "./features/general/pickuplog";
import "./features/diana/DianaMobDetect";
// import "./features/Diana/DianaWaypoints";
import "./features/guis/BobberCounter";
import "./features/general/PartyCommands";
import "./features/general/messageHider";
import "./features/general/Waypoints";
import "./features/diana/DianaBurrowDetect";
import "./features/slayer/BlazeSlayer";
import "./features/general/CopyMessage";

// in sbo addons packen
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import "./features/dungeon/recognizeRareRoom";
import "./features/general/alphaCheck";

import { request } from "../requestV2";
import { toTitleCase } from "./utils/functions";
import { Overlay } from "./utils/overlay";
import { attributeShorts } from "./utils/constants";


register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

register("chat", (message, event) => {
    message = message.removeFormatting();
    if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
        cancel(event);
    }
    if (message.includes("Refelctor")) {
        Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
    }
    if (message.includes("Blue Goblin Egg")) {
        Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
    }
}).setCriteria("&r&aYou received ${message}");

register("chat", (player, message, event) =>{
    // cancel original message
    // send new guildbot message
    if (!player.includes(" ")) {
        cancel(event);
        player = player.removeFormatting();
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
    }
    else if (player.includes("replying to")) {
        cancel(event);
        let split = player.split(" ");
        let player1 = split[0];
        let player2 = split[3];
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
        // print("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
    }
}).setCriteria("&r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}:${message}").setContains()
// geht
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rSuccesfully invited kenchika to the party!&r
// &r&2Guild > &b[MVP&2+&b] MasterNR &3[320]&f: &rnice&r
// testen
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rWiggleSnakey replying to dtAxl: WWDYM&r 

// register("command", () => {
//     // Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, "&r&b[MVP&f+&b] RolexDE", 0, 90, 20);
//     Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
//     ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &5Minos Relic!");
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.0);
//     }, 0);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.2);
//     }, 50);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.4);
//     }, 100);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.6);
//     }, 150);
// }).setName("sboinq");

let lastUpdate = 0;
let updateing = false;
let kuudraItems = {};
register("step", () => {
    // update every 5 minutes
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

let overlayStringExample = `&r&6Terror Chestplate
&r&7Blazing Resistance &r&7(150.00K coins)
&r&7Breeze &r&7(2.49M coins)
&r&6Crimson Chestplate
&r&7Life Regeneration &r&7(1.90M coins)
&r&7Magic Find &r&7(4.00M coins)`;

let overlay = new Overlay("bobberCounter", ["all", "misc"], [10, 10, 1], "sbomoveItemOverlay", overlayStringExample, "bobberCounter");
overlay.setRenderGuiBool(false);

register("guiClosed", () => {
    overlay.setRenderGuiBool(false);
});

let shorts = {
    blazing_resistance: "BR",
    breeze: "BR",
    life_regeneration: "LR",
    magic_find: "MF",
    vitality: "VI"
}

register("guiOpened", () => {
    setTimeout(() => {
        const container = Player.getContainer();
        if (container == null) return;
        if (container.getName() == "container") return;
        ChatLib.chat("&r&6[SBO] &r&6&lGUI OPENED! " + container.getName());
        const items = container.getItems();
        if (items.length == 0) return;
        let overlayString = "";
        let highestPrice = 0;
        let tempString = "";
        items.forEach((item, index) => {
            if (item == null) return;
            if (container.getName() != "container") {
                if (index >= items.length - 36) return;
            }
            attributeDict = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("attributes").toObject();
            if (attributeDict == null) return;
            let first = true;
            tempString = "";
            highestPrice = 0;
            for (let name in attributeDict) {
                let lvl = attributeDict[name];
                
                if (name == "mending") {
                    name = "vitality";
                }

                if (first) {
                    first = false;
                    itemId = item.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id");
                    if (!allowedItemIds.includes(itemId)) return;
                    let displayName = toTitleCase(itemId.replace("_", " "));
                    tempString += `&r&6${displayName}\n(`; 
                }

                let price = getPrice(itemId, name, lvl);
                if (container.getName() != "container") {
                    // console.log("attribute: " + name + " " + lvl + " price: " + price);
                    if (price >= highestPrice) {
                        highestPrice = price;
                    }
                    
                    tempString += `&r&7${attributeShorts[name]} &r&7${formatPrice(price)} `;
                    
                }
                else {
                    // // edit item lore with price behind the attribute
                    // let lore = item.getLore();
                    // // only edit the attribute lore line 
                    // let newLore = [];
                    // for (let i = 0; i < lore.length - 4; i++) {
                    //     newLore.push(lore[i]);
                    //     if (lore[i].includes(toTitleCase(name.replace("_", " ")))) {
                    //         newLore.push(lore[i] + " &r&7(" + price + " coins)");
                    //         print(lore[i] + " &r&7(" + price + " coins)");
                    //     }

                    // item.setLore(newLore);
                    // }
                }
            }
            if (highestPrice != 0) {
                overlayString += `&e${formatPrice(highestPrice)}&r ${tempString})\n`;
            }
        });
        overlay.message = overlayString;
        overlay.setRenderGuiBool(true);
    }, 100);
});

function formatPrice(price) {
    if (price >= 1000000) {
        return (price / 1000000).toFixed(2) + "M";
    }
    else if (price >= 1000) {
        return (price / 1000).toFixed(2) + "K";
    }
    return price;
}

function getPrice(itemId, attribute, lvl) {
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
    }
}

// inv == 45 slots
// if "attributes" in item_data["ExtraAttributes"]:
//                     attributes = []
//                     for attribute in item_data["ExtraAttributes"]["attributes"]:
//                         attributeLvl= int(str(item_data["ExtraAttributes"]["attributes"].get(attribute, 0)))
//                         attributes.append(Attribute(attribute, attributeLvl, starting_bid))
//                         if attribute not in allattributes:
//                             if attribute == "mending":
//                                 attribute = "vitality"
//                             allattributes.append(attribute)

//                     if "magic_find" in item_data["ExtraAttributes"]["attributes"]:
//                         attributeLvlMf = int(str(item_data["ExtraAttributes"]["attributes"].get("magic_find", 0)))
//                     else:
//                         attributeLvlMf = 0
//                 else:
//                     attributeLvlMf = 0
//                     attributes = []

// const primary = getItemValue(container.getStackInSlot(11));
// let secondary = container.getStackInSlot(12);
// secondary = bazaar[secondary.getNBT().getCompoundTag("tag").getCompoundTag("ExtraAttributes").getString("id")]?.[0] || getItemValue(secondary);
// let essence = container.getStackInSlot(14).getNBT().toObject().tag.display.Name;
// essence = parseInt(essence.slice(essence.indexOf('x') + 1)) * bazaar.ESSENCE_CRIMSON[0];
// const teeth = settings.maxChili ? (bazaar.ENCHANTMENT_TABASCO_3[0] - 64*bazaar.CHILI_PEPPER[1])/6 * Math.ceil(tier / 2) : 0;
// const cost = KEY_COST[tier - 1][0] + KEY_COST[tier - 1][1] * Math.min(bazaar.ENCHANTED_RED_SAND[1], bazaar.ENCHANTED_MYCELIUM[1]);
// const value = primary + secondary + essence + teeth;
// chestProfit = value - cost;