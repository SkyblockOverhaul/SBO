import settings from "../../settings";
import { setTimeout, checkSendInqMsg, getParty } from "../../utils/functions";
import { data, registerWhen } from "../../utils/variables";
import { checkDiana } from "../../utils/checkDiana";

let playerIsMuted = false;
let sendPing = false;

registerWhen(register("chat", (trash, expire) => {
    if (!checkDiana()) return;
    if (playerIsMuted) return;
    playerIsMuted = true;
    // if (sendPing) sendInqPingSocket();
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