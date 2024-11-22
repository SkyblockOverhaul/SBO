import settings from "../../settings";
import { getplayername, trace, formatTimeMinSec, setTimeout, getTextureID, isInSkyblock } from "../../utils/functions";
import { registerWhen, timerCrown, data } from "../../utils/variables";
import { getWorld, getZone } from "../../utils/world";
import { createWorldWaypoint, removeWorldWaypoint } from "./Waypoints";
import RenderLibV2 from "../../../RenderLibV2";
import { SboOverlay, OverlayTextLine, OverlayButton, hoverText } from "../../utils/overlays";

// register dragon wings for golden dragon nest
let found = false;
registerWhen(register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    if (name === "mob.enderdragon.wings") {
        if (!found) {
            // createNestWayoint(pos.x, pos.y, pos.z);
            createWorldWaypoint("§6Dragon's Lair", pos.x, pos.y, pos.z, 1*255, 0.84*255, 0)
            Client.showTitle(`&r&6&l<&b&l&kO&6&l> &6&lDragon's Lair Found! &6&l<&b&l&kO&6&l>`, "&eat x: " + pos.x + " y: " + pos.y + " z: " + pos.z, 0, 40, 20);
            ChatLib.chat("&6[SBO] &r&eFound &6Dragon's Lair!&r&e at x: " + pos.x + " y: " + pos.y + " z: " + pos.z);
            found = true;
        }
    }
}), () => getWorld() === "Crystal Hollows" && settings.findDragonNest);

registerWhen(register("chat", (player) => {
    if (player.includes(Player.getName())) {
        checkIfInMineshaft()    
    }
}).setCriteria("${player} &r&7entered the mineshaft&r&7!&r"), () => settings.exitWaypoint);

let timeout = 0;
function checkIfInMineshaft() {
    if (getWorld() === "Mineshaft") {
        timeout = 0;
        setTimeout(function() {
            createWorldWaypoint("§eExit", Math.round(Player.getLastX()), Math.round(Player.getLastY()), Math.round(Player.getLastZ()), 3, 252, 244);
        }, 100);
    }
    else {
        timeout++;
        if (timeout < 15) {
            setTimeout(checkIfInMineshaft, 500);
        }
    }
}

registerWhen(register("chat", (player, dings) => {
    if (player.includes(Player.getName())) {
        checkIfInMineshaft();
    }
}).setCriteria("${player} &r&eentered &r&aGlacite Mineshafts${dings}"), () => settings.exitWaypoint);


registerWhen(register("chat", (dings1, event) => {
    cancel(event);
}).setCriteria("&a&aYou tipped ${dings1}"), () => settings.hideTippedPlayers);
// registerWhen(register("chat", () => {
//     Client.showTitle("&l&9MINESHEFT!", "", 0, 90, 20);
// }).setCriteria("&r&5&lWOW! &r&aYou found a &r&bGlacite Mineshaft &r&aportal!&r"), () => settings.mineshaftAnnouncer);

// format Bridge Bot
registerWhen(register("chat", (botName, player, message, event) =>{
    // cancel original message
    // send new guildbot message
    botName = botName.removeFormatting();
    if (botName.includes("]")) {
        botName = botName.split("] ")[1];
    }
    if (botName.includes(" ")) {
        botName = botName.split(" ")[0];
    }
    if (settings.bridgeBotName.toLowerCase() == botName.toLowerCase()) {
        if (!player.includes(" ")) {
            cancel(event);
            player = player.removeFormatting();
            ChatLib.chat("&r&2Guild > &b[Bridge] &b" + player + "&r: " + message);
            // print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
        }
        else if (player.includes("replying to")) {
            cancel(event);
            let split = player.split(" ");
            let player1 = split[0];
            let player2 = split[3];
            ChatLib.chat("&r&2Guild > &b[Bridge] &b" + player1.removeFormatting() + " &3replying to &b" + player2 + "&r: " + message);
            // print("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
        }
    }
}).setCriteria("&r&2Guild > ${botName}: ${player}: ${message}"), () => settings.formatBridgeBot);
// old &r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}: ${message}
// geht
// &r&2Guild > &a[VIP] SlowDT &3[GM]&f: &rSuccesfully invited kenchika to the party!&r
// &r&2Guild > &b[MVP&2+&b] MasterNR &3[320]&f: &rnice&r
// testen
// &r&2Guild > birgeBot: player: message

registerWhen(register("chat", (pet, event) => {
    cancel(event);
}).setCriteria("&cAutopet &eequipped your ${pet}&a&lVIEW RULE&r"), () => settings.hideAutoPetMSG);

// &cAutopet &eequipped your &7[Lvl 100] &6Griffin&d ✦&e! &a&lVIEW RULE&r

registerWhen(register("chat", (player, command) => {
    command = command.removeFormatting().toLowerCase().replace(" ", "");
    if (command == "!inv" || command == "!invite") {
        player = player.removeFormatting()
        player = getplayername(player)
        setTimeout(function() {
            new TextComponent("&6[SBO] &eClick to inv player").setClick("run_command", "/p invite " + player).setHover("show_text", "/p invite " + player).chat();
        }, 50);
    }
}).setCriteria("&dFrom ${player}&r&7: ${command}"), () => settings.clickableInvite);

let lampOn = false;
registerWhen(register("packetReceived", (packet, event) => { 
    if (isInSkyblock()) {
        if (getZone().replaceAll(" ", "").replaceAll(/[^a-zA-Z]/g, "") == "Carnival") {
            const blockPos =  new BlockPos(packet.func_179827_b());
            const blockState = packet.func_180728_a();
            if (blockState == "minecraft:lit_redstone_lamp") {
                if (blockPos.getX() != -101 && blockPos.getY() != 70 && blockPos.getZ() != 14) {
                    createWorldWaypoint("", blockPos.getX() +1, blockPos.getY() +1, blockPos.getZ(), 255, 0, 0, true, false, false);
                    lampOn = true;
                }
            }
            else if (blockState == "minecraft:redstone_lamp") {
                removeWorldWaypoint(blockPos.getX()+1, blockPos.getY() +1, blockPos.getZ());
                lampOn = false;
            }
        }
    }
}).setFilteredClass(net.minecraft.network.play.server.S23PacketBlockChange), () => settings.carnivalLamp);

const EntityZombie = Java.type("net.minecraft.entity.monster.EntityZombie")
const mobRating = {
    "Leather": 1,
    "Gold": 3,
    "Iron": 2,
    "Diamond": 4
}
registerWhen(register("renderWorld", () => {
    if (getZone().replaceAll(" ", "").replaceAll(/[^a-zA-Z]/g, "") == "Carnival") {
        const entities = World.getAllEntitiesOfType(EntityZombie.class).filter(a => !a.isInvisible())
        let bestMob = undefined
        let bestMobRating = 0

        for(let i = 0; i < entities.length; i++) {
            let helmetName = new EntityLivingBase(entities[i].getEntity()).getItemInSlot(4)?.getName()?.removeFormatting()
            if (helmetName != undefined) {
                let type = undefined
                switch (helmetName) {
                    case "Leather Cap":
                        type = "Leather"
                        break
                    case "Golden Helmet":
                        type = "Gold"
                        break
                    case "Diamond Helmet":
                        type = "Diamond"
                        break
                    case "Iron Helmet":
                        type = "Iron"
                        break
                }
                if (type != undefined) {
                    if (mobRating[type] > bestMobRating) {
                        bestMob = entities[i]
                        bestMobRating = mobRating[type]
                    }
                }
            }
        }
        if (bestMob != undefined) {
            RenderLibV2.drawEspBoxV2(bestMob.getRenderX(), bestMob.getRenderY(), bestMob.getRenderZ(), 1, 2, 1, 0, 0, 1, 1, false)
            if (!lampOn && settings.CarnivalZombieLine) {
                trace(bestMob.getRenderX(), bestMob.getRenderY() + 1, bestMob.getRenderZ(), 0, 0, 1, 0.7, "", 2);
            }
        }
    }
}), () => settings.carnivalZombie);

let goldenFishOverlay = new SboOverlay("Golden Fish", "goldenFishTimer", "render", "GoldenFishLoc")
let goldenFishTitle = new OverlayTextLine("&6&lGolden Fish Timer")
let goldenFishSpawnLine = new OverlayTextLine("&6Next Spawn: &b15m 0s")
let goldenFishTimeLine = new OverlayTextLine("&6Cast Before: &b3m 0s")
goldenFishOverlay.setLines([goldenFishTitle, goldenFishSpawnLine, goldenFishTimeLine])

let spawnTimer = 0
let lastTimeThrown = 0
let throwBool = false
registerWhen(register("playerInteract", (action, pos) => {
    let item = Player.getHeldItem()
    if (item == null) return
    if (item.getLore()[1].includes("§8Lava Rod")) {
        lastTimeThrown = Date.now()
        throwBool = true
    }
}), () => settings.goldenFishTimer && getWorld() == "Crimson Isle");

registerWhen(register("tick", () => {
    if (getWorld() != "Crimson Isle" && goldenFishOverlay.renderGui) {
        if (spawnTimer != 0) resetGoldenFish();
        goldenFishOverlay.renderGui = false
    } else if (getWorld() == "Crimson Isle" && !goldenFishOverlay.renderGui) {
        goldenFishOverlay.renderGui = true
    }
    if (spawnTimer == 0 && lastTimeThrown != 0) {
        spawnTimer = Date.now()
    }    
    if (Date.now() - lastTimeThrown > 150000 && throwBool) {
        if (settings.goldenFishNotification) {
            ChatLib.chat("&6[SBO] &eYou have not thrown your Lava Rod in over 2 minutes and 30 seconds")
            for (let i = 0; i < 6; i++) {
                setTimeout(() => {
                    World.playSound("random.levelup", 100, 1);
                }, i * 500);
            }
            Client.showTitle("&cCast Rod", "", 0, 100, 20);
            throwBool = false
        } 
    }
    if (lastTimeThrown != 0) { // at 15m its 0% chance at 20m its 100% chance linearly
        if (900000 - (Date.now() - spawnTimer) < 0) {
            let spawnChance = calculatePercentage(Date.now() - spawnTimer, 900000, 1200000)
            goldenFishSpawnLine.setText("&6Spawn Chance: &b" + (spawnChance).toFixed(2) + "%")
        } else {
            goldenFishSpawnLine.setText(`&6Next Spawn: &b${formatTimeMinSec(900000 - (Date.now() - spawnTimer))}`)
        }
        goldenFishTimeLine.setText(`&6Cast Before: &b${formatTimeMinSec(180000 - (Date.now() - lastTimeThrown))}`)
        if (180000 - (Date.now() - lastTimeThrown) < 0) {
            resetGoldenFish();
        }
    }
}), () => settings.goldenFishTimer);

function resetGoldenFish() {
    spawnTimer = 0
    lastTimeThrown = 0
    throwBool = false
    goldenFishSpawnLine.setText("&6Next Spawn: &b15m 0s")
    goldenFishTimeLine.setText("&6Cast Before: &b3m 0s")
}

function calculatePercentage(timeInMillis, minTime, maxTime) {
    if (timeInMillis <= minTime) {
        return 0;
    } else if (timeInMillis >= maxTime) {
        return 100;
    } else {
        return ((timeInMillis - minTime) / (maxTime - minTime)) * 100;
    }
}

registerWhen(register("chat", (rarity) => {
    resetGoldenFish();
}).setCriteria("TROPHY FISH! You caught a Golden Fish ${rarity}"), () => settings.goldenFishTimer && getWorld() == "Crimson Isle"); 
registerWhen(register("chat", () => {
    resetGoldenFish();
}).setCriteria("&r&9The &r&6Golden Fish &r&9swims back beneath the lava...&r"), () => settings.goldenFishTimer && getWorld() == "Crimson Isle");

registerWhen(register("chat", () => {
    ChatLib.chat("&6[SBO] &eA Golden Fish has spawned")
}).setCriteria("You spot a Golden Fish surface from beneath the lava!"), () => settings.goldenFishTimer && getWorld() == "Crimson Isle");

let flareOverlay = new SboOverlay("Flare", "flareTimer", "render", "FlareLoc")
let flareLine = new OverlayTextLine("&5SOS Flare: &b3m 0s")
flareOverlay.renderGui = false
flareOverlay.setLines([flareLine])

let flareType = ""
let flareTimer = 0
let warningSent = false
let flareClicked = false
let flareEntity = undefined
function resetFlare() {
    if (flareTimer == 0) return
    flareType = ""
    flareTimer = 0
    warningSent = false
    flareClicked = false
    flareOverlay.renderGui = false
    flareEntity = undefined
    flareLine.setText("&5SOS Flare: &b3m 0s")
    ChatLib.chat("&6[SBO] &eFlare has expired")
}

registerWhen(register("worldUnload", () => {
    resetFlare();
    resetGoldenFish();
}), () => settings.flareTimer || settings.goldenFishTimer);
registerWhen(register("chat", () => {
    resetFlare();
}).setCriteria("&r&eYour flare disappeared because you were too far away!&r"), () => settings.flareTimer);

const EntityFireworkRocket = Java.type("net.minecraft.entity.item.EntityFireworkRocket");
function findFlare() {
    const player = Player.getPlayer()
    const flareObj = World.getAllEntitiesOfType(EntityFireworkRocket).filter(flare => flare.distanceTo(player) <= 40)
    if (flareObj.length > 0) {
        flareLine.setText(flareType + ": &b" + formatTimeMinSec(180000))
        warningSent = false
        flareOverlay.renderGui = true
        flareEntity = flareObj[0]
        flareTimer = Date.now()
    }
}

registerWhen(register("tick", () => {
    if (flareTimer != 0 || bestRandomFlare != "") {
        let inRange = false
        if (flareTimer != 0) inRange = flareEntity.distanceTo(Player.getPlayer()) <= 40;
        if (flareScore[flareType] >= flareScore[bestRandomFlare] && flareTimer != 0 && (!settings.notInRangeSetting || inRange)) {
            const rangeText = inRange ? "&7(&ain range&7)" : "&7(&cout of range&7)"
            flareLine.setText(flareType + ": &b" + formatTimeMinSec(180000 - (Date.now() - flareTimer)) + " " + rangeText)
            if (!warningSent && Date.now() - flareTimer > 160000) { // 2 minutes 40 seconds
                ChatLib.chat("&6[SBO] &eFlare will expire in 20 seconds")
                warningSent = true
                if (settings.flareExpireAlert) {
                    Client.showTitle("&cFlare Expires In 20 Seconds", "", 0, 40, 20);
                }
            }
            if (Date.now() - flareTimer > 180000) { // 3 minutes
                resetFlare();
            }
        } else {
            if (bestRandomFlare != "") {
                flareLine.setText(bestRandomFlare + ": &bin range")
            } else {
                flareLine.setText("")
            }
        }
    } 
    if (flareClicked) {
        flareClicked = false
        setTimeout(() => {
            findFlare()
        }, 400);
    }
}), () => settings.flareTimer);

const flareHeads = {
    "c0062cc98ebda72a6a4b89783adcef2815b483a01d73ea87b3df76072a89d13b": "&5SOS Flare",
    "9d2bf9864720d87fd06b84efa80b795c48ed539b16523c3b1f1990b40c003f6b": "&9Alert Flare",
    "22e2bf6c1ec330247927ba63479e5872ac66b06903c86c82b52dac9f1c971458": "&aWarning Flare"
}

const flareScore = {
    "&5SOS Flare": 3,
    "&9Alert Flare": 2,
    "&aWarning Flare": 1,
    "": 0
}

let randomFlares = []
let bestRandomFlare = ""
registerWhen(register("step", () => {
    randomFlares = []
    const player = Player.getPlayer()
    World.getAllEntitiesOfType(net.minecraft.entity.item.EntityArmorStand).filter(flare => flare.distanceTo(player) <= 40).forEach((flare) => {
        let headItem = new EntityLivingBase(flare.getEntity()).getItemInSlot(4)
        let headNbt = headItem?.getNBT()

        if (headNbt != undefined) {
            if (flareHeads[getTextureID(headNbt)]) {
                randomFlares.push(flareHeads[getTextureID(headNbt)])
            }
        }
    })
    bestRandomFlare = ""
    if (randomFlares.length != 0) {
        flareOverlay.renderGui = true
        for (let i = 0; i < randomFlares.length; i++) {
            if (flareScore[randomFlares[i]] > flareScore[bestRandomFlare]) {
                bestRandomFlare = randomFlares[i]
            }
        }
    }
    else {
        if (flareTimer == 0) {
            flareOverlay.renderGui = false
        }
    }
}).setFps(1), () => settings.flareTimer);

registerWhen(register("playerInteract", (action, pos) => {
    let item = Player.getHeldItem()
    if (item == null) return
    if (item.getName().includes("Flare") && action.toString().includes('RIGHT_CLICK') && !flareClicked) {
        flareType = item.getName().replace("§", "&")
        flareClicked = true
    }
}), () => settings.flareTimer);