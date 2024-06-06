import settings from "../settings";
import { registerWhen } from "./variables";
import { HypixelModAPI } from "./../../HypixelModAPI";
import { getWorld } from "./world";

// geklaut von coleweight for drawline
if(!GlStateManager) {
    let GL11=Java.type("org.lwjgl.opengl.GL11")
    let GlStateManager=Java.type("net.minecraft.client.renderer.GlStateManager")
}
export function trace (x, y, z, red, green, blue, alpha, type, lineWidth){
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
export function mobAnnouncement(chat,mob,x,y,z){
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

let partyBool = false;
export function getPartyBool() {
    return partyBool;
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

// return 1sec long true if entity death occurred //
export function isActiveForOneSecond() {
    return state.entityDeathOccurred;
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
}), () => settings.dianaLootTracker);

registerWhen(register("entityDeath", (entity) => {
    let dist = entity.distanceTo(Player.getPlayer());
    if (dist < 30 ) {
        allowedToTrackSacks = true;
        state.entityDeathOccurred = true;
        setTimeout(() => {
            state.entityDeathOccurred = false;
        }, 2000);
    }
}), () => getWorld() === "Hub" && settings.dianaLootTracker);

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
    playerItems = {}
    let playerInv = Player.getInventory();
    let playerInvItems = playerInv.getItems();
    for (let i in playerInv.getItems()) {
        if (i <= slots) {
            if (playerInvItems[i] !== null) {
                if (playerItems[getSBID(playerInvItems[i])]) {
                    playerItems[getSBID(playerInvItems[i])] += playerInvItems[i].getStackSize();
                }
                else {
                    playerItems[getSBID(playerInvItems[i])] = playerInvItems[i].getStackSize();
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
    let tempDict = {
        MobLoc: {
            "x": 15,
            "y": 22,
            "s": 1
        },
        LootLoc: {
            "x": 15,
            "y": 112,
            "s": 1
        },
        BobberLoc: {
            "x": 10,
            "y": 295,
            "s": 1
        },
        MythosHpLoc: {
            "x": 400,
            "y": 50,
            "s": 1
        },
        EffectsLoc: {
            "x": 10,
            "y": 200,
            "s": 1
        },
        BlazeLoc: {
            "x": 10,
            "y": 10,
            "s": 1
        },
        KuudraValueLoc: {
            "x": 10,
            "y": 10,
            "s": 1
        },
        fossilLoc: {
            "x": 275,
            "y": 185,
            "s": 1
        },
        LegionLoc: {
            "x": 10,
            "y": 310,
            "s": 1
        },
        StatsLoc: {
            "x": 15,
            "y": 290,
            "s": 1
        }
    };
    return tempDict;
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
    // check if all Properties are present if not set this property to default
    let defaultSettings = initializeGuiSettings();
    if (!loadedSettings.hasOwnProperty("MobLoc")) {
        loadedSettings["MobLoc"] = defaultSettings["MobLoc"];
    }
    if (!loadedSettings.hasOwnProperty("LootLoc")) {
        loadedSettings["LootLoc"] = defaultSettings["LootLoc"];
    }
    if (!loadedSettings.hasOwnProperty("BobberLoc")) {
        loadedSettings["BobberLoc"] = defaultSettings["BobberLoc"];
    }
    if (!loadedSettings.hasOwnProperty("MythosHpLoc")) {
        loadedSettings["MythosHpLoc"] = defaultSettings["MythosHpLoc"];
    }
    if (!loadedSettings.hasOwnProperty("EffectsLoc")) {
        loadedSettings["EffectsLoc"] = defaultSettings["EffectsLoc"];
    }
    if (!loadedSettings.hasOwnProperty("BlazeLoc")) {
        loadedSettings["BlazeLoc"] = defaultSettings["BlazeLoc"];
    }
    if (!loadedSettings.hasOwnProperty("KuudraValueLoc")) {
        loadedSettings["KuudraValueLoc"] = defaultSettings["KuudraValueLoc"];
    }
    if (!loadedSettings.hasOwnProperty("fossilLoc")) {
        loadedSettings["fossilLoc"] = defaultSettings["fossilLoc"];
    }
    if (!loadedSettings.hasOwnProperty("LegionLoc")) {
        loadedSettings["LegionLoc"] = defaultSettings["LegionLoc"];
    }
    if (!loadedSettings.hasOwnProperty("StatsLoc")) {
        loadedSettings["StatsLoc"] = defaultSettings["StatsLoc"];
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
    worldLoaded = true;
});

register("command", () => {
    printBool = !printBool;
}).setName("sbodev")


// party detection
// partyleader
// register("chat", (party) => {
//     partyMembers = [];
//     party = party.removeFormatting().replaceAll("'s", "");
//     // ChatLib.chat("party: " + party);
//     party = party.replace(/\[.*?\]/g, '').replaceAll(" ", "")
//     partyMembers.push(party)
// }).setCriteria("&eYou have joined ${party} &r&eparty!&r");

// // rest of party
// register("chat", (party) => {
//     party = party.removeFormatting()
//     // ChatLib.chat("party: " + party);
//     party = party.replace(/\[.*?\]/g, '').replaceAll(" ", "")
//     // string to list names are separated by commas and extent partyMembers list with new names
//     partyMembers = partyMembers.concat(party.split(","))
// }).setCriteria("&eYou'll be partying with: ${party}");

// // player joined party
// register("chat", (player) => {
//     player = player.removeFormatting()
//     player = getplayername(player)
//     partyMembers.push(player)
// }).setCriteria("${player} &r&ejoined the party.&r");

// // player left party
// register("chat", (player) => {
//     player = player.removeFormatting()
//     player = getplayername(player)
//     partyMembers = partyMembers.filter(e => e !== player)
// }).setCriteria("${player} &r&ehas left the party.&r");

// // &b[MVP&r&3+&r&b] hiddeeee &r&ehas been removed from the party.&r
// register("chat", (player) => {
//     player = player.removeFormatting()
//     player = getplayername(player)
//     partyMembers = partyMembers.filter(e => e !== player)
// }).setCriteria("${player} &r&ehas been removed from the party.&r");

// // &eThe party was transferred to &r&b[MVP&r&3+&r&b] NotACrafter &r&ebecause &r&b[MVP&r&d+&r&b] AlexIy &r&eleft&r
// // player left party version 2
// register("chat", (leader, player) => {
//     player = player.removeFormatting()
//     player = getplayername(player)
//     partyMembers = partyMembers.filter(e => e !== player)
// }).setCriteria("&eThe party was transferred to ${leader} &r&ebecause ${player} &r&eleft");

// // you left party
// register("chat", () => {
//     partyMembers = [];
// }).setCriteria("&eYou left the party.&r");

// // get party members from party list
// register("chat", (type, player) => {
//     player = player.removeFormatting()
//     if (player.split("●").length > 0) {
//         player = player.split("●")
//         for (let i = 0; i < player.length; i++) {
//             player[i] = getplayername(player[i])
//             partyMembers.push(player[i]) 
//         }
//     }
//     else {
//         player = getplayername(player)
//         partyMembers.push(player)
//     }
// }).setCriteria("&eParty ${type}: ${player}");

// register("chat", (count) => {
//     partyMembers = [];
// }).setCriteria("&r&aParty members ${count}");

export function playCustomSound(sound, volume) {
    if (sound != "") {
        if (sound.includes(".ogg")) sound = sound.replace(".ogg", "");
        if (FileLib.exists(Config.modulesFolder.replace("modules", "images") + `/${sound}.ogg`)) {
            new Sound({ source: new java.lang.String(sound + ".ogg") }).setVolume(volume/100).play()
        }
        else {
            ChatLib.chat(`&6[SBO] &cSound file not found! (if the filename is correct, make sure to reload ct by "/ct load")`);
        }
    }
}

// add time to life of 5 sek
export function setInterval(func, delay, ttl) {
    let startTime = java.lang.System.currentTimeMillis();
    let thread = new java.lang.Thread(new java.lang.Runnable({
        run: function() {
            while (true) {
                if (java.lang.System.currentTimeMillis() - startTime > ttl) {
                    thread.stop();
                    break;
                }
                func();
                java.lang.Thread.sleep(delay);
            }
        }
    }));
    thread.start();
    return thread;
}

export function clearInterval(thread) {
    thread.stop();
}

HypixelModAPI.on("partyInfo", (partyInfo) => {
    Object.keys(partyInfo).forEach(key => {
        partyMembersUuids.push(key);
    })
    partyBool = true;
})

export function sendPartyRequest() {
    partyMembersUuids = [];
    partyBool = false;
    HypixelModAPI.requestPartyInfo();
}