import settings from "../../settings";
import { getplayername, trace } from "../../utils/functions";
import { registerWhen } from "../../utils/variables";
import { getWorld, getZone } from "../../utils/world";
import { createWorldWaypoint, removeWorldWaypoint } from "./Waypoints";
import RenderLibV2 from "../../../RenderLibV2";

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
// &a&aYou tipped 5 players in 1 game!&r
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
// &dFrom &r&b[MVP&r&a+&r&b] LeWhiteCore&r&7: &r&7!inv&r

// carnival helper
let lampOn = false;
registerWhen(register("packetReceived", (packet, event) => { 
    // only chars a-z und A-Z in carnival and no spaces
    if (getZone().replaceAll(" ", "").replaceAll(/[^a-zA-Z]/g, "") == "Carnival") {
        const blockPos =  new BlockPos(packet.func_179827_b());
        const blockState = packet.func_180728_a();
        if (blockState == "minecraft:lit_redstone_lamp") {
            // ChatLib.chat(`detected block change packet: ${blockPos} ${blockState} `);
            print(getZone().replaceAll(" ", "").replaceAll(/[^a-zA-Z]/g, ""));
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
                // print(helmetName)
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