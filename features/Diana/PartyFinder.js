import { request } from "../../../requestV2";
import { formatNumberCommas, getplayername, sendPartyRequest, toTitleCase, getRarity, getNumberColor, getGriffinItemColor } from "../../utils/functions";
import { HypixelModAPI } from "./../../../HypixelModAPI";
import { checkDiana } from "../../utils/checkDiana";

let api = "https://api.skyblockoverhaul.com";

function getPartyInfo(party) {
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
    if (checkDiana()) {
        setTimeout(() => {
            new TextComponent("&6[SBO] &eClick to check party members").setClick("run_command", "/sbocheckp").setHover("show_text", "/sbocheckp").chat();
        }, 100);
    }
}).setCriteria("&eYou have joined ${party} &r&eparty!&r");

// command to check party members
let checkPartyBool = false;
const partyLimit = 7;
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
    if (count >= partyLimit) {
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
        ChatLib.chat("&6[SBO] &eName&r&f: &r&b" + partyinfo[i].name + "&r&9│ &r&eLvL&r&f: &r&6" + partyinfo[i].sbLvl + "&r&9│ &r&eEman 9&r&f: &r&f" + (partyinfo[i].eman9 ? "&r&a✓" : "&4✗") + "&r&9│ &r&el5 Daxe&r&f: " + (partyinfo[i].looting5daxe ? "&a✓" : "&4✗") + "&r&9│ &r&eKills&r&f: &r&6" + (partyinfo[i].mythosKills / 1000).toFixed(2) + "k");
    }
}

/// Player Checker
let lastCheckedPlayer = undefined;
function printCheckedPlayer(playerinfo) {
    playerinfo = playerinfo[0];
    lastCheckedPlayer = playerinfo;
    new TextComponent("&6[SBO] &eName&r&f: &r&b" + playerinfo.name + 
    "&r&9│ &r&eLvL&r&f: &r&6" + playerinfo.sbLvl + 
    "&r&9│ &r&eEman 9&r&f: &r&f" + (playerinfo.eman9 ? "&r&a✓" : "&4✗") + "&r&9│ &r&el5 Daxe&r&f: " + 
    (playerinfo.looting5daxe ? "&a✓" : "&4✗") + 
    "&r&9│ &r&eKills&r&f: &r&6" + 
    (playerinfo.mythosKills / 1000).toFixed(2) + "k")
    .setClick("run_command", "/p " + playerinfo.name).setHover("show_text", "/p " + playerinfo.name).chat();
    new TextComponent("&7[&3Extra Stats&7]").setClick("run_command", "/extrastatsbuttonforsbo").setHover("show_text", "Click for more details").chat();
}

register("command", () => {
    let messageString = "&6[SBO] &eExtra Stats: " + lastCheckedPlayer.name;

    if (lastCheckedPlayer.daxeLootingLvl != -1) {
        messageString += "\n&3Daedalus Axe&7: " +
            "\n&7- &9Chimera&7: " + getNumberColor(lastCheckedPlayer.daxeChimLvl, 5) +
            "\n&7- &9Looting&7: " + getNumberColor(lastCheckedPlayer.daxeLootingLvl, 5);
    } else {
        messageString += "\n&3Daedalus Axe&7: &4✗";
    }

    if (lastCheckedPlayer.griffinRarity != -1) {
        messageString += "\n&3Griffin&7: " +
            "\n&7- &9Rarity&7: " + getRarity(lastCheckedPlayer.griffinRarity) +
            "\n&7- &9Item&7: " + getGriffinItemColor(lastCheckedPlayer.griffinItem);
    } else {
        messageString += "\nGriffin&7: &4✗";
    }
    
    messageString += "\n&3Misc&7: " +
        "\n&7- &9Enderman Slayer&7: " + getNumberColor(lastCheckedPlayer.emanLvl, 9) +
        "\n&7- &9Diana Kills&7: " + formatNumberCommas(lastCheckedPlayer.mythosKills);

    if (lastCheckedPlayer.clover) {
        messageString += "\n&7- &9Clover&7: &a✓";
    }

    messageString += "\n&7- &9Inventory API&7: " + (lastCheckedPlayer.invApi ? "&aOn" : "&4Off");
    ChatLib.chat(messageString);
}).setName("extrastatsbuttonforsbo");

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
        player = player.removeFormatting()
        player = getplayername(player)
        new TextComponent("&6[SBO] &eClick to check player").setClick("run_command", "/sbocheck " + player).setHover("show_text", "/sbocheck " + player).chat();
    }, 50);
}).setCriteria("&dFrom ${player}&r&7: &r&d&lBoop!&r");
