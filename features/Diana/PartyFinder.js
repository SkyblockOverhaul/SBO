import { request } from "../../../requestV2";

// let api = "https://api.skyblockoverhaul.com";
let api = "http://127.0.0.1:8000";

function getPartyInfo(party) {
    print(party)
    request({
        url: api + "/partyInfo?party=" + party,
        json: true
    }).then((response)=> {
        print("test")
        printPartyInfo(response.PartyInfo)
        print("test2")
    }).catch((error)=> {
        console.error(error);
    });
}

function queueAsPlayer() {
    // Queue as a player
    let success = false;
    request({
        url: api + "/addPlayerToQueue?PlayerName=" + Player.getName(),
        json: true
    }).then((response)=>{
        success = response.Success;
        if (success) {
            ChatLib.chat("Successfully queued as " + Player.getName());
        }
        else {
            ChatLib.chat("Failed to queue as " + Player.getName());
        }    
    }).catch((error)=>{
        console.error(error);
    });
}

function removeFromQueue() {
    // Remove player from queue
    let success = false;
    request({
        url: api + "/removePlayerFromQueue?PlayerName=" + Player.getName(),
        json: true
    }).then((response)=>{
        success = response.Success;
        if (success) {
            ChatLib.chat("Successfully removed " + Player.getName() + " from queue");
        }
        else {
            ChatLib.chat("Failed to remove " + Player.getName() + " from queue");
        }
        getQueuePlayer();
    }).catch((error)=>{
        console.error(error);
    });
}
    
let queue = [];
function getQueuePlayer() {
    // Get the queue
    request({
        url: api + "/queuePlayer",
        json: true
    }).then((response)=>{
        queue = response.Queue;
        ChatLib.chat("Queue: ");
        if (queue.length == 0) {
            ChatLib.chat("Empty");
        }
        for (let i = 0; i < queue.length; i++) {
            ChatLib.chat("name: " + queue[i].name + " sb lvl: " + queue[i].sbLvl + " eman9 " + queue[i].eman9 + " kills: " + queue[i].mythosKills + " legi pet: " + queue[i].legPet);
        }
    }).catch((error)=>{
        console.error(error);
    });
}

register("command", () => {
    queueAsPlayer();
}).setName("sboqueue");

// each player in queue has a name, rank, sbLvl, eman9, mythosKills, legPet
register("command", () => {
    getQueuePlayer();
}).setName("sbopf");

register("chat", (party) => {
    removeFromQueue();
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

register("chat", (party) => {
    party = party.removeFormatting().replaceAll("'s", "");
    ChatLib.chat("party: " + party);
    party = party.replace(/\[.*?\]/g, '').replaceAll(" ", "")
    partyinfo = getPartyInfo(party);
    for (let i = 0; i < partyinfo.length; i++) {
        ChatLib.chat("name: " + partyinfo[i].name + " sb lvl: " + partyinfo[i].sbLvl + " eman9 " + partyinfo[i].eman9 + " kills: " + partyinfo[i].mythosKills + " legi pet: " + partyinfo[i].legPet);
    }
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

register("chat", (party) => {
    party = party.removeFormatting()
    ChatLib.chat("party: " + party);
    party = party.replace(/\[.*?\]/g, '').replaceAll(" ", "")
    partyinfo = getPartyInfo(party);
    for (let i = 0; i < partyinfo.length; i++) {
        ChatLib.chat("name: " + partyinfo[i].name + " sb lvl: " + partyinfo[i].sbLvl + " eman9 " + partyinfo[i].eman9 + " kills: " + partyinfo[i].mythosKills + " legi pet: " + partyinfo[i].legPet);
    }
}).setCriteria("&eYou'll be partying with: ${party}");

// &eYou'll be partying with: &r&b[MVP&r&c+&r&b] vxnp&r&e, &r&b[MVP&r&0+&r&b] saltyarcher&r&e, &r&6[MVP&r&2++&r&6] Boi_&r&e, &r&b[MVP&r&2+&r&b] rigis&r
// You have joined [MVP++] Tricksyz's party!

register("command", () => {
    party = "&r&b[MVP&r&c+&r&b] vxnp&r&e, &r&b[MVP&r&0+&r&b] saltyarcher&r&e, &r&6[MVP&r&2++&r&6] Boi_&r&e, &r&b[MVP&r&2+&r&b] rigis&r";
    party = party.removeFormatting();
    party = party.replace(/\[.*?\]/g, '').replaceAll(" ", "")
    getPartyInfo(party);
}).setName("sbopfparty");

function printPartyInfo(partyinfo) {
    for (let i = 0; i < partyinfo.length; i++) {
        ChatLib.chat("name: " + partyinfo[i].name + " sb lvl: " + partyinfo[i].sbLvl + " eman9 " + partyinfo[i].eman9 + " kills: " + partyinfo[i].mythosKills + " legi pet: " + partyinfo[i].legPet);
    }
}