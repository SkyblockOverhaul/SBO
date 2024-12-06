import { request } from "../../../requestV2";
import { formatNumberCommas, getplayername, sendPartyRequest, toTitleCase, getRarity, getNumberColor, getGriffinItemColor, matchLvlToColor, getDianaStats, setTimeout } from "../../utils/functions";
import { HypixelModAPI } from "./../../../HypixelModAPI";
import { checkDiana } from "../../utils/checkDiana";
import { trackWithCheckPlayer } from "./DianaAchievements";
import settings from "../../settings";
import { data } from "../../utils/variables";

let api = "https://api.skyblockoverhaul.com";

let firstTimeStamp = 0;
function getPartyInfo(party) {
    firstTimeStamp = Date.now();
    request({
        url: api + "/partyInfoByUuids?uuids=" + party.join(",").replaceAll("-", ""),
        json: true
    }).then((response)=> {
        if (response.Success) {
            let timeTaken = Date.now() - firstTimeStamp;
            ChatLib.chat("&6[SBO] &eParty members checked in " + timeTaken + "ms");
            printPartyInfo(response.PartyInfo)
        }
        else {
            ChatLib.chat("&6[SBO] &4Error: " + response.Error);
        }
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
    (playerinfo.invApi ? (playerinfo.looting5daxe ? "&a✓" : "&4✗") : "&4?") + 
    "&r&9│ &r&eKills&r&f: &r&6" + 
    (playerinfo.mythosKills / 1000).toFixed(2) + "k")
    .setClick("run_command", "/pv " + playerinfo.name).setHover("show_text", "/pv " + playerinfo.name).chat();

    if (playerinfo.name == Player.getName()) {
        trackWithCheckPlayer(playerinfo);
        new TextComponent("&7[&3Extra Stats&7]").setClick("run_command", "/extrastatsbuttonforsbo").setHover("show_text", "Click for more details").chat();
        data.dianaStats = playerinfo;
        data.save();
    }
    else {
        new Message(
            new TextComponent("&7[&3Extra Stats&7]").setClick("run_command", "/extrastatsbuttonforsbo").setHover("show_text", "Click for more details"),
            new TextComponent(" &7[&eClick To invite&7]").setClick("run_command", "/p invite " + playerinfo.name).setHover("show_text", "/p " + playerinfo.name),
        ).chat();
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
        if (response.Success) {
            printCheckedPlayer(response.PartyInfo)
        } else {
            ChatLib.chat("&6[SBO] &4Error: " + response.Error);
        }
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
let inQueue = false;
let partyReqs = ""
let requeue = false;
export function createParty(reqs) {
    if (!creatingParty) {
        partyReqs = reqs
        requeue = false;
        creatingParty = true;
        sendPartyRequest();
        createPartyTimeStamp = Date.now();
    } else {
        ChatLib.chat("&6[SBO] &eYou are already in queue.");
    }
}
// "http://127.0.0.1:8000/createParty?uuids=" + party.join(",").replaceAll("-", ""),
export function getInQueue() {
    return inQueue;
}

function checkIfPlayerMeetsReqs(player, reqs) {
    if (reqs.lvl && player.sbLvl < reqs.lvl) {
        return false;
    }
    if (reqs.eman9 && !player.eman9) {
        return false;
    }
    if (reqs.kills && player.mythosKills < reqs.kills) {
        return false;
    }
    if (reqs.looting5 && !player.looting5daxe) {
        return false;
    }
    return true;
}

let playersSendRequest = [];
export function sendJoinRequest(partyLeader, partyReqs) {
    let playerInfo = getDianaStats();
    if (checkIfPlayerMeetsReqs(playerInfo, partyReqs)) {
        let generatedUUID = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
        ChatLib.chat("&6[SBO] &eSending join request to " + partyLeader);
        ChatLib.command("msg " + partyLeader + " [SBO] join party request - id:" + generatedUUID + " - " + generatedUUID.length)
        playersSendRequest.push(partyLeader.toLowerCase().trim());
    } else {
        ChatLib.chat("&6[SBO] &eYou don't meet the requirements to join this party");
    }
}

register("chat", (player) => {
    player = getplayername(player).toLowerCase().trim();
    if (playersSendRequest.includes(player)) {
        ChatLib.chat("&6[SBO] &eJoining party: " + player);
        playersSendRequest = [];
        ChatLib.command("p accept " + player);
    }
}).setCriteria("${player} &r&ehas invited you to join their party!").setContains();

let ghostParty = false;
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
    } else if (creatingParty) {
        ghostParty = true;
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
            }).then((response)=> {
                if (!response.Success) {
                    ChatLib.chat("&6[SBO] &4" + response.Error);
                    inQueue = false;
                    inParty = false;
                }
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

const partyDisbanded = [
    /^.+ &r&ehas disbanded the party!&r$/,
    /^&cThe party was disbanded because (.+)$/,
    /^&eYou left the party.&r$/,
    /^&cYou are not currently in a party.&r$/,
    /^&eYou have been kicked from the party by .+$/
] 
const leaderMessages = [
    /^&eYou have joined &r(.+)'s* &r&eparty!&r$/,
    /^&eThe party was transferred to &r(.+) &r&eby &r.+&r$/,
    /^(.+) &r&e has promoted &r(.+) &r&eto Party Leader&r$/
]
const memberJoined = [
    /^(.+) &r&ejoined the party.&r$/,
    /^&eYou have joined &r(.+)'[s]? &r&eparty!&r$/
]
const memberLeft = [
    /^(.+) &r&ehas been removed from the party.&r$/,
    /^(.+) &r&ehas left the party.&r$/,
    /^(.+) &r&ewas removed from your party because they disconnected.&r$/,
    /^&eKicked (.+) because they were offline.&r$/
] 
let inParty = false;
export function isInParty() {
    return inParty;
}

let partyCount = 0;
function trackMemberCount(number) {
    partyCount = partyCount + number;
    if (inQueue) {
        if (partyCount >= 6) {      
            setTimeout(() => {
                ChatLib.chat("&6[SBO] &eYour party is full and removed from the queue.");
                removePartyFromQueue();
            }, 150);
        }
    }
    else if (checkDiana()) {
        if (partyCount < 6 && partyReqs != "" && !requeue) {
            requeue = true;
            setTimeout(() => {
                new TextComponent("&6[SBO] &eClick to requeue party with your last used requirements").setClick("run_command", "/sboqueue").setHover("show_text", "/sboqueue").chat();
            }, 150);
        }
    }
}

register("command", () => {
    ChatLib.chat("&6[SBO] &eRequeuing party with last used requirements...");
    createParty(partyReqs);
}).setName("sboqueue");


register("chat", (event) => {
    let formatted = ChatLib.getChatMessage(event, true)
    leaderMessages.forEach(regex => {
        let match = formatted.match(regex)
        if (match) {
            removePartyFromQueue()
        }
    })
    partyDisbanded.forEach(regex => {
        let match = formatted.match(regex)
        if (match) {
            removePartyFromQueue()
            partyCount = 0;
            inParty = false;
        }
    })
    memberJoined.forEach(regex => {
        let match = formatted.match(regex)
        if (match) {
            updatePartyInQueue()
            trackMemberCount(1);
            inParty = true;
        }
    })
    memberLeft.forEach(regex => {
        let match = formatted.match(regex)
        if (match) {
            updatePartyInQueue()
            trackMemberCount(-1);
            inParty = true;
        }
    })
})


register("chat", (toFrom, player, id, event) => {
    if (inQueue && toFrom.includes("From")) {
        // join request message
        if (partyCount < 6) {
            player = getplayername(player);
            if (settings.autoInvite) {
                ChatLib.chat("&6[SBO] &eSending invite to " + player);
                ChatLib.command("p invite " + player);
            } else {
                ChatLib.chat(ChatLib.getChatBreak("&b-"))
                new Message(
                    new TextComponent(`&6[SBO] &b${player} &ewants to join your party.\n`),
                    new TextComponent(`&7[&aAccept&7]`).setClick("run_command", "/p invite " + player).setHover("show_text", "&a/p invite " + player),
                    new TextComponent(` &7[&eCheck Player&7]`).setClick("run_command", "/sbocheck " + player).setHover("show_text", "&eCheck " + player)
                ).chat();
                ChatLib.chat(ChatLib.getChatBreak("&b-"))
            }
        }
    }
    cancel(event);
}).setCriteria("&d${toFrom} ${player}&r&7: &r&7[SBO] join party request - ${id}");

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
    partyCount = party.length;
    if (creatingParty) {
        if (party[0] != Player.getUUID() && party.length > 1) {
            ChatLib.chat("&6[SBO] &eYou are not the party leader. Only party leader can queue with the party.");
            creatingParty = false;
            return;
        }
        if (party.length > 5) {
            ChatLib.chat("&6[SBO] &eParty members limit reached. You can only queue with up to 5 members.");
            creatingParty = false;
            return;
        }
        request({
            url: api + "/createParty?uuids=" + party.join(",").replaceAll("-", "") + "&reqs=" + partyReqs,
            json: true
        }).then((response)=> {
            if (response.Success) {
                let timeTaken = Date.now() - createPartyTimeStamp;
                ChatLib.chat("&6[SBO] &eParty created successfully in " + timeTaken + "ms \n&6[SBO] &eRefresh to see the party in the list");
                inQueue = true; 
                creatingParty = false;
                if (ghostParty) {
                    removePartyFromQueue();
                    ghostParty = false;
                }
                if (inParty) ChatLib.command("pc [SBO] Party now in queue.");
            } else {
                ChatLib.chat("&6[SBO] &4Error: " + response.Error);
                inQueue = false;
                creatingParty = false;
            }
        }).catch((error)=> {
            inQueue = false;
            creatingParty = false;
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
        let updatePartyTimeStamp = Date.now();
        if (party.length > 5 || party.length < 2) return;
        ChatLib.chat("&6[SBO] &eUpdating party members in queue...");
        request({
            url: api + "/queuePartyUpdate?uuids=" + party.join(",").replaceAll("-", "") + "&reqs=" + partyReqs,
            json: true
        }).then((response)=> {
            if (response.Success) {
                let timeTaken = Date.now() - updatePartyTimeStamp;
                ChatLib.chat("&6[SBO] &eParty in queue updated successfully " + timeTaken + "ms");
            } else {
                ChatLib.chat("&6[SBO] &4Error: " + response.Error);
            }
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