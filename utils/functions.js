import settings from "../settings";
import { registerWhen } from "./variables";

// geklaut von coleweight for drawline
if(!GlStateManager) {
    var GL11=Java.type("org.lwjgl.opengl.GL11")
    var GlStateManager=Java.type("net.minecraft.client.renderer.GlStateManager")
}
export function trace (x, y, z, red, green, blue, alpha, lineWidth = 1){
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
    if(Player.isSneaking())
        drawLine(Player.getRenderX(), Player.getRenderY() + 1.54, Player.getRenderZ(), x, y, z, red, green, blue, alpha, lineWidth)
    else
        drawLine(Player.getRenderX(), Player.getRenderY() + 1.62, Player.getRenderZ(), x, y, z, red, green, blue, alpha, lineWidth)
}

function drawLine (x1, y1, z1, x2, y2, z2, red, green, blue, alpha, lineWidth = 1)
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

export function getSBID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("id") || null;
}
export function getSBUUID(item) {
    return item?.getNBT()?.getCompoundTag("tag")?.getCompoundTag("ExtraAttributes")?.getString("uuid") || null;
}

export function checkIfInSkyblock() {
    return Scoreboard.getTitle()?.removeFormatting().includes("SKYBLOCK");
}

// returns if in skyblock //
let inSkyblock = false;
export function isInSkyblock() {
    return inSkyblock;
}

export let state = {
    entityDeathOccurred: false
}

// return 1sec long true if entity death occurred //
export function isActiveForOneSecond() {
    return state.entityDeathOccurred;
}

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
    if (!worldLoaded) return {};
    if (type === "hotbar") {
        var slots = 8;
    }
    else {
        var slots = 39;
    }
    playerItems = {}
    var playerInv = Player.getInventory();
    var playerInvItems = playerInv.getItems();
    for (var i in playerInv.getItems()) {
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
    var hotbarItems = readPlayerInventory("hotbar");
    for (var i in hotbarItems) {
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
registerWhen(register("step", () => {
    spadeBool = checkItemInHotbar("ANCESTRAL_SPADE");
}).setFps(1), () => settings.dianaLootTracker || settings.dianaMobTracker);

// initialize tracker //
export function initializeTracker() {
    tempTracker = {
        items: {
            "coins": 0,
            "Griffin Feather": 0,
            "Crown of Greed": 0,
            "Washed-up Souvenir": 0,
            "Chimera": 0,
            "Daedalus Stick": 0,
            "DWARF_TURTLE_SHELMET": 0,
            "CROCHET_TIGER_PLUSHIE": 0,
            "ANTIQUE_REMEDIES": 0,
            "ENCHANTED_ANCIENT_CLAW": 0,
            "ANCIENT_CLAW": 0,
            "MINOS_RELIC": 0,
            "Total Burrows": 0
        },
        mobs: {
            "Minos Inquisitor": 0,
            "Minos Champion": 0,
            "Minotaur": 0,
            "Gaia Construct": 0,
            "Siamese Lynxes": 0,
            "Minos Hunter": 0,
            "TotalMobs": 0
        }
    };
    return tempTracker;
}

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
            "x": 10,
            "y": 50,
            "s": 1
        },
        LootLoc: {
            "x": 10,
            "y": 150,
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
            "x": 561,
            "y": 203,
            "s": 1
        },
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
    return loadedSettings;
}


export function saveGuiSettings(guiSettings) {
        FileLib.write("SBO", "guiSettings.json", JSON.stringify(guiSettings, null, 4));
}

export function drawRect(x1,y1,scale,z) {
    let x = x1/scale
    let y = y1/scale
    // settings.myColor: java.awt.Color[r=19,g=145,b=224]
    let color = Renderer.color(settings.myColor.getRed(), settings.myColor.getGreen(), settings.myColor.getBlue(), 200)
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
    name = player.substring(num+2)
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

