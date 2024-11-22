import RenderLib from "../../../RenderLib";
import settings from "../../settings";
import { checkDiana } from "../../utils/checkDiana";
import { isWorldLoaded, playCustomSound, toTitleCase } from "../../utils/functions";
import { registerWhen } from "../../utils/variables";
import { Color } from '../../../Vigilance';
import renderBeaconBeam from "../../../BeaconBeam/index";

let inqWaypoints = [];
let burrowWaypoints = [];
let guessWaypoint = undefined;
class Waypoint {
    static waypoints = [];
    static formattedWaypoints = [];
    constructor(x, y, z, r, g, b, alpha = 0.5, text = "MyWaypoint", beam = true, distanceBool = true, removeAfter = 30, ownList = false, hideAtDistance = 0, xyCheck = []) {
        this.x = Math.round(x);
        this.y = Math.round(y);
        this.z = Math.round(z);
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = alpha;
        this.text = text;
        this.beam = beam;
        this.distanceBool = distanceBool;
        this.removeAfter = removeAfter;
        this.hideAtDistance = hideAtDistance;
        this.xyCheck = xyCheck;
        this.timeStamp = Date.now();
        if (!ownList) Waypoint.waypoints.push(this);
    }

    setPos(x, y, z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    setText(text) {
        this.text = text;
    }

    formatWaypoint() {
        const distanceRaw = Math.hypot(Player.getX() - this.x, Player.getY() - this.y, Player.getZ() - this.z);
        let x = this.x
        let z = this.z
        let y = this.y - 1
        // Makes it so waypoint render even if it is far away
        if (distanceRaw >= 230) {
            x = Player.getX() + (this.x - Player.getX()) * (230 / distanceRaw);
            z = Player.getZ() + (this.z - Player.getZ()) * (230 / distanceRaw);
        }

        // Formats and realigns everything
        let xSign, zSign = 0;
        if (this.xyCheck.length) {
            if (this.xyCheck[0] > 0) {
                xSign = 1;
            }
            else if (this.xyCheck[0] < 0) {
                xSign = -1;
            }
            if (this.xyCheck[2] > 0) {
                zSign = 1;
            }
            else if (this.xyCheck[2] < 0) {
                zSign = -1;
            }
        }
        else {
            xSign = x == 0 ? 1 : Math.sign(x);
            zSign = z == 0 ? 1 : Math.sign(z);
        }

        const distance = Math.round(distanceRaw) + "m";
        let wp = [`${this.text} §b[${distance}]`, x + 0.5*xSign, y, z + 0.5*zSign, distanceRaw, this.beam, this.distanceBool, this.hideAtDistance, this.alpha];
        if (xSign == 1) xSign = 0;
        if (zSign == 1) zSign = 0;
        let wp2 = [x + xSign, y, z + zSign];
        Waypoint.formattedWaypoints.push([wp, wp2, [this.r, this.g, this.b]]);
    }
}

function renderWaypoint(waypoint) {
    const box = waypoint[0];
    const beam = waypoint[1];
    const rgb = waypoint[2];
    const hideAtDistance = box[7];
    const alpha = box[8];
    if (box[4] <= hideAtDistance) return;
    RenderLib.drawInnerEspBox(box[1], box[2], box[3], 1, 1, rgb[0], rgb[1], rgb[2], alpha/2, true);
    let hexCodeString = javaColorToHex(new Color(rgb[0], rgb[1], rgb[2]));

    if (box[0] != "" && box[0] != "§7") {
        Tessellator.drawString(box[0], box[1], box[2] + 1.5, box[3], parseInt(hexCodeString, 16), true);
    }

    if (box[4] >= hideAtDistance && box[5]) {
        renderBeaconBeam(beam[0], beam[1]+1, beam[2], rgb[0], rgb[1], rgb[2], alpha, false);
    }

}

function javaColorToHex(javaColor) {
    // Extract RGB components
    let red = javaColor.getRed();
    let green = javaColor.getGreen();
    let blue = javaColor.getBlue();

    // Convert RGB to hexadecimal
    let hex = "0x" + componentToHex(red) + componentToHex(green) + componentToHex(blue);

    return hex;
}

// Helper function to convert a single color component to hexadecimal
function componentToHex(c) {
    let hex = c.toString(16);
    return hex.length == 1 ? "0" + hex : hex;
}

register("step", () => {
    Waypoint.waypoints = Waypoint.waypoints.filter((waypoint) => {
        if (waypoint.removeAfter == 0) return true;
        return Date.now() - waypoint.timeStamp < waypoint.removeAfter * 1000;
    })
    inqWaypoints = inqWaypoints.filter((waypoint) => {
        return Date.now() - waypoint.timeStamp < waypoint.removeAfter * 1000;
    })
}).setFps(1);

register("step", () => {
    Waypoint.formattedWaypoints = [];
    Waypoint.waypoints.forEach((waypoint) => {
        waypoint.formatWaypoint();
    });
    inqWaypoints.forEach((waypoint) => {
        waypoint.formatWaypoint();
    });
}).setFps(10);

register("renderWorld", () => {
    Waypoint.formattedWaypoints.forEach((waypoint) => {
        renderWaypoint(waypoint);
    });
});

registerWhen(register("chat", (player, spacing, x, y, z, event) => {
    if (isWorldLoaded()) {
        if (checkDiana() && settings.allWaypointsAreInqs) {
            isInq = true;
        }
        else {
            isInq = !z.includes(" ");
        }
        const bracketIndex = player.indexOf('[') - 2;
        const channel = player.substring(0, bracketIndex);
        // channel.includes("Guild") || channel.includes("Party") || channel.includes("Co-op")
        if (channel.includes("Guild")) return;
        if (bracketIndex >= 0)
            player = player.replaceAll('&', '§').substring(bracketIndex, player.length);
        else
            player = player.replaceAll('&', '§');

        if (isInq) {
            if(settings.inqHighlight || settings.inqCircle && checkDiana()) {
                highlighInquis = true;
                setTimeout(() => {
                    highlighInquis = false;
                }, 80000); // 80 seconds so it only unregisters after inq is 100% dead cause it despawns after 75 secs
            }
            z = z.replace("&r", "");

            if(settings.inqWaypoints && checkDiana()) {
                Client.showTitle(`&r&6&l<&b&l&kO&6&l> &b&lINQUISITOR! &6&l<&b&l&kO&6&l>`, player, 0, 90, 20);
                playCustomSound(settings.inqSound, settings.inqVolume);
                
                if (inqWaypoints.length > 3) inqWaypoints.shift();
                if (!(player.includes(Player.getName()) && (settings.hideOwnWaypoints == 1 || settings.hideOwnWaypoints == 3))) {
                    if (z.split(" ").length > 1) {
                        z = z.split(" ")[0];
                    }
                    inqWaypoints.push(new Waypoint(x, y, z, 0, 1, 0.84, 0, player + closestWarpString(x, y, z), true, true, 60, true));
                }
            }
            else{
                new Waypoint(x, y, z, 0, 0.2, 1, 0.5, player);
            }
        }
        else {
            if(settings.patcherWaypoints) {
                z = z.split(" ")[0];
                new Waypoint(x, y, z, 0, 0.2, 1, 0.5, player);
            }
        }
    }
}).setCriteria("${player}&f${spacing}x: ${x}, y: ${y}, z: ${z}"), () => settings.patcherWaypoints || settings.inqWaypoints || settings.inqHighlight || settings.inqCircle);

let hubWarps = {
    castle: {x: -250, y: 130, z: 45, unlocked: true},
    da: {x: 92, y: 75, z: 174, unlocked: true},
    hub: {x: -3, y: 70, z: -70, unlocked: true},
    museum: {x: -76, y: 76, z: 81, unlocked: true},
};

let tryWarp = false;
let closestWarp = undefined;
registerWhen(register("chat", () => {
    if (tryWarp) {
        ChatLib.chat("§6[SBO] §4Warp " + toTitleCase(closestWarp) + " is not unlocked!")
        hubWarps[closestWarp].unlocked = false;
    }
}).setCriteria("&r&cYou haven't unlocked this fast travel destination!&r"), () => settings.inqWarpKey);

// const warpKey = new Keybind("Burrow Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
// warpKey.registerKeyPress(() => {
//     if (settings.dianaBurrowWarp && finalLocation != null) {
//         getClosestWarp(finalLocation.x, finalLocation.y, finalLocation.z);
//         if (warpPlayer) {
//             ChatLib.command("warp " + closestWarp);
//             tryWarp = true;
//             setTimeout(() => {
//                 tryWarp = false;
//             }, 2000);
//         }
//     }
// });

// const inquisWarpKey = new Keybind("Iqnuis Warp", Keyboard.KEY_NONE, "SkyblockOverhaul");
// inquisWarpKey.registerKeyPress(() => {
//     if (settings.inqWarpKey) {
//         warps = getInqWaypoints();
//         if (warps.length > 0) {
//             getClosestWarp(warps[warps.length - 1][1], warps[warps.length - 1][2], warps[warps.length - 1][3]);
//             if (warpPlayer) {
//                 ChatLib.command("warp " + closestWarp);
//                 tryWarp = true;
//                 setTimeout(() => {
//                     tryWarp = false;
//                 }, 2000);
//             }
//         }
//     }
// });

let warpPlayer = false;
let closestDistance = Infinity;
function getClosestWarp(x, y, z) {
    const closestPlayerdistance = Math.sqrt(
        (Player.getLastX() - x)**2 +
        (Player.getLastY() - y)**2 +
        (Player.getLastZ() - z)**2
    );
    closestDistance = Infinity;

    switch (settings.dianaAddWarps) {
        case 0:
            delete hubWarps.wizard;
            delete hubWarps.crypt;
            break;
        case 1:
            hubWarps.wizard = {x: 42, y: 122, z: 69, unlocked: true}
            delete hubWarps.crypt;
            break;
        case 2:
            hubWarps.crypt = {x: -161, y: 61, z: -99, unlocked: true}
            delete hubWarps.wizard;
            break;
        case 3:
            hubWarps.wizard = {x: 42, y: 122, z: 69, unlocked: true}
            hubWarps.crypt = {x: -161, y: 61, z: -99, unlocked: true}
            break;
    }

    if (settings.stonksWarp) {
        hubWarps.stonks = {x: -53, y: 72, z: -53, unlocked: true}
    } else {
        delete hubWarps.stonks;
    }

    for (let warp in hubWarps) {
        if (hubWarps[warp].unlocked) {
            let distance = Math.sqrt(
                (hubWarps[warp].x - x)**2 +
                (hubWarps[warp].y - y)**2 +
                (hubWarps[warp].z - z)**2
            );
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWarp = warp;
            }
        }
    }
    settings.warpDiff = settings.warpDiff.replace(/\D/g, '');
    let warpDiff = parseInt(settings.warpDiff);

    const warpConditions = {
        condition1: Math.round(parseInt(closestPlayerdistance)) > Math.round(parseInt(closestDistance) + warpDiff),
        condition2: (Math.round(parseInt(closestPlayerdistance)) > Math.round(parseInt(closestDistance) + warpDiff) &&
                    inqWaypoints.length > 0)
    };
    // Math.round(getClosestBurrow(formattedBurrow)[1]) > 60) ||
    if (settings.dontWarpIfBurrowNearby ? warpConditions.condition2 : warpConditions.condition1) {
        warpPlayer = true;
    } else {
        warpPlayer = false;
    }
    if (warpPlayer) {
        return closestWarp;
    }
    else {
        return "no warp";
    }
}

let warpString = "";
function closestWarpString(x, y, z) {
    closestWarp = getClosestWarp(x, y, z);
    if (closestWarp == "no warp") {
        closestWarp = "";
        warpString = "";
    }
    else {
        warpString = ` (warp ${closestWarp})`;
    }
    return warpString;
}