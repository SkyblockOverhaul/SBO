import { request } from "../../../requestV2";
import { formatNumberCommas, getplayername, sendPartyRequest, toTitleCase, getRarity, getNumberColor, getGriffinItemColor, matchLvlToColor } from "../../utils/functions";
import { HypixelModAPI } from "./../../../HypixelModAPI";
import { checkDiana } from "../../utils/checkDiana";
import { trackWithCheckPlayer } from "./DianaAchievements";

let api = "https://api.skyblockoverhaul.com";

let firstTimeStamp = 0;
function getPartyInfo(party) {
    firstTimeStamp = Date.now();
    request({
        url: api + "/partyInfoByUuids?uuids=" + party.join(",").replaceAll("-", ""),
        json: true
    }).then((response)=> {
        let timeTaken = Date.now() - firstTimeStamp;
        ChatLib.chat("&6[SBO] &eParty members checked in " + timeTaken + "ms");
        printPartyInfo(response.PartyInfo)
    }).catch((error)=> {
        if (error.detail) {
            ChatLib.chat("&6[SBO] &4Error: " + error.detail);
        } else {
            console.error(JSON.stringify(error));
            ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking party members");
        }
    });
}

// message to check party when joining a party
register("chat", (party) => {
    if (checkDiana(true)) {
        setTimeout(() => {
            new TextComponent("&6[SBO] &eClick to check party members").setClick("run_command", "/sbocheckp").setHover("show_text", "/sbocheckp").chat();
        }, 100);
    }
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

// command to check party members
let checkPartyBool = false;
const partyLimit = 6;
HypixelModAPI.on("partyInfo", (partyInfo) => {
    if (!checkPartyBool) return;
    checkPartyBool = false;
    let party = [];
    let count = 0;
    Object.keys(partyInfo).forEach(key => {
        if (key == Player.getUUID()) return;
        count++;
        if (count >= partyLimit+1) return;
        party.push(key);
    })
    if (count >= partyLimit+1) {
        ChatLib.chat("&6[SBO] &eParty members limit reached. Only checking first " + partyLimit + " members.");
    }
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
    }
    else {
        ChatLib.chat("&6[SBO] &ePlease wait 1 minutes before checking party members again.");
    }
}).setName("sbocheckp").setAliases("sbocp");

function printPartyInfo(partyinfo) {
    for (let i = 0; i < partyinfo.length; i++) {
        ChatLib.chat("&6[SBO] &eName&r&f: &r&b" + partyinfo[i].name + "&r&9│ &r&eLvL&r&f: &r&6" + matchLvlToColor(partyinfo[i].sbLvl) + "&r&9│ &r&eEman 9&r&f: &r&f" + (partyinfo[i].eman9 ? "&r&a✓" : "&4✗") + "&r&9│ &r&el5 Daxe&r&f: " + (partyinfo[i].looting5daxe ? "&a✓" : "&4✗") + "&r&9│ &r&eKills&r&f: &r&6" + (partyinfo[i].mythosKills / 1000).toFixed(2) + "k");
    }
}

/// Player Checker
let lastCheckedPlayer = undefined;
function printCheckedPlayer(playerinfo) {
    playerinfo = playerinfo[0];
    lastCheckedPlayer = playerinfo;
    new TextComponent("&6[SBO] &eName&r&f: &r&b" + playerinfo.name + 
    "&r&9│ &r&eLvL&r&f: &r&6" + matchLvlToColor(playerinfo.sbLvl) + 
    "&r&9│ &r&eEman 9&r&f: &r&f" + (playerinfo.eman9 ? "&r&a✓" : "&4✗") + "&r&9│ &r&el5 Daxe&r&f: " + 
    (playerinfo.looting5daxe ? "&a✓" : "&4✗") + 
    "&r&9│ &r&eKills&r&f: &r&6" + 
    (playerinfo.mythosKills / 1000).toFixed(2) + "k")
    .setClick("run_command", "/p " + playerinfo.name).setHover("show_text", "/p " + playerinfo.name).chat();
    new TextComponent("&7[&3Extra Stats&7]").setClick("run_command", "/extrastatsbuttonforsbo").setHover("show_text", "Click for more details").chat();
    if (playerinfo.name == Player.getName()) {
        trackWithCheckPlayer(playerinfo);
    }
}

let messageString = "";
register("command", () => {
    if (lastCheckedPlayer) {
        messageString = "";
        messageString = "&6[SBO] &eExtra Stats: " + lastCheckedPlayer.name;

        if (lastCheckedPlayer.daxeLootingLvl != -1) {
            messageString += "\n&3Daedalus Axe&7: " +
                "\n&7- &9Chimera&7: " + getNumberColor(lastCheckedPlayer.daxeChimLvl, 5) +
                "\n&7- &9Looting&7: " + getNumberColor(lastCheckedPlayer.daxeLootingLvl, 5);
        } else {
            messageString += "\n&3Daedalus Axe&7: &4✗";
        }
        if (lastCheckedPlayer.griffinRarity != "none") {
            messageString += "\n&3Griffin&7: " +
                "\n&7- &9Rarity&7: " + getRarity(lastCheckedPlayer.griffinRarity) +
                "\n&7- &9Item&7: " + getGriffinItemColor(lastCheckedPlayer.griffinItem);
        } else {
            messageString += "\n&3Griffin&7: &4✗";
        }

        if (lastCheckedPlayer.invApi) {
            messageString += "\n&3Talis&7: " +
                "\n&7- &9Magical Power&7: &b" + formatNumberCommas(lastCheckedPlayer.magicalPower) +
                "\n&7- &9Enrichments&7: &b" + lastCheckedPlayer.enrichments;
        } else {
            messageString += "\n&3Talis&7: " +
                "\n&7- &9Magical Power&7: ~" + formatNumberCommas(lastCheckedPlayer.magicalPower);
        }
        
        if (lastCheckedPlayer.missingEnrichment > 0)
            messageString += "\n&7- &9Missing Enrichments&7: " + lastCheckedPlayer.missingEnrichments;
        
        messageString += "\n&3Misc&7: " +
            "\n&7- &9Enderman Slayer&7: " + getNumberColor(lastCheckedPlayer.emanLvl, 9) +
            "\n&7- &9Diana Kills&7: &b" + formatNumberCommas(lastCheckedPlayer.mythosKills) + (lastCheckedPlayer.killLeaderboard <= 100 ? " &7(#" + lastCheckedPlayer.killLeaderboard + ")" : "");

        if (lastCheckedPlayer.clover) 
            messageString += "\n&7- &9Clover&7: &a✓";
        

        messageString += "\n&7- &9Inventory API&7: " + (lastCheckedPlayer.invApi ? "&aOn" : "&4Off");
        let messageParts = messageString.split("\n");
        for (let i = 0; i < messageParts.length; i++) {
            ChatLib.chat(messageParts[i]);
        }
        new TextComponent("&7[&3Copy All&7]").setClick("run_command", "/buttonforsbotocopystats").setHover("show_text", "Click to copy").chat();
    }
    else {
        ChatLib.chat("&6[SBO] &4Invalid button. Please check a player first. /sbocheck <player>");
    }
}).setName("extrastatsbuttonforsbo");

register("command", () => {
    ChatLib.command("ct copy " + messageString.removeFormatting(), true);
    ChatLib.chat("&6[SBO] &aCopied to Clipboard");
}).setName("buttonforsbotocopystats");


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
        if (error.detail) {
            ChatLib.chat("&6[SBO] &4Error: " + error.detail);
        } else {
            console.error(JSON.stringify(error));
            ChatLib.chat("&6[SBO] &4Unexpected error occurred while checking player: " + playerName); 
        }
    });
}

register("command", (args1, ...args) => {
    if (args1 == undefined) args1 = Player.getName();
    checkPlayer(args1);
}).setName("sbocheck").setAliases("sboc");;

register("chat", (player) => {
    setTimeout(() => {
        player = player.removeFormatting()
        player = getplayername(player)
        new TextComponent("&6[SBO] &eClick to check player").setClick("run_command", "/sbocheck " + player).setHover("show_text", "/sbocheck " + player).chat();
    }, 50);
}).setCriteria("&dFrom ${player}&r&7: &r&d&lBoop!&r");

let creatingParty = false;
let updateBool = false;
let createPartyTimeStamp = 0;
export function createParty() {
    print("Creating Party");
    creatingParty = true;
    sendPartyRequest();
    createPartyTimeStamp = Date.now();
}
// "http://127.0.0.1:8000/createParty?uuids=" + party.join(",").replaceAll("-", ""),
export function getInQueue() {
    return inQueue;
}
let inQueue = false;
let partyList = [];
export function getAllParties(useCallback = false, callback = null) { 
    request({
        url: api + "/getAllParties",
        json: true
    }).then((response)=> {
        partyList = response.Parties;
        if (partyList.length == 0) {
            ChatLib.chat("&6[SBO] &eNo parties found. Try again later.");
        }
        if (useCallback && callback) {
            callback(partyList);
        }
    }).catch((error)=> {
        if (error.detail) {
            ChatLib.chat("&6[SBO] &4Error2: " + error.detail);
        } else {
            console.error(JSON.stringify(error));
            ChatLib.chat("&6[SBO] &4Unexpected error occurred while getting all parties");
        }
        return [];
    });
}

export function sendJoinRequest(partyLeader) {
    let generatedUUID = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    ChatLib.chat("&6[SBO] &eSending join request to " + partyLeader);
    ChatLib.command("msg " + partyLeader + " [SBO] join party request - id:" + generatedUUID + " - " + generatedUUID.length)
}

export function removePartyFromQueue(useCallback = false, callback = null) {
    if (inQueue) {
        inQueue = false;
        request({
            url: api + "/unqueueParty?leaderId=" + Player.getUUID().replaceAll("-", ""),
            json: true
        }).then((response)=> {
            ChatLib.chat("&6[SBO] &eYou have been removed from the queue");
            if (useCallback && callback) {
                callback(true);
            }
        }).catch((error)=> {
            if (error.detail) {
                ChatLib.chat("&6[SBO] &4Error3: " + error.detail);
            } else {
                console.error(JSON.stringify(error));
                ChatLib.chat("&6[SBO] &4Unexpected error occurred while removing party from queue");
            }
        });
    }
}

let requestSend = false;
function updatePartyInQueue() {
    if (inQueue) {
        updateBool = true;
        requestSend = false;
        setTimeout(() => {
            if (updateBool && !requestSend) { // because skytils sends request to mod api after every party member join/leave
                requestSend = true;
                sendPartyRequest();
            }
        }, 500);
    }
}

let lastUpdated = 0;
register("step", () => {
    if (inQueue) {
        // 4 minutes
        if (Date.now() - lastUpdated > 240000) {
            lastUpdated = Date.now();
            request({
                url: api + "/queueUpdate?leaderId=" + Player.getUUID().replaceAll("-", ""),
                json: true
            }).catch((error)=> {
                inQueue = false;
                if (error.detail) {
                    ChatLib.chat("&6[SBO] &4Error4: " + error.detail);
                } else {
                    console.error(JSON.stringify(error));
                    ChatLib.chat("&6[SBO] &4Unexpected error occurred while updating queue");
                }
            });
        }
    }
}).setFps(1);

partyDisbanded = [ 
    /^.+ &r&ehas disbanded the party!&r$/,
    /^&cThe party was disbanded because all invites expired and the party was empty.&r$/,
    /^&eYou left the party.&r$/,
    /^You are not currently in a party\.$/,
    /^You have been kicked from the party by .+$/
]
leaderMessages = [
    /^&eYou have joined &r(.+)'s* &r&eparty!&r$/,
    /^&eThe party was transferred to &r(.+) &r&eby &r.+&r$/
]
memberJoined = [
    /^(.+) &r&ejoined the party.&r$/,
    /^&eYou have joined &r(.+)'[s]? &r&eparty!&r$/,
    /^&dParty Finder &r&f> &r(.+) &r&ejoined the dungeon group! \(&r&b.+&r&e\)&r$/
]
memberLeft = [
    /^(.+) &r&ehas been removed from the party.&r$/,
    /^(.+) &r&ehas left the party.&r$/,
    /^(.+) was removed from your party because they disconnected.$/,
    /^Kicked (.+) because they were offline.$/
]
register("chat", (event) => {
    if (inQueue) {
        let formatted = ChatLib.getChatMessage(event, true)
        leaderMessages.forEach(regex => {
            let match = formatted.match(regex)
            if (match) removePartyFromQueue()
        })
        partyDisbanded.forEach(regex => {
            let match = formatted.match(regex)
            if (match) removePartyFromQueue()
        })
        memberJoined.forEach(regex => {
            let match = formatted.match(regex)
            if (match) updatePartyInQueue()
        })
        memberLeft.forEach(regex => {
            let match = formatted.match(regex)
            if (match) updatePartyInQueue()
        })
    }
})

register("chat", (player, id, event) => {
    cancel(event);
    if (inQueue) {
        // join request message
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
        new Message(
            new TextComponent(`&6[SBO] &b${player} &ewants to join your party.\n`),
            new TextComponent(`&7[&aAccept&7]`).setClick("run_command", "/p invite " + player).setHover("show_text", "&a/p invite " + player),
            new TextComponent(` &7[&eCheck Player&7]`).setClick("run_command", "/sbocheck " + player).setHover("show_text", "&eCheck " + player)
        ).chat();
        ChatLib.chat(ChatLib.getChatBreak("&b-"))
    }
}).setCriteria("&dFrom ${player}&r&7: &r&7[SBO] join party request - ${id}");

register("gameUnload", () => {
    if (inQueue) {
        removePartyFromQueue();
    }
})

register("serverDisconnect", () => {
    if (inQueue) {
        removePartyFromQueue();
    }
})

HypixelModAPI.on("partyInfo", (partyInfo) => {
    let party = [];
    Object.keys(partyInfo).forEach(key => {
        if (partyInfo[key] == "LEADER") {
            party.unshift(key);
        } else {
            party.push(key);
        }
    })
    if (party.length == 0) party.push(Player.getUUID());

    if (creatingParty) {
        creatingParty = false;
        if (party[0] != Player.getUUID() && party.length > 1) {
            ChatLib.chat("&6[SBO] &eYou are not the party leader. Only party leader can queue with the party.");
            return;
        }
        request({
            url: api + "/createParty?uuids=" + party.join(",").replaceAll("-", ""),
            json: true
        }).then((response)=> {
            let timeTaken = Date.now() - createPartyTimeStamp;
            ChatLib.chat("&6[SBO] &eParty created successfully in " + timeTaken + "ms \n&6[SBO] &eRefresh to see the party in the list");
            inQueue = true; 
        }).catch((error)=> {
            if (error.detail) {
                ChatLib.chat("&6[SBO] &4Error1: " + error.detail);
            } else {
                console.error(JSON.stringify(error));
                ChatLib.chat("&6[SBO] &4Unexpected error occurred while creating party");
            }
        });
    }
    if (updateBool && inQueue) {
        updateBool = false;
        ChatLib.chat("&6[SBO] &eUpdating party members in queue...");
        request({
            url: api + "/queuePartyUpdate?uuids=" + party.join(",").replaceAll("-", ""),
            json: true
        }).catch((error)=> {
            inQueue = false;
            if (error.detail) {
                ChatLib.chat("&6[SBO] &4Error4: " + error.detail);
            } else {
                console.error(JSON.stringify(error));
                ChatLib.chat("&6[SBO] &4Unexpected error occurred while updating queue");
            }
        });
    }
})