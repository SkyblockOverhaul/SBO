/// <reference types="../CTAutocomplete" />
import "./features/Diana/DianaBurrows";
import "./features/diana/DianaMobDetect";
import "./features/general/CopyMessage";
import "./features/general/PartyCommands";
import "./features/general/Waypoints";
import "./features/general/messageHider";
import "./features/general/pickuplog";
import Settings from "./settings";
import "./utils/overlays";

// in sbo addons packen
import "./features/dungeon/recognizeRareRoom";
import "./features/general/QOL";
import { createWorldWaypoint } from "./features/general/Waypoints";
import "./features/guis/SlayerGuis";


register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});


// &eYou'll be partying with: &r&b[MVP&r&c+&r&b] vxnp&r&e, &r&b[MVP&r&0+&r&b] saltyarcher&r&e, &r&6[MVP&r&2++&r&6] Boi_&r&e, &r&b[MVP&r&2+&r&b] rigis&r
// You have joined [MVP++] Tricksyz's party!
register("chat", (event) => {
    Client.showTitle("&l&9!!!!!WORM!!!!!!", "&eKILL!!!", 0, 90, 20);
}).setCriteria("&r&7&oYou hear the sound of something approaching...&r");

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
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player + "&r: " + message);
        // print("&r&2Guild > &b[DC] &b" + player + "&r:" + message);
    }
    else if (player.includes("replying to")) {
        cancel(event);
        let split = player.split(" ");
        let player1 = split[0];
        let player2 = split[3];
        ChatLib.chat("&r&2Guild > &b[DC] &b" + player1.removeFormatting() + " &3replying to &b" + player2 + "&r: " + message);
        // print("&r&2Guild > &b[DC] &b" + player1 + " &3replying to &b" + player2 + "&r:" + message);
    }
}).setCriteria("&r&2Guild > &a[VIP] SlowDT &3[GM]&f: ${player}: ${message}").setContains()
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
//    }, 0);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.2);
//    }, 50);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.4);
//    }, 100);
//     setTimeout(function() {
//         World.playSound("random.levelup", 1, 1.6);
//    }, 150);
// }).setName("sboinq");

// register("guiOpened", () => {
//     const container = Player.getContainer();
//     if (container !== null) {
//         const rawFish = 0.5;   
//         const rawSalmon = 0.7;
//         const clownfish = 2.0;
//         const pufferfish = 1.0;
//         const prismarineShard = 0.5;
//         const prismarineCrystals = 0.5;
//         const sponge = 0.5;
//         // echanted version 
//         const enchantedPrismarineShard = 40; //
//         const enchantedPrismarineCrystals = 40; //
//         const enchantedSponge = 20; //
//         const enchantedClownfish = 320; //
//         const enchantedPufferfish = 160; //
//         const enchantedRawFish = 80; 
//         const enchantedRawSalmon = 112;

//         const cookedFish = 12800;
//         const cookedSalmon = 17920;
//         const wetSponge = 25600;

//         let totalRawExp = 0;
//         let items = container.getItems();
//         items.forEach((item, index) => {
//             if (item === null) return;
//             if (index > 53) return;
//             let name = item.getName();
//             let count = item.getStackSize();
//             if (name.includes("Raw Fish")) {
//                 totalRawExp += count * rawFish;
//             }
//             else if (name.includes("Raw Salmon")) {
//                 totalRawExp += count * rawSalmon;
//             }
//             else if (name.includes("Clownfish")) {
//                 totalRawExp += count * clownfish;
//             }
//             else if (name.includes("Pufferfish")) {
//                 totalRawExp += count * pufferfish;
//             }
//             else if (name.includes("Prismarine Shard")) {
//                 totalRawExp += count * prismarineShard;
//             }
//             else if (name.includes("Prismarine Crystals")) {
//                 totalRawExp += count * prismarineCrystals;
//             }
//             else if (name.includes("Sponge")) {
//                 totalRawExp += count * sponge;
//             }
//             else if (name.includes("Enchanted Prismarine Shard")) {
//                 totalRawExp += count * enchantedPrismarineShard;
//             }
//             else if (name.includes("Enchanted Prismarine Crystals")) {
//                 totalRawExp += count * enchantedPrismarineCrystals;
//             }
//             else if (name.includes("Enchanted Sponge")) {
//                 totalRawExp += count * enchantedSponge;
//             }
//             else if (name.includes("Enchanted Clownfish")) {
//                 totalRawExp += count * enchantedClownfish;
//             }
//             else if (name.includes("Enchanted Pufferfish")) {
//                 totalRawExp += count * enchantedPufferfish;
//             }
//             else if (name.includes("Enchanted Raw Fish")) {
//                 totalRawExp += count * enchantedRawFish;
//             }
//             else if (name.includes("Enchanted Raw Salmon")) {
//                 totalRawExp += count * enchantedRawSalmon;
//             }
//             else if (name.includes("Cooked Fish")) {
//                 totalRawExp += count * cookedFish;
//             }
//             else if (name.includes("Cooked Salmon")) {
//                 totalRawExp += count * cookedSalmon;
//             }
//             else if (name.includes("Wet Sponge")) {
//                 totalRawExp += count * wetSponge;
//             }
//             else {
//                 print(`&r&cUnknown Item: &e${name}`);
//             }
            
//         });
//         print(`&r&6Total Raw Exp: &e${totalRawExp}`);

//     }
// })