/// <reference types="../CTAutocomplete" />
import "./features/Diana/DianaBurrows";
import "./features/Kuudra";
import "./features/diana/DianaMobDetect";
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

// in sbo addons packen
import "./features/dungeon/recognizeRareRoom";
import "./features/general/QOL";
import "./features/guis/SlayerGuis";
import { data } from "./utils/variables";
import { isDataLoaded } from "./utils/checkData";


register("command", () => Settings.openGUI()).setName("skyblockoverhaul").setAliases("sbo");

// Title bug fix
register("worldLoad", () => {
    Client.showTitle("", "", 0, 40, 20);
});

// dowload msg beispiel
const newVersion = "0.1.7" // hier neue version eintragen wenn changelog angezeigt werden soll
const downloadMsgReg = register("step", () => {
    if (!World.isLoaded()) return
    if (!isDataLoaded()) return
    if (data.downloadMsg) {
        downloadMsgReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&aThanks for importing &6 SBO`)
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
    if (data.changelogVersion === newVersion) { 
        changeLogReg.unregister()
        return
    }
    ChatLib.chat(ChatLib.getChatBreak("&b-"))
    ChatLib.chat(`&6[SBO] &r&bVersion &e${newVersion}&r`)
    ChatLib.chat(`&aChangelog:`)
    ChatLib.chat(`&7> &aUpdate Fossil Solver (better detection)`)
    ChatLib.chat(`&7> &aRemoved Mineshaft title (hypixel added it)`)
    ChatLib.chat(`&7> &aFixed bug with Blaze Slayer Effects`)
    ChatLib.chat(`&7> &aAdded Guild Bridge Bot Formatter`)
    ChatLib.chat(`&7> &aSome minor bug fixes`)
    ChatLib.chat(ChatLib.getChatBreak("&b-"))

    data.changelogVersion = newVersion
    data.save()
    changeLogReg.unregister()
}).setFps(1)


// let eggs = []
// register("spawnParticle", (particle, type, event) => {
//     if (type.toString() == "CRIT_MAGIC" || type.toString() == "CRIT" || type.toString() == "REDSTONE") {
//         if (eggs.length > 3) {
//             eggs.shift();
//         }
//         const particlepos = particle.getPos();
//         const xyz = [particlepos.getX(), particlepos.getY(), particlepos.getZ()];
//         const [x, y , z] = [xyz[0], xyz[1], xyz[2]];
//         // check if coords are already in eggs also check for eggs that no eggs are 3 blocks away
//         if (eggs.some(egg => egg.x === x && egg.y === y && egg.z === z)) {
//             return;
//         }
//         if (eggs.some(egg => Math.abs(egg.x - x) <= 3 && Math.abs(egg.y - y) <= 3 && Math.abs(egg.z - z) <= 3)) {
//             return;
//         }
//         // check if coords are already in found eggs or 3 blocks away
//         if (foundEggs.some(egg => egg.x === x && egg.y === y && egg.z === z)) {
//             return;
//         }
//         if (foundEggs.some(egg => Math.abs(egg.x - x) <= 3 && Math.abs(egg.y - y) <= 3 && Math.abs(egg.z - z) <= 3)) {
//             return;
//         }
//         // eggs.push({"x": x, "y": y, "z": z});

//     }
//     // if (type.toString() != "CRIT_MAGIC" && type.toString() != "CRIT" && type.toString() != "REDSTONE" && type.toString() != "SNOWBALL" && type.toString() !== "SPELL_MOB" && type.toString() !== "DRIP_WATER") {
//     //     ChatLib.chat(type.toString());
//     // }
// })

// let foundEggs = []
// register("chat", () => {
//     // remove egg nearst to player
//     if (foundEggs.length > 3) {
//         foundEggs.shift();
//     }
//     ChatLib.chat(`found egg`);
//     const player = Player.getPos();
//     let closestDistance = Infinity;
//     let closestEgg = null;
//     eggs.forEach(egg => {
//         const distance = Math.sqrt(
//             (player.x - egg.x)**2 +
//             (player.y - egg.y)**2 +
//             (player.z - egg.z)**2
//         );
//         if (distance < closestDistance) {
//             closestDistance = distance;
//             closestEgg = egg;
//         }
//     });
//     if (closestEgg !== null) {
//         eggs = eggs.filter(egg => egg !== closestEgg);
//         ChatLib.chat(`found egg at ${closestEgg.x} ${closestEgg.y} ${closestEgg.z}`);
//         foundEggs.push(closestEgg);
//     }
// }).setCriteria("${color}HOPPITY'S HUNT &r&dYou found a ${type} Egg ${loc}")
// // &r&d&lHOPPITY'S HUNT &r&dYou found a &r&6Chocolate Breakfast Egg &r&dbehind the Gold Forger&r&d!&r
// // &r&d&lHOPPITY'S HUNT &r&dYou found a &r&9Chocolate Lunch Egg &r&din front of the Iron Forger&r&d!&r
// // &r&d&lHOPPITY'S HUNT &r&dYou found a &r&aChocolate Dinner Egg &r&dnear Rusty&r&d!&r


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