/// <reference types="../CTAutocomplete" />
import "./features/Diana/DianaBurrows";
import "./features/Kuudra";
import "./features/Diana/DianaMobDetect";
import "./features/general/CopyMessage";
import "./features/general/PartyCommands";
import "./features/general/Waypoints";
import "./features/general/fossilSolver";
import "./features/general/messageHider";
import "./features/general/pickuplog";
import "./features/guis/BobberCounter";
import "./features/guis/LegionCounter";
import "./features/slayer/BlazeSlayer";
import Settings from "./settings";
import "./utils/overlays";
import "./features/Diana/PartyFinder";
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import { data } from "./utils/variables";
import { isDataLoaded } from "./utils/checkData";
import { formatNumber, printDev } from "./utils/functions";
import { createWorldWaypoint, removeWorldWaypoint } from "./features/general/Waypoints";


register("command", (args1, ...args) => {
    if (args1 == undefined) {
        Settings.openGUI()
    } else {
        switch (args1.toLowerCase()) { 
            case "help":
                ChatLib.chat("&6[SBO] &eCommands:")
                ChatLib.chat("&7> &a/sbo &7- &eOpen the settings")
                ChatLib.chat("&7> &a/sbo help &7- &eShow this message")
                ChatLib.chat("&7> &a/sboguis &7- &eOpen the GUIs and move them around (alias /sbomoveguis)")
                ChatLib.chat("&7> &a/sboclearburrows &7- &eClear all burrow waypoints (alias /sbocb)")
                ChatLib.chat("&7> &a/sbocheck <player> &7- &eCheck a player (alias /sboc <player>)")
                ChatLib.chat("&7> &a/sbocheckp &7- &eCheck your party (alias /sbocp)")
                ChatLib.chat("&7> &a/sbodc &7- &eCommand for diana dropchances")
                ChatLib.chat("&7> &a/sbopartyblacklist &7- &eCommand for party commands blacklisting")
                break;
            default:
                ChatLib.chat("&6[SBO] &eUnknown command. Use /sbo help for a list of commands")
                break;
        }
    }
}).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

// dowload msg beispiel
const newVersion = "0.3.0" // hier neue version eintragen wenn changelog angezeigt werden soll
const downloadMsgReg = register("step", () => {
    if (!World.isLoaded()) return
    if (!isDataLoaded()) return
    if (data.downloadMsg) {
        downloadMsgReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&aThanks for importing &6SBO`)
    ChatLib.chat(`&7> &ayou can open the settings with /sbo`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.downloadMsg = true
    data.changelogVersion = newVersion
    data.save()
    
    downloadMsgReg.unregister()
}).setFps(1)

// changelog beispiel
const changeLogReg = register("step", () => {
    if (!World.isLoaded()) return
    if (!isDataLoaded()) return
    if (!data.downloadMsg) return
    if (data.changelogVersion === newVersion) { 
        changeLogReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&6[SBO] &r&bVersion &e${newVersion}&r`)
    ChatLib.chat(`&aChangelog:`)
    ChatLib.chat(`&7> &a-- Diana Guis -- `)
    ChatLib.chat(`&7> &aAdded profit/hr`)
    ChatLib.chat(`&7> &a-- Party Commands --`)
    ChatLib.chat(`&7> &aAdded !playtime, !pt`)
    ChatLib.chat(`&7> &aAdded !profit`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.changelogVersion = newVersion
    data.save()
    changeLogReg.unregister()
}).setFps(1)

register("soundPlay", (pos, name, volume, pitch, categoryName, event) => {
    printDev(`Sound: ${name} | Volume: ${volume} | Pitch: ${pitch} | Category: ${categoryName}`)
})


let waypoints = [];
//detected block change packet: BlockPos{x=-99, y=77, z=32} minecraft:lit_redstone_lamp (2)
//detected block change packet: BlockPos{x=-106, y=77, z=31} minecraft:redstone_lamp
// detected block change packet: Vec3i{x=-99,y=77,z=32} minecraft:redstone_lamp
register("packetReceived", (packet, event) => { 
    const blockPos =  new BlockPos(packet.func_179827_b());// getBlockPosition
    const blockState = packet.func_180728_a() // getBlockState
    if (blockState == "minecraft:lit_redstone_lamp") {
        ChatLib.chat(`detected block change packet: ${blockPos} ${blockState}`)

        createWorldWaypoint("Lamp", blockPos.getX() +1, blockPos.getY() +1, blockPos.getZ(), 255, 0, 0, true);
    }
    else if (blockState == "minecraft:redstone_lamp") {
        removeWorldWaypoint(blockPos.getX()+1, blockPos.getY() +1, blockPos.getZ());
    }
}).setFilteredClass(net.minecraft.network.play.server.S23PacketBlockChange)

register("chat", () => {
    waypoints = []
}).setCriteria("&e[NPC] Carnival Cowboy&f: &rGood luck, pal!&r");

// dojo sounds:
// [DEV]: Sound: mob.cat.hiss | Volume: 2 | Pitch: 1.4920635223388672 | Category: ANIMALS
// [DEV]: Sound: mob.zombie.woodbreak | Volume: 1.5 | Pitch: 1 | Category: MOBS

// when hit with leftclick:
// [DEV]: Sound: random.successful_hit | Volume: 1 | Pitch: 1 | Category: PLAYERS

// register("chat", (drop) => {
//     let magicFindMatch = drop.match(/\+(&r&b)?(\d+)%/);
//     let chimMf = parseInt((magicFindMatch ? magicFindMatch[2] : 0));
//     if(chimMf > 0) {
//         if(data.last10ChimMagicFind.length >= 10) {
//             data.last10ChimMagicFind.shift();
//         }
//         data.last10ChimMagicFind.push(chimMf);
    
//         let sum = data.last10ChimMagicFind.reduce((a, b) => a + b, 0);
//         data.avgChimMagicFind = parseInt(sum / data.last10ChimMagicFind.length);
//     }

// }).setCriteria("&r&6&lRARE DROP! ${drop}");

// register("step", () => {
//     // print each egg
//     print(`&r&6Eggs: &e${eggs.length}`);
//     eggs.forEach(egg => {
//         createWorldWaypoint("Egg", egg.x, egg.y, egg.z, 0, 255, 0);
//     })
// }).setFps(1);

// register("chat", (event) => {
//     Client.showTitle("&l&9!!!!!WORM!!!!!!", "&eKILL!!!", 0, 90, 20);
// }).setCriteria("&r&7&oYou hear the sound of something approaching...&r");

// register("chat", (message, event) => {
//     message = message.removeFormatting();
//     if (!message.includes("Powder") && !message.includes("Refelctor") && !message.includes("Blue Goblin Egg") && !message.includes("Heart")) {
//         cancel(event);
//     }
//     if (message.includes("Refelctor")) {
//         Client.showTitle("&9Robotron Reflector", "&eCarrot", 0, 40, 20);
//     }
//     if (message.includes("Blue Goblin Egg")) {
//         Client.showTitle("&3Blue Goblin Egg", "&eCarrot", 0, 40, 20);
//     }
// }).setCriteria("&r&aYou received ${message}");

// register("command", () => {
//     // Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, "&r&b[MVP&f+&b] RolexDE", 0, 90, 20);
// //     Client.showTitle(`&5&lMinos Relic!`, "", 0, 25, 35);
// //     ChatLib.chat("&6[SBO] &r&6&lRARE DROP! &5Minos Relic!");
// //     setTimeout(function() {
// //         World.playSound("random.levelup", 1, 1.0);
// //    }, 0);
// //     setTimeout(function() {
// //         World.playSound("random.levelup", 1, 1.2);
// //    }, 50);
// //     setTimeout(function() {
// //         World.playSound("random.levelup", 1, 1.4);
// //    }, 100);
// //     setTimeout(function() {
// //         World.playSound("random.levelup", 1, 1.6);
// //    }, 150);
// }).setName("sboinq");


