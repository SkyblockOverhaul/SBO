import { request } from "../../../requestV2";
import { getPartyBool, getPartyMembers, getplayername, setInterval, clearInterval, sendPartyRequest, getPartyMembersUuids } from "../../utils/functions";

let api = "https://api.skyblockoverhaul.com";

function getPartyInfo(party) {
    // // remove empty strings from partylist (idk why this works but it does)
    // party = party.filter(Boolean);
    // // remove user from partylist
    // party = party.filter((name) => name != Player.getName());
    // // remove duplicates 
    // party = [...new Set(party)].join(",");
    // // if last char of party is a comma remove it
    // if (party.charAt(party.length - 1) == ",") {
    //     party = party.slice(0, -1);
    // }
    let playerName = Player.getName();
    // Filter out empty strings, the current user, and duplicates, then join into a string
    party = [...new Set(party.filter(name => name && name != playerName))].join(",").replaceAll(" ", "").replaceAll(",,", ",");
    if (party.charAt(party.length - 1) == ",") {
        party = party.slice(0, -1);
    }
    
    // send for each player one reqeust to the api and only send the next request if the previous one is finished
    let promises = [];
    for (let i = 0; i < party.length; i++) {
        promises.push(request({
            url: api + "/partyInfo?party=" + party[i],
            json: true
        }));
    }
    Promise.all(promises).then((responses)=> {
        for (let i = 0; i < responses.length; i++) {
            printPartyInfo(responses[i].PartyInfo);
        }
    }).catch((error)=> {
        console.error(error);
        ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking party member: " + party[i]); 
    });
}

function getPartyInfoByUuids(party) {
    request({
        url: api + "/partyInfoByUuids?party=" + party.join(","),
        json: true
    }).then((response)=> {
        printPartyInfo(response.PartyInfo)
    }).catch((error)=> {
        console.error(error);
    });
}

// old for the complete party at once
// request({
//     url: api + "/partyInfo?party=" + party,
//     json: true
// }).then((response)=> {
//     printPartyInfo(response.PartyInfo)
// }).catch((error)=> {
//     console.error(error);
// });

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

register("chat", (party) => {
    setTimeout(() => {
        // send clickable message to execute command
        new TextComponent("&6[SBO] &eClick to check party members").setClick("run_command", "/sbocheckp").setHover("show_text", "/sbocheckp").chat();
    }, 100);
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

// &eYou'll be partying with: &r&b[MVP&r&c+&r&b] vxnp&r&e, &r&b[MVP&r&0+&r&b] saltyarcher&r&e, &r&6[MVP&r&2++&r&6] Boi_&r&e, &r&b[MVP&r&2+&r&b] rigis&r
// You have joined [MVP++] Tricksyz's party!

let lastUsed = 0;
register("command", () => {
    if (Date.now() - lastUsed > 60000 || lastUsed == 0) { // 1 minutes
        lastUsed = Date.now();
        try {
            ChatLib.chat("&6[SBO] &eChecking party members...");
            let party = getPartyMembers();
            if (party.length == 0) {
                ChatLib.chat("&6[SBO] &eNo party members found. try /pl and /sbocheckp again if your in a party.");
                return;
            }
            getPartyInfo(party);
        } catch (error) {
            ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking party members. Please try /pl and /sbocheckp again.");
            console.error(error);
        }
    }
    else {
        ChatLib.chat("&6[SBO] &ePlease wait 1 minutes before checking party members again.");
    }
}).setName("sbocheckp");

register("command", () => {
    // loop until partyBool is true with a delay of 10ms
    ChatLib.chat("&6[SBO] &eChecking party members...");
    sendPartyRequest();
    let interval = setInterval(() => {
        partyBool = getPartyBool();
        print(partyBool);
        if (partyBool) {
            let party = getPartyMembersUuids();
            if (party.length == 0) {
                ChatLib.chat("&6[SBO] &eNo party members found. try join a party");
                return;
            }
            print("send api request to get party info by uuids")
            getPartyInfoByUuids(party);
            clearInterval(interval);
        }
    }, 100);
}).setName("sbocheckpuuid");

function printPartyInfo(partyinfo) {
    for (let i = 0; i < partyinfo.length; i++) {
        // if (partyinfo[i].legPet) { // to remove all player without legendary griffin pet
            ChatLib.chat("&6[SBO] &eName&r&f: &r&b" + partyinfo[i].name + "&r&e&r&9│ &r&eLvL&r&f: &r&6" + partyinfo[i].sbLvl + "&r&e&r&9│ &r&eEman 9&r&f: &r&f" + (partyinfo[i].eman9 ? "&r&a✓" : "&4✗") + "&e&r&9│ &r&el5 Daxe&r&f: " + (partyinfo[i].looting5daxe ? "&a✓" : "&4✗") + "&e &r&9│ &r&eKills&r&f: &r&6" + (partyinfo[i].mythosKills / 1000).toFixed(2) + "k");
        // }
    }
}

function printCheckedPlayer(playerinfo) {
    // send clickable message to execute command to invite the checked player
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
}).setName("sbocheck");

register("chat", (player) => {
    setTimeout(() => {
        // send clickable message to execute command
        player = player.removeFormatting()
        player = getplayername(player)
        new TextComponent("&6[SBO] &eClick to check player").setClick("run_command", "/sbocheck " + player).setHover("show_text", "/sbocheck " + player).chat();
    }, 50);
}).setCriteria("&dFrom ${player}&r&7: &r&d&lBoop!&r");