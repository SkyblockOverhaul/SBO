import { getLastGuessTime } from "./DianaGuess";
import { Keybind } from "../../../tska/shared/Keybind"
import { Waypoint, lockWarp } from "../general/Waypoints";
import settings from "../../settings";
import { toTitleCase, setTimeout } from "../../utils/functions";

let closestWarpGuess = undefined;
let closestWarpInq = undefined;
let warpedTo = ""
let tryWarp = false;
const warpKey = new Keybind("Burrow Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
warpKey.registerKeyPress(() => {
    if (!settings.dianaBurrowWarp) return;
    if (settings.warpDelay && Date.now() - getLastGuessTime() < settings.warpDelayTime) return;
    closestWarpGuess = Waypoint.getClosestWarp(Waypoint.guessWp);
    if (closestWarpGuess && !tryWarp) {
        warpedTo = closestWarpGuess;
        tryWarp = true;
        ChatLib.command("warp " + closestWarpGuess);
        setTimeout(() => {
            tryWarp = false;
        }, 2000);
    }
});

const inquisWarpKey = new Keybind("Iqnuis Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
inquisWarpKey.registerKeyPress(() => {
    if (!settings.inqWarpKey) return;

    const inqWaypoints = Waypoint.getWaypointsOfType("inq");
    if (inqWaypoints.length == 0) return;

    closestWarpInq = Waypoint.getClosestWarp(inqWaypoints[inqWaypoints.length - 1]);
    if (closestWarpInq && !tryWarp) {
        warpedTo = closestWarpInq;
        tryWarp = true;
        ChatLib.command("warp " + closestWarpInq);
        setTimeout(() => {
            tryWarp = false;
        }, 2000);
    }
});

register("chat", () => {
    if (tryWarp) {
        ChatLib.chat("§6[SBO] §4Warp " + toTitleCase(warpedTo) + " is not unlocked!")
        lockWarp(warpedTo);
    }
}).setCriteria("&r&cYou haven't unlocked this fast travel destination!&r");