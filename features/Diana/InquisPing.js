import settings from "../../settings.js";
import { setTimeout, checkSendInqMsg, getParty } from "../../utils/functions";
import socket from "../../../SBOSOCKET";
import { data, registerWhen } from "../../utils/variables.js";
import { checkDiana } from "../../utils/checkDiana.js";

let playerIsMuted = false;
let sendPing = false;
function sendInqPingSocket() {
    let party = getParty();
    if (party.length == 0) return;
    sendPing = false;
    let serverId = TabList.getNames().find((line) => line.startsWith(' Server:'));
    print(serverId.substring(serverId.lastIndexOf(' ') + 1))
    socket.send("inqPing", { 
        owner: Player.getDisplayName().getText(),
        uuids: party,
        coords: {
            x: Math.round(Player.getX()),
            y: Math.round(Player.getY()),
            z: Math.round(Player.getZ())
        }
    });
    ChatLib.chat("&6[SBO] &eInquisitor Ping Sent per Server!");
}

registerWhen(register("chat", (expire) => {
    if (!checkDiana()) return;
    if (playerIsMuted) return;
    playerIsMuted = true;
    if (sendPing) sendInqPingSocket();
}).setCriteria("&r&7Your mute will expire ${expire}"), settings.inquisDetect);

export function onInqSpawn() {
    let since = data.mobsSinceInq;
    if (settings.inquisDetectCopy) {
        ChatLib.command("ct copy x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
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

register("command", () => {
    playerIsMuted = true;
    if (settings.inquisDetect) {
        if (!playerIsMuted) {
            ChatLib.command("pc x: " + Math.round(Player.getLastX()) + ", " + "y: " + Math.round(Player.getLastY()) + ", " + "z: " + Math.round(Player.getLastZ()));
            sendPing = true;
        } else {
            sendInqPingSocket();
        }
    }
}).setName("sbotestsocketinqping");