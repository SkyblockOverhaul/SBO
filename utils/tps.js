import settings from "../settings";
import { registerWhen } from "./variables";

let totalTps = 0;
let count = 0;

// credits to GriffinOwO
function calculateThreeSecondAvarageTps() {
    count++;
    totalTps += getTps()

    if (count >= 60) {
        ChatLib.command(`pc tps > ${(totalTps / 60).toFixed(1)}`);
        count = 0;
        totalTps = 0;
        return;
    }

    setTimeout(() => {
        calculateThreeSecondAvarageTps();
    }, 50);
}


export function tpsCommand(player) {   
    if (count === 0) {
        setTimeout(() => {
            ChatLib.chat(`&6[SBO] &eTrying to get tps, wait for 3s`);
            calculateThreeSecondAvarageTps();
        }, 10);
    } else {
        setTimeout(() => {
            ChatLib.chat(`&6[SBO] &eCalculating please wait!`);
        }, 10);
    }
}

const S03PacketTimeUpdate = Java.type("net.minecraft.network.play.server.S03PacketTimeUpdate");

let prevTime = 0;
let averageTps = 20.0;
let tps = 20.0;

registerWhen(register("worldLoad", () => {
    prevTime = 0;
    averageTps = 20.0;
}), () => settings.tpsCommand);

registerWhen(register("packetReceived", (packet) => {
    if (!(packet instanceof S03PacketTimeUpdate)) return;

    tps = 20000 / (Date.now() - prevTime + 1);
    tps = tps > 20 ? 20.0 :
        tps < 0 ? 0.0 :
            tps;

    if (prevTime !== 0) {
        averageTps = tps * 0.182 + averageTps * 0.818;
    }

    prevTime = Date.now();
}), () => settings.tpsCommand);

export function getAverageTps() {
    return averageTps;
}

export function getTps() {
    return tps;
}
