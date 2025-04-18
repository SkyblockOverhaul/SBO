import settings from "../../settings";
import { setTimeout, checkSendInqMsg, getParty } from "../../utils/functions";
import socket from "../../../SBOSOCKET";
import { data, registerWhen } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";

let playerIsMuted = false;
let sendPing = false;
function sendInqPingSocket() {
    let party = getParty();
    if (party.length == 0) return;
    let playerUuid = Player.getUUID();
    party = party.filter(uuid => uuid !== playerUuid);
    sendPing = false;
    let serverId = TabList.getNames().find(tab => tab.includes("Server:")).split("Server: ")[1].split(" ")[0].removeFormatting();
    displayName = Player.getDisplayName().getText()
    lvlString = displayName.split("[")[1].split("]")[0];
    socket.send("inqPing", { 
        rankColor: displayName.split(" ")[1].substring(0, 2),
        lvlColor: lvlString.substring(0, 2),
        lvl: lvlString.removeFormatting(),
        uuids: party,
        coords: {
            x: Math.round(Player.getX()),
            y: Math.round(Player.getY()),
            z: Math.round(Player.getZ())
        },
        server: serverId,
    });
    ChatLib.chat("&6[SBO] &eInquisitor Ping Sent per Server!");
}

registerWhen(register("chat", (trash, expire) => {
    if (!checkDiana()) return;
    if (playerIsMuted) return;
    playerIsMuted = true;
    if (sendPing) sendInqPingSocket();
}).setCriteria("${trash}&7Your mute will expire ${expire}"), settings.inquisDetect);

export function onInqSpawn() {
    let since = data.mobsSinceInq;
    if (settings.inquisDetectCopy) {
        ChatLib.command("ct copy x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()), true);
    }

    if (settings.inquisDetect) {
        if (!playerIsMuted) {
            ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
            sendPing = true;
        } else {
            sendInqPingSocket();
        }
    }

    if (settings.announceKilltext !== "" && !playerIsMuted) {
        setTimeout(function () {
            let [send, text] = checkSendInqMsg(since);
            if (send) {
                ChatLib.command("pc " + text);
            }
        }, 5000);
    }
}