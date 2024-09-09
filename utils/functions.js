import { request } from "../../requestV2";
import settings, { getcustomSounds } from "../settings";
import { HypixelModAPI } from "./../../HypixelModAPI";
import { registerWhen, dianaTrackerMayor as trackerMayor, dianaTrackerSession as trackerSession, dianaTrackerTotal as trackerTotal } from "./variables";
import { getWorld } from "./world";

// geklaut von coleweight for drawline
if(!GlStateManager) {
    let GL11=Java.type("org.lwjgl.opengl.GL11")
    let GlStateManager=Java.type("net.minecraft.client.renderer.GlStateManager")
}
export function trace (x, y, z, red, green, blue, alpha, type, lineWidth) {
    if (type === "calc")
    {
        if (x >= 0) {
            x = parseFloat(x) + 0.5;
        } else {
            x = parseFloat(x) - 0.5;
        }
        if (z >= 0)
        {
            z = parseFloat(z) + 0.5;
        } else {
            z = parseFloat(z) - 0.5;
        }
    }
    if(Player.isSneaking())
        drawLine(Player.getRenderX(), Player.getRenderY() + 1.54, Player.getRenderZ(), x, y, z, red, green, blue, alpha, lineWidth)
    else
        drawLine(Player.getRenderX(), Player.getRenderY() + 1.62, Player.getRenderZ(), x, y, z, red, green, blue, alpha, lineWidth)
}

function drawLine (x1, y1, z1, x2, y2, z2, red, green, blue, alpha, lineWidth)
{
    GL11.glBlendFunc(770,771)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glLineWidth(lineWidth)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glDepthMask(false)
    GlStateManager.func_179094_E()

    Tessellator.begin(GL11.GL_LINE_STRIP).colorize(red, green, blue, alpha)
    Tessellator.pos(x1, y1, z1).tex(0, 0)
    Tessellator.pos(x2, y2, z2).tex(0, 0)
    Tessellator.draw()

    GlStateManager.func_179121_F()
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glDepthMask(true)
    GL11.glDisable(GL11.GL_BLEND)
}

export function getClosest(origin, positions) {
    let closestPosition = positions.length > 0 ? positions[0] : [0, 0, 0];
    let closestDistance = 999;
    let distance = 999;

    positions.forEach(position => {
        distance = Math.hypot(origin[1] - position[1], origin[2] - position[2], origin[3] - position[3]);
        if (distance < closestDistance) {
            closestDistance = distance;
            closestPosition = position;
        }
    });

    return [closestPosition, closestDistance];
};
export function convertToPascalCase(input) {
    if (!input) return; 

    return input
        .split(" ")
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join("");
}
/** 
 * @param {string} chat
 * @param {string} mob
 * @param {number} x
 * @param {number} y
 * @param {number} z
 */
export function mobAnnouncement(chat,mob,x,y,z) {
    x = Math.round(x);
    y = Math.round(y);
    z = Math.round(z);

    let zone = Scoreboard.getLines().find(line => line.getName().includes("⏣"));
    if(zone === undefined) zone = Scoreboard.getLines().find(line => line.getName().includes("ф"));
    const area = zone === undefined ? "None" : zone.getName().removeFormatting();

    ChatLib.command(`pc x: ${x} y: ${y} z: ${z} | ${mob} found at ${area}!`);
}

let partyMembersUuids = [];
export function getPartyMembersUuids() {
    return partyMembersUuids;
}

export function getSBID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("id") || null;
}
export function getSBUUID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("uuid") || null;
}

export function checkIfInSkyblock() {
    inSkyblockBool = (settings.alwaysInSkyblock || Scoreboard.getTitle()?.removeFormatting().includes("SKYBLOCK"));
    return inSkyblockBool;
}

// returns if in skyblock //
let inSkyblock = false;
export function isInSkyblock() {
    return inSkyblock;
}

let state = {
    entityDeathOccurred: false
}

let state2 = {
    entityDeathOccurred: false
}

// return 2sec long true if entity death occurred //
export function mobDeath2SecsTrue() {
    return state.entityDeathOccurred;
}

// return 4sec long true if entity death occurred //
export function mobDeath4SecsTrue() {
    return state2.entityDeathOccurred;
}   

let allowedToTrackSacks = false;
export function getAllowedToTrackSacks() {
    return allowedToTrackSacks;
}

registerWhen(register("guiOpened", () => {
    setTimeout(() => {
        if (Player.getContainer() != undefined) {
            if (Player.getContainer().getName() == "Sack of Sacks") {
                allowedToTrackSacks = false;
            }
        }
    }, 300);
}), () => settings.dianaTracker);

let dianaMobNames = ["Minos Inquisitor", "Minotaur", "Iron Golem", "Ocelot", "Minos Champion", "Zombie"];

function trackLsInq(tracker, amount) {
    if (tracker["mobs"]["Minos Inquisitor Ls"] != null) {
        tracker["mobs"]["Minos Inquisitor Ls"] += amount;
    }
    else {
        tracker["mobs"]["Minos Inquisitor Ls"] = amount;
    }
    tracker.save();
}
let hasTrackedInq = false;
registerWhen(register("entityDeath", (entity) => { // geht noch nicht weil er real enitiy names mint wie ZOMBIE, Iron Golem etc
    let dist = entity.distanceTo(Player.getPlayer());
    entityName = entity.getName().toString();
    if (gotLootShare() && entityName == "Minos Inquisitor" && !hasTrackedInq) {
        trackLsInq(trackerMayor, 1);
        trackLsInq(trackerSession, 1);
        trackLsInq(trackerTotal, 1);
        hasTrackedInq = true;
        setTimeout(() => {
            hasTrackedInq = false;
        }, 4000);
    }
    if (dianaMobNames.includes(entityName.trim())) {
        if (dist < 30 ) {
            allowedToTrackSacks = true;
            state.entityDeathOccurred = true;
            state2.entityDeathOccurred = true;
            setTimeout(() => {
                state.entityDeathOccurred = false;
            }, 2000);
            setTimeout(() => {
                state2.entityDeathOccurred = false;
            }, 4000);
        }
    }
}), () => getWorld() === "Hub" && settings.dianaTracker);

export function getDateString(date) {
    return `${date.getDate()}.${date.getMonth() + 1}.${date.getFullYear()}`;
}

// string to title case //
export function toTitleCase(str) {
    return str.replace(
      /\w\S*/g,
      function(txt) {
        return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
      }
    );
}

// read player inventory //
export function readPlayerInventory(type="") {
    let slots = 0;
    if (!worldLoaded) return {};
    if (type === "hotbar") {
        slots = 8;
    }
    else {
        slots = 39;
    }
    let playerItems = {}
    let playerInv = Player.getInventory();
    if (playerInv == null) return playerItems;
    let playerInvItems = playerInv.getItems();
    // const inventory = Player.getContainer();
    for (let i in playerInv.getItems()) {
        if (i <= slots) {
            if (playerInvItems[i] !== null && ![8, 36, 37, 38, 39].includes(parseInt(i))) { 
                if (playerItems[getSBID(playerInvItems[i])]) {
                    playerItems[getSBID(playerInvItems[i])][0] += playerInvItems[i].getStackSize();
                }
                else {
                    playerItems[getSBID(playerInvItems[i])] = [playerInvItems[i].getStackSize(), playerInvItems[i].getName()];
                }
            }
        }
    }
    return playerItems;
}

// check if item is in hotbar //
export function checkItemInHotbar(item) {
    if (!worldLoaded) return false;
    let hotbarItems = readPlayerInventory("hotbar");
    for (let i in hotbarItems) {
        if (item == i) {
            return true;
        }
    }
    return false;
}

export function checkHotbarItems() {
    let hotbarItems = []
    if (!worldLoaded) return hotbarItems;
    let playerInv = Player.getInventory();
    if (playerInv == null) return hotbarItems;
    let playerInvItems = playerInv.getItems();
    playerInvItems.forEach((item, i) => {
        if (i <= 8) {
            if (playerInvItems[i] !== null) {
                hotbarItems.push(item);
            }
        }
    });
    return hotbarItems;
}


export function checkDaxeEnchants() {
    let chimVbool, lootingVbool, divineGift3bool = false;
    if (!worldLoaded) return [false, false, false];
    let hotbarItems = checkHotbarItems();
    for (let item of hotbarItems) {
        let nbtData = item.getNBT();
        if (!nbtData) return [false, false, false];
        let itemName = nbtData.getCompoundTag("tag").getCompoundTag("display").getString("Name");
        if (!itemName) itemName = "";
        itemName = itemName.removeFormatting().trim();
        enchantments = nbtData.getCompoundTag("tag").getCompoundTag("ExtraAttributes").getCompoundTag("enchantments");
        if (!enchantments) enchantments = {};
        if (itemName.includes("Daedalus Axe")) {
            if (enchantments.getInteger("ultimate_chimera") == 5) {
                chimVbool = true;
            }
            if (enchantments.getInteger("looting") == 5) {
                lootingVbool = true;
            }
            if (enchantments.getInteger("divine_gift") == 3) {
                divineGift3bool = true;
            }
        }
        continue;
    }
    return [chimVbool, lootingVbool, divineGift3bool];
}

export function playerHasSpade() {
    return spadeBool;
}

let spadeBool = false;
register("step", () => {
    spadeBool = checkItemInHotbar("ANCESTRAL_SPADE");
}).setFps(1)

// return 1sec long true if player got loot share //
export function gotLootShare() {
    return lootShareBool;
}

// check if player got loot share //
let lootShareBool = false;
register("chat" , (player) => {
    lootShareBool = true;
    setTimeout(() => {
        lootShareBool = false;
    }, 2000);
}).setCriteria("&r&e&lLOOT SHARE &r&r&r&fYou received loot for assisting &r${player}&r&f!&r");
// &r&e&lLOOT SHARE &r&r&r&fYou received loot for assisting &r&6D4rkSwift&r&f!&r

// check if in skyblock //
register("step", () => {
    inSkyblock = checkIfInSkyblock();
}).setFps(1);


export function initializeGuiSettings() {
    return {
        MobLoc: { "x": 15, "y": 22, "s": 1 },
        LootLoc: { "x": 15, "y": 112, "s": 1 },
        BobberLoc: { "x": 10, "y": 295, "s": 1 },
        MythosHpLoc: { "x": 400, "y": 50, "s": 1 },
        EffectsLoc: { "x": 10, "y": 200, "s": 1 },
        BlazeLoc: { "x": 10, "y": 10, "s": 1 },
        KuudraValueLoc: { "x": 10, "y": 10, "s": 1 },
        fossilLoc: { "x": 275, "y": 185, "s": 1 },
        LegionLoc: { "x": 10, "y": 310, "s": 1 },
        StatsLoc: { "x": 15, "y": 290, "s": 1 },
        AvgMagicFindLoc: { "x": 15, "y": 350, "s": 1 },
        PickupLogLoc: { "x": 2, "y": 2, "s": 1 },
        CrownLoc: { "x": 15, "y": 435, "s": 1 },
        GoldenFishLoc: { "x": 15, "y": 50, "s": 1 },   
    };
}

export function loadGuiSettings() {
    let loadedSettings = {};
    try {
        loadedSettings = JSON.parse(FileLib.read("SBO", "guiSettings.json")) || initializeGuiSettings();
        loadedSettings = checkSettings(loadedSettings);
    } 
    catch (e) {
        loadedSettings = initializeGuiSettings();
        saveGuiSettings(loadedSettings);
    }
    return loadedSettings;
}

function checkSettings(loadedSettings) {
    // Get the default settings
    const defaultSettings = initializeGuiSettings();

    // Loop through default settings and ensure loaded settings have all properties
    for (let key in defaultSettings) {
        if (!loadedSettings.hasOwnProperty(key)) {
            loadedSettings[key] = defaultSettings[key];
        }
    }
    
    return loadedSettings;
}

export function saveGuiSettings(guiSettings) {
    FileLib.write("SBO", "guiSettings.json", JSON.stringify(guiSettings, null, 4));
}

export function drawRect(x1,y1,scale,z) {
    let x = x1/scale
    let y = y1/scale
    // settings.slotColor: java.awt.Color[r=19,g=145,b=224]
    let color = Renderer.color(settings.slotColor.getRed(), settings.slotColor.getGreen(), settings.slotColor.getBlue(), 200)
    Renderer.translate(0, 0, z)
    Renderer.scale(scale,scale)
    Renderer.drawRect(color, x, y, 6.5, 6.5);
}

export function drawOutlinedString(text,x1,y1,scale,z) {
    let outlineString = "&0" + ChatLib.removeFormatting(text)
    let x = x1/scale
    let y = y1/scale

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x + 1, y)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x - 1, y)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x, y + 1)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(outlineString, x, y - 1)

    Renderer.translate(0,0,z)
    Renderer.scale(scale,scale)
    Renderer.drawString(text, x, y)
}

let printBool = false;
export function printDev(msg) {
    if(!printBool) return;
    return print("[DEV]: " + msg);
}

export function getplayername(player) {
    let num
    let name
    num = player.indexOf(']')
    if (num == -1) {                // This part is only  ***When I wrote this, god and I knew how it worked, now only he knows.***
        num = player.indexOf('&7')  // for nons because
        if (num == -1) {            // it doesnt work
            num = -2                // without that
        }                           // #BanNons
    }
    name = player.substring(num+2).removeFormatting()
    name = name.replaceAll(/[^a-zA-Z0-9_]/g, '').replaceAll(' ', '')
return name
}

export function isWorldLoaded() {
    return worldLoaded;
}

let worldLoaded = false;
register("worldUnload", () => {
    worldLoaded = false;
});

register("worldLoad", () => {
    setTimeout(() => {
        worldLoaded = true;
    }, 1000);
});

register("command", () => {
    printBool = !printBool;
    if(printBool) {
        ChatLib.chat("&6[SBO] &aDev Mode enabled");
    }
    else {
        ChatLib.chat("&6[SBO] &aDev Mode disabled");
    }
}).setName("sbodev")

export function playCustomSound(sound, volume) {
    if (sound != "" && sound != undefined && sound != "none") {
        if (sound.includes(".ogg")) sound = sound.replace(".ogg", "");
        if (FileLib.exists(Config.modulesFolder.replace("modules", "images") + `/${sound}.ogg`)) {
            new Sound({ source: new java.lang.String(sound + ".ogg") }).setVolume(volume/100).play()
        }
        else {
            ChatLib.chat(`&6[SBO] &cSound file not found! (if the filename is correct, make sure to reload ct by "/ct load")`);
        }
    }
}

let customSounds = undefined
register("command", () => {
    customSounds = getcustomSounds();
    playCustomSound(customSounds[settings.customSound], settings.customVolume);
}).setName("playsbotestsound");

// party detection
HypixelModAPI.on("partyInfo", (partyInfo) => {
    partyMembersUuids = [];
    Object.keys(partyInfo).forEach(key => {
        partyMembersUuids.push(key);
    })
})

export function sendPartyRequest() {
    HypixelModAPI.requestPartyInfo();
}

let lastUpdate = 0;
let updateing = false;
let kuudraItems = undefined;
let dianaItems = undefined;
let bazaarItems = undefined;

export function getKuudraItems() {
    return kuudraItems;
}

export function getDianaItems() {
    return dianaItems;
}

export function getBazaarItems() {
    return bazaarItems;
}

register("step", () => {
    // update every 5 minutes
    if (updateing) return;
    if (Date.now() - lastUpdate > 300000 || lastUpdate == 0) {
        // print("updating kuudra items with api");
        updateing = true;
        lastUpdate = Date.now();
        updateItemValues()
        setTimeout(() => {
            updateing = false;
        }, 300000);
    }
}).setFps(1);

function updateItemValues() {
    request({
        url: "https://api.skyblockoverhaul.com/ahItems",
        json: true
    }).then((response)=>{
        kuudraItems = response[0];
        dianaItems = response[1];
        if (kuudraItems == undefined) {
            print("kuudra items undefined");
            kuudraItems = {};
        }
        if (dianaItems == undefined) {
            print("diana items undefined");
            dianaItems = {};
        }
    }).catch((error)=>{
        console.error("An error occurred: " + error);
    });

    request({
        url: "https://api.hypixel.net/skyblock/bazaar?product",
        json: true
    }).then((response)=>{
        bazaarItems = response;
        if (bazaarItems == undefined) {
            print("bazaar items undefined");
            bazaarItems = {};
        }
    }).catch((error)=>{
        console.error("bazaar " + error);
    });
}

let activeUsers = undefined
export function getActiveUsers(useCallback = false, callback = null) {
    request({
        url: "https://api.skyblockoverhaul.com/activeUsers",
        json: true
    }).then((response) => {
        activeUsers = response.activeUsers;

        if (activeUsers === undefined) {
            print("active users undefined");
            activeUsers = 0;
        }

        if (useCallback && callback) {
            callback(activeUsers);
        } else {
            ChatLib.chat("&6[SBO] &aActive user: &e" + activeUsers);
        }
    }).catch((error) => {
        console.error("An error occurred: " + error);
    });
}

register("command", () => {
    getActiveUsers();
}).setName("sboactiveuser");

export function getBazaarPriceKuudra(itemId) {
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

export function getBazaarPriceDiana(itemId) {
    if (bazaarItems == undefined) return 0;
    if (bazaarItems.success == false) return 0;
    let product = bazaarItems.products[itemId];
    if (product == undefined) return 0;
    if (settings.bazaarSettingDiana == 0) {
        return product.quick_status.sellPrice;
    }
    else {
        return product.quick_status.buyPrice;
    }
}

export function getDianaAhPrice(itemId) {
    if (dianaItems == undefined) return 0;
    if (dianaItems[itemId] == undefined) return 0;
    if (itemId == "CROWN_OF_GREED") {
        if (dianaItems[itemId].price < 1000000) return 1000000;
        return dianaItems[itemId].price;
    }
    return dianaItems[itemId].price;
}

export function formatNumber(number) {
    if(number == undefined) return 0;
    if (number >= 1000000000) {
        return (number / 1000000000).toFixed(2) + "b";
    }
    else if (number >= 1000000) {
        return (number / 1000000).toFixed(1) + "m";  
    }
    else if (number >= 1000) {
        return (number / 1000).toFixed(1) + "k";
    }
    return parseFloat(number).toFixed(0);
}

export function formatNumberCommas(number) {
    // add commas to number 1000000 -> 1,000,000
    if(number == undefined) return 0;
    return number.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

export function getPurse() {
    let scoreboard = Scoreboard.getLines();
    if (scoreboard != undefined) {
        for (let line of scoreboard) {
            line = line.getName().removeFormatting();
            if (line.includes("Purse")) {
                let parts = line.split(": ");
                parts[1] = parts[1].split(" ")[0];
                parts[1] = parts[1].replace(/[^0-9,]/g, '').replaceAll(",", "");
                let purse = parseInt(parts[1]);
                return purse; 
            }
        }      
    }
    return -1;
}

// calc percent from tracker //
export function calcPercent(trackerToCalc, type) {
    if (trackerToCalc == undefined) return;
    percentDict = {};
    if(type == "mobs") {
        for (let mob in trackerToCalc["mobs"]) {
            percentDict[mob] = parseFloat((trackerToCalc["mobs"][mob] / trackerToCalc["mobs"]["TotalMobs"] * 100).toFixed(2));
        }
        return percentDict;
    }
    else {
        for (let obj in ["Minos Inquisitor", "Minos Champion", "Minotaur"].values()) {
            switch (obj) {
                case "Minos Inquisitor":
                    percentDict["Chimera"] = parseFloat((trackerToCalc["items"]["Chimera"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
                    percentDict["ChimeraLs"] = parseFloat((trackerToCalc["items"]["ChimeraLs"] / trackerToCalc["mobs"]["Minos Inquisitor Ls"] * 100).toFixed(2));
                    break;
                case "Minos Champion":
                    percentDict["MINOS_RELIC"] = parseFloat((trackerToCalc["items"]["MINOS_RELIC"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
                    break;
                case "Minotaur":
                    percentDict["Daedalus Stick"] = parseFloat((trackerToCalc["items"]["Daedalus Stick"] / trackerToCalc["mobs"][obj] * 100).toFixed(2));
                    break;
            }
        }
        return percentDict;
    }
}

export function calcPercentOne(tracker, item, mob = undefined) {
    if (tracker == undefined) return;
    if (mob) {
        return parseFloat((tracker["items"][item] / tracker["mobs"][mob] * 100).toFixed(2));
    } else {
        return parseFloat((tracker["mobs"][item] / tracker["mobs"]["TotalMobs"] * 100).toFixed(2));  
    }
}

export function getTracker(setting) {
    switch (setting) {
        case 1:
            return trackerTotal;
        case 2:
            return trackerMayor;
        case 3:
            return trackerSession;
    }
}

export function formatTime(milliseconds) {
    const totalSeconds = parseInt(milliseconds / 1000);
    const totalMinutes = parseInt(totalSeconds / 60);
    const totalHours = parseInt(totalMinutes / 60);
    const days = parseInt(totalHours / 24);
    const hours = totalHours % 24;
    const minutes = totalMinutes % 60;
    const seconds = totalSeconds % 60;

    let formattedTime = '';
    if (days > 0) {
        formattedTime += `${days}d `;
    }
    if (hours > 0 || days > 0) {
        formattedTime += `${hours}h `;
    }
    if (minutes > 0 || hours > 0 || days > 0) {
        formattedTime += `${minutes}m `;
    }
    if (minutes < 1 && hours == 0 && days == 0) {
        formattedTime += `${seconds}s `;
    }

    return formattedTime.trim();
}

export function formatTimeMinSec(milliseconds) {
    const totalSeconds = parseInt(milliseconds / 1000);
    const totalMinutes = parseInt(totalSeconds / 60);
    const minutes = totalMinutes % 60;
    const seconds = totalSeconds % 60;

    let formattedTime = `${minutes}m ${seconds}s`;

    return formattedTime;
}

let dianaMayorTotalProfit = 0;
let dianaMayorOfferType
let profitPerHour = 0;
let burrowsPerHour = 0;

export function getBurrowsPerHour() {
    return burrowsPerHour;
}

export function setBurrowsPerHour(burrows) {
    burrowsPerHour = burrows;
}

export function getDianaMayorTotalProfitAndOfferType() {
    return [dianaMayorTotalProfit, dianaMayorOfferType, profitPerHour];
}

export function setDianaMayorTotalProfit(profit, offerType, profitHour) {
    dianaMayorTotalProfit = profit;
    dianaMayorOfferType = offerType;
    profitPerHour = profitHour;
}

export function getRarity(item){
    switch (item.toLowerCase().trim()) {
        case "common":
            return "&f" + item;
        case "uncommon":
            return "&a" + item;
        case "rare":
            return "&9" + item;
        case "epic":
            return "&5" + item;
        case "legendary":
            return "&6" + item;
        case "mythic":
            return "&d" + item;
        default:
            return "&7";
    }
}

export function getNumberColor(number, range) {
    if (number === range) {
        return "&c" + number;
    }
    else if (number === range - 1) {
        return "&6" + number;
    }
    else {
        return "&9" + number;
    }
}

export function getGriffinItemColor(item) {
    if (item != 0) {
        if (!item) return "";
        let name = item.replace("PET_ITEM_", "");
        name = toTitleCase(name.replaceAll("_", " "));
        switch (name) {
            case "Four Eyed Fish":
                return "&5" + name;
            case "Dwarf Turtle Shelmet":
                return "&a" + name;
            case "Crochet Tiger Plushie":
                return "&5" + name;
            case "Antique Remedies":
                return "&5" + name;
            case "Lucky Clover":
                return "&a" + name;
            case "Minos Relic":
                return "&5" + name;
            default:
                return "&7" + name;
        }
    }
    return "";
}

export function getMagicFind(mf) {
    let magicFindMatch = mf.match(/\+(&r&b)?(\d+)%/);
    let magicFind = parseInt((magicFindMatch ? magicFindMatch[2] : 0));
    return magicFind;
}

export function matchLvlToColor(lvl) {
    if (lvl >= 480) {
        return "&4" + lvl;
    } else if (lvl >= 440) {
        return "&c" + lvl;
    } else if (lvl >= 400) {
        return "&6" + lvl;
    } else if (lvl >= 360) {
        return "&5" + lvl;
    } else if (lvl >= 320) {
        return "&d" + lvl;
    } else if (lvl >= 280) {
        return "&9" + lvl;
    } else if (lvl >= 240) {
        return "&3" + lvl;
    } else if (lvl >= 200) {
        return "&b" + lvl;
    } else {
        return "&7" + lvl;
    }
}

// gui functions/classes
export function drawRectangleOutline(color, x, y, width, height, thickness) {
    Renderer.drawLine(color, x , y, x + width, y, thickness); // Top Line
    Renderer.drawLine(color, x, y, x, y + height, thickness); // Left Line
    Renderer.drawLine(color, x , y + height, x + width, y + height, thickness); // Bottom Line
    Renderer.drawLine(color, x + width, y, x + width, y + height, thickness); // Right Line
}

export function roundRect(color, x, y, width, height, radius) {
    radius = Math.min(radius, width / 2, height / 2);

    rect(color, x + radius, y + radius, width - 2 * radius, height - 2 * radius);

    rect(color, x + radius, y, width - 2 * radius, radius); // Top rectangle
    rect(color, x + radius, y + height - radius, width - 2 * radius, radius); // Bottom rectangle
    rect(color, x, y + radius, radius, height - 2 * radius); // Left rectangle
    rect(color, x + width - radius, y + radius, radius, height - 2 * radius); // Right rectangle

    Renderer.drawCircle(color, x + radius, y + radius, radius, 100, 5); // Top-left corner
    Renderer.drawCircle(color, x + width - radius, y + radius, radius, 100, 5); // Top-right corner
    Renderer.drawCircle(color, x + width - radius, y + height - radius, radius, 100, 5); // Bottom-right corner
    Renderer.drawCircle(color, x + radius, y + height - radius, radius, 100, 5); // Bottom-left corner
}

export function rect(color, x, y, width, height) {
    Renderer.drawRect(color, x, y, width, height);
}

export function color(r, g, b, a) {
    return Renderer.color(r, g, b, a)
}

export function line(color, x1, y1, x2, y2, thickness) {
    Renderer.drawLine(color, x1, y1, x2, y2, thickness)
}

export class TextClass {
    constructor(color, x, y, string, scale, shadow) {
        this.object = new Text(string);
        this.color = color;
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.shadow = shadow;
        this.checked = false;
        this.width = undefined;
        this.height = undefined;
    }

    draw() {
        let guiScale = Client.settings.video.getGuiScale();
        let compensation;
        if (guiScale === 1) {
            compensation = 1.9;
        } else if (guiScale === 3) {
            compensation = 0.66;
        } else {
            const targetScale = 2.0;
            compensation = guiScale / targetScale;
        }
        let scale = this.scale * compensation;
        this.object.setColor(this.color);
        this.object.setScale(scale);
        this.object.setShadow(this.shadow);
        this.object.setX(this.x).setY(this.y);
        this.object.draw();
        this.width = this.object.getWidth();
        this.height = this.object.getHeight();
        return this;
    }

    setX(x) {
        this.x = x;
        return this;
    }

    setY(y) {
        this.y = y;
        return this;
    }

    setText(text) {
        this.object.setString(text);
        return this;
    }

    setColor(color) {
        this.color = color;
        return this;
    }

    setScale(scale) {
        this.scale = scale;
        return this;
    }

    setShadow(shadow) {
        this.shadow = shadow;
        return this;
    }
}

export class Button {
    constructor(x, y, width, height, text, rightClick, outlined, background, hoverPriority = "", updateScaling = true) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.rightClick = rightClick;
        this.outlined = outlined;
        this.background = background;
        this.hoverPriority = hoverPriority;
        this.updateScaling = updateScaling
        this.isHovering = false;
        this.lastScale = undefined;
        this.originalWidth = width;
        this.originalHeight = height;
        this.lastScreenSize = undefined;
        this.action = undefined;

        this.textWidth = undefined;
        this.textColor = color(255, 255, 255, 255);
        this.textScale = 1;
        this.textX = this.x + (this.width - this.textWidth) / 2;
        this.textY = (this.y + this.height / 2) - 4;
        this.bgColor = color(0, 0, 0, 150);
        this.outlineColor = color(255, 255, 255, 255);
        this.textObject = new TextClass(this.textColor, this.textX, this.textY, this.text, this.textScale, false);
    }

    onClick(action) {
        this.action = action;
        return this;
    }

    isClicked(mouseX, mouseY, button) {
        if (mouseX >= this.x && mouseX <= this.x + this.width && 
            mouseY >= this.y && mouseY <= this.y + this.height) {
            if (button == 0 || (button == 1 && this.rightClick)) {
                this.action();
                return true;
            }
        }
        return false;
    }

    isHovered(mouseX, mouseY) {
        return (
            mouseX >= this.x && 
            mouseX <= this.x + this.width && 
            mouseY >= this.y && 
            mouseY <= this.y + this.height
        );
    }

    updateDimensions() {
        let guiScale = Client.settings.video.getGuiScale();
        let displayX = Renderer.screen.getWidth();
        let displayY = Renderer.screen.getHeight();
        if (this.lastScreenSize !== displayX + displayY) {
            this.lastScale = undefined;
            this.lastScreenSize = displayX + displayY;
        }
        if (this.lastScale !== guiScale) {
            if (guiScale == 1) {
                this.width = this.originalWidth * 2;
                this.height = this.originalHeight * 2;
                this.textX = this.x + (this.width / 2) * 0.05;
                this.textY = this.y + (this.height / 2) * 0.35;
            }
            else if (guiScale == 3) {
                this.width = this.originalWidth * 0.66;
                this.height = this.originalHeight * 0.66;
                this.textX = this.x + (this.width - this.textWidth) / 2;
                this.textY = this.y + (this.height / 2);
            }
            else {
                this.width = this.originalWidth;
                this.height = this.originalHeight;
            }
            this.lastScale = guiScale;
        }
    }

    updateTextDimensions(width, height) {
        this.textX = this.x + (this.width - width) / 2
        this.textY = this.y + (this.height - height) / 2;
    }
    // text erst nach dem erstellen skalieren was x/y angeht wichtig!!!

    draw(mouseX, mouseY, buttons = []) {
        if (this.updateScaling)
            this.updateDimensions();
    
        let isAnyJoinHovered = buttons.some(button => button.isHovered(mouseX, mouseY) && button.hoverPriority === "join");
        if (this.hoverPriority === "join") {
            this.isHovering = this.isHovered(mouseX, mouseY);
        } else if (this.hoverPriority === "partyInfo" && !isAnyJoinHovered) {
            this.isHovering = this.isHovered(mouseX, mouseY);
        } else if (this.hoverPriority === "") {
            this.isHovering = this.isHovered(mouseX, mouseY);
        }
        else {
            this.isHovering = false;
        }
        let bgColor = this.isHovering ? color(255, 255, 255, 150) : this.bgColor;
        if (!this.background) bgColor = this.isHovering ? color(255, 255, 255, 150) : color(0, 0, 0, 0);
        rect(bgColor, this.x, this.y, this.width, this.height);
        if (this.outlined)     
            drawRectangleOutline(this.outlineColor, this.x, this.y, this.width, this.height, 1);
        this.textObject.draw();
        this.updateTextDimensions(this.textObject.width, this.textObject.height);
        this.textObject.setX(this.textX).setY(this.textY)
        return this;
    }

    customize(options) {
        if (options.textColor) this.textColor = options.textColor;
        if (options.textScale) this.textScale = options.textScale;
        if (options.bgColor) this.bgColor = options.bgColor;
        if (options.outlineColor) this.outlineColor = options.outlineColor;
        if (options.text) this.text = options.text;
        if (options.width) {
            this.width = options.width;
            this.originalWidth = options.width;
        }
        if (options.height) {
            this.height = options.height;
            this.originalHeight = options.height;
        }
        if (options.x) this.x = options.x;
        if (options.y) this.y = options.y;
        if (this.hoverPriority !== "partyInfo")
            this.updateDimensions();
        if (options.textX) this.textX = this.textX * options.textX;
        if (options.textY) this.textY = this.textY * options.textY;
        return this;
    }
}

let dianaStats = undefined;
export function getDianaStats() {
    if (dianaStats == undefined) {
        loadDianaStats(true, (stats) => {
            return stats;
        });
    }
    else {
        return dianaStats;
    }
}

function loadDianaStats(useCallback = false, callback = null) {
    request({
        url: "https://api.skyblockoverhaul.com/partyInfoByUuids?uuids=" + Player.getUUID().replaceAll("-", ""),
        json: true
    }).then((response) => {
        dianaStats = response[0];
        if (useCallback && callback) {
            callback(dianaStats);
        } 
    }).catch((error) => {
        console.error("An error occurred: " + error);
    });
}