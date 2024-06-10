import { request } from "../../../requestV2";
import { getPartyBool, getplayername, setInterval, clearInterval, sendPartyRequest, getPartyMembersUuids } from "../../utils/functions";
import { HypixelModAPI } from "./../../../HypixelModAPI";

let api = "https://api.skyblockoverhaul.com";

function getPartyInfo(party) {
    party = party.filter(uuid => uuid != Player.getUUID());
    request({
        url: api + "/partyInfoByUuids?uuids=" + party.join(",").replaceAll("-", ""),
        json: true
    }).then((response)=> {
        printPartyInfo(response.PartyInfo)
    }).catch((error)=> {
        console.error(JSON.stringify(error));
    });
}

// message to check party when joining a party
register("chat", (party) => {
    setTimeout(() => {
        new TextComponent("&6[SBO] &eClick to check party members").setClick("run_command", "/sbocheckp").setHover("show_text", "/sbocheckp").chat();
    }, 100);
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

// command to check party members
let checkPartyBool = false;
HypixelModAPI.on("partyInfo", (partyInfo) => {
    if (!checkPartyBool) return;
    checkPartyBool = false;
    let party = [];
    Object.keys(partyInfo).forEach(key => {
        party.push(key);
    })
    if (party.length == 0) {
        ChatLib.chat("&6[SBO] &eNo party members found. try join a party");
        return;
    }
    getPartyInfo(party);
})

let lastUsed = 0;
register("command", () => {
    if (Date.now() - lastUsed > 60000 || lastUsed == 0) { // 1 minutes
        checkPartyBool = true;
        lastUsed = Date.now();
        ChatLib.chat("&6[SBO] &eChecking party members...");
        sendPartyRequest();
        // let interval = setInterval(() => {
        //     if (getPartyBool()) {
        //         let party = getPartyMembersUuids();
        //         if (party.length == 0) {
        //             ChatLib.chat("&6[SBO] &eNo party members found. try join a party");
        //             return;
        //         }
        //         getPartyInfo(party);
        //         clearInterval(interval);
        //     }
        // }, 100, 50000);
    }
    else {
        ChatLib.chat("&6[SBO] &ePlease wait 1 minutes before checking party members again.");
    }
}).setName("sbocheckp").setAliases("sbocp");;

function printPartyInfo(partyinfo) {
    for (let i = 0; i < partyinfo.length; i++) {
        // if (partyinfo[i].legPet) { // to remove all player without legendary griffin pet
            ChatLib.chat("&6[SBO] &eName&r&f: &r&b" + partyinfo[i].name + "&r&e&r&9│ &r&eLvL&r&f: &r&6" + partyinfo[i].sbLvl + "&r&e&r&9│ &r&eEman 9&r&f: &r&f" + (partyinfo[i].eman9 ? "&r&a✓" : "&4✗") + "&e&r&9│ &r&el5 Daxe&r&f: " + (partyinfo[i].looting5daxe ? "&a✓" : "&4✗") + "&e &r&9│ &r&eKills&r&f: &r&6" + (partyinfo[i].mythosKills / 1000).toFixed(2) + "k");
        // }
    }
}

/// Player Checker
function printCheckedPlayer(playerinfo) {
    playerinfo = playerinfo[0];
    new TextComponent("&6[SBO] &eName&r&f: &r&b" + playerinfo.name + 
    "&r&e&r&9│ &r&eLvL&r&f: &r&6" + playerinfo.sbLvl + 
    "&r&e&r&9│ &r&eEman 9&r&f: &r&f" + (playerinfo.eman9 ? "&r&a✓" : "&4✗") + "&e&r&9│ &r&el5 Daxe&r&f: " + 
    (playerinfo.looting5daxe ? "&a✓" : "&4✗") + 
    "&e &r&9│ &r&eKills&r&f: &r&6" + 
    (playerinfo.mythosKills / 1000).toFixed(2) + "k")
    .setClick("run_command", "/p " + playerinfo.name).setHover("show_text", "/p " + playerinfo.name).chat();
}

function checkPlayer(player) {
    let playerName = player;
    if (!playerName) {
        ChatLib.chat("&6[SBO] &ePlease provide a player name to check.");
        return;
    }
    ChatLib.chat("&6[SBO] &eChecking Player: " + playerName);
    request({
        url: api + "/partyInfo?party=" + playerName,
        json: true
    }).then((response)=> {
        printCheckedPlayer(response.PartyInfo)
    }).catch((error)=> {
        console.error(error);
        ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking party member: " + playerName); 
    });
}

register("command", (args1, ...args) => {
    checkPlayer(args1);
}).setName("sbocheck").setAliases("sboc");;

register("chat", (player) => {
    setTimeout(() => {
        // send clickable message to execute command
        player = player.removeFormatting()
        player = getplayername(player)
        new TextComponent("&6[SBO] &eClick to check player").setClick("run_command", "/sbocheck " + player).setHover("show_text", "/sbocheck " + player).chat();
    }, 50);
}).setCriteria("&dFrom ${player}&r&7: &r&d&lBoop!&r");


// register("command", () => {
//     const playernames = TabList.getNames();
//     for (let i = 0; i < playernames.length; i++) {
//         print(playernames[i]);
//     }
// }).setName("sbogetplayers");

// ).setTabCompletions((args) => {
//     return TabList.getNames();
// }

// old code 

// old party checker
// register("command", () => {
//     if (Date.now() - lastUsed > 60000 || lastUsed == 0) { // 1 minutes
//         lastUsed = Date.now();
//         try {
//             ChatLib.chat("&6[SBO] &eChecking party members...");
//             let party = getPartyMembers();
//             if (party.length == 0) {
//                 ChatLib.chat("&6[SBO] &eNo party members found. try /pl and /sbocheckp again if your in a party.");
//                 return;
//             }
//             getPartyInfo(party);
//         } catch (error) {
//             ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking party members. Please try /pl and /sbocheckp again.");
//             console.error(error);
//         }
//     }
//     else {
//         ChatLib.chat("&6[SBO] &ePlease wait 1 minutes before checking party members again.");
//     }
// }).setName("sbocheckp");

/// partyfinde test code
// function queueAsPlayer() {
//     // Queue as a player
//     let success = false;
//     request({
//         url: api + "/addPlayerToQueue?PlayerName=" + Player.getName(),
//         json: true
//     }).then((response)=>{
//         success = response.Success;
//         if (success) {
//             ChatLib.chat("Successfully queued as " + Player.getName());
//         }
//         else {
//             ChatLib.chat("Failed to queue as " + Player.getName());
//         }    
//     }).catch((error)=>{
//         console.error(error);
//     });
// }

// function removeFromQueue() {
//     // Remove player from queue
//     let success = false;
//     request({
//         url: api + "/removePlayerFromQueue?PlayerName=" + Player.getName(),
//         json: true
//     }).then((response)=>{
//         success = response.Success;
//         if (success) {
//             ChatLib.chat("Successfully removed " + Player.getName() + " from queue");
//         }
//         else {
//             ChatLib.chat("Failed to remove " + Player.getName() + " from queue");
//         }
//         getQueuePlayer();
//     }).catch((error)=>{
//         console.error(error);
//     });
// }
    
// let queue = [];
// function getQueuePlayer() {
//     // Get the queue
//     request({
//         url: api + "/queuePlayer",
//         json: true
//     }).then((response)=>{
//         queue = response.Queue;
//         ChatLib.chat("Queue: ");
//         if (queue.length == 0) {
//             ChatLib.chat("Empty");
//         }
//         for (let i = 0; i < queue.length; i++) {
//             ChatLib.chat("name: " + queue[i].name + " sb lvl: " + queue[i].sbLvl + " eman9 " + queue[i].eman9 + " kills: " + queue[i].mythosKills + " legi pet: " + queue[i].legPet);
//         }
//     }).catch((error)=>{
//         console.error(error);
//     });
// }

// register("command", () => {
//     queueAsPlayer();
// }).setName("sboqueue");

// // each player in queue has a name, rank, sbLvl, eman9, mythosKills, legPet
// register("command", () => {
//     getQueuePlayer();
// }).setName("sbopf");
